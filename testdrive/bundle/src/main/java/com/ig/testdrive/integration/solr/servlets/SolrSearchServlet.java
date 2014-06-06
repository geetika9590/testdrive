package com.ig.testdrive.integration.solr.servlets;


import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.PrintWriter;
import java.util.*;

/**
 * This is the search servlet which prepares the solr query,
 * hits the solr server and fetches the response.
 */
@Component(label = "Solr Search Servlet", description = "This servlet queries the Solr Server",
        enabled = true, immediate = true, metatype = true)
@Service(Servlet.class)
@Properties({
        @Property(name = "service.description", value = "Solr Descriptors Servlet"),
        @Property(name = "service.vendor", value = "Intelligrape"),
        @Property(name = "sling.servlet.paths", value = "/bin/service/solrsearch.html", propertyPrivate = true)
})
public class SolrSearchServlet extends SlingSafeMethodsServlet {

    /**
     * Log variable for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SolrSearchServlet.class);

    /**
     * This is a constant field to reference the SEARCH_URL property.
     */
    private static final String PROP_SEARCH_ENGINE_URL = "search.engine.url";

    /**
     * It is a configuration property which holds the solr server url.
     */
    @Property(name = PROP_SEARCH_ENGINE_URL, label = "Search Engine URL", description = "Enter the search engine url excluding the collection/core name",
            value = "http://localhost:8983/solr/", propertyPrivate = false)
    private String SEARCH_URL;

    /**
     * It is a constant field used for getting the tag title.
     */
    private static final String TAG_KEY = "Tags";

    /**
     * It is a constant field that contains the path for saved tags.
     */
    private static final String TAG_PATH = "/etc/tags";

    /**
     * Boolean variable which if true indicates
     * whether faceting is enabled or not.
     */
    private boolean isFacet;

    /**
     * This holds the reference of ResourceResolver.
     */
    private ResourceResolver resourceResolver;

    /**
     * An array which holds the solr fields that
     * will be returned in solr response.
     */
    private String[] queryFields;

