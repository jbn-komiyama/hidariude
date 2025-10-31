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
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    body {
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
      background: linear-gradient(135deg, #e8f4f8 0%, #f0f8ff 50%, #e6f2ff 100%);
      min-height: 100vh;
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 20px;
    }
    .reset-container {
      background: #ffffff;
      padding: 2.5rem;
      border-radius: 20px;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
      border: 1px solid rgba(0, 0, 0, 0.06);
      width: 100%;
      max-width: 480px;
    }
    .reset-title {
      font-size: 1.8rem;
      font-weight: 700;
      color: #2c3e50;
      text-align: center;
      margin-bottom: 1rem;
      letter-spacing: 0.5px;
    }
    .reset-description {
      color: #6c757d;
      text-align: center;
      margin-bottom: 2rem;
      font-size: 0.95rem;
      line-height: 1.6;
    }
    .role-badge {
      display: inline-block;
      background: linear-gradient(135deg, #007bff, #0056b3);
      color: #ffffff;
      padding: 0.4rem 1rem;
      border-radius: 20px;
      font-size: 0.85rem;
      font-weight: 600;
      margin-bottom: 1.5rem;
      letter-spacing: 0.5px;
    }
    .error-message {
      background: #f8d7da;
      border: 1px solid #f5c2c7;
      color: #842029;
      padding: 1rem;
      border-radius: 10px;
      margin-bottom: 1.5rem;
      font-size: 0.9rem;
    }
    .form-group {
      margin-bottom: 1.5rem;
    }
    label {
      display: block;
      color: #495057;
      margin-bottom: 0.5rem;
      font-size: 0.95rem;
      font-weight: 500;
    }
    input[type="email"] {
      width: 100%;
      padding: 0.9rem;
      background: #f8f9fa;
      border: 1px solid #dee2e6;
      border-radius: 10px;
      color: #212529;
      font-size: 1rem;
      transition: all 0.3s ease;
    }
    input[type="email"]:focus {
      outline: none;
      border-color: #007bff;
      background: #ffffff;
      box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.15);
    }
    input::placeholder {
      color: #adb5bd;
    }
    .submit-btn {
      width: 100%;
      padding: 1rem;
      background: linear-gradient(135deg, #007bff, #0056b3);
      border: none;
      border-radius: 12px;
      color: #ffffff;
      font-size: 1.05rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s ease;
      letter-spacing: 0.5px;
      box-shadow: 0 4px 12px rgba(0, 123, 255, 0.25);
    }
    .submit-btn:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(0, 123, 255, 0.35);
    }
    .submit-btn:active {
      transform: translateY(0);
    }
    .back-link {
      text-align: center;
      margin-top: 1.5rem;
    }
    .back-link a {
      color: #6c757d;
      text-decoration: none;
      font-size: 0.9rem;
      transition: color 0.3s ease;
    }
    .back-link a:hover {
      color: #495057;
    }
  </style>
</head>
<body>
  <div class="reset-container">
    <div style="text-align: center;">
      <div class="role-badge">顧客</div>
    </div>
    
    <h1 class="reset-title">パスワードリセット</h1>
    
    <div class="reset-description">
      登録済みのメールアドレスを入力してください。<br>
      パスワードリセット用のリンクをメールでお送りします。
    </div>

    <c:if test="${not empty errorMsg}">
      <div class="error-message">${errorMsg}</div>
    </c:if>

    <form method="post" action="<%=request.getContextPath()%>/customer/password_reset/request" id="resetForm">
      <div class="form-group">
        <label>メールアドレス</label>
        <input type="email" name="email" value="${email}" placeholder="example@example.com" required>
      </div>
      <button type="submit" class="submit-btn" id="submitBtn">リセットリンクを送信</button>
    </form>

    <div class="back-link">
      <a href="<%=request.getContextPath()%>/customer">← ログイン画面に戻る</a>
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

