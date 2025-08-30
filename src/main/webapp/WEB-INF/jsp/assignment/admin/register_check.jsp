<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8" />
  <title>アサイン登録（確認）</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
</head>
<body class="bg-light">
<div class="container py-4">
  <h1 class="h4 mb-3">アサイン登録（確認）</h1>

  <div class="card shadow-sm">
    <div class="card-body">
      <dl class="row">
        <dt class="col-sm-3">対象月</dt>
        <dd class="col-sm-9"><c:out value="${form_targetYearMonth}"/></dd>

        <dt class="col-sm-3">顧客ID</dt>
        <dd class="col-sm-9"><c:out value="${form_customerId}"/></dd>

        <dt class="col-sm-3">秘書ID</dt>
        <dd class="col-sm-9"><c:out value="${form_secretaryId}"/></dd>

        <dt class="col-sm-3">タスクランクID</dt>
        <dd class="col-sm-9"><c:out value="${form_taskRankId}"/></dd>

        <dt class="col-sm-3">単価（顧客）</dt>
        <dd class="col-sm-9"><fmt:formatNumber value="${form_basePayCustomer}" pattern="#,##0"/></dd>

        <dt class="col-sm-3">単価（秘書）</dt>
        <dd class="col-sm-9"><fmt:formatNumber value="${form_basePaySecretary}" pattern="#,##0"/></dd>

        <dt class="col-sm-3">増額（顧客）</dt>
        <dd class="col-sm-9"><fmt:formatNumber value="${form_increaseBasePayCustomer}" pattern="#,##0"/></dd>

        <dt class="col-sm-3">増額（秘書）</dt>
        <dd class="col-sm-9"><fmt:formatNumber value="${form_increaseBasePaySecretary}" pattern="#,##0"/></dd>

        <dt class="col-sm-3">継続単価（顧客）</dt>
        <dd class="col-sm-9"><fmt:formatNumber value="${form_customerBasedIncentiveForCustomer}" pattern="#,##0"/></dd>

        <dt class="col-sm-3">継続単価（秘書）</dt>
        <dd class="col-sm-9"><fmt:formatNumber value="${form_customerBasedIncentiveForSecretary}" pattern="#,##0"/></dd>

        <dt class="col-sm-3">ステータス</dt>
        <dd class="col-sm-9"><c:out value="${form_status}"/></dd>
      </dl>

      <form method="post" action="${pageContext.request.contextPath}/admin/assignment/register_done" class="text-end">
        <input type="hidden" name="customerId" value="${form_customerId}">
        <input type="hidden" name="secretaryId" value="${form_secretaryId}">
        <input type="hidden" name="taskRankId" value="${form_taskRankId}">
        <input type="hidden" name="targetYearMonth" value="${form_targetYearMonth}">
        <input type="hidden" name="basePayCustomer" value="${form_basePayCustomer}">
        <input type="hidden" name="basePaySecretary" value="${form_basePaySecretary}">
        <input type="hidden" name="increaseBasePayCustomer" value="${form_increaseBasePayCustomer}">
        <input type="hidden" name="increaseBasePaySecretary" value="${form_increaseBasePaySecretary}">
        <input type="hidden" name="customerBasedIncentiveForCustomer" value="${form_customerBasedIncentiveForCustomer}">
        <input type="hidden" name="customerBasedIncentiveForSecretary" value="${form_customerBasedIncentiveForSecretary}">
        <input type="hidden" name="status" value="${form_status}">
        <button type="submit" class="btn btn-primary">登録する</button>
        <a href="javascript:history.back()" class="btn btn-secondary">戻る</a>
      </form>
    </div>
  </div>
</div>
</body>
</html>