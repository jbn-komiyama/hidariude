<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8" />
  <title>アカウント編集（確認）</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf" %>

<div class="container py-4">

  <h1 class="h3 mb-3">管理者 マイページ編集（確認）</h1>

  <div class="card shadow-sm">
    <div class="card-body">
      <h2 class="h6 mb-3">入力内容</h2>
      <dl class="row mb-0">
        <dt class="col-sm-3">メールアドレス</dt>
        <dd class="col-sm-9"><c:out value="${form.mail}"/></dd>

        <dt class="col-sm-3">パスワード</dt>
        <dd class="col-sm-9">
          <c:choose>
            <c:when test="${willChangePassword}">（入力あり）</c:when>
            <c:otherwise>（変更なし）</c:otherwise>
          </c:choose>
        </dd>

        <dt class="col-sm-3">氏名</dt>
        <dd class="col-sm-9"><c:out value="${form.name}"/></dd>

        <dt class="col-sm-3">氏名（ふりがな）</dt>
        <dd class="col-sm-9"><c:out value="${form.nameRuby}"/></dd>
      </dl>
    </div>

    <div class="card-body border-top">
      <h2 class="h6 mb-3">参考情報</h2>
      <dl class="row mb-0">
        <dt class="col-sm-3">管理者ID</dt>
        <dd class="col-sm-9"><c:out value="${admin.id}"/></dd>

        <dt class="col-sm-3">作成日時</dt>
        <dd class="col-sm-9">
          <c:choose>
            <c:when test="${not empty admin.createdAt}">
              <fmt:formatDate value="${admin.createdAt}" pattern="yyyy/MM/dd HH:mm" />
            </c:when>
            <c:otherwise>—</c:otherwise>
          </c:choose>
        </dd>

        <dt class="col-sm-3">更新日時</dt>
        <dd class="col-sm-9">
          <c:choose>
            <c:when test="${not empty admin.updatedAt}">
              <fmt:formatDate value="${admin.updatedAt}" pattern="yyyy/MM/dd HH:mm" />
            </c:when>
            <c:otherwise>—</c:otherwise>
          </c:choose>
        </dd>

        <dt class="col-sm-3">最終ログイン</dt>
        <dd class="col-sm-9">
          <c:choose>
            <c:when test="${not empty admin.lastLoginAt}">
              <fmt:formatDate value="${admin.lastLoginAt}" pattern="yyyy/MM/dd HH:mm" />
            </c:when>
            <c:otherwise>—</c:otherwise>
          </c:choose>
        </dd>
      </dl>
    </div>

    <div class="card-footer d-flex justify-content-between">
      <form method="post" action="${pageContext.request.contextPath}/admin/mypage/edit" class="mb-0">
        <input type="hidden" name="mail" value="${fn:escapeXml(form.mail)}" />
        <input type="hidden" name="name" value="${fn:escapeXml(form.name)}" />
        <input type="hidden" name="nameRuby" value="${fn:escapeXml(form.nameRuby)}" />
        <input type="hidden" name="password" value="${fn:escapeXml(form.password)}" />
        <button type="submit" class="btn btn-outline-secondary">修正する</button>
      </form>

      <form method="post" action="${pageContext.request.contextPath}/admin/mypage/edit_done" class="mb-0">
        <input type="hidden" name="mail" value="${fn:escapeXml(form.mail)}" />
        <input type="hidden" name="name" value="${fn:escapeXml(form.name)}" />
        <input type="hidden" name="nameRuby" value="${fn:escapeXml(form.nameRuby)}" />
        <input type="hidden" name="password" value="${fn:escapeXml(form.password)}" />
        <button type="submit" class="btn btn-primary">更新する</button>
      </form>
    </div>
  </div>

</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

