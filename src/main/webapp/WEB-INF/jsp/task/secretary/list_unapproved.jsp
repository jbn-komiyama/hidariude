<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>未承認業務一覧（全顧客）</title>
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
	rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
	<div class="container py-4">

		<div class="d-flex justify-content-between align-items-center mb-3">
			<div>
				<h1 class="h4 mb-1">未承認業務一覧（全顧客）</h1>
				<div class="text-muted small">
					年月：<strong>${yearMonth}</strong>
				</div>
			</div>
			<div class="d-flex gap-2">
				<a class="btn btn-sm btn-outline-secondary"
					href="<%=request.getContextPath()%>/secretary/home">戻る</a>
			</div>
		</div>

		<c:url var="urlAll" value="/secretary/task/list_all">
			<c:param name="yearMonth" value="${yearMonth}" />
		</c:url>
		<c:url var="urlApproved" value="/secretary/task/list_approved">
			<c:param name="yearMonth" value="${yearMonth}" />
		</c:url>
		<c:url var="urlUnapp" value="/secretary/task/list_unapproved">
			<c:param name="yearMonth" value="${yearMonth}" />
		</c:url>
		<c:url var="urlRemand" value="/secretary/task/list_remanded">
			<c:param name="yearMonth" value="${yearMonth}" />
		</c:url>
		<ul class="nav nav-tabs mb-3">
			<li class="nav-item"><a class="nav-link" href="${urlAll}">すべて</a></li>
			<li class="nav-item"><a class="nav-link" href="${urlApproved}">承認済み</a></li>
			<li class="nav-item"><a class="nav-link active"
				href="${urlUnapp}">未承認</a></li>
			<li class="nav-item"><a class="nav-link" href="${urlRemand}">差戻</a></li>
		</ul>

		<form method="get"
			action="<%=request.getContextPath()%>/secretary/task/list_all"
			class="card card-body shadow-sm mb-3">
			<div class="row g-2 align-items-center">
				<div class="col-auto">
					<input id="ymInput" type="month" name="yearMonth"
						class="form-control form-control-sm" value="${yearMonth}">
				</div>
				<div class="col-auto">
					<button type="submit"
						class="btn btn-sm btn-outline-primary text-nowrap">表示</button>
				</div>
				<div class="col-auto ms-auto">
					<a id="btnRegister" class="btn btn-sm btn-primary text-nowrap"
						href="<%=request.getContextPath()%>/secretary/task/register?yearMonth=${yearMonth}">
						タスク登録 </a>
				</div>
			</div>
		</form>

		<c:set var="totalMinutes" value="${totalMinute}" />
		<c:set var="hoursRaw"
			value="${(totalMinutes - (totalMinutes % 60)) / 60}" />
		<c:set var="remMinutes" value="${totalMinutes % 60}" />
		<div class="alert alert-info">
			<span class="me-3">件数：<strong>${count}</strong></span> <span
				class="me-3">合計稼働：<strong><fmt:formatNumber
						value="${hoursRaw}" type="number" maxFractionDigits="0" /></strong> 時間 <strong>${remMinutes}</strong>
				分
			</span> <span>合計金額：<strong><fmt:formatNumber value="${sum}"
						type="number" maxFractionDigits="0" groupingUsed="true" /> 円</strong></span>
		</div>

		<c:choose>
			<c:when test="${empty tasks}">
				<div class="p-4 text-center text-muted">この月の未承認業務はありません。</div>
			</c:when>
			<c:otherwise>
				<div class="card shadow-sm">
					<div class="card-body p-0">
						<c:set var="prevCompany" value="__INIT__" />
						<c:forEach var="t" items="${tasks}" varStatus="st">
							<c:set var="company" value="${t.assignment.companyName}" />
							<c:if test="${company ne prevCompany}">
								<c:if test="${st.index ne 0}">
									</tbody>
									</table>
					</div>
					</c:if>
					<div
						class="bg-light px-3 py-2 border-top ${st.index==0 ? '' : 'mt-2'}">
						<strong>${fn:escapeXml(company)}</strong> のタスク一覧
					</div>
					<div class="table-responsive">
						<table class="table table-sm table-hover align-middle mb-0">
							<thead class="table-secondary">
								<tr>
									<th style="width: 48px;">#</th>
									<th style="width: 140px;">日付</th>
									<th style="width: 130px;">時間</th>
									<th style="width: 60px;">稼働</th>
									<th style="width: 60px;">ランク</th>
									<th>内容</th>
									<th style="width: 90px;">単価</th>
									<th style="width: 90px;">報酬</th>
									<th style="width: 90px;">状態</th>
									<th style="width: 140px;">操作</th>
								</tr>
							</thead>
							<tbody>
								<c:set var="prevCompany" value="${company}" />
								</c:if>

								<tr>
									<td>${st.count}</td>
									<td><c:if test="${t.workDate ne null}">
											<fmt:formatDate value="${t.workDate}"
												pattern="yyyy-MM-dd (E)" timeZone="Asia/Tokyo" />
										</c:if></td>
									<td><c:if test="${t.startTime ne null}">
											<fmt:formatDate value="${t.startTime}" pattern="HH:mm"
												timeZone="Asia/Tokyo" />
										</c:if> ～ <c:if test="${t.endTime ne null}">
											<fmt:formatDate value="${t.endTime}" pattern="HH:mm"
												timeZone="Asia/Tokyo" />
										</c:if></td>
									<td><c:choose>
											<c:when test="${t.workMinute ne null}">
												<fmt:formatNumber value="${t.workMinute}" type="number"
													maxFractionDigits="0" />分</c:when>
											<c:otherwise>—</c:otherwise>
										</c:choose></td>
									<td>${fn:escapeXml(t.assignment.taskRankName)}</td>
									<td>${fn:escapeXml(t.workContent)}</td>
									<td><c:choose>
											<c:when test="${t.hourFee ne null}">
												<fmt:formatNumber value="${t.hourFee}" type="number"
													maxFractionDigits="0" groupingUsed="true" /> 円</c:when>
											<c:otherwise>—</c:otherwise>
										</c:choose></td>
									<td><c:choose>
											<c:when test="${t.fee ne null}">
												<fmt:formatNumber value="${t.fee}" type="number"
													maxFractionDigits="0" groupingUsed="true" /> 円</c:when>
											<c:otherwise>—</c:otherwise>
										</c:choose></td>
									<td>
                      <c:choose>
                      
                       <c:when test="${t.approvedAt eq null and t.hasRemander}">
                          <span class="badge bg-danger text-white" title="${fn:escapeXml(t.remandComment)}">差戻修正</span>
                        </c:when>
                        <c:when test="${t.remandedAt ne null}">
												<span class="badge text-bg-danger">差戻</span>
											</c:when>
                        <c:when test="${t.approvedAt ne null}">
                          <span class="badge text-bg-success">承認済</span>
                        </c:when>
                        <c:otherwise>
                          <span class="badge text-bg-warning">未承認</span>
                        </c:otherwise>
                      </c:choose>
                    </td>

									<td>
										<div class="d-flex gap-1">
											<form method="get"
												action="<%=request.getContextPath()%>/secretary/task/edit"
												class="m-0">
												<input type="hidden" name="id" value="${t.id}" /> <input
													type="hidden" name="companyId"
													value="${t.assignment.customerId}" /> <input type="hidden"
													name="companyName"
													value="${fn:escapeXml(t.assignment.companyName)}" /> <input
													type="hidden" name="yearMonth" value="${yearMonth}" />
												<button type="submit"
													class="btn btn-sm btn-outline-secondary">編集</button>
											</form>
											<form method="post"
												action="<%=request.getContextPath()%>/secretary/task/delete_done"
												class="m-0"
												onsubmit="return confirm('この業務を削除します。よろしいですか？');">
												<input type="hidden" name="id" value="${t.id}" /> <input
													type="hidden" name="companyId"
													value="${t.assignment.customerId}" /> <input type="hidden"
													name="companyName"
													value="${fn:escapeXml(t.assignment.companyName)}" /> <input
													type="hidden" name="yearMonth" value="${yearMonth}" />
												<button type="submit" class="btn btn-sm btn-outline-danger">削除</button>
											</form>
										</div>
									</td>
								</tr>

								<c:if test="${st.last}">
							</tbody>
						</table>
					</div>
					</c:if>
					</c:forEach>
				</div>
	</div>
	</c:otherwise>
	</c:choose>

	</div>
</body>
<script>
(() => {
  const ym  = document.getElementById('ymInput');
  const btn = document.getElementById('btnRegister');
  if (!ym || !btn) return;
  const base = '<%=request.getContextPath()%>/secretary/task/register?yearMonth=';
  const sync = () => btn.href = base + encodeURIComponent(ym.value || '${yearMonth}');
  ym.addEventListener('change', sync);
  sync();
})();
</script>
</html>
