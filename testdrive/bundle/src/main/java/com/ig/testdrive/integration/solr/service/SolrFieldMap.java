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

}
