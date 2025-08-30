<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>${selectedYm} アサイン一覧</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <div class="container py-4">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h1 class="h3 mb-0">アサイン登録</h1>
    </div>

    <div class="card shadow-sm">
      <div class="card-body">
        <form method="post" action="<%= request.getContextPath() %>/admin/assignment/pm_register_done">
          <div class="row g-3">

            <!-- 顧客 -->
            <div class="col-md-6">
              <label class="form-label">顧客</label>
              <p class="form-control-plaintext mb-0" id="customerName">${customer.companyName}</p>
			  <input type="hidden" name="customerId" id="customerId" value="${customer.id}">
            </div>

            <!-- 秘書（ランクの増額を data-* で埋め込む） -->
            <div class="col-md-6">
              <label class="form-label">PM秘書</label>
              <select name="secretaryId" id="secretaryId" class="form-select" required>
                <option value="">選択してください</option>
                <c:forEach var="s" items="${secretaries}">
                  <option
                    value="${s.id}"
                    data-inc-cust="${s.secretaryRank != null && s.secretaryRank.increaseBasePayCustomer != null ? s.secretaryRank.increaseBasePayCustomer : 0}"
                    data-inc-sec="${s.secretaryRank != null && s.secretaryRank.increaseBasePaySecretary != null ? s.secretaryRank.increaseBasePaySecretary : 0}">
                    <c:out value="${s.name}"/>
                    <c:if test="${s.secretaryRank != null}">（<c:out value="${s.secretaryRank.rankName}"/>）</c:if>
                  </option>
                </c:forEach>
              </select>
            </div>

            <!-- タスクランク（単価を data-* で埋め込む） -->
            <div class="col-md-6">
              <label class="form-label">業務ランク</label>
              <p class="form-control-plaintext mb-0" id="taskRank">${taskRank.rankName}</p>
			  <input type="hidden" name="taskRankId" id="taskRankId" value="${taskRank.id}">
            </div>

            <!-- 対象月 -->
            <div class="col-md-6">
              <label class="form-label">対象月</label>
              <input type="month" name="targetYearMonth" id="targetYearMonth" class="form-control" required>
            </div>

            <hr class="mt-2 mb-1">

            <!-- 単価（自動反映） -->
            <div class="col-md-3">
			  <label class="form-label">単価（顧客）</label>
			  <p class="form-control-plaintext mb-0" id="basePayCustomerDisplay"><fmt:formatNumber 
			    value="${taskRank.basePayCustomer}" 
			    type="number" 
			    minFractionDigits="0" 
			    maxFractionDigits="0"/></p>
			  <input step="1" type="hidden" name="basePayCustomer" id="basePayCustomer" value="${taskRank.basePayCustomer}">
			</div>
			<div class="col-md-3">
			  <label class="form-label">単価（秘書）</label>
			  <p class="form-control-plaintext mb-0" id="basePaySecretaryDisplay"><fmt:formatNumber 
			    value="${taskRank.basePaySecretary}" 
			    type="number" 
			    minFractionDigits="0" 
			    maxFractionDigits="0"/></p>
			  <input step="1" type="hidden" name="basePaySecretary" id="basePaySecretary" value="${taskRank.basePaySecretary}">
			</div>
			
            <div class="col-md-6">
              <label class="form-label">ステータス（任意）</label>
              <select name="status" class="form-select">
                <option value="">未選択</option>
                <option value="draft">下書き</option>
                <option value="active">有効</option>
                <option value="paused">一時停止</option>
              </select>
            </div>

            <!-- 送信 -->
            <div class="col-12 text-end">
              <button type="submit" class="btn btn-primary">登録する</button>
            </div>
          </div>
        </form>
      </div>
    </div>
  </div>

<script>
(function () {
  // --- 要素取得 ---
  const ym               = document.getElementById('targetYearMonth');

  const taskRankSel      = document.getElementById('taskRankId');
  const secretarySel     = document.getElementById('secretaryId');

  // --- ユーティリティ ---
  function isBlank(v) {
    return v === null || v === undefined || v === '';
  }
  function toIntOrNull(v) {
    if (isBlank(v)) return null;
    const n = Math.trunc(Number(v));
    return Number.isFinite(n) ? n : null;
  }


  // --- 対象月の初期値（空なら今日の年月） ---
  (function initYearMonth() {
    if (!ym || ym.value) return;
    const d = new Date();
    const m = ('0' + (d.getMonth() + 1)).slice(-2);
    ym.value = d.getFullYear() + '-' + m;   // YYYY-MM
  })();

})();
</script>

</body>
</html>