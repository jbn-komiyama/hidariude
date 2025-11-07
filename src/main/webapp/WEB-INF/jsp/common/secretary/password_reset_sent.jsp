<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>メール送信完了</title>
  <link rel="stylesheet" href="<c:url value='/css/pages/secretary-auth.css'/>">
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

    <div class="info-box info-box--info">
      <strong>ご注意</strong>
      <ul>
        <li>リンクの有効期限は24時間です</li>
        <li>メールが届かない場合は、迷惑メールフォルダをご確認ください</li>
        <li>リンクは一度のみ使用できます</li>
      </ul>
    </div>

    <a href="<%=request.getContextPath()%>/secretary" class="back-btn">ログイン画面へ戻る</a>
  </div>
</body>
</html>

