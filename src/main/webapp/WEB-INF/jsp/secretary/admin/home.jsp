<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>秘書一覧</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf" %>
  <div class="container py-4">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h1 class="h3 mb-0">秘書一覧</h1>
      <form method="post" action="<%= request.getContextPath() %>/admin/secretary/register" class="m-0">
        <!-- 青系に統一 -->
        <button type="submit" class="btn btn-primary">新規登録</button>
      </form>
    </div>

    <c:if test="${not empty errorMsg}">
      <div class="alert alert-danger">${errorMsg}</div>
    </c:if>

    <div class="card shadow-sm">
      <div class="table-responsive">
        <table class="table table-hover table-bordered align-middle mb-0">
          <thead class="table-primary">
            <tr>
              <th style="width:72px;">No.</th>
              <th>秘書コード</th>
              <th>氏名</th>
              <th>ランク</th>
              <th>PM対応</th>
              <th>連絡先</th>
              <th style="width:220px;">操作</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="sec" items="${secretaries}" varStatus="st">
              <tr>
                <td class="text-center fw-semibold">${st.index + 1}</td>
                <td class="text-nowrap">${sec.secretaryCode}</td>
                <td class="text-nowrap"><a href="<%= request.getContextPath() %>/admin/secretary/detail?id=${sec.id}">${sec.name}</a> <span class="text-muted small">${sec.nameRuby}</span></td>
                <td class="text-nowrap"><c:out value="${sec.secretaryRank.rankName}"/></td>
                <td class="text-center">
                  <c:choose>
                    <c:when test="${sec.pmSecretary}"><span class="badge bg-success">可</span></c:when>
                    <c:otherwise><span class="badge bg-secondary">不可</span></c:otherwise>
                  </c:choose>
                </td>
                <td class="text-break">
                  <c:if test="${not empty sec.mail}">
                    <a href="mailto:${sec.mail}">${sec.mail}</a><br/>
                  </c:if>
                  ${sec.phone}<br/>
                  <c:if test="${not empty sec.postalCode}">〒${sec.postalCode}<br/></c:if>
                  ${sec.address1} <c:out value="${sec.address2}"/><br/>
                  <c:out value="${sec.building}"/>
                </td>
                <td class="text-center">
                  <div class="d-flex flex-wrap gap-1 justify-content-center">
                    <!-- 追加：詳細（青系、GETリンク） -->
                    <a class="btn btn-sm btn-primary"
                       href="<%= request.getContextPath() %>/admin/secretary/detail?id=${sec.id}">
                      詳細
                    </a>
                    <!-- 既存：編集（青系に統一：outline） -->
                    <form method="post" action="<%= request.getContextPath() %>/admin/secretary/edit" class="m-0">
                      <input type="hidden" name="id" value="${sec.id}">
                      <button type="submit" class="btn btn-sm btn-outline-primary">編集</button>
                    </form>
                    <!-- 既存：削除（青系に統一：outline） -->
                    <form method="post" action="<%= request.getContextPath() %>/admin/secretary/delete" class="m-0" onsubmit="return confirm('本当に削除しますか？');">
                      <input type="hidden" name="id" value="${sec.id}">
                      <button type="submit" class="btn btn-sm btn-outline-primary">削除</button>
                    </form>
                  </div>
                </td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </div>
      <div class="card-footer text-end small text-muted">
        件数：<span class="fw-semibold"><c:out value="${fn:length(secretaries)}"/></span>
      </div>
    </div>
  </div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
