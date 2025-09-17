<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>担当者一覧</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf" %>
  <div class="container py-4">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div>
        <h1 class="h3 mb-0">担当者一覧</h1>
        <div class="text-muted small">
          顧客：<span class="fw-semibold"><c:out value="${customer.companyName}"/></span>
          <c:if test="${not empty customer.companyCode}">
            （<c:out value="${customer.companyCode}"/>）
          </c:if>
        </div>
      </div>
      <form method="post" action="<%= request.getContextPath() %>/admin/contact/register" class="m-0">
        <input type="hidden" name="customerId" value="${customer.id}">
        <button type="submit" class="btn btn-primary">
          新規担当者
        </button>
      </form>
    </div>
    <c:if test="${not empty errorMsg}">
      <div class="alert alert-danger" role="alert">${errorMsg}</div>
    </c:if>

    <div class="card shadow-sm">
      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-hover table-bordered align-middle mb-0">
            <thead class="table-primary">
              <tr>
                <th style="width:72px;">No.</th>
                <th>氏名</th>
                <th>部署</th>
                <th>連絡先</th>
                <th style="width:110px;">主担当</th>
                <th style="width:160px;">操作</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="ct" items="${contacts}" varStatus="st">
                <tr>
                  <td class="text-center fw-semibold">${st.index + 1}</td>
                  <td class="text-nowrap">
                    <div class="fw-semibold"><c:out value="${ct.name}"/></div>
                    <div class="small text-muted"><c:out value="${ct.nameRuby}"/></div>
                  </td>
                  <td class="text-nowrap"><c:out value="${ct.department}"/></td>
                  <td class="text-break">
                    <c:if test="${not empty ct.mail}">
                      <a href="mailto:${ct.mail}">${ct.mail}</a><br/>
                    </c:if>
                    <c:out value="${ct.phone}"/>
                  </td>
                  <td class="text-center">
                    <c:choose>
                      <c:when test="${ct.primary}">
                        <span class="badge text-bg-success">主担当</span>
                      </c:when>
                      <c:otherwise>
                        <span class="badge text-bg-secondary">—</span>
                      </c:otherwise>
                    </c:choose>
                  </td>
                  <td class="text-center">
                    <div class="d-flex gap-1 justify-content-center">
                      <form method="post" action="<%= request.getContextPath() %>/admin/contact/edit" class="m-0">
                        <input type="hidden" name="customerId" value="${customer.id}">
                        <input type="hidden" name="id" value="${ct.id}">
                        <button type="submit" class="btn btn-sm btn-primary">編集</button>
                      </form>
                      <form method="post" action="<%= request.getContextPath() %>/admin/contact/delete" class="m-0"
                            onsubmit="return confirm('本当に削除しますか？');">
                        <input type="hidden" name="customerId" value="${customer.id}">
                        <input type="hidden" name="id" value="${ct.id}">
                        <button type="submit" class="btn btn-sm btn-danger">削除</button>
                      </form>
                    </div>
                  </td>
                </tr>
              </c:forEach>
              <c:if test="${empty contacts}">
                <tr>
                  <td colspan="6" class="text-center text-muted py-4">担当者が登録されていません。</td>
                </tr>
              </c:if>
            </tbody>
          </table>
        </div>
      </div>
      <div class="card-footer text-end small text-muted">
        件数：<span class="fw-semibold"><c:out value="${fn:length(contacts)}"/></span>
      </div>
    </div>

    <div class="mt-3">
      <a href="<%= request.getContextPath() %>/admin/customer" class="btn btn-outline-secondary btn-sm">顧客一覧へ戻る</a>
    </div>
  </div>
  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>