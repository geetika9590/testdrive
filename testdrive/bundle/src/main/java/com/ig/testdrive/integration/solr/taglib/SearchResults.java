package com.ig.testdrive.integration.solr.taglib;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchResults extends SimpleTagSupport {
	
	final Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	private static final String SEARCH_URL = "http://localhost:8983/solr/";
	private static final String TAG_KEY = "Tags";
	private static final String TAG_PATH = "/etc/tags";
	
	private ResourceResolver resourceResolver;
	
	@Override
    public void doTag() throws JspException {
		
		final PageContext pageContext = (PageContext) getJspContext();
		
		ValueMap properties = (ValueMap) pageContext.getAttribute("properties");
		SlingHttpServletRequest request = (SlingHttpServletRequest)pageContext.getAttribute("slingRequest");
		SlingHttpServletResponse response = (SlingHttpServletResponse)pageContext.getAttribute("slingResponse");
		resourceResolver = (ResourceResolver)pageContext.getAttribute("resourceResolver");
		Resource resource = (Resource)pageContext.getAttribute("resource");
		
		String query = request.getParameter("q");
        LOG.debug("Query parameter received from request is {}", query);
        if (query.equals("")) {
            query = "*:*";
        }
        String searchIn=properties.get("searchIn","/content");
        if(!query.contains(":")){
            query="id:/.*"+searchIn+".*"+query+".*/";
        }
        LOG.debug("query string is"+query);
                
        try {
            PrintWriter out = response.getWriter();            
            JSONObject jsonList = getSolrResponseAsJson(resource, query);
            LOG.debug("Response received from Solr is {}", jsonList);
            out.print(jsonList);
        } catch (Exception e) {
            LOG.debug("Error during quering Data =" + e);
        }
    }
	
	private JSONObject getSolrResponseAsJson(Resource resource, String query) throws Exception {
		
		ValueMap properties = resource.adaptTo(ValueMap.class);
		
		String core = properties.get("core","geometrixx");
        String maxRows = properties.get("maxRows","20");
        boolean isFacet = Boolean.parseBoolean(properties.get("isFacet","false"));
        String[] facetFields = properties.get("facetFields", String[].class);
        String[] queryFields = properties.get("queryFields", String[].class);
    	String url = SEARCH_URL + core;
    	
    	return getSolrResponseAsJson(url, maxRows, core, query, facetFields, isFacet, queryFields);
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
    private JSONObject getSolrResponseAsJson(String url, String maxRows, String core, String query, String[] facetFields, boolean isFacet, String[] queryFields) throws Exception {

        JSONObject jsonList;

        HttpSolrServer solr = new HttpSolrServer(url);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);


        solrQuery.setFields(queryFields);
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
        jsonList = parseResponse(qp, isFacet);
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
    private JSONObject parseResponse(QueryResponse qp, boolean isFacet) throws Exception {
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
                    LOG.debug("Count name is" + count.getName() + "(" + count.getCount() + ")");
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
