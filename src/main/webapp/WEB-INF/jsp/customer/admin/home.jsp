<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>顧客一覧</title>
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
	rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
	<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf"%>
	<div class="container py-4">
		<div class="d-flex align-items-center justify-content-between mb-3">
			<h1 class="h3 mb-0">顧客一覧</h1>
			<a href="<%= request.getContextPath() %>/admin/customer/register" class="btn btn-primary">
				<i class="bi bi-plus-circle"></i> 新規登録
			</a>
		</div>

		<div class="card shadow-sm">
			<div class="card-body p-0">
				<div class="table-responsive">
					<table class="table table-hover table-bordered align-middle mb-0">
						<thead class="table-primary">
							<tr>
								<th style="width: 72px;">No.</th>
								<th>会社コード</th>
								<th>会社名</th>
								<th>担当者</th>
								<th>連絡先</th>
								<th style="width: 260px;">操作</th>
							</tr>
						</thead>
						<tbody>
							<c:forEach var="cst" items="${customers}" varStatus="st">
								<c:set var="contacts" value="${cst.customerContacts}" />
								<c:choose>
									<%-- 担当者が1名以上いる場合 --%>
									<c:when test="${not empty contacts}">
										<c:set var="rowspan" value="${fn:length(contacts)}" />
										<c:forEach var="ct" items="${contacts}" varStatus="cs">
											<tr>
												<c:if test="${cs.first}">
													<%-- 顧客共通情報はrowspan --%>
													<td class="text-center fw-semibold" rowspan="${rowspan}">${st.index + 1}</td>
													<td class="text-nowrap" rowspan="${rowspan}"><c:out
															value="${cst.companyCode}" /></td>
													<td class="text-nowrap" rowspan="${rowspan}"><a
														href="<%= request.getContextPath() %>/admin/customer/detail?id=${cst.id}"><c:out
																value="${cst.companyName}" /></a></td>
												</c:if>

												<!-- 担当者名だけ縦展開 -->
												<td class="text-nowrap"><c:out value="${ct.name}" /> <c:if
														test="${not empty ct.nameRuby}">
														<div class="small text-muted">
															<c:out value="${ct.nameRuby}" />
														</div>
													</c:if></td>

												<c:if test="${cs.first}">
													<%-- 連絡先は顧客単位でrowspan --%>
													<td class="text-break" rowspan="${rowspan}"><c:if
															test="${not empty cst.mail}">
															<a href="mailto:${cst.mail}">${cst.mail}</a>
															<br />
														</c:if> <c:out value="${cst.phone}" /></td>

													<%-- 操作も顧客単位でrowspan --%>
													<td class="text-center" rowspan="${rowspan}">
														<div class="d-flex gap-1 justify-content-center flex-wrap">
															<!-- 詳細（追加） -->
															<a
																href="<%= request.getContextPath() %>/admin/customer/detail?id=${cst.id}"
																class="btn btn-sm btn-outline-primary">詳細</a>

															<!-- 顧客編集 -->
															<form method="post"
																action="<%=request.getContextPath()%>/admin/customer/edit"
																class="m-0">
																<input type="hidden" name="id" value="${cst.id}">
																<button type="submit" class="btn btn-sm btn-primary">編集</button>
															</form>

															<!-- 顧客削除 -->
															<form method="post"
																action="<%=request.getContextPath()%>/admin/customer/delete"
																class="m-0" onsubmit="return confirm('本当に削除しますか？');">
																<input type="hidden" name="id" value="${cst.id}">
																<button type="submit" class="btn btn-sm btn-danger">削除</button>
															</form>

															<!-- 担当者管理 -->
															<form method="post"
																action="<%=request.getContextPath()%>/admin/contact"
																class="m-0">
																<input type="hidden" name="customerId" value="${cst.id}">
																<button type="submit"
																	class="btn btn-sm btn-outline-primary">担当者管理</button>
															</form>
														</div>
													</td>
												</c:if>
											</tr>
										</c:forEach>
									</c:when>

									<%-- 担当者0名の場合 --%>
									<c:otherwise>
										<tr>
											<td class="text-center fw-semibold">${st.index + 1}</td>
											<td class="text-nowrap"><c:out
													value="${cst.companyCode}" /></td>
											<td class="text-nowrap"><c:out
													value="${cst.companyName}" /></td>
											<td class="text-muted">—（担当者未登録）</td>
											<td class="text-break"><c:if
													test="${not empty cst.mail}">
													<a href="mailto:${cst.mail}">${cst.mail}</a>
													<br />
												</c:if> <c:out value="${cst.phone}" /><br /> <c:if
													test="${not empty cst.postalCode}">〒<c:out
														value="${cst.postalCode}" />
													<br />
												</c:if> <c:out value="${cst.address1}" /> <c:out
													value="${cst.address2}" /><br /> <c:out
													value="${cst.building}" /></td>
											<td class="text-center">
												<div class="d-flex gap-1 justify-content-center flex-wrap">
													<!-- 詳細（追加） -->
													<a
														href="<%= request.getContextPath() %>/admin/customer/detail?id=${cst.id}"
														class="btn btn-sm btn-outline-primary">詳細</a>

													<!-- 顧客編集 -->
													<form method="post"
														action="<%=request.getContextPath()%>/admin/customer/edit"
														class="m-0">
														<input type="hidden" name="id" value="${cst.id}">
														<button type="submit" class="btn btn-sm btn-primary">編集</button>
													</form>

													<!-- 顧客削除 -->
													<form method="post"
														action="<%=request.getContextPath()%>/admin/customer/delete"
														class="m-0" onsubmit="return confirm('本当に削除しますか？');">
														<input type="hidden" name="id" value="${cst.id}">
														<button type="submit" class="btn btn-sm btn-danger">削除</button>
													</form>

													<!-- 担当者管理 -->
													<form method="post"
														action="<%=request.getContextPath()%>/admin/contact"
														class="m-0">
														<input type="hidden" name="customerId" value="${cst.id}">
														<button type="submit"
															class="btn btn-sm btn-outline-primary">担当者管理</button>
													</form>
												</div>
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
						value="${fn:length(customers)}" /></span>
			</div>
		</div>
	</div>
	<script
		src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
