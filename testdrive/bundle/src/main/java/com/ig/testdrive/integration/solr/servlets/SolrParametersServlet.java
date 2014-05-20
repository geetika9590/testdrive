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
 * Created with IntelliJ IDEA.
 * User: Geetika
 * Date: 19/5/14
 * Time: 10:32 AM
 */
@Component(label = "Solr Parameter Extraction servlet", description = "This servlet extracts the solr fields which are searchable ", enabled = true, immediate = true, metatype = true)
@Service(value = Servlet.class)
@Properties({
        @Property(name = "service.description", value = "This servlet extracts the solr fields which are searchable "),
        @Property(name = "service.vendor", value = "Intelligrape"),
        @Property(name = "sling.servlet.paths", value = "/bin/service/parameters.json", propertyPrivate = true),
        @Property(name = "sling.servlet.methods", value = "GET", propertyPrivate = true)
})
public class SolrParametersServlet extends SlingSafeMethodsServlet {

    @Reference
    SolrFieldMap solrFieldMap;

    private static final Logger log = LoggerFactory.getLogger(SolrParametersServlet.class);
    private HashSet<String> propertySet = new HashSet<String>();

    @Activate
    protected void activate(ComponentContext componentContext) {
        log.debug("inside activate method of parameters servlet");
    }

    @Modified
    protected void modified(ComponentContext componentContext) {
        log.debug("Values have been modified");
        activate(componentContext);
    }


    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        HashMap[] maps = {solrFieldMap.getPageFieldMap(), solrFieldMap.getCompFieldMap(), solrFieldMap.getAssetFieldMap()};
        addFields(maps);
        Iterator iterator = this.propertySet.iterator();
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            jsonObject.put("text", "id");
            jsonObject.put("value", "id");
            jsonArray.put(jsonObject);
            while (iterator.hasNext()) {
                jsonObject = new JSONObject();
                String field = iterator.next().toString();
                jsonObject.put("text", field);
                jsonObject.put("value", field);
                log.debug("json obj is" + jsonObject);
                jsonArray.put(jsonObject);
            }
            log.debug("Solr field set is " + propertySet);
            if (jsonArray.length() > 0) {
                out.print(jsonArray);
            } else {
                out.print("[]");
            }
        } catch (JSONException e) {
            log.error("Exception occured while updating Json object " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addFields(HashMap[] maps) {
        for (HashMap map : maps) {
            log.debug("map received is" + map);
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
}
