package com.ig.testdrive.integration.solr.servlets;

import com.ig.testdrive.commons.util.CommonMethods;
import com.ig.testdrive.integration.solr.beans.SolrFieldMappingBean;
import com.ig.testdrive.integration.solr.service.SolrFieldMap;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Geetika
 * Date: 19/5/14
 * Time: 10:32 AM
 */
@Component(label = "Solr Page Component servlet", description = "This servlet generates XML for CQ Pages", enabled = true, immediate = true, metatype = true)
@Service(value = Servlet.class)
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

    @Reference
    SolrFieldMap solrFieldMap;

    private static final Logger log = LoggerFactory.getLogger(SolrPageServlet.class);

    private static final String PARSYS = "foundation/components/parsys";

    private static final String IPARSYS = "foundation/components/iparsys";

    private HashMap<String, String> pageFieldMap = new HashMap<String, String>();
    private HashMap<String, ArrayList> compFieldMap = new HashMap<String, ArrayList>();

    @Activate
    protected void activate(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties();
        log.debug("inside activate method of Page servlet");

    }

    @Modified
    protected void modified(ComponentContext componentContext) {
        log.debug("Values have been modified");
        activate(componentContext);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        Resource resource = request.getResource();
        log.debug("resource is " + resource);
        ValueMap valueMap = resource.adaptTo(ValueMap.class);
        log.debug("page & page metadata is" + valueMap);
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = request.getResourceResolver();
            if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {
                pageFieldMap=solrFieldMap.getPageFieldMap();
                compFieldMap=solrFieldMap.getCompFieldMap();
                log.debug("Solr field names and values for page are " + pageFieldMap);
                log.debug("Solr field names and values for components are " + compFieldMap);
                String xmlString = getXMLData(resourceResolver, resource, pageFieldMap, compFieldMap);
                response.getOutputStream().write(xmlString.getBytes());
            }
        } finally {
            log.debug("finally executed");
            if (resourceResolver != null)
                resourceResolver.close();
        }
    }

    private String getXMLData(ResourceResolver resourceResolver, Resource resource, HashMap pagefieldMap, HashMap compFieldMap) {

        String tempUrl = "";
        ValueMap pageValueAap = null;
        StringBuffer xmlData = new StringBuffer("<add>\n" +
                "<doc>\n" + "<field name=\"id\">" + resource.getPath() + "</field>\n" +
                "<field name=\"type\">page</field> "
        );
        if (!(resource.getPath().endsWith("/jcr:content"))) {
            tempUrl = resource.getPath() + "/jcr:content";
            Resource tempResource = resourceResolver.getResource(tempUrl);
            pageValueAap = tempResource.adaptTo(ValueMap.class);
            xmlData = CommonMethods.parseFieldMap(pageValueAap, pagefieldMap, xmlData);

            log.debug("xmldata for page so far is" + xmlData);
            xmlData = getXMLDataForComp(xmlData, compFieldMap, tempResource, resourceResolver);
        }
        xmlData.append("</doc>\n").append("</add>");
        log.debug("Xml generated is" + xmlData);
        return xmlData.toString();
    }


    private StringBuffer getXMLDataForComp(StringBuffer xmlData, HashMap compFieldMap, Resource tempResource, ResourceResolver resourceResolver) {
        Iterator<Resource> compResourceIterator = tempResource.listChildren();
        Set keys = compFieldMap.keySet();
        Iterator compFieldIterator;
        while (compResourceIterator.hasNext()) {
            Resource actualResource = compResourceIterator.next();

            if (actualResource.getResourceType().equalsIgnoreCase(PARSYS) || actualResource.getResourceType().equalsIgnoreCase(IPARSYS)) {
                Iterator<Resource> parCompIterator = actualResource.listChildren();
                while (parCompIterator.hasNext()) {
                    compFieldIterator = keys.iterator();
                    Resource parCompResource = parCompIterator.next();
                    while (compFieldIterator.hasNext()) {
                        String resourceTypeProp = compFieldIterator.next().toString();
                        if (parCompResource.getResourceType().equalsIgnoreCase(resourceTypeProp)) {
                            xmlData = getXMLDataForResourceType(parCompResource, compFieldMap, xmlData, resourceTypeProp, resourceResolver);
                            break;
                        }
                    }
                }
            } else {
                compFieldIterator = keys.iterator();
                while (compFieldIterator.hasNext()) {
                    String resourceTypeProp = compFieldIterator.next().toString();
                    if (actualResource.getResourceType().equalsIgnoreCase(resourceTypeProp)) {
                        xmlData = getXMLDataForResourceType(actualResource, compFieldMap, xmlData, resourceTypeProp, resourceResolver);
                        break;
                    }
                }
            }
        }
        log.debug("Returning from getXmlDataForcomp "+ xmlData);
        return xmlData;
    }

    private StringBuffer getXMLDataForResourceType(Resource resource, HashMap compFieldMap, StringBuffer xmlData, String resourceType, ResourceResolver resourceResolver) {


        ValueMap compValueMap = resource.adaptTo(ValueMap.class);
        log.debug("Component value map " + compValueMap);
        ArrayList fieldsInfo = (ArrayList)compFieldMap.get(resourceType);

        Iterator iterator=fieldsInfo.iterator();
        while (iterator.hasNext()){
            SolrFieldMappingBean solrFieldMappingBean = (SolrFieldMappingBean) iterator.next();
            if (solrFieldMappingBean.isReference()) {
                String referenceResourceType = (String)compValueMap.get(solrFieldMappingBean.getReferenceField());
                log.debug("Reference paragraph component is referring to " + referenceResourceType);
                Resource referenceResource = resourceResolver.getResource(referenceResourceType);
                compValueMap = referenceResource.adaptTo(ValueMap.class);
                fieldsInfo = (ArrayList) compFieldMap.get(referenceResource.getResourceType());
                log.debug("reference resource value map is " + compValueMap);
            }
            log.debug("Field map" + fieldsInfo);
            xmlData = CommonMethods.parseFieldBean(compValueMap, fieldsInfo, xmlData);
        }
        log.debug("Returning from getXMLDataForResourceType" + xmlData);
        return xmlData;

    }
}
