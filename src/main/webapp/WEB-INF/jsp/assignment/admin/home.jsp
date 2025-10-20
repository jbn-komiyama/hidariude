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
<style>
.ym-nav .disabled {
	pointer-events: none;
	opacity: .4;
}
</style>
</head>
<body class="bg-primary bg-opacity-10">
	<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf"%>

	<div class="container py-4">

		<!-- タイトル＋対象年月＋引継ぎ -->
		<div class="d-flex align-items-center justify-content-between mb-3">
			<div>
				<h1 class="h3 mb-1">アサイン表</h1>
				<div class="text-muted small">
					対象年月：<span>${targetYM}</span>
				</div>
			</div>

			<c:if test="${canCarryOver}">
				<form method="get"
					action="${pageContext.request.contextPath}/admin/assignment/carry_over_preview"
					class="ms-3">
					<input type="hidden" name="fromYM" value="${prevYM}"> <input
						type="hidden" name="toYM" value="${targetYM}">
					<button type="submit" class="btn btn-primary">先月のアサインを引き継ぐ</button>
				</form>
			</c:if>
		</div>

		<!-- 検索フォーム（※月UIはここから撤去） -->
		<div class="card mb-3">
			<div class="card-body">
				<form method="get"
					action="${pageContext.request.contextPath}/admin/assignment"
					class="row g-3 align-items-end">
					<input type="hidden" name="targetYM" value="${targetYM}">

					<div class="col-md-3">
						<label class="form-label small mb-1">継続月数（以上）</label>
						<div class="input-group input-group-sm">
							<span class="input-group-text">≧</span> <input type="number"
								min="0" step="1" class="form-control" name="minMonths"
								value="${f_minMonths}" placeholder="例: 6">
						</div>
					</div>

					<div class="col-md-3">
						<label class="form-label small mb-1">担当秘書</label> <select
							class="form-select form-select-sm" name="secretaryId">
							<option value="">（すべて）</option>
							<c:forEach var="s" items="${secretaries}">
								<option value="${s.id}"
									<c:if test="${f_secretaryId == s.id}">selected</c:if>>
									<c:out value="${s.name}" />
								</option>
							</c:forEach>
						</select>
					</div>

					<div class="col-md-4">
						<label class="form-label small mb-1">顧客名</label> <input
							type="text" class="form-control form-control-sm" name="qCustomer"
							value="${f_qCustomer}" placeholder="部分一致">
					</div>

					<div class="col-md-2">
						<label class="form-label small mb-1 d-block">ソート</label>
						<div class="form-check form-switch">
							<input class="form-check-input" type="checkbox" id="sortSwitch"
								name="sort" value="months_desc"
								<c:if test="${f_sort == 'months_desc'}">checked</c:if>>
							<label class="form-check-label small" for="sortSwitch">継続月数の高い順</label>
						</div>
					</div>

					<div class="col-12 d-flex gap-2">
						<button class="btn btn-primary btn-sm">検索</button>
						<a class="btn btn-outline-secondary btn-sm"
							href="${pageContext.request.contextPath}/admin/assignment?targetYM=${targetYM}">
							条件クリア </a>
					</div>
				</form>
			</div>
		</div>

		<!-- 月ナビ（検索枠の外／表の直前） -->
		<div class="d-flex align-items-center justify-content-between mb-2">
			<div class="d-flex align-items-center gap-2">
				<a class="btn btn-link p-0 px-2 text-muted fs-4 js-prev" href="#"
					aria-label="前月">‹</a> <span
					class="badge bg-light border text-dark fw-semibold js-ym-text"
					style="padding: .35rem .6rem;">${targetYM}</span> <a
					class="btn btn-link p-0 px-2 text-muted fs-4 js-next" href="#"
					aria-label="次月">›</a> <a
					class="btn btn-link p-0 ms-2 small js-today" href="#"
					style="text-decoration: none;">今月へ</a>

				<!-- コンパクトな month 入力もここへ移動 -->
				<form id="ymForm" method="get"
					action="${pageContext.request.contextPath}/admin/assignment"
					class="ms-2 d-inline-flex align-items-center gap-1">
					<input type="month" class="form-control form-control-sm"
						style="width: 170px;" name="targetYM" value="${targetYM}"
						max="${maxYM}">
					<!-- 既存の検索条件は維持 -->
					<input type="hidden" name="minMonths" value="${f_minMonths}">
					<input type="hidden" name="secretaryId" value="${f_secretaryId}">
					<input type="hidden" name="qCustomer" value="${f_qCustomer}">
					<input type="hidden" name="sort" value="${f_sort}">
					<button class="btn btn-outline-secondary btn-sm">移動</button>
				</form>
			</div>

			<div class="text-muted small">
				最大表示可能月（来月まで）：<span class="fw-semibold">${maxYM}</span>
			</div>
		</div>



		<!-- 結果テーブル -->
		<div class="card shadow-sm">
			<div class="card-body p-0">
				<c:if test="${not empty errorMsg}">
					<div class="alert alert-danger m-3">
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
								<th class="text-center" style="width: 100px;">継続（月）</th>
								<th>ランク</th>
								<th class="text-end">基本単価</th>
								<th class="text-end">秘書単価</th>
								<th class="text-end">継続単価</th>
								<th class="text-end">合計単価</th>
								<th style="width: 100px;">操作</th>
							</tr>
						</thead>
						<tbody>
							<c:set var="totalRows" value="0" />
							<c:forEach var="c" items="${customers}" varStatus="st">
								<c:set var="groups" value="${c.assignmentGroups}" />
								<c:choose>
									<c:when test="${not empty groups}">
										<c:set var="customerRowspan" value="0" />
										<c:forEach var="g0" items="${groups}">
											<c:set var="customerRowspan"
												value="${customerRowspan + fn:length(g0.assignments)}" />
										</c:forEach>

										<c:forEach var="g" items="${groups}" varStatus="gs">
											<c:forEach var="a" items="${g.assignments}" varStatus="as">
												<c:set var="totalRows" value="${totalRows + 1}" />
												<tr>
													<c:if test="${gs.first && as.first}">
														<td class="text-center fw-semibold"
															rowspan="${customerRowspan}">${st.index + 1}</td>
														<td class="text-nowrap" rowspan="${customerRowspan}">
															<a href="${pageContext.request.contextPath}/admin/customer/detail?id=${c.id}"><c:out value="${c.companyName}" /></a> <c:set var="hasPm"
																value="false" /> <c:forEach var="g2" items="${groups}">
																<c:if test="${g2.secretary != null}">
																	<c:set var="hasPm" value="true" />
																</c:if>
															</c:forEach> <c:if test="${hasPm}">
																<form method="get"
																	action="${pageContext.request.contextPath}/admin/assignment/register"
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
															rowspan="${fn:length(g.assignments)}"><a href="${pageContext.request.contextPath}/admin/secretary/detail?id=${g.secretary.id}"><c:out
																value="${g.secretary != null ? g.secretary.name : '—'}" /></a>
														</td>
														<td class="text-nowrap"
															rowspan="${fn:length(g.assignments)}"><c:out
																value="${g.secretary != null && g.secretary.secretaryRank != null ? g.secretary.secretaryRank.rankName : '—'}" />
														</td>
														<!-- 継続月数：顧客×秘書で同一なのでグループ1回だけ表示 -->
														<!-- 変更後（各行ごとに表示） -->

													</c:if>
													<td class="text-center"><c:out
															value="${contMonths[a.id]}" /></td>

													<td class="text-nowrap"><c:out
															value="${a.taskRankName != null ? a.taskRankName : '—'}" /></td>

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
															value="${(a.basePayCustomer) + (a.increaseBasePayCustomer) + (a.customerBasedIncentiveForCustomer)}"
															pattern="#,##0" /> (<fmt:formatNumber
															value="${(a.basePaySecretary) + (a.increaseBasePaySecretary) + (a.customerBasedIncentiveForSecretary)}"
															pattern="#,##0" />)</td>

													<td class="text-nowrap">
														<c:set var="isRankP" value="${a.taskRankName == 'P'}" />

														<c:choose>
															<c:when test="${isRankP}">
																<button type="button"
																	class="btn btn-sm btn-outline-secondary" disabled
																	title="ランクPは変更できません">変更</button>
															</c:when>

															<c:otherwise>
																<form method="get"
																	action="${pageContext.request.contextPath}/admin/assignment/edit"
																	class="d-inline">
																	<input type="hidden" name="id" value="${a.id}">
																	<input type="hidden" name="targetYM"
																		value="${targetYM}">
																	<button type="submit"
																		class="btn btn-sm btn-outline-secondary">変更</button>
																</form>
															</c:otherwise>
														</c:choose>
														<form method="post"
															action="${pageContext.request.contextPath}/admin/assignment/delete"
															class="d-inline"
															onsubmit="return confirm('このアサインを削除します。よろしいですか？');">
															<input type="hidden" name="id" value="${a.id}"> <input
																type="hidden" name="targetYM" value="${targetYM}">
															<button type="submit"
																class="btn btn-sm btn-outline-danger">削除</button>
														</form>
													</td>
												</tr>
											</c:forEach>
										</c:forEach>
									</c:when>

									<c:otherwise>
										<tr>
											<td class="text-center fw-semibold">${st.index + 1}</td>
											<td class="text-nowrap"><a href="${pageContext.request.contextPath}/admin/customer/detail?id=${c.id}"><c:out value="${c.companyName}" /></a></td>
											<td colspan="8" class="text-center text-muted">
												<form method="post"
													action="${pageContext.request.contextPath}/admin/assignment/pm_register">
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

			<div
				class="card-footer d-flex justify-content-between small text-muted">
				<div>
					件数：<span class="fw-semibold"><c:out value="${totalRows}" /></span>
				</div>
				<div>
					最大表示可能月（来月まで）：<span class="fw-semibold"><c:out
							value="${maxYM}" /></span>
				</div>
			</div>
		</div>
	</div>

	<script>
