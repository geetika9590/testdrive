package com.ig.testdrive.integration.solr.service.impl;

import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.ig.testdrive.integration.solr.service.CQOperationsForSolrReplication;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * This class is a helper class for solr replication.
 */
@Component(label = "CQ operations for Solr Replication",
        description = "This servlet is for preparing the xml request for solr",
        metatype = true, immediate = true, enabled = true)
@Service(CQOperationsForSolrReplication.class)
public class CQOperationsForSolrReplicationImpl implements CQOperationsForSolrReplication {
    /**
     * It declares that this class needs an instance of RequestResponseFactory.
     */
    @Reference
    RequestResponseFactory requestResponseFactory;

    /**
     * It declares that this class needs an instance of SlingRequestProcessor.
     */
    @Reference
    SlingRequestProcessor requestProcessor;

    /**
     * This method makes a request to the xmldataurl passed as
     * a parameter and returns the response.
     * @param resourceResolver
     * @param xmlDataUrl
     * @return It returns the XML data.
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public String getXMLData(final ResourceResolver resourceResolver, final String xmlDataUrl) throws ServletException, IOException {

        HttpServletRequest request = requestResponseFactory.createRequest("GET", xmlDataUrl);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletResponse response = requestResponseFactory.createResponse(out);
        requestProcessor.processRequest(request, response, resourceResolver);
        return new String(out.toByteArray(), "UTF-8");
    }
}
