<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>顧客一覧</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <div class="container py-4">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h1 class="h3 mb-0">顧客一覧</h1>
      <!-- 右上アクション（新規作成など）を置く場合はここ -->
    </div>

    <div class="card shadow-sm">
      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-hover table-bordered align-middle mb-0">
            <thead class="table-primary">
              <tr>
                <th style="width:72px;">No.</th>
                <th>会社コード</th>
                <th>会社名</th>
                <th>担当者</th>
                <th>連絡先</th>
                <th style="width:110px;">操作</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="cst" items="${customers}" varStatus="st">
                <tr>
                  <!-- 1 からの連番 -->
                  <td class="text-center fw-semibold">${st.index + 1}</td>

                  <td class="text-nowrap">${cst.companyCode}</td>
                  <td class="text-nowrap">${cst.companyName}</td>
				  <td class="text-nowrap">
				  <c:forEach var="customerContact" items="${cst.customerContacts}" varStatus="st">${customerContact.name}</c:forEach>
				  </td>
                  <td class="text-break">
                    <c:if test="${not empty cst.mail}">
                      <a href="mailto:${cst.mail}">${cst.mail}</a><br/>
                    </c:if>
                  ${cst.phone}<br/>
                    <c:if test="${not empty cst.postalCode}">〒${cst.postalCode}<br/></c:if>
                    ${cst.address1} <c:out value="${cst.address2}"/><br/>
                    <c:out value="${cst.building}"/>
                  </td>

                  <td class="text-center">
                    <div class="d-flex gap-1 justify-content-center">
                      <!-- 編集 -->
                      <form method="post" action="<%= request.getContextPath() %>/admin/customer/edit" class="m-0">
                        <input type="hidden" name="id" value="${cst.id}">
                        <button type="submit" class="btn btn-sm btn-primary">編集</button>
                      </form>
                      <!-- 削除 -->
                      <form method="post" action="<%= request.getContextPath() %>/admin/customer/delete" class="m-0"
                            onsubmit="return confirm('本当に削除しますか？');">
                        <input type="hidden" name="id" value="${cst.id}">
                        <button type="submit" class="btn btn-sm btn-danger">削除</button>
                      </form>
                    </div>
                  </td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 件数フッタ -->
      <div class="card-footer text-end small text-muted">
        件数：<span class="fw-semibold"><c:out value="${fn:length(customers)}"/></span>
      </div>
    </div>
  </div>
</body>
</html>
