<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <title>マイページ</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/secretary/navbar.jspf" %>

<div class="container py-4">
  <div class="d-flex align-items-center justify-content-between mb-3">
    <h1 class="h4 mb-0">マイページ</h1>
    <a href="<%= request.getContextPath() %>/secretary/mypage/edit" class="btn btn-primary btn-sm">編集</a>
  </div>

  <!-- 基本情報 -->
  <div class="card border-primary mb-3">
    <div class="card-header bg-primary text-white">基本情報</div>
    <div class="card-body">
      <dl class="row mb-0">
        <dt class="col-sm-3">秘書コード</dt>
        <dd class="col-sm-9"><c:out value="${empty secretary.secretaryCode ? '—' : secretary.secretaryCode}"/></dd>

        <dt class="col-sm-3">氏名</dt>
        <dd class="col-sm-9"><c:out value="${empty secretary.name ? '—' : secretary.name}"/></dd>

        <dt class="col-sm-3">氏名（ふりがな）</dt>
        <dd class="col-sm-9"><c:out value="${empty secretary.nameRuby ? '—' : secretary.nameRuby}"/></dd>

        <dt class="col-sm-3">ランク</dt>
        <dd class="col-sm-9">
          <c:out value="${secretary.secretaryRank != null ? secretary.secretaryRank.rankName : '—'}"/>
        </dd>

        <dt class="col-sm-3">PM対応</dt>
        <dd class="col-sm-9">
          <c:choose>
            <c:when test="${secretary.pmSecretary}">可</c:when>
            <c:otherwise>不可</c:otherwise>
          </c:choose>
        </dd>
      </dl>
    </div>
  </div>

  <!-- 連絡先 -->
  <div class="card border-primary mb-3">
    <div class="card-header bg-primary text-white">連絡先</div>
    <div class="card-body">
      <dl class="row mb-0">
        <dt class="col-sm-3">メール</dt>
        <dd class="col-sm-9">
          <c:choose>
            <c:when test="${not empty secretary.mail}">
              <a href="mailto:${secretary.mail}"><c:out value="${secretary.mail}"/></a>
            </c:when>
            <c:otherwise>—</c:otherwise>
          </c:choose>
        </dd>

        <dt class="col-sm-3">電話番号</dt>
        <dd class="col-sm-9"><c:out value="${empty secretary.phone ? '—' : secretary.phone}"/></dd>
      </dl>
    </div>
  </div>

  <!-- 住所 -->
  <div class="card border-primary mb-3">
    <div class="card-header bg-primary text-white">住所</div>
    <div class="card-body">
      <dl class="row mb-0">
        <dt class="col-sm-3">郵便番号</dt>
        <dd class="col-sm-9">
          <c:choose>
            <c:when test="${not empty secretary.postalCode}">〒<c:out value="${secretary.postalCode}"/></c:when>
            <c:otherwise>—</c:otherwise>
          </c:choose>
        </dd>

        <dt class="col-sm-3">住所1</dt>
        <dd class="col-sm-9"><c:out value="${empty secretary.address1 ? '—' : secretary.address1}"/></dd>

        <dt class="col-sm-3">住所2</dt>
        <dd class="col-sm-9"><c:out value="${empty secretary.address2 ? '—' : secretary.address2}"/></dd>

        <dt class="col-sm-3">建物名</dt>
        <dd class="col-sm-9"><c:out value="${empty secretary.building ? '—' : secretary.building}"/></dd>
      </dl>
    </div>
  </div>

  <!-- 口座情報 -->
  <div class="card border-primary mb-4">
    <div class="card-header bg-primary text-white">口座情報</div>
    <div class="card-body">
      <dl class="row mb-0">
        <dt class="col-sm-3">銀行名</dt>
        <dd class="col-sm-9"><c:out value="${empty secretary.bankName ? '—' : secretary.bankName}"/></dd>

        <dt class="col-sm-3">支店名</dt>
        <dd class="col-sm-9"><c:out value="${empty secretary.bankBranch ? '—' : secretary.bankBranch}"/></dd>

        <dt class="col-sm-3">種別</dt>
        <dd class="col-sm-9"><c:out value="${empty secretary.bankType ? '—' : secretary.bankType}"/></dd>

        <dt class="col-sm-3">口座番号</dt>
        <dd class="col-sm-9"><c:out value="${empty secretary.bankAccount ? '—' : secretary.bankAccount}"/></dd>

        <dt class="col-sm-3">口座名義</dt>
        <dd class="col-sm-9"><c:out value="${empty secretary.bankOwner ? '—' : secretary.bankOwner}"/></dd>
      </dl>
    </div>
  </div>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
