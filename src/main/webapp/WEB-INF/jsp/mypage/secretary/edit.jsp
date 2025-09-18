<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>秘書 マイページ編集</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <style>
    .avatar {
      width:64px; height:64px; border-radius:50%;
      display:flex; align-items:center; justify-content:center;
      background:#e9ecef; font-weight:700; font-size:22px;
    }
    .kv th{width:160px; white-space:nowrap;}
    .ym-nav .btn-link { line-height:1; }
    .ym-nav .btn-link.js-today { color:#0d6efd; }
    .ym-nav .btn-link.text-muted { color:#6c757d !important; }
  </style>
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jsp/_parts/secretary/navbar.jspf" %>

<div class="container py-4">
    <h1 class="h3 mb-3">秘書 マイページ編集</h1>
  <div class="d-flex align-items-center justify-content-between mb-3">
    <c:if test="${not empty errorMsg}">
    <div class="alert alert-danger">
      <ul class="mb-0">
        <c:forEach var="m" items="${errorMsg}">
          <li><c:out value="${m}"/></li>
        </c:forEach>
      </ul>
    </div>
  </c:if>

  <form method="post" action="${pageContext.request.contextPath}/secretary/mypage/edit_check" class="card p-3 shadow-sm">
    <div class="row g-3">
    
      <div class="col-md-6">
        <label class="form-label">パスワード</label>
        <input type="password" name="password" class="form-control" minlength="8" autocomplete="new-password"
               value="">
        <div class="form-text">※空欄の場合は変更されません。</div>
      </div>
      
      <div class="col-md-6">
        <label class="form-label">氏名 <span class="text-danger">*</span></label>
        <input type="text" name="name" class="form-control" required
               value="${not empty param.name ? param.name : (not empty name ? name : secretary.name)}">
      </div>

      <div class="col-md-6">
        <label class="form-label">氏名（ふりがな）</label>
        <input type="text" name="nameRuby" class="form-control"
               value="${not empty param.nameRuby ? param.nameRuby : (not empty nameRuby ? nameRuby : secretary.nameRuby)}">
      </div>
      
     
        <div class="col-md-4">
          <label class="form-label">ランク</label>
          <c:set var="currentRankId" value="${empty secretaryRankId ? secretary.secretaryRank.id : secretaryRankId}"/>
          <div class="form-control-plaintext fw-semibold">
            <c:out value="${secretary.secretaryRank != null ? secretary.secretaryRank.rankName : '—'}"/>
          </div>
          <input type="hidden" name="secretaryRankId" value="${currentRankId}"/>
        </div>
   
        <div class="col-md-4">
          <label class="form-label">PM対応</label><br/>
          <c:set var="pmVal" value="${empty pmSecretary ? secretary.pmSecretary : (pmSecretary == 'true')}"/>
          <span class="badge ${pmVal ? 'bg-primary' : 'bg-secondary'}">
            <c:choose>
              <c:when test="${pmVal}">可</c:when>
              <c:otherwise>不可</c:otherwise>
            </c:choose>
          </span> 
          <input type="hidden" name="pmSecretary" value="${pmVal ? 'true' : 'false'}"/>
        </div>

      <div class="col-md-6">
        <label class="form-label">メール <span class="text-danger">*</span></label>
        <input type="email" name="mail" class="form-control" required
               value="${not empty param.mail ? param.mail : (not empty mail ? mail : secretary.mail)}">
      </div>

      <div class="col-md-6">
        <label class="form-label">電話番号</label>
        <input type="tel" name="phone" class="form-control"
               value="${not empty param.phone ? param.phone : (not empty phone ? phone : secretary.phone)}">
      </div>

      <div class="col-md-6">
        <label class="form-label">郵便番号</label>
        <input type="text" name="postalCode" class="form-control" id="postalCode" placeholder="100-0001"
               value="${not empty param.postalCode ? param.postalCode : (not empty postalCode ? postalCode : secretary.postalCode)}">
      </div>

      <div class="col-md-6">
        <label class="form-label">住所</label>
        <input type="text" name="address1" class="form-control" id="address1"
               value="${not empty param.address1 ? param.address1 : (not empty address1 ? address1 : secretary.address1)}">
      </div>

      <div class="col-md-6">
        <label class="form-label">住所2</label>
        <input type="text" name="address2" class="form-control"
               value="${not empty param.address2 ? param.address2 : (not empty address2 ? address2 : secretary.address2)}">
      </div>

      <div class="col-md-6">
        <label class="form-label">建物名</label>
        <input type="text" name="building" class="form-control"
               value="${not empty param.building ? param.building : (not empty building ? building : secretary.building)}">
      </div>
    </div>

    <div class="mt-3 text-end">
      <a class="btn btn-outline-secondary" href="<c:url value='/secretary/mypage'/>">戻る</a>
      <button type="submit" class="btn btn-primary">確認へ</button>
    </div>
  </form>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
(function() {
  const pcInput = document.getElementById("postalCode");
  const addrInput = document.getElementById("address1");

  if (!pcInput || !addrInput) return;

  pcInput.addEventListener("blur", async function () {
    const digits = (pcInput.value || "").replace(/\D/g, ""); 
    if (digits.length !== 7) return;

    try {
      pcInput.disabled = true;

      const res = await fetch("https://zipcloud.ibsnet.co.jp/api/search?zipcode=" + digits, {
        method: "GET",
        mode: "cors",
        cache: "no-store"
      });
      const data = await res.json();

      if (data && data.status === 200 && Array.isArray(data.results) && data.results.length > 0) {
        const r = data.results[0];
        const address = (r.address1 || "") + (r.address2 || "") + (r.address3 || "");
        addrInput.value = address;

        pcInput.value = digits.slice(0,3) + "-" + digits.slice(3);
      } else {
        alert("住所が見つかりませんでした（郵便番号を確認してください）");
      }
    } catch (e) {
      console.error("住所検索エラー:", e);
      alert("住所検索でエラーが発生しました。時間をおいてお試しください。");
    } finally {
      pcInput.disabled = false;
    }
  });
})();
</script>
</body>
</html>