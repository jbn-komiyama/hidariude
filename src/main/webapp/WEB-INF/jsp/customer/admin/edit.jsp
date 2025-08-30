<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>顧客 編集</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <div class="container py-4">

    <div class="d-flex align-items-center justify-content-between mb-3">
      <h1 class="h3 mb-0">顧客 編集</h1>
      <a class="btn btn-outline-secondary" href="<%= request.getContextPath() %>/admin/customer">一覧へ戻る</a>
    </div>

    <c:if test="${not empty errorMsg}">
      <div class="alert alert-danger">${errorMsg}</div>
    </c:if>

    <div class="card shadow-sm">
      <div class="card-body">
        <form method="post" action="<%= request.getContextPath() %>/admin/customer/edit_check"> 
          <input type="hidden" name="id" value="${customer.id}"/>

          <div class="row g-3">

            <div class="col-md-4">
              <label class="form-label">会社コード</label>
              <input type="text" name="companyCode" class="form-control" value="${customer.companyCode}">
            </div>

            <div class="col-md-8">
              <label class="form-label">会社名 <span class="text-danger">*</span></label>
              <input type="text" name="companyName" class="form-control" value="${customer.companyName}" required>
            </div>

            <div class="col-md-6">
              <label class="form-label">メール</label>
              <input type="email" name="mail" class="form-control" value="${customer.mail}">
            </div>

            <div class="col-md-6">
              <label class="form-label">電話番号</label>
              <input type="text" name="phone" class="form-control" value="${customer.phone}">
            </div>

            <div class="col-md-3">
              <label class="form-label">郵便番号</label>
              <input type="text" name="postalCode" class="form-control" value="${customer.postalCode}">
            </div>

            <div class="col-md-9">
              <label class="form-label">住所1（都道府県・市区町村）</label>
              <input type="text" name="address1" class="form-control" value="${customer.address1}">
            </div>

            <div class="col-md-6">
              <label class="form-label">住所2（番地・建物名等）</label>
              <input type="text" name="address2" class="form-control" value="${customer.address2}">
            </div>

            <div class="col-md-6">
              <label class="form-label">ビル名</label>
              <input type="text" name="building" class="form-control" value="${customer.building}">
            </div>

          </div>

          <div class="mt-4 d-flex gap-2">
            <button type="submit" class="btn btn-primary">更新する</button>
            <a class="btn btn-outline-secondary" href="<%= request.getContextPath() %>/admin/customer">キャンセル</a>
          </div>
        </form>
      </div>
    </div>

  </div>
</body>
</html>