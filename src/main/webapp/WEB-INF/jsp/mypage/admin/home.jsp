<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8" />
  <title>マイページ（管理者）</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf" %>

<div class="container py-4">

  <h1 class="h4 mb-4">マイページ（管理者）</h1>

  <c:if test="${not empty successMsg}">
    <div class="alert alert-success">${successMsg}</div>
  </c:if>
  <c:if test="${not empty errorMsg}">
    <div class="alert alert-danger">${errorMsg}</div>
  </c:if>

  <div class="card shadow-sm">
    <div class="card-body">
      <dl class="row mb-0">
        <dt class="col-sm-3">管理者ID</dt>
        <dd class="col-sm-9"><c:out value="${admin.id}"/></dd>

        <dt class="col-sm-3">メールアドレス</dt>
        <dd class="col-sm-9"><c:out value="${admin.mail}"/></dd>

        <dt class="col-sm-3">パスワード</dt>
        <dd class="col-sm-9"><span class="text-muted">********（非表示）</span></dd>

        <dt class="col-sm-3">氏名</dt>
        <dd class="col-sm-9"><c:out value="${admin.name}"/></dd>

        <dt class="col-sm-3">氏名（かな）</dt>
        <dd class="col-sm-9"><c:out value="${admin.nameRuby}"/></dd>

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

  <div class="mt-3 d-flex gap-2">
    <a href="${pageContext.request.contextPath}/admin/mypage/edit" class="btn btn-primary">編集する</a>
  </div>

</div>
<script
		src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