    /**
     * Activate method for this component.
     *
     * @param componentContext
     */
    @Activate
    protected void activate(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties();
        SEARCH_URL = properties.get(PROP_SEARCH_ENGINE_URL).toString();
        LOG.debug("Configured SolrSearchServlet url, = {}", SEARCH_URL);
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
     * doGet() method is called for a GET request.
     *
     * @param request
     * @param response
     */
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {

        String query = request.getParameter("query");
        LOG.debug("Query parameter received from request is {}", query);
        if (query.equals("")) {
            query = "*:*";
        }
        String searchIn=request.getParameter("searchIn");
        if(query.equals("*:*")){
            searchIn="id:/.*"+searchIn+".*/";
            query=query+"\n"+searchIn;
        }
        else{
            query="id:/.*"+searchIn+".*"+query+".*/";
        }
        LOG.debug("query string is"+query);
        resourceResolver = request.getResourceResolver();
        LOG.debug("Resource resolver got from request is" + resourceResolver);
        String core = request.getParameter("core");
        String maxRows = request.getParameter("maxRows");
        isFacet = Boolean.parseBoolean(request.getParameter("isFacet"));
        String facetFields = request.getParameter("facetFields");
        String queryFieldString = request.getParameter("queryFields");
        String url = SEARCH_URL + core;
        LOG.debug("search url is" + url);
        LOG.debug("request parameter received from request is {}" + core + " " + maxRows + " " + isFacet + " " + facetFields);
        try {
            PrintWriter out = response.getWriter();
            String[] fields = null;

            if (isFacet && facetFields != null) {
                fields = facetFields.split(",");
                LOG.debug("facet fields are {}", fields);
            }
            if (queryFieldString != null) {
                queryFields = queryFieldString.split(",");
                LOG.debug("facet fields are {}", queryFields);
            }
            JSONObject jsonList = getSolrResponseAsJson(url, maxRows, core, query, fields);
            LOG.debug("Response received from Solr is {}", jsonList);
            out.print(jsonList);
        } catch (Exception e) {
            LOG.debug("Error during quering Data =" + e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
            LOG.debug("Exiting finally from Solr Search");
        }
    }

    /**
     * It prepares the query,hits the server
     * and fetches the response.
     *
     * @param url
     * @param maxRows
     * @param core
     * @param query
     * @param facetFields
     * @return jsonList.
     * @throws Exception
     */
    private JSONObject getSolrResponseAsJson(String url, String maxRows, String core, String query, String[] facetFields) throws Exception {

        JSONObject jsonList;

        HttpSolrServer solr = new HttpSolrServer(url);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);


        solrQuery.setFields(this.queryFields);
        if (isFacet) {
            solrQuery.setFacet(true);
            solrQuery.setFacetMinCount(1);
            solrQuery.setRows(new Integer(100));
            solrQuery.setStart(new Integer(0));
            for (String facetField : facetFields) {
                if (facetField != null || facetField.equalsIgnoreCase(""))
                    solrQuery.addFacetField(facetField);
            }
        }
        solrQuery.setStart(0);
        solrQuery.setRows(Integer.parseInt(maxRows));
        LOG.debug("Solr query is" + solrQuery);
        QueryResponse qp = solr.query(solrQuery);
        LOG.debug("time taken for query is"+qp.getElapsedTime()) ;
        LOG.debug("=====Response received from Solr is {}", qp);
        jsonList = parseResponse(qp);
        LOG.debug("=====JSONObject received after parsing document is {}", jsonList);
        return jsonList;
    }

    /**
     * This method parses the response and returns a JSONObject.
     *
     * @param qp
     * @return jsonResponse
     * @throws Exception
     */
    private JSONObject parseResponse(QueryResponse qp) throws Exception {
        SolrDocumentList solrDocumentList = qp.getResults();
        JSONObject jsonObject;
        JSONArray jsonDocArray = new JSONArray();
        JSONObject jsonResponse = new JSONObject();
        String key = "";
        for (int i = 0; i < solrDocumentList.size(); ++i) {
            jsonObject = new JSONObject();
            String value = "";
            boolean isAllowed = false;
            for (Map.Entry<String, Object> stringObjectEntry : solrDocumentList.get(i)) {
                key = stringObjectEntry.getKey();
                if (key.equalsIgnoreCase("id")) {
                    value = stringObjectEntry.getValue().toString();
                    Resource resource = resourceResolver.getResource(value);
                    if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {
                        isAllowed = true;
                    }
                }
                if (isAllowed) {
                    jsonObject.put(key, stringObjectEntry.getValue());
                }
            }
            if (isAllowed) {
                LOG.debug("Added object in doc array" + jsonObject);
                jsonDocArray.put(jsonObject);
            }
        }
        LOG.debug("JSON Doc object is" + jsonDocArray);
        jsonResponse.put("solrDocs", jsonDocArray);
        jsonResponse.put("solrResponseTime",qp.getQTime());
        if (isFacet) {
            jsonResponse.put("solrFacets", parseFacetFields(qp.getFacetFields()));
        }
        return jsonResponse;
    }

    /**
     * This method parses the facet fields.
     *
     * @param facetFields
     * @return jsonFacetResponse
     * @throws JSONException
     */
    private JSONObject parseFacetFields(List<FacetField> facetFields) throws JSONException {

        Iterator<FacetField> facetFieldIterator = facetFields.iterator();

        JSONObject jsonFacetResponse = new JSONObject();
        HashMap<String, Object> solrFacetFieldMap = new HashMap<String, Object>();
        String key = "";
        while (facetFieldIterator.hasNext()) {
            FacetField facetField = facetFieldIterator.next();
            LOG.debug("facet field" + facetField + " " + facetField.getName() + "values " + facetField.getValueCount());
            if (facetField.getValueCount() != 0) {
                List<FacetField.Count> facetFieldCounts = facetField.getValues();
                Iterator<FacetField.Count> facetFieldCountIterator = facetFieldCounts.iterator();
                JSONObject jsonObject = new JSONObject();

                while (facetFieldCountIterator.hasNext()) {
                    FacetField.Count count = facetFieldCountIterator.next();
                    LOG.debug("Count nameis" + count.getName() + "(" + count.getCount() + ")");
                    if (facetField.getName().endsWith(TAG_KEY)) {
                        String tagTitle = getTagTitle(count.getName());
                        if (tagTitle != null && !tagTitle.equalsIgnoreCase("")) {
                            JSONObject titleObject = new JSONObject();
                            titleObject.put(tagTitle, String.valueOf(count.getCount()));
                            jsonObject.put(count.getName(), titleObject);
                        } else {
                            continue;
                        }
                    } else {
                        jsonObject.put(count.getName(), String.valueOf(count.getCount()));
                    }
                    jsonFacetResponse.put(facetField.getName(), jsonObject);
                }
            }
        }
        LOG.debug("Facet jsonObject is" + jsonFacetResponse);
        return jsonFacetResponse;
    }

    /**
     * This method traverses the TAG_PATH and returns the title of a tag.
     *
     * @param tagKey
     * @return
     */
    private String getTagTitle(String tagKey) {
        String title = "";
        String resPath = "";
        if (tagKey.contains(":") && tagKey.contains("/")) {
            String node[] = tagKey.split(":");
            String[] prop = node[1].split("/");
            resPath = TAG_PATH + "/" + node[0] + "/" + prop[0] + "/" + prop[1];
            LOG.debug("res path is" + resPath);
        } else if (tagKey.contains(":")) {
            String node[] = tagKey.split(":");
            resPath = TAG_PATH + "/" + node[0] + "/" + node[1];
            LOG.debug("res path is" + resPath);
        } else {
            resPath = TAG_PATH + "/" + tagKey;
            LOG.debug("res path is" + resPath);
        }

        Resource resource = resourceResolver.getResource(resPath);
        if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {
            ValueMap valueMap = resource.adaptTo(ValueMap.class);
            title = (String) valueMap.get("jcr:title");
        }

        LOG.debug("title fetched is" + title);
        return title;
    }
}
