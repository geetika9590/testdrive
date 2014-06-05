package com.ig.testdrive.integration.solr.replication;

import com.day.cq.dam.api.Asset;
import com.day.cq.replication.*;
import com.day.cq.wcm.api.Page;
import com.ig.testdrive.integration.solr.service.CQOperationsForSolrReplication;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

/**
 * This class replicates content to Solr Server.
 */
@Component(label = "Solr Replication Content Builder", description = "This servlet replicates data to solr", immediate = true, enabled = true, metatype = true)
@Service(ContentBuilder.class)
@Property(name = "name", value = "SolrReplication", propertyPrivate = true)
public class SolrReplicationContentBuilderImpl implements ContentBuilder {

    public static final String name = "SolrReplication";
    /**
     * This serves as the title of the serialization type.
     */
    public static final String title = "Solr Replication Content Builder";
    /**
     * This contains the black list urls for move operation.
     */
    private static final String BLACK_LIST_FOR_MOVE_OPERATION = "black.list.uri.for.page.move.operation";

    /**
     * It is a configuration property to add the blacklist urls.
     */
    @Property(name = BLACK_LIST_FOR_MOVE_OPERATION, value = {"/content/catalogs", "/content/campaigns"},
            description = "default blacklist urls are \" /content/catalogs,/content/campaigns\"",
            label = BLACK_LIST_FOR_MOVE_OPERATION, cardinality = Integer.MAX_VALUE)
    private String[] BLACK_LIST_URLS;

    /**
     * Log variable for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SolrReplicationContentBuilderImpl.class);

    /**
     * It contains the reference for CQOperationsForSolrReplication service.
     */
    @Reference
    CQOperationsForSolrReplication cqOperationsForSolrReplication;

    /**
     * It contains the reference for ResourceResolverFactory service.
     */
    @Reference
    ResourceResolverFactory resourceResolverFactory;

    /**
     * Activate method of this component.
     *
     * @param componentContext
     */
    @Activate
    protected void activate(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties();
        BLACK_LIST_URLS = (String[]) properties.get(BLACK_LIST_FOR_MOVE_OPERATION);
    }

    /**
     * Modified method of this service.
     *
     * @param componentContext
     */
    @Modified
    protected void modified(ComponentContext componentContext) {
        activate(componentContext);
    }

    /**
     * Getter method of name.
     *
     * @return name.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Getter method for title.
     *
     * @return title
     */
    @Override
    public String getTitle() {
        return title;
    }

    /**
     * @param session
     * @param action
     * @param replicationContentFactory
     * @param stringObjectMap
     * @return
     * @throws ReplicationException
     */
    @Override
    public ReplicationContent create(Session session, ReplicationAction action, ReplicationContentFactory replicationContentFactory, Map<String, Object> stringObjectMap) throws ReplicationException {
        return null;
    }

    /**
     * It creates the data which will be pushed to solr server.
     *
     * @param session
     * @param action
     * @param factory
     * @return ReplicationContent
     * @throws ReplicationException
     */
    @Override
    public ReplicationContent create(Session session, ReplicationAction action, ReplicationContentFactory factory) throws ReplicationException {

        AgentConfig config = action.getConfig();
        String solrUri = config.getTransportURI();
        ResourceResolver resourceResolver = null;
        String resourcePath = action.getPath();
        String resType = "";
        try {
            HashMap map = new HashMap();
            map.put("user.jcr.session", session);
            resourceResolver = resourceResolverFactory.getResourceResolver(map);
            LOG.debug("resource resolver got is" + resourceResolver);
            Resource resource = resourceResolver.resolve(resourcePath);
            LOG.debug("resource path is"+resource +" path is"+resourcePath);
            ValueMap properties = resource.adaptTo(ValueMap.class);
            String primaryType = (String) properties.get("jcr:primaryType");
            LOG.debug("Primary type is {}", primaryType);
            Page page = null;
            Asset asset = null;
            if (primaryType.equalsIgnoreCase("cq:Page")) {
                page = resource.adaptTo(Page.class);
                resType = "page";
            } else if (primaryType.equalsIgnoreCase("dam:Asset")) {
                asset = resource.adaptTo(Asset.class);
                resType = "asset";
            }
            if (page != null || asset != null) {
                return modificationEvents(resourceResolver, resourcePath, solrUri, action.getType(), factory, resType);
            } else
                return moveEvent(resourcePath, solrUri, action.getType(), factory);
        } catch (LoginException e) {
            LOG.error("Exception occured while getting the resource resolver ");
            e.printStackTrace();
        } catch (ServletException e) {
            LOG.error("Servlet exception occured");
            e.printStackTrace();
        } catch (IOException e) {
            LOG.error("Exception occured while doing I/O operations");
            e.printStackTrace();
        } finally {
            if (resourceResolver != null)
                resourceResolver.close();
            LOG.debug("finally executed successfully");
        }
        return blankData(factory);
    }


