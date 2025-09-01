<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8" />
  <title>アサイン登録（完了）</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
</head>
<body class="bg-light">
<div class="container py-4">
  <div class="alert alert-success">
    登録が完了しました。
  </div>
  <div class="text-end">
    <a href="<%= request.getContextPath() %>/admin/assignment" class="btn btn-primary">一覧へ戻る</a>
  </div>
</div>
</body>
</html>
