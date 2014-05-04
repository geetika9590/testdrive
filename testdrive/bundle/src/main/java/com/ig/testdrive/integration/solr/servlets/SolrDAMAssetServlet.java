package com.ig.testdrive.integration.solr.servlets;

import com.day.cq.dam.api.Asset;
import com.ig.testdrive.commons.util.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;


@Component(label = "Solr DAM Asset servlet", description = "This servlet posts data to Solr server for DAM assets", enabled = true, immediate = true, metatype = true)
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
    ResourceResolverFactory resolverFactory;

    private static final Logger log = LoggerFactory.getLogger(SolrDAMAssetServlet.class);

    private static final String METADATA_VALUES_TO_BE_INDEXED = "indexed.values";

    @Property(name = METADATA_VALUES_TO_BE_INDEXED, label = "Metadata Indexed values", description = "Enter the fields to be indexed for DAM assets",
            value = {"title=dc:title", "description=dc:description", "format=dc:format","damTags=cq:tags"}, propertyPrivate = false, cardinality = Integer.MAX_VALUE)
    private String INDEX_VALUES;
    private String[] indexedVal;

    @Activate
    protected void activate(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties();
        log.info("inside activate method of DAM servlet");
        indexedVal = (String[]) properties.get(METADATA_VALUES_TO_BE_INDEXED);

        log.info("Index Values are" + indexedVal);

    }

    @Modified
    protected void modified(ComponentContext componentContext) {
        log.info("Values have been modified");
        activate(componentContext);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
          log.info("get called" + indexedVal);
        PrintWriter out = response.getWriter();
        Resource resource = request.getResource();
        log.info("resource is " + resource);
        Asset asset = resource.adaptTo(Asset.class);

        log.info("asset & asset metadata is" + asset + " " + asset.getMetadata());
        ResourceResolver resourceResolver = null;
        HashMap<String, String> map = new HashMap<String, String>();
        try {
            resourceResolver = resolverFactory.getAdministrativeResourceResolver(null);
            if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {
                log.info("inside if");
                for (String s : indexedVal) {
                    log.info("String is"+s);
                    String[] parts = s.split("=");
                    map.put(parts[0], parts[1]);
                }
                log.info("Solr field names and values are " + map);
                String xmlString=getXMLData(asset,map);
                response.getOutputStream().write(xmlString.getBytes());
            }
        } catch (LoginException e) {
            log.info("Exception occured"+e.getMessage());
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            log.info("finally executed");
            if (resourceResolver != null)
                resourceResolver.close();
        }


    }

    private String getXMLData(Asset asset, HashMap fieldMap) {

        HashMap assetMetadata = (HashMap) asset.getMetadata();
        Set keys = fieldMap.keySet();
        Iterator iterator = keys.iterator();
        StringBuffer xmlData = new StringBuffer("<add>\n" +
                "<doc>\n" + "<field name=\"id\">" + asset.getPath() +
                "</field>\n"+"<field name=\"type\">asset</field>\n");
        log.info("xmlData so far is"+xmlData);
        while (iterator.hasNext()) {
            log.info("xmlData so far in loop is"+xmlData);
            String type=iterator.next().toString();
            if ( assetMetadata.get(fieldMap.get(type)) instanceof Object[]){
                log.info("inside if val");
                Object[] arr= (Object[]) assetMetadata.get(fieldMap.get(type));
                for(Object value:arr){
                    log.info("Value is"+value);
                    if (value!=null){
                        value= StringEscapeUtils.escapeXml(value.toString());
                        log.info("value after escaping character is"+value);
                        xmlData=xmlData.append("<field name=\"").append(type)
                                .append("\">").append(value.toString()).append("</field>\n");
                    }
                }
            }
            else{
                String value= (String)assetMetadata.get(fieldMap.get(type));
                log.info("Value is"+value);
                if (value!=null){
                    value= StringEscapeUtils.escapeXml(value.toString());;
                    log.info("value after escaping character is"+value);
                    xmlData=xmlData.append("<field name=\"").append(type)
                            .append("\">").append(value).append("</field>\n");
                }
            }


        }
        xmlData.append("</doc>\n").append("</add>");

        log.info("Xml generated is" + xmlData);
        return xmlData.toString();
    }
}
