<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <title>MyForm</title>
</head>
<body>
    <h1>XSS测试</h1>
    <%
	String username = request.getParameter("username");
	%>
    <p>用户名: <%=username%></p>
</body>
<script>
    var username = "<%=username%>";
</script>
</html>
