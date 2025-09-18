<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <title>売上サマリー（顧客×月）</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf" %>

  <div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h1 class="h4 text-primary mb-0">売上サマリー（顧客×月）</h1>
      <form class="d-flex gap-2" method="get" action="<%= request.getContextPath() %>/admin/summary/sales">
        <div class="input-group">
          <span class="input-group-text bg-primary text-white border-primary">年度</span>
          <input type="number" class="form-control" name="fy" value="${fy}" min="2000" max="2100">
        </div>
        <button class="btn btn-primary">表示</button>
      </form>
    </div>

    <c:if test="${not empty errorMsg}">
      <div class="alert alert-danger">${errorMsg}</div>
    </c:if>

    <div class="card border-primary shadow-sm">
      <div class="card-header bg-primary text-white">
        <div class="d-flex justify-content-between">
          <span>会計年度：<strong>${fy}</strong>（${months[0]} ～ ${months[11]}）</span>
        </div>
      </div>

      <div class="table-responsive">
        <table class="table table-bordered table-hover align-middle mb-0 text-nowrap">
          <thead class="table-primary">
            <tr>
              <th style="min-width:220px;">顧客</th>
              <c:forEach var="m" items="${months}">
                <th class="text-center" style="min-width:120px;">${m}</th>
              </c:forEach>
              <th class="text-center" style="min-width:140px;">行合計</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="r" items="${rows}">
              <tr>
                <th class="bg-primary-subtle">${r.label}</th>
                <c:forEach var="m" items="${months}">
                  <td class="text-end">
                    <fmt:formatNumber value="${r.amountByYm[m]}" pattern="#,##0"/>
                  </td>
                </c:forEach>
                <td class="text-end fw-bold">
                  <fmt:formatNumber value="${r.rowTotal}" pattern="#,##0"/>
                </td>
              </tr>
            </c:forEach>
          </tbody>
          <tfoot class="table-primary">
            <tr>
              <th>列合計</th>
              <c:forEach var="m" items="${months}">
                <td class="text-end fw-bold">
                  <fmt:formatNumber value="${colTotals[m]}" pattern="#,##0"/>
                </td>
              </c:forEach>
              <td class="text-end fw-bold">
                <fmt:formatNumber value="${grandTotal}" pattern="#,##0"/>
              </td>
            </tr>
          </tfoot>
        </table>
      </div>

      <div class="card-footer small text-muted">* 横にスワイプ/スクロールできます（12か月）。</div>
    </div>
  </div>

  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
