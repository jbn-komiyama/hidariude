<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>業務登録</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
<div class="container py-4">

  <div class="d-flex justify-content-between align-items-center mb-3">
    <div>
      <h1 class="h4 mb-1">業務登録</h1>
      <div class="text-muted small">
        年月：
        <strong>${yearMonth}</strong>
      </div>
    </div>
    <a href="<%=request.getContextPath()%>/secretary/home" class="btn btn-sm btn-outline-secondary">ホームへ</a>
  </div>

  <!-- ===== 当月のみ選択可能 / 稼働は自動計算 ===== -->
  <jsp:useBean id="now" class="java.util.Date" />
  <fmt:formatDate value="${now}" pattern="yyyy-MM-dd" timeZone="Asia/Tokyo" var="todayStr" />

  <!-- yearMonth（未指定なら today から当月） -->
  <c:set var="thisYM" value="${empty yearMonth ? fn:substring(todayStr,0,7) : yearMonth}" />

  <!-- thisYM から当月の最小/最大日付を生成 -->
  <fmt:parseDate value="${thisYM}-01" pattern="yyyy-MM-dd" var="ymFirstDate" />
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

  <!-- ===== フォーム本体 ===== -->
  <div class="card shadow-sm">
    <div class="card-header bg-light">
      <span class="fw-semibold">新規業務登録（${thisYM}）</span>
    </div>
    <div class="card-body">

      <!-- assignmentsAll が空ならガイダンス -->
      <c:if test="${empty assignmentsAll}">
        <div class="alert alert-warning mb-0">
          今月のアサインが見つかりません。先に「顧客へのアサイン（ランク設定）」を作成してください。
        </div>
      </c:if>

      <form id="taskRegisterForm" method="post"
            action="<%=request.getContextPath()%>/secretary/task/register_done" class="mt-2">

        <!-- サーバへ渡す hidden -->
        <input type="hidden" name="companyId"    id="companyId">
        <input type="hidden" name="companyName"  id="companyName">
        <input type="hidden" name="yearMonth"    value="${thisYM}">
        <input type="hidden" name="workMinute"   id="workMinute"><!-- 自動計算した稼働分 -->

        <table class="table table-bordered table-sm bg-white align-middle mb-2">
          <thead class="table-light">
            <tr>
              <th style="width:220px;">顧客</th>
              <th style="width:150px;">ランク</th>
              <th style="width:130px;">日付</th>
              <th style="width:200px;">時間</th>
              <th style="width:90px;">稼働</th>
              <th>内容</th>
              <th style="width:100px;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <!-- ① アサイン済み顧客 -->
              <td>
                <select id="customerSelect" class="form-select form-select-sm" required
                        <c:if test="${empty assignmentsAll}">disabled</c:if>>
                  <option value="" disabled selected>顧客を選択</option>
                  <!-- JSで assignmentsAll からユニーク化して投入 -->
                </select>
              </td>

              <!-- ② 顧客ごとのランク名（=Assignment選択） -->
              <td>
                <select id="rankSelect" name="assignmentId" class="form-select form-select-sm" required disabled>
                  <option value="" disabled selected>ランクを選択</option>
                </select>
              </td>

              <!-- 日付（当月のみ） -->
              <td>
                <input type="date" name="workDate" id="workDate"
                       class="form-control form-control-sm"
                       value="${todayStr}" min="${minDate}" max="${maxDate}" required>
              </td>

              <!-- 時間 -->
              <td>
                <div class="d-flex">
                  <input type="time" name="startTime" id="startTime" class="form-control form-control-sm" required>
                  <span class="mx-1">～</span>
                  <input type="time" name="endTime" id="endTime" class="form-control form-control-sm" required>
                </div>
              </td>

              <!-- 稼働（自動計算の表示のみ） -->
              <td>
                <span id="workMinuteText" class="fw-semibold">—</span> 分
              </td>

              <!-- 内容 -->
              <td>
                <input type="text" name="workContent" class="form-control form-control-sm"
                       placeholder="内容" required>
              </td>

              <!-- 送信 -->
              <td>
                <button type="submit" id="submitBtn" class="btn btn-sm btn-success"
                        <c:if test="${empty assignmentsAll}">disabled</c:if>>
                  登録
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </form>
    </div>
  </div>
</div>

<!-- ===== assignmentsAll を JS 配列化 ===== -->
<script>
  // JSP から安全に文字列を埋め込むため、属性は data-* で逃がす方法もありますが、
  // 今回は配列を明示生成します（必要フィールドのみ）。
  const ASSIGNMENTS = [
    <c:forEach var="a" items="${assignmentsAll}" varStatus="st">
      {
        id:         "${a.id}",
        customerId: "${a.customerId}",
        companyName: "${fn:escapeXml(a.companyName)}",
        rankName:   "${fn:escapeXml(a.taskRankName)}"
      }<c:if test="${!st.last}">,</c:if>
    </c:forEach>
  ];
