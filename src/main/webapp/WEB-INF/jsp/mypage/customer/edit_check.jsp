<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<title>顧客 マイページ編集（確認）</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light"><!-- ★ 秘書側と統一 -->
<%@ include file="/WEB-INF/jsp/_parts/customer/navbar.jspf" %>

<div class="container py-4">
  <h1 class="h3 mb-3">顧客 マイページ編集（確認）</h1>

  <!-- ★ エラーブロック（秘書側と同様） -->
  <c:if test="${not empty errorMsg}">
    <div class="alert alert-danger">
      <ul class="mb-0">
        <c:forEach var="m" items="${errorMsg}">
          <li><c:out value="${m}"/></li>
        </c:forEach>
      </ul>
    </div>
  </c:if>

  <div class="card shadow-sm">
    <div class="card-body">
      <h2 class="h6">顧客担当者情報</h2>
      <dl class="row mb-3">
        <dt class="col-sm-3">氏名</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.contactName ? param.contactName : contactName}"/></dd>

        <dt class="col-sm-3">氏名（ふりがな）</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.contactNameRuby ? param.contactNameRuby : contactNameRuby}"/></dd>

        <dt class="col-sm-3">部署</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.contactDepartment ? param.contactDepartment : contactDepartment}"/></dd>

        <dt class="col-sm-3">メール</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.contactMail ? param.contactMail : contactMail}"/></dd>

        <dt class="col-sm-3">電話番号</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.contactPhone ? param.contactPhone : contactPhone}"/></dd>
      </dl>

      <h2 class="h6">会社情報</h2>
      <dl class="row mb-0">
        <dt class="col-sm-3">会社名</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.companyName ? param.companyName : companyName}"/></dd>

        <dt class="col-sm-3">代表メール</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.companyMail ? param.companyMail : companyMail}"/></dd>

        <dt class="col-sm-3">電話番号</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.companyPhone ? param.companyPhone : companyPhone}"/></dd>

        <dt class="col-sm-3">郵便番号</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.postalCode ? param.postalCode : postalCode}"/></dd>

        <dt class="col-sm-3">住所</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.address1 ? param.address1 : address1}"/></dd>

        <dt class="col-sm-3">住所2</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.address2 ? param.address2 : address2}"/></dd>

        <dt class="col-sm-3">建物名</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.building ? param.building : building}"/></dd>
      </dl>
    </div>

    <!-- ★ フッターのボタン配置も秘書側と同じ -->
    <div class="card-footer d-flex justify-content-between">
      <!-- 修正へ戻る（入力値を持ち回り） -->
      <form method="post" action="${pageContext.request.contextPath}/customer/mypage/edit" class="mb-0">
        <c:set var="v" value="${param}"/>
        <c:forEach var="k" items="${['contactName','contactNameRuby','contactDepartment','contactMail','contactPhone','companyName','companyMail','companyPhone','postalCode','address1','address2','building']}">
          <input type="hidden" name="${k}" value="${v[k]}"/>
        </c:forEach>
        <button type="submit" class="btn btn-outline-secondary">修正する</button>
      </form>

      <!-- 確定 -->
      <form method="post" action="${pageContext.request.contextPath}/customer/mypage/edit_done" class="mb-0">
        <c:set var="v2" value="${param}"/>
        <c:forEach var="k" items="${['contactName','contactNameRuby','contactDepartment','contactMail','contactPhone','companyName','companyMail','companyPhone','postalCode','address1','address2','building']}">
          <input type="hidden" name="${k}" value="${v2[k]}"/>
        </c:forEach>
        <button type="submit" class="btn btn-primary">更新する</button>
      </form>
    </div>
  </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
