<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>管理者ログイン</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
  <div class="container py-5">
    <div class="row justify-content-center">
      <div class="col-md-4">
        <div class="card shadow-sm">
          <div class="card-body">
            <h1 class="h4 mb-3 text-center">管理者ログイン</h1>

            <c:if test="${not empty errorMsg}">
              <div class="alert alert-danger">${errorMsg}</div>
            </c:if>

            <form method="post" action="<%=request.getContextPath()%>/admin/login">
              <div class="mb-3">
                <label class="form-label">メールアドレス</label>
                <input type="email" name="loginId" class="form-control" required>
              </div>
              <div class="mb-3">
                <label class="form-label">パスワード</label>
                <input type="password" name="password" class="form-control" required>
              </div>
              <div class="d-grid">
                <button type="submit" class="btn btn-primary">ログイン</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</body>
</html>