<%@include file="/apps/testdrive/components/page/global.jspx" %>
<%@page session="false" %>
<%
%>
<%@taglib prefix="testdrive" uri="http://ig.testdrive.com/marketing/tags" %>
<cq:includeClientLib categories="testdrive.search"/>

<testdrive:solrsearch/>

<div style="width: 100%">
    <center>
        <form name="f1" action="${currentPage.path}.html">
            <input size="41" maxlength="2048" name="q" value="${escapedQueryForAttr}"/>
            <input type="submit" value="Submit"/>
        </form>
    </center>
    <br/>

    <div class="searchResult">
        <div id="searchResult" class="searchLeft">
            <c:if test="${not empty resultList}">
                <c:set var="resultsPerPage" value="${fn:length(resultList)}" scope="request" />
                <c:set var="currentPageIndex" value="${currentPageNo}" scope="request"/>
                <c:set var="startValue" value="${((currentPageNo-1) * properties.maxRows)+1}" />
                <p id="responseTime">Showing
                    <c:out value="${startValue}"/> -
                    <c:out value="${startValue + resultsPerPage-1}"/> results of <c:out value="${totalHits}"/> matching results.
                   ( <c:out value="${solrResponseTime}"/> ms ).
                </p>
                <ul>
                    <table class="searchLeft">
                        <c:forEach var="results" items="${resultList}">
                            <c:set var="itemMap" value="${results}" scope="request"/>
                            <cq:include script="searchResults.jsp"/>
                        </c:forEach>
                    </table>
                </ul>
            </c:if>
        </div>
        <div class="searchRight">
            <c:if test="${not empty facetFieldMap}">
                <table class="searchRight">
                    <ul>
                        <c:forEach var="facetResults" items="${facetFieldMap}">
                            <c:set var="facetMap" value="${facetResults}" scope="request"/>
                            <cq:include script="facetResult.jsp"/>
                        </c:forEach>
                    </ul>
                </table>
            </c:if>
        </div>
    </div>
</div>
<div id="paginate" style="clear:both;">
    <c:set var="totalHits" value="${totalHits}" scope="request"/>
    <c:set var="totalPages" value="${totalPages}" scope="request" />
    <c:set var="escapedQuery" value="${escapedQuery}" scope="request" />
    <cq:include script="pagination.jsp"/>
</div>

