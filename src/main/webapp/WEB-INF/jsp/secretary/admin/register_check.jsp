<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>秘書 新規登録（確認）</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf" %>
  <div class="container py-4">
    <h1 class="h3 mb-3">秘書 新規登録（確認）</h1>

    <div class="card p-3 shadow-sm">
      <dl class="row">
        <dt class="col-sm-3">秘書コード</dt><dd class="col-sm-9">${secretaryCode}</dd>
        <dt class="col-sm-3">ランクID</dt><dd class="col-sm-9">${secretaryRankId}</dd>
        <dt class="col-sm-3">PM対応</dt><dd class="col-sm-9"><c:choose><c:when test="${pmSecretary == 'true'}">可</c:when><c:otherwise>不可</c:otherwise></c:choose></dd>
        <dt class="col-sm-3">氏名</dt><dd class="col-sm-9">${name}</dd>
        <dt class="col-sm-3">氏名（ふりがな）</dt><dd class="col-sm-9">${nameRuby}</dd>
        <dt class="col-sm-3">メール</dt><dd class="col-sm-9">${mail}</dd>
        <dt class="col-sm-3">電話番号</dt><dd class="col-sm-9">${phone}</dd>
        <dt class="col-sm-3">郵便番号</dt><dd class="col-sm-9">${postalCode}</dd>
        <dt class="col-sm-3">住所</dt><dd class="col-sm-9">${address1} ${address2} ${building}</dd>
      </dl>

      <!-- 戻る用（編集可能） -->
      <form method="post" action="<%= request.getContextPath() %>/admin/secretary/register" class="d-inline">
        <c:forEach var="n" items="${['secretaryCode','secretaryRankId','pmSecretary','name','nameRuby','mail','password','phone','postalCode','address1','address2','building']}">
          <input type="hidden" name="${n}" value="${requestScope[n]}">
        </c:forEach>
        <button type="submit" class="btn btn-outline-secondary">戻って修正</button>
      </form>

      <!-- 確定 -->
      <form method="post" action="<%= request.getContextPath() %>/admin/secretary/register_done" class="d-inline">
        <c:forEach var="n" items="${['secretaryCode','secretaryRankId','pmSecretary','name','nameRuby','mail','password','phone','postalCode','address1','address2','building']}">
          <input type="hidden" name="${n}" value="${requestScope[n]}">
        </c:forEach>
        <button type="submit" class="btn btn-primary">登録する</button>
      </form>
    </div>
  </div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>