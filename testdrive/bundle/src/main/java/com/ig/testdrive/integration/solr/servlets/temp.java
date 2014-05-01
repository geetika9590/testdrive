package com.ig.testdrive.integration.solr.servlets;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: intelligrape
 * Date: 25/4/14
 * Time: 5:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class temp {
    public static void main(String[] args) {

            String tagKey="properties:style/color";
            String node[]=tagKey.split(":");
            String[] prop=node[1].split("/");
            String title="";
        System.out.println(prop[0]+" "+prop[1]);


     /*   HashMap<String,String> solrFieldMap=new HashMap();
        solrFieldMap.put(solrFieldParts)
        compFieldMap.put(parts[0], parts[1]);*/
    }
}
