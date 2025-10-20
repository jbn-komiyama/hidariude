<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<title>秘書 編集</title>
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
	rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
	<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf"%>

	<div class="container py-4">
		<h1 class="h3 mb-3">秘書 編集</h1>

		<c:if test="${not empty errorMsg}">
			<div class="alert alert-danger" role="alert">
				<ul class="mb-0">
					<c:forEach var="m" items="${errorMsg}">
						<li><c:out value="${m}" /></li>
					</c:forEach>
				</ul>
			</div>
		</c:if>

		<form method="post"
			action="<c:url value='/admin/secretary/edit_check'/>"
			class="card p-3 shadow-sm">
			<input type="hidden" name="id"
				value="${empty id ? secretary.id : id}" />

			<div class="row g-3">
				<div class="col-md-4">
					<label class="form-label">秘書コード（任意）</label> <input type="text"
						name="secretaryCode" class="form-control"
						value="${empty secretaryCode ? secretary.secretaryCode : secretaryCode}">
				</div>

				<div class="col-md-4">
					<label class="form-label">ランク</label> <select
						name="secretaryRankId" class="form-select">
						<c:forEach var="r" items="${ranks}">
							<c:set var="val"
								value="${empty secretaryRankId ? secretary.secretaryRank.id : secretaryRankId}" />
							<option value="${r.id}"
								<c:if test="${r.id == val}">selected</c:if>>${r.rankName}</option>
						</c:forEach>
					</select>
				</div>

				<div class="col-md-4">
					<label class="form-label">PM対応</label><br />
					<c:set var="pmVal"
						value="${empty pmSecretary ? secretary.pmSecretary : (pmSecretary == 'true')}" />
					<input type="checkbox" name="pmSecretary" value="true"
						<c:if test="${pmVal}">checked</c:if>> 可
				</div>

				<div class="col-md-6">
					<label class="form-label">氏名 <span class="text-danger">*</span></label>
					<input type="text" name="name" class="form-control" required
						value="${empty name ? secretary.name : name}">
				</div>

				<div class="col-md-6">
					<label class="form-label">氏名（ふりがな）</label> <input type="text"
						name="nameRuby" class="form-control"
						value="${empty nameRuby ? secretary.nameRuby : nameRuby}">
				</div>

				<div class="col-md-6">
					<label class="form-label">メール <span class="text-danger">*</span></label>
					<input type="email" name="mail" class="form-control" required
						value="${empty mail ? secretary.mail : mail}">
				</div>

				<div class="col-md-6">
					<label class="form-label">電話番号</label> <input type="text"
						name="phone" class="form-control"
						value="${empty phone ? secretary.phone : phone}">
				</div>

				<!-- 郵便番号 + 住所検索（秘書マイページと同仕様） -->
				<div class="col-md-6">
					<label class="form-label">郵便番号</label>
					<div class="input-group">
						<input type="text" id="postalCode" name="postalCode"
							class="form-control" inputmode="numeric"
							placeholder="1000001（ハイフン不要）"
							value="${not empty param.postalCode ? param.postalCode : (not empty postalCode ? postalCode : secretary.postalCode)}">
						<button class="btn btn-outline-primary" type="button"
							id="btnLookup">住所検索</button>
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
						<c:out
							value="${not empty param.address1 ? param.address1 : (not empty address1 ? address1 : secretary.address1)}" />
					</div>
					<div class="form-text mb-2">（自動入力）</div>

					<!-- 住所2（直書き）：番地・丁目など -->
					<label class="form-label mb-1">住所2（番地・丁目など）</label>
					<div id="addr2View" class="border rounded p-2 bg-white text-body"
						contenteditable="true" tabindex="0" aria-label="番地等を入力">
						<c:out
							value="${not empty param.address2 ? param.address2 : (not empty address2 ? address2 : secretary.address2)}" />
					</div>

					<!-- 建物名（直書き） -->
					<label class="form-label mt-2 mb-1">建物名・部屋番号</label>
					<div id="bldgView" class="border rounded p-2 bg-white text-body"
						contenteditable="true" tabindex="0" aria-label="建物名・部屋番号を入力">
						<c:out
							value="${not empty param.building ? param.building : (not empty building ? building : secretary.building)}" />
					</div>

					<!-- 送信用 hidden（※パラメータ名は従来通り：address1 / address2 / building） -->
					<input type="hidden" name="address1" id="address1Hidden"
						value="${fn:escapeXml(not empty param.address1 ? param.address1 : (not empty address1 ? address1 : secretary.address1))}">
					<input type="hidden" name="address2" id="address2Hidden"
						value="${fn:escapeXml(not empty param.address2 ? param.address2 : (not empty address2 ? address2 : secretary.address2))}">
					<input type="hidden" name="building" id="buildingHidden"
						value="${fn:escapeXml(not empty param.building ? param.building : (not empty building ? building : secretary.building))}">
				</div>
			</div>

			<div class="mt-3 text-end">
				<a href="<c:url value='/admin/secretary'/>"
					class="btn btn-outline-secondary">戻る</a>
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

    // 表示領域 → hidden 同期（常に最新値をPOSTできるようにする）
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

	<script
		src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
