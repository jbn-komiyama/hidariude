<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>秘書ホーム</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h1 class="h3 mb-0">秘書ホーム</h1>
      <a href="<%=request.getContextPath()%>/secretary/logout" class="btn btn-outline-danger btn-sm">ログアウト</a>
    </div>

    <div class="list-group">
      <a href="<%=request.getContextPath()%>/secretary/assignments" class="list-group-item list-group-item-action">
        担当アサイン一覧
      </a>
      <a href="<%=request.getContextPath()%>/secretary/profile" class="list-group-item list-group-item-action">
        プロフィール編集
      </a>
    </div>
  </div>
</body>
</html>