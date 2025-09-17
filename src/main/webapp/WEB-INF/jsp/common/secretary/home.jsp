<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>秘書ホーム</title>
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
	rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
	<%@ include file="/WEB-INF/jsp/_parts/secretary/navbar.jspf" %>
	
	
	
	<div class="container py-4">
		<div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <div>
        <h1 class="h3 mb-1">ダッシュボード</h1>
        <p class="text-muted small mb-0">タスクの承認状況と請求サマリー</p>
      </div>
      <div class="text-end">
        <span class="badge rounded-pill bg-primary-subtle text-primary me-2">今月 ${yearMonth}</span>
        <span class="text-secondary">${secretaryName} さん</span>
      </div>
    </div>
    
    <!-- 見出し：今月 -->
		<h6 class="text-secondary mt-3 mb-1">今月</h6>

		<!-- 今月 4カード -->
		<div class="row g-3 mb-4">
			<div class="col-6 col-md-3">
				<a class="text-decoration-none"
					href="<%=request.getContextPath()%>/secretary/task/list_unapproved?status=unapproved&yearMonth=${yearMonth}">
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
					href="<%=request.getContextPath()%>/secretary/task/list_approved?status=approved&yearMonth=${yearMonth}">
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
					href="<%=request.getContextPath()%>/secretary/task/list_remanded?status=remanded&yearMonth=${yearMonth}">
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
					href="<%=request.getContextPath()%>/secretary/invoice?yearMonth=${yearMonth}">
					<div class="card shadow-sm h-100 border-0">
						<div class="card-body">
							<div class="text-muted small mb-2">合計金額（承認済み）</div>
							<div class="display-6 fw-bold">
								<fmt:formatNumber value="${task.sumAmountApproved}"
									type="number" maxFractionDigits="0" groupingUsed="true" />
								<span class="fs-6 fw-normal">円</span>
							</div>
						</div>
					</div>
				</a>
			</div>
		</div>

		<!-- 見出し：先月 -->
		<h6 class="text-secondary mt-3 mb-1">先月</h6>

		<!-- ★先月 4カード -->
		<div class="row g-3 mb-4">
			<div class="col-6 col-md-3">
				<a class="text-decoration-none"
					href="<%=request.getContextPath()%>/secretary/task/list_unapproved?status=unapproved&yearMonth=${prevYearMonth}">
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
					href="<%=request.getContextPath()%>/secretary/task/list_approved?status=approved&yearMonth=${prevYearMonth}">
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
					href="<%=request.getContextPath()%>/secretary/task/list_remanded?status=remanded&yearMonth=${prevYearMonth}">
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
					href="<%=request.getContextPath()%>/secretary/invoice?yearMonth=${prevYearMonth}">
					<div class="card shadow-sm h-100 border-0">
						<div class="card-body">
							<div class="text-muted small mb-2">合計金額（承認済み）</div>
							<div class="display-6 fw-bold">
								<fmt:formatNumber value="${taskPrev.sumAmountApproved}"
									type="number" maxFractionDigits="0" groupingUsed="true" />
								<span class="fs-6 fw-normal">円</span>
							</div>
						</div>
					</div>
				</a>
			</div>
		</div>

		<h2 class="h5 text-secondary mb-3">現在アサイン中の案件</h2>

		<div class="card mb-4 shadow-sm">
			<div class="card-body p-0">
				<c:choose>
					<c:when test="${empty assignRows}">
						<div class="p-3 text-muted">今月のアサインはありません。</div>
					</c:when>
					<c:otherwise>
						<div class="table-responsive">
							<table class="table table-sm mb-0 align-middle">
								<thead class="table-secondary">
									<tr>
										<th style="width: 64px;">#</th>
										<th>顧客</th>
										<th style="width: 160px;">タスクランク</th>
										<th style="width: 140px;">基本単価</th>
										<th style="width: 140px;">増額(ランク)</th>
										<th style="width: 140px;">増額(継続)</th>
										<th style="width: 160px;">合計単価</th>
									</tr>
								</thead>
								<tbody>
									<c:forEach var="r" items="${assignRows}" varStatus="st">
										<tr>
											<td>${st.index + 1}</td>
											<td>${fn:escapeXml(r.company)}</td>
											<td>${fn:escapeXml(r.rank)}</td>
											<td><c:choose>
													<c:when test="${r.base ne null}">
														<fmt:formatNumber value="${r.base}" type="number"
															maxFractionDigits="0" groupingUsed="true" /> 円
                      </c:when>
													<c:otherwise>—</c:otherwise>
												</c:choose></td>
											<td><c:choose>
													<c:when test="${r.incRank ne null}">
														<fmt:formatNumber value="${r.incRank}" type="number"
															maxFractionDigits="0" groupingUsed="true" /> 円
                      </c:when>
													<c:otherwise>—</c:otherwise>
												</c:choose></td>
											<td><c:choose>
													<c:when test="${r.incCont ne null}">
														<fmt:formatNumber value="${r.incCont}" type="number"
															maxFractionDigits="0" groupingUsed="true" /> 円
                      </c:when>
													<c:otherwise>—</c:otherwise>
												</c:choose></td>
											<td><c:choose>
													<c:when test="${r.total ne null}">
														<strong><fmt:formatNumber value="${r.total}"
																type="number" maxFractionDigits="0" groupingUsed="true" /></strong> 円
                      </c:when>
													<c:otherwise>—</c:otherwise>
												</c:choose></td>
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
	<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
	
</body>
</html>
