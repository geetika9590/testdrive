package com.ig.testdrive.integration.solr.taglib;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.ig.testdrive.integration.solr.service.SolrFieldMap;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.xss.XSSAPI;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.ig.testdrive.integration.solr.beans.Facet;

public class SearchResults extends SimpleTagSupport {

    final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private String SEARCH_URL;
    private static final String TAG_KEY = "Tags";
    private static final String TAG_PATH = "/etc/tags";

    private ResourceResolver resourceResolver;

    private PageContext pageContext;

    @Override
    public void doTag() throws JspException {

        this.pageContext = (PageContext) getJspContext();

        ValueMap properties = (ValueMap) pageContext.getAttribute("properties");
        SlingScriptHelper sling = (SlingScriptHelper) pageContext.getAttribute("sling");
        SlingHttpServletRequest request = (SlingHttpServletRequest) pageContext.getAttribute("slingRequest");
        SlingHttpServletResponse response = (SlingHttpServletResponse) pageContext.getAttribute("slingResponse");
        resourceResolver = (ResourceResolver) pageContext.getAttribute("resourceResolver");
        Resource resource = (Resource) pageContext.getAttribute("resource");
        XSSAPI xssAPI = (XSSAPI) pageContext.getAttribute("xssAPI");

        SolrFieldMap solrFieldMap = sling.getService(SolrFieldMap.class);

        SEARCH_URL = solrFieldMap.getSearchURL();

        String query = request.getParameter("q");
        final String escapedQuery = xssAPI.encodeForHTML(query);
        final String escapedQueryForAttr = xssAPI.encodeForHTMLAttr(query);

        pageContext.setAttribute("escapedQuery", escapedQuery);
        pageContext.setAttribute("escapedQueryForAttr", escapedQueryForAttr);

        LOG.debug("Query parameter received from request is {}", query);

        if (StringUtils.isNotBlank(query)) {
            String searchIn = properties.get("searchIn", "/content");
            searchIn = searchIn.replace("/", "\\/");
            query = "id:/.*" + searchIn + ".*/" + " AND " + query;
            LOG.debug("query string is" + query);
            try {
                getSolrResponseAsJson(resource, query);
            } catch (Exception e) {
                LOG.debug("Error during quering Data =" + e);
            }

        }
    }


    /**
     * It prepares the query,hits the server
     * and fetches the response.
     *
     * @param query
     * @throws Exception
     */
    private void getSolrResponseAsJson(Resource resource, String query) throws Exception {

        ValueMap properties = resource.adaptTo(ValueMap.class);

        String core = properties.get("core", "geometrixx");
        String maxRows = properties.get("maxRows", "20");
        boolean isFacet = Boolean.parseBoolean(properties.get("isFacet", "false"));
        String[] facetFields = (String[]) properties.get("facetFields");
        String[] queryFields = (String[]) properties.get("queryFields");
        String url = SEARCH_URL + core;

        HttpSolrServer solr = new HttpSolrServer(url);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setFields(queryFields);

        if (isFacet) {
            solrQuery.setFacet(true);
            solrQuery.setFacetMinCount(1);
//            solrQuery.setRows(new Integer(100));
//            solrQuery.setStart(new Integer(0));
            for (String facetField : facetFields) {
                if (facetField != null || facetField.equalsIgnoreCase(""))
                    solrQuery.addFacetField(facetField);
            }
        }
        solrQuery.setStart(0);

        solrQuery.setRows(Integer.parseInt(maxRows));
        LOG.debug("Solr query is" + solrQuery);

        QueryResponse qp = solr.query(solrQuery);
        LOG.debug("Total query time = {}", qp.getElapsedTime());

        parseResponse(qp, isFacet);
    }

