<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>パスワード再設定完了</title>
  <link rel="stylesheet" href="<c:url value='/css/pages/customer-auth.css'/>">
</head>
<body>
  <div class="done-container">
    <div class="success-icon">✓</div>
    
    <h1 class="done-title">パスワードを再設定しました</h1>
    
    <div class="done-message">
      パスワードの再設定が完了しました。<br>
      新しいパスワードでログインしてください。
    </div>

    <a href="<%=request.getContextPath()%>/customer" class="login-btn">ログイン画面へ</a>
  </div>
</body>
</html>

