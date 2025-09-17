<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <title>マイページ</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10"><!-- ★ 青系背景 -->
<%@ include file="/WEB-INF/jsp/_parts/secretary/navbar.jspf" %>

<div class="container py-4">

  <div class="d-flex align-items-center justify-content-between mb-3">
    <h1 class="h4 mb-0">マイページ</h1>
    <a href="<%= request.getContextPath() %>/secretary/mypage/edit?id=${secretary.id}" class="btn btn-primary btn-sm">編集</a>
  </div>

  <div class="card shadow-sm mb-4">
    <div class="card-header bg-primary text-white">プロフィール</div><!-- ★ 見出しを青 -->
    <div class="card-body">

      <!-- ヘッダー部（簡易アバター＋氏名） -->
      <div class="d-flex align-items-center mb-3">
        <span class="rounded-circle bg-primary-subtle text-primary d-inline-flex align-items-center justify-content-center me-3 px-3 py-2 fw-bold">
          <c:out value="${empty secretary.name ? '?' : fn:substring(secretary.name,0,1)}"/>
        </span>
        <div>
          <div class="h5 mb-0"><c:out value="${secretary.name}"/></div>
          <div class="text-muted small">
            ランク：
            <c:out value="${secretary.secretaryRank != null ? secretary.secretaryRank.rankName : '—'}"/>
            ／ PM対応：
            <c:choose>
              <c:when test="${secretary.pmSecretary}">可</c:when>
              <c:otherwise>不可</c:otherwise>
            </c:choose>
          </div>
        </div>
      </div>

      <!-- 明細（キー/値テーブル） -->
      <div class="table-responsive">
        <table class="table table-sm align-middle mb-0">
          <tbody>
            <tr>
              <th class="text-secondary w-25">秘書コード</th>
              <td><c:out value="${empty secretary.secretaryCode ? '—' : secretary.secretaryCode}"/></td>
            </tr>
            <tr>
              <th class="text-secondary w-25">氏名（ふりがな）</th>
              <td><c:out value="${empty secretary.nameRuby ? '—' : secretary.nameRuby}"/></td>
            </tr>
            <tr>
              <th class="text-secondary w-25">ランク</th>
              <td><c:out value="${secretary.secretaryRank != null ? secretary.secretaryRank.rankName : '—'}"/></td>
            </tr>
            <tr>
              <th class="text-secondary w-25">PM秘書</th>
              <td>
                <c:choose>
                  <c:when test="${secretary.pmSecretary}">可</c:when>
                  <c:otherwise>不可</c:otherwise>
                </c:choose>
              </td>
            </tr>
            <tr>
              <th class="text-secondary w-25">メール</th>
              <td>
                <a href="mailto:${secretary.mail}">
                  <c:out value="${secretary.mail}"/>
                </a>
              </td>
            </tr>
            <tr>
              <th class="text-secondary w-25">電話番号</th>
              <td><c:out value="${empty secretary.phone ? '—' : secretary.phone}"/></td>
            </tr>
            <tr>
              <th class="text-secondary w-25">住所</th>
              <td>
                <c:choose>
                  <c:when test="${empty secretary.postalCode and empty secretary.address1 and empty secretary.address2 and empty secretary.building}">
                    —
                  </c:when>
                  <c:otherwise>
                    <c:if test="${not empty secretary.postalCode}">〒<c:out value="${secretary.postalCode}"/></c:if>
                    <c:if test="${not empty secretary.address1}"> <c:out value="${secretary.address1}"/></c:if>
                    <c:if test="${not empty secretary.address2}"> <c:out value="${secretary.address2}"/></c:if>
                    <c:if test="${not empty secretary.building}"> <c:out value="${secretary.building}"/></c:if>
                  </c:otherwise>
                </c:choose>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

    </div>
  </div>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
