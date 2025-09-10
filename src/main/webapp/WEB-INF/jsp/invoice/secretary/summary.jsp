<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8" />
  <title>請求サマリー</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
  <style> .btn-month { height: 32px; width:60px;}</style>
</head>
<body class="bg-light">
<div class="container py-4">

  <div class="d-flex justify-content-between align-items-center mb-3">
    <div>
      <h1 class="h4 mb-1">請求サマリー</h1>
      <div class="text-muted small">対象年月：<strong>${yearMonth}</strong></div>
    </div>
    <div class="d-flex gap-2">
      <form method="get" action="<%= request.getContextPath() %>/secretary/invoice" class="d-flex align-items-center gap-2">
        <input type="month" name="yearMonth" class="form-control form-control-sm month-ctl" value="${yearMonth}" />
        <button type="submit" class="btn btn-sm btn-outline-primary btn-month">表示</button>
      </form>
      <a href="<%=request.getContextPath()%>/secretary/home" class="btn btn-sm btn-outline-secondary">戻る</a>
    </div>
  </div>
  
  <c:set var="sumInvoiceMinutes" value="0"/>
  <c:forEach var="inv" items="${invoices}">
    <c:set var="m" value="${inv.totalMinute != null ? inv.totalMinute : 0}" />
    <c:set var="sumInvoiceMinutes" value="${sumInvoiceMinutes + m}" />
  </c:forEach>
  <c:set var="sumInvoiceHours" value="${(sumInvoiceMinutes - (sumInvoiceMinutes % 60)) / 60}" />
  <c:set var="sumInvoiceRemMin" value="${sumInvoiceMinutes % 60}" />

  <div class="row g-3 mb-4">
    <div class="col-md-3">
      <div class="card shadow-sm">
        <div class="card-body">
          <div class="text-muted small">総請求額</div>
          <div class="h5 mb-0">
            <fmt:formatNumber value="${grandTotalFee}" type="number" maxFractionDigits="0" groupingUsed="true" /> 円
          </div>
        </div>
      </div>
    </div>
    <div class="col-md-3">
      <div class="card shadow-sm">
        <div class="card-body">
          <div class="text-muted small">請求件数（会社×ランク）</div>
          <div class="h5 mb-0">${fn:length(invoices)}</div>
        </div>
      </div>
    </div>
    <div class="col-md-3">
      <div class="card shadow-sm">
        <div class="card-body">
          <div class="text-muted small">合計稼働（請求ベース）</div>
          <div class="h5 mb-0">
            <fmt:formatNumber value="${sumInvoiceHours}" type="number" maxFractionDigits="0" /> 時間 ${sumInvoiceRemMin} 分
          </div>
        </div>
      </div>
    </div>
    <div class="col-md-3">
      <div class="card shadow-sm">
        <div class="card-body">
          <div class="text-muted small">タスク件数</div>
          <div class="h5 mb-0">${fn:length(tasks)}</div>
        </div>
      </div>
    </div>
  </div>

  <div class="card shadow-sm mb-4">
    <div class="card-header bg-white">
      <span class="fw-semibold">会社別請求一覧（${yearMonth}）</span>
    </div>
    <div class="card-body p-0">
      <c:choose>
        <c:when test="${empty invoices}">
          <div class="p-4 text-center text-muted">請求データがありません。</div>
        </c:when>
        <c:otherwise>
          <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
              <thead class="table-light">
                <tr>
                  <th>会社名</th>
                  <th style="width: 120px;">ランク</th>
                  <th style="width: 120px;">合計稼働</th>
                  <th style="width: 120px;">時給</th>
                  <th style="width: 140px;">請求額</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="inv" items="${invoices}">
                  <tr>
                    <td>${fn:escapeXml(inv.customerCompanyName)}</td>
                    <td>${fn:escapeXml(inv.taskRankName)}</td>
                    <td><fmt:formatNumber value="${inv.totalMinute}" type="number" maxFractionDigits="0" /> 分</td>
                    <td>
                      <c:choose>
                        <c:when test="${inv.hourlyPay ne null}">
                          <fmt:formatNumber value="${inv.hourlyPay}" type="number" maxFractionDigits="0" groupingUsed="true" /> 円/時
                        </c:when>
                        <c:otherwise>—</c:otherwise>
                      </c:choose>
                    </td>
                    <td>
                      <c:choose>
                        <c:when test="${inv.fee ne null}">
                          <strong><fmt:formatNumber value="${inv.fee}" type="number" maxFractionDigits="0" groupingUsed="true" /></strong> 円
                        </c:when>
                        <c:otherwise>—</c:otherwise>
                      </c:choose>
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
              <tfoot class="table-secondary">
                <tr>
                  <th colspan="3">合計</th>
                  <th class="text-end">総額</th>
                  <th>
                    <strong><fmt:formatNumber value="${grandTotalFee}" type="number" maxFractionDigits="0" groupingUsed="true" /></strong> 円
                  </th>
                </tr>
              </tfoot>
            </table>
          </div>
        </c:otherwise>
      </c:choose>
    </div>
  </div>

  <div class="card shadow-sm">
    <div class="card-header bg-white d-flex justify-content-between align-items-center">
      <span class="fw-semibold">タスク明細（${yearMonth}）</span>
    </div>
    <div class="card-body p-0">
      <c:choose>
        <c:when test="${empty tasks}">
          <div class="p-4 text-center text-muted">タスク明細はありません。</div>
        </c:when>
        <c:otherwise>
          <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
              <thead class="table-light">
                <tr>
                  <th style="width: 140px;">日付</th>
                  <th style="width: 120px;">時間</th>
                  <th style="width: 90px;">稼働</th>
                  <th>顧客</th>
                  <th style="width: 60px;">ランク</th>
                  <th>内容</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="t" items="${tasks}">
                  <tr>
                    <td>
                      <c:if test="${t.workDate ne null}">
                        <fmt:formatDate value="${t.workDate}" pattern="yyyy-MM-dd (E)" timeZone="Asia/Tokyo" />
                      </c:if>
                    </td>
                    <td>
                      <c:if test="${t.startTime ne null}">
                        <fmt:formatDate value="${t.startTime}" pattern="HH:mm" timeZone="Asia/Tokyo" />
                      </c:if>
                      ～
                      <c:if test="${t.endTime ne null}">
                        <fmt:formatDate value="${t.endTime}" pattern="HH:mm" timeZone="Asia/Tokyo" />
                      </c:if>
                    </td>
                    <td><fmt:formatNumber value="${t.workMinute}" type="number" maxFractionDigits="0" /> 分</td>
                    <td>${t.assignment.companyName}</td>
                    <td>${t.assignment.taskRankName}</td>
                    <td>${fn:escapeXml(t.workContent)}</td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </c:otherwise>
      </c:choose>
    </div>
  </div>

</div>
</body>
</html>
