package com.ig.testdrive.integration.solr.servlets;

import com.day.cq.dam.api.Asset;
import com.ig.testdrive.commons.util.CommonMethods;
import com.ig.testdrive.integration.solr.service.SolrFieldMap;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Geetika
 * Date: 19/5/14
 * Time: 10:32 AM
 */

@Component(label = "Solr DAM Asset servlet", description = "This servlet generates for DAM assets", enabled = true, immediate = true, metatype = true)
@Service(Servlet.class)
@Properties({
        @Property(name = "service.description", value = "This servlet posts data to Solr server for DAM assets"),
        @Property(name = "service.vendor", value = "Intelligrape"),
        @Property(name = "sling.servlet.resourceTypes", value = "dam:Asset", propertyPrivate = true),
        @Property(name = "sling.servlet.extensions", value = "xml", propertyPrivate = true),
        @Property(name = "sling.servlet.selectors", value = "solrsearch", propertyPrivate = true),
        @Property(name = "sling.servlet.methods", value = "GET", propertyPrivate = true)
})
public class SolrDAMAssetServlet extends SlingSafeMethodsServlet {

    @Reference
    SolrFieldMap solrFieldMap;

    private static final Logger log = LoggerFactory.getLogger(SolrDAMAssetServlet.class);

    private HashMap<String, String> map = new HashMap<String, String>();

    @Activate
    protected void activate(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties();
        log.debug("inside activate method of DAM servlet");
    }

    @Modified
    protected void modified(ComponentContext componentContext) {
        log.debug("Values have been modified");
        activate(componentContext);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        Resource resource = request.getResource();
        log.debug("resource is " + resource);
        Asset asset = resource.adaptTo(Asset.class);
        log.debug("asset & asset metadata is" + asset + " " + asset.getMetadata());
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = request.getResourceResolver();
            if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {
                log.debug("inside if");
                map=solrFieldMap.getAssetFieldMap();
                log.debug("Solr field names and values are " + map);
                String xmlString=getXMLData(asset,map);
                response.getOutputStream().write(xmlString.getBytes());
            }
        }finally {
            log.debug("finally executed");
            if (resourceResolver != null)
                resourceResolver.close();
        }
    }

    private String getXMLData(Asset asset, HashMap fieldMap) {
        HashMap assetMetadata = (HashMap) asset.getMetadata();
        StringBuffer xmlData = new StringBuffer("<add>\n" +
                "<doc>\n" + "<field name=\"id\">" + asset.getPath() +
                "</field>\n"+"<field name=\"type\">asset</field>\n");
        log.debug("xmlData so far is"+xmlData);
        xmlData=CommonMethods.parseFieldMap((ValueMap)assetMetadata,fieldMap,xmlData);
        xmlData.append("</doc>\n").append("</add>");
        log.debug("Xml generated is" + xmlData);
        return xmlData.toString();
    }
}
