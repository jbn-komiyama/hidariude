<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8" />
  <title>PM秘書 アサイン登録（完了）</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
</head>
<body class="bg-light">
<div class="container py-4">
  <h1 class="h4 mb-3">PM秘書 アサイン登録（完了）</h1>

  <c:if test="${not empty message}">
    <div class="alert alert-success"><c:out value="${message}"/></div>
  </c:if>
  <c:if test="${empty message}">
    <div class="alert alert-success">PMアサインの登録が完了しました。</div>
  </c:if>

  <div class="mt-3">
    <a href="<%= request.getContextPath() %>/admin/assignment" class="btn btn-primary">一覧へ戻る</a>
  </div>
</div>
</body>
</html>
