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
    pre { white-space: pre-wrap; word-break: break-word; }
  </style>
</head>
<body class="bg-primary bg-opacity-10"><!-- 青系背景で統一 -->
<%@ include file="/WEB-INF/jsp/_parts/customer/navbar.jspf" %>

<div class="container py-4">

  <!-- タイトル -->
  <div class="d-flex align-items-center justify-content-between mb-3">
    <h1 class="h4 mb-0">秘書プロフィール</h1>
  </div>

  <!-- プロフィールカード -->
  <div class="card shadow-sm mb-4">
    <div class="card-header bg-primary text-white">プロフィール</div>
    <div class="card-body">

      <!-- 氏名 -->
      <div class="mb-3">
        <div class="h5 mb-1">
          <c:out value="${empty secretaryName ? '—' : secretaryName}"/>
          <c:if test="${not empty secretaryNameRuby}">
            <span class="text-muted small ms-2">（<c:out value="${secretaryNameRuby}"/>）</span>
          </c:if>
        </div>
      </div>
      <div class="mb-4"></div>
      <!-- 明細テーブル：プロフィール未登録でも空ページを表示（—を出す） -->
      <div class="table-responsive">
        <table class="table table-sm align-middle mb-0">
          <tbody>
            <tr>
              <th class="text-secondary w-25">氏名（ふりがな）</th>
              <td>
                <c:out value="${empty secretaryName ? '—' : secretaryName}"/>
                <c:if test="${not empty secretaryNameRuby}">（<c:out value="${secretaryNameRuby}"/>）</c:if>
              </td>
            </tr>
            <tr>
              <th class="text-secondary w-25">資格保有状況</th>
              <td><pre class="mb-0"><c:out value="${empty profile.qualification ? '—' : profile.qualification}"/></pre></td>
            </tr>
            <tr>
              <th class="text-secondary w-25">職歴</th>
              <td><pre class="mb-0"><c:out value="${empty profile.workHistory ? '—' : profile.workHistory}"/></pre></td>
            </tr>
            <tr>
              <th class="text-secondary w-25">最終学歴</th>
              <td><pre class="mb-0"><c:out value="${empty profile.academicBackground ? '—' : profile.academicBackground}"/></pre></td>
            </tr>
            <tr>
              <th class="text-secondary w-25">自己紹介</th>
              <td><pre class="mb-0"><c:out value="${empty profile.selfIntroduction ? '—' : profile.selfIntroduction}"/></pre></td>
            </tr>
            <tr>
              <th class="text-secondary w-25">備考</th>
              <td><pre class="mb-0"><c:out value="${empty profile.remark ? '—' : profile.remark}"/></pre></td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- プロフィール未登録の補足メッセージ（任意表示） -->
      <c:if test="${empty profile}">
        <div class="text-muted small mt-2">
          プロフィールは未登録です。未登録内容は “—” と表示されています。
        </div>
      </c:if>

    </div>
  </div>

  <div class="mt-3 text-end">
    <a href="javascript:history.back()" class="btn btn-outline-secondary">戻る</a>
  </div>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
