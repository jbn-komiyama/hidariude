<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>秘書 マイページ編集（確認）</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jsp/_parts/secretary/navbar.jspf" %>

  <div class="container py-4">
    <h1 class="h3 mb-3">秘書 マイページ編集（確認）</h1>

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
      <dl class="row mb-0">
        <dt class="col-sm-3">パスワード</dt>
        <dd class="col-sm-9">
          <c:choose>
            <c:when test="${not empty param.password or not empty password}">（入力あり）</c:when>
            <c:otherwise>（変更なし）</c:otherwise>
          </c:choose>
        </dd>

        <dt class="col-sm-3">氏名</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.name ? param.name : name}"/></dd>

        <dt class="col-sm-3">氏名（ふりがな）</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.nameRuby ? param.nameRuby : nameRuby}"/></dd>

		<dt class="col-sm-3">ランク</dt>
        <dd class="col-sm-9">
          <c:out value="${secretary.secretaryRank != null ? secretary.secretaryRank.rankName : '—'}"/>

        <dt class="col-sm-3">PM対応</dt>
        <dd class="col-sm-9">
          <c:choose>
            <c:when test="${secretary != null && secretary.pmSecretary}">可</c:when>
            <c:otherwise>不可</c:otherwise>
          </c:choose>
        </dd>
        
        <dt class="col-sm-3">メール</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.mail ? param.mail : mail}"/></dd>

        <dt class="col-sm-3">電話番号</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.phone ? param.phone : phone}"/></dd>

        <dt class="col-sm-3">郵便番号</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.postalCode ? param.postalCode : postalCode}"/></dd>

        <dt class="col-sm-3">住所</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.address1 ? param.address1 : address1}"/></dd>

        <dt class="col-sm-3">住所2</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.address2 ? param.address2 : address2}"/></dd>

        <dt class="col-sm-3">建物名</dt>
        <dd class="col-sm-9"><c:out value="${not empty param.building ? param.building : building}"/></dd>
      </dl>
    </div>

    <div class="card-footer d-flex justify-content-between">
      <!-- 修正へ戻る（入力値を持ち回り） -->
      <form method="post" action="${pageContext.request.contextPath}/secretary/mypage/edit" class="mb-0">
        <input type="hidden" name="password"   value="${not empty param.password ? param.password : password}"/>
        <input type="hidden" name="name"       value="${not empty param.name ? param.name : name}"/>
        <input type="hidden" name="nameRuby"   value="${not empty param.nameRuby ? param.nameRuby : nameRuby}"/>
        <input type="hidden" name="mail"       value="${not empty param.mail ? param.mail : mail}"/>
        <input type="hidden" name="phone"      value="${not empty param.phone ? param.phone : phone}"/>
        <input type="hidden" name="postalCode" value="${not empty param.postalCode ? param.postalCode : postalCode}"/>
        <input type="hidden" name="address1"   value="${not empty param.address1 ? param.address1 : address1}"/>
        <input type="hidden" name="address2"   value="${not empty param.address2 ? param.address2 : address2}"/>
        <input type="hidden" name="building"   value="${not empty param.building ? param.building : building}"/>
        <button type="submit" class="btn btn-outline-secondary">修正する</button>
      </form>

      <!-- 確定 -->
      <form method="post" action="${pageContext.request.contextPath}/secretary/mypage/edit_done" class="mb-0">
        <input type="hidden" name="password"   value="${not empty param.password ? param.password : password}"/>
        <input type="hidden" name="name"       value="${not empty param.name ? param.name : name}"/>
        <input type="hidden" name="nameRuby"   value="${not empty param.nameRuby ? param.nameRuby : nameRuby}"/>
        <input type="hidden" name="mail"       value="${not empty param.mail ? param.mail : mail}"/>
        <input type="hidden" name="phone"      value="${not empty param.phone ? param.phone : phone}"/>
        <input type="hidden" name="postalCode" value="${not empty param.postalCode ? param.postalCode : postalCode}"/>
        <input type="hidden" name="address1"   value="${not empty param.address1 ? param.address1 : address1}"/>
        <input type="hidden" name="address2"   value="${not empty param.address2 ? param.address2 : address2}"/>
        <input type="hidden" name="building"   value="${not empty param.building ? param.building : building}"/>
        <button type="submit" class="btn btn-primary">更新する</button>
      </form>
    </div>
  </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>