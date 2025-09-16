<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>担当者 変更（確認）</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf" %>
  <div class="container py-4">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div>
        <h1 class="h3 mb-0">担当者 変更（確認）</h1>
        <div class="text-muted small">
          顧客：<span class="fw-semibold"><c:out value="${customer.companyName}"/></span>
        </div>
      </div>
      <a href="<%= request.getContextPath() %>/admin/contact?customerId=${customer.id}" class="btn btn-outline-secondary btn-sm">一覧へ戻る</a>
    </div>

    <div class="card shadow-sm">
      <div class="card-body">
        <dl class="row mb-0">
          <dt class="col-sm-3">氏名</dt>
          <dd class="col-sm-9"><c:out value="${name}"/></dd>

          <dt class="col-sm-3">ふりがな</dt>
          <dd class="col-sm-9"><c:out value="${nameRuby}"/></dd>

          <dt class="col-sm-3">部署</dt>
          <dd class="col-sm-9"><c:out value="${department}"/></dd>

          <dt class="col-sm-3">メール</dt>
          <dd class="col-sm-9"><c:out value="${mail}"/></dd>

          <dt class="col-sm-3">電話番号</dt>
          <dd class="col-sm-9"><c:out value="${phone}"/></dd>

          <dt class="col-sm-3">主担当</dt>
          <dd class="col-sm-9">
            <c:choose>
              <c:when test="${isPrimary == 'true'}">主担当に設定</c:when>
              <c:otherwise>—</c:otherwise>
            </c:choose>
          </dd>
        </dl>

        <div class="d-flex justify-content-end gap-2 mt-4">
          <form method="post" action="<%= request.getContextPath() %>/admin/contact/edit" class="m-0">
            <input type="hidden" name="customerId" value="${customer.id}">
            <input type="hidden" name="id" value="${id}">
            <input type="hidden" name="name" value="${name}">
            <input type="hidden" name="nameRuby" value="${nameRuby}">
            <input type="hidden" name="department" value="${department}">
            <input type="hidden" name="mail" value="${mail}">
            <input type="hidden" name="phone" value="${phone}">
            <input type="hidden" name="isPrimary" value="${isPrimary}">
            <button type="submit" class="btn btn-outline-secondary">戻る</button>
          </form>

          <form method="post" action="<%= request.getContextPath() %>/admin/contact/edit_done" class="m-0">
            <input type="hidden" name="customerId" value="${customer.id}">
            <input type="hidden" name="id" value="${id}">
            <input type="hidden" name="name" value="${name}">
            <input type="hidden" name="nameRuby" value="${nameRuby}">
            <input type="hidden" name="department" value="${department}">
            <input type="hidden" name="mail" value="${mail}">
            <input type="hidden" name="phone" value="${phone}">
            <input type="hidden" name="isPrimary" value="${isPrimary}">
            <button type="submit" class="btn btn-primary">更新する</button>
          </form>
        </div>
      </div>
    </div>

  </div>
  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>