package com.ig.testdrive.integration.solr.service;

import org.apache.sling.api.resource.ResourceResolver;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Geetika
 * Date: 19/5/14
 * Time: 10:32 AM
 */
public interface CQOperationsForSolrReplication {

    /**
     * This method generates the XML for posting the data to Solr
     * @param resourceResolver
     * @param xmlDataUrl
     * @return String
     * @throws ServletException
     * @throws IOException
     */
    String getXMLData(ResourceResolver resourceResolver, String xmlDataUrl)
             throws ServletException, IOException;
}
