<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>マイページ</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
  <style>
    .avatar {
      width:64px; height:64px; border-radius:50%;
      display:flex; align-items:center; justify-content:center;
      background:#e9ecef; font-weight:700; font-size:22px;
    }
    .kv th{width:160px; white-space:nowrap;}
    .ym-nav .btn-link { line-height:1; }
    .ym-nav .btn-link.js-today { color:#0d6efd; }
    .ym-nav .btn-link.text-muted { color:#6c757d !important; }
  </style>
</head>
<body class="bg-light">
<div class="container py-4">

  <div class="d-flex align-items-center justify-content-between mb-3">
    <h1 class="h4 mb-0">マイページ</h1>
  </div>

  <div class="card shadow-sm mb-4">
    <div class="card-body">
      <div class="d-flex align-items-center mb-3">
        <div class="avatar me-3">
          <c:out value="${empty secretary.name ? '?' : fn:substring(secretary.name,0,1)}"/>
        </div>
        <div>
          <div class="h5 mb-0"><c:out value="${secretary.name}"/></div>
          <div class="text-muted">
            ランク：
            <c:out value="${secretary.secretaryRank != null ? secretary.secretaryRank.rankName : '—'}"/>
            ／ PM対応：
            <c:choose>
              <c:when test="${secretary.pmSecretary}">可</c:when>
              <c:otherwise>不可</c:otherwise>
            </c:choose>
          </div>
        </div>
        <div class="ms-auto">
          <a href="<%= request.getContextPath() %>/secretary/mypage/edit?id=${secretary.id}" class="btn btn-sm btn-primary">編集</a>
        </div>
      </div>

      <table class="table table-sm kv">
        <tbody>
        <tr><th>秘書コード</th><td><c:out value="${empty secretary.secretaryCode ? '—' : secretary.secretaryCode}"/></td></tr>
        <tr><th>氏名（ふりがな）</th><td><c:out value="${empty secretary.nameRuby ? '—' : secretary.nameRuby}"/></td></tr>
        <tr><th>ランク</th><td><c:out value="${secretary.secretaryRank != null ? secretary.secretaryRank.rankName : '—'}"/></td></tr>
        <tr><th>PM秘書</th><td><c:choose><c:when test="${secretary.pmSecretary}">可</c:when> <c:otherwise>不可</c:otherwise> </c:choose>
  </td>
</tr>
        <tr><th>メール</th><td><a href="mailto:${secretary.mail}"><c:out value="${secretary.mail}"/></a></td></tr>
        <tr><th>電話番号</th><td><c:out value="${empty secretary.phone ? '—' : secretary.phone}"/></td></tr>
        <tr><th>住所</th>
          <td>
            <c:out value="${empty secretary.postalCode ? '' : '〒' += secretary.postalCode}"/>
            <c:out value="${empty secretary.address1 ? '' : ' ' += secretary.address1}"/>
            <c:out value="${empty secretary.address2 ? '' : ' ' += secretary.address2}"/>
            <c:out value="${empty secretary.building ? '' : ' ' += secretary.building}"/>
            <c:if test="${empty secretary.postalCode and empty secretary.address1 and empty secretary.address2 and empty secretary.building}">—</c:if>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
<div class="mt-3 text-end">
        <a href="<%= request.getContextPath() %>/secretary/home" class="btn btn-outline-secondary">ホームへ戻る</a>
      </div>
    </form>
</div>
</body>
</html>