    /**
     * It caters for any move operation on the content.
     *
     * @param resourceResolver
     * @param currentResUri
     * @param solrUri
     * @param operation
     * @param factory
     * @param resType
     * @return ReplicationContent
     * @throws ServletException
     * @throws IOException
     */
    private ReplicationContent modificationEvents(ResourceResolver resourceResolver, String currentResUri, String solrUri, ReplicationActionType operation, ReplicationContentFactory factory, String resType) throws ServletException, IOException {

        if (!solrUri.equals("")) {
            if (operation.equals(ReplicationActionType.ACTIVATE)) {
                return pageActivationEvent(resourceResolver, currentResUri, factory);
            } else if (operation.equals(ReplicationActionType.DEACTIVATE)) {
                return deactivationEvent(currentResUri, solrUri, factory);
            } else if (operation.equals(ReplicationActionType.DELETE)) {
                return deactivationEvent(currentResUri, solrUri, factory);
            }
        }
        return blankData(factory);
    }

    /**
     * It caters for any activation events on content.
     *
     * @param resourceResolver
     * @param currentUri
     * @param factory
     * @return
     * @throws ServletException
     * @throws IOException
     */
    private ReplicationContent pageActivationEvent(ResourceResolver resourceResolver, String currentUri, ReplicationContentFactory factory) throws ServletException, IOException {

        currentUri = currentUri + ".solrsearch.xml";
        String xmlData = cqOperationsForSolrReplication.getXMLData(resourceResolver, currentUri);

        LOG.debug("\n ========xml Data for URI {} = \n {} \n", currentUri, xmlData);
        return (!xmlData.equals("")) ? create(factory, xmlData) : blankData(factory);
    }

    /**
     * It caters for any deactivation events on content.
     *
     * @param currentResUri
     * @param solrUri
     * @param factory
     * @return ReplicationContent
     * @throws IOException
     */
    private ReplicationContent deactivationEvent(String currentResUri, String solrUri, ReplicationContentFactory factory) throws IOException {

        LOG.debug(" \n Deactivating the Page with URI = {} \n for solrUri = {}", currentResUri, solrUri);
        String xmlData = "<delete><id>" + currentResUri + "</id></delete>";
        LOG.debug(" \n XMLData generated for deleting the content is", xmlData);
        return create(factory, xmlData);
    }

    /**
     * It creates a temporary file with the content passed in the method.
     *
     * @param factory
     * @param xmlData
     * @return ReplicationContent
     * @throws IOException
     */
    private ReplicationContent create(ReplicationContentFactory factory, String xmlData) throws IOException {

        File tempFile = null;
        BufferedWriter out = null;
        try {
            tempFile = File.createTempFile("xmlData", ".tmp");
            out = new BufferedWriter(new FileWriter(tempFile));
            out.write(xmlData);
            out.close();
            return factory.create("application/xml", tempFile, true);
        } finally {
            if (out != null) {
                IOUtils.closeQuietly(out);
                if (tempFile != null)
                    tempFile.delete();
            }
        }
    }

    /**
     * It creates a blank xml.
     *
     * @param factory
     * @return
     */
    private ReplicationContent blankData(ReplicationContentFactory factory) {
        try {
            String xmlData = "<add><doc><field name=\"id\">_blank_</field></doc></add>";
            LOG.debug("blank xmlData is" + xmlData);
            return create(factory, xmlData);
        } catch (Exception e) {
            LOG.debug("Error in Replication Handler blank data method ");
            e.printStackTrace();
        }
        return ReplicationContent.VOID;
    }

    /**
     * It caters to the move event.
     *
     * @param currentResUri
     * @param solrUri
     * @param operation
     * @param factory
     * @return ReplicationContent
     * @throws IOException
     */
    private ReplicationContent moveEvent(String currentResUri, String solrUri, ReplicationActionType operation, ReplicationContentFactory factory) throws IOException {

        boolean flag = isPageUriStartsWithBlackListPaths(currentResUri);
        if (!flag) {
            if (solrUri.equals("")) {
                LOG.debug(" \n Solr User Agents doesn't Have Read Permission for {} \n ", currentResUri);
            } else {
                LOG.debug("\n Move Operation Performed on page \n");
                if (operation.equals(ReplicationActionType.DELETE))
                    return deactivationEvent(currentResUri, solrUri, factory);
            }
        }
        return deactivationEvent("_blank_", solrUri, factory);
    }

    /**
     * It checks whether a page starts with the black list uri's.
     *
     * @param pageUri
     * @return
     */
    private boolean isPageUriStartsWithBlackListPaths(final String pageUri) {

        LOG.debug("pageURI received is" + pageUri);

        boolean status = false;
        for (String blackListUrl : BLACK_LIST_URLS) {
            if (pageUri.startsWith(blackListUrl))
                status = true;
            break;
        }
        return status;
    }

}
