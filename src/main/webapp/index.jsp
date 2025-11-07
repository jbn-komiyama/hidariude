<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>BackDesk - ログイン選択</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/index.css">
</head>
<body>
    <div class="container">
        <div class="logo">BackDesk</div>
        <div class="subtitle">役割を選択してログインしてください</div>
        
        <div class="login-options">
            <a href="<%= request.getContextPath() %>/admin" class="login-btn admin-btn">
                管理者ログイン
            </a>
            
            <a href="<%= request.getContextPath() %>/secretary" class="login-btn secretary-btn">
                秘書ログイン
            </a>
            
            <a href="<%= request.getContextPath() %>/customer" class="login-btn customer-btn">
                顧客ログイン
            </a>
        </div>
    </div>
</body>
</html>
