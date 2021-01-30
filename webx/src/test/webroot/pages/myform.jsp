<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <title>MyForm</title>
</head>
<body>
    <h1>XSS测试</h1>
    <!-- 文本框输入：aaa";alert(document.cookie);// -->
    <form action="/xssfile" method="post">
        <input type="text" name="username" placeholder="请输入用户名"></br>
        <input type="password" name="password" placeholder="请输入密码"></br>
        <input type="submit" value="提交">
    </form>
</body>
</html>
