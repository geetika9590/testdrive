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
    private int bufferSize;
    private static final String TAG_KEY = "Tags";

    private ResourceResolver resourceResolver;

    private PageContext pageContext;
    private Map<String, Object> hitMap;
    private Map<String, List<Facet>> solrFacetFieldMap = new HashMap<String, List<Facet>>();
    private int resultsPerPage;
    private boolean isFacet;


    /**
     * The method which will be executed when the
     * corresponding tag will be encountered in a JSP.
     * @throws javax.servlet.jsp.JspException
     */
    @Override
    public void doTag() throws JspException {

        this.pageContext = (PageContext) getJspContext();

        ValueMap properties = (ValueMap) pageContext.getAttribute("properties");
        resourceResolver = (ResourceResolver) pageContext.getAttribute("resourceResolver");
        Resource resource = (Resource) pageContext.getAttribute("resource");
        HashMap<String,String> queryParams=getQueryParams();
        String query=queryParams.get("query");
        if (StringUtils.isNotBlank(query)) {
            String searchIn = properties.get("searchIn", "/content");
            searchIn = searchIn.replace("/", "\\/");
            query = "id:/.*" + searchIn + ".*/" + " AND " + query;
            LOG.debug("query string is" + query);
            try {
                getSolrResponseAsJson(resource, query,queryParams.get("page"));
            } catch (Exception e) {
                LOG.debug("Error during quering Data =" + e);
            }

        }
    }

    /**
     * This method gets the queryParameter and puts them in a HashMap.
     * @return queryParams
     */
     private HashMap<String,String> getQueryParams(){
         HashMap<String,String> queryParams=new HashMap<String, String>();
         SlingScriptHelper sling = (SlingScriptHelper) pageContext.getAttribute("sling");
         SlingHttpServletRequest request = (SlingHttpServletRequest) pageContext.getAttribute("slingRequest");
         XSSAPI xssAPI = (XSSAPI) pageContext.getAttribute("xssAPI");

         SolrFieldMap solrFieldMap = sling.getService(SolrFieldMap.class);
         SEARCH_URL = solrFieldMap.getSearchURL();
         bufferSize=solrFieldMap.getBufferSize();

         queryParams.put("page",request.getParameter("page"));

         String query = request.getParameter("q");
         final String escapedQuery = xssAPI.encodeForHTML(query);
         final String escapedQueryForAttr = xssAPI.encodeForHTMLAttr(query);
         pageContext.setAttribute("escapedQuery", escapedQuery);
         pageContext.setAttribute("escapedQueryForAttr", escapedQueryForAttr);

         queryParams.put("query",query);
         LOG.debug("Query parameter Map received from request is {}", queryParams);

         return queryParams;
     }

    /**
     * It prepares the query,hits the server
     * and fetches the response.
     *
     * @param query
     * @throws Exception
     */
    private void getSolrResponseAsJson(Resource resource, String query,String page) throws Exception {

        ValueMap properties = resource.adaptTo(ValueMap.class);
        String core = properties.get("core", "geometrixx");
        String url = SEARCH_URL + core;
        resultsPerPage =Integer.parseInt(properties.get("maxRows", "20"));
        LOG.debug("result per page is"+resultsPerPage);
        int pageNo=StringUtils.isNotBlank(page)?Integer.parseInt(page):1;
        pageContext.setAttribute("currentPageNo",pageNo);
        LOG.debug("page no is"+pageNo);
        int limit=pageNo*resultsPerPage;
        isFacet = Boolean.parseBoolean(properties.get("isFacet", "false"));
        HttpSolrServer solr = new HttpSolrServer(url);

        SolrQuery solrQuery=prepareQuery(resource,query);
        LOG.debug("Solr query is" + solrQuery);

        QueryResponse qp = solr.query(solrQuery);
        LOG.debug("Total query time = {}", qp.getElapsedTime());

        parseResponse(qp,limit);
    }

    /**
     * This method prepares & returns the solrQuery.
     * @param resource
     * @param query
     * @return SolrQuery
     */
    private SolrQuery prepareQuery(Resource resource, String query){
        ValueMap properties = resource.adaptTo(ValueMap.class);

        String[] facetFields = (String[]) properties.get("facetFields");
        String[] queryFields = (String[]) properties.get("queryFields");
        boolean isFacet = Boolean.parseBoolean(properties.get("isFacet", "false"));
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setFields(queryFields);

        if (isFacet) {
            solrQuery.setFacet(true);
            solrQuery.setFacetMinCount(1);
            for (String facetField : facetFields) {
                if (facetField != null || facetField.equalsIgnoreCase(""))
                    solrQuery.addFacetField(facetField);
            }
        }
        solrQuery.setStart(0);
        solrQuery.setRows(bufferSize);
        return solrQuery;
    }

    /**
     * This method parses the response and returns a JSONObject.
     *
     * @param qp
     * @return jsonResponse
     * @throws Exception
     */
    private void parseResponse(QueryResponse qp,int limit) throws Exception {
        SolrDocumentList solrDocumentList = qp.getResults();
        List<Map<String, Object>> documentList = new ArrayList<Map<String, Object>>();
        int permittedRecordCounter=1;
        int ignoreLimit=limit-resultsPerPage;
        boolean counterReached=false;
        LOG.debug("ignore limt "+ignoreLimit+" & limit "+limit);
        for (int i = 0; i < solrDocumentList.size(); ++i) {
            LOG.debug("Solr doc list" + solrDocumentList);
            SolrDocument solrDocList = solrDocumentList.get(i);
            boolean isAllowed=checkResultsForPermissions(solrDocList);
            if (isAllowed) {
                if (permittedRecordCounter>ignoreLimit && permittedRecordCounter<=limit && !counterReached){
                    LOG.debug("adding doc"+permittedRecordCounter);
                    documentList.add(hitMap);
                }
                permittedRecordCounter++;
                if(permittedRecordCounter>limit && !counterReached) {
                    counterReached=true;
                    LOG.debug("counter reached"+counterReached);
                }
            }
        }
        pageContext.setAttribute("totalHits",permittedRecordCounter-1);
        pageContext.setAttribute("totalPages",calculatePages(permittedRecordCounter-1));
        pageContext.setAttribute("resultList", documentList);
        pageContext.setAttribute("solrResponseTime", qp.getQTime());

        if (isFacet) {
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
        while (facetFieldIterator.hasNext()) {
            FacetField facetField = facetFieldIterator.next();
            LOG.debug("facet field" + facetField + " " + facetField.getName() + "values " + facetField.getValueCount());
            parseFacet(facetField);
        }
        pageContext.setAttribute("facetFieldMap", solrFacetFieldMap);
    }

    /**
     * This method checks for permissions on the resources
     * and leaves those which are protected.
     * @param solrDocList
     * @return
     */
    private boolean checkResultsForPermissions(SolrDocument solrDocList){
        hitMap = new HashMap<String, Object>();
        String key = "";
        String value = "";
        boolean isAllowed = false;
        boolean evaluateId = true;
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
        return isAllowed;
    }

    /**
     * This method parses each facet and updates the solrFacetFieldMap.
     * @param facetField
     */
    private void parseFacet(FacetField facetField){

        if (facetField.getValueCount() != 0) {
            List<FacetField.Count> facetFieldCounts = facetField.getValues();
            Iterator<FacetField.Count> facetFieldCountIterator = facetFieldCounts.iterator();
            List<Facet> facetTypeList = new ArrayList<Facet>();

            while (facetFieldCountIterator.hasNext()) {
                FacetField.Count count = facetFieldCountIterator.next();
                Facet facet = getFacetBean(count, facetField.getName());
                facetTypeList.add(facet);
            }
            solrFacetFieldMap.put(facetField.getName(), facetTypeList);
        }
    }

    /**
     * This method parses each facet count and
     * prepares the Facet Bean object.
     * @param count
     * @param facetFieldName
     * @return Facet
     */
    private  Facet getFacetBean( FacetField.Count count,String facetFieldName){
        XSSAPI xssAPI = (XSSAPI) pageContext.getAttribute("xssAPI");
        LOG.debug("Count name received in getFacetType method is " + count.getName() + "(" + count.getCount() + ")");
        String facetTitle = "";
        String facetString = facetFieldName + ":" + "\"" + count.getName() + "\"";
        final String escapedQuery = xssAPI.encodeForHTML(facetString);
        String facetQuery = pageContext.getAttribute("escapedQuery").toString() + "+AND+" + escapedQuery;

        if (facetFieldName.endsWith(TAG_KEY)) {
            TagManager tagManager = ((ResourceResolver) pageContext.getAttribute("resourceResolver")).adaptTo(TagManager.class);
            Tag tag = tagManager.resolve(count.getName());
            facetTitle = StringUtils.isNotBlank(tag.getTitle()) ? tag.getTitle() : tag.getName();
        } else {
            facetTitle = count.getName();
        }
        Facet facet = new Facet(facetFieldName, facetTitle, String.valueOf(count.getCount()), facetQuery);
        return facet;
    }

    /**
     * This method calculate the no of pages to be
     * displayed for the corresponding result.
     * @param size
     * @return
     */
    private long calculatePages(long size){
        long rows=size/resultsPerPage;
        if( size%resultsPerPage >0){
            rows+=1;
        }
        return rows;
    }
}


