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


@Component(label = "Solr search servlet", enabled = true, immediate = true, metatype = true)
@Service(Servlet.class)
@Properties({
        @Property(name = "service.description", value = "Solr Descriptors Servlet"),
        @Property(name = "service.vendor", value = "Intelligrape"),
        @Property(name="sling.servlet.paths", value = "/bin/service/solrsearch.html", propertyPrivate = true)
})
public class SolrSearch extends SlingSafeMethodsServlet {


    private static final Logger log = LoggerFactory.getLogger(SolrSearch.class);

    private static final String PROP_SEARCH_ENGINE_URL = "search.engine.url";

    private static final String PROP_QUERY_SEARCH_FIELDS = "search.query.fields";

    private static final String TAG_KEY = "Tags";

    private static final String TAG_PATH="/etc/tags";

    @Property(name = PROP_SEARCH_ENGINE_URL,label = "Search Engine URL",description = "Enter the search engine url excluding the collection/core name",
    value = "http://localhost:8983/solr/", propertyPrivate = false)
    private String SEARCH_URL;

    @Property(name = PROP_QUERY_SEARCH_FIELDS, label = "Query earch Fields", description = "Enter the query search fields",
            value = {"id","title", "description", "format","text_data"}, propertyPrivate = false, cardinality = Integer.MAX_VALUE)
    private String QUERY_SEARCH_FIELDS;

    private boolean isFacet;

    private ResourceResolver resourceResolver;

    private String[] queryFields;

    @Activate
    protected void activate(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties();
        SEARCH_URL = properties.get(PROP_SEARCH_ENGINE_URL).toString();
        queryFields= (String[]) properties.get(PROP_QUERY_SEARCH_FIELDS);

        log.info("Faceting is" + isFacet);
        log.info("Configured SolrSearch url, = {}", SEARCH_URL);
        log.info("Query search fields are, = {}", queryFields);
    }

