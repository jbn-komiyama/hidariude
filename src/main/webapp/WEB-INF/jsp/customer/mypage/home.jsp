<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>顧客マイページ</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
<style>
  .avatar{width:64px;height:64px;border-radius:50%;display:flex;align-items:center;justify-content:center;background:#e9ecef;font-weight:700;font-size:22px;}
  .kv th{width:180px;white-space:nowrap;}
</style>
</head>
<body class="bg-light">
<div class="container py-4">

  <!-- セッションから取得（customer は customerContact の中から参照） -->
  <c:set var="cc" value="${sessionScope.loginUser.customerContact}" />
  <c:set var="customer" value="${cc.customer}" />

  <div class="d-flex align-items-center justify-content-between mb-3">
    <h1 class="h4 mb-0">顧客マイページ</h1>
  </div>

  <div class="card shadow-sm mb-4">
    <div class="card-body">
      <div class="d-flex align-items-center mb-3">
        <div class="avatar me-3">
          <c:out value="${empty cc.name ? '?' : fn:substring(cc.name,0,1)}"/>
        </div>
        <div>
          <div class="h5 mb-0"><c:out value="${empty cc.name ? '—' : cc.name}"/></div>
          <div class="text-muted">
            所属：<c:out value="${empty cc.department ? '—' : cc.department}"/> ／ 主担当：
            <c:choose>
              <c:when test="${cc.primary}">はい</c:when>
              <c:otherwise>いいえ</c:otherwise>
            </c:choose>
            <c:if test="${not empty customer}">
              ／ 会社：<c:out value="${empty customer.companyName ? '—' : customer.companyName}"/>
            </c:if>
          </div>
        </div>
        <div class="ms-auto">
          <a href="<%= request.getContextPath() %>/customer/mypage/edit?id=${cc.id}" class="btn btn-sm btn-primary">編集</a>
        </div>
      </div>

      <h2 class="h6 mt-3">顧客担当者情報</h2>
      <table class="table table-sm kv">
        <tbody>
          <tr>
            <th>氏名（ふりがな）</th>
            <td>
              <c:out value="${empty cc.name ? '—' : cc.name}"/>
              <c:if test="${not empty cc.nameRuby}">（<c:out value="${cc.nameRuby}"/>）</c:if>
            </td>
          </tr>
          <tr>
            <th>メール</th>
            <td>
              <c:choose>
                <c:when test="${empty cc.mail}">—</c:when>
                <c:otherwise><a href="mailto:${cc.mail}"><c:out value="${cc.mail}"/></a></c:otherwise>
              </c:choose>
            </td>
          </tr>
          <tr><th>電話番号</th><td><c:out value="${empty cc.phone ? '—' : cc.phone}"/></td></tr>
          <tr><th>部署</th><td><c:out value="${empty cc.department ? '—' : cc.department}"/></td></tr>
          <tr>
            <th>主担当</th>
            <td>
              <c:choose>
                <c:when test="${cc.primary}">はい</c:when>
                <c:otherwise>いいえ</c:otherwise>
              </c:choose>
            </td>
          </tr>
        </tbody>
      </table>

      <h2 class="h6 mt-4">会社情報</h2>
      <table class="table table-sm kv">
        <tbody>
          <tr><th>会社コード</th><td><c:out value="${empty customer.companyCode ? '—' : customer.companyCode}"/></td></tr>
          <tr><th>会社名</th><td><c:out value="${empty customer.companyName ? '—' : customer.companyName}"/></td></tr>
          <tr>
            <th>代表メール</th>
            <td>
              <c:choose>
                <c:when test="${empty customer.mail}">—</c:when>
                <c:otherwise><a href="mailto:${customer.mail}"><c:out value="${customer.mail}"/></a></c:otherwise>
              </c:choose>
            </td>
          </tr>
          <tr><th>電話番号</th><td><c:out value="${empty customer.phone ? '—' : customer.phone}"/></td></tr>
          <tr>
            <th>住所</th>
            <td>
              <c:choose>
                <c:when test="${empty customer.postalCode and empty customer.address1 and empty customer.address2 and empty customer.building}">
                  —
                </c:when>
                <c:otherwise>
                  <c:if test="${not empty customer.postalCode}">〒<c:out value="${customer.postalCode}"/></c:if>
                  <c:if test="${not empty customer.address1}"> <c:out value="${customer.address1}"/></c:if>
                  <c:if test="${not empty customer.address2}"> <c:out value="${customer.address2}"/></c:if>
                  <c:if test="${not empty customer.building}"> <c:out value="${customer.building}"/></c:if>
                </c:otherwise>
              </c:choose>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 同一会社の他担当者一覧 -->
      <c:if test="${not empty customer.customerContacts}">
        <h2 class="h6 mt-4">この会社の他の担当者</h2>
        <table class="table table-sm align-middle">
          <thead>
            <tr class="text-muted">
              <th style="width:32px;"></th>
              <th>氏名</th>
              <th>部署</th>
              <th>メール</th>
              <th>主担当</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="p" items="${customer.customerContacts}">
              <tr>
                <td class="text-center"><span class="badge bg-secondary"><c:out value="${empty p.name ? '?' : fn:substring(p.name,0,1)}"/></span></td>
                <td><c:out value="${empty p.name ? '—' : p.name}"/></td>
                <td><c:out value="${empty p.department ? '—' : p.department}"/></td>
                <td>
                  <c:choose>
                    <c:when test="${empty p.mail}">—</c:when>
                    <c:otherwise><a href="mailto:${p.mail}"><c:out value="${p.mail}"/></a></c:otherwise>
                  </c:choose>
                </td>
                <td>
                  <c:choose>
                    <c:when test="${p.primary}">はい</c:when>
                    <c:otherwise>いいえ</c:otherwise>
                  </c:choose>
                </td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </c:if>

    </div>
  </div>

  <div class="mt-3 text-end">
    <a href="<%= request.getContextPath() %>/customer/home" class="btn btn-outline-secondary">ホームへ戻る</a>
  </div>

</div>
</body>
</html>
