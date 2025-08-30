<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>システム管理者 変更完了</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <div class="container py-5">
    <div class="card shadow-sm">
      <div class="card-body text-center">
        <h1 class="h4 mb-3">システム管理者の更新が完了しました。</h1>
        <p class="text-muted mb-4">${message}</p>
        <a href="<%= request.getContextPath() %>/admin/system_admin" class="btn btn-primary">一覧へ</a>
      </div>
    </div>
  </div>
</body>
</html>