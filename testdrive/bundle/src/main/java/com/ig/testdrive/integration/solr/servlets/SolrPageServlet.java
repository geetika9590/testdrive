package com.ig.testdrive.integration.solr.servlets;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.servlet.Servlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;


@Component(label = "Solr Page Component servlet", description = "This servlet posts data to Solr server for CQ Pages", enabled = true, immediate = true, metatype = true)
@Service(Servlet.class)
@Properties({
        @Property(name = "service.description", value = "This servlet posts data to Solr server for DAM assets"),
        @Property(name = "service.vendor", value = "Intelligrape"),
        @Property(name = "sling.servlet.resourceTypes", value = "cq:Page", propertyPrivate = true),
        @Property(name = "sling.servlet.extensions", value = "xml", propertyPrivate = true),
        @Property(name = "sling.servlet.selectors", value = "solrsearch", propertyPrivate = true),
        @Property(name = "sling.servlet.methods", value = "GET", propertyPrivate = true)
})
public class SolrPageServlet extends SlingSafeMethodsServlet {

    @Reference
    ResourceResolverFactory resolverFactory;

    private static final Logger log = LoggerFactory.getLogger(SolrPageServlet.class);

    private static final String COMP_METADATA_VALUES_TO_BE_INDEXED = "comp.values";

    private static final String PARSYS="foundation/components/parsys";

    private static final String RESOURCE_SEPARATOR ="@";
    private static final String PROPERTY_SEPARATOR ="<>";

    private String[] pageIndexedVal,compIndexedVal;

    private static final String PAGE_METADATA_VALUES_TO_BE_INDEXED = "page.values";

    @Property(name = PAGE_METADATA_VALUES_TO_BE_INDEXED, label = "Indexed values", description = "Enter the solr and property mapping for pages",
            value = {"title=jcr:title", "description=jcr:description"}, propertyPrivate = false, cardinality = Integer.MAX_VALUE)
    private String PAGE_INDEX_VALUES;

    @Property(name = COMP_METADATA_VALUES_TO_BE_INDEXED, label = "Component Indexed values", description = "Enter the component resource types, their property mapping separated by @",
            value = {"foundation/components/title@title=jcr:title","foundation/components/text@text_data=text",
                    "foundation/components/image@text_data=fileReference<>title=jcr:title"},propertyPrivate = false, cardinality = Integer.MAX_VALUE)
    private String COMP_INDEX_VALUES;



    @Activate
    protected void activate(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties();
        log.info("inside activate method of DAM servlet");
        pageIndexedVal = (String[]) properties.get(PAGE_METADATA_VALUES_TO_BE_INDEXED);
        compIndexedVal= (String[]) properties.get(COMP_METADATA_VALUES_TO_BE_INDEXED);

        log.info("Comp Index Values are" + compIndexedVal);
        log.info("Page Index Values are" + pageIndexedVal);

    }

