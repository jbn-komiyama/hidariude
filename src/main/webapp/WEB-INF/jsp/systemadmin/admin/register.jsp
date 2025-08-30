<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>システム管理者 新規登録</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <div class="container py-4">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h1 class="h3 mb-0">システム管理者 新規登録</h1>
      <a href="<%= request.getContextPath() %>/admin/system_admin" class="btn btn-outline-secondary btn-sm">一覧へ戻る</a>
    </div>

    <c:if test="${not empty errorMsg}">
      <div class="alert alert-danger" role="alert">${errorMsg}</div>
    </c:if>

    <div class="card shadow-sm">
      <div class="card-body">
        <form method="post" action="<%= request.getContextPath() %>/admin/system_admin/register_check" class="row g-3">
          <div class="col-md-6">
            <label class="form-label">氏名 <span class="text-danger">*</span></label>
            <input type="text" name="name" class="form-control" value="${name}" required>
          </div>
          <div class="col-md-6">
            <label class="form-label">ふりがな</label>
            <input type="text" name="nameRuby" class="form-control" value="${nameRuby}">
          </div>
          <div class="col-md-6">
            <label class="form-label">メール <span class="text-danger">*</span></label>
            <input type="email" name="mail" class="form-control" value="${mail}" required>
          </div>
          <div class="col-md-6">
            <label class="form-label">初期パスワード <span class="text-danger">*</span></label>
            <input type="text" name="password" class="form-control" value="${password}" required>
          </div>
          <div class="col-12 text-end">
            <button type="submit" class="btn btn-primary">確認へ</button>
          </div>
        </form>
      </div>
    </div>

  </div>
</body>
</html>