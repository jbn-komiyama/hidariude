<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8" />
  <title>PM秘書 アサイン登録（確認）</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
</head>
<body class="bg-light">
<div class="container py-4">
  <h1 class="h4 mb-3">PM秘書 アサイン登録（確認）</h1>

  <c:if test="${not empty errorMsg}">
    <div class="alert alert-danger">
      <ul class="mb-0">
        <c:forEach var="m" items="${errorMsg}">
          <li><c:out value="${m}"/></li>
        </c:forEach>
      </ul>
    </div>
  </c:if>

  <div class="card shadow-sm">
    <div class="card-body">
      <div class="row g-3 mb-2">
        <div class="col-md-6">
          <label class="form-label">顧客</label>
          <div class="form-control-plaintext fw-semibold"><c:out value="${customer.companyName}"/></div>
        </div>
        <div class="col-md-6">
          <label class="form-label">対象月</label>
          <div class="form-control-plaintext"><c:out value="${targetYM}"/></div>
        </div>

        <div class="col-md-6">
          <label class="form-label">PM秘書</label>
          <div class="form-control-plaintext"><c:out value="${secretary.name}"/></div>
        </div>

        <div class="col-md-6">
          <label class="form-label">タスクランク</label>
          <div class="form-control-plaintext"><c:out value="${taskRank.rankName}"/></div>
        </div>

        <div class="col-md-3">
          <label class="form-label">基本単価（顧客）</label>
          <div class="form-control-plaintext">
            <fmt:formatNumber value="${h_basePayCustomer}" pattern="#,##0"/>
          </div>
        </div>
        <div class="col-md-3">
          <label class="form-label">基本単価（秘書）</label>
          <div class="form-control-plaintext">
            <fmt:formatNumber value="${h_basePaySecretary}" pattern="#,##0"/>
          </div>
        </div>

        <div class="col-md-6">
          <label class="form-label">ステータス</label>
          <div class="form-control-plaintext">
            <c:choose>
              <c:when test="${empty status}">未選択</c:when>
              <c:when test="${status == 'draft'}">下書き</c:when>
              <c:when test="${status == 'active'}">有効</c:when>
              <c:when test="${status == 'paused'}">一時停止</c:when>
              <c:otherwise><c:out value="${status}"/></c:otherwise>
            </c:choose>
          </div>
        </div>
      </div>

      <form method="post" action="<%= request.getContextPath() %>/admin/assignment/pm_register_done">
        <!-- 送信用 hidden（サービスの受け取り名に合わせる） -->
        <input type="hidden" name="customerId"       value="${h_customerId}">
        <input type="hidden" name="secretaryId"      value="${h_secretaryId}">
        <input type="hidden" name="taskRankId"       value="${h_taskRankId}">
        <input type="hidden" name="targetYM"  value="${targetYM}">
        <input type="hidden" name="basePayCustomer"  value="${h_basePayCustomer}">
        <input type="hidden" name="basePaySecretary" value="${h_basePaySecretary}">
        <input type="hidden" name="status"           value="${status}">

        <div class="text-end">
          <button type="submit" class="btn btn-primary">登録する</button>
          <button type="button" class="btn btn-secondary" onclick="history.back()">戻る</button>
        </div>
      </form>
    </div>
  </div>

</div>
</body>
</html>