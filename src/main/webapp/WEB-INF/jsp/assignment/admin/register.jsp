<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8" />
<title>アサイン登録</title>
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
	rel="stylesheet" />
</head>
<body  class="bg-primary bg-opacity-10">
	<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf"%>
	<div class="container py-4">
		<h1 class="h4 mb-3">アサイン登録</h1>

		<c:if test="${not empty errorMsg}">
			<div class="alert alert-danger">
				<ul class="mb-0">
					<c:forEach var="m" items="${errorMsg}">
						<li><c:out value="${m}" /></li>
					</c:forEach>
				</ul>
			</div>
		</c:if>

		<div class="card shadow-sm">
			<div class="card-body">
				<form method="post"
					action="${pageContext.request.contextPath}/admin/assignment/register_check">
					<div class="row g-3">

						<div class="col-md-6">
							<label class="form-label">顧客</label>
							<div class="form-control-plaintext fw-semibold">
								<c:out
									value="${not empty param.companyName ? param.companyName : customer.companyName}" />
								<input type="hidden" name="id" id="id" value="${customer.id}">
								<input type="hidden" name="companyName" id="companyName"
									value="${customer.companyName}">
							</div>
						</div>

						<div class="col-md-6">
							<label class="form-label">対象月 <span class="text-danger">*</span></label>
							<input type="month" class="form-control" name="targetYM"
								value="${not empty param.targetYearMonth ? param.targetYearMonth : (not empty targetYM ? targetYM : '')}"
								required>
							<!-- //★ -->
						</div>

						<div class="col-md-6">
							<label class="form-label">秘書 <span class="text-danger">*</span></label>
							<select class="form-select" name="secretaryId" id="secretaryId"
								required>
								<option value="">選択してください</option>
								<c:forEach var="s" items="${secretaries}">
									<c:choose>
										<c:when test="${not empty s.secretaryRank}">
											<fmt:formatNumber
												value="${s.secretaryRank.increaseBasePaySecretary}"
												maxFractionDigits="0" groupingUsed="false"
												var="rankupSecPlain" />
											<fmt:formatNumber
												value="${s.secretaryRank.increaseBasePayCustomer}"
												maxFractionDigits="0" groupingUsed="false"
												var="rankupCustPlain" />
										</c:when>
										<c:otherwise>
											<c:set var="rankupSecPlain" value="0" />
											<c:set var="rankupCustPlain" value="0" />
										</c:otherwise>
									</c:choose>
									<option value="${s.id}" data-rankup="${rankupSecPlain}"
										data-rankupcust="${rankupCustPlain}"
										<c:if test="${param.secretaryId == s.id}">selected</c:if>>
										<c:out value="${s.name}" />(
										<c:out value="${s.secretaryRank.rankName}" />)
									</option>
								</c:forEach>
							</select>
						</div>

						<div class="col-md-6">
							<label class="form-label">タスクランク <span
								class="text-danger">*</span></label> <select class="form-select"
								name="taskRankId" id="taskRankId" required>
								<option value="">選択してください</option>
								<c:forEach var="tr" items="${taskRanks}">
									<fmt:formatNumber value="${tr.basePayCustomer}"
										maxFractionDigits="0" groupingUsed="false"
										var="trBaseCustPlain" />
									<fmt:formatNumber value="${tr.basePaySecretary}"
										maxFractionDigits="0" groupingUsed="false"
										var="trBaseSecPlain" />
									<option value="${tr.id}" data-basecust="${trBaseCustPlain}"
										data-basesec="${trBaseSecPlain}" data-rankname="${tr.rankName}"
										<c:if test="${param.taskRankId == tr.id}">selected</c:if>>
										<c:out value="${tr.rankName}" />（ 顧客:
										<fmt:formatNumber value="${tr.basePayCustomer}"
											pattern="#,##0" /> / 秘書:
										<fmt:formatNumber value="${tr.basePaySecretary}"
											pattern="#,##0" />）
									</option>
								</c:forEach>
							</select>
						</div>

						<div class="col-md-3">
							<label class="form-label">基本単価（顧客）</label>
							<div class="form-control-plaintext" id="dispBaseCust">-</div>
						</div>
						<div class="col-md-3">
							<label class="form-label">基本単価（秘書）</label>
							<div class="form-control-plaintext" id="dispBaseSec">-</div>
						</div>

						<div class="col-md-3">
							<label class="form-label">ランクアップ単価（顧客）</label>
							<div class="form-control-plaintext" id="dispIncCust">0</div>
						</div>
						<div class="col-md-3">
							<label class="form-label">ランクアップ単価（秘書）</label>
							<div class="form-control-plaintext" id="dispIncSec">0</div>
						</div>

						<div class="col-md-3">
							<label class="form-label">継続単価（顧客）</label> <input type="number"
								step="1" min="0" class="form-control"
								name="customerBasedIncentiveForCustomer"
								value="${not empty param.customerBasedIncentiveForCustomer ? param.customerBasedIncentiveForCustomer : '0'}">
						</div>
						<div class="col-md-3">
							<label class="form-label">継続単価（秘書）</label> <input type="number"
								step="1" min="0" class="form-control"
								name="customerBasedIncentiveForSecretary"
								value="${not empty param.customerBasedIncentiveForSecretary ? param.customerBasedIncentiveForSecretary : '0'}">
						</div>

						<div class="col-md-6">
							<label class="form-label">ステータス（任意）</label> <select name="status"
								class="form-select">
								<option value="">未選択</option>
								<option value="draft"
									<c:if test="${param.status == 'draft'}">selected</c:if>>下書き</option>
								<!-- //★ -->
								<option value="active"
									<c:if test="${param.status == 'active'}">selected</c:if>>有効</option>
								<!-- //★ -->
								<option value="paused"
									<c:if test="${param.status == 'paused'}">selected</c:if>>一時停止</option>
								<!-- //★ -->
							</select>
						</div>

						<!-- hidden は直前POSTの値を優先 -->
						<input type="hidden" name="basePayCustomer" id="hBaseCust"
							value="${not empty param.basePayCustomer ? param.basePayCustomer : ''}">
						<!-- //★ -->
						<input type="hidden" name="basePaySecretary" id="hBaseSec"
							value="${not empty param.basePaySecretary ? param.basePaySecretary : ''}">
						<!-- //★ -->
						<input type="hidden" name="increaseBasePayCustomer" id="hIncCust"
							value="${not empty param.increaseBasePayCustomer ? param.increaseBasePayCustomer : '0'}">
						<!-- //★ -->
						<input type="hidden" name="increaseBasePaySecretary" id="hIncSec"
							value="${not empty param.increaseBasePaySecretary ? param.increaseBasePaySecretary : '0'}">
						<!-- //★ -->

						<div class="col-12 text-end">
							<button type="submit" class="btn btn-primary">確認へ</button>
							<a href="${pageContext.request.contextPath}/admin/assignment"
								class="btn btn-secondary">戻る</a>
						</div>
					</div>
				</form>
			</div>
		</div>
	</div>

	<hr class="my-4" />
	<div class="container py-4">
		<h2 class="h4 mb-3">
			<c:out value="${customer.companyName}" />
			様 既存アサイン一覧
		</h2>

		<c:choose>
			<c:when test="${empty futureAssignments}">
				<div class="text-muted">該当データはありません。</div>
			</c:when>
			<c:otherwise>
				<div class="table-responsive">
					<table class="table table-sm table-hover align-middle">
						<thead class="table-light">
							<tr>
								<th style="white-space: nowrap;">対象月</th>
								<th>秘書</th>
								<th>ランク</th>
								<th class="text-end">基本単価 顧客 - 秘書</th>
								<th class="text-end">基本単価 顧客 - 秘書</th>
								<th class="text-end">継続単価 顧客 - 秘書</th>
								<th class="text-end">合計単価 顧客 - 秘書</th>
								<th class="text-end">継続月数</th>
							</tr>
						</thead>
						<tbody>
							<c:forEach var="a" items="${futureAssignments}">
								<tr>
									<td><c:out value="${a.targetYearMonth}" /></td>
									<td><a href="${pageContext.request.contextPath}/admin/secretary/detail?id=${a.secretaryId}"><c:out value="${a.secretaryName}" /></a></td>
									<td><c:out value="${a.taskRankName}" /></td>
									<fmt:formatNumber value="${a.basePayCustomer}" type="number"
										maxFractionDigits="0" var="baseCust" />
									<fmt:formatNumber value="${a.basePaySecretary}" type="number"
										maxFractionDigits="0" var="baseSec" />
									<fmt:formatNumber value="${a.increaseBasePayCustomer}"
										type="number" maxFractionDigits="0" var="rankupCust" />
									<fmt:formatNumber value="${a.increaseBasePaySecretary}"
										type="number" maxFractionDigits="0" var="rankupSec" />
									<fmt:formatNumber
										value="${a.customerBasedIncentiveForCustomer}" type="number"
										maxFractionDigits="0" var="contCust" />
									<fmt:formatNumber
										value="${a.customerBasedIncentiveForSecretary}" type="number"
										maxFractionDigits="0" var="contSec" />
									<td class="text-end">${empty baseCust ? '-' : baseCust} -
										${empty baseSec ? '-' : baseSec}</td>
									<td class="text-end">${empty rankupCust ? '-' : rankupCust}
										- ${empty rankupSec ? '-' : rankupSec}</td>
									<td class="text-end">${empty contCust ? '-' : contCust} -
										${empty contSec ? '-' : contSec}</td>
									<td class="text-end">
									<fmt:formatNumber
										value="${a.hourlyPayCustomer }" type="number"
										maxFractionDigits="0"/> - 
									<fmt:formatNumber
										value="${a.hourlyPaySecretary }" type="number"
										maxFractionDigits="0"/></td>
									<td class="text-end">${a.consecutiveMonths }か月</td>
								</tr>
							</c:forEach>
						</tbody>
					</table>
				</div>
			</c:otherwise>
		</c:choose>
	</div>