(function() {
  var serverYm = '${targetYM}';
  var maxYm    = '${maxYM}';
  var url      = new URL(location.href);
  var params   = new URLSearchParams(url.search);

  var ym = params.get('targetYM') || serverYm;

  function ymToDate(ym){ var p=(ym||'').split('-'); return new Date(+p[0], (+p[1]||1)-1, 1); }
  function dateToYm(d){ var y=d.getFullYear(), m=('0'+(d.getMonth()+1)).slice(-2); return y+'-'+m; }
  function addMonths(ym, n){ var d=ymToDate(ym); d.setMonth(d.getMonth()+n); return dateToYm(d); }
  function clampNext(ym, maxYm){ return ym>maxYm ? maxYm : ym; }
  function buildHref(newYm){
    var p = new URLSearchParams(params.toString());
    p.set('targetYM', newYm);
    return '?' + p.toString();
  }

  var prevYm  = addMonths(ym, -1);
  var nextYm  = clampNext(addMonths(ym, +1), maxYm);
  var todayYm = clampNext(dateToYm(new Date()), maxYm);

  document.querySelectorAll('.js-prev').forEach(a => a.href = buildHref(prevYm));
  document.querySelectorAll('.js-next').forEach(a => a.href = buildHref(nextYm));
  document.querySelectorAll('.js-today').forEach(a => a.href = buildHref(todayYm));
  document.querySelectorAll('.js-ym-text').forEach(el => el.textContent = ym);

  if (ym >= maxYm) {
    document.querySelectorAll('.js-next').forEach(a => { a.classList.add('disabled'); a.removeAttribute('href'); });
  }
})();
</script>


	<script
		src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