    @Modified
    protected void modified(ComponentContext componentContext) {
        log.info("Values have been modified");
        activate(componentContext);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        log.info("get called" + pageIndexedVal);
        PrintWriter out = response.getWriter();
        Resource resource = request.getResource();
        log.info("resource is " + resource);
        ValueMap valueMap = resource.adaptTo(ValueMap.class);

        log.info("page & page metadata is" + valueMap );
        ResourceResolver resourceResolver = null;
        HashMap<String, String> pageFieldMap = new HashMap<String, String>();
        HashMap<String, HashMap> compFieldMap = new HashMap<String, HashMap>();
        Resource res = null;
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = null;
        Node node = null;
        String tempUrl = "";
        try {
            resourceResolver = resolverFactory.getAdministrativeResourceResolver(null);
            if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {
                log.info("inside if");
                for (String s : pageIndexedVal) {
                    String[] parts = s.split("=");
                    pageFieldMap.put(parts[0], parts[1]);
                }
                log.info("Solr field names and values for page are " + pageFieldMap);
                for (String s : compIndexedVal) {
                    String[] parts = s.split(RESOURCE_SEPARATOR);
                    String[] fieldParts=parts[1].split(PROPERTY_SEPARATOR);
                    HashMap<String,String> solrFieldMap=new HashMap();

                    for (String str:fieldParts) {
                        String[] finalS=str.split("=");
                        solrFieldMap.put(finalS[0], finalS[1]);
                    }
                    compFieldMap.put(parts[0], solrFieldMap);
                }
                log.info("Solr field names and values for components are " + compFieldMap);
                String xmlString=getXMLData(resourceResolver,resource,pageFieldMap,compFieldMap);
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

    private String getXMLData(ResourceResolver resourceResolver,Resource resource, HashMap pagefieldMap,HashMap compFieldMap) {

//        HashMap<String, String> assetMetadata = (HashMap) asset.getMetadata();

        Set keys = pagefieldMap.keySet();
        Iterator iterator = keys.iterator();
        String tempUrl="";
        ValueMap pageValueAap = null;
        StringBuffer xmlData = new StringBuffer("<add>\n" +
                "<doc>\n" + "<field name=\"id\">" + resource.getPath() + "</field>\n"+
                "<field name=\"type\">page</field> "
        );
        if (!(resource.getPath().endsWith("/jcr:content"))) {
            tempUrl = resource.getPath() + "/jcr:content";
            log.info("tempurl is" + tempUrl);
            Resource tempResource = resourceResolver.getResource(tempUrl);
            log.info("temp res is" + tempResource);
            pageValueAap = tempResource.adaptTo(ValueMap.class);
            log.info("page value map is" + pageValueAap);
            while (iterator.hasNext()) {
                String type=iterator.next().toString();
                if ( pageValueAap.get(pagefieldMap.get(type)) instanceof Object[]){
                    log.info("inside if val");
                    Object[] arr= (Object[]) pageValueAap.get(pagefieldMap.get(type));
                    for(Object value:arr){
                        log.info("Value is"+value);
                        xmlData=xmlData.append("<field name=\"").append(type)
                                .append("\">").append(value.toString()).append("</field>\n");
                    }
                }
                else{
                    String value= (String)pageValueAap.get(pagefieldMap.get(type));
                    log.info("Value is"+value);
                    xmlData=xmlData.append("<field name=\"").append(type)
                            .append("\">").append(value).append("</field>\n");
                }


            }

            log.info("xmldata for page so far is"+xmlData);
            xmlData=getXMLDataForComp(xmlData, compFieldMap, tempResource, resourceResolver);
        }

        xmlData.append("</doc>\n").append("</add>");

        log.info("Xml generated is" + xmlData);
        return xmlData.toString();
    }


    private StringBuffer getXMLDataForComp(StringBuffer xmlData,HashMap compFieldMap,Resource tempResource,ResourceResolver resourceResolverr){

        Iterator<Resource> compResourceIterator = tempResource.listChildren();
        Set keys=compFieldMap.keySet();
        Iterator compFieldIterator=keys.iterator();
        while (compResourceIterator.hasNext()) {
            Resource res = compResourceIterator.next();
            ValueMap compValueMap = res.adaptTo(ValueMap.class);
            while (compFieldIterator.hasNext()){
                String resourceType=compFieldIterator.next().toString();
                if (res.getResourceType().equalsIgnoreCase(resourceType)){

                    HashMap fieldMap= (HashMap) compFieldMap.get(resourceType);
                    Set fieldSet=fieldMap.keySet();
                    Iterator fieldIterator=fieldSet.iterator();
                    while (fieldIterator.hasNext()){
                        String type=fieldIterator.next().toString();
                        if ( compValueMap.get(fieldMap.get(type)) instanceof Object[]){
                            Object[] arr= (Object[]) compValueMap.get(fieldMap.get(type));
                            for(Object value:arr){
                                if (value!=null){
                                xmlData=xmlData.append("<field name=\"").append(type)
                                        .append("\">").append(value.toString()).append("</field>\n");
                                }
                            }
                        }
                        else{
                            String value= (String)compValueMap.get(fieldMap.get(type));
                            if (value!=null){
                            xmlData=xmlData.append("<field name=\"").append(type)
                                    .append("\">").append(value).append("</field>\n");
                            }
                        }
                    }
                }
                else if(res.getResourceType().equalsIgnoreCase(PARSYS)){
                    Iterator<Resource> parCompIterator=res.listChildren();
                    while (parCompIterator.hasNext()){
                        Resource parCompResource=parCompIterator.next();
                        ValueMap parCompValueMap=parCompResource.adaptTo(ValueMap.class);
                        if (parCompResource.getResourceType().equalsIgnoreCase(resourceType)){

                            HashMap fieldMap= (HashMap) compFieldMap.get(resourceType);
                            Set fieldSet=fieldMap.keySet();
                            Iterator fieldIterator=fieldSet.iterator();
                            while (fieldIterator.hasNext()){
                                String type=fieldIterator.next().toString();
                                if ( parCompValueMap.get(fieldMap.get(type)) instanceof Object[]){
                                        Object[] arr= (Object[]) parCompValueMap.get(fieldMap.get(type));
                                    for(Object value:arr){
                                        if (value!=null){
                                            xmlData=xmlData.append("<field name=\"").append(type)
                                                    .append("\">").append(value.toString()).append("</field>\n");
                                        }
                                    }
                                }
                                else{
                                    String value= (String)parCompValueMap.get(fieldMap.get(type));
                                    if (value!=null){
                                        xmlData=xmlData.append("<field name=\"").append(type)
                                                .append("\">").append(value).append("</field>\n");
                                    }
                                }
                            }
                        }
                    }

                }
            }

        }

        return xmlData;
    }
}
