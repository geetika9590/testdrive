<%@include file="/apps/testdrive/components/page/global.jspx"%>
<%@page import="com.ig.testdrive.commons.util.CommonMethods" %>
<%
     String html=properties.get("text", "");
	html=CommonMethods.parseHtmlToText(html);
%>


<field name="text_data"><%=html%></field>