<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>担当者 編集</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <div class="container py-4">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div>
        <h1 class="h3 mb-0">担当者 編集</h1>
        <div class="text-muted small">
          顧客：<span class="fw-semibold"><c:out value="${customer.companyName}"/></span>
        </div>
      </div>
      <a href="<%= request.getContextPath() %>/admin/contact?customerId=${customer.id}" class="btn btn-outline-secondary btn-sm">一覧へ戻る</a>
    </div>

    <c:if test="${not empty errorMsg}">
      <div class="alert alert-danger" role="alert">${errorMsg}</div>
    </c:if>

    <div class="card shadow-sm">
      <div class="card-body">
        <form method="post" action="<%= request.getContextPath() %>/admin/contact/edit_check" class="row g-3">
          <input type="hidden" name="customerId" value="${customer.id}">
          <input type="hidden" name="id" value="${contact.id}">

          <div class="col-md-6">
            <label class="form-label">氏名 <span class="text-danger">*</span></label>
            <input type="text" name="name" class="form-control" value="${name != null ? name : contact.name}" required>
          </div>
          <div class="col-md-6">
            <label class="form-label">氏名（ふりがな）</label>
            <input type="text" name="nameRuby" class="form-control" value="${nameRuby != null ? nameRuby : contact.nameRuby}">
          </div>

          <div class="col-md-6">
            <label class="form-label">部署</label>
            <input type="text" name="department" class="form-control" value="${department != null ? department : contact.department}">
          </div>

          <div class="col-md-6">
            <label class="form-label">メール <span class="text-danger">*</span></label>
            <input type="email" name="mail" class="form-control" value="${mail != null ? mail : contact.mail}" required> <!-- ★ CHANGED -->
          </div>

          <div class="col-md-6">
            <label class="form-label">電話番号</label>
            <input type="text" name="phone" class="form-control" value="${phone != null ? phone : contact.phone}">
          </div>

          <div class="col-md-6">
            <div class="form-check mt-4 pt-2">
              <input class="form-check-input" type="checkbox" name="isPrimary" value="true" id="isPrimary"
                     <c:if test="${(isPrimary == 'true') or (empty isPrimary and contact.primary)}">checked</c:if>>
              <label class="form-check-label" for="isPrimary">主担当に設定する</label>
            </div>
          </div>

          <div class="col-12 text-end">
            <button type="submit" class="btn btn-primary">確認へ</button>
          </div>
        </form>
      </div>
    </div>

  </div>
</body>
</html>