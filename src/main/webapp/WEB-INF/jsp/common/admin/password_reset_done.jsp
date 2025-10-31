<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>パスワード再設定完了</title>
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
    .done-container {
      background: #ffffff;
      padding: 3rem;
      border-radius: 20px;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
      border: 1px solid rgba(0, 0, 0, 0.06);
      width: 100%;
      max-width: 520px;
      text-align: center;
    }
    .success-icon {
      width: 80px;
      height: 80px;
      margin: 0 auto 2rem;
      background: linear-gradient(135deg, #28a745, #20c997);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 2.5rem;
      color: #ffffff;
      box-shadow: 0 4px 16px rgba(40, 167, 69, 0.25);
    }
    .done-title {
      font-size: 1.8rem;
      font-weight: 700;
      color: #2c3e50;
      margin-bottom: 1.5rem;
      letter-spacing: 0.5px;
    }
    .done-message {
      color: #495057;
      font-size: 1rem;
      line-height: 1.8;
      margin-bottom: 2.5rem;
    }
    .login-btn {
      display: inline-block;
      padding: 0.9rem 2.5rem;
      background: linear-gradient(135deg, #dc3545, #c82333);
      border: none;
      border-radius: 12px;
      color: #ffffff;
      font-size: 1rem;
      font-weight: 600;
      text-decoration: none;
      cursor: pointer;
      transition: all 0.3s ease;
      letter-spacing: 0.5px;
      box-shadow: 0 4px 12px rgba(220, 53, 69, 0.25);
    }
    .login-btn:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(220, 53, 69, 0.35);
    }
    .login-btn:active {
      transform: translateY(0);
    }
  </style>
</head>
<body>
  <div class="done-container">
    <div class="success-icon">✓</div>
    
    <h1 class="done-title">パスワードを再設定しました</h1>
    
    <div class="done-message">
      パスワードの再設定が完了しました。<br>
      新しいパスワードでログインしてください。
    </div>

    <a href="<%=request.getContextPath()%>/admin" class="login-btn">ログイン画面へ</a>
  </div>
</body>
</html>

