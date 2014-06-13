package com.ig.testdrive.integration.solr.servlets;

import com.ig.testdrive.integration.solr.beans.SolrFieldMappingBean;
import com.ig.testdrive.integration.solr.service.SolrFieldMap;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * This is a servlet which is used for parameter extraction of solr fields.
 */
@Component(label = "Solr Parameter Extraction servlet", description = "This servlet extracts the solr fields which are searchable ", enabled = true, immediate = true, metatype = true)
@Service(value = Servlet.class)
@Properties({
        @Property(name = "service.description", value = "This servlet extracts the solr fields which are searchable "),
        @Property(name = "service.vendor", value = "Intelligrape"),
        @Property(name = "sling.servlet.paths", value = "/bin/service/parameters", propertyPrivate = true),
        @Property(name = "sling.servlet.extensions", value = "json", propertyPrivate = true),
        @Property(name = "sling.servlet.selectors", value = "facet", propertyPrivate = true),
        @Property(name = "sling.servlet.methods", value = "GET", propertyPrivate = true)
})
public class SolrParametersServlet extends SlingSafeMethodsServlet {

    /**
     * It holds the reference of SolrFieldMap service.
     */
    @Reference
    SolrFieldMap solrFieldMap;

    /**
     * Log variable for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SolrParametersServlet.class);
    /**
     * It contains the property set for solrFields.
     */
    private HashSet<String> propertySet = new HashSet<String>();

    /**
     * Activate method for this component.
     *
     * @param componentContext
     */
    @Activate
    protected void activate(ComponentContext componentContext) {
        LOG.debug("inside activate method of parameters servlet");
    }

    /**
     * Modified method for this component.
     *
     * @param componentContext
     */
    @Modified
    protected void modified(ComponentContext componentContext) {
        LOG.debug("Values have been modified");
        activate(componentContext);
    }

    /**
     * doGet() method is called for and HTTP GET request.
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        String selectorString = request.getRequestPathInfo().getSelectorString();
        JSONArray jsonArray;
        try {
            if (selectorString.contains("facetfields")) {
                jsonArray = addFacetFields();
            } else {
                jsonArray = addAllFields();
            }
             LOG.debug("JSON Array returned is "+jsonArray);
            if (jsonArray.length() > 0) {
                out.print(jsonArray);
            } else {
                out.print("[]");
            }
        }catch (JSONException e){
            LOG.error("Exception occured while updating Json object " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method parses the fieldmaps and updates the propertySet.
     *
     * @param maps
     */
    public void parseFieldMaps(HashMap[] maps) {
        for (HashMap map : maps) {
            LOG.debug("map received is" + map);
            Set set = map.keySet();
            Iterator iterator = set.iterator();
            while (iterator.hasNext()) {
                String key = iterator.next().toString();
                Object value = map.get(key);
                if (value instanceof ArrayList) {
                    ArrayList fieldList = (ArrayList) map.get(key);
                    Iterator fieldIterator = fieldList.iterator();
                    while (fieldIterator.hasNext()) {
                        SolrFieldMappingBean fieldMappingBean = (SolrFieldMappingBean) fieldIterator.next();
                        this.propertySet.add(fieldMappingBean.getSolrField());
                    }
                } else if (value instanceof String) {
                    this.propertySet.add(value.toString());
                }
            }
        }
    }

    /**
     * This method extracts all configured solr fields types.
     *
     * @return JSONArray of SolrFields
     */
    public JSONArray addAllFields() throws JSONException {

        HashMap[] maps = {solrFieldMap.getPageFieldMap(), solrFieldMap.getCompFieldMap(), solrFieldMap.getAssetFieldMap()};
        parseFieldMaps(maps);
        Iterator iterator = propertySet.iterator();
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        jsonObject.put("text", "id");
        jsonObject.put("value", "id");
        jsonArray.put(jsonObject);
        while (iterator.hasNext()) {
            jsonObject = new JSONObject();
            Object obj = iterator.next();
            if (obj != null) {
                String field = obj.toString();
                if (!field.equalsIgnoreCase("text_data")) {
                    jsonObject.put("text", field);
                    jsonObject.put("value", field);
                    LOG.debug("json obj is" + jsonObject);
                    jsonArray.put(jsonObject);
                }
            }
        }
        jsonObject = new JSONObject();
        jsonObject.put("text", "type");
        jsonObject.put("value", "type");
        jsonArray.put(jsonObject);
        LOG.debug("Solr field set is " + propertySet);
        return jsonArray;
    }

    /**
     * This method extracts all configured facet fields .
     *
     * @return JSONArray of facetFields
     */

    public JSONArray addFacetFields() throws JSONException {
        String[] facetFields = solrFieldMap.getFacetFields();
        JSONObject jsonObject;
        JSONArray jsonArray = new JSONArray();
        for (String s:facetFields){
            jsonObject=new JSONObject();
            jsonObject.put("text",s);
            jsonObject.put("value",s);
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }

}
