<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8" />
  <title>アサイン登録</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
</head>
<body class="bg-light">
<div class="container py-4">
  <h1 class="h4 mb-3">アサイン登録</h1>

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
      <form method="post" action="${pageContext.request.contextPath}/admin/assignment/register_check">
        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label">対象月</label>
            <input type="month" class="form-control" name="targetYearMonth" value="${targetYm}" required>
          </div>

          <div class="col-md-6">
            <label class="form-label">顧客</label>
            <div class="form-control-plaintext fw-semibold"><c:out value="${customer.companyName}"/></div>
            <input type="hidden" name="customerId" value="${customer.id}">
          </div>

          <div class="col-md-6">
            <label class="form-label">秘書</label>
            <select class="form-select" name="secretaryId" required>
              <option value="">選択してください</option>
              <c:forEach var="s" items="${secretaries}">
                <option value="${s.id}"><c:out value="${s.name}"/></option>
              </c:forEach>
            </select>
          </div>

          <div class="col-md-6">
            <label class="form-label">タスクランク</label>
            <select class="form-select" name="taskRankId" required>
              <option value="">選択してください</option>
              <c:forEach var="tr" items="${taskRanks}">
                <option value="${tr.id}">
                  <c:out value="${tr.rankName}"/>（
                  顧客:<fmt:formatNumber value="${tr.basePayCustomer}" pattern="#,##0"/> /
                  秘書:<fmt:formatNumber value="${tr.basePaySecretary}" pattern="#,##0"/>）
                </option>
              </c:forEach>
            </select>
          </div>

          <div class="col-md-3">
            <label class="form-label">単価（顧客）</label>
            <input type="number" step="1" min="0" class="form-control" name="basePayCustomer" required>
          </div>
          <div class="col-md-3">
            <label class="form-label">単価（秘書）</label>
            <input type="number" step="1" min="0" class="form-control" name="basePaySecretary" required>
          </div>
          <div class="col-md-3">
            <label class="form-label">増額（顧客）</label>
            <input type="number" step="1" min="0" class="form-control" name="increaseBasePayCustomer" value="0">
          </div>
          <div class="col-md-3">
            <label class="form-label">増額（秘書）</label>
            <input type="number" step="1" min="0" class="form-control" name="increaseBasePaySecretary" value="0">
          </div>

          <div class="col-md-6">
            <label class="form-label">継続単価（顧客）</label>
            <input type="number" step="1" min="0" class="form-control" name="customerBasedIncentiveForCustomer" value="0">
          </div>
          <div class="col-md-6">
            <label class="form-label">継続単価（秘書）</label>
            <input type="number" step="1" min="0" class="form-control" name="customerBasedIncentiveForSecretary" value="0">
          </div>

          <div class="col-md-6">
            <label class="form-label">ステータス（任意）</label>
            <select name="status" class="form-select">
              <option value="">未選択</option>
              <option value="draft">下書き</option>
              <option value="active">有効</option>
              <option value="paused">一時停止</option>
            </select>
          </div>

          <div class="col-12 text-end">
            <button type="submit" class="btn btn-primary">確認へ</button>
            <a href="${pageContext.request.contextPath}/admin/assignment" class="btn btn-secondary">戻る</a>
          </div>
        </div>
      </form>
    </div>
  </div>
</div>
</body>
</html>