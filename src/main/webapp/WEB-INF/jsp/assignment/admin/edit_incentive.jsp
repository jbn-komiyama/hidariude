<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>継続単価の変更</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf" %>

<div class="container py-4">
  <h1 class="h4 mb-3">継続単価の変更</h1>

  <c:if test="${not empty errorMsg}">
    <div class="alert alert-danger">
      <ul class="mb-0">
        <c:forEach var="msg" items="${errorMsg}">
          <li><c:out value="${msg}" /></li>
        </c:forEach>
      </ul>
    </div>
  </c:if>

  <div class="alert alert-info py-2">
    この変更は <strong>同じ顧客×秘書×年月</strong> の全アサインに適用されます。<br/>
    また、<strong>翌月に同一の顧客×秘書で登録がある場合</strong>は、翌月分にも同じ継続単価を反映します。
  </div>

  <div class="card shadow-sm">
    <div class="card-body">
      <dl class="row">
        <dt class="col-sm-3">顧客</dt><dd class="col-sm-9"><c:out value="${row.customerCompanyName}" /></dd>
        <dt class="col-sm-3">秘書</dt><dd class="col-sm-9"><c:out value="${row.secretaryName}" /></dd>
        <dt class="col-sm-3">年月</dt><dd class="col-sm-9"><c:out value="${row.targetYearMonth}" /></dd>
        <dt class="col-sm-3">ランク</dt><dd class="col-sm-9"><c:out value="${row.taskRankName}" /></dd>
      </dl>

      <!-- 追加: 現在の数値内訳 -->
      <div class="table-responsive mb-3">
        <table class="table table-sm table-bordered align-middle">
          <thead class="table-light">
            <tr>
              <th style="width: 24%"></th>
              <th class="text-end" style="width: 38%">顧客サイド</th>
              <th class="text-end" style="width: 38%">秘書サイド</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <th>基本単価</th>
              <td class="text-end">
                <fmt:formatNumber value="${row.basePayCustomer}" pattern="#,##0"/>
              </td>
              <td class="text-end">
                <fmt:formatNumber value="${row.basePaySecretary}" pattern="#,##0"/>
              </td>
            </tr>
            <tr>
              <th>増額</th>
              <td class="text-end">
                <fmt:formatNumber value="${row.increaseBasePayCustomer}" pattern="#,##0"/>
              </td>
              <td class="text-end">
                <fmt:formatNumber value="${row.increaseBasePaySecretary}" pattern="#,##0"/>
              </td>
            </tr>
            <tr>
              <th>現行 継続単価</th>
              <td class="text-end">
                <fmt:formatNumber value="${row.customerBasedIncentiveForCustomer}" pattern="#,##0"/>
              </td>
              <td class="text-end">
                <fmt:formatNumber value="${row.customerBasedIncentiveForSecretary}" pattern="#,##0"/>
              </td>
            </tr>
            <tr class="table-secondary">
              <th>現行 合計</th>
              <td class="text-end fw-semibold">
                <fmt:formatNumber
                  value="${row.basePayCustomer + row.increaseBasePayCustomer + row.customerBasedIncentiveForCustomer}"
                  pattern="#,##0"/>
              </td>
              <td class="text-end fw-semibold">
                <fmt:formatNumber
                  value="${row.basePaySecretary + row.increaseBasePaySecretary + row.customerBasedIncentiveForSecretary}"
                  pattern="#,##0"/>
              </td>
            </tr>
            <tr class="table-primary">
              <th>変更後 合計（プレビュー）</th>
              <td class="text-end fw-bold">
                <span id="previewTotalCustomer"></span>
              </td>
              <td class="text-end fw-bold">
                <span id="previewTotalSecretary"></span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 入力 -->
      <form method="post" action="${pageContext.request.contextPath}/admin/assignment/edit_update" class="mt-3" id="editForm"
            data-base-cust="${row.basePayCustomer}" data-inc-cust="${row.increaseBasePayCustomer}"
            data-base-sec="${row.basePaySecretary}" data-inc-sec="${row.increaseBasePaySecretary}">
        <input type="hidden" name="id" value="${row.assignmentId}">
        <input type="hidden" name="targetYM" value="${targetYM}">

        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label">継続単価（顧客）</label>
            <input type="text" name="customerBasedIncentiveForCustomer" class="form-control js-money"
                   value="<fmt:formatNumber value='${row.customerBasedIncentiveForCustomer}' pattern='#,##0'/>"
                   inputmode="numeric" autocomplete="off">
          </div>
          <div class="col-md-6">
            <label class="form-label">継続単価（秘書）</label>
            <input type="text" name="customerBasedIncentiveForSecretary" class="form-control js-money"
                   value="<fmt:formatNumber value='${row.customerBasedIncentiveForSecretary}' pattern='#,##0'/>"
                   inputmode="numeric" autocomplete="off">
          </div>
        </div>

        <div class="mt-4 d-flex gap-2">
          <a class="btn btn-secondary" href="${pageContext.request.contextPath}/admin/assignment?targetYM=${targetYM}">戻る</a>
          <button type="submit" class="btn btn-primary">この条件（顧客×秘書×年月）の全アサインに適用</button>
        </div>
      </form>
    </div>
  </div>
</div>
<script>
(function(){
  // 3桁区切り→数値
  function toNumber(v){ if(v === null || v === undefined) return 0; return parseInt(String(v).replace(/,/g,''),10)||0; }
  function fmt(n){ return n.toLocaleString(); }

  const form = document.getElementById('editForm');
  const baseCust = toNumber(form.dataset.baseCust);
  const incCust  = toNumber(form.dataset.incCust);
  const baseSec  = toNumber(form.dataset.baseSec);
  const incSec   = toNumber(form.dataset.incSec);

  const inpCust = form.querySelector('input[name="customerBasedIncentiveForCustomer"]');
  const inpSec  = form.querySelector('input[name="customerBasedIncentiveForSecretary"]');

  const outCust = document.getElementById('previewTotalCustomer');
  const outSec  = document.getElementById('previewTotalSecretary');

  function update(){
    const incenCust = toNumber(inpCust.value);
    const incenSec  = toNumber(inpSec.value);
    outCust.textContent = fmt(baseCust + incCust + incenCust);
    outSec.textContent  = fmt(baseSec  + incSec  + incenSec);
  }

  // 入力フォーマット（blurでカンマ付与）
  function addComma(e){
    const raw = String(e.target.value).trim();
    if (raw === "") {                     // 空欄は空欄のまま（=未入力）
      e.target.value = "";
      return;
    }
    const n = toNumber(raw);              // "0" は 0 に
    e.target.value = fmt(n);              // 0 のときも "0" を表示
  }

  inpCust.addEventListener('input', update);
  inpSec .addEventListener('input', update);
  inpCust.addEventListener('blur', addComma);
  inpSec .addEventListener('blur', addComma);

  update(); // 初期表示
})();
</script>


<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
