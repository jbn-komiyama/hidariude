<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8" />
  <title>アカウント編集（管理者）</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
  <style>
    .form-note { font-size: .9rem; color: #6c757d; }
  </style>
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf" %>

<div class="container py-4">

  <h1 class="h4 mb-4">アカウント編集（管理者）</h1>

  <c:if test="${not empty errorMsg}">
    <div class="alert alert-danger">${errorMsg}</div>
  </c:if>

  <form method="post" action="${pageContext.request.contextPath}/admin/id_edit_done">
    <!-- CSRFトークンを使っている場合はここに hidden を追加 -->

    <div class="card shadow-sm">
      <div class="card-body">
        <div class="mb-3">
          <label class="form-label">メールアドレス</label>
          <input type="email" name="mail" class="form-control" required value="${form.mail}">
        </div>

        <div class="mb-3">
          <label class="form-label">パスワード</label>
          <input type="password" name="password" class="form-control" placeholder="変更しない場合は空のまま">
          <div class="form-note">空欄のまま送信するとパスワードは変更されません。</div>
        </div>

        <div class="mb-3">
          <label class="form-label">氏名</label>
          <input type="text" name="name" class="form-control" required value="${form.name}">
        </div>

        <div class="mb-3">
          <label class="form-label">氏名（かな）</label>
          <input type="text" name="nameRuby" class="form-control" value="${form.nameRuby}">
        </div>
      </div>
    </div>

    <div class="mt-3 d-flex gap-2">
      <button type="submit" class="btn btn-primary">保存する</button>
    </div>
  </form>

</div>
<script
		src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