    @Modified
    protected  void modified(ComponentContext componentContext){
        log.info("Values have been modified");
        activate(componentContext);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {

        String query = request.getParameter("query");
        log.info("Query parameter received from request is {}",query);
        if (query.equals("")){
            query = "*:*";
        }
        resourceResolver=request.getResourceResolver();
        log.info("Resource resolver got from request is"+resourceResolver);
        String core = request.getParameter("core");
        String maxRows = request.getParameter("maxRows");
        isFacet=Boolean.parseBoolean(request.getParameter("isFacet"));
        String facetFields=request.getParameter("facetFields");
        String url = SEARCH_URL + core;
        log.info("search url is"+url);
        log.info("request parameter received from request is {}"+core+" "+maxRows+" "+isFacet+" "+facetFields);
        try {
            PrintWriter out = response.getWriter();
            String[] fields = null;
            if (isFacet && facetFields != null) {
                fields = facetFields.split(",");
                log.info("facet fields are {}", fields);
            }
            JSONObject jsonList = getSolrResponseAsJson(url, maxRows, core, query, fields);
            log.info("Response received from Solr is {}", jsonList);
            out.print(jsonList);
        } catch (Exception e) {
            log.info("Error during quering Data =" + e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
            log.info("Exiting finally from Solr Search");
        }

    }

    private JSONObject getSolrResponseAsJson(String url, String maxRows, String core, String query,String[] facetFields) throws Exception {

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
            log.info("Solr query is" + solrQuery);
            QueryResponse qp = solr.query(solrQuery);
            log.info("=====Response received from Solr is {}", qp);
            jsonList = parseResponse(qp);
            log.info("=====JSONObject received after parsing document is {}", jsonList);
        return jsonList;
    }


    private JSONObject parseResponse(QueryResponse qp) throws Exception {
        SolrDocumentList solrDocumentList = qp.getResults();
        JSONObject jsonObject ;
        JSONArray jsonDocArray = new JSONArray();
        JSONObject jsonResponse= new JSONObject();
        String key = "";
        for (int i = 0; i < solrDocumentList.size(); ++i) {
            jsonObject= new JSONObject();
            String value="";
            boolean isAllowed=false;
            for (Map.Entry<String, Object> stringObjectEntry : solrDocumentList.get(i)) {
                key = stringObjectEntry.getKey();
                if (key.equalsIgnoreCase("id")){
                    value= stringObjectEntry.getValue().toString();
                    Resource resource=resourceResolver.getResource(value);
                    if (resource !=null && !ResourceUtil.isNonExistingResource(resource)){
                       isAllowed=true;
                    }
                }
                if (isAllowed){
                    jsonObject.put(key,stringObjectEntry.getValue());
                }
            }
            if (isAllowed)  {
                log.info("Added object in doc array"+jsonObject);
            jsonDocArray.put(jsonObject);
            }
        }
        log.info("JSON Doc object is"+jsonDocArray);
        jsonResponse.put("solrDocs", jsonDocArray);
        if (isFacet){
            jsonResponse.put("solrFacets", parseFacetFields(qp.getFacetFields()));
        }
        return jsonResponse;
    }


    private JSONObject parseFacetFields(List<FacetField> facetFields) throws JSONException {

        Iterator<FacetField> facetFieldIterator = facetFields.iterator();

        JSONObject jsonFacetResponse=new JSONObject();
        HashMap<String, Object> solrFacetFieldMap = new HashMap<String, Object>();
        String key = "";
        while(facetFieldIterator.hasNext()) {
            FacetField facetField = facetFieldIterator.next();
            log.info("facet field"+facetField+" "+facetField.getName()+"values "+facetField.getValueCount());
                if (facetField.getValueCount()!=0){
                    List<FacetField.Count> facetFieldCounts = facetField.getValues();
                    Iterator<FacetField.Count> facetFieldCountIterator = facetFieldCounts.iterator();
                    JSONObject jsonObject=new JSONObject();

                    while(facetFieldCountIterator.hasNext()) {
                        FacetField.Count count = facetFieldCountIterator.next();
                        log.info("Count nameis" + count.getName() + "(" + count.getCount() + ")");
                        if (facetField.getName().endsWith(TAG_KEY))  {
                            String tagTitle=getTagTitle(count.getName());
                            if (tagTitle != null && !tagTitle.equalsIgnoreCase("")){
                                JSONObject titleObject=new JSONObject();
                                titleObject.put(tagTitle,String.valueOf(count.getCount())) ;
                                jsonObject.put(count.getName(),titleObject );
                            } else {
                                continue;
                            }
                        }
                        else{
                            jsonObject.put(count.getName(), String.valueOf(count.getCount()));
                        }
                        jsonFacetResponse.put(facetField.getName(),jsonObject);
                    }
                }
        }
        log.info("Facet jsonObject is"+jsonFacetResponse);
        return jsonFacetResponse;
    }

    private String getTagTitle(String tagKey)  {
        String title="";
        String resPath="";
        if (tagKey.contains(":") && tagKey.contains("/")){
            String node[]=tagKey.split(":");
            String[] prop=node[1].split("/");
            resPath=TAG_PATH+"/"+node[0]+"/"+prop[0]+"/"+prop[1];
            log.info("res path is"+resPath);
        }
        else if (tagKey.contains(":")){
            String node[]=tagKey.split(":");
            resPath=TAG_PATH+"/"+node[0]+"/"+node[1];
            log.info("res path is"+resPath);
        }
        else {
            resPath=TAG_PATH+"/"+tagKey;
            log.info("res path is"+resPath);
        }

         Resource resource=resourceResolver.getResource(resPath);
         if (resource !=null && !ResourceUtil.isNonExistingResource(resource)){
             ValueMap valueMap=resource.adaptTo(ValueMap.class);
             title=(String)valueMap.get("jcr:title");
         }

        log.info("title fetched is"+title);
        return title;
    }
}
