<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8" />
  <title>秘書プロフィール</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"/>
  <style>
    .label-col { width: 160px; color:#6c757d; }
    pre { white-space: pre-wrap; word-break: break-word; }
  </style>
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/customer/navbar.jspf" %>

<div class="container py-4">
  <div class="mb-3">
    <h1 class="h4 mb-1">秘書プロフィール</h1>
    <div class="text-muted small">資格・職歴・学歴</div>
  </div>

  <c:choose>
    <c:when test="${empty profile}">
      <div class="alert alert-warning">該当の秘書プロフィールが見つかりませんでした。</div>
    </c:when>
    <c:otherwise>
      <div class="card shadow-sm">
  <div class="card-body">
    <div class="mb-3">
      <div class="h5 mb-0">
        <c:out value="${secretaryName}"/>
        <span class="text-muted small ms-2"><c:out value="${secretaryNameRuby}"/></span>
      </div>
    </div>

    <c:choose>
      <c:when test="${profile == null}">
        <div class="alert alert-warning mb-0">
          該当の秘書プロフィールが見つかりませんでした。
        </div>
      </c:when>
      <c:otherwise>
        <dl class="row">
          <dt class="col-sm-3 label-col">資格保有状況</dt>
          <dd class="col-sm-9"><pre class="mb-0"><c:out value="${profile.qualification}"/></pre></dd>

          <dt class="col-sm-3 label-col">職歴</dt>
          <dd class="col-sm-9"><pre class="mb-0"><c:out value="${profile.workHistory}"/></pre></dd>

          <dt class="col-sm-3 label-col">最終学歴</dt>
          <dd class="col-sm-9"><pre class="mb-0"><c:out value="${profile.academicBackground}"/></pre></dd>
        </dl>
      </c:otherwise>
    </c:choose>
  </div>
</div>
    </c:otherwise>
  </c:choose>

  <div class="mt-3">
    <a href="javascript:history.back()" class="btn btn-outline-secondary btn-sm">戻る</a>
  </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
