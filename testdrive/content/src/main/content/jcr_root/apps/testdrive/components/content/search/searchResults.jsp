<%@include file="/apps/testdrive/components/page/global.jspx" %><%
%><tr><td><br /><li>
<c:set var="itemMap" value="${requestScope.results}"/>
    <c:choose>
        <c:when test="${itemMap['type'] eq 'page'}">
            <c:choose>
                <c:when test="${not empty itemMap['titleText']}">
                    <a href="${itemMap['id']}.html"><c:out
                            value="${itemMap['titleText']}"/></a>
                </c:when>
                <c:otherwise>
                    <a href="${itemMap['id']}.html"><c:out
                            value="${itemMap['id']}"/></a>
                </c:otherwise>
            </c:choose>
        </c:when>
        <c:when test="${itemMap['type'] eq 'asset'}">
            <c:choose>
                <c:when test="${not empty itemMap['titleText']}">
                    <a href="${itemMap['id']}"><c:out
                            value="${itemMap['titleText']}"/></a>
                </c:when>
                <c:otherwise>
                    <a href="${itemMap['id']}"><c:out
                            value="${itemMap['id']}"/></a>
                </c:otherwise>
            </c:choose>
        </c:when>
    </c:choose>
    <br /><c:if test="${not empty itemMap['description']}">
    <c:out value="${itemMap['description']}"/>
</c:if>
</li></td>
</tr>