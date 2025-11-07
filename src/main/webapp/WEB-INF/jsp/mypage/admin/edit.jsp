<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
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
<body class="bg-light">
<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf" %>

<div class="container py-4">

  <h1 class="h3 mb-3">管理者 マイページ編集</h1>

  <c:if test="${not empty errorMsg}">
    <div class="alert alert-danger" role="alert">${errorMsg}</div>
  </c:if>

  <div class="card shadow-sm mb-4">
    <div class="card-body">
      <h2 class="h6 text-muted mb-3">現在のアカウント情報</h2>
      <dl class="row mb-0">
        <dt class="col-sm-3">管理者ID</dt>
        <dd class="col-sm-9"><c:out value="${admin.id}"/></dd>

        <dt class="col-sm-3">作成日時</dt>
        <dd class="col-sm-9">
          <c:choose>
            <c:when test="${not empty admin.createdAt}">
              <fmt:formatDate value="${admin.createdAt}" pattern="yyyy/MM/dd HH:mm" />
            </c:when>
            <c:otherwise>—</c:otherwise>
          </c:choose>
        </dd>

        <dt class="col-sm-3">更新日時</dt>
        <dd class="col-sm-9">
          <c:choose>
            <c:when test="${not empty admin.updatedAt}">
              <fmt:formatDate value="${admin.updatedAt}" pattern="yyyy/MM/dd HH:mm" />
            </c:when>
            <c:otherwise>—</c:otherwise>
          </c:choose>
        </dd>

        <dt class="col-sm-3">最終ログイン</dt>
        <dd class="col-sm-9">
          <c:choose>
            <c:when test="${not empty admin.lastLoginAt}">
              <fmt:formatDate value="${admin.lastLoginAt}" pattern="yyyy/MM/dd HH:mm" />
            </c:when>
            <c:otherwise>—</c:otherwise>
          </c:choose>
        </dd>
      </dl>
    </div>
  </div>

  <form method="post" action="${pageContext.request.contextPath}/admin/mypage/edit_check" class="card p-3 shadow-sm">
    <!-- CSRFトークンを使っている場合はここに hidden を追加 -->
    <div class="row g-3">
      <div class="col-md-6">
        <label class="form-label">メールアドレス <span class="text-danger">*</span></label>
        <input type="email" name="mail" class="form-control" required value="${fn:escapeXml(form.mail)}">
      </div>

      <div class="col-md-6">
        <label class="form-label">氏名 <span class="text-danger">*</span></label>
        <input type="text" name="name" class="form-control" required value="${fn:escapeXml(form.name)}">
      </div>

      <div class="col-md-6">
        <label class="form-label">氏名（ふりがな）</label>
        <input type="text" name="nameRuby" class="form-control" value="${fn:escapeXml(form.nameRuby)}">
      </div>

      <div class="col-md-6">
        <label class="form-label">パスワード</label>
        <input type="password" name="password" class="form-control" placeholder="変更しない場合は空のまま">
        <div class="form-text">空欄のまま送信するとパスワードは変更されません。</div>
      </div>

      <div class="col-12 d-flex justify-content-end gap-2 mt-2">
        <a href="${pageContext.request.contextPath}/admin/mypage/home" class="btn btn-outline-secondary">戻る</a>
        <button type="submit" class="btn btn-primary">確認する</button>
      </div>
    </div>
  </form>

</div>
<script
		src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
