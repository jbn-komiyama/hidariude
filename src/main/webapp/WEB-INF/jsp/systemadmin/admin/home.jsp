<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>システム管理者一覧</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf" %>
  <div class="container py-4">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h1 class="h3 mb-0">システム管理者一覧</h1>
      <form method="post" action="<%= request.getContextPath() %>/admin/system_admin/register" class="m-0">
        <button type="submit" class="btn btn-primary">新規登録</button>
      </form>
    </div>

    <c:if test="${not empty errorMsg}">
      <div class="alert alert-danger" role="alert">${errorMsg}</div>
    </c:if>

    <div class="card shadow-sm">
      <div class="table-responsive">
        <table class="table table-hover table-bordered align-middle mb-0">
            <thead class="table-primary">
              <tr>
                <th style="width:72px;">No.</th>
                <th>氏名</th>
                <th>ふりがな</th>
                <th>メール</th>
                <th style="width:160px;">操作</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="adm" items="${admins}" varStatus="st">
                <tr>
                  <td class="text-center fw-semibold">${st.index + 1}</td>
                  <td class="text-nowrap"><c:out value="${adm.name}"/></td>
                  <td class="text-nowrap"><c:out value="${adm.nameRuby}"/></td>
                  <td class="text-break"><a href="mailto:${adm.mail}"><c:out value="${adm.mail}"/></a></td>
                  <td class="text-center">
                    <div class="d-flex gap-1 justify-content-center">
                      <form method="post" action="<%= request.getContextPath() %>/admin/system_admin/edit" class="m-0">
                        <input type="hidden" name="id" value="${adm.id}">
                        <button type="submit" class="btn btn-sm btn-primary">編集</button>
                      </form>
                      <form method="post" action="<%= request.getContextPath() %>/admin/system_admin/delete" class="m-0" onsubmit="return confirm('本当に削除しますか？');">
                        <input type="hidden" name="id" value="${adm.id}">
                        <button type="submit" class="btn btn-sm btn-danger">削除</button>
                      </form>
                    </div>
                  </td>
                </tr>
              </c:forEach>
              <c:if test="${empty admins}">
                <tr>
                  <td colspan="5" class="text-center text-muted py-4">管理者が登録されていません。</td>
                </tr>
              </c:if>
            </tbody>
          </table>
        </div>
      <div class="card-footer text-end small text-muted">
        件数：<span class="fw-semibold"><c:out value="${fn:length(admins)}"/></span>
      </div>
    </div>
  </div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>