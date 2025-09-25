<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<title>顧客 マイページ編集</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light"><!-- ★ 秘書側と同じ薄グレー背景 -->
<%@ include file="/WEB-INF/jsp/_parts/customer/navbar.jspf" %>

<div class="container py-4">
  <h1 class="h3 mb-3">顧客 マイページ編集</h1>

  <!-- ★ エラーブロック（秘書側と同じ扱い） -->
  <c:if test="${not empty errorMsg}">
    <div class="alert alert-danger">
      <ul class="mb-0">
        <c:forEach var="m" items="${errorMsg}">
          <li><c:out value="${m}"/></li>
        </c:forEach>
      </ul>
    </div>
  </c:if>

  <!-- ★ フォームカード（shadow-sm / p-3） -->
  <form method="post" action="<c:url value='/customer/mypage/edit_check'/>" class="card p-3 shadow-sm">
    <h2 class="h6 mb-3">顧客担当者情報</h2>
    <div class="row g-3">
      <div class="col-md-6">
        <label class="form-label">氏名 <span class="text-danger">*</span></label>
        <input name="contactName" class="form-control" required
               value="${not empty param.contactName ? param.contactName : (not empty contactName ? contactName : cc.name)}"/>
      </div>
      <div class="col-md-6">
        <label class="form-label">氏名（ふりがな）</label>
        <input name="contactNameRuby" class="form-control"
               value="${not empty param.contactNameRuby ? param.contactNameRuby : (not empty contactNameRuby ? contactNameRuby : cc.nameRuby)}"/>
      </div>
      <div class="col-md-6">
        <label class="form-label">部署</label>
        <input name="contactDepartment" class="form-control"
               value="${not empty param.contactDepartment ? param.contactDepartment : (not empty contactDepartment ? contactDepartment : cc.department)}"/>
      </div>
      <div class="col-md-6">
        <label class="form-label">メール <span class="text-danger">*</span></label>
        <input type="email" name="contactMail" class="form-control" required
               value="${not empty param.contactMail ? param.contactMail : (not empty contactMail ? contactMail : cc.mail)}"/>
      </div>
      <div class="col-md-6">
        <label class="form-label">電話番号</label>
        <input name="contactPhone" class="form-control"
               value="${not empty param.contactPhone ? param.contactPhone : (not empty contactPhone ? contactPhone : cc.phone)}"/>
      </div>
    </div>

    <hr class="my-4"/>

    <h2 class="h6 mb-3">会社情報</h2>
    <div class="row g-3">
      <div class="col-md-8">
        <label class="form-label">会社名 <span class="text-danger">*</span></label>
        <input name="companyName" class="form-control" required
               value="${not empty param.companyName ? param.companyName : (not empty companyName ? companyName : customer.companyName)}"/>
      </div>
      <div class="col-md-4">
        <label class="form-label">会社コード</label>
        <div class="form-control-plaintext fw-semibold">
          <c:out value="${empty customer.companyCode ? '—' : customer.companyCode}"/>
        </div>
      </div>

      <div class="col-md-6">
        <label class="form-label">代表メール</label>
        <input type="email" name="companyMail" class="form-control"
               value="${not empty param.companyMail ? param.companyMail : (not empty companyMail ? companyMail : customer.mail)}"/>
      </div>
      <div class="col-md-6">
        <label class="form-label">電話番号</label>
        <input name="companyPhone" class="form-control"
               value="${not empty param.companyPhone ? param.companyPhone : (not empty companyPhone ? companyPhone : customer.phone)}"/>
      </div>

      <!-- ▼ 郵便番号 + 住所検索（秘書側と同仕様） -->
      <div class="col-md-6">
        <label class="form-label">郵便番号</label>
        <div class="input-group">
          <input type="text" id="postalCode" name="postalCode"
                 class="form-control" inputmode="numeric"
                 placeholder="1000001（ハイフン不要）"
                 value="${not empty param.postalCode ? param.postalCode : (not empty postalCode ? postalCode : customer.postalCode)}">
          <button class="btn btn-outline-primary" type="button" id="btnLookup">住所検索</button>
        </div>
        <div class="form-text">数字7桁を入力して「住所検索」を押すと、都道府県・市区町村・町域を自動入力します。</div>
        <div id="zipError" class="invalid-feedback d-none"></div>
      </div>

      <!-- 住所（表示＋hidden送信） -->
      <div class="col-12">
        <label class="form-label mb-1">住所</label>

        <!-- 住所1（自動）：都道府県・市区町村・町域 -->
        <div id="addr1View" class="border rounded p-2 bg-white text-body"
             tabindex="0" aria-live="polite">
          <c:out value="${not empty param.address1 ? param.address1 : (not empty address1 ? address1 : customer.address1)}"/>
        </div>
        <div class="form-text mb-2">（自動入力）</div>

        <!-- 住所2（直書き）：番地・丁目など -->
        <label class="form-label mb-1">住所2（番地・丁目など）</label>
        <div id="addr2View" class="border rounded p-2 bg-white text-body"
             contenteditable="true" tabindex="0" aria-label="番地等を入力">
          <c:out value="${not empty param.address2 ? param.address2 : (not empty address2 ? address2 : customer.address2)}"/>
        </div>

        <!-- 建物名（直書き） -->
        <label class="form-label mt-2 mb-1">建物名・部屋番号</label>
        <div id="bldgView" class="border rounded p-2 bg-white text-body"
             contenteditable="true" tabindex="0" aria-label="建物名・部屋番号を入力">
          <c:out value="${not empty param.building ? param.building : (not empty building ? building : customer.building)}"/>
        </div>

        <!-- 送信用 hidden -->
        <input type="hidden" name="address1" id="address1Hidden"
               value="${fn:escapeXml(not empty param.address1 ? param.address1 : (not empty address1 ? address1 : customer.address1))}">
        <input type="hidden" name="address2" id="address2Hidden"
               value="${fn:escapeXml(not empty param.address2 ? param.address2 : (not empty address2 ? address2 : customer.address2))}">
        <input type="hidden" name="building" id="buildingHidden"
               value="${fn:escapeXml(not empty param.building ? param.building : (not empty building ? building : customer.building))}">
      </div>
      <!-- ▲ 郵便番号/住所ブロック -->
    </div>

    <div class="mt-3 text-end">
      <a class="btn btn-outline-secondary" href="<c:url value='/customer/mypage/home'/>">戻る</a>
      <button type="submit" class="btn btn-primary">確認へ</button>
    </div>
  </form>
