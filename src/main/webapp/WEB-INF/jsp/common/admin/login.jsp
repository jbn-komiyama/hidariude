<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>管理者ログイン</title>
  <link rel="stylesheet" href="<c:url value='/css/pages/admin-auth.css'/>">
</head>
<body>
  <div class="login-container">
    <div class="login-title">
      <div class="role-badge">管理者</div>
      <div>ログイン</div>
    </div>

    <c:if test="${not empty errorMsg}">
      <div class="error-message">${errorMsg}</div>
    </c:if>

    <form method="post" action="<%=request.getContextPath()%>/admin/login">
      <div class="form-group">
        <label>メールアドレス</label>
        <input type="email" name="loginId" required>
      </div>
      <div class="form-group">
        <label>パスワード</label>
        <input type="password" name="password" required>
      </div>
      <button type="submit" class="submit-btn">ログイン</button>
    </form>

    <div class="password-reset-section">
      <a href="<%=request.getContextPath()%>/admin/password_reset" class="password-reset-link">
        <span class="password-reset-icon">🔑</span>
        <span>パスワードをお忘れの方はこちら</span>
      </a>
    </div>

    <div class="back-link back-link--compact">
      <a href="<%=request.getContextPath()%>/">← ログイン選択に戻る</a>
    </div>
  </div>
</body>
</html>