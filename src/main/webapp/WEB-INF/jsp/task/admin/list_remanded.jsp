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
<title>業務一覧（差戻）</title>
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
	rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
	<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf"%>
	<div class="container py-4">

		<div class="d-flex justify-content-between align-items-center mb-3">
			<div>
				<h1 class="h3 mb-0">業務一覧（差戻）</h1>
				<div class="text-muted small">
					年月：<strong>${yearMonth}</strong>
				</div>
			</div>
		</div>

		<!-- タブ -->
		<c:url var="urlAll" value="/admin/task/list_all">
			<c:param name="yearMonth" value="${yearMonth}" />
			<c:param name="sec" value="${sec}" />
			<c:param name="cust" value="${cust}" />
		</c:url>
		<c:url var="urlUnapp" value="/admin/task/list_unapproved">
			<c:param name="yearMonth" value="${yearMonth}" />
			<c:param name="sec" value="${sec}" />
			<c:param name="cust" value="${cust}" />
		</c:url>
		<c:url var="urlApp" value="/admin/task/list_approved">
			<c:param name="yearMonth" value="${yearMonth}" />
			<c:param name="sec" value="${sec}" />
			<c:param name="cust" value="${cust}" />
		</c:url>
		<c:url var="urlRemand" value="/admin/task/list_remanded">
			<c:param name="yearMonth" value="${yearMonth}" />
			<c:param name="sec" value="${sec}" />
			<c:param name="cust" value="${cust}" />
		</c:url>
		<ul class="nav nav-tabs mb-3">
			<li class="nav-item"><a class="nav-link" href="${urlAll}">全件</a></li>
			<li class="nav-item"><a class="nav-link" href="${urlUnapp}">未承認</a></li>
			<li class="nav-item"><a class="nav-link" href="${urlApp}">承認済</a></li>
			<li class="nav-item"><a class="nav-link active"
				href="${urlRemand}">差戻</a></li>
		</ul>

		<!-- フィルタ -->
		<form method="get"
			action="<%=request.getContextPath()%>/admin/task/list_remanded"
			class="card card-body shadow-sm mb-3">
			<div class="row g-2 align-items-center">
				<div class="col-auto">
					<input type="month" name="yearMonth"
						class="form-control form-control-sm" value="${yearMonth}">
				</div>
				<div class="col-auto">
					<input type="text" name="sec" class="form-control form-control-sm"
						placeholder="秘書名を含む" value="${sec}">
				</div>
				<div class="col-auto">
					<input type="text" name="cust" class="form-control form-control-sm"
						placeholder="顧客名を含む" value="${cust}">
				</div>
				<div class="col-auto">
					<button type="submit" class="btn btn-sm btn-outline-primary">表示</button>
				</div>
			</div>
		</form>

		<div class="alert alert-info">
			<span class="me-3">件数：<strong>${fn:length(tasks)}</strong></span> <span
				class="me-3">合計稼働：<strong><fmt:formatNumber
						value="${totalMinute/60}" type="number" maxFractionDigits="0" /></strong>
				時間 <strong>${totalMinute%60}</strong> 分
			</span> <span>合計金額：<strong> <fmt:formatNumber
						value="${sumCustomer}" type="number" maxFractionDigits="0"
						groupingUsed="true" /> / <fmt:formatNumber value="${sumSecretary}"
						type="number" maxFractionDigits="0" groupingUsed="true" /> 円
			</strong></span>
		</div>

		<div class="card shadow-sm">
			<div class="card-header bg-light">
				<span class="fw-semibold">差戻一覧（${yearMonth}）</span>
			</div>

			<div class="card-body p-0">
				<c:choose>
					<c:when test="${empty tasks}">
						<div class="p-4 text-center text-muted">該当データはありません。</div>
					</c:when>
					<c:otherwise>
						<div class="table-responsive">
							<table class="table table-sm table-hover align-middle mb-0">
								<thead class="table-secondary">
									<tr>
										<th>秘書</th>
										<th>顧客</th>
										<th style="width: 70px;">日付</th>
										<th style="width: 130px;">時間</th>
										<th style="width: 70px;">稼働</th>
										<th style="width: 70px;">ランク</th>
										<th>内容</th>
										<th style="width: 100px;">単価</th>
										<th style="width: 100px;">報酬</th>
										<th style="width: 160px;">差戻日時</th>
										<th>差戻コメント</th>
									</tr>
								</thead>
								<tbody>
									<c:forEach var="t" items="${tasks}">
										<tr>
											<td><a
												href="${pageContext.request.contextPath}/admin/secretary/detail?id=${t.assignment.secretaryId}">${t.assignment.secretaryName}</a></td>
											<td><a
												href="${pageContext.request.contextPath}/admin/customer/detail?id=${t.assignment.customerId}">${t.assignment.companyName}</a></td>

											<td><fmt:formatDate value="${t.workDate}"
													pattern="dd (E)" timeZone="Asia/Tokyo" /></td>
											<td><fmt:formatDate value="${t.startTime}"
													pattern="HH:mm" timeZone="Asia/Tokyo" /> ～ <fmt:formatDate
													value="${t.endTime}" pattern="HH:mm" timeZone="Asia/Tokyo" />
											</td>
											<td><fmt:formatNumber value="${t.workMinute}"
													type="number" maxFractionDigits="0" />分</td>
											<td><c:out
													value="${t.assignment.taskRankName != null ? t.assignment.taskRankName : '—'}" /></td>
											<td>${fn:escapeXml(t.workContent)}</td>
											<td><c:choose>
													<c:when test="${t.hourFeeCustomer ne null}">
														<fmt:formatNumber value="${t.hourFeeCustomer}"
															type="number" maxFractionDigits="0" groupingUsed="true" /> 円
                        </c:when>
													<c:otherwise>—</c:otherwise>
												</c:choose></td>
											<td><c:choose>
													<c:when test="${t.feeCustomer ne null}">
														<fmt:formatNumber value="${t.feeCustomer}" type="number"
															maxFractionDigits="0" groupingUsed="true" /> 円
                        </c:when>
													<c:otherwise>—</c:otherwise>
												</c:choose></td>
											<td><fmt:formatDate value="${t.remandedAt}"
													pattern="yyyy-MM-dd HH:mm" timeZone="Asia/Tokyo" /></td>
											<td>${fn:escapeXml(t.remandComment)}</td>
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
	<script
		src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