</div>

<script>
(() => {
  const $ = (q) => document.querySelector(q);

  const postal = $('#postalCode');
  const btn = $('#btnLookup');
  const err = $('#zipError');

  const addr1View = $('#addr1View');
  const addr2View = $('#addr2View');
  const bldgView  = $('#bldgView');

  const address1Hidden = $('#address1Hidden');
  const address2Hidden = $('#address2Hidden');
  const buildingHidden = $('#buildingHidden');

  // 表示領域 → hidden 同期
  function syncHidden() {
    address1Hidden.value = (addr1View.textContent || '').trim();
    address2Hidden.value = (addr2View.textContent || '').trim();
    buildingHidden.value = (bldgView.textContent  || '').trim();
  }
  ['input','blur','keyup','paste'].forEach(ev=>{
    addr2View.addEventListener(ev, syncHidden);
    bldgView.addEventListener(ev,  syncHidden);
  });
  document.addEventListener('DOMContentLoaded', syncHidden);

  // 郵便番号検索
  async function lookup() {
    err.classList.add('d-none');
    err.textContent = '';

    const raw = (postal.value || '').replace(/[^0-9]/g, '');
    postal.value = raw; // 数字のみ保持
    if (raw.length !== 7) {
      err.textContent = '郵便番号は数字7桁で入力してください。';
      err.classList.remove('d-none');
      return;
    }

    btn.disabled = true;
    btn.classList.add('disabled');

    try {
      const res = await fetch('https://zipcloud.ibsnet.co.jp/api/search?zipcode=' + raw);
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const data = await res.json();

      if (data.status !== 200 || !data.results || !data.results.length) {
        err.textContent = '該当する住所が見つかりませんでした。';
        err.classList.remove('d-none');
        return;
      }

      const r = data.results[0];
      const addr1 = (r.address1 || '') + (r.address2 || '') + (r.address3 || '');
      addr1View.textContent = addr1;

      syncHidden();
    } catch (e) {
      err.textContent = '住所検索に失敗しました。ネットワークをご確認ください。';
      err.classList.remove('d-none');
      console.error(e);
    } finally {
      btn.disabled = false;
      btn.classList.remove('disabled');
    }
  }

  btn.addEventListener('click', lookup);
  postal.addEventListener('blur', () => {
    if ((postal.value || '').replace(/[^0-9]/g,'').length === 7) lookup();
  });

  // 初期オート検索（住所未入力かつ郵便番号が7桁）
  document.addEventListener('DOMContentLoaded', () => {
    const raw = (postal.value || '').replace(/[^0-9]/g,'');
    if (!addr1View.textContent.trim() && raw.length === 7) lookup();
  });

  // 送信直前に同期（念のため）
  document.querySelector('form').addEventListener('submit', syncHidden);
})();
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
