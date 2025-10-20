<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<fmt:setLocale value="ja_JP" />
<fmt:setTimeZone value="Asia/Tokyo" />
<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<title>タスク一覧（顧客）</title>
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
	rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
	<%@ include file="/WEB-INF/jsp/_parts/customer/navbar.jspf"%>

	<div class="container py-4">
		<h1 class="h3 mb-3">タスク一覧</h1>

		<!-- エラー表示（あれば） -->
		<c:if test="${not empty errorMsg}">
			<div class="alert alert-danger" role="alert">
				<ul class="mb-0">
					<c:forEach var="m" items="${errorMsg}">
						<li><c:out value="${m}" /></li>
					</c:forEach>
				</ul>
			</div>
		</c:if>
		<div class="d-flex justify-content-between align-items-center mb-2">
			<form method="get" action="<c:url value='/customer/task/list'/>"
				class="row g-2">
				<div class="col-auto">
					<label class="col-form-label">対象月</label>
				</div>
				<div class="col-auto">
					<input type="month" name="ym" class="form-control"
						value="${selectedYm}" />
				</div>
				<div class="col-auto">
					<button class="btn btn-outline-primary">表示</button>
				</div>
			</form>
		</div>
		<div class="card shadow-sm">
			<div class="card-body p-0">
				<div class="table-responsive">
					<fmt:setLocale value="ja_JP" />
					<table class="table table-sm table-striped align-middle mb-0">
						<thead class="table-light">
							<tr>
								<th style="width: 4rem;">#</th>
								<th>秘書名</th>
								<th style="width: 8rem;">日付</th>
								<th style="width: 12rem;">時間</th>
								<th style="width: 6rem;">稼働</th>
								<th style="width: 4rem;">ランク</th>
								<th>内容</th>
								<th style="width: 8rem;">単価</th>
								<th style="width: 8rem;">コスト</th>
								<th style="width: 8rem;">操作</th>
							</tr>
						</thead>
						<tbody>
							<c:forEach var="t" items="${tasks}" varStatus="st">
								<tr>
									<!-- #（1..n） -->
									<td class="text-muted"><c:out value="${st.index + 1}" /></td>

									<!-- 秘書名 -->
									<td><c:out value="${t.assignment.secretaryName}" /></td>

									<!-- 日付：例「01(月)」 -->
									<td><c:choose>
											<c:when test="${not empty t.workDate}">
												<fmt:formatDate value="${t.workDate}" pattern="dd(E)" />
											</c:when>
											<c:otherwise>—</c:otherwise>
										</c:choose></td>

									<!-- 時間：HH:mm ～ HH:mm -->
									<td><c:choose>
											<c:when
												test="${not empty t.startTime and not empty t.endTime}">
												<fmt:formatDate value="${t.startTime}" pattern="HH:mm" />
      ～
      <fmt:formatDate value="${t.endTime}" pattern="HH:mm" />
											</c:when>
											<c:otherwise>—</c:otherwise>
										</c:choose></td>

									<!-- 稼働：n分 -->
									<td><c:choose>
											<c:when test="${not empty t.workMinute}">
												<c:out value="${t.workMinute}" />分
    </c:when>
											<c:otherwise>—</c:otherwise>
										</c:choose></td>

									<!-- ランク（例：C） -->
									<td><c:out
											value="${empty t.assignment.taskRankName ? '—' : t.assignment.taskRankName}" /></td>

									<!-- 内容 -->
									<td class="text-truncate" style="max-width: 32rem;"><c:out
											value="${t.workContent}" /></td>

									<!-- 単価：#,### 円 -->
									<td><c:choose>
											<c:when test="${not empty t.hourFeeCustomer}">
												<fmt:formatNumber value="${t.hourFeeCustomer}"
													groupingUsed="true" /> 円
    </c:when>
											<c:otherwise>—</c:otherwise>
										</c:choose></td>


									<!-- コスト：#,### 円 -->
									<td><c:choose>
											<c:when test="${not empty t.feeCustomer}">
												<fmt:formatNumber value="${t.feeCustomer}"
													groupingUsed="true" /> 円
    </c:when>
											<c:otherwise>—</c:otherwise>
										</c:choose></td>
									<td>
									<c:choose>
									<c:when test="${empty t.alertedAt}">
										<!-- 顧客の確認申請フォーム（1行＝1フォーム） -->
										<form method="post"
											action="<c:url value='/customer/task/alert'/>"
											class="d-inline alert-form">
											<!-- 必須：task UUID -->
											<input type="hidden" name="id" value="${t.id}" />
											<!-- 入力ダイアログでセットするコメント -->
											<input type="hidden" name="comment" value="" />

											<!-- ① 初期表示：確認ボタン（クリックで入力ダイアログ） -->
											<button type="button"
												class="btn btn-sm btn-outline-primary btn-alert-confirm">
												確認</button>

											<!-- ② コメント入力後に表示：申請ボタン（POST 実行） -->
											<button type="submit"
												class="btn btn-sm btn-primary btn-alert-submit d-none">
												申請</button>

											<!-- ③ 参考表示：入力済みコメントのプレビュー（任意） -->
											<small class="text-muted ms-1 alert-comment-preview d-none"></small>
										</form>
										</c:when>
											<c:when
												test="${not empty t.alertedAt and empty t.remandedById}">
												<span class="badge text-bg-secondary bg-opacity-75">申請中</span><br>${t.alertComment }
											</c:when>
											<c:when
												test="${not empty t.alertedAt and not empty t.remandedById and not empty t.remandedAt}">
												<span class="badge text-bg-danger bg-opacity-75">差戻中</span><br>${t.alertComment }
											</c:when>
											<c:when
												test="${not empty t.remandedById and empty t.remandedAt}">
												<span class="badge text-bg-success bg-opacity-75">対応完了</span><br>${t.alertComment }
											</c:when>
										</c:choose>
									</td>
								</tr>
							</c:forEach>

							<c:if test="${empty tasks}">
								<tr>
									<td colspan="10" class="text-center text-muted py-4">表示できるタスクがありません。</td>
								</tr>
							</c:if>
						</tbody>
					</table>
				</div>
			</div>
		</div>

	</div>

	<script
		src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
	<script>
  (function() {
    const MIN_COMMENT_LEN = 10; // 最低文字数

    // サロゲートペア等も1文字として数える（Unicode コードポイント数）
    function countChars(s) {
      return Array.from(s).length;
    }

    document.querySelectorAll('.btn-alert-confirm').forEach(function(btn) {
      btn.addEventListener('click', function () {
        const form      = btn.closest('form.alert-form');
        const hidden    = form.querySelector('input[name="comment"]');
        const submitBtn = form.querySelector('.btn-alert-submit');
        const preview   = form.querySelector('.alert-comment-preview');

        let seed = hidden.value || '';
        while (true) {
          // ★ ここをテンプレートリテラル → 文字連結に変更
          const msg = '確認コメントを入力してください（' + MIN_COMMENT_LEN + '文字以上・必須）';
          const input = window.prompt(msg, seed);
          if (input === null) return; // キャンセル

          const trimmed = input.trim();
          if (countChars(trimmed) >= MIN_COMMENT_LEN) {
            hidden.value = trimmed;
            btn.classList.add('d-none');
            submitBtn.classList.remove('d-none');
            preview.textContent = '「' + trimmed + '」';
            preview.classList.remove('d-none');
            return;
          }
          // ★ ここも同様に変更
          alert('コメントは' + MIN_COMMENT_LEN + '文字以上で入力してください。');
          seed = trimmed;
        }
      });
    });
  })();
</script>
</body>
</html>
