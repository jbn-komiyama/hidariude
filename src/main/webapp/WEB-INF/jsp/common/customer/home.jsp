<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn"  uri="jakarta.tags.functions" %>
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
    /* 未承認バッジ（一覧と統一） */
    .badge-unapproved{
      background-color:#FFE3D5; color:#B54708; border:1px solid #FFBF99;
      border-radius:9999px; font-weight:700; padding:.35rem .6rem;
    }
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

  <!-- ===== URL を事前生成（属性：ymPrev3/2/1/Now はサービスで設定） ===== -->
  <c:url var="urlPrev3" value="/customer/invoice">
    <c:param name="yearMonth" value="${ymPrev3}"/>
  </c:url>
  <c:url var="urlPrev2" value="/customer/invoice">
    <c:param name="yearMonth" value="${ymPrev2}"/>
  </c:url>
  <c:url var="urlPrev1" value="/customer/invoice">
    <c:param name="yearMonth" value="${ymPrev1}"/>
  </c:url>
  <c:url var="urlNow" value="/customer/invoice">
    <c:param name="yearMonth" value="${ymNow}"/>
  </c:url>

  <div class="row row-gap">

    <!-- 3か月前 -->
    <div class="col-12 col-sm-6 col-lg-3">
      <a class="tile-link" href="${urlPrev3}">
        <div class="card shadow-sm h-100 border-0 tile-card">
          <div class="card-body">
            <div class="tile-title mb-2">
              <c:out value="${m3}"/>月の支払（3か月前）
              <c:choose>
                <c:when test="${statPrev3 ne null and statPrev3.unapproved gt 0}">
                  <span class="badge badge-unapproved badge-gap">未承認あり</span>
                </c:when>
                <c:when test="${statPrev3 ne null and statPrev3.total ne null}">
                  <span class="badge bg-success badge-gap">承認済み</span>
                </c:when>
                <c:otherwise>
                  <span class="badge bg-secondary badge-gap">データなし</span>
                </c:otherwise>
              </c:choose>
            </div>
            <div class="tile-value">
              <fmt:formatNumber value="${statPrev3 ne null and statPrev3.total ne null ? statPrev3.total : 0}" type="number" maxFractionDigits="0" groupingUsed="true"/>
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
                <c:when test="${statPrev2 ne null and statPrev2.unapproved gt 0}">
                  <span class="badge badge-unapproved badge-gap">未承認あり</span>
                </c:when>
                <c:when test="${statPrev2 ne null and statPrev2.total ne null}">
                  <span class="badge bg-success badge-gap">承認済み</span>
                </c:when>
                <c:otherwise>
                  <span class="badge bg-secondary badge-gap">データなし</span>
                </c:otherwise>
              </c:choose>
            </div>
            <div class="tile-value">
              <fmt:formatNumber value="${statPrev2 ne null and statPrev2.total ne null ? statPrev2.total : 0}" type="number" maxFractionDigits="0" groupingUsed="true"/>
              <span class="fs-6 fw-normal">円</span>
            </div>
          </div>
        </div>
      </a>
    </div>

    <!-- 先月 -->
    <div class="col-12 col-sm-6 col-lg-3">
      <a class="tile-link" href="${urlPrev1}">
        <div class="card shadow-sm h-100 border-0 tile-card">
          <div class="card-body">
            <div class="tile-title mb-2">
              <c:out value="${m1}"/>月の支払（1か月前）
              <c:choose>
                <c:when test="${statPrev1 ne null and statPrev1.unapproved gt 0}">
                  <span class="badge badge-unapproved badge-gap">未承認あり</span>
                </c:when>
                <c:when test="${statPrev1 ne null and statPrev1.total ne null}">
                  <span class="badge bg-success badge-gap">承認済み</span>
                </c:when>
                <c:otherwise>
                  <span class="badge bg-secondary badge-gap">データなし</span>
                </c:otherwise>
              </c:choose>
            </div>
            <div class="tile-value">
              <fmt:formatNumber value="${statPrev1 ne null and statPrev1.total ne null ? statPrev1.total : 0}" type="number" maxFractionDigits="0" groupingUsed="true"/>
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
                <c:when test="${statNow ne null and statNow.unapproved gt 0}">
                  <span class="badge badge-unapproved badge-gap">未承認あり</span>
                </c:when>
                <c:when test="${statNow ne null and statNow.total ne null}">
                  <span class="badge bg-success badge-gap">承認済み</span>
                </c:when>
                <c:otherwise>
                  <span class="badge bg-secondary badge-gap">データなし</span>
                </c:otherwise>
              </c:choose>
            </div>
            <div class="tile-value">
              <fmt:formatNumber value="${statNow ne null and statNow.total ne null ? statNow.total : 0}" type="number" maxFractionDigits="0" groupingUsed="true"/>
              <span class="fs-6 fw-normal">円</span>
            </div>
          </div>
        </div>
      </a>
    </div>

  </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
