


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

                var text="<table width='70%' cellspacing='0' style=\"border:0px \">"+
                        "<tr>"+
                        "</tr>";
                var objList=data.solrDocs;
                for(var i=0;i<objList.length;i++){
                    text+="<tr>"+
                            "<td><a href='javascript:openMe(\""+objList[i].id+"\");'>"+objList[i].title+"</a></br><br/></td>"+
                            "</tr>";
                }
                text+="</table>";

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
                        "</tr>";
                var objList=data.solrDocs;
                for(var i=0;i<objList.length;i++){
                    text+="<tr>"+
                            "<td><a href='javascript:openMe(\""+objList[i].id+"\");'>"+objList[i].title+"</a></br><br/></td>"+
                            "</tr>";
                }
                text+="</table>";
                document.getElementById("searchResult").innerHTML=text;
                if(f1.isFacet.value=="true"){

                    var facet= data.solrFacets;
                    var facetedText="<table width='70%' cellspacing='0' style=\"border:0px\">"+
                            "<tr>"+
                            "<th width='100%' style=\"text-align:center\">Facets</br><br/></th>"+
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
