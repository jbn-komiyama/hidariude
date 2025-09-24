<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false"%>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn"  uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <title>請求サマリー（管理）</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf" %>

<div class="container py-4">
  <div class="d-flex align-items-center justify-content-between mb-3">
    <h1 class="h3 mb-0">請求サマリー（管理）</h1>
    <form method="get" class="d-flex gap-2">
      <input type="month" class="form-control" name="yearMonth" value="${yearMonth}">
      <button type="submit" class="btn btn-primary">表示</button>
    </form>
  </div>

  <!-- KPI Cards -->
  <div class="row g-3 mb-4">
    <div class="col-12 col-md-3">
      <div class="card border-primary h-100">
        <div class="card-header bg-primary text-white">総支払額</div>
        <div class="card-body">
          <h3 class="card-title mb-0">
            <fmt:formatNumber value="${adminTotals.totalAmount}" pattern="#,##0" /> 円
          </h3>
          <div class="text-muted small">対象年月：${yearMonth}</div>
        </div>
      </div>
    </div>
    <div class="col-12 col-md-3">
      <div class="card border-primary h-100">
        <div class="card-header bg-primary text-white">支払い件数</div>
        <div class="card-body">
          <h3 class="card-title mb-0">
            <fmt:formatNumber value="${adminTotals.totalTasksCount}" pattern="#,##0" /> 件
          </h3>
        </div>
      </div>
    </div>
    <div class="col-12 col-md-3">
      <div class="card border-primary h-100">
        <div class="card-header bg-primary text-white">合計稼働時間</div>
        <div class="card-body">
          <c:set var="mins" value="${adminTotals.totalWorkMinutes}" />
          <c:set var="hrs"  value="${mins / 60}" />
          <c:set var="rem"  value="${mins % 60}" />
          <h3 class="card-title mb-0">
            <fmt:formatNumber value="${hrs}" pattern="#,##0" /> 時間
            <fmt:formatNumber value="${rem}" pattern="#,##0" /> 分
          </h3>
        </div>
      </div>
    </div>
    <div class="col-12 col-md-3">
      <div class="card border-primary h-100">
        <div class="card-header bg-primary text-white">前月との比較</div>
        <div class="card-body">
          <c:choose>
            <c:when test="${diffFromPrev >= 0}">
              <h3 class="card-title mb-0 text-primary">＋<fmt:formatNumber value="${diffFromPrev}" pattern="#,##0" /> 円</h3>
            </c:when>
            <c:otherwise>
              <h3 class="card-title mb-0 text-danger">－<fmt:formatNumber value="${-diffFromPrev}" pattern="#,##0" /> 円</h3>
            </c:otherwise>
          </c:choose>
          <div class="text-muted small">前月：${prevYearMonth}</div>
        </div>
      </div>
    </div>
  </div>

  <!-- 明細（顧客ごと） -->
  <c:if test="${empty adminGrouped}">
    <div class="alert alert-info">表示対象のデータがありません。</div>
  </c:if>

  <c:forEach var="entry" items="${adminGrouped}">
    <c:set var="customerName" value="${entry.key}" />
    <c:set var="rows" value="${entry.value}" />

    <div class="card mb-4 border-primary">
      <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
        <span class="fw-semibold">${customerName}</span>
        <span class="small">年月：${yearMonth}</span>
      </div>
      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-hover table-bordered align-middle mb-0">
            <thead class="table-primary">
              <tr>
                <th style="min-width:160px;">秘書名</th>
                <th style="min-width:120px;">ランク</th>
                <th class="text-end" style="min-width:120px;">合計稼働</th>
                <th class="text-end" style="min-width:140px;">時給</th>
                <th class="text-end" style="min-width:140px;">支払額</th>
              </tr>
            </thead>
            <tbody>
              <c:set var="custTotal" value="0" />
              <c:forEach var="r" items="${rows}">
                <tr>
                  <td class="text-nowrap"><c:out value="${r.secretaryName}" /></td>
                  <td class="text-nowrap"><c:out value="${r.taskRankName}" /></td>
                  <td class="text-end">
                    <fmt:formatNumber value="${r.totalMinute}" pattern="#,##0" /> 分
                  </td>
                  <td class="text-end">
                    <fmt:formatNumber value="${r.hourlyPay}" pattern="#,##0" /> 円/時
                  </td>
                  <td class="text-end">
                    <fmt:formatNumber value="${r.fee}" pattern="#,##0" /> 円
                  </td>
                </tr>
                <c:set var="custTotal" value="${custTotal + r.fee}" />
              </c:forEach>
            </tbody>
            <tfoot>
              <tr class="table-light">
                <th colspan="4" class="text-end">合計</th>
                <th class="text-end">
                  <fmt:formatNumber value="${custTotal}" pattern="#,##0" /> 円
                </th>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </div>
  </c:forEach>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
