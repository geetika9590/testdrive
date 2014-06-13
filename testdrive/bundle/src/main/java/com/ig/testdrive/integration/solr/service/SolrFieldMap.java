package com.ig.testdrive.integration.solr.service;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This interface declares the methods for fetching the field mappings  .
 */
public interface SolrFieldMap {
    /**
     * Getter method for retrieving pageFieldMap
     * @return  It return a HashMap which will contains the page field mapping.
     */
    public HashMap<String, String> getPageFieldMap();

    /**
     * Getter method for retrieving compFieldMap
     * @return  It return a HashMap which will contains the component field mapping.
     */
    public HashMap<String, ArrayList> getCompFieldMap();

    /**
     * Getter method for retrieving assetFieldMap
     * @return  It return a HashMap which will contains the asset field mapping.
     */
    public HashMap<String, String> getAssetFieldMap();

    /**
     * Getter method for retrieving SearchURL
     * @return  It returns the Solr Search URL.
     */
    public String getSearchURL();
    /**
     * Getter method for facetFields.
     *
     * @return It returns the facetFields.
     */
    public String[] getFacetFields();

    /**
     * Getter method for retrieving buffer size which
     * describes the maximum rows that will be fetched in a go.
     * @return  It return the search buffer size.
     */
    public int getBufferSize();

}
