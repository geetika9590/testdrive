package com.ig.testdrive.integration.solr.replication;

import com.day.cq.dam.api.Asset;
import com.day.cq.replication.*;
import com.day.cq.wcm.api.Page;
import com.ig.testdrive.integration.solr.service.*;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

@Component(immediate = true, enabled = true, metatype = true)
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
    CQOperationsForSolrSearch cqOperationsForSolrSearch;

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
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            Resource resource = resourceResolver.resolve(resourcePath);
            ValueMap properties = resource.adaptTo(ValueMap.class);
            String primaryType = (String) properties.get("jcr:primaryType");
            log.info("Primary type is {}", primaryType);
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
        } catch (Exception e) {
            log.info("cause of error =>" + e.getMessage());
        } finally {
            if (resourceResolver != null)
                resourceResolver.close();
        }

        return blankData(factory);
    }

    private String getXMLData(ResourceResolver resourceResolver, String xmlDataUrl) throws Exception {

        Asset asset = getSpecificAsset(resourceResolver, xmlDataUrl);
        HashMap metadata = (HashMap) asset.getMetadata();
        StringBuffer xmlData = new StringBuffer("<add>\n" +
                "    <doc>\n" +
                "        <field name=\"id\">" + xmlDataUrl + "</field>\n" +
                "        <field name=\"text_data\">" + "random text" + "</field>\n" +
                "    </doc>\n" +
                "</add>");
        log.info("Xml generated is" + xmlData);
        return xmlData.toString();
    }

    private Page getSpecificPage(ResourceResolver resourceResolver, String pageUri) {

        Resource resource = resourceResolver.resolve(pageUri);
        return resource.adaptTo(Page.class);
    }

    private Asset getSpecificAsset(ResourceResolver resourceResolver, String assetUri) {

        Resource resource = resourceResolver.resolve(assetUri);
        return resource.adaptTo(Asset.class);
    }


    private ReplicationContent modificationEvents(ResourceResolver resourceResolver, String currentResUri, String solrUri, ReplicationActionType operation, ReplicationContentFactory factory, String resType) throws Exception {

        if (!solrUri.equals("")) {
            if (operation.equals(ReplicationActionType.ACTIVATE)) {
                if (resType.equalsIgnoreCase("page")) {
                    return pageActivationEvent(resourceResolver, currentResUri, factory);
                } else if (resType.equalsIgnoreCase("asset")) {
                    return pageActivationEvent(resourceResolver, currentResUri, factory);
                }
            } else if (operation.equals(ReplicationActionType.DEACTIVATE)) {
                return deactivationEvent(currentResUri, solrUri, factory);
            } else if (operation.equals(ReplicationActionType.DELETE)) {
                return deactivationEvent(currentResUri, solrUri, factory);
            }
        }
        return blankData(factory);
    }

    private ReplicationContent assetActivationEvent(ResourceResolver resourceResolver, String currentResUri, ReplicationContentFactory factory) throws Exception {

        String xmlData = getXMLData(resourceResolver, currentResUri);

        log.info("\n ========xml Data for page URI {} = \n {} \n", currentResUri, xmlData);
//        xmlData="";
        return (!xmlData.equals("")) ? create(factory, xmlData) : blankData(factory);
    }

    private ReplicationContent pageActivationEvent(ResourceResolver resourceResolver, String currentPageUri, ReplicationContentFactory factory) throws Exception {

        currentPageUri = currentPageUri + ".solrsearch.xml";
        String xmlData = cqOperationsForSolrSearch.getXMLData(resourceResolver, currentPageUri);

        log.info("\n ========xml Data for page URI {} = \n {} \n", currentPageUri, xmlData);
//        xmlData="";
        return (!xmlData.equals("")) ? create(factory, xmlData) : blankData(factory);
    }

    private ReplicationContent deactivationEvent(String currentResUri, String solrUri, ReplicationContentFactory factory) throws Exception {

        log.info(" \n Deactivating the Page with URI = {} \n for solrUri = {}", currentResUri, solrUri);
        String xmlData = "<delete><id>" + currentResUri + "</id></delete>";
        return create(factory, xmlData);
    }

    private ReplicationContent create(ReplicationContentFactory factory, String xmlData) throws Exception {

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
            log.info(xmlData);
            return create(factory, xmlData);
        } catch (Exception e) {
            log.info("Error in Replication Handler blank data method " + e.getMessage());
        }
        return ReplicationContent.VOID;
    }

    private ReplicationContent moveEvent(String currentResUri, String solrUri, ReplicationActionType operation, ReplicationContentFactory factory) throws Exception {

        boolean flag = isPageUriStartsWithBlackListPaths(currentResUri);
        if (!flag) {
            if (solrUri.equals("")) {
                log.info(" \n Solr User Agents doesn't Have Read Permission for {} \n ", currentResUri);
            } else {
                log.info("\n Move Operation Performed on page \n");
                if (operation.equals(ReplicationActionType.DELETE))
                    return deactivationEvent(currentResUri, solrUri, factory);
            }
        }
        return deactivationEvent("_blank_", solrUri, factory);
    }

    private boolean isPageUriStartsWithBlackListPaths(String pageUri) {

        boolean status = false;
        for (String blackListUrl : BLACK_LIST_URLS) {
            if (pageUri.startsWith(blackListUrl))
                status = true;
            break;
        }
        return status;
    }

}
