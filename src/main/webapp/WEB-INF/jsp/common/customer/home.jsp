<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <title>ダッシュボード（顧客）</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <style>
    .tile-link{ text-decoration:none; color:inherit; }
    .tile-card{ transition: box-shadow .15s ease; }
    .tile-card:hover{ box-shadow: 0 .5rem 1rem rgba(0,0,0,.15)!important; }
    .tile-title{ font-size:.9rem; color:#6c757d; }
    .tile-value{ font-size:2.25rem; font-weight:700; line-height:1; }
    .badge-gap{ margin-left:.5rem; vertical-align:middle; }
    .row-gap{ row-gap:1rem; }
    .badge-unapproved{ background-color:#FFE3D5; color:#B54708; border:1px solid #FFBF99; border-radius:9999px; font-weight:700; padding:.35rem .6rem; }
  </style>
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/customer/navbar.jspf" %>

<div class="container py-4">
  <div class="d-flex justify-content-between align-items-center mb-3">
    <div>
      <h1 class="h3 mb-1">ダッシュボード</h1>
      <p class="text-muted small mb-0">各月の支払いサマリー</p>
    </div>
  </div>

  <!-- 金額/バッジ用の事前フラグ。total が正の値のときだけ「データあり」 -->
  <c:set var="hasDataPrev3" value="${statPrev3 ne null and statPrev3.total ne null and statPrev3.total gt 0}" />
  <c:set var="hasDataPrev2" value="${statPrev2 ne null and statPrev2.total ne null and statPrev2.total gt 0}" />
  <c:set var="hasDataPrev1" value="${statPrev1 ne null and statPrev1.total ne null and statPrev1.total gt 0}" />
  <c:set var="hasDataNow"   value="${statNow   ne null and statNow.total   ne null and statNow.total   gt 0}" />

  <!-- URL を事前生成（サービスが ym* を設定済み） -->
  <c:url var="urlPrev3" value="/customer/invoice"><c:param name="yearMonth" value="${ymPrev3}"/></c:url>
  <c:url var="urlPrev2" value="/customer/invoice"><c:param name="yearMonth" value="${ymPrev2}"/></c:url>
  <c:url var="urlPrev1" value="/customer/invoice"><c:param name="yearMonth" value="${ymPrev1}"/></c:url>
  <c:url var="urlNow"   value="/customer/invoice"><c:param name="yearMonth" value="${ymNow}"/></c:url>

  <!-- カード4枚-->
  <div class="row row-gap mb-4">

    <!-- 3か月前 -->
    <div class="col-12 col-sm-6 col-lg-3">
      <a class="tile-link" href="${urlPrev3}">
        <div class="card shadow-sm h-100 border-0 tile-card">
          <div class="card-body">
            <div class="tile-title mb-2">
              <c:out value="${m3}"/>月の支払（3か月前）
              <c:choose>
                <c:when test="${not hasDataPrev3}">
                  <span class="badge bg-secondary badge-gap">データなし</span>
                </c:when>
                <c:when test="${statPrev3.unapproved gt 0}">
                  <span class="badge badge-unapproved badge-gap">未承認あり</span>
                </c:when>
                <c:otherwise>
                  <span class="badge bg-success badge-gap">承認済み</span>
                </c:otherwise>
              </c:choose>
            </div>
            <div class="tile-value">
              <c:choose>
                <c:when test="${hasDataPrev3}">
                  <fmt:formatNumber value="${statPrev3.total}" type="number" maxFractionDigits="0" groupingUsed="true"/>
                </c:when>
                <c:otherwise>-</c:otherwise>
              </c:choose>
              <span class="fs-6 fw-normal">円</span>
            </div>
          </div>
        </div>
      </a>
    </div>

    <!-- 2か月前 -->
    <div class="col-12 col-sm-6 col-lg-3">
      <a class="tile-link" href="${urlPrev2}">
        <div class="card shadow-sm h-100 border-0 tile-card">
          <div class="card-body">
            <div class="tile-title mb-2">
              <c:out value="${m2}"/>月の支払（2か月前）
              <c:choose>
                <c:when test="${not hasDataPrev2}">
                  <span class="badge bg-secondary badge-gap">データなし</span>
                </c:when>
                <c:when test="${statPrev2.unapproved gt 0}">
                  <span class="badge badge-unapproved badge-gap">未承認あり</span>
                </c:when>
                <c:otherwise>
                  <span class="badge bg-success badge-gap">承認済み</span>
                </c:otherwise>
              </c:choose>
            </div>
            <div class="tile-value">
              <c:choose>
                <c:when test="${hasDataPrev2}">
                  <fmt:formatNumber value="${statPrev2.total}" type="number" maxFractionDigits="0" groupingUsed="true"/>
                </c:when>
                <c:otherwise>-</c:otherwise>
              </c:choose>
              <span class="fs-6 fw-normal">円</span>
            </div>
          </div>
        </div>
      </a>
    </div>

    <!-- 1か月前 -->
    <div class="col-12 col-sm-6 col-lg-3">
      <a class="tile-link" href="${urlPrev1}">
        <div class="card shadow-sm h-100 border-0 tile-card">
          <div class="card-body">
            <div class="tile-title mb-2">
              <c:out value="${m1}"/>月の支払（1か月前）
              <c:choose>
                <c:when test="${not hasDataPrev1}">
                  <span class="badge bg-secondary badge-gap">データなし</span>
                </c:when>
                <c:when test="${statPrev1.unapproved gt 0}">
                  <span class="badge badge-unapproved badge-gap">未承認あり</span>
                </c:when>
                <c:otherwise>
                  <span class="badge bg-success badge-gap">承認済み</span>
                </c:otherwise>
              </c:choose>
            </div>
            <div class="tile-value">
              <c:choose>
                <c:when test="${hasDataPrev1}">
                  <fmt:formatNumber value="${statPrev1.total}" type="number" maxFractionDigits="0" groupingUsed="true"/>
                </c:when>
                <c:otherwise>-</c:otherwise>
              </c:choose>
              <span class="fs-6 fw-normal">円</span>
            </div>
          </div>
        </div>
      </a>
    </div>

    <!-- 今月 -->
    <div class="col-12 col-sm-6 col-lg-3">
      <a class="tile-link" href="${urlNow}">
        <div class="card shadow-sm h-100 border-0 tile-card">
          <div class="card-body">
            <div class="tile-title mb-2">
              <c:out value="${m0}"/>月の支払（今月）
              <c:choose>
                <c:when test="${not hasDataNow}">
                  <span class="badge bg-secondary badge-gap">データなし</span>
                </c:when>
                <c:when test="${statNow.unapproved gt 0}">
                  <span class="badge badge-unapproved badge-gap">未承認あり</span>
                </c:when>
                <c:otherwise>
                  <span class="badge bg-success badge-gap">承認済み</span>
                </c:otherwise>
              </c:choose>
            </div>
            <div class="tile-value">
              <c:choose>
                <c:when test="${hasDataNow}">
                  <fmt:formatNumber value="${statNow.total}" type="number" maxFractionDigits="0" groupingUsed="true"/>
                </c:when>
                <c:otherwise>-</c:otherwise>
              </c:choose>
              <span class="fs-6 fw-normal">円</span>
            </div>
          </div>
        </div>
      </a>
    </div>
  </div>

  <!-- 直近2か月のアサイン（上余白を追加） -->
  <div class="card shadow-sm mb-4 mt-2">
    <div class="card-header bg-primary text-white">直近2か月のアサイン</div>
    <div class="card-body">
      <c:choose>
        <c:when test="${empty recentAssignments}">
          <p class="text-muted mb-0">直近2か月のアサインはありません。</p>
        </c:when>
        <c:otherwise>
          <div class="table-responsive">
            <table class="table table-sm align-middle mb-0">
              <thead class="text-muted">
                <tr>
                  <th>対象月</th>
                  <th>秘書名</th>
                  <th>タスクランク</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="a" items="${recentAssignments}">
                  <c:url var="profUrl" value="/customer/assignment/profile">
                    <c:param name="secretaryName" value="${a.secretaryName}" />
                  </c:url>
                  <tr>
                    <td><c:out value="${a.targetYearMonth}"/></td>
                    <td>
                      <a href="${profUrl}">
                        <c:out value="${a.secretaryName}"/>
                      </a>
                    </td>
                    <td>
                      <c:out value="${empty a.taskRankName ? '—' : a.taskRankName}"/>
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
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
