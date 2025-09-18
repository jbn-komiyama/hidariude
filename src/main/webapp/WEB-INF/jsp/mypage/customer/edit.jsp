<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>マイページ編集（担当者＋会社）</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<div class="container py-4">
  <h1 class="h3 mb-3">マイページ編集</h1>

  <c:if test="${not empty errorMsg}">
    <div class="alert alert-danger">
      <ul class="mb-0">
        <c:forEach var="m" items="${errorMsg}">
          <li><c:out value="${m}"/></li>
        </c:forEach>
      </ul>
    </div>
  </c:if>

  <form method="post" action="<c:url value='/customer/mypage/edit_check'/>" class="card p-3 shadow-sm">
    <h2 class="h6">顧客担当者情報</h2>
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

    <h2 class="h6">会社情報</h2>
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

      <div class="col-md-4">
        <label class="form-label">郵便番号</label>
        <input id="postalCode" name="postalCode" class="form-control" placeholder="100-0001"
               value="${not empty param.postalCode ? param.postalCode : (not empty postalCode ? postalCode : customer.postalCode)}"/>
      </div>
      <div class="col-md-8">
        <label class="form-label">住所</label>
        <input id="address1" name="address1" class="form-control"
               value="${not empty param.address1 ? param.address1 : (not empty address1 ? address1 : customer.address1)}"/>
      </div>
      <div class="col-md-6">
        <label class="form-label">住所2</label>
        <input name="address2" class="form-control"
               value="${not empty param.address2 ? param.address2 : (not empty address2 ? address2 : customer.address2)}"/>
      </div>
      <div class="col-md-6">
        <label class="form-label">建物名</label>
        <input name="building" class="form-control"
               value="${not empty param.building ? param.building : (not empty building ? building : customer.building)}"/>
      </div>
    </div>

    <div class="mt-3 text-end">
      <a class="btn btn-outline-secondary" href="<c:url value='/customer/mypage/home'/>">戻る</a>
      <button type="submit" class="btn btn-primary">確認へ</button>
    </div>
  </form>
</div>

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
      const res = await fetch("https://zipcloud.ibsnet.co.jp/api/search?zipcode=" + digits);
      const data = await res.json();
      if (data && data.status === 200 && Array.isArray(data.results) && data.results.length > 0) {
        const r = data.results[0];
        addrInput.value = (r.address1 || "") + (r.address2 || "") + (r.address3 || "");
        pcInput.value = digits.slice(0,3) + "-" + digits.slice(3);
      } else {
        alert("住所が見つかりませんでした（郵便番号を確認してください）");
      }
    } catch (e) { alert("住所検索でエラーが発生しました。"); }
    finally { pcInput.disabled = false; }
  });
})();
</script>
</body>
</html>
