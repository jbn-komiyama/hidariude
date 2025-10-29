<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<title>マスタ管理</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf" %>
  <div class="container py-4">
    <div class="d-flex align-items-center justify-content-between mb-4">
      <h1 class="h3 mb-0">マスタ管理</h1>
    </div>

    <c:if test="${not empty errorMsg}">
      <div class="alert alert-danger">${errorMsg}</div>
    </c:if>

    <!-- 業務ランク一覧 -->
    <div class="card shadow-sm mb-4">
      <div class="card-header bg-primary text-white fw-bold">
        業務ランク一覧
      </div>
      <div class="table-responsive">
        <table class="table table-hover table-bordered align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th style="width:72px;" class="text-center">No.</th>
              <th>ランク名</th>
              <th class="text-end">
                顧客側基本単価
                <span class="badge bg-warning text-dark ms-1">税抜</span>
              </th>
              <th class="text-end">
                秘書側基本単価
                <span class="badge bg-info text-dark ms-1">税込</span>
              </th>
              <th style="width:180px;">登録日</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="rank" items="${taskRanks}" varStatus="st">
              <tr>
                <td class="text-center fw-semibold">${st.index + 1}</td>
                <td><c:out value="${rank.rankName}"/></td>
                <td class="text-end">
                  <fmt:formatNumber value="${rank.basePayCustomer}" type="number" maxFractionDigits="0" groupingUsed="true"/>円
                  <small class="text-muted ms-1">(税抜)</small>
                </td>
                <td class="text-end">
                  <fmt:formatNumber value="${rank.basePaySecretary}" type="number" maxFractionDigits="0" groupingUsed="true"/>円
                  <small class="text-muted ms-1">(税込)</small>
                </td>
                <td>
                  <fmt:formatDate value="${rank.createdAt}" pattern="yyyy年MM月dd日 HH:mm"/>
                </td>
              </tr>
            </c:forEach>
            <c:if test="${empty taskRanks}">
              <tr>
                <td colspan="5" class="text-muted text-center py-3">データがありません</td>
              </tr>
            </c:if>
          </tbody>
        </table>
      </div>
      <div class="card-footer text-end small text-muted">
        件数：<span class="fw-semibold"><c:out value="${fn:length(taskRanks)}"/></span>
      </div>
    </div>

    <!-- 秘書ランク一覧 -->
    <div class="card shadow-sm">
      <div class="card-header bg-primary text-white fw-bold">
        秘書ランク一覧
      </div>
      <div class="table-responsive">
        <table class="table table-hover table-bordered align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th style="width:72px;" class="text-center">No.</th>
              <th>ランク名</th>
              <th>説明</th>
              <th class="text-end">
                顧客側増額
                <span class="badge bg-warning text-dark ms-1">税抜</span>
              </th>
              <th class="text-end">
                秘書側増額
                <span class="badge bg-info text-dark ms-1">税込</span>
              </th>
              <th style="width:180px;">登録日</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="rank" items="${secretaryRanks}" varStatus="st">
              <tr>
                <td class="text-center fw-semibold">${st.index + 1}</td>
                <td><c:out value="${rank.rankName}"/></td>
                <td><c:out value="${rank.description}"/></td>
                <td class="text-end">
                  <fmt:formatNumber value="${rank.increaseBasePayCustomer}" type="number" maxFractionDigits="0" groupingUsed="true"/>円
                  <small class="text-muted ms-1">(税抜)</small>
                </td>
                <td class="text-end">
                  <fmt:formatNumber value="${rank.increaseBasePaySecretary}" type="number" maxFractionDigits="0" groupingUsed="true"/>円
                  <small class="text-muted ms-1">(税込)</small>
                </td>
                <td>
                  <fmt:formatDate value="${rank.createdAt}" pattern="yyyy年MM月dd日 HH:mm"/>
                </td>
              </tr>
            </c:forEach>
            <c:if test="${empty secretaryRanks}">
              <tr>
                <td colspan="6" class="text-muted text-center py-3">データがありません</td>
              </tr>
            </c:if>
          </tbody>
        </table>
      </div>
      <div class="card-footer text-end small text-muted">
        件数：<span class="fw-semibold"><c:out value="${fn:length(secretaryRanks)}"/></span>
      </div>
    </div>

  </div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

