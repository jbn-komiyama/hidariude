<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>顧客 登録確認</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <c:set var="vCompanyCode" value="${empty companyCode ? param.companyCode : companyCode}" />
  <c:set var="vCompanyName" value="${empty companyName ? param.companyName : companyName}" />
  <c:set var="vMail"        value="${empty mail ? param.mail : mail}" />
  <c:set var="vPhone"       value="${empty phone ? param.phone : phone}" />
  <c:set var="vPostalCode"  value="${empty postalCode ? param.postalCode : postalCode}" />
  <c:set var="vAddress1"    value="${empty address1 ? param.address1 : address1}" />
  <c:set var="vAddress2"    value="${empty address2 ? param.address2 : address2}" />
  <c:set var="vBuilding"    value="${empty building ? param.building : building}" />

  <div class="container py-4">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h1 class="h3 mb-0">顧客 登録確認</h1>
      <a class="btn btn-outline-secondary" href="<%= request.getContextPath() %>/admin/customer">一覧へ戻る</a>
    </div>

    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <dl class="row">
          <dt class="col-sm-3">会社コード</dt>
          <dd class="col-sm-9"><c:out value="${vCompanyCode}"/></dd>

          <dt class="col-sm-3">会社名</dt>
          <dd class="col-sm-9"><strong><c:out value="${vCompanyName}"/></strong></dd>

          <dt class="col-sm-3">メール</dt>
          <dd class="col-sm-9"><c:out value="${vMail}"/></dd>

          <dt class="col-sm-3">電話番号</dt>
          <dd class="col-sm-9"><c:out value="${vPhone}"/></dd>

          <dt class="col-sm-3">郵便番号</dt>
          <dd class="col-sm-9">〒<c:out value="${vPostalCode}"/></dd>

          <dt class="col-sm-3">住所1</dt>
          <dd class="col-sm-9"><c:out value="${vAddress1}"/></dd>

          <dt class="col-sm-3">住所2</dt>
          <dd class="col-sm-9"><c:out value="${vAddress2}"/></dd>

          <dt class="col-sm-3">ビル名</dt>
          <dd class="col-sm-9"><c:out value="${vBuilding}"/></dd>
        </dl>
      </div>
    </div>

    <div class="d-flex gap-2">
      <!-- 修正：履歴で戻る（フォーム値保持のため） -->
      <button type="button" class="btn btn-outline-secondary" onclick="history.back();">修正する</button>

      <!-- 確定：登録実行 -->
      <form method="post" action="<%= request.getContextPath() %>/admin/customer/register_done" class="m-0">
        <input type="hidden" name="companyCode" value="${vCompanyCode}">
        <input type="hidden" name="companyName" value="${vCompanyName}">
        <input type="hidden" name="mail"        value="${vMail}">
        <input type="hidden" name="phone"       value="${vPhone}">
        <input type="hidden" name="postalCode"  value="${vPostalCode}">
        <input type="hidden" name="address1"    value="${vAddress1}">
        <input type="hidden" name="address2"    value="${vAddress2}">
        <input type="hidden" name="building"    value="${vBuilding}">
        <button type="submit" class="btn btn-primary">この内容で登録する</button>
      </form>
    </div>
  </div>
</body>
</html>