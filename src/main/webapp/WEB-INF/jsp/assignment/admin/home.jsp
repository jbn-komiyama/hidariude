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
<title>${targetYM}アサイン一覧</title>
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
	rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf" %>
	<div class="container py-4">
		<div class="d-flex align-items-center justify-content-between mb-3">
			<h1 class="h3 mb-0">${targetYM}のアサイン</h1>
		</div>
		<!--    ページネーション-->
		<div class="ym-nav d-flex align-items-center">
			<a class="btn btn-link p-0 px-2 text-muted fs-4 js-prev" href="#"
				aria-label="前月">‹</a> <a class="btn btn-link px-3 js-today" href="#"
				style="text-decoration: none;">今月</a> <a
				class="btn btn-link p-0 px-2 text-muted fs-4 js-next" href="#"
				aria-label="次月">›</a>
		</div>
		<div class="card shadow-sm">
			<div class="card-body p-0">
				<c:if test="${not empty errorMsg}">
					<div class="alert alert-danger">
						<ul class="mb-0">
							<c:forEach var="msg" items="${errorMsg}">
								<li><c:out value="${msg}" /></li>
							</c:forEach>
						</ul>
					</div>
				</c:if>
				<div class="table-responsive">
					<table class="table table-hover table-bordered align-middle mb-0">
						<thead class="table-primary">
							<tr>
								<th style="width: 72px;">No.</th>
								<th>顧客名</th>
								<th>秘書名</th>
								<th>秘書ランク</th>
								<th>ランク</th>
								<th class="text-end">基本単価</th>
								<th class="text-end">秘書単価</th>
								<th class="text-end">継続単価</th>
								<th class="text-end">合計単価</th>
							</tr>
						</thead>
						<tbody>
							<c:forEach var="c" items="${customers}" varStatus="st">
								<c:set var="groups" value="${c.assignmentGroups}" />
								<c:choose>
									<c:when test="${not empty groups}">
										<c:set var="customerRowspan" value="0" />
										<c:forEach var="g" items="${groups}">
											<c:set var="customerRowspan"
												value="${customerRowspan + fn:length(g.assignments)}" />
										</c:forEach>

										<c:forEach var="g" items="${groups}" varStatus="gs">
											<c:forEach var="a" items="${g.assignments}" varStatus="as">
												<tr>
													<c:if test="${gs.first && as.first}">
														<td class="text-center fw-semibold"
															rowspan="${customerRowspan}">${st.index + 1 }</td>

														<td class="text-nowrap" rowspan="${customerRowspan}">
															<c:out value="${c.companyName}" /> <c:set var="hasPm"
																value="false" /> <c:forEach var="g2" items="${groups}">
																<c:if test="${g2.secretary != null}">
																	<c:set var="hasPm" value="true" />
																</c:if>
															</c:forEach> <c:if test="${hasPm}">
																<form method="post"
																	action="<%=request.getContextPath()%>/admin/assignment/register"
																	class="d-inline ms-2">
																	<input type="hidden" name="id" value="${c.id}">
																	<input type="hidden" name="companyName"
																		value="${c.companyName}">
																	<button type="submit"
																		class="btn btn-sm btn-outline-primary">秘書登録</button>
																</form>
															</c:if>
														</td>
													</c:if>

													<c:if test="${as.first}">
														<td class="text-nowrap"
															rowspan="${fn:length(g.assignments)}"><c:out
																value="${g.secretary != null ? g.secretary.name : '—'}" />
														</td>
														<td class="text-nowrap"
															rowspan="${fn:length(g.assignments)}"><c:out
																value="${g.secretary != null && g.secretary.secretaryRank != null ? g.secretary.secretaryRank.rankName : '—'}" />
														</td>
													</c:if>

													<td class="text-nowrap"><c:out
															value="${a.taskRankName != null ? a.taskRankName : '—'}" />
													</td>

													<td class="text-nowrap text-end"><fmt:formatNumber
															value="${a.basePayCustomer}" pattern="#,##0" /> (<fmt:formatNumber
															value="${a.basePaySecretary}" pattern="#,##0" />)</td>

													<td class="text-nowrap text-end"><fmt:formatNumber
															value="${a.increaseBasePayCustomer}" pattern="#,##0" />
														(<fmt:formatNumber value="${a.increaseBasePaySecretary}"
															pattern="#,##0" />)</td>

													<td class="text-nowrap text-end"><fmt:formatNumber
															value="${a.customerBasedIncentiveForCustomer}"
															pattern="#,##0" /> (<fmt:formatNumber
															value="${a.customerBasedIncentiveForSecretary}"
															pattern="#,##0" />)</td>

													<td class="text-end"><fmt:formatNumber
															value="${
			                  (a.basePayCustomer) + (a.increaseBasePayCustomer) + (a.customerBasedIncentiveForCustomer)
			                }"
															pattern="#,##0" /> (<fmt:formatNumber
															value="${
			                  (a.basePaySecretary) + (a.increaseBasePaySecretary) + (a.customerBasedIncentiveForSecretary)
			                }"
															pattern="#,##0" />)</td>
												</tr>
											</c:forEach>
										</c:forEach>
									</c:when>

									<c:otherwise>
										<tr>
											<td class="text-center fw-semibold">${st.index + 1}</td>
											<td class="text-nowrap"><c:out value="${c.companyName}" /></td>
											<td colspan="7" class="text-center text-muted">
												<form method="post"
													action="<%=request.getContextPath()%>/admin/assignment/pm_register">
													<input type="hidden" name="companyId" value="${c.id}">
													<input type="hidden" name="companyName"
														value="${c.companyName}"> <input type="hidden"
														name="targetYM" value="${targetYM}">
													<button type="submit" class="btn btn-sm btn-primary">PM秘書登録</button>
												</form>
											</td>
										</tr>
									</c:otherwise>
								</c:choose>
							</c:forEach>
						</tbody>
					</table>
				</div>
			</div>

			<div class="card-footer text-end small text-muted">
				件数：<span class="fw-semibold"><c:out
						value="${fn:length(assignments)}" /></span>
			</div>
		</div>
	</div>
	<!--  ページネーション-->
	<script>
		(function() {
			var serverYm = '${targetYM}';
			var urlYm = new URLSearchParams(location.search).get('targetYM');
			var ym = urlYm || serverYm;

			function ymToDate(ym) {
				var p = (ym || '').split('-');
				return new Date(parseInt(p[0], 10),
						(parseInt(p[1], 10) || 1) - 1, 1);
			}
			function dateToYm(d) {
				var y = d.getFullYear();
				var m = ('0' + (d.getMonth() + 1)).slice(-2);
				return y + '-' + m;
			}
			function addMonths(ym, delta) {
				var d = ymToDate(ym);
				d.setMonth(d.getMonth() + delta);
				return dateToYm(d);
			}

			document.querySelectorAll('.js-prev').forEach(function(a) {
				a.href = '?targetYM=' + addMonths(ym, -1);
			});
			document.querySelectorAll('.js-next').forEach(function(a) {
				a.href = '?targetYM=' + addMonths(ym, +1);
			});
			document.querySelectorAll('.js-today').forEach(function(a) {
				a.href = '?targetYM=' + dateToYm(new Date());
			});
		})();
	</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>