<hr class="my-5"/>

<div class="container py-4">
  <h2 class="h4 mb-3">秘書の稼働可否（フィルタ & ソート）</h2>

  <!-- フィルタ & ソート（GETで自ページへ。必須パラメータはhiddenで維持） -->
  <form method="get" action="${pageContext.request.contextPath}/admin/assignment/register" class="card mb-3">
    <div class="card-body">
      <input type="hidden" name="id"          value="${customer.id}">
      <input type="hidden" name="companyName" value="${customer.companyName}">
      <input type="hidden" name="targetYM"
             value="${not empty param.targetYM ? param.targetYM : (not empty targetYM ? targetYM : '')}">

      <div class="row g-3 align-items-end">
        <!-- 平日 -->
        <div class="col-12 col-lg-4">
          <div class="border rounded p-3 h-100">
            <div class="fw-semibold mb-2">平日</div>
            <div class="form-check form-check-inline">
              <input class="form-check-input" type="checkbox" id="wdAm"   name="wdAm"   value="1"
                     <c:if test="${not empty param.wdAm}">checked</c:if>>
              <label class="form-check-label" for="wdAm">朝</label>
            </div>
            <div class="form-check form-check-inline">
              <input class="form-check-input" type="checkbox" id="wdDay"  name="wdDay"  value="1"
                     <c:if test="${not empty param.wdDay}">checked</c:if>>
              <label class="form-check-label" for="wdDay">昼</label>
            </div>
            <div class="form-check form-check-inline">
              <input class="form-check-input" type="checkbox" id="wdNight" name="wdNight" value="1"
                     <c:if test="${not empty param.wdNight}">checked</c:if>>
              <label class="form-check-label" for="wdNight">夜</label>
            </div>
          </div>
        </div>

        <!-- 土曜 -->
        <div class="col-12 col-lg-4">
          <div class="border rounded p-3 h-100">
            <div class="fw-semibold mb-2">土曜</div>
            <div class="form-check form-check-inline">
              <input class="form-check-input" type="checkbox" id="saAm"   name="saAm"   value="1"
                     <c:if test="${not empty param.saAm}">checked</c:if>>
              <label class="form-check-label" for="saAm">朝</label>
            </div>
            <div class="form-check form-check-inline">
              <input class="form-check-input" type="checkbox" id="saDay"  name="saDay"  value="1"
                     <c:if test="${not empty param.saDay}">checked</c:if>>
              <label class="form-check-label" for="saDay">昼</label>
            </div>
            <div class="form-check form-check-inline">
              <input class="form-check-input" type="checkbox" id="saNight" name="saNight" value="1"
                     <c:if test="${not empty param.saNight}">checked</c:if>>
              <label class="form-check-label" for="saNight">夜</label>
            </div>
          </div>
        </div>

        <!-- 日曜 -->
        <div class="col-12 col-lg-4">
          <div class="border rounded p-3 h-100">
            <div class="fw-semibold mb-2">日曜</div>
            <div class="form-check form-check-inline">
              <input class="form-check-input" type="checkbox" id="suAm"   name="suAm"   value="1"
                     <c:if test="${not empty param.suAm}">checked</c:if>>
              <label class="form-check-label" for="suAm">朝</label>
            </div>
            <div class="form-check form-check-inline">
              <input class="form-check-input" type="checkbox" id="suDay"  name="suDay"  value="1"
                     <c:if test="${not empty param.suDay}">checked</c:if>>
              <label class="form-check-label" for="suDay">昼</label>
            </div>
            <div class="form-check form-check-inline">
              <input class="form-check-input" type="checkbox" id="suNight" name="suNight" value="1"
                     <c:if test="${not empty param.suNight}">checked</c:if>>
              <label class="form-check-label" for="suNight">夜</label>
            </div>
          </div>
        </div>

        <!-- ソート -->
        <div class="col-12 col-md-8">
          <label class="form-label">並び替え</label>
          <div class="d-flex gap-2">
            <select class="form-select" name="sortKey">
              <option value="">指定なし</option>
              <option value="wdHours"    <c:if test="${param.sortKey=='wdHours'}">selected</c:if>>平日時間</option>
              <option value="saHours"    <c:if test="${param.sortKey=='saHours'}">selected</c:if>>土曜時間</option>
              <option value="suHours"    <c:if test="${param.sortKey=='suHours'}">selected</c:if>>日曜時間</option>
              <option value="totalHours" <c:if test="${param.sortKey=='totalHours'}">selected</c:if>>合計時間</option>
              <option value="lastMonth"  <c:if test="${param.sortKey=='lastMonth'}">selected</c:if>>先月稼働時間（切上）</option>
              <option value="capacity"   <c:if test="${param.sortKey=='capacity'}">selected</c:if>>余力時間</option>
            </select>
            <select class="form-select" name="sortDir">
              <option value="desc" <c:if test="${empty param.sortDir or param.sortDir=='desc'}">selected</c:if>>降順</option>
              <option value="asc"  <c:if test="${param.sortDir=='asc'}">selected</c:if>>昇順</option>
            </select>
          </div>
          <div class="form-text">※ フィルタを指定した場合は「〇→△」優先で並び、その後に上記ソートを適用します。</div>
        </div>

        <div class="col-12 col-md-4 text-md-end">
          <button class="btn btn-primary me-2" type="submit">適用</button>
          <a class="btn btn-outline-secondary"
             href="${pageContext.request.contextPath}/admin/assignment/register?id=${customer.id}&companyName=${fn:escapeXml(customer.companyName)}&targetYM=${not empty param.targetYM ? param.targetYM : targetYM}">
            リセット
          </a>
        </div>
      </div>
    </div>
  </form>

  <!-- 一覧 -->
  <c:choose>
    <c:when test="${empty secretaryCandidates}">
      <div class="text-muted">該当する秘書がいません。</div>
    </c:when>
    <c:otherwise>
      <div class="d-flex justify-content-between align-items-center mb-2">
        <div class="small text-muted">候補：<strong>${fn:length(secretaryCandidates)}</strong> 名</div>
        <div class="small text-muted">凡例：<span class="me-2">〇=可</span><span class="me-2">△=相談</span><span>×=不可</span></div>
      </div>

      <div class="table-responsive">
        <table class="table table-striped table-hover table-sm align-middle">
          <thead class="table-primary">
            <tr>
              <th>秘書名</th>
              <th>ランク</th>
              <th class="text-center">平日 朝</th>
              <th class="text-center">平日 昼</th>
              <th class="text-center">平日 夜</th>
              <th class="text-center">土曜 朝</th>
              <th class="text-center">土曜 昼</th>
              <th class="text-center">土曜 夜</th>
              <th class="text-center">日曜 朝</th>
              <th class="text-center">日曜 昼</th>
              <th class="text-center">日曜 夜</th>
              <th class="text-end">平日時間</th>
              <th class="text-end">土曜時間</th>
              <th class="text-end">日曜時間</th>
              <th class="text-end">合計時間</th>
              <th class="text-end">先月稼働(切上)</th>
              <th class="text-end">余力</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="r" items="${secretaryCandidates}">
              <tr>
                <td><a href="${pageContext.request.contextPath}/admin/secretary/detail?id=${r.id}"><c:out value="${r.name}"/></a></td>
                <td><c:out value="${empty r.rankName ? '-' : r.rankName}"/></td>

                <!-- 0/1/2 を  ×/△/〇 に -->
                <td class="text-center">${r.wdAm   == 2 ? '〇' : (r.wdAm   == 1 ? '△' : '×')}</td>
                <td class="text-center">${r.wdDay  == 2 ? '〇' : (r.wdDay  == 1 ? '△' : '×')}</td>
                <td class="text-center">${r.wdNight== 2 ? '〇' : (r.wdNight== 1 ? '△' : '×')}</td>

                <td class="text-center">${r.saAm   == 2 ? '〇' : (r.saAm   == 1 ? '△' : '×')}</td>
                <td class="text-center">${r.saDay  == 2 ? '〇' : (r.saDay  == 1 ? '△' : '×')}</td>
                <td class="text-center">${r.saNight== 2 ? '〇' : (r.saNight== 1 ? '△' : '×')}</td>

                <td class="text-center">${r.suAm   == 2 ? '〇' : (r.suAm   == 1 ? '△' : '×')}</td>
                <td class="text-center">${r.suDay  == 2 ? '〇' : (r.suDay  == 1 ? '△' : '×')}</td>
                <td class="text-center">${r.suNight== 2 ? '〇' : (r.suNight== 1 ? '△' : '×')}</td>

                <td class="text-end"><fmt:formatNumber value="${r.wdHours}"    pattern="#,##0"/></td>
                <td class="text-end"><fmt:formatNumber value="${r.saHours}"    pattern="#,##0"/></td>
                <td class="text-end"><fmt:formatNumber value="${r.suHours}"    pattern="#,##0"/></td>
                <td class="text-end"><fmt:formatNumber value="${r.totalHours}" pattern="#,##0"/></td>
                <td class="text-end"><fmt:formatNumber value="${r.lastMonth}"  pattern="#,##0"/></td>
                <td class="text-end"><fmt:formatNumber value="${r.capacity}"   pattern="#,##0"/></td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </div>
    </c:otherwise>
  </c:choose>
