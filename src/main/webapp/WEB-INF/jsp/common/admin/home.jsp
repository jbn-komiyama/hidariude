<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8" />
<title>管理ダッシュボード</title>
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
	rel="stylesheet" />
</head>
<body class="bg-primary bg-opacity-10">
	<!-- 青系背景 -->
	<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf"%>

	<div class="container py-4">
		<div class="d-flex justify-content-between align-items-center mb-3">
			<div>
				<h1 class="h3 mb-1">ダッシュボード</h1>
				<p class="text-muted small mb-0">全体のタスク承認状況と請求サマリー</p>
			</div>
			<div class="text-end">
				<span class="badge rounded-pill bg-primary-subtle text-primary me-2">今月
					${yearMonth}</span> <span class="text-secondary"><c:out
						value="${adminName}" /> さん</span>
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
					href="<%=request.getContextPath()%>/admin/invoice/sales?yearMonth=${yearMonth}">
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
					href="<%=request.getContextPath()%>/admin/invoice/sales?yearMonth=${prevYearMonth}">
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

		<!-- 顧客からのアラート一覧 -->
		<div class="card shadow-sm mt-5">
			<div class="card-header fw-bold">直近の顧客からのアラート一覧</div>
			<div class="card-body p-0">
				<div class="table-responsive">
					<table class="table table-sm table-striped mb-0 align-middle">
						<thead class="table-light">
							<tr>
								<th style="width: 3rem;" class="text-center">#</th>
								<th>会社名</th>
								<th>秘書名</th>
								<th style="width: 8rem;">日付</th>
								<th style="width: 6rem;">分数</th>
								<th>ランク</th>
								<th>仕事内容</th>
								<th>アラート内容</th>
							</tr>
						</thead>
						<tbody>
							<c:forEach var="t" items="${alerts}" varStatus="st">
								<tr>
									<td class="text-center"><c:out value="${st.index + 1}" /></td>
									<td><c:out value="${t.assignment.companyName}" /></td>
									<td><c:out value="${t.assignment.secretaryName }" /></td>
									<td><fmt:formatDate value="${t.workDate}"
											pattern="yyyy年MM月dd日" /></td>
									<td><c:out value="${t.workMinute}" />分</td>
									<td><c:out value="${t.assignment.taskRankName}" /></td>
									<td><c:out value="${t.workContent}" /></td>
									<td><c:out value="${t.alertComment}" /></td>
								</tr>
							</c:forEach>
							<c:if test="${empty alerts}">
								<tr>
									<td colspan="9" class="text-muted text-center py-3">アラートはありません</td>
								</tr>
							</c:if>
						</tbody>
					</table>
				</div>
			</div>
		</div>

		<div class="row g-4 mt-4">

			<!-- 最近登録された秘書 (10件) -->
			<div class="col-lg-6">
				<div class="card shadow-sm h-100">
					<div class="card-header fw-bold">最近登録された秘書</div>
					<div class="card-body p-0">
						<div class="table-responsive">
							<table class="table table-sm table-striped mb-0 align-middle">
								<thead class="table-light">
									<tr>
										<th style="width: 3rem;" class="text-center">#</th>
										<th>秘書名</th>
										<th>ランク</th>
										<th>プロフ有無</th>
										<th style="width: 12rem;">登録日</th>
									</tr>
								</thead>
								<tbody>
									<c:forEach var="s" items="${recentSecretaries}" varStatus="st">
										<tr>
											<td class="text-center"><c:out value="${st.index + 1}" /></td>
											<td><a
												href="${pageContext.request.contextPath}/admin/secretary/detail?id=${s.id}"><c:out
														value="${s.name}" /></a></td>
											<td><c:out
													value="${empty s.rankName ? '—' : s.rankName}" /></td>
											<td><c:choose>
													<c:when test="${s.hasProfile}">
														<span class="badge bg-primary">有</span>
													</c:when>
													<c:otherwise>
														<span class="badge bg-secondary">無</span>
													</c:otherwise>
												</c:choose></td>
											<td><fmt:formatDate value="${s.createdAt}"
													pattern="yyyy年MM月dd日" /></td>
										</tr>
									</c:forEach>
									<c:if test="${empty recentSecretaries}">
										<tr>
											<td colspan="5" class="text-muted text-center py-3">データがありません</td>
										</tr>
									</c:if>
								</tbody>
							</table>
						</div>
					</div>
				</div>
			</div>

			<!-- 最近登録されたお客様 (10件) -->
			<div class="col-lg-6">
				<div class="card shadow-sm h-100">
					<div class="card-header fw-bold">最近登録されたお客様</div>
					<div class="card-body p-0">
						<div class="table-responsive">
							<table class="table table-sm table-striped mb-0 align-middle">
								<thead class="table-light">
									<tr>
										<th style="width: 3rem;" class="text-center">#</th>
										<th>顧客名</th>
										<th style="width: 12rem;">登録日</th>
									</tr>
								</thead>
								<tbody>
									<c:forEach var="c" items="${recentCustomers}" varStatus="st">
										<tr>
											<td class="text-center"><c:out value="${st.index + 1}" /></td>
											<td><a
												href="${pageContext.request.contextPath}/admin/customer/detail?id=${c.id}"><c:out
														value="${c.companyName}" /></a></td>
											<td><fmt:formatDate value="${c.createdAt}"
													pattern="yyyy年MM月dd日" /></td>
										</tr>
									</c:forEach>
									<c:if test="${empty recentCustomers}">
										<tr>
											<td colspan="3" class="text-muted text-center py-3">データがありません</td>
										</tr>
									</c:if>
								</tbody>
							</table>
						</div>
					</div>
				</div>
			</div>

		</div>


	</div>

	<script
		src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
