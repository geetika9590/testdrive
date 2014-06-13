package com.ig.testdrive.integration.solr.servlets;

import com.day.cq.dam.api.Asset;
import com.ig.testdrive.commons.util.SolrSearchUtil;
import com.ig.testdrive.integration.solr.service.SolrFieldMap;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.HashMap;


@Component(label = "Solr DAM Asset servlet", description = "This servlet generates for DAM assets",
        enabled = true, immediate = true, metatype = true)
@Service(value = Servlet.class)
@Properties({
        @Property(name = "service.description", value = "This servlet posts data to Solr server for DAM assets"),
        @Property(name = "service.vendor", value = "Intelligrape"),
        @Property(name = "sling.servlet.resourceTypes", value = "dam:Asset", propertyPrivate = true),
        @Property(name = "sling.servlet.extensions", value = "xml", propertyPrivate = true),
        @Property(name = "sling.servlet.selectors", value = "solrsearch", propertyPrivate = true),
        @Property(name = "sling.servlet.methods", value = "GET", propertyPrivate = true)
})
/**
 * This is a servlet which extracts the metadata
 * properties for an asset and prepares and XML,
 */
public class SolrDAMAssetServlet extends SlingSafeMethodsServlet {

    /**
     * It contains the reference for a service object SolrFieldMap.
     */
    @Reference
    SolrFieldMap solrFieldMap;

    /**
     * Log variable for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SolrDAMAssetServlet.class);

    /**
     * Map which holds the mappings for CQ and solr fields for an asset.
     */
    private HashMap<String, String> map = new HashMap<String, String>();

    /**
     * Activate method for this component.
     * @param componentContext
     */
    @Activate
    protected void activate(ComponentContext componentContext) {
        LOG.debug("inside activate method of DAM servlet");
    }

    /**
     * Modified method for this component.
     * @param componentContext
     */
    @Modified
    protected void modified(ComponentContext componentContext) {
        LOG.debug("Values have been modified");
        activate(componentContext);
    }

    /**
     * doGet is the method which gets called for a GET request.
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        Resource resource = request.getResource();
        LOG.debug("resource is " + resource);
        Asset asset = resource.adaptTo(Asset.class);
        LOG.debug("asset & asset metadata is" + asset + " " + asset.getMetadata());
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = request.getResourceResolver();
            if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {
                LOG.debug("inside if");
                map = solrFieldMap.getAssetFieldMap();
                LOG.debug("Solr field names and values are " + map);
                String xmlString = getXMLData(asset, map);
                response.getOutputStream().write(xmlString.getBytes());
            }
        } finally {
            LOG.debug("finally executed");
            if (resourceResolver != null)
                resourceResolver.close();
        }
    }

    /**
     * It extracts the metadata properties for  an asset.
     * @param asset
     * @param fieldMap
     * @return
     */
    private String getXMLData(Asset asset, HashMap fieldMap) {
        HashMap assetMetadata = (HashMap)asset.getMetadata();
        StringBuffer xmlData = new StringBuffer("<add>\n" +
                "<doc>\n" + "<field name=\"id\">" + asset.getPath() +
                "</field>\n" + "<field name=\"type\">asset</field>\n");
        LOG.debug("xmlData so far is" + xmlData);
        xmlData = SolrSearchUtil.parseFieldMap(assetMetadata, fieldMap, xmlData);
        xmlData.append("</doc>\n").append("</add>");
        LOG.debug("Xml generated is" + xmlData);
        return xmlData.toString();
    }
}
