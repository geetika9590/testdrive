<%@include file="/apps/testdrive/components/page/global.jspx"%>
<%@page session="false" %><%
%><%@taglib prefix="testdrive" uri="http://ig.testdrive.com/marketing/tags" %>
<cq:includeClientLib categories="testdrive.search"/>

<testdrive:solrsearch />

<div>
	<center>
	    <form name="f1" action="${currentPage.path}.html">
	        <input size="41" maxlength="2048" name="q" value="${escapedQueryForAttr}"/>
	        <input type="submit" value="Submit"/>
	    </form>
    </center>
    
    <br/>
    <div class="searchResult">
        <p id="responseTime"><c:out value="${solrResponseTime}"/></p>
        
        <div id="searchResult" class="searchLeft" >
        
        <c:if test="${not empty resultList}">
        	<c:forEach var="itemMap" items="${resultList}">
        		<a href="${itemMap['id']}.html"><c:out value="${itemMap['titleText']}" /></a><br />
        		
        		<c:if test="${not empty itemMap['description']}">
	        		Description = <div><c:out value="${itemMap['description']}"/></div>
	        		<br /><br />
        		</c:if>
        		  	
        	</c:forEach>
        </c:if>

        </div>
        <div class="searchRight">

        <p>Page Tags</p>
        <c:set var="pageTagsList" value="${facetFieldMap['pageTags']}"/>
            <c:forEach var="facet" items="${pageTagsList}">
                Query = ${facet.query}

    			<a title="filter results" href="${currentPage}.html?q=${facet.query}"><c:out value="${facet.title} (${facet.count})"/></a> 
        		<br />
        	</c:forEach>

            <div id="facetResult">
            </div></div></div>
</div>
<div style="clear:both;"></div>



