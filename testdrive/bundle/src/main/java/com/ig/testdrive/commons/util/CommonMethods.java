package com.ig.testdrive.commons.util;

import com.ig.testdrive.integration.solr.beans.SolrFieldMappingBean;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Geetika
 * Date: 19/5/14
 * Time: 10:32 AM
 */
public class CommonMethods {

    private static final Logger LOG = LoggerFactory.getLogger(CommonMethods.class);

    /**
     * This method parse the field map for CQ pages and updates the xmlfor pushing data to solr.
     * @param valueMap
     * @param fieldMap
     * @param xmlData
     * @return It returns the updated xml.
     */
    public static StringBuffer parseFieldMap(ValueMap valueMap, HashMap fieldMap, StringBuffer xmlData) {
        Set keys = fieldMap.keySet();
        Iterator iterator = keys.iterator();
        while (iterator.hasNext()) {
            String propName = iterator.next().toString();
            String type = fieldMap.get(propName).toString();
            if (valueMap.get(propName) instanceof Object[]) {
                Object[] arr = (Object[]) valueMap.get(propName);
                for (Object value : arr) {
                    if (value != null) {
                        value = StringEscapeUtils.escapeXml(value.toString());
                        LOG.debug("value after escaping character is" + value);
                        xmlData = CommonMethods.getXMLFieldTag(xmlData, type, value);
                    }
                }
            } else {
                String value = String.valueOf(valueMap.get(propName));
                if (value != null && !value.equalsIgnoreCase("null")) {
                    value = StringEscapeUtils.escapeXml(value.toString());
                    LOG.debug("value after escaping character is" + value);
                    xmlData = CommonMethods.getXMLFieldTag(xmlData, type, value);
                }
            }
        }
        return xmlData;

    }

    /**
     * This method parse the field map for DAM Assets and updates the xml for pushing data to solr.
     * @param assetMetadata
     * @param fieldMap
     * @param xmlData
     * @return It returns the updated xml.
     */
    public static StringBuffer parseFieldMap(HashMap assetMetadata, HashMap fieldMap, StringBuffer xmlData) {
        Set keys = fieldMap.keySet();
        Iterator iterator = keys.iterator();
        while (iterator.hasNext()) {
            String propName = iterator.next().toString();
            String type = fieldMap.get(propName).toString();
            if (assetMetadata.get(propName) instanceof Object[]) {
                Object[] arr = (Object[]) assetMetadata.get(propName);
                for (Object value : arr) {
                    if (value != null) {
                        value = StringEscapeUtils.escapeXml(value.toString());
                        LOG.debug("value after escaping character is" + value);
                        xmlData = CommonMethods.getXMLFieldTag(xmlData, type, value);
                    }
                }
            } else {
                String value = String.valueOf(assetMetadata.get(propName));
                if (value != null && !value.equalsIgnoreCase("null")) {
                    value = StringEscapeUtils.escapeXml(value.toString());
                    LOG.debug("value after escaping character is" + value);
                    xmlData = CommonMethods.getXMLFieldTag(xmlData, type, value);
                }
            }
        }
        return xmlData;

    }



    /**
     * This method parse the field list and updates the xmlfor pushing data to solr.
     * @param valueMap
     * @param fieldList
     * @param xmlData
     * @return It returns the updated xml.
     */
    public static StringBuffer parseFieldMap(ValueMap valueMap, ArrayList fieldList, StringBuffer xmlData) {
        LOG.debug("list is" + fieldList);
        Iterator iterator = fieldList.iterator();
        while (iterator.hasNext()) {
            SolrFieldMappingBean solrFieldMappingBean = (SolrFieldMappingBean) iterator.next();
            LOG.debug("bean is" + solrFieldMappingBean);
            if (valueMap.get(solrFieldMappingBean.getCqField()) instanceof Object[]) {
                Object[] arr = (Object[]) valueMap.get(solrFieldMappingBean.getCqField());
                for (Object value : arr) {
                    if (value != null) {
                        value = StringEscapeUtils.escapeXml(value.toString());
                        LOG.debug("value after escaping character is" + value);
                        xmlData = CommonMethods.getXMLFieldTag(xmlData, solrFieldMappingBean.getSolrField(), value);
                    }
                }
            } else {
                String value = String.valueOf(valueMap.get(solrFieldMappingBean.getCqField()));
                if (value != null && !value.equalsIgnoreCase("null")) {
                    value = StringEscapeUtils.escapeXml(value.toString());
                    LOG.debug("value after escaping character is" + value);
                    xmlData = CommonMethods.getXMLFieldTag(xmlData, solrFieldMappingBean.getSolrField(), value);
                }
            }
        }
        return xmlData;
    }

    /**
     * This method appends the xml.
     * @param xmlData
     * @param type
     * @param value
     * @return It returns the updated xml.
     */
    public static StringBuffer getXMLFieldTag(StringBuffer xmlData, String type, Object value) {
        xmlData = xmlData.append("<field name=\"").append(type)
                .append("\">").append(value.toString()).append("</field>\n");
        return xmlData;

    }
}