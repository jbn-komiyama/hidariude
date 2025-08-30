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
        <form method="post" action="<%= request.getContextPath() %>/admin/assignment/register">
          <div class="row g-3">

            <!-- 顧客 -->
            <div class="col-md-6">
              <label class="form-label">顧客</label>
              <select name="customerId" id="customerId" class="form-select" required>
                <option value="">選択してください</option>
                <c:forEach var="c" items="${customers}">
                  <option value="${c.id}">
                    <c:out value="${c.companyCode}"/> - <c:out value="${c.companyName}"/>
                  </option>
                </c:forEach>
              </select>
            </div>

            <!-- 秘書（ランクの増額を data-* で埋め込む） -->
            <div class="col-md-6">
              <label class="form-label">秘書</label>
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
              <select name="taskRankId" id="taskRankId" class="form-select" required>
                <option value="">選択してください</option>
                <c:forEach var="tr" items="${taskRanks}">
                  <option
                    value="${tr.id}"
                    data-base-cust="${tr.basePayCustomer != null ? tr.basePayCustomer : 0}"
                    data-base-sec="${tr.basePaySecretary != null ? tr.basePaySecretary : 0}">
                    <c:out value="${tr.rankName}"/>
                  </option>
                </c:forEach>
              </select>
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
			  <p class="form-control-plaintext mb-0" id="basePayCustomerDisplay">—</p>
			  <input type="hidden" name="basePayCustomer" id="basePayCustomer">
			</div>
			<div class="col-md-3">
			  <label class="form-label">単価（秘書）</label>
			  <p class="form-control-plaintext mb-0" id="basePaySecretaryDisplay">—</p>
			  <input type="hidden" name="basePaySecretary" id="basePaySecretary">
			</div>
			
			<div class="col-md-3">
			  <label class="form-label">増額（顧客）</label>
			  <p class="form-control-plaintext mb-0" id="increaseBasePayCustomerDisplay">—</p>
			  <input type="hidden" name="increaseBasePayCustomer" id="increaseBasePayCustomer">
			</div>
			<div class="col-md-3">
			  <label class="form-label">増額（秘書）</label>
			  <p class="form-control-plaintext mb-0" id="increaseBasePaySecretaryDisplay">—</p>
			  <input type="hidden" name="increaseBasePaySecretary" id="increaseBasePaySecretary">
			</div>

            <!-- 継続単価（ユーザ入力＝顧客/秘書インセンティブ） -->
            <div class="col-md-6">
              <label class="form-label">継続単価（顧客）</label>
              <input type="number" step="0.01" min="0" class="form-control" name="customerBasedIncentiveForCustomer" id="customerBasedIncentiveForCustomer" placeholder="例: 500">
            </div>
            <div class="col-md-6">
              <label class="form-label">継続単価（秘書）</label>
              <input type="number" step="0.01" min="0" class="form-control" name="customerBasedIncentiveForSecretary" id="customerBasedIncentiveForSecretary" placeholder="例: 200">
            </div>

            <!-- ステータス（任意） -->
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

  // hidden inputs（サーバ送信用）
  const baseCust         = document.getElementById('basePayCustomer');
  const baseSec          = document.getElementById('basePaySecretary');
  const incCust          = document.getElementById('increaseBasePayCustomer');
  const incSec           = document.getElementById('increaseBasePaySecretary');

  // 表示用テキスト
  const baseCustDisp     = document.getElementById('basePayCustomerDisplay');
  const baseSecDisp      = document.getElementById('basePaySecretaryDisplay');
  const incCustDisp      = document.getElementById('increaseBasePayCustomerDisplay');
  const incSecDisp       = document.getElementById('increaseBasePaySecretaryDisplay');

  // --- ユーティリティ ---
  function isBlank(v) {
    return v === null || v === undefined || v === '';
  }
  function toIntOrNull(v) {
    if (isBlank(v)) return null;
    const n = Math.trunc(Number(v));
    return Number.isFinite(n) ? n : null;
  }

  /**
   * 選択状態＆値に応じて hidden と表示を更新する
   * - 未選択: hidden=""（送らない/サーバ側で null）、表示は "—"
   * - 選択済み:
   *    - 数値0: hidden="0" を送信、表示は "0"
   *    - 数値>0: その値、表示はカンマ区切り
   */
  function setFromOption(opt, attrName, hiddenEl, displayEl) {
    if (!opt || isBlank(opt.value)) {
      hiddenEl.value = '';                 // 未選択は空で送信（＝サーバでnull扱い）
      displayEl.textContent = '—';
      return;
    }
    const raw = opt.getAttribute(attrName);
    const n = toIntOrNull(raw);
    if (n === null) {
      hiddenEl.value = '';
      displayEl.textContent = '—';
    } else {
      hiddenEl.value = String(n);          // 0 も "0" として送る
      displayEl.textContent = n === 0 ? '0' : n.toLocaleString();
    }
  }

  // --- 反映ロジック ---
  function updatePricesFromTaskRank() {
    const opt = taskRankSel ? taskRankSel.options[taskRankSel.selectedIndex] : null;
    setFromOption(opt, 'data-base-cust', baseCust, baseCustDisp);
    setFromOption(opt, 'data-base-sec',  baseSec,  baseSecDisp);
  }

  function updateIncrementsFromSecretary() {
    const opt = secretarySel ? secretarySel.options[secretarySel.selectedIndex] : null;
    setFromOption(opt, 'data-inc-cust', incCust, incCustDisp);
    setFromOption(opt, 'data-inc-sec',  incSec,  incSecDisp);
  }

  // --- 対象月の初期値（空なら今日の年月） ---
  (function initYearMonth() {
    if (!ym || ym.value) return;
    const d = new Date();
    const m = ('0' + (d.getMonth() + 1)).slice(-2);
    ym.value = d.getFullYear() + '-' + m;   // YYYY-MM
  })();

  // --- イベント登録 ---
  taskRankSel && taskRankSel.addEventListener('change', updatePricesFromTaskRank);
  secretarySel && secretarySel.addEventListener('change', updateIncrementsFromSecretary);

  // --- 初期反映 ---
  updatePricesFromTaskRank();
  updateIncrementsFromSecretary();
})();
</script>

</body>
</html>