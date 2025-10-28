<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <title>エラー</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">

<div class="container py-5">

  <div class="card shadow-sm">
    <div class="card-header bg-danger text-white">
      <h1 class="h5 mb-0">エラーが発生しました</h1>
    </div>
    <div class="card-body">

      <c:choose>
        <!-- 複数エラーメッセージ（List） -->
        <c:when test="${not empty errorMsg and fn:length(errorMsg) > 0 and not empty errorMsg[0]}">
          <ul class="mb-0">
            <c:forEach var="msg" items="${errorMsg}">
              <li><c:out value="${msg}"/></li>
            </c:forEach>
          </ul>
        </c:when>
        <!-- 単一エラーメッセージ（String） -->
        <c:when test="${not empty errorMsg}">
          <p class="mb-0"><c:out value="${errorMsg}"/></p>
        </c:when>
        <c:otherwise>
          <p class="mb-0 text-muted">不明なエラーが発生しました。</p>
        </c:otherwise>
      </c:choose>

    </div>
    <div class="card-footer text-end">
      <a href="<%=request.getContextPath()%>/customer/home" class="btn btn-sm btn-outline-secondary">ホームに戻る</a>
    </div>
  </div>

</div>

</body>
</html>
