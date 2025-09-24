<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>

<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <title>秘書支払いサマリー</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf"%>

<div class="container py-4">
  <!-- タイトル & 月指定 -->
  <div class="d-flex align-items-center justify-content-between mb-3">
    <h1 class="h3 mb-0">秘書支払いサマリー</h1>
    <form id="ymForm" method="get" action="${pageContext.request.contextPath}/invoice/admin/cost_summary" class="d-flex gap-2">
      <input type="month" class="form-control" name="targetYM" value="${yearMonth}">
      <button class="btn btn-primary">表示</button>
    </form>
  </div>

  <!-- KPIカード（今月＋過去3ヶ月） -->
  <div class="row g-3 mb-4">
    <div class="col-12 col-md-3">
      <a class="text-decoration-none" href="${pageContext.request.contextPath}/invoice/admin/cost_summary?targetYM=${ymNow}">
        <div class="card border-primary h-100">
          <div class="card-header bg-primary text-white">今月（${m0}月）</div>
          <div class="card-body">
            <h3 class="card-title mb-0"><fmt:formatNumber value="${costNow}" pattern="#,##0" /> 円</h3>
            <div class="text-muted small">対象年月：${yearMonth}</div>
          </div>
        </div>
      </a>
    </div>

    <div class="col-12 col-md-3">
      <a class="text-decoration-none" href="${pageContext.request.contextPath}/invoice/admin/cost_summary?targetYM=${ymPrev1}">
        <div class="card border-primary h-100">
          <div class="card-header bg-primary text-white">先月（${m1}月）</div>
          <div class="card-body">
            <h3 class="card-title mb-0"><fmt:formatNumber value="${costPrev1}" pattern="#,##0" /> 円</h3>
          </div>
        </div>
      </a>
    </div>

    <div class="col-12 col-md-3">
      <a class="text-decoration-none" href="${pageContext.request.contextPath}/invoice/admin/cost_summary?targetYM=${ymPrev2}">
        <div class="card border-primary h-100">
          <div class="card-header bg-primary text-white">2ヶ月前（${m2}月）</div>
          <div class="card-body">
            <h3 class="card-title mb-0"><fmt:formatNumber value="${costPrev2}" pattern="#,##0" /> 円</h3>
          </div>
        </div>
      </a>
    </div>

    <div class="col-12 col-md-3">
      <a class="text-decoration-none" href="${pageContext.request.contextPath}/invoice/admin/cost_summary?targetYM=${ymPrev3}">
        <div class="card border-primary h-100">
          <div class="card-header bg-primary text-white">3ヶ月前（${m3}月）</div>
          <div class="card-body">
            <h3 class="card-title mb-0"><fmt:formatNumber value="${costPrev3}" pattern="#,##0" /> 円</h3>
          </div>
        </div>
      </a>
    </div>
  </div>

  <!-- 明細 -->
  <div class="card border-primary">
    <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
      <span class="fw-semibold">秘書支払い明細</span>
      <span class="small">対象年月：${targetYM}　/　合計：
        <strong><fmt:formatNumber value="${grandTotalCost}" pattern="#,##0" /></strong> 円
      </span>
    </div>

    <div class="card-body p-0">
      <c:choose>
        <c:when test="${empty costLines}">
          <div class="p-4 text-muted">表示対象のデータがありません。</div>
        </c:when>
        <c:otherwise>
          <div class="table-responsive">
            <table class="table table-hover table-bordered align-middle mb-0">
              <thead class="table-primary">
                <tr>
                  <th style="min-width:160px;">秘書名</th>
                  <th style="min-width:180px;">顧客名</th>
                  <th style="min-width:120px;">ランク</th>
                  <th class="text-end" style="min-width:120px;">合計稼働</th>
                  <th class="text-end" style="min-width:140px;">時給</th>
                  <th class="text-end" style="min-width:140px;">支払額</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="row" items="${costLines}">
                  <tr>
                    <td class="text-nowrap"><c:out value="${row.secretaryName}" /></td>
                    <td class="text-nowrap"><c:out value="${row.customerCompanyName}" /></td>
                    <td class="text-nowrap"><c:out value="${row.taskRankName}" /></td>
                    <td class="text-end">
                      <fmt:formatNumber value="${row.totalMinute}" pattern="#,##0" /> 分
                    </td>
                    <td class="text-end">
                      <fmt:formatNumber value="${row.hourlyPay}" pattern="#,##0" /> 円/時
                    </td>
                    <td class="text-end">
                      <fmt:formatNumber value="${row.fee}" pattern="#,##0" /> 円
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
              <tfoot>
                <tr class="table-light">
                  <th colspan="5" class="text-end">合計</th>
                  <th class="text-end">
                    <fmt:formatNumber value="${grandTotalCost}" pattern="#,##0" /> 円
                  </th>
                </tr>
              </tfoot>
            </table>
          </div>
        </c:otherwise>
      </c:choose>
    </div>
  </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
