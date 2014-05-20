package com.ig.testdrive.integration.solr.replication;

import com.day.cq.dam.api.Asset;
import com.day.cq.replication.*;
import com.day.cq.wcm.api.Page;
import com.ig.testdrive.integration.solr.service.*;
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
 * Created with IntelliJ IDEA.
 * User: Geetika
 * Date: 19/5/14
 * Time: 10:32 AM
 */
@Component(label = "Solr Replication Content Builder", description = "This servlet replicates data to solr",immediate = true, enabled = true, metatype = true)
@Service(ContentBuilder.class)
@Property(name = "name", value = "SolrReplication", propertyPrivate = true)
public class SolrReplicationContentBuilderImpl implements ContentBuilder {

    public static final String name = "SolrReplication";
    public static final String title = "Solr Replication Content Builder";
    private static final String BLACK_LIST_FOR_MOVE_OPERATION = "black.list.uri.for.page.move.operation";

    @Property(name = BLACK_LIST_FOR_MOVE_OPERATION, value = {"/content/catalogs", "/content/campaigns"},
            description = "default blacklist urls are \" /content/catalogs,/content/campaigns\"",
            label = BLACK_LIST_FOR_MOVE_OPERATION, cardinality = Integer.MAX_VALUE)
    private String[] BLACK_LIST_URLS;

    private static final Logger log = LoggerFactory.getLogger(SolrReplicationContentBuilderImpl.class);

    @Reference
    CQOperationsForSolrReplication cqOperationsForSolrReplication;

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Activate
    protected void activate(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties();
        BLACK_LIST_URLS = (String[]) properties.get(BLACK_LIST_FOR_MOVE_OPERATION);
    }

    @Modified
    protected void modified(ComponentContext componentContext) {
        activate(componentContext);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public ReplicationContent create(Session session, ReplicationAction action, ReplicationContentFactory replicationContentFactory, Map<String, Object> stringObjectMap) throws ReplicationException {
        return null;
    }

    @Override
    public ReplicationContent create(Session session, ReplicationAction action, ReplicationContentFactory factory) throws ReplicationException {

        AgentConfig config = action.getConfig();
        String solrUri = config.getTransportURI();
        ResourceResolver resourceResolver = null;
        String resourcePath = action.getPath();
        String resType = "";
        try {
            HashMap map=new HashMap();
            map.put( "user.jcr.session", session);
            resourceResolver=resourceResolverFactory.getResourceResolver(map);
            log.debug("resource resolver got is"+resourceResolver   );
            Resource resource = resourceResolver.resolve(resourcePath);
            ValueMap properties = resource.adaptTo(ValueMap.class);
            String primaryType = (String) properties.get("jcr:primaryType");
            log.debug("Primary type is {}", primaryType);
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
            log.error("Exception occured while getting the resource resolver ");
            e.printStackTrace();
        } catch (ServletException e) {
            log.error("Servlet exception occured");
            e.printStackTrace();
        } catch (IOException e) {
            log.error("Exception occured while doing I/O operations");
            e.printStackTrace();
        }  finally {
            if (resourceResolver != null)
                resourceResolver.close();
            log.debug("finally executed successfully");
        }

        return blankData(factory);
    }


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


    private ReplicationContent pageActivationEvent(ResourceResolver resourceResolver, String currentPageUri, ReplicationContentFactory factory) throws ServletException, IOException {

        currentPageUri = currentPageUri + ".solrsearch.xml";
        String xmlData = cqOperationsForSolrReplication.getXMLData(resourceResolver, currentPageUri);

        log.debug("\n ========xml Data for page URI {} = \n {} \n", currentPageUri, xmlData);
        return (!xmlData.equals("")) ? create(factory, xmlData) : blankData(factory);
    }

    private ReplicationContent deactivationEvent(String currentResUri, String solrUri, ReplicationContentFactory factory) throws IOException {

        log.debug(" \n Deactivating the Page with URI = {} \n for solrUri = {}", currentResUri, solrUri);
        String xmlData = "<delete><id>" + currentResUri + "</id></delete>";
        log.debug(" \n XMLData generated for deleting the content is", xmlData);
        return create(factory, xmlData);
    }

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

    private ReplicationContent blankData(ReplicationContentFactory factory) {
        try {
            String xmlData = "<add><doc><field name=\"id\">_blank_</field></doc></add>";
            log.debug("blank xmlData is" + xmlData);
            return create(factory, xmlData);
        } catch (Exception e) {
            log.debug("Error in Replication Handler blank data method ");
            e.printStackTrace();
        }
        return ReplicationContent.VOID;
    }

    private ReplicationContent moveEvent(String currentResUri, String solrUri, ReplicationActionType operation, ReplicationContentFactory factory) throws IOException {

        boolean flag = isPageUriStartsWithBlackListPaths(currentResUri);
        if (!flag) {
            if (solrUri.equals("")) {
                log.debug(" \n Solr User Agents doesn't Have Read Permission for {} \n ", currentResUri);
            } else {
                log.debug("\n Move Operation Performed on page \n");
                if (operation.equals(ReplicationActionType.DELETE))
                    return deactivationEvent(currentResUri, solrUri, factory);
            }
        }
        return deactivationEvent("_blank_", solrUri, factory);
    }

    private boolean isPageUriStartsWithBlackListPaths(String pageUri) {

        log.debug("pageURI received is"+pageUri);

        boolean status = false;
        for (String blackListUrl : BLACK_LIST_URLS) {
            if (pageUri.startsWith(blackListUrl))
                status = true;
            break;
        }
        return status;
    }

}
