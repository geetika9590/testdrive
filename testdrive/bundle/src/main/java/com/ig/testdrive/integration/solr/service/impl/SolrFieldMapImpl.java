package com.ig.testdrive.integration.solr.service.impl;

import com.ig.testdrive.integration.solr.beans.SolrFieldMappingBean;
import com.ig.testdrive.integration.solr.service.SolrFieldMap;
import org.apache.felix.scr.annotations.*;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;

/**
 * This class defines the methods for fetching the field mappings.
 */
@Component(label = "Solr Field Map servlet", description = "This servlet contains the mapping of solr and CQ fields", enabled = true, immediate = true, metatype = true)
@Service(SolrFieldMap.class)
public class SolrFieldMapImpl implements SolrFieldMap {

    /**
     * Log variable for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SolrFieldMap.class);

    /**
     * Variable for fetching the page values.
     */
    private static final String PAGE_METADATA_VALUES_TO_BE_INDEXED = "page.indexed.values";

    /**
     * Variable for fetching the asset values.
     */
    private static final String ASSET_METADATA_VALUES_TO_BE_INDEXED = "asset.indexed.values";

    /**
     * Variable for fetching the component values.
     */
    private static final String COMP_METADATA_VALUES_TO_BE_INDEXED = "comp.indexed.values";

    /**
     * This is a configuration property for
     * configuring the properties for a page that can be indexed.
     */
    @Property(name = PAGE_METADATA_VALUES_TO_BE_INDEXED, label = "Page Indexed values", description = "Enter the solr and property mapping for pages",
            value = {"titleText=jcr:title", "description=jcr:description", "pageTags=cq:tags"}, propertyPrivate = false, cardinality = Integer.MAX_VALUE)
    private String PAGE_INDEX_VALUES;

    /**
     * This is a configuration property for
     * configuring the properties for a component that can be indexed.
     */
    @Property(name = COMP_METADATA_VALUES_TO_BE_INDEXED, label = "Component Indexed values", description = "Enter the component resource types, their property mapping separated by @",
            value = {"foundation/components/title@title=jcr:title", "foundation/components/textimage@title=jcr:title<>text_data=fileReference",
                    "foundation/components/image@text_data=fileReference<>title=jcr:title", "foundation/components/search@title=jcr:title<>text_data=searchIn"},
            propertyPrivate = false, cardinality = Integer.MAX_VALUE)
    private String COMP_INDEX_VALUES;

    /**
     * This is a configuration property for
     * configuring the properties for an asset that can be indexed.
     */
    @Property(name = ASSET_METADATA_VALUES_TO_BE_INDEXED, label = "DAM Asset Metadata Indexed values",
            description = "Enter the fields to be indexed for DAM assets", value = {"titleText=dc:title", "description=dc:description",
            "format=dc:format", "damTags=cq:tags"}, propertyPrivate = false, cardinality = Integer.MAX_VALUE)
    private String INDEX_VALUES;

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

    private String searchURL;
    /**
     * This is a map that contains the page field mappings.
     */
    private HashMap<String, String> pageFieldMap = new HashMap<String, String>();

    /**
     * This is a map that contains the component field mappings.
     */
    private HashMap<String, ArrayList> compFieldMap = new HashMap<String, ArrayList>();

    /**
     * This is a map that contains the asset field mappings.
     */
    private HashMap<String, String> assetFieldMap = new HashMap<String, String>();
    private String[] pageIndexedVal, compIndexedVal, damIndexedVal;

    /**
     * This variable contains the separator which
     * separates a resource type from its mappings.
     */
    private static final String RESOURCE_SEPARATOR = "@";

