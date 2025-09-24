<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <title>稼働プロフィール 登録</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/secretary/navbar.jspf" %>

<div class="container py-4">
  <div class="d-flex align-items-center justify-content-between mb-3">
    <h1 class="h4 mb-0">稼働プロフィール 登録</h1>
  </div>

  <c:if test="${not empty errorMsg}">
    <div class="alert alert-danger">
      <ul class="mb-0">
        <c:forEach var="m" items="${errorMsg}"><li><c:out value="${m}"/></li></c:forEach>
      </ul>
    </div>
  </c:if>

  <form method="post" action="${pageContext.request.contextPath}/secretary/profile/register_done" class="card p-3 shadow-sm">
    <div class="table-responsive mb-3">
      <table class="table table-bordered align-middle mb-0">
        <thead class="table-primary">
          <tr>
            <th>区分</th><th class="text-center">朝</th><th class="text-center">日中</th><th class="text-center">夜</th><th class="text-center">稼働希望時間</th>
          </tr>
        </thead>
        <tbody>
          <c:set var="wm" value="${weekdayMorning}"/><c:set var="wd" value="${weekdayDaytime}"/><c:set var="wn" value="${weekdayNight}"/>
          <c:set var="sm" value="${saturdayMorning}"/><c:set var="sd" value="${saturdayDaytime}"/><c:set var="sn" value="${saturdayNight}"/>
          <c:set var="um" value="${sundayMorning}"/><c:set var="ud" value="${sundayDaytime}"/><c:set var="un" value="${sundayNight}"/>

          <tr>
            <th>平日</th>
            <td class="text-center">
              <c:set var="nameParam" value="weekdayMorning"/><c:set var="currentValue" value="${wm}"/><%@ include file="/WEB-INF/jsp/_parts/secretary/availability.jspf" %>
            </td>
            <td class="text-center">
              <c:set var="nameParam" value="weekdayDaytime"/><c:set var="currentValue" value="${wd}"/><%@ include file="/WEB-INF/jsp/_parts/secretary/availability.jspf" %>
            </td>
            <td class="text-center">
              <c:set var="nameParam" value="weekdayNight"/><c:set var="currentValue" value="${wn}"/><%@ include file="/WEB-INF/jsp/_parts/secretary/availability.jspf" %>
            </td>
            <td class="text-center">
              <div class="input-group">
                <input type="number" step="0.25" min="0" max="24" name="weekdayWorkHours" class="form-control" value="${weekdayWorkHours}">
                <span class="input-group-text">時間/日</span>
              </div>
            </td>
          </tr>

          <tr>
            <th>土曜</th>
            <td class="text-center">
              <c:set var="nameParam" value="saturdayMorning"/><c:set var="currentValue" value="${sm}"/><%@ include file="/WEB-INF/jsp/_parts/secretary/availability.jspf" %>
            </td>
            <td class="text-center">
              <c:set var="nameParam" value="saturdayDaytime"/><c:set var="currentValue" value="${sd}"/><%@ include file="/WEB-INF/jsp/_parts/secretary/availability.jspf" %>
            </td>
            <td class="text-center">
              <c:set var="nameParam" value="saturdayNight"/><c:set var="currentValue" value="${sn}"/><%@ include file="/WEB-INF/jsp/_parts/secretary/availability.jspf" %>
            </td>
            <td class="text-center">
              <div class="input-group">
                <input type="number" step="0.25" min="0" max="24" name="saturdayWorkHours" class="form-control" value="${saturdayWorkHours}">
                <span class="input-group-text">時間/日</span>
              </div>
            </td>
          </tr>

          <tr>
            <th>日曜</th>
            <td class="text-center">
              <c:set var="nameParam" value="sundayMorning"/><c:set var="currentValue" value="${um}"/><%@ include file="/WEB-INF/jsp/_parts/secretary/availability.jspf" %>
            </td>
            <td class="text-center">
              <c:set var="nameParam" value="sundayDaytime"/><c:set var="currentValue" value="${ud}"/><%@ include file="/WEB-INF/jsp/_parts/secretary/availability.jspf" %>
            </td>
            <td class="text-center">
              <c:set var="nameParam" value="sundayNight"/><c:set var="currentValue" value="${un}"/><%@ include file="/WEB-INF/jsp/_parts/secretary/availability.jspf" %>
            </td>
            <td class="text-center">
              <div class="input-group">
                <input type="number" step="0.25" min="0" max="24" name="sundayWorkHours" class="form-control" value="${sundayWorkHours}">
                <span class="input-group-text">時間/日</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="mb-3">
      <label class="form-label">月の就業時間数</label>
      <div class="input-group">
        <input type="number" step="0.5" min="0" max="744" name="monthlyWorkHours" class="form-control" value="${monthlyWorkHours}">
        <span class="input-group-text">時間/月</span>
      </div>
    </div>

    <div class="row g-3">
      <div class="col-md-6">
        <label class="form-label">備考</label>
        <textarea name="remark" rows="5" class="form-control">${remark}</textarea>
      </div>
      <div class="col-md-6">
        <label class="form-label">資格保持状況</label>
        <textarea name="qualification" rows="5" class="form-control">${qualification}</textarea>
      </div>
      <div class="col-md-6">
        <label class="form-label">職歴</label>
        <textarea name="workHistory" rows="6" class="form-control">${workHistory}</textarea>
      </div>
      <div class="col-md-6">
        <label class="form-label">最終学歴</label>
        <textarea name="academicBackground" rows="6" class="form-control">${academicBackground}</textarea>
      </div>
      <div class="col-12">
        <label class="form-label">自己紹介</label>
        <textarea name="selfIntroduction" rows="5" class="form-control">${selfIntroduction}</textarea>
      </div>
    </div>

    <div class="mt-3 d-flex justify-content-between">
      <a class="btn btn-outline-secondary" href="<%= request.getContextPath() %>/secretary/profile">戻る</a>
      <button class="btn btn-primary" type="submit">登録する</button>
    </div>
  </form>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
