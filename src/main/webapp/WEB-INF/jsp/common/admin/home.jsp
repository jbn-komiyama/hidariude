<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8" />
  <title>管理ダッシュボード</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
</head>
<body class="bg-primary bg-opacity-10"><!-- 青系背景 -->
  <%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf" %>

  <div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <div>
        <h1 class="h3 mb-1">ダッシュボード</h1>
        <p class="text-muted small mb-0">全体のタスク承認状況と請求サマリー</p>
      </div>
      <div class="text-end">
        <span class="badge rounded-pill bg-primary-subtle text-primary me-2">今月 ${yearMonth}</span>
        <span class="text-secondary"><c:out value="${adminName}" /> さん</span>
      </div>
    </div>

    <!-- 見出し：今月 -->
    <h6 class="text-secondary mt-3 mb-1">今月</h6>

    <!-- 4カード（今月） -->
    <div class="row g-3 mb-4">
      <div class="col-6 col-md-3">
        <a class="text-decoration-none"
           href="<%=request.getContextPath()%>/admin/task/list_unapproved?status=unapproved&yearMonth=${yearMonth}">
          <div class="card shadow-sm h-100 border-0">
            <div class="card-body">
              <div class="text-muted small mb-2">未承認のタスク</div>
              <div class="display-6 fw-bold">
                <c:out value="${task.unapproved}" />
                <span class="fs-6 fw-normal">件</span>
              </div>
            </div>
          </div>
        </a>
      </div>

      <div class="col-6 col-md-3">
        <a class="text-decoration-none"
           href="<%=request.getContextPath()%>/admin/task/list_approved?status=approved&yearMonth=${yearMonth}">
          <div class="card shadow-sm h-100 border-0">
            <div class="card-body">
              <div class="text-muted small mb-2">承認済みのタスク</div>
              <div class="display-6 fw-bold">
                <c:out value="${task.approved}" />
                <span class="fs-6 fw-normal">件</span>
              </div>
            </div>
          </div>
        </a>
      </div>

      <div class="col-6 col-md-3">
        <a class="text-decoration-none"
           href="<%=request.getContextPath()%>/admin/task/list_remanded?status=remanded&yearMonth=${yearMonth}">
          <div class="card shadow-sm h-100 border-0">
            <div class="card-body">
              <div class="text-muted small mb-2">差戻しのタスク</div>
              <div class="display-6 fw-bold">
                <c:out value="${task.remanded}" />
                <span class="fs-6 fw-normal">件</span>
              </div>
            </div>
          </div>
        </a>
      </div>

      <div class="col-6 col-md-3">
        <a class="text-decoration-none"
           href="<%=request.getContextPath()%>/admin/invoice?yearMonth=${yearMonth}">
          <div class="card shadow-sm h-100 border-0">
            <div class="card-body">
              <div class="text-muted small mb-2">合計金額（承認済み）</div>
              <div class="display-6 fw-bold">
                <fmt:formatNumber value="${task.sumAmountApproved}" type="number" maxFractionDigits="0" groupingUsed="true" />
                <span class="fs-6 fw-normal">円</span>
              </div>
            </div>
          </div>
        </a>
      </div>
    </div>

    <!-- 見出し：先月 -->
    <h6 class="text-secondary mt-3 mb-1">先月</h6>

    <!-- 4カード（先月） -->
    <div class="row g-3 mb-4">
      <div class="col-6 col-md-3">
        <a class="text-decoration-none"
           href="<%=request.getContextPath()%>/admin/task/list_unapproved?status=unapproved&yearMonth=${prevYearMonth}">
          <div class="card shadow-sm h-100 border-0">
            <div class="card-body">
              <div class="text-muted small mb-2">未承認のタスク</div>
              <div class="display-6 fw-bold">
                <c:out value="${taskPrev.unapproved}" />
                <span class="fs-6 fw-normal">件</span>
              </div>
            </div>
          </div>
        </a>
      </div>

      <div class="col-6 col-md-3">
        <a class="text-decoration-none"
           href="<%=request.getContextPath()%>/admin/task/list_approved?status=approved&yearMonth=${prevYearMonth}">
          <div class="card shadow-sm h-100 border-0">
            <div class="card-body">
              <div class="text-muted small mb-2">承認済みのタスク</div>
              <div class="display-6 fw-bold">
                <c:out value="${taskPrev.approved}" />
                <span class="fs-6 fw-normal">件</span>
              </div>
            </div>
          </div>
        </a>
      </div>

      <div class="col-6 col-md-3">
        <a class="text-decoration-none"
           href="<%=request.getContextPath()%>/admin/task/list_remanded?status=remanded&yearMonth=${prevYearMonth}">
          <div class="card shadow-sm h-100 border-0">
            <div class="card-body">
              <div class="text-muted small mb-2">差戻しのタスク</div>
              <div class="display-6 fw-bold">
                <c:out value="${taskPrev.remanded}" />
                <span class="fs-6 fw-normal">件</span>
              </div>
            </div>
          </div>
        </a>
      </div>

      <div class="col-6 col-md-3">
        <a class="text-decoration-none"
           href="<%=request.getContextPath()%>/admin/invoice?yearMonth=${prevYearMonth}">
          <div class="card shadow-sm h-100 border-0">
            <div class="card-body">
              <div class="text-muted small mb-2">合計金額（承認済み）</div>
              <div class="display-6 fw-bold">
                <fmt:formatNumber value="${taskPrev.sumAmountApproved}" type="number" maxFractionDigits="0" groupingUsed="true" />
                <span class="fs-6 fw-normal">円</span>
              </div>
            </div>
          </div>
        </a>
      </div>
    </div>

  </div>

  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
