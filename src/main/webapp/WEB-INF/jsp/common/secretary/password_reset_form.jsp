<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>パスワード再設定</title>
  <link rel="stylesheet" href="<c:url value='/css/pages/secretary-auth.css'/>">
</head>
<body>
  <div class="reset-container">
    <div style="text-align: center;">
      <div class="role-badge">秘書</div>
    </div>
    
    <h1 class="reset-title">新しいパスワード設定</h1>
    
    <div class="reset-description">
      新しいパスワードを入力してください。
    </div>

    <c:if test="${not empty errorMsg}">
      <div class="error-message">${errorMsg}</div>
    </c:if>

    <div class="info-box info-box--warning">
      <strong>パスワードの条件：</strong><br>
      ・8文字以上<br>
      ・英大文字、英小文字、数字をそれぞれ含む
    </div>

    <form method="post" action="<%=request.getContextPath()%>/secretary/password_reset/reset">
      <input type="hidden" name="token" value="${token}">
      
      <div class="form-group">
        <label>新しいパスワード</label>
        <input type="password" name="newPassword" placeholder="新しいパスワードを入力" required>
      </div>
      
      <div class="form-group">
        <label>新しいパスワード（確認）</label>
        <input type="password" name="confirmPassword" placeholder="もう一度入力してください" required>
      </div>
      
      <button type="submit" class="submit-btn">パスワードを設定</button>
    </form>
  </div>
</body>
</html>

