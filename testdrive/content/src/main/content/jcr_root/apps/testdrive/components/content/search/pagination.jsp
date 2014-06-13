<%@include file="/apps/testdrive/components/page/global.jspx" %>
<%
%>
<c:if test="${totalHits > resultsPerPage}">
    <fmt:parseNumber var="totalPages" integerOnly="true"
                     type="number" value="${requestScope.totalPages}" />
    Results
    <c:if test="${currentPageIndex !=1}">
        <a href="${currentPage.path}.html?q=${escapedQuery}&page=${currentPageIndex-1}" > Previous</a>
    </c:if>
    <c:forEach var="pageIndex" begin="1" end="${totalPages}" step="1">
        <c:choose>
            <c:when test="${currentPageIndex == pageIndex}" >
                <c:out value="${pageIndex}" />
            </c:when>
            <c:otherwise>
                <a class="pageLinks" href="${currentPage.path}.html?q=${escapedQuery}&page=${pageIndex}" >
                    <c:out value="${pageIndex}" /></a>
            </c:otherwise>
        </c:choose>
    </c:forEach>
    <c:if test="${currentPageIndex < totalPages}">
        <a href="${currentPage.path}.html?q=${escapedQuery}&page=${currentPageIndex+1}" > Next</a>
    </c:if>
</c:if>

