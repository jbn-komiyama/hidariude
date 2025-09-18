<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>引継ぎプレビュー（${fromYM} → ${toYM}）</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf" %>

<div class="container py-4">
  <div class="d-flex justify-content-between align-items-center mb-3">
    <div>
      <h1 class="h4 mb-1">先月のアサインを引き継ぐ</h1>
      <div class="text-muted small">${fromYM} → <span class="fw-semibold">${toYM}</span></div>
    </div>
  </div>

  <div class="card shadow-sm">
    <div class="card-body p-0">
      <c:choose>
        <c:when test="${empty candidates}">
          <div class="p-4 text-muted">引継ぎ候補はありません（または、対象月に既に同一アサインが存在します）。</div>
        </c:when>
        <c:otherwise>
          <form id="coForm" method="post" action="<%=request.getContextPath()%>/admin/assignment/carry_over_apply"
                onsubmit="return confirm('チェック済みのアサインを ${toYM} に登録します。よろしいですか？');">
            <input type="hidden" name="toYM" value="${toYM}">
            <div class="table-responsive">
              <table class="table table-hover table-bordered align-middle mb-0">
                <thead class="table-primary">
                  <tr>
                    <th style="width:44px;" class="text-center">
                      <input type="checkbox" id="chk-all">
                    </th>
                    <th>顧客名</th>
                    <th>秘書名</th>
                    <th>秘書ランク</th>
                    <th>ランク</th>
                    <th class="text-end">基本単価</th>
                    <th class="text-end">秘書単価</th>
                    <th class="text-end">継続単価</th>
                    <th class="text-end">合計単価</th>
                    <th class="text-center" style="width:100px;">継続月数</th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach var="a" items="${candidates}">
                    <tr>
                      <td class="text-center">
                        <input type="checkbox" name="assignmentId" value="${a.assignmentId}" class="chk">
                      </td>
                      <td class="text-nowrap"><c:out value="${a.customerCompanyName}"/></td>
                      <td class="text-nowrap"><c:out value="${a.secretaryName}"/></td>
                      <td class="text-nowrap"><c:out value="${a.secretaryRankName != null ? a.secretaryRankName : '—'}"/></td>
                      <td class="text-nowrap"><c:out value="${a.taskRankName != null ? a.taskRankName : '—'}"/></td>

                      <td class="text-end">
                        <fmt:formatNumber value="${a.basePayCustomer}" pattern="#,##0"/> 
                      </td>
                      <td class="text-end">
                        <fmt:formatNumber value="${a.basePaySecretary}" pattern="#,##0"/>
                      </td>
                      <td class="text-end">
                        <fmt:formatNumber value="${a.customerBasedIncentiveForCustomer}" pattern="#,##0"/> 
                        (<fmt:formatNumber value="${a.customerBasedIncentiveForSecretary}" pattern="#,##0"/>)
                      </td>
                      <td class="text-end">
                        <fmt:formatNumber value="${a.basePayCustomer + a.increaseBasePayCustomer + a.customerBasedIncentiveForCustomer}" pattern="#,##0"/> 
                        (<fmt:formatNumber value="${a.basePaySecretary + a.increaseBasePaySecretary + a.customerBasedIncentiveForSecretary}" pattern="#,##0"/>)
                      </td>
                      <td class="text-center">
                        <c:out value="${contMonths[a.assignmentId]}"/>
                      </td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>

            <div class="card-footer d-flex justify-content-between align-items-center">
              <div class="small text-muted">候補：<span class="fw-semibold"><c:out value="${fn:length(candidates)}"/></span> 件</div>
              <div>
                <button type="submit" class="btn btn-primary">選択を一括登録</button>
              </div>
            </div>
          </form>
        </c:otherwise>
      </c:choose>
    </div>
  </div>
</div>

<script>
document.getElementById('chk-all')?.addEventListener('change', function(){
  document.querySelectorAll('.chk').forEach(function(ch){ ch.checked = event.target.checked; });
});
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
