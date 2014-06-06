<%@include file="/apps/testdrive/components/page/global.jspx"%>
<%@page session="false" %>

<cq:includeClientLib categories="testdrive.search"/>
<%
    String core=properties.get("solrCore","");
    String maxRows=properties.get("maxRows","10");
    String isFacet=properties.get("isFacet","false");
    String searchIn = (String) properties.get("searchIn","/content/geometrixx");
    searchIn=searchIn.replace("/","\\/");
    String[] facetFields=properties.get("facetFields",String[].class);
    String[] searchFields=properties.get("queryFields",String[].class);
    String fields="";
    String queryFieldsString="";
    if(Integer.parseInt(maxRows)<0){
        maxRows="10";
    }
%>

<%
    if (isFacet.equals("true") && facetFields!=null){
        for (String s:facetFields){
            fields+=s+",";
        }
    }
    if (searchFields!=null){
        for (String str:searchFields){
            queryFieldsString+=str+",";
        }
    }
%>

<div>
    <form name="f1" >
        <input type="text" name="query" value="" />
        <input type="button" value="Submit" onclick="submitMe()"/>
        <input type="hidden" name="core" value="<%=core%>" />
        <input type="hidden" name="maxRows" value="<%=maxRows%>" />
        <input type="hidden" name="queryFields" value="<%=queryFieldsString%>" />
        <input type="hidden" name="facetFields" value="<%=fields%>" />
        <input type="hidden" name="isFacet" value="<%=isFacet%>" />
        <input type="hidden" name="searchIn" value="<%=searchIn%>" />
    </form>
    <br/><br/>
    <div class="searchResult">
        <p id="responseTime"></p>
        <div id="searchResult" class="searchLeft" >

        </div>
        <div class="searchRight">
            <div id="facetResult">
            </div></div></div>
</div>
<div style="clear:both;"></div>




