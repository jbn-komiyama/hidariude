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
<title>業務一覧</title>
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
	rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">

	<div class="container py-4">

		<div class="d-flex justify-content-between align-items-center mb-3">
			<div>
				<h1 class="h4 mb-1">業務一覧</h1>
				<div class="text-muted small">
					会社名：<strong>${fn:escapeXml(companyName)}</strong> ／ 年月：<strong>${yearMonth}</strong>
				</div>
			</div>

			<div class="d-flex gap-2">
				<form method="get"
					action="<%=request.getContextPath()%>/secretary/task/list"
					class="d-flex align-items-center gap-2">
					<input type="hidden" name="companyId" value="${customerId}" /> <input
						type="hidden" name="companyName"
						value="${fn:escapeXml(companyName)}" /> <input type="month"
						name="yearMonth" class="form-control form-control-sm"
						value="${yearMonth}" />
					<button type="submit" class="btn btn-sm btn-outline-primary">表示</button>
				</form>

				<a href="<%=request.getContextPath()%>/secretary/home"
					class="btn btn-sm btn-outline-secondary">戻る</a>
			</div>
		</div>

		<!-- 合計稼働の算出 -->
		<c:set var="totalMinutes" value="0" />
		<c:forEach var="t" items="${tasks}">
			<c:set var="m" value="${t.workMinute != null ? t.workMinute : 0}" />
			<c:set var="totalMinutes" value="${totalMinutes + m}" />
		</c:forEach>
		<c:set var="totalHoursRaw"
			value="${(totalMinutes - (totalMinutes % 60)) / 60}" />
		<c:set var="remMinutes" value="${totalMinutes % 60}" />

		<div class="alert alert-info">
			<span class="me-3">件数：<strong>${fn:length(tasks)}</strong></span> <span>
				合計稼働： <strong><fmt:formatNumber value="${totalHoursRaw}"
						type="number" maxFractionDigits="0" /></strong> 時間 <strong>${remMinutes}</strong>
				分
			</span> <span class="me-3">合計金額：<strong> <fmt:formatNumber
						value="${sum}" type="number" maxFractionDigits="0"
						groupingUsed="true" /> 円
			</strong></span>
		</div>

		<!-- 一覧テーブル -->
		<div class="card shadow-sm">
			<div class="card-header bg-light">
				<div class="d-flex align-items-center justify-content-between">
					<span class="fw-semibold">一覧（${fn:escapeXml(companyName)} /
						${yearMonth}）</span>
				</div>
			</div>

			<div class="card-body p-0">
				<c:choose>
					<c:when test="${empty tasks}">
						<div class="p-4 text-center text-muted">
							この月の業務はありません。<br /> 「業務登録」ボタンから登録できます。
						</div>
					</c:when>
					<c:otherwise>
						<div class="table-responsive">
							<table class="table table-sm table-hover align-middle mb-0">
								<thead class="table-secondary">
									<tr>
										<th style="width: 140px;">日付</th>
										<th style="width: 130px;">時間</th>
										<th style="width: 60px;">稼働</th>
										<th style="width: 60px;">ランク</th>
										<th>内容</th>
										<th style="width: 90px;">単価</th>
										<th style="width: 90px;">報酬</th>
										<th style="width: 90px;">承認</th>
										<th style="width: 140px;">操作</th>
									</tr>
								</thead>
								<tbody>
									<c:forEach var="t" items="${tasks}">
										<tr>
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
															maxFractionDigits="0" />分
                          </c:when>
													<c:otherwise>—</c:otherwise>
												</c:choose></td>
											<td><c:choose>
													<c:when test="${t.assignment.taskRankName ne null}">
                            ${t.assignment.taskRankName}
                          </c:when>
													<c:otherwise>—</c:otherwise>
												</c:choose></td>
											<td>${fn:escapeXml(t.workContent)}</td>

											<td><c:choose>
													<c:when
														test="${t.assignment ne null && t.assignment.basePaySecretary ne null}">
														<fmt:formatNumber value="${t.hourFee}" type="number"
															maxFractionDigits="0" groupingUsed="true" /> 円
                          </c:when>
													<c:otherwise>—</c:otherwise>
												</c:choose></td>

											<td><c:choose>
													<c:when
														test="${t.assignment ne null && t.assignment.basePaySecretary ne null}">
														<fmt:formatNumber value="${t.fee}" type="number"
															maxFractionDigits="0" groupingUsed="true" /> 円
                          </c:when>
													<c:otherwise>—</c:otherwise>
												</c:choose></td>

											<td><c:choose>
													<c:when test="${t.approvedAt ne null}">
														<span class="badge text-bg-success">承認済み</span>
														<div class="small text-muted">
															<fmt:formatDate value="${t.approvedAt}"
																pattern="yyyy-MM-dd HH:mm" timeZone="Asia/Tokyo" />
														</div>
													</c:when>
													<c:otherwise>
														<span class="badge text-bg-warning">未承認</span>
													</c:otherwise>
												</c:choose></td>
											<td>
												<div class="d-flex gap-1">
													<form method="get"
														action="<%=request.getContextPath()%>/secretary/task/edit"
														class="m-0">
														<input type="hidden" name="id" value="${t.id}" /> <input
															type="hidden" name="companyId" value="${customerId}" />
														<input type="hidden" name="companyName"
															value="${fn:escapeXml(companyName)}" /> <input
															type="hidden" name="yearMonth" value="${yearMonth}" />
														<button type="submit"
															class="btn btn-sm btn-outline-secondary"
															<c:if test="${t.approvedAt ne null}">disabled title="承認済みは編集できません"</c:if>>
															編集</button>
													</form>

													<form method="post"
														action="<%=request.getContextPath()%>/secretary/task/delete_done"
														class="m-0"
														onsubmit="return confirm('この業務を削除します。よろしいですか？');">
														<input type="hidden" name="id" value="${t.id}" /> <input
															type="hidden" name="companyId" value="${customerId}" />
														<input type="hidden" name="companyName"
															value="${fn:escapeXml(companyName)}" /> <input
															type="hidden" name="yearMonth" value="${yearMonth}" />
														<button type="submit"
															class="btn btn-sm btn-outline-danger"
															<c:if test="${t.approvedAt ne null}">disabled title="承認済みは削除できません"</c:if>>
															削除</button>
													</form>
												</div>
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

		<!-- ===== 新規業務登録（当月のみ選択可能 / 稼働は自動計算） ===== -->
		<jsp:useBean id="now" class="java.util.Date" />
		<fmt:formatDate value="${now}" pattern="yyyy-MM-dd"
			timeZone="Asia/Tokyo" var="todayStr" />

		<!-- yearMonth から当月の最小/最大日付を生成 -->
		<fmt:parseDate value="${yearMonth}-01" pattern="yyyy-MM-dd"
			var="ymFirstDate" />
		<jsp:useBean id="cal" class="java.util.GregorianCalendar" />
		<%
		java.util.Date d = (java.util.Date) pageContext.findAttribute("ymFirstDate");
		java.util.Calendar c = (java.util.Calendar) pageContext.findAttribute("cal");
		c.setTime(d);
		java.util.Date minD = c.getTime(); // 当月1日
		c.add(java.util.Calendar.MONTH, 1);
		c.add(java.util.Calendar.DATE, -1); // 当月末日
		java.util.Date maxD = c.getTime();
		pageContext.setAttribute("minD", minD);
		pageContext.setAttribute("maxD", maxD);
		%>
		<fmt:formatDate value="${minD}" pattern="yyyy-MM-dd" var="minDate" />
		<fmt:formatDate value="${maxD}" pattern="yyyy-MM-dd" var="maxDate" />

		<div class="mt-4">
			<h2 class="h6 mb-3">新規業務登録</h2>
			<form id="taskRegisterForm" method="post"
				action="<%=request.getContextPath()%>/secretary/task/register_done">
				<input type="hidden" name="companyId" value="${customerId}" /> <input
					type="hidden" name="companyName"
					value="${fn:escapeXml(companyName)}" /> <input type="hidden"
					name="yearMonth" value="${yearMonth}" />
				<!-- 自動計算した稼働分をPOSTする用 -->
				<input type="hidden" name="workMinute" id="workMinute" />

				<table class="table table-bordered table-sm bg-white">
					<thead class="table-light">
						<tr>
							<th style="width: 120px;">日付</th>
							<th style="width: 200px;">時間</th>
							<th style="width: 90px;">稼働</th>
							<th style="width: 120px;">ランク</th>
							<th>内容</th>
							<th style="width: 100px;">操作</th>
						</tr>
					</thead>
					<tbody>
						<tr>
							<td><input type="date" name="workDate" id="workDate"
								class="form-control form-control-sm" value="${todayStr}"
								min="${minDate}" max="${maxDate}" required /></td>
							<td>
								<div class="d-flex">
									<input type="time" name="startTime" id="startTime"
										class="form-control form-control-sm" required /> <span
										class="mx-1">～</span> <input type="time" name="endTime"
										id="endTime" class="form-control form-control-sm" required />
								</div>
							</td>
							<!-- 稼働は自動計算して表示のみ -->
							<td><span id="workMinuteText" class="fw-semibold">—</span> 分
							</td>
							<td><c:choose>
									<c:when test="${not empty assignments}">
										<select name="assignmentId" class="form-select form-select-sm"
											required>
											<option value="" disabled selected>選択してください</option>
											<c:forEach var="a" items="${assignments}">
												<option value="${a.id}">${fn:escapeXml(a.taskRankName)}</option>
											</c:forEach>
										</select>
									</c:when>
									<c:otherwise>
										<div class="text-muted small">先にアサイン（ランク）を作成してください。</div>
									</c:otherwise>
								</c:choose></td>
							<td><input type="text" name="workContent"
								class="form-control form-control-sm" placeholder="内容" required />
							</td>
							<td>
								<button type="submit" id="submitBtn"
									class="btn btn-sm btn-success"
									<c:if test="${empty assignments}">disabled</c:if>>登録</button>
							</td>
						</tr>
					</tbody>
				</table>
			</form>
		</div>

	</div>

	<script>
    (function() {
      const start  = document.getElementById('startTime');
      const end    = document.getElementById('endTime');
      const text   = document.getElementById('workMinuteText');
      const hidden = document.getElementById('workMinute');
      const btn    = document.getElementById('submitBtn');
      const form   = document.getElementById('taskRegisterForm');
      const dateEl = document.getElementById('workDate');

      // 当月範囲内に日付を補正（todayが別月ならminへ）
      function clampDateToRange() {
        const v = dateEl.value;
        const min = dateEl.min;
        const max = dateEl.max;
        if (v && min && v < min) dateEl.value = min;
        if (v && max && v > max) dateEl.value = max;
      }
      clampDateToRange();

      function parseMinutes(hhmm) {
        if (!hhmm || !/^\d{2}:\d{2}$/.test(hhmm)) return null;
        const [h, m] = hhmm.split(':').map(Number);
        return h * 60 + m;
      }

      function recalc() {
        const s = parseMinutes(start.value);
        const e = parseMinutes(end.value);

        if (s == null || e == null) {
          text.textContent = '—';
          hidden.value = '';
          btn.disabled = true;
          return;
        }

        // 同日想定。終業 <= 始業 は無効。
        const diff = e - s;
        if (diff <= 0) {
          text.textContent = '—';
          hidden.value = '';
          btn.disabled = true;
          return;
        }

        text.textContent = diff.toString();
        hidden.value = diff.toString();
        btn.disabled = false;
      }

      start.addEventListener('change', recalc);
      end.addEventListener('change', recalc);
      // 初期状態
      recalc();

      // 送信前の最終ガード
      form.addEventListener('submit', function(ev) {
        if (!hidden.value) {
          ev.preventDefault();
          alert('開始時刻・終了時刻を正しく入力してください。');
        }
        // 当月範囲外ならブロック
        if (dateEl.value && (dateEl.value < dateEl.min || dateEl.value > dateEl.max)) {
          ev.preventDefault();
          alert('日付は当月のみ選択できます。');
        }
      });
    })();
  </script>
</body>
</html>