    /**
     * This method parses the response and returns a JSONObject.
     *
     * @param qp
     * @return jsonResponse
     * @throws Exception
     */
    private void parseResponse(QueryResponse qp, boolean isFacet) throws Exception {
        SolrDocumentList solrDocumentList = qp.getResults();
        List<Map<String, Object>> documentList = new ArrayList<Map<String, Object>>();

        for (int i = 0; i < solrDocumentList.size(); ++i) {
            LOG.debug("Solr doc list" + solrDocumentList);
            Map<String, Object> hitMap = new HashMap<String, Object>();
            String key = "";
            String value = "";
            boolean isAllowed = false;
            boolean evaluateId = true;
            SolrDocument solrDocList = solrDocumentList.get(i);
            for (Map.Entry<String, Object> stringObjectEntry : solrDocList) {

                if (solrDocList.containsKey("id")) {
                    if (evaluateId) {
                        value = solrDocList.get("id").toString();

                        Resource resource = resourceResolver.getResource(value);
                        if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {
                            isAllowed = true;
                            hitMap.put("id", value);
                        }
                        evaluateId = false;
                    }

                    if (isAllowed) {
                        key = stringObjectEntry.getKey();
                        value = stringObjectEntry.getValue().toString().replace("[", "").replace("]", "");
                        LOG.debug("JSON stringObjectEntry.getValue() = {}", stringObjectEntry.getValue());

                        hitMap.put(key, value);
                    }
                }

            }

            if (isAllowed) {
                documentList.add(hitMap);
            }
        }

        pageContext.setAttribute("resultList", documentList);
        pageContext.setAttribute("solrResponseTime", qp.getQTime());

        if (isFacet) {
            //pageContext.setAttribute("solrFacets", parseFacetFields(qp.getFacetFields()));
            parseFacetFields(qp.getFacetFields());
        }
    }

    /**
     * This method parses the facet fields.
     *
     * @param facetFields
     * @return jsonFacetResponse
     * @throws JSONException
     */
    private void parseFacetFields(List<FacetField> facetFields) throws JSONException {


        Iterator<FacetField> facetFieldIterator = facetFields.iterator();
        Map<String, List<Facet>> solrFacetFieldMap = new HashMap<String, List<Facet>>();
        XSSAPI xssAPI = (XSSAPI) pageContext.getAttribute("xssAPI");


        while (facetFieldIterator.hasNext()) {
            FacetField facetField = facetFieldIterator.next();
            LOG.debug("facet field" + facetField + " " + facetField.getName() + "values " + facetField.getValueCount());
            if (facetField.getValueCount() != 0) {
                List<FacetField.Count> facetFieldCounts = facetField.getValues();
                Iterator<FacetField.Count> facetFieldCountIterator = facetFieldCounts.iterator();

                List<Facet> facetTypeList = new ArrayList<Facet>();

                while (facetFieldCountIterator.hasNext()) {
                    FacetField.Count count = facetFieldCountIterator.next();
                    LOG.debug("Count name is" + count.getName() + "(" + count.getCount() + ")");

                    String facetTitle = "";
                    String facetQuery = pageContext.getAttribute("escapedQuery").toString() + "+AND+" + facetField.getName();

                    if (facetField.getName().endsWith(TAG_KEY)) {

                        TagManager tagManager = ((ResourceResolver) pageContext.getAttribute("resourceResolver")).adaptTo(TagManager.class);
                        Tag tag = tagManager.resolve(count.getName());
                        facetTitle = StringUtils.isNotBlank(tag.getTitle()) ? tag.getTitle() : tag.getName();
                        String tagString = facetField.getName() + ":" + "\"" + count.getName() + "\"";
                        final String escapedQuery = xssAPI.encodeForHTML(tagString);
                        facetQuery = pageContext.getAttribute("escapedQuery").toString() + "+AND+" + escapedQuery;

                    } else {
                        facetTitle = count.getName();
                    }

                    Facet facet = new Facet(facetField.getName(), facetTitle, String.valueOf(count.getCount()), facetQuery);
                    facetTypeList.add(facet);
                }

                solrFacetFieldMap.put(facetField.getName(), facetTypeList);
            }
        }

        pageContext.setAttribute("facetFieldMap", solrFacetFieldMap);
        LOG.debug("facet field map is" + solrFacetFieldMap);
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
