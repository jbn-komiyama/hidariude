<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>システム管理者 編集</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <div class="container py-4">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h1 class="h3 mb-0">システム管理者 編集</h1>
      <a href="<%= request.getContextPath() %>/admin/system_admin" class="btn btn-outline-secondary btn-sm">一覧へ戻る</a>
    </div>

    <c:if test="${not empty errorMsg}">
      <div class="alert alert-danger" role="alert">${errorMsg}</div>
    </c:if>

    <div class="card shadow-sm">
      <div class="card-body">
        <form method="post" action="<%= request.getContextPath() %>/admin/system_admin/edit_check" class="row g-3">
          <input type="hidden" name="id" value="${id != null ? id : admin.id}">
          <div class="col-md-6">
            <label class="form-label">氏名 <span class="text-danger">*</span></label>
            <input type="text" name="name" class="form-control" value="${name != null ? name : admin.name}" required>
          </div>
          <div class="col-md-6">
            <label class="form-label">ふりがな</label>
            <input type="text" name="nameRuby" class="form-control" value="${nameRuby != null ? nameRuby : admin.nameRuby}">
          </div>
          <div class="col-md-6">
            <label class="form-label">メール <span class="text-danger">*</span></label>
            <input type="email" name="mail" class="form-control" value="${mail != null ? mail : admin.mail}" required>
          </div>
          <div class="col-md-6">
            <label class="form-label">パスワード</label>
            <input type="text" name="password" class="form-control" value="${password}">
            <div class="form-text">未入力の場合は変更しません。</div>
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