    /**
     * This variable contains the separator which
     * separates each property mapping from each other.
     */
    private static final String PROPERTIES_SEPARATOR = "<>";
    /**
     * This variable contains the separator which
     * separates an individual mapping i.e. speartor for cq field and solr field.
     */
    private static final String PROPERTY_SEPARATOR = "=";
    /**
     * This variable works as an start indicator for reference component.
     * The reference path property is present between these separators.
     */
    private static final String REFERENCE_START_SEPARATOR = "{";
    /**
     * This variable works as an end indicator for reference component.
     * The reference path property is present between these separators.
     */
    private static final String REFERENCE_END_SEPARATOR = "}";
    /**
     * Activate method for this class
     *
     * @param componentContext
     */
    @Activate
    protected void activate(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties();
        LOG.debug("inside activate method of Page servlet");
        pageIndexedVal = (String[]) properties.get(PAGE_METADATA_VALUES_TO_BE_INDEXED);
        compIndexedVal = (String[]) properties.get(COMP_METADATA_VALUES_TO_BE_INDEXED);
        damIndexedVal = (String[]) properties.get(ASSET_METADATA_VALUES_TO_BE_INDEXED);
        searchURL=(String) properties.get(PROP_SEARCH_ENGINE_URL);
        parseIndexedValues(pageIndexedVal, damIndexedVal, compIndexedVal);
    }




    /**
     * Modified method for this class
     *
     * @param componentContext
     */
    @Modified
    protected void modified(ComponentContext componentContext) {
        LOG.debug("Values have been modified");
        activate(componentContext);
    }

    /**
     * Getter method for pageFieldMap.
     *
     * @return It returns the pageFieldMap.
     */
    @Override
    public HashMap<String, String> getPageFieldMap() {
        return pageFieldMap;
    }

    /**
     * Getter method for Search URL.
     *
     * @return It returns the searchURL.
     */
    @Override
    public String getSearchURL() {
        return searchURL;
    }

    /**
     * Getter method for compFieldMap.
     *
     * @return It returns the compFieldMap.
     */
    @Override
    public HashMap<String, ArrayList> getCompFieldMap() {
        return compFieldMap;
    }

    /**
     * Getter method for assetFieldMap.
     *
     * @return It returns the assetFieldMap.
     */
    @Override
    public HashMap<String, String> getAssetFieldMap() {
        return assetFieldMap;
    }

    /**
     * It is a wrapper method for calling the individual methods
     * for parsing the indexed values.
     *
     * @param pageIndexedVal
     * @param damIndexedVal
     * @param compIndexedVal
     */
    private void parseIndexedValues(String[] pageIndexedVal, String[] damIndexedVal, String[] compIndexedVal) {
        parsePageFieldMap(pageIndexedVal);
        parseAssetFieldMap(damIndexedVal);
        parseCompFieldMap(compIndexedVal);
    }

    /**
     * It parses the assetFieldMap
     *
     * @param damIndexedVal
     */
    private void parseAssetFieldMap(String[] damIndexedVal) {
        for (String s : damIndexedVal) {
            LOG.debug("String is" + s);
            String[] parts = s.split("=");
            assetFieldMap.put(parts[1], parts[0]);
        }
        LOG.debug("Solr field names and values for assets are " + assetFieldMap);
    }

    /**
     * It parses the comp field map.
     *
     * @param compIndexedVal
     */
    private void parseCompFieldMap(String[] compIndexedVal) {
        for (String s : compIndexedVal) {
            String[] parts = s.split(RESOURCE_SEPARATOR);
            String[] fieldParts = parts[1].split(PROPERTIES_SEPARATOR);
            ArrayList solrFieldList = new ArrayList();
            SolrFieldMappingBean fieldMappingBean = null;
            for (String str : fieldParts) {
                if (str.startsWith(REFERENCE_START_SEPARATOR)) {
                    str = str.replace(REFERENCE_START_SEPARATOR, "").replace(REFERENCE_END_SEPARATOR, "");
                    fieldMappingBean = new SolrFieldMappingBean(parts[0], true, str);
                } else {
                    String[] finalS = str.split(PROPERTY_SEPARATOR);
                    fieldMappingBean = new SolrFieldMappingBean(finalS[0], finalS[1], parts[0]);
                }
                solrFieldList.add(fieldMappingBean);
            }
            compFieldMap.put(parts[0], solrFieldList);
        }
        LOG.debug("Solr field names and values for components are " + compFieldMap);
    }

    /**
     * It parses the pageFieldMap.
     *
     * @param pageIndexedVal
     */
    private void parsePageFieldMap(String[] pageIndexedVal) {
        for (String s : pageIndexedVal) {
            String[] parts = s.split("=");
            pageFieldMap.put(parts[1], parts[0]);
        }

        LOG.debug("Solr field names and values for page are " + pageFieldMap);
    }

}

