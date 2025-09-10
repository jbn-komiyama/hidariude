<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>管理者ホーム</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h1 class="h3 mb-0">管理者ホーム</h1>
      <a href="<%=request.getContextPath()%>/admin/logout" class="btn btn-outline-danger btn-sm">ログアウト</a>
    </div>

    <div class="list-group">
      <a href="<%=request.getContextPath()%>/admin/assignment" class="list-group-item list-group-item-action">
        アサイン一覧
      </a>
      <a href="<%=request.getContextPath()%>/admin/customer" class="list-group-item list-group-item-action">
        顧客一覧
      </a>
      <a href="<%=request.getContextPath()%>/admin/customer/register" class="list-group-item list-group-item-action">
        顧客登録
      </a>
      <a href="<%=request.getContextPath()%>/admin/secretary" class="list-group-item list-group-item-action">
        秘書一覧
      </a>
      <a href="<%=request.getContextPath()%>/admin/secretary/register" class="list-group-item list-group-item-action">
        秘書登録
      </a>
      <a href="<%=request.getContextPath()%>/admin/taskrank" class="list-group-item list-group-item-action">
        タスクランク管理
      </a>
      <a href="<%=request.getContextPath()%>/admin/task/list_all" class="list-group-item list-group-item-action">
        タスク管理
      </a>
    </div>
  </div>
</body>
</html>