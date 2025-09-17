<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>業務編集</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/secretary/navbar.jspf" %>
<div class="container py-4">

  <!-- ヘッダ -->
  <div class="d-flex justify-content-between align-items-center mb-3">
    <div>
      <h1 class="h4 mb-1">業務編集</h1>
      <div class="text-muted small">
        会社名：<strong>${fn:escapeXml(companyName)}</strong>
        ／ 年月：<strong>${yearMonth}</strong>
      </div>
    </div>

    <div class="d-flex gap-2">
      <!-- 一覧へ戻る -->
      <form method="get" action="<%=request.getContextPath()%>/secretary/task/list" class="m-0">
        <input type="hidden" name="companyId" value="${companyId}">
        <input type="hidden" name="companyName" value="${fn:escapeXml(companyName)}">
        <input type="hidden" name="yearMonth" value="${yearMonth}">
        <button type="submit" class="btn btn-sm btn-outline-secondary">一覧へ戻る</button>
      </form>
    </div>
  </div>

  <!-- エラー表示（任意） -->
  <c:if test="${not empty errorMsg}">
    <div class="alert alert-danger">
      <ul class="mb-0">
        <c:forEach var="msg" items="${errorMsg}">
          <li><c:out value="${msg}"/></li>
        </c:forEach>
      </ul>
    </div>
  </c:if>

  <!-- 既存値のプレフィル用（task が無い場合は空で表示） -->
  <c:set var="d_workDate">
    <c:choose>
      <c:when test="${not empty task and task.workDate ne null}">
        <fmt:formatDate value="${task.workDate}" pattern="yyyy-MM-dd" timeZone="Asia/Tokyo"/>
      </c:when>
      <c:otherwise></c:otherwise>
    </c:choose>
  </c:set>

  <c:set var="d_startTime">
    <c:choose>
      <c:when test="${not empty task and task.startTime ne null}">
        <fmt:formatDate value="${task.startTime}" pattern="HH:mm" timeZone="Asia/Tokyo"/>
      </c:when>
      <c:otherwise></c:otherwise>
    </c:choose>
  </c:set>

  <c:set var="d_endTime">
    <c:choose>
      <c:when test="${not empty task and task.endTime ne null}">
        <fmt:formatDate value="${task.endTime}" pattern="HH:mm" timeZone="Asia/Tokyo"/>
      </c:when>
      <c:otherwise></c:otherwise>
    </c:choose>
  </c:set>

  <c:set var="d_content" value="${not empty task ? fn:escapeXml(task.workContent) : ''}"/>
  <c:set var="d_assignmentId" value="${not empty task and task.assignment ne null ? task.assignment.id : ''}"/>

  <!-- yearMonth から当月の最小/最大日付を生成（登録画面と同様の制約を編集にも適用） -->
  <fmt:parseDate value="${yearMonth}-01" pattern="yyyy-MM-dd" var="ymFirstDate" />
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

  <!-- 編集フォーム -->
  <div class="card shadow-sm">
    <div class="card-header bg-light">
      <span class="fw-semibold">業務内容を編集</span>
    </div>

    <div class="card-body">
      <form id="taskEditForm" method="post" action="<%=request.getContextPath()%>/secretary/task/edit_done">
        <!-- 必須 hidden -->
        <input type="hidden" name="id" value="${id}">
        <input type="hidden" name="companyId" value="${companyId}">
        <input type="hidden" name="companyName" value="${fn:escapeXml(companyName)}">
        <input type="hidden" name="yearMonth" value="${yearMonth}">
        <input type="hidden" name="clearRemand" value="${clearRemand ? '1' : '0'}">
        <c:if test="${clearRemand}">
				  <div class="alert alert-warning py-2">
				    このタスクは <strong>差戻解除</strong> として保存されます
				  </div>
				  <div class="alert alert-warning py-2">
				    差戻コメント：${remandComment}
				  </div>
				</c:if>

        <div class="row g-3">
          <div class="col-md-3">
            <label class="form-label form-label-sm">日付</label>
            <input type="date" name="workDate" id="workDate"
                   class="form-control form-control-sm"
                   value="${d_workDate}" min="${minDate}" max="${maxDate}" required>
          </div>

          <div class="col-md-4">
            <label class="form-label form-label-sm">時間</label>
            <div class="d-flex">
              <input type="time" name="startTime" id="startTime" class="form-control form-control-sm" value="${d_startTime}" required>
              <span class="mx-1 align-self-center">～</span>
              <input type="time" name="endTime" id="endTime" class="form-control form-control-sm" value="${d_endTime}" required>
            </div>
            <div class="form-text">
              稼働：<span id="workMinuteText" class="fw-semibold">—</span> 分（保存時はサーバ側でも再計算）
            </div>
          </div>

          <div class="col-md-3">
            <label class="form-label form-label-sm">ランク（アサイン）</label>
            <c:choose>
              <c:when test="${not empty assignments}">
                <select name="assignmentId" class="form-select form-select-sm" required>
                  <option value="" disabled ${empty d_assignmentId ? 'selected' : ''}>選択してください</option>
                  <c:forEach var="a" items="${assignments}">
                    <option value="${a.id}" ${a.id == d_assignmentId ? 'selected' : ''}>
                      ${fn:escapeXml(a.taskRankName)}
                    </option>
                  </c:forEach>
                </select>
              </c:when>
              <c:otherwise>
                <div class="text-muted small">アサインがありません。先に作成してください。</div>
              </c:otherwise>
            </c:choose>
          </div>

          <div class="col-md-12">
            <label class="form-label form-label-sm">内容</label>
            <input type="text" name="workContent" class="form-control form-control-sm"
                   value="${d_content}" placeholder="内容" required>
          </div>
        </div>

        <div class="mt-4 d-flex gap-2">
				  <button type="submit" id="submitBtn" class="btn btn-primary btn-sm">更新する</button>
				  <!-- ここは a に変更（フォームの入れ子をやめる） -->
				  <a class="btn btn-outline-secondary btn-sm"
				     href="<%=request.getContextPath()%>/secretary/task/list?companyId=${companyId}&companyName=${fn:escapeXml(companyName)}&yearMonth=${yearMonth}">
				     キャンセル
				  </a>
				</div>
      </form>
    </div>
  </div>

</div>

<script>
(function() {
  const start  = document.getElementById('startTime');
  const end    = document.getElementById('endTime');
  const text   = document.getElementById('workMinuteText');
  const dateEl = document.getElementById('workDate');
  const form   = document.getElementById('taskEditForm');

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
      return;
    }
    const diff = e - s;
    if (diff <= 0) {
      text.textContent = '—';
      return;
    }
    text.textContent = diff.toString();
  }

  start.addEventListener('change', recalc);
  end.addEventListener('change', recalc);
  recalc();

  // 送信前ガード
  form.addEventListener('submit', function(ev) {
    // 当月範囲外ならブロック
    if (dateEl.value && (dateEl.value < dateEl.min || dateEl.value > dateEl.max)) {
      ev.preventDefault();
      alert('日付は当月のみ選択できます。');
    }
    const s = parseMinutes(start.value);
    const e = parseMinutes(end.value);
    if (s == null || e == null || e - s <= 0) {
      ev.preventDefault();
      alert('開始/終了時刻を正しく入力してください。');
    }
  });
})();
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