</script>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
(() => {
  const byId      = (id) => document.getElementById(id);
  const $cust     = byId('customerSelect');
  const $rank     = byId('rankSelect');
  const $companyId= byId('companyId');
  const $companyNm= byId('companyName');
  const $workDate = byId('workDate');
  const $st       = byId('startTime');
  const $et       = byId('endTime');
  const $wTxt     = byId('workMinuteText');
  const $wMin     = byId('workMinute');
  const $btn      = byId('submitBtn');
  const form      = byId('taskRegisterForm');

  // 当月範囲内に日付を補正（todayが別月ならminへ）
  function clampDateToRange() {
    const v = $workDate.value, min = $workDate.min, max = $workDate.max;
    if (v && min && v < min) $workDate.value = min;
    if (v && max && v > max) $workDate.value = max;
  }
  clampDateToRange();

  // 1) 顧客プルダウンをユニーク化して構築
  function buildCustomerOptions() {
    const map = new Map(); // customerId -> companyName
    ASSIGNMENTS.forEach(a => {
      if (a.customerId && !map.has(a.customerId)) map.set(a.customerId, a.companyName || '(名称未設定)');
    });
    // 会社名でソート
    const arr = Array.from(map.entries()).sort((x,y) => x[1].localeCompare(y[1], 'ja'));
    for (const [cid, name] of arr) {
      const opt = new Option(name, cid);
      opt.dataset.companyName = name;
      $cust.add(opt);
    }
    // データがあれば有効化
    if (arr.length > 0) $cust.disabled = false;
  }

  // 2) 顧客選択時：ランク(=Assignment)をフィルタして構築
  function rebuildRankOptions(customerId) {
  $rank.length = 0;
  const ph = new Option('ランクを選択', '');
  ph.selected = true;
  ph.disabled = true;
  $rank.add(ph);

  if (!customerId) { $rank.disabled = true; return; }

  const key = String(customerId).trim().toLowerCase();
  const list = ASSIGNMENTS.filter(a =>
    String(a.customerId || '').trim().toLowerCase() === key
  );

  list.sort((a,b) => (a.rankName||'').localeCompare(b.rankName||'', 'ja'));
  list.forEach(a => {
    const opt = new Option(a.rankName || '—', a.id || '');
    opt.dataset.customerId = a.customerId || '';
    $rank.add(opt);
  });

  $rank.disabled = (list.length === 0);
  const compName = $cust.options[$cust.selectedIndex]?.dataset?.companyName || '';
  $companyId.value = customerId || '';
  $companyName.value = compName || '';

  if (list.length === 1) $rank.selectedIndex = 1;
  toggleSubmit();
}

  // 3) 稼働分の自動計算
  function parseMinutes(hhmm) {
    if (!hhmm || !/^\d{2}:\d{2}$/.test(hhmm)) return null;
    const [h,m] = hhmm.split(':').map(Number);
    return h*60 + m;
  }
  function recalcMinutes() {
    const s = parseMinutes($st.value);
    const e = parseMinutes($et.value);
    const ok = (s!=null && e!=null && e>s);
    $wTxt.textContent = ok ? String(e-s) : '—';
    $wMin.value = ok ? String(e-s) : '';
    toggleSubmit();
  }

  // 4) 送信可否
  function toggleSubmit() {
    const hasCust  = !!$companyId.value;
    const hasRank  = !!$rank.value;
    const hasMin   = !!$wMin.value;
    const inRange  = $workDate.value && (!($workDate.min && $workDate.value < $workDate.min)) &&
                                    (!($workDate.max && $workDate.value > $workDate.max));
    $btn.disabled = !(hasCust && hasRank && hasMin && inRange);
  }

  // 初期構築
  buildCustomerOptions();
  recalcMinutes();
  toggleSubmit();

  // イベント
  $cust.addEventListener('change', () => rebuildRankOptions($cust.value));
  $rank.addEventListener('change',  toggleSubmit);
  $st.addEventListener('change',    recalcMinutes);
  $et.addEventListener('change',    recalcMinutes);
  $workDate.addEventListener('change', () => { clampDateToRange(); toggleSubmit(); });

  // 送信前最終チェック
  form.addEventListener('submit', (ev) => {
    if ($btn.disabled) {
      ev.preventDefault();
      alert('入力に不足があります。顧客・ランク・日付・時間をご確認ください。');
    }
  });

  // （任意）URLクエリで companyId が渡っていたら初期選択
  try {
    const params = new URLSearchParams(location.search);
    const cid = params.get('companyId');
    if (cid && Array.from($cust.options).some(o => o.value === cid)) {
      $cust.value = cid;
      rebuildRankOptions(cid);
    }
  } catch {}
})();
</script>
</body>
</html>
