<%@page import="com.day.cq.wcm.api.WCMMode"%>
<%
    WCMMode mode=WCMMode.fromRequest(request);
    if(mode==WCMMode.EDIT || mode ==WCMMode.DESIGN)
        WCMMode.DISABLED.toRequest(request);
    String url = request.getRequestURI().toString();
    url=url.replace(".solrsearch.html","");
%>
<%@include file="/apps/testdrive/components/page/global.jspx"%>

<add>
    <doc>
        <field name="id"><%=url%></field>
        <cq:include path="par" resourceType="foundation/components/parsys"/>
    </doc>
</add>