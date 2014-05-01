package com.ig.testdrive.integration.solr.service;

import org.apache.sling.api.resource.ResourceResolver;

public interface CQOperationsForSolrSearch {
    String getXMLData(ResourceResolver resourceResolver, String xmlDataUrl) throws Exception;
}
