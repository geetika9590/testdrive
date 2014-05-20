package com.ig.testdrive.integration.solr.service;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: intelligrape
 * Date: 20/5/14
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SolrFieldMap {

    public HashMap<String, String> getPageFieldMap();
    public HashMap<String, ArrayList> getCompFieldMap();
    public HashMap<String, String> getAssetFieldMap() ;

}
