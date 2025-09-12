<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>顧客ホーム</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <div class="container py-4">

    <!-- セッションから取得 -->
    <c:set var="cc" value="${sessionScope.loginUser.customerContact}" />
    <c:set var="customer" value="${sessionScope.loginUser.customer}" />

    <div class="d-flex justify-content-between align-items-center mb-3">
      <div>
        <h1 class="h3 mb-1">顧客ホーム</h1>
        <!-- 表示: 会社名（会社名） 担当者名（担当者）様 -->
        <div class="text-muted">
          <strong>
            <c:out value="${empty customer.companyName ? '—' : customer.companyName}"/>
          </strong>
          　
          <span class="ms-2">
            <strong><c:out value="${empty cc.name ? '—' : cc.name}"/></strong>
           様
          </span>
        </div>
      </div>
      <a href="<%=request.getContextPath()%>/customer/logout" class="btn btn-outline-danger btn-sm">ログアウト</a>
    </div>

    <div class="list-group">
      <a href="<%=request.getContextPath()%>/customer/billing_summary" class="list-group-item list-group-item-action">
        請求サマリー
      </a>
      <a href="<%=request.getContextPath()%>/customer/mypage" class="list-group-item list-group-item-action">
        マイ会社ページ
      </a>
    </div>
  </div>
</body>
</html>
