package com.ig.testdrive.integration.solr.service;

import org.apache.sling.api.resource.ResourceResolver;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * This interface is designed as an helper interface for solr replication
 */
public interface CQOperationsForSolrReplication {

    /**
     * This method makes a request to the xmldataurl passed as
     * a parameter and returns the response.
     * @param resourceResolver
     * @param xmlDataUrl
     * @return String
     * @throws ServletException
     * @throws IOException
     */
    String getXMLData(ResourceResolver resourceResolver, String xmlDataUrl)
            throws ServletException, IOException;
}
