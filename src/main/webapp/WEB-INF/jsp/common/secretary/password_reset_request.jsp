<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>パスワードリセット申請</title>
  <link rel="stylesheet" href="<c:url value='/css/pages/secretary-auth.css'/>">
</head>
<body>
  <div class="reset-container">
    <div style="text-align: center;">
      <div class="role-badge">秘書</div>
    </div>
    
    <h1 class="reset-title">パスワードリセット</h1>
    
    <div class="reset-description">
      登録済みのメールアドレスを入力してください。<br>
      パスワードリセット用のリンクをメールでお送りします。
    </div>

    <c:if test="${not empty errorMsg}">
      <div class="error-message">${errorMsg}</div>
    </c:if>

    <form method="post" action="<%=request.getContextPath()%>/secretary/password_reset/request" id="resetForm">
      <div class="form-group">
        <label>メールアドレス</label>
        <input type="email" name="email" value="${email}" placeholder="example@example.com" required>
      </div>
      <button type="submit" class="submit-btn" id="submitBtn">リセットリンクを送信</button>
    </form>

    <div class="back-link">
      <a href="<%=request.getContextPath()%>/secretary">← ログイン画面に戻る</a>
    </div>
  </div>

  <script>
    // 二重送信防止
    document.getElementById('resetForm').addEventListener('submit', function() {
      var btn = document.getElementById('submitBtn');
      btn.disabled = true;
      btn.textContent = '送信中...';
      btn.style.opacity = '0.6';
      btn.style.cursor = 'not-allowed';
    });
  </script>
</body>
</html>

