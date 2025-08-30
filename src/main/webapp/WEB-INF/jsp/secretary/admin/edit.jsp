<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>秘書 編集</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
  <div class="container py-4">
    <h1 class="h3 mb-3">秘書 編集</h1>

    <c:if test="${not empty errorMsg}">
      <div class="alert alert-danger">${errorMsg}</div>
    </c:if>

    <form method="post" action="<%= request.getContextPath() %>/admin/secretary/edit_check" class="card p-3 shadow-sm">
      <input type="hidden" name="id" value="${empty id ? secretary.id : id}"/>

      <div class="row g-3">
        <div class="col-md-4">
          <label class="form-label">秘書コード（任意）</label>
          <input type="text" name="secretaryCode" class="form-control"
                 value="${empty secretaryCode ? secretary.secretaryCode : secretaryCode}">
        </div>
        <div class="col-md-4">
          <label class="form-label">ランク</label>
          <select name="secretaryRankId" class="form-select">
            <c:forEach var="r" items="${ranks}">
              <c:set var="val" value="${empty secretaryRankId ? secretary.secretaryRank.id : secretaryRankId}"/>
              <option value="${r.id}" <c:if test="${r.id == val}">selected</c:if>>${r.rankName}</option>
            </c:forEach>
          </select>
        </div>
        <div class="col-md-4">
          <label class="form-label">PM対応</label><br/>
          <c:set var="pmVal" value="${empty pmSecretary ? secretary.pmSecretary : (pmSecretary == 'true')}"/>
          <input type="checkbox" name="pmSecretary" value="true" <c:if test="${pmVal}">checked</c:if>> 可
        </div>

        <div class="col-md-6">
          <label class="form-label">氏名</label>
          <input type="text" name="name" class="form-control"
                 value="${empty name ? secretary.name : name}" required>
        </div>
        <div class="col-md-6">
          <label class="form-label">氏名（ふりがな）</label>
          <input type="text" name="nameRuby" class="form-control"
                 value="${empty nameRuby ? secretary.nameRuby : nameRuby}">
        </div>

        <div class="col-md-6">
          <label class="form-label">メール</label>
          <input type="email" name="mail" class="form-control"
                 value="${empty mail ? secretary.mail : mail}" required>
        </div>

        <div class="col-md-4">
          <label class="form-label">電話番号</label>
          <input type="text" name="phone" class="form-control"
                 value="${empty phone ? secretary.phone : phone}">
        </div>
        <div class="col-md-4">
          <label class="form-label">郵便番号</label>
          <input type="text" name="postalCode" class="form-control"
                 value="${empty postalCode ? secretary.postalCode : postalCode}">
        </div>

        <div class="col-md-6">
          <label class="form-label">住所１</label>
          <input type="text" name="address1" class="form-control"
                 value="${empty address1 ? secretary.address1 : address1}">
        </div>
        <div class="col-md-6">
          <label class="form-label">住所２</label>
          <input type="text" name="address2" class="form-control"
                 value="${empty address2 ? secretary.address2 : address2}">
        </div>
        <div class="col-md-6">
          <label class="form-label">建物名</label>
          <input type="text" name="building" class="form-control"
                 value="${empty building ? secretary.building : building}">
        </div>
      </div>

      <div class="mt-3 text-end">
        <a href="<%= request.getContextPath() %>/admin/secretary" class="btn btn-outline-secondary">戻る</a>
        <button type="submit" class="btn btn-primary">確認へ</button>
      </div>
    </form>
  </div>
</body>
</html>