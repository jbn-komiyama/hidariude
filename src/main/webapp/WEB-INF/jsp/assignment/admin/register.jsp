<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8" />
  <title>アサイン登録</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
</head>
<body class="bg-light">
<div class="container py-4">
  <h1 class="h4 mb-3">アサイン登録</h1>

  <c:if test="${not empty errorMsg}">
    <div class="alert alert-danger">
      <ul class="mb-0">
        <c:forEach var="m" items="${errorMsg}">
          <li><c:out value="${m}"/></li>
        </c:forEach>
      </ul>
    </div>
  </c:if>

  <div class="card shadow-sm">
    <div class="card-body">
      <form method="post" action="${pageContext.request.contextPath}/admin/assignment/register_check">
        <div class="row g-3">

          <div class="col-md-6">
            <label class="form-label">顧客</label>
            <div class="form-control-plaintext fw-semibold"><c:out value="${customer.companyName}"/></div>
            <input type="hidden" name="customerId" value="${customer.id}">
          </div>

          <div class="col-md-6">
            <label class="form-label">対象月</label>
            <input type="month" class="form-control" name="targetYearMonth" value="${targetYm}" required>
          </div>

          <div class="col-md-6">
            <label class="form-label">秘書</label>
            <select class="form-select" name="secretaryId" id="secretaryId" required>
              <option value="">選択してください</option>
              <c:forEach var="s" items="${secretaries}">
                <c:choose>
                  <c:when test="${not empty s.secretaryRank}">
                    <!-- 小数なし・カンマなしで埋め込む -->
                    <fmt:formatNumber value="${s.secretaryRank.increaseBasePaySecretary}" maxFractionDigits="0" groupingUsed="false" var="rankupSecPlain"/>
                    <fmt:formatNumber value="${s.secretaryRank.increaseBasePayCustomer}"  maxFractionDigits="0" groupingUsed="false" var="rankupCustPlain"/>
                  </c:when>
                  <c:otherwise>
                    <c:set var="rankupSecPlain" value="0"/>
                    <c:set var="rankupCustPlain" value="0"/>
                  </c:otherwise>
                </c:choose>
                <option value="${s.id}"
                        data-rankup="${rankupSecPlain}"
                        data-rankupcust="${rankupCustPlain}">
                  <c:out value="${s.name}"/>(<c:out value="${s.secretaryRank.rankName}"/>)
                </option>
              </c:forEach>
            </select>
          </div>

          <div class="col-md-6">
            <label class="form-label">タスクランク</label>
            <select class="form-select" name="taskRankId" id="taskRankId" required>
              <option value="">選択してください</option>
              <c:forEach var="tr" items="${taskRanks}">
                <fmt:formatNumber value="${tr.basePayCustomer}"  maxFractionDigits="0" groupingUsed="false" var="trBaseCustPlain"/>
                <fmt:formatNumber value="${tr.basePaySecretary}" maxFractionDigits="0" groupingUsed="false" var="trBaseSecPlain"/>
                <option value="${tr.id}"
                        data-basecust="${trBaseCustPlain}"
                        data-basesec="${trBaseSecPlain}">
                  <c:out value="${tr.rankName}"/>（
                  顧客:<fmt:formatNumber value="${tr.basePayCustomer}" pattern="#,##0"/> /
                  秘書:<fmt:formatNumber value="${tr.basePaySecretary}" pattern="#,##0"/>）
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
            <label class="form-label">継続単価（顧客）</label>
            <input type="number" step="1" min="0" class="form-control" name="customerBasedIncentiveForCustomer" value="0">
          </div>
          <div class="col-md-3">
            <label class="form-label">継続単価（秘書）</label>
            <input type="number" step="1" min="0" class="form-control" name="customerBasedIncentiveForSecretary" value="0">
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

          <input type="hidden" name="basePayCustomer"  id="hBaseCust" value="">
          <input type="hidden" name="basePaySecretary" id="hBaseSec"  value="">
          <input type="hidden" name="increaseBasePayCustomer"  id="hIncCust" value="0">
          <input type="hidden" name="increaseBasePaySecretary" id="hIncSec"  value="0">

          <div class="col-12 text-end">
            <button type="submit" class="btn btn-primary">確認へ</button>
            <a href="${pageContext.request.contextPath}/admin/assignment" class="btn btn-secondary">戻る</a>
          </div>
        </div>
      </form>
    </div>
  </div>
</div>

<script>
(function(){
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

	  function updateRank(){
	    const opt = selRank.options[selRank.selectedIndex];
	    const baseCust = opt ? opt.getAttribute('data-basecust') : '';
	    const baseSec  = opt ? opt.getAttribute('data-basesec')  : '';
	    dispBaseCust.textContent = baseCust ? fmt.format(parseInt(baseCust,10)||0) : '-';
	    dispBaseSec.textContent  = baseSec  ? fmt.format(parseInt(baseSec,10)||0)  : '-';
	    hBaseCust.value = baseCust || '';
	    hBaseSec.value  = baseSec  || '';
	  }

	  function updateSecretary(){
	    const opt = selSec.options[selSec.selectedIndex];
	    const incSec  = opt ? opt.getAttribute('data-rankup')     : '0';
	    const incCust = opt ? opt.getAttribute('data-rankupcust') : '0';
	    const nSec  = parseInt(incSec, 10)  || 0;
	    const nCust = parseInt(incCust, 10) || 0;
	    dispIncSec.textContent  = fmt.format(nSec);
	    dispIncCust.textContent = fmt.format(nCust);
	    hIncSec.value  = String(nSec);
	    hIncCust.value = String(nCust);
	  }

	  selRank.addEventListener('change', updateRank);
	  selSec.addEventListener('change', updateSecretary);

	  updateRank();
	  updateSecretary();
	})();
</script>
</body>
</html>