<%@include file="/apps/testdrive/components/page/global.jspx"%>
<%@page session="false" %>

<cq:includeClientLib categories="testdrive.search"/>
<%
    String core=properties.get("solrCore","");
    String maxRows=properties.get("maxRows","10");
    String isFacet=properties.get("isFacet","false");
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
<head>
    <script type="text/javascript">


        function openMe(url){
            if(url.lastIndexOf(".")==-1){
                url=url+".html";
            }
            window.open(url);
            window.target('_new');
        }

        function filterMe(parentValue,key){
            var queryParam=parentValue+":\""+key+"\"";
            var queryString ="query="+queryParam+"&core="+f1.core.value+"&maxRows="+f1.maxRows.value+"&queryFields="+f1.queryFields.value;
            if(f1.isFacet.value=="true"){
                queryString=queryString+"&isFacet="+f1.isFacet.value+"&facetFields="+f1.facetFields.value;
            }
            var url="/bin/service/solrsearch.html?"+queryString;
            $.getJSON( url, function( data ) {
                if ( console && console.log ) {
                    console.log( "Solr Response:", data );
                }

                var text="<table width='70%' cellspacing='0' style=\"border:0px\">"+
                        "<tr>"+
                        "</tr><ol>";
                var objList=data.solrDocs;
                var responseTime=data.solrResponseTime;
                document.getElementById("responseTime").innerHTML='Response Time for fetching '+objList.length+" records is "+responseTime+' ms'+"</br><br/>";;
                for(var i=0;i<objList.length;i++){
                    text+="<tr>";

                    var printId=true;
                    if(typeof objList[i].titleText!= "undefined"){
                        text+="<td><li><a href='javascript:openMe(\""+objList[i].id+"\");'>"+objList[i].titleText.replace("[", "").replace("]","")+"</a></li>";
                    }
                    else if(typeof objList[i].title!= "undefined") {
                        text+="<td><li><a href='javascript:openMe(\""+objList[i].id+"\");'>"+objList[i].title.replace("[", "").replace("]","")+"</a></li>";
                    }else{
                        text+="<td><li><a href='javascript:openMe(\""+objList[i].id+"\");'>"+objList[i].id+"</a></li>";
                        printId=false;
                    }

                    if(typeof objList[i].description!= "undefined"){
                        text+=objList[i].description.replace("[", "").replace("]","")+"</br>";
                    }
                    if(printId) {
                        text+=objList[i].id;
                    }
                    text+="</br><br/></td></tr>";

                }
                text+="</ol></table>";
                document.getElementById("searchResult").innerHTML=text;

                if(f1.isFacet.value=="true"){

                    var facet= data.solrFacets;
                    var facetedText="<table width='70%' cellspacing='0' style=\"border:0px\">"+
                            "<tr>"+
                            "<th width='100%' \">Facets</th>"+
                            "</tr>";
                    $.each(facet,function(parentKey,parentValue){
                        facetedText+="<tr>"+
                                "<td></br><br/><ul><li>"+parentKey+"</br><br/>";
                        $.each(parentValue,function(key,value){
                            var funt="filterMe('"+parentKey+"','"+key+"')";
                            if(parentKey.lastIndexOf("Tags")!=-1){
                                $.each(value,function(k,v){
                                    facetedText+="<ul><li><a href=\"javascript:"+funt+"\">"+k+"("+v+")"+"</a></li></ul>";
                                });

                            }
                            else{

                                facetedText+="<ul><li><a href=\"javascript:"+funt+"\">"+key+"("+value+")"+"</a></li></ul>";
                            }
                        });
                        facetedText+="</li></ul></td></tr>";
                    });

                    facetedText+="</table>";
                    document.getElementById("facetResult").innerHTML=facetedText;
                }
            });
        }

        function submitMe(){

            if(f1.query.value==""){
                return false;
            }
            queryString ="query="+f1.query.value+"&core="+f1.core.value+"&maxRows="+f1.maxRows.value+"&queryFields="+f1.queryFields.value;
            if(f1.isFacet.value=="true"){
                queryString=queryString+"&isFacet="+f1.isFacet.value+"&facetFields="+f1.facetFields.value;
            }
            url="/bin/service/solrsearch.html?"+queryString;
            $.getJSON( url, function( data ) {
                if ( console && console.log ) {
                    console.log( "Solr Response:", data );
                }

                var text="<table width='70%' cellspacing='0' style=\"border:0px\">"+
                        "<tr>"+
                        "</tr><ol>";
                var objList=data.solrDocs;
                var responseTime=data.solrResponseTime;
                document.getElementById("responseTime").innerHTML='Response Time for fetching '+objList.length+" records is "+responseTime+' ms'+"</br><br/>";;
                for(var i=0;i<objList.length;i++){
                    text+="<tr>";
                    var printId=true;

                if(typeof objList[i].titleText!= "undefined"){
                    text+="<td><li><a href='javascript:openMe(\""+objList[i].id+"\");'>"+objList[i].titleText.replace("[", "").replace("]","")+"</a></li>";
                }
                else if(typeof objList[i].title!= "undefined") {
                    text+="<td><li><a href='javascript:openMe(\""+objList[i].id+"\");'>"+objList[i].title.replace("[", "").replace("]","")+"</a></li>";
                }else{
                    text+="<td><li><a href='javascript:openMe(\""+objList[i].id+"\");'>"+objList[i].id+"</a></li>";
                    printId=false;
                }

                if(typeof objList[i].description!= "undefined"){
                    text+=objList[i].description.replace("[", "").replace("]","")+"</br>";
                }
                    if(printId) {
                        text+=objList[i].id;
                    }
                    text+="</br><br/></td></tr>";

                }
                text+="</ol></table>";
                document.getElementById("searchResult").innerHTML=text;
                if(f1.isFacet.value=="true"){

                    var facet= data.solrFacets;
                    var facetedText="<table width='70%' cellspacing='0' style=\"border:0px\">"+
                            "<tr>"+
                            "<th width='100%' style=\"text-align:center\">Facets</th>"+
                            "</tr>";
                    $.each(facet,function(parentKey,parentValue){

                        facetedText+="<tr>"+
                                "<td><ul><li>"+parentKey+"</br><br/>";
                        $.each(parentValue,function(key,value){
                            var funt="filterMe('"+parentKey+"','"+key+"')";
                            if(parentKey.lastIndexOf("Tags")!=-1){
                                $.each(value,function(k,v){
                                    facetedText+="<ul><li><a href=\"javascript:"+funt+"\">"+k+"("+v+")"+"</a></li></ul>";
                                });

                            }
                            else{

                                facetedText+="<ul><li><a href=\"javascript:"+funt+"\">"+key+"("+value+")"+"</a></li></ul>";
                            }
                        });
                        facetedText+="</li></ul></td></tr>";
                    });

                    facetedText+="</table>";
                    document.getElementById("facetResult").innerHTML=facetedText;
                }
            });
        }

    </script>
</head>


<div>
    <form name="f1" >
        <input type="text" name="query" value="" />
        <input type="button" value="Submit" onclick="submitMe()"/>
        <input type="hidden" name="core" value="<%=core%>" />
        <input type="hidden" name="maxRows" value="<%=maxRows%>" />
        <input type="hidden" name="queryFields" value="<%=queryFieldsString%>" />
        <input type="hidden" name="facetFields" value="<%=fields%>" />
        <input type="hidden" name="isFacet" value="<%=isFacet%>" />
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




