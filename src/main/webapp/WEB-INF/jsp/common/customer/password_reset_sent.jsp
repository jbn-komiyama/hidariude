<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>メール送信完了</title>
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
    .sent-container {
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
    .sent-title {
      font-size: 1.8rem;
      font-weight: 700;
      color: #2c3e50;
      margin-bottom: 1.5rem;
      letter-spacing: 0.5px;
    }
    .sent-message {
      color: #495057;
      font-size: 1rem;
      line-height: 1.8;
      margin-bottom: 2rem;
    }
    .info-box {
      background: #e7f3ff;
      border-left: 4px solid #0d6efd;
      padding: 1.2rem;
      border-radius: 8px;
      margin-bottom: 2rem;
      text-align: left;
    }
    .info-box ul {
      margin: 0.5rem 0 0 1.2rem;
      color: #495057;
      font-size: 0.9rem;
      line-height: 1.8;
    }
    .back-btn {
      display: inline-block;
      padding: 0.9rem 2.5rem;
      background: linear-gradient(135deg, #007bff, #0056b3);
      border: none;
      border-radius: 12px;
      color: #ffffff;
      font-size: 1rem;
      font-weight: 600;
      text-decoration: none;
      cursor: pointer;
      transition: all 0.3s ease;
      letter-spacing: 0.5px;
      box-shadow: 0 4px 12px rgba(0, 123, 255, 0.25);
    }
    .back-btn:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(0, 123, 255, 0.35);
    }
    .back-btn:active {
      transform: translateY(0);
    }
  </style>
</head>
<body>
  <div class="sent-container">
    <div class="success-icon">✓</div>
    
    <h1 class="sent-title">メールを送信しました</h1>
    
    <div class="sent-message">
      パスワードリセット用のリンクをメールで送信しました。<br>
      メールに記載されているリンクをクリックして、<br>
      新しいパスワードを設定してください。
    </div>

    <div class="info-box">
      <strong>ご注意</strong>
      <ul>
        <li>リンクの有効期限は24時間です</li>
        <li>メールが届かない場合は、迷惑メールフォルダをご確認ください</li>
        <li>リンクは一度のみ使用できます</li>
      </ul>
    </div>

    <a href="<%=request.getContextPath()%>/customer" class="back-btn">ログイン画面へ戻る</a>
  </div>
</body>
</html>

