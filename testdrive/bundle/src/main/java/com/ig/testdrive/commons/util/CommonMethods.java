package com.ig.testdrive.commons.util;

import com.ig.testdrive.integration.solr.beans.SolrFieldMappingBean;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.sling.api.resource.Resource;
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

    private static final Logger log = LoggerFactory.getLogger(CommonMethods.class);

    public static final String[] SPECIAL_CHARS={"@", "<","&","=","+","|","!","{","}","(",")","^","~",">"};

    public static String parseHtmlToText(String html) {
        html = html.replaceAll("\\<.*?\\>", "").replaceAll("&nbsp;", "");
        html = html.replaceAll("\n", "").replaceAll("\r", "");
        return html;
    }

    public static String removeSpecialChars(String source){
        System.out.println("String to be modified is"+source);
        for(String s:SPECIAL_CHARS){
            if (source.contains(s)){
                source=source.replaceAll(s," ");
            }
        }
        System.out.println("String after modification is"+source);
        return source;
    }


    public static StringBuffer parseFieldMap(ValueMap valueMap,HashMap fieldMap,StringBuffer xmlData){
        Set keys = fieldMap.keySet();
        Iterator iterator = keys.iterator();
        while (iterator.hasNext()) {
            String propName = iterator.next().toString();
            String type=fieldMap.get(propName).toString();
            if (valueMap.get(propName) instanceof Object[]) {
                Object[] arr = (Object[]) valueMap.get(propName);
                for (Object value : arr) {
                    if (value != null) {
                        value = StringEscapeUtils.escapeXml(value.toString());
                        log.debug("value after escaping character is" + value);
                        xmlData = CommonMethods.getXMLFieldTag(xmlData,type,value);
                    }
                }
            } else {
                String value = String.valueOf(valueMap.get(propName));
                if (value != null && !value.equalsIgnoreCase("null")) {
                    value = StringEscapeUtils.escapeXml(value.toString());
                    log.debug("value after escaping character is" + value);;
                    xmlData = CommonMethods.getXMLFieldTag(xmlData,type,value);
                }
            }
        }
        return xmlData;

    }

    public static StringBuffer parseFieldBean(ValueMap valueMap,ArrayList fieldList,StringBuffer xmlData){
        log.info("list is" + fieldList);
            Iterator iterator = fieldList.iterator();
            while (iterator.hasNext()) {
                SolrFieldMappingBean solrFieldMappingBean = (SolrFieldMappingBean) iterator.next();
                log.info("bean is"+solrFieldMappingBean);
                if (valueMap.get(solrFieldMappingBean.getCqField()) instanceof Object[]) {
                    Object[] arr = (Object[]) valueMap.get(solrFieldMappingBean.getCqField());
                    for (Object value : arr) {
                        if (value != null) {
                            value = StringEscapeUtils.escapeXml(value.toString());
                            log.debug("value after escaping character is" + value);
                            xmlData = CommonMethods.getXMLFieldTag(xmlData,solrFieldMappingBean.getSolrField(),value);
                        }
                    }
                } else {
                    String value = String.valueOf(valueMap.get(solrFieldMappingBean.getCqField()));
                    if (value != null && !value.equalsIgnoreCase("null")) {
                        value = StringEscapeUtils.escapeXml(value.toString());
                        log.debug("value after escaping character is" + value);;
                        xmlData = CommonMethods.getXMLFieldTag(xmlData,solrFieldMappingBean.getSolrField(),value);
                    }
                }
            }
        return xmlData;
    }


    public static StringBuffer getXMLFieldTag(StringBuffer xmlData, String type, Object value){
        xmlData=xmlData.append("<field name=\"").append(type)
                .append("\">").append(value.toString()).append("</field>\n");
        return xmlData;

    }

    public static String checkNull(String text){
        return ((text!=null)?text : "");
    }
}