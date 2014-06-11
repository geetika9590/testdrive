<%@include file="/apps/testdrive/components/page/global.jspx" %>
<%
%>
<tr>
    <td><br/>
        <c:set var="facetMap" value="${requestScope.facetResults}"/>
        <c:out value="${facetMap.key}"/>
    </td>
</tr>
<c:forEach var="facet" items="${facetMap.value}">
    <tr>
        <td>
            <li>
                <c:if test="${not empty facet.title}">
                    <a title="filter results"
                       href="${currentPage.path}.html?q=${facet.query}"><c:out
                            value="${facet.title} (${facet.count})"/></a>
                </c:if></li>
        </td>
    </tr>
</c:forEach>