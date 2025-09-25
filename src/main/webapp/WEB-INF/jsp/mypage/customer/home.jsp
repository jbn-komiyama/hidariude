<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <title>顧客マイページ</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10"><!-- ★ 青系背景で統一 -->
<%@ include file="/WEB-INF/jsp/_parts/customer/navbar.jspf" %>

<div class="container py-4">

  <div class="d-flex align-items-center justify-content-between mb-3">
    <h1 class="h4 mb-0">顧客マイページ</h1>
    <a href="<%= request.getContextPath() %>/customer/mypage/edit?id=${cc.id}" class="btn btn-primary btn-sm">編集</a>
  </div>

  <!-- プロフィール -->
  <div class="card shadow-sm mb-4">
    <div class="card-header bg-primary text-white">プロフィール</div>
    <div class="card-body">

      <!-- ヘッダー（簡易アバター＋氏名行） -->
      <div class="d-flex align-items-center mb-3">
        <span class="rounded-circle bg-primary-subtle text-primary d-inline-flex align-items-center justify-content-center me-3 px-3 py-2 fw-bold">
          <c:out value="${empty cc.name ? '?' : fn:substring(cc.name,0,1)}"/>
        </span>
        <div>
          <div class="h5 mb-0"><c:out value="${empty cc.name ? '—' : cc.name}"/></div>
          <div class="text-muted small">
            所属：
            <c:out value="${empty cc.department ? '—' : cc.department}"/>
            ／ ロール：
            <c:choose>
              <c:when test="${cc.primary}">主担当</c:when>
              <c:otherwise>一般</c:otherwise>
            </c:choose>
            <c:if test="${not empty customer}">
              ／ 会社：<c:out value="${empty customer.companyName ? '—' : customer.companyName}"/>
            </c:if>
          </div>
        </div>
      </div>

      <!-- 明細（キー/値テーブル：秘書側と同トーン） -->
      <div class="table-responsive">
        <table class="table table-sm align-middle mb-0">
          <tbody>
            <tr>
              <th class="text-secondary w-25">氏名（ふりがな）</th>
              <td>
                <c:out value="${empty cc.name ? '—' : cc.name}"/>
                <c:if test="${not empty cc.nameRuby}">（<c:out value="${cc.nameRuby}"/>）</c:if>
              </td>
            </tr>
            <tr>
              <th class="text-secondary w-25">メール</th>
              <td>
                <c:choose>
                  <c:when test="${empty cc.mail}">—</c:when>
                  <c:otherwise><a href="mailto:${cc.mail}"><c:out value="${cc.mail}"/></a></c:otherwise>
                </c:choose>
              </td>
            </tr>
            <tr>
              <th class="text-secondary w-25">電話番号</th>
              <td><c:out value="${empty cc.phone ? '—' : cc.phone}"/></td>
            </tr>
            <tr>
              <th class="text-secondary w-25">部署</th>
              <td><c:out value="${empty cc.department ? '—' : cc.department}"/></td>
            </tr>
            <tr>
              <th class="text-secondary w-25">ロール</th>
              <td>
                <c:choose>
                  <c:when test="${cc.primary}">主担当</c:when>
                  <c:otherwise>一般</c:otherwise>
                </c:choose>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

    </div>
  </div>

  <!-- 会社情報 -->
  <div class="card shadow-sm mb-4">
    <div class="card-header bg-primary text-white">会社情報</div>
    <div class="card-body">
      <div class="table-responsive">
        <table class="table table-sm align-middle mb-0">
          <tbody>
            <tr>
              <th class="text-secondary w-25">会社コード</th>
              <td><c:out value="${empty customer.companyCode ? '—' : customer.companyCode}"/></td>
            </tr>
            <tr>
              <th class="text-secondary w-25">会社名</th>
              <td><c:out value="${empty customer.companyName ? '—' : customer.companyName}"/></td>
            </tr>
            <tr>
              <th class="text-secondary w-25">代表メール</th>
              <td>
                <c:choose>
                  <c:when test="${empty customer.mail}">—</c:when>
                  <c:otherwise><a href="mailto:${customer.mail}"><c:out value="${customer.mail}"/></a></c:otherwise>
                </c:choose>
              </td>
            </tr>
            <tr>
              <th class="text-secondary w-25">電話番号</th>
              <td><c:out value="${empty customer.phone ? '—' : customer.phone}"/></td>
            </tr>
            <tr>
              <th class="text-secondary w-25">住所</th>
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
      </div>
    </div>
  </div>

  <!-- 同一会社の他担当者 -->
  <c:if test="${not empty customer.customerContacts}">
    <div class="card shadow-sm mb-4">
      <div class="card-header bg-primary text-white">会社担当者一覧</div>
      <div class="card-body">
        <div class="table-responsive">
          <table class="table table-sm align-middle mb-0">
            <thead class="text-muted">
              <tr>
                <th style="width:48px;"></th>
                <th>氏名</th>
                <th>部署</th>
                <th>メール</th>
                <th>ロール</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="p" items="${customer.customerContacts}">
                <tr>
                  <td class="text-center">
                    <span class="rounded-circle bg-primary-subtle text-primary d-inline-flex align-items-center justify-content-center px-2 py-1 fw-bold">
                      <c:out value="${empty p.name ? '?' : fn:substring(p.name,0,1)}"/>
                    </span>
                  </td>
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
                      <c:when test="${p.primary}">主担当</c:when>
                      <c:otherwise>一般</c:otherwise>
                    </c:choose>
                  </td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </c:if>

  <div class="mt-3 text-end">
    <a href="<%= request.getContextPath() %>/customer/home" class="btn btn-outline-secondary">ホームへ戻る</a>
  </div>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
