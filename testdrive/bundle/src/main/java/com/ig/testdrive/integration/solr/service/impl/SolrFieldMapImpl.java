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
 * Created with IntelliJ IDEA.
 * User: intelligrape
 * Date: 20/5/14
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
@Component(label = "Solr Field Map servlet", description = "This servlet contains the mapping of solr and CQ fields", enabled = true, immediate = true, metatype = true)
@Service(SolrFieldMap.class)
  public class SolrFieldMapImpl implements SolrFieldMap {

    private static final Logger log = LoggerFactory.getLogger(SolrFieldMap.class);

    private static final String PAGE_METADATA_VALUES_TO_BE_INDEXED = "page.values";
    private static final String ASSET_METADATA_VALUES_TO_BE_INDEXED = "indexed.values";

    private static final String COMP_METADATA_VALUES_TO_BE_INDEXED = "comp.values";


    @Property(name = PAGE_METADATA_VALUES_TO_BE_INDEXED, label = "Page Indexed values", description = "Enter the solr and property mapping for pages",
            value = {"title=jcr:title", "description=jcr:description", "pageTags=cq:tags"}, propertyPrivate = false, cardinality = Integer.MAX_VALUE)
    private String PAGE_INDEX_VALUES;


    @Property(name = COMP_METADATA_VALUES_TO_BE_INDEXED, label = "Component Indexed values", description = "Enter the component resource types, their property mapping separated by @",
            value = {"foundation/components/title@title=jcr:title", "foundation/components/textimage@title=jcr:title<>text_data=fileReference",
                    "foundation/components/image@text_data=fileReference<>title=jcr:title", "foundation/components/search@title=jcr:title<>text_data=searchIn"}, propertyPrivate = false, cardinality = Integer.MAX_VALUE)
    private String COMP_INDEX_VALUES;

    @Property(name = ASSET_METADATA_VALUES_TO_BE_INDEXED, label = "DAM Asset Metadata Indexed values", description = "Enter the fields to be indexed for DAM assets",
            value = {"title=dc:title", "description=dc:description", "format=dc:format","damTags=cq:tags"}, propertyPrivate = false, cardinality = Integer.MAX_VALUE)
    private String INDEX_VALUES;

    private HashMap<String, String> pageFieldMap = new HashMap<String, String>();

    private HashMap<String, ArrayList> compFieldMap = new HashMap<String, ArrayList>();
    private HashMap<String, String> assetFieldMap = new HashMap<String, String>();

    private String[] pageIndexedVal,compIndexedVal,damIndexedVal;
    private static final String RESOURCE_SEPARATOR = "@";
    private static final String PROPERTIES_SEPARATOR = "<>";
    private static final String PROPERTY_SEPARATOR = "=";
    private static final String REFERENCE_START_SEPARATOR = "{";
    private static final String REFERENCE_END_SEPARATOR = "}";

    @Activate
    protected void activate(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties();
        log.debug("inside activate method of Page servlet");
        pageIndexedVal = (String[]) properties.get(PAGE_METADATA_VALUES_TO_BE_INDEXED);
        compIndexedVal = (String[]) properties.get(COMP_METADATA_VALUES_TO_BE_INDEXED);
        damIndexedVal= (String[]) properties.get(ASSET_METADATA_VALUES_TO_BE_INDEXED);

        parseIndexedValues(pageIndexedVal,damIndexedVal,compIndexedVal);
    }

    @Modified
    protected void modified(ComponentContext componentContext) {
        log.debug("Values have been modified");
        activate(componentContext);
    }
    @Override
    public HashMap<String, String> getPageFieldMap() {
        return pageFieldMap;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public HashMap<String, ArrayList> getCompFieldMap() {
        return compFieldMap;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public HashMap<String, String> getAssetFieldMap() {
        return assetFieldMap;  //To change body of implemented methods use File | Settings | File Templates.
    }


    private void parseIndexedValues(String[] pageIndexedVal,String[] damIndexedVal,String[] compIndexedVal){
        parsePageFieldMap(pageIndexedVal);
        parseAssetFieldMap(damIndexedVal);
        parseCompFieldMap(compIndexedVal);
    }

    private void parseAssetFieldMap(String[] damIndexedVal){
        for (String s : damIndexedVal) {
            log.debug("String is"+s);
            String[] parts = s.split("=");
            assetFieldMap.put(parts[1], parts[0]);
        }
        log.debug("Solr field names and values for assets are " + assetFieldMap);
    }

    private void parseCompFieldMap(String[] compIndexedVal){
        for (String s : compIndexedVal) {
            String[] parts = s.split(RESOURCE_SEPARATOR);
            String[] fieldParts = parts[1].split(PROPERTIES_SEPARATOR);
            ArrayList solrFieldList = new ArrayList();
            SolrFieldMappingBean fieldMappingBean = null;
            for (String str : fieldParts) {
                if (str.startsWith(REFERENCE_START_SEPARATOR)) {
                    str = str.replace(REFERENCE_START_SEPARATOR, "").replace(REFERENCE_END_SEPARATOR, "");
                    fieldMappingBean=new SolrFieldMappingBean(parts[0],true,str);
                } else {
                    String[] finalS = str.split(PROPERTY_SEPARATOR);
                    fieldMappingBean = new SolrFieldMappingBean(finalS[0], finalS[1], parts[0]);
                }
                solrFieldList.add(fieldMappingBean);
            }
            compFieldMap.put(parts[0], solrFieldList);
        }
        log.debug("Solr field names and values for components are " + compFieldMap);
    }


    private void parsePageFieldMap(String[] pageIndexedVal){
        for (String s : pageIndexedVal) {
            String[] parts = s.split("=");
            pageFieldMap.put(parts[1], parts[0]);
        }

        log.debug("Solr field names and values for page are " + pageFieldMap);
    }

}

