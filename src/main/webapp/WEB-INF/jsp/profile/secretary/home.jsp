<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <title>プロフィール</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/secretary/navbar.jspf" %>

<div class="container py-4">
  <div class="d-flex align-items-center justify-content-between mb-3">
    <h1 class="h4 mb-0">プロフィール</h1>
    <div>
      <c:choose>
        <c:when test="${empty profile}">
          <a href="${pageContext.request.contextPath}/secretary/profile/register" class="btn btn-primary btn-sm">新規登録</a>
        </c:when>
        <c:otherwise>
          <a href="${pageContext.request.contextPath}/secretary/profile/edit" class="btn btn-primary btn-sm">変更</a>
        </c:otherwise>
      </c:choose>
    </div>
  </div>

  <c:if test="${not empty errorMsg}">
    <div class="alert alert-danger"><c:out value="${errorMsg}"/></div>
  </c:if>

  <c:choose>
    <c:when test="${empty profile}">
      <div class="alert alert-info">プロフィールは未登録です。「新規登録」から作成してください。</div>
    </c:when>
    <c:otherwise>

      <!-- 稼働可否 -->
      <div class="card border-primary mb-3">
        <div class="card-header bg-primary text-white">稼働可否</div>
        <div class="card-body">
          <div class="table-responsive">
            <table class="table table-bordered align-middle mb-0">
              <thead class="table-primary">
                <tr>
                  <th>区分</th><th class="text-center">朝</th><th class="text-center">日中</th><th class="text-center">夜</th><th class="text-center">稼働希望時間</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <th>平日</th>
                  <td class="text-center">${profile.weekdayMorning==2?'〇':(profile.weekdayMorning==1?'△':'×')}</td>
                  <td class="text-center">${profile.weekdayDaytime==2?'〇':(profile.weekdayDaytime==1?'△':'×')}</td>
                  <td class="text-center">${profile.weekdayNight==2?'〇':(profile.weekdayNight==1?'△':'×')}</td>
                  <td class="text-center"><c:out value="${profile.weekdayWorkHours}"/> 時間/日</td>
                </tr>
                <tr>
                  <th>土曜</th>
                  <td class="text-center">${profile.saturdayMorning==2?'〇':(profile.saturdayMorning==1?'△':'×')}</td>
                  <td class="text-center">${profile.saturdayDaytime==2?'〇':(profile.saturdayDaytime==1?'△':'×')}</td>
                  <td class="text-center">${profile.saturdayNight==2?'〇':(profile.saturdayNight==1?'△':'×')}</td>
                  <td class="text-center"><c:out value="${profile.saturdayWorkHours}"/> 時間/日</td>
                </tr>
                <tr>
                  <th>日曜</th>
                  <td class="text-center">${profile.sundayMorning==2?'〇':(profile.sundayMorning==1?'△':'×')}</td>
                  <td class="text-center">${profile.sundayDaytime==2?'〇':(profile.sundayDaytime==1?'△':'×')}</td>
                  <td class="text-center">${profile.sundayNight==2?'〇':(profile.sundayNight==1?'△':'×')}</td>
                  <td class="text-center"><c:out value="${profile.sundayWorkHours}"/> 時間/日</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="card-footer text-end">月の就業希望時間：<span class="fw-semibold"><c:out value="${profile.monthlyWorkHours}"/></span> 時間</div>
      </div>

      <!-- 自由記述 -->
      <div class="row g-3">
        <div class="col-md-6">
          <div class="card h-100">
            <div class="card-header bg-primary text-white">備考</div>
            <div class="card-body"><pre class="mb-0"><c:out value="${profile.remark}"/></pre></div>
          </div>
        </div>
        <div class="col-md-6">
          <div class="card h-100">
            <div class="card-header bg-primary text-white">資格保持状況</div>
            <div class="card-body"><pre class="mb-0"><c:out value="${profile.qualification}"/></pre></div>
          </div>
        </div>
        <div class="col-md-6">
          <div class="card h-100">
            <div class="card-header bg-primary text-white">職歴</div>
            <div class="card-body"><pre class="mb-0"><c:out value="${profile.workHistory}"/></pre></div>
          </div>
        </div>
        <div class="col-md-6">
          <div class="card h-100">
            <div class="card-header bg-primary text-white">最終学歴</div>
            <div class="card-body"><pre class="mb-0"><c:out value="${profile.academicBackground}"/></pre></div>
          </div>
        </div>
        <div class="col-12">
          <div class="card">
            <div class="card-header bg-primary text-white">自己紹介</div>
            <div class="card-body"><pre class="mb-0"><c:out value="${profile.selfIntroduction}"/></pre></div>
          </div>
        </div>
      </div>

    </c:otherwise>
  </c:choose>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
