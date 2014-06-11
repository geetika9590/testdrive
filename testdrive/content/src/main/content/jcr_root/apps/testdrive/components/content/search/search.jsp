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
                <p id="responseTime">Response time for fetching <c:out value="${fn:length(resultList)}"/> results is
                    <c:out value="${solrResponseTime}"/> ms.</p>
                <ul>
                    <table class="searchLeft">
                        <c:forEach var="itemMap" items="${resultList}">
                            <c:set var="results" value="${itemMap}" scope="request"/>
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
                        <c:forEach var="facetMap" items="${facetFieldMap}">
                            <c:set var="facetResults" value="${facetMap}" scope="request"/>
                            <cq:include script="facetResult.jsp"/>
                        </c:forEach>
                    </ul>
                </table>
            </c:if>
        </div>
    </div>
</div>
<div style="clear:both;"></div>

