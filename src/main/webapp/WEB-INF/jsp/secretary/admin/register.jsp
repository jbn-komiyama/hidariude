<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>秘書 新規登録</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
  <div class="container py-4">
    <h1 class="h3 mb-3">秘書 新規登録</h1>

    <c:if test="${not empty errorMsg}">
      <div class="alert alert-danger">${errorMsg}</div>
    </c:if>

    <form method="post" action="<%= request.getContextPath() %>/admin/secretary/register_check" class="card p-3 shadow-sm">
      <div class="row g-3">
        <div class="col-md-4">
          <label class="form-label">秘書コード（任意）</label>
          <input type="text" name="secretaryCode" class="form-control" value="${secretaryCode}">
        </div>
        <div class="col-md-4">
          <label class="form-label">ランク <span class="text-danger">*</span></label>
          <select name="secretaryRankId" class="form-select">
            <option value="">選択してください</option>
            <c:forEach var="r" items="${sessionScope.ranks}">
              <option value="${r.id}" <c:if test="${r.id == secretaryRankId}">selected</c:if>>${r.rankName}</option>
            </c:forEach>
          </select>
        </div>
        <div class="col-md-4">
          <label class="form-label">PM対応</label><br/>
          <input type="checkbox" name="pmSecretary" value="true" <c:if test="${pmSecretary == 'true'}">checked</c:if>> 可
        </div>

        <div class="col-md-6">
          <label class="form-label">氏名 <span class="text-danger">*</span></label>
          <input type="text" name="name" class="form-control" value="${name}" required>
        </div>
        <div class="col-md-6">
          <label class="form-label">氏名（ふりがな）</label>
          <input type="text" name="nameRuby" class="form-control" value="${nameRuby}">
        </div>

        <div class="col-md-6">
          <label class="form-label">メール <span class="text-danger">*</span></label>
          <input type="email" name="mail" class="form-control" value="${mail}" required>
        </div>
        <div class="col-md-6">
          <label class="form-label">パスワード <span class="text-danger">*</span></label>
          <input type="password" name="password" class="form-control" value="${password}" required>
        </div>

        <div class="col-md-4">
          <label class="form-label">電話番号</label>
          <input type="text" name="phone" class="form-control" value="${phone}">
        </div>
        <div class="col-md-4">
          <label class="form-label">郵便番号</label>
          <input type="text" name="postalCode" class="form-control" value="${postalCode}">
        </div>

        <div class="col-md-6">
          <label class="form-label">住所１</label>
          <input type="text" name="address1" class="form-control" value="${address1}">
        </div>
        <div class="col-md-6">
          <label class="form-label">住所２</label>
          <input type="text" name="address2" class="form-control" value="${address2}">
        </div>
        <div class="col-md-6">
          <label class="form-label">建物名</label>
          <input type="text" name="building" class="form-control" value="${building}">
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