</div>

<script>
(function() {
  const fmt = new Intl.NumberFormat('ja-JP');
  const selRank = document.getElementById('taskRankId');
  const selSec  = document.getElementById('secretaryId');

  const dispBaseCust = document.getElementById('dispBaseCust');
  const dispBaseSec  = document.getElementById('dispBaseSec');
  const dispIncCust  = document.getElementById('dispIncCust');
  const dispIncSec   = document.getElementById('dispIncSec');

  const hBaseCust = document.getElementById('hBaseCust');
  const hBaseSec  = document.getElementById('hBaseSec');
  const hIncCust  = document.getElementById('hIncCust');
  const hIncSec   = document.getElementById('hIncSec');

  // ★ 継続単価の2入力（顧客/秘書）
  const inpContCust = document.querySelector('input[name="customerBasedIncentiveForCustomer"]');
  const inpContSec  = document.querySelector('input[name="customerBasedIncentiveForSecretary"]');

  function updateRank() {
    const opt = selRank.options[selRank.selectedIndex];
    const baseCust = opt ? opt.getAttribute('data-basecust') : '';
    const baseSec  = opt ? opt.getAttribute('data-basesec')  : '';
    dispBaseCust.textContent = baseCust ? fmt.format(parseInt(baseCust, 10) || 0) : '-';
    dispBaseSec.textContent  = baseSec  ? fmt.format(parseInt(baseSec, 10)  || 0) : '-';
    hBaseCust.value = baseCust || '';
    hBaseSec.value  = baseSec  || '';
  }

  function updateSecretary() {
    const opt = selSec.options[selSec.selectedIndex];
    const incSec  = opt ? opt.getAttribute('data-rankup')     : '0';
    const incCust = opt ? opt.getAttribute('data-rankupcust') : '0';
    const nSec  = parseInt(incSec,  10) || 0;
    const nCust = parseInt(incCust, 10) || 0;
    dispIncSec.textContent  = fmt.format(nSec);
    dispIncCust.textContent = fmt.format(nCust);
    hIncSec.value  = String(nSec);
    hIncCust.value = String(nCust);
  }

  // ★ ランクがPなら4項目を0固定（表示・hidden・入力禁止）
  function enforceZeroIfRankP() {
    const opt = selRank.options[selRank.selectedIndex];
    const rankName = (opt && opt.getAttribute('data-rankname')) || '';
    const isP = rankName === 'P';

    if (isP) {
      // ランクアップ単価（顧客/秘書）：表示＆POST値ともに 0
      dispIncCust.textContent = '0';
      dispIncSec.textContent  = '0';
      hIncCust.value = '0';
      hIncSec.value  = '0';

      // 継続単価（顧客/秘書）：入力欄を 0 にしてreadOnly化
      inpContCust.value = '0';
      inpContSec.value  = '0';
      inpContCust.readOnly = true;
      inpContSec.readOnly  = true;
      // 念のため min を満たす（既に min=0 指定済み）
    } else {
      // ランクP以外は編集可能
      inpContCust.readOnly = false;
      inpContSec.readOnly  = false;
      // ランクアップ単価は秘書選択に従う（updateSecretaryで更新）
    }
  }

  selRank.addEventListener('change', function(){
    updateRank();
    updateSecretary();   // ランクP以外へ切替時は秘書由来の増額を反映
    enforceZeroIfRankP();
  });

  selSec.addEventListener('change', function(){
    updateSecretary();
    enforceZeroIfRankP(); // ランクPなら0固定を優先
  });

  // 初期表示
  updateRank();
  updateSecretary();
  enforceZeroIfRankP();
})();
</script>

	<script
		src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
