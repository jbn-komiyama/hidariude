<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<title>タスク アラート一覧</title>
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
	rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
	<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf"%>

	<div class="container py-4">
		<h1 class="h3 mb-3">タスク アラート一覧</h1>

		<c:if test="${not empty errorMsg}">
			<div class="alert alert-danger" role="alert">
				<ul class="mb-0">
					<c:forEach var="m" items="${errorMsg}">
						<li><c:out value="${m}" /></li>
					</c:forEach>
				</ul>
			</div>
		</c:if>

		<div class="card shadow-sm">
			<div class="card-header fw-bold">顧客からの確認申請（アラート）</div>
			<div class="card-body p-0">
				<div class="table-responsive">
					<table class="table table-sm table-striped mb-0 align-middle">
						<thead class="table-light">
							<tr>
								<th style="width: 3rem;">#</th>
								<th>会社名・秘書名</th>
								<th style="width: 10rem;">日時</th>
								<th style="width: 3rem;">ランク</th>
								<th style="width: 13rem;">仕事内容</th>
								<th style="width: 13rem;">アラート内容</th>
								<th style="width: 6rem;">状態</th>
								<th style="width: 13rem;">差戻コメント</th>
								<th style="width: 8rem;">操作</th>
							</tr>
						</thead>
						<tbody>
							<c:forEach var="t" items="${tasks}" varStatus="st">
								<tr>
									<td><c:out value="${st.count}" /></td>
									<td><c:out value="${t.assignment.companyName}" /><br>
										<c:out value="${t.assignment.secretaryName}" /></td>
									<td><c:choose>
											<c:when test="${not empty t.workDate}">
												<fmt:formatDate value="${t.workDate}" pattern="yyyy-MM-dd" />
												<br>
												<fmt:formatDate value="${t.startTime}" pattern="HH:mm"
													timeZone="Asia/Tokyo" />～<fmt:formatDate
													value="${t.endTime}" pattern="HH:mm" timeZone="Asia/Tokyo" />
												(<c:out value="${empty t.workMinute ? '—' : t.workMinute}" />分)
                      </c:when>
											<c:otherwise>—</c:otherwise>
										</c:choose></td>
									<td><c:out
											value="${empty t.assignment.taskRankName ? '—' : t.assignment.taskRankName}" /></td>
									<td><c:out value="${t.workContent}" /></td>
									<td><c:out value="${t.alertComment}" /></td>
									<td><c:choose>
											<c:when
												test="${not empty t.alertedAt and empty t.remandedById}">
												<span class="badge bg-secondary">未処理</span>
											</c:when>

											<c:when
												test="${not empty t.alertedAt and not empty t.remandedById and not empty t.remandedAt}">
												<span class="badge bg-danger">差戻中</span>
											</c:when>

											<c:when
												test="${not empty t.alertedAt and not empty t.remandedById and empty t.remandedAt}">
												<span class="badge bg-success">対応済</span>
											</c:when>

											<c:otherwise>
												<span class="badge bg-light text-muted">—</span>
											</c:otherwise>
										</c:choose></td>
									<td><c:out value="${t.remandComment}" /></td>
									<td class="text-nowrap">
										<!-- 差戻：コメントなしでも送信可（実装済の adminTaskRemandDone を想定） -->
										<button type="button" class="btn btn-sm btn-outline-danger"
											data-bs-toggle="modal" data-bs-target="#remandModal"
											data-task-id="${t.id}">差戻</button>
										<form method="post"
											action="<c:url value='/admin/task/alert_delete'/>"
											class="d-inline"
											onsubmit="return confirm('このアラートを取消します。よろしいですか？');">
											<input type="hidden" name="id" value="${t.id}" />
											<button type="submit"
												class="btn btn-sm btn-outline-secondary">取消</button>
										</form>
									</td>
								</tr>
							</c:forEach>
							<c:if test="${empty tasks}">
								<tr>
									<td colspan="11" class="text-center text-muted py-3">アラートはありません</td>
								</tr>
							</c:if>
						</tbody>
					</table>
				</div>
			</div>
		</div>
	</div>
	<div class="modal fade" id="remandModal" tabindex="-1"
		aria-hidden="true">
		<div class="modal-dialog">
			<form method="post"
				action="<%=request.getContextPath()%>/admin/task/remand_done"
				class="modal-content">
				<input type="hidden" name="taskId" id="remandTaskId"> <input
					type="hidden" name="yearMonth" value="${yearMonth}"> <input
					type="hidden" name="sec" value="${sec}"> <input
					type="hidden" name="cust" value="${cust}">
				<div class="modal-header">
					<h5 class="modal-title">差戻コメント</h5>
					<button type="button" class="btn-close" data-bs-dismiss="modal"></button>
				</div>
				<div class="modal-body">
					<textarea class="form-control" name="remandComment" rows="5"
						placeholder="差戻理由を記入してください（相手に通知されます）" required></textarea>
				</div>
				<div class="modal-footer">
					<button type="submit" class="btn btn-danger">差戻する</button>
					<button type="button" class="btn btn-secondary"
						data-bs-dismiss="modal">キャンセル</button>
				</div>
			</form>
		</div>
	</div>
	<script>
document.getElementById('remandModal')?.addEventListener('show.bs.modal', function (event) {
  const btn = event.relatedTarget;
  const taskId = btn?.getAttribute('data-task-id');
  document.getElementById('remandTaskId').value = taskId || '';
});
</script>
	<script
		src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
