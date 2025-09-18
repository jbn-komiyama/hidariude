<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8" />
  <title>請求サマリー（顧客）</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
  <style>
    .btn-month { height:32px; width:60px; }
    .col-w-60  { width:60px; }
    .col-w-90  { width:90px; }
    .col-w-110 { width:110px; }
    .col-w-120 { width:120px; }
    .col-w-140 { width:140px; }
    .table thead.table-primary th { vertical-align:middle; }
    .table .mb-0 { margin-bottom:0; }

    /* 未承認だけに使うやわらかいオレンジ系バッジ */
    .badge-unapproved {
      background-color: #FFE3D5; /* 背景 */
      color: #B54708;            /* 文字 */
      border: 1px solid #FFBF99; /* 枠線 */
      border-radius: 9999px;     /* ピル型 */
      font-weight: 700;
      padding: .35rem .6rem;
    }
  </style>
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/customer/navbar.jspf" %>
<div class="container py-4">

  <!-- ===== 事前集計 ===== -->
  <c:set var="unapprovedCount" value="0"/>
  <c:forEach var="t" items="${tasks}">
    <c:if test="${t.approvedAt == null}">
      <c:set var="unapprovedCount" value="${unapprovedCount + 1}"/>
    </c:if>
  </c:forEach>
  <c:set var="totalTasks" value="${fn:length(tasks)}"/>
  <c:set var="approvedCount" value="${totalTasks - unapprovedCount}"/>

  <!-- ===== ヘッダ ===== -->
  <div class="d-flex justify-content-between align-items-center mb-3">
    <div>
      <h1 class="h4 mb-1">支払サマリー</h1>
      <div class="text-muted small">対象年月：<strong>${yearMonth}</strong></div>
    </div>
    <form method="get" action="<%= request.getContextPath() %>/customer/invoice" class="d-flex align-items-center gap-2">
      <input type="month" name="yearMonth" class="form-control form-control-sm" value="${yearMonth}" />
      <button type="submit" class="btn btn-sm btn-outline-primary btn-month">表示</button>
    </form>
  </div>

  <!-- ===== サマリーカード ===== -->
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
        <div class="card-header bg-primary text-white">総支払額</div>
        <div class="card-body">
          <div class="h5 mb-0">
            <fmt:formatNumber value="${grandTotalFee}" type="number" maxFractionDigits="0" groupingUsed="true" /> 円
          </div>
        </div>
      </div>
    </div>
    <div class="col-md-3">
      <div class="card shadow-sm">
        <div class="card-header bg-primary text-white">支払件数（秘書×ランク）</div>
        <div class="card-body"><div class="h5 mb-0">${fn:length(invoices)}</div></div>
      </div>
    </div>
    <div class="col-md-3">
      <div class="card shadow-sm">
        <div class="card-header bg-primary text-white">合計稼働（支払ベース）</div>
        <div class="card-body">
          <div class="h5 mb-0">
            <fmt:formatNumber value="${sumInvoiceHours}" type="number" maxFractionDigits="0" /> 時間 ${sumInvoiceRemMin} 分
          </div>
        </div>
      </div>
    </div>
    <div class="col-md-3">
      <div class="card shadow-sm">
        <div class="card-header bg-primary text-white">タスク件数</div>
        <div class="card-body"><div class="h5 mb-0">${totalTasks}</div></div>
      </div>
    </div>
  </div>

  <!-- ===== 秘書別支払金 ===== -->
  <div class="card shadow-sm mb-4">
    <div class="card-header bg-primary text-white">
      <span class="fw-semibold">秘書別支払金（${yearMonth}）</span>
    </div>
    <div class="card-body p-0">
      <c:choose>
        <c:when test="${empty invoices}">
          <div class="p-4 text-center text-muted">データがありません。</div>
        </c:when>
        <c:otherwise>
          <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
              <thead class="table-primary">
                <tr>
                  <th>秘書名</th>
                  <th class="col-w-120">ランク</th>
                  <th class="col-w-120">合計稼働</th>
                  <th class="col-w-120">時給</th>
                  <th class="col-w-140">支払額</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="inv" items="${invoices}">
                  <tr>
                    <!-- ※ DAO側で secretary_name を customerCompanyName に格納している前提 -->
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
              <tfoot class="table-primary-subtle">
                <tr>
                  <th colspan="3">合計</th>
                  <th class="text-end">総支払額</th>
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

  <!-- ===== タスク一覧（承認/未承認を1表で表示） ===== -->
  <div class="card shadow-sm mb-4">
    <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
      <span class="fw-semibold">タスク一覧（全${totalTasks}件：承認済み ${approvedCount} / 未承認 ${unapprovedCount}）</span>
    </div>
    <div class="card-body p-0">
      <c:choose>
        <c:when test="${totalTasks == 0}">
          <div class="p-4 text-center text-muted">タスクはありません。</div>
        </c:when>
        <c:otherwise>
          <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
              <thead class="table-primary">
                <tr>
                  <th class="col-w-140">日付</th>
                  <th class="col-w-120">時間</th>
                  <th class="col-w-90">稼働</th>
                  <th>秘書</th>
                  <th class="col-w-60">ランク</th>
                  <th>内容</th>
                  <th class="col-w-110">ステータス</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="t" items="${tasks}">
                  <!-- 未承認行だけ淡い青で強調 -->
                  <c:set var="rowClass" value=""/>
                  <c:if test="${t.approvedAt == null}">
                    <c:set var="rowClass" value="bg-primary-subtle"/>
                  </c:if>
                  <tr class="${rowClass}">
                    <td>
                      <c:if test="${t.workDate ne null}">
                        <fmt:formatDate value="${t.workDate}" pattern="yyyy-MM-dd (E)" timeZone="Asia/Tokyo"/>
                      </c:if>
                    </td>
                    <td>
                      <c:if test="${t.startTime ne null}">
                        <fmt:formatDate value="${t.startTime}" pattern="HH:mm" timeZone="Asia/Tokyo"/>
                      </c:if>
                      ～
                      <c:if test="${t.endTime ne null}">
                        <fmt:formatDate value="${t.endTime}" pattern="HH:mm" timeZone="Asia/Tokyo"/>
                      </c:if>
                    </td>
                    <td><fmt:formatNumber value="${t.workMinute}" type="number" maxFractionDigits="0" /> 分</td>
                    <td>${t.assignment.secretaryName}</td>
                    <td>${t.assignment.taskRankName}</td>
                    <td>${fn:escapeXml(t.workContent)}</td>
                    <td>
                      <!-- 承認済みは何も表示しない／未承認のみバッジ表示 -->
                      <c:if test="${t.approvedAt == null}">
                        <span class="badge badge-unapproved">未承認</span>
                      </c:if>
                    </td>
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

<!-- 未承認アラート（任意） -->
<c:if test="${unapprovedCount gt 0}">
<script>
  alert('未承認タスクがあります。管理者に承認をしてもらってから請求処理を行ってください。　OK');
</script>
</c:if>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
