<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>顧客ログイン</title>
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
    .login-container {
      background: #ffffff;
      padding: 2.5rem;
      border-radius: 20px;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
      border: 1px solid rgba(0, 0, 0, 0.06);
      width: 100%;
      max-width: 420px;
    }
    .login-title {
      font-size: 2rem;
      font-weight: 700;
      color: #2c3e50;
      text-align: center;
      margin-bottom: 2rem;
      letter-spacing: 1px;
    }
    .role-badge {
      display: inline-block;
      background: linear-gradient(135deg, #0d6efd, #0b5ed7);
      color: #ffffff;
      padding: 0.5rem 1.2rem;
      border-radius: 20px;
      font-size: 0.9rem;
      font-weight: 600;
      margin-bottom: 2rem;
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
    input[type="email"],
    input[type="password"] {
      width: 100%;
      padding: 0.9rem;
      background: #f8f9fa;
      border: 1px solid #dee2e6;
      border-radius: 10px;
      color: #212529;
      font-size: 1rem;
      transition: all 0.3s ease;
    }
    input[type="email"]:focus,
    input[type="password"]:focus {
      outline: none;
      border-color: #0d6efd;
      background: #ffffff;
      box-shadow: 0 0 0 3px rgba(13, 110, 253, 0.15);
    }
    input::placeholder {
      color: #adb5bd;
    }
    .submit-btn {
      width: 100%;
      padding: 1rem;
      background: linear-gradient(135deg, #0d6efd, #0b5ed7);
      border: none;
      border-radius: 12px;
      color: #ffffff;
      font-size: 1.05rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s ease;
      letter-spacing: 0.5px;
      box-shadow: 0 4px 12px rgba(13, 110, 253, 0.25);
      position: relative;
      overflow: hidden;
    }
    .submit-btn::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(255,255,255,0.15);
      transform: translateX(-100%);
      transition: transform 0.3s ease;
    }
    .submit-btn:hover::before {
      transform: translateX(0);
    }
    .submit-btn:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(13, 110, 253, 0.35);
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
  <div class="login-container">
    <div class="login-title">
      <div class="role-badge">顧客</div>
      <div>ログイン</div>
    </div>

    <c:if test="${not empty errorMsg}">
      <div class="error-message">${errorMsg}</div>
    </c:if>

    <form method="post" action="<%=request.getContextPath()%>/customer/login">
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

    <div class="back-link">
      <a href="<%=request.getContextPath()%>/">← ログイン選択に戻る</a>
    </div>
  </div>
</body>
</html>