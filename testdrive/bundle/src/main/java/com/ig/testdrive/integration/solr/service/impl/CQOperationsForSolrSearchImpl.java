package com.ig.testdrive.integration.solr.service.impl;

import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.ig.testdrive.integration.solr.service.*;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;


@Component(immediate = true, enabled = true)
@Service(CQOperationsForSolrSearch.class)
public class CQOperationsForSolrSearchImpl implements CQOperationsForSolrSearch {

    private static final Logger log = LoggerFactory.getLogger(CQOperationsForSolrSearchImpl.class);

    @Reference
    RequestResponseFactory requestResponseFactory;

    @Reference
    SlingRequestProcessor requestProcessor;

    @Override
    public String getXMLData(ResourceResolver resourceResolver, String xmlDataUrl) throws Exception {

        HttpServletRequest request = requestResponseFactory.createRequest("GET", xmlDataUrl);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletResponse response;
        response = requestResponseFactory.createResponse(out);
        requestProcessor.processRequest(request, response, resourceResolver);
        return new String(out.toByteArray(), "UTF-8");
    }
}