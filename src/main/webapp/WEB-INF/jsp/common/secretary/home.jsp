<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>秘書ホーム</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
  <div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h1 class="h3 mb-0">秘書ホーム</h1>
      <a href="<%=request.getContextPath()%>/secretary/logout" class="btn btn-outline-danger btn-sm">ログアウト</a>
    </div>

    <div class="alert alert-info mb-3">
      表示中の年月: <strong>${yearMonth}</strong>
    </div>

    <h2 class="h5 text-secondary mb-3">現在アサイン中の案件</h2>

    <c:forEach var="c" items="${customers}">
      <div class="card mb-4 shadow-sm">
        <div class="card-header bg-light">
          <div class="d-flex justify-content-between align-items-center">
            <h5 class="mb-0">${fn:escapeXml(c.companyName)}</h5>
            <form method="get" action="<%=request.getContextPath()%>/secretary/task/list" class="m-0">
              <input type="hidden" name="companyId" value="${c.id}" />
              <input type="hidden" name="companyName" value="${c.companyName}" />
              <input type="hidden" name="yearMonth" value="${yearMonth}" />
              <button type="submit" class="btn btn-sm btn-primary">業務登録</button>
            </form>
          </div>
        </div>

        <div class="card-body p-0">
          <c:choose>
            <c:when test="${empty c.assignments}">
              <div class="p-3 text-muted">この顧客には今月のアサインはありません。</div>
            </c:when>
            <c:otherwise>
              <div class="table-responsive">
                <table class="table table-sm mb-0">
                  <thead class="table-secondary">
                    <tr>
                      <th>タスクランク</th>
                      <th>単価</th>
                      <th>増額（秘書ランク）</th>
                      <th>増額（継続）</th>
                    </tr>
                  </thead>
                  <tbody>
                    <c:forEach var="a" items="${c.assignments}">
                      <tr>
                        <td>${fn:escapeXml(a.taskRankName)}</td>
                        <td>
                          <c:choose>
                            <c:when test="${a.basePaySecretary ne null}">
                              時間単価
                              <fmt:formatNumber value="${a.basePaySecretary}" type="number" maxFractionDigits="0" groupingUsed="true"/>
                              円
                            </c:when>
                            <c:otherwise>—</c:otherwise>
                          </c:choose>
                        </td>
                        <td>
                          <c:choose>
                            <c:when test="${a.increaseBasePaySecretary ne null}">
                              <fmt:formatNumber value="${a.increaseBasePaySecretary}" type="number" maxFractionDigits="0" groupingUsed="true"/>円
                            </c:when>
                            <c:otherwise>—</c:otherwise>
                          </c:choose>
                        </td>
                        <td>
                          <c:choose>
                            <c:when test="${a.customerBasedIncentiveForSecretary ne null}">
                              <fmt:formatNumber value="${a.customerBasedIncentiveForSecretary}" type="number" maxFractionDigits="0" groupingUsed="true"/>円
                            </c:when>
                            <c:otherwise>—</c:otherwise>
                          </c:choose>
                        </td>
                      </tr>
                    </c:forEach>
                  </tbody>
                </table>
              </div>
            </c:otherwise>
          </c:choose>
        </div>
      </div>
    </c:forEach>

    <div class="list-group">
      <a href="<%=request.getContextPath()%>/secretary/assignments" class="list-group-item list-group-item-action">
        担当アサイン一覧
      </a>
      <a href="<%=request.getContextPath()%>/secretary/profile" class="list-group-item list-group-item-action">
        プロフィール編集
      </a>
    </div>
  </div>
</body>
</html>
