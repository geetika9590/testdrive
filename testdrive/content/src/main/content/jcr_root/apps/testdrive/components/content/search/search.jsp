<%@include file="/apps/testdrive/components/page/global.jspx"%>
<%@page session="false" %>


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
    if (facetFields!=null){
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
<html>
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
                var text="<table width='70%' cellspacing='0' style=\"border:1px solid #ff0000;\">"+
                        "<tr style=\"background-color:#aaccaa;color:white;\">"+
                        "<th width='100%' style=\"text-align:left\">Search Result links</th>"+
                        "</tr>";
                var objList=data.solrDocs;
                for(var i=0;i<objList.length;i++){
                    text+="<tr>"+
                            "<td><a href='javascript:openMe(\""+objList[i].id+"\");'>"+objList[i].id+"</a></td>"+
                            "</tr>";
                }
                text+="</table>";
                document.getElementById("searchResult").innerHTML=text;

                if(f1.isFacet.value=="true"){

                    var facet= data.solrFacets;
                    var facetedText="<table width='70%' cellspacing='0' style=\"border:1px solid #ff0000;\">"+
                            "<tr style=\"background-color:#aaccaa;color:white;\">"+
                            "<th width='100%' style=\"text-align:left\">Facets</th>"+
                            "</tr>";
                    $.each(facet,function(parentKey,parentValue){
                        facetedText+="<tr>"+
                                "<td><ul><li>"+parentKey;
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
                var text="<table width='70%' cellspacing='0' style=\"border:1px solid #ff0000;\">"+
                        "<tr style=\"background-color:#aaccaa;color:white;\">"+
                        "<th width='100%' style=\"text-align:left\">Search Result links</th>"+
                        "</tr>";
                var objList=data.solrDocs;
                for(var i=0;i<objList.length;i++){
                    text+="<tr>"+
                            "<td><a href='javascript:openMe(\""+objList[i].id+"\");'>"+objList[i].id+"</a></td>"+
                            "</tr>";
                }
                text+="</table>";
                document.getElementById("searchResult").innerHTML=text;
                if(f1.isFacet.value=="true"){

                    var facet= data.solrFacets;
                    var facetedText="<table width='70%' cellspacing='0' style=\"border:1px solid #ff0000;\">"+
                            "<tr style=\"background-color:#aaccaa;color:white;\">"+
                            "<th width='100%' style=\"text-align:left\">Facets</th>"+
                            "</tr>";
                    $.each(facet,function(parentKey,parentValue){

                        facetedText+="<tr>"+
                                "<td><ul><li>"+parentKey;
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
<body>


<form name="f1" >
    <input type="text" name="query" value="" />
    <input type="button" value="Submit" onclick="submitMe()"/>
    <input type="hidden" name="core" value="<%=core%>" />
    <input type="hidden" name="maxRows" value="<%=maxRows%>" />
    <input type="hidden" name="queryFields" value="<%=queryFieldsString%>" />
    <input type="hidden" name="facetFields" value="<%=fields%>" />
    <input type="hidden" name="isFacet" value="<%=isFacet%>" />
</form>
<div id="searchResult"></div>
<div id="facetResult"></div>

</body>
</html>