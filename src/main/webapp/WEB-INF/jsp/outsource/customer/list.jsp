<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8" />
  <title>連絡体制図 兼 第三者委託先一覧</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"/>
  <style>
    .label-col { width: 160px; color:#6c757d; }
    .value-col { word-break: break-word; }
    .th-wrap { line-height:1.1; }
    .th-sub { font-size:.8em; color:#0d6efd; opacity:.9; }
    .cell-addr { white-space:normal; }
  </style>
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/customer/navbar.jspf" %>

<div class="container py-4">
  <div class="mb-3">
    <h1 class="h4 mb-1">連絡体制図 兼 第三者委託先一覧</h1>
    <div class="text-muted small">対象年月：<strong>${yearMonth}</strong></div>
  </div>

  <!-- 基本情報（省略可） -->

  <div class="card shadow-sm mb-2">
    <div class="card-header bg-primary text-white"><span class="fw-semibold">第三者委託先一覧</span></div>
    <div class="card-body p-0">
      <div class="table-responsive">
        <table class="table table-sm table-hover table-bordered align-middle mb-0">
          <thead class="table-primary">
            <tr>
              <th style="width:22%;">一次委託</th>
              <th style="width:26%;">二次委託</th>
              <th style="width:110px;">作業場所</th>
              <th style="width:80px;">要員数</th>
              <th style="width:110px;">契約形態</th>
              <th style="width:200px;">
                <div class="th-wrap">実施責任者<br><span class="th-sub">サブ実施責任者（任意）</span></div>
              </th>
              <th style="width:20%;">備考</th>
            </tr>
          </thead>
          <tbody>
            <!-- データ有無判定 -->
            <c:set var="hasData" value="${not empty secondaries
                 or not empty secondaryPersonName or not empty secondaryAddress
                 or not empty secondaryResponsible or not empty secondarySubResponsible
                 or not empty secondaryNote}" />
            <c:set var="secCount" value="${empty secondaries ? 0 : fn:length(secondaries)}" />
            <c:set var="rowSpan" value="${secCount gt 0 ? secCount : 1}" />

            <c:choose>
              <c:when test="${hasData}">
                <!-- 1行目 -->
                <tr>
                  <!-- 一次（rowspan） -->
                  <td rowspan="${secCount gt 0 ? secCount : 1}" class="cell-addr">
                    <div class="fw-semibold">
                      <c:out value="${empty primaryCompanyName ? 'OurDesk株式会社' : primaryCompanyName}" />
                    </div>
                    <c:if test="${not empty primaryCompanyAddress}">（${fn:escapeXml(primaryCompanyAddress)}）</c:if>
                  </td>

                  <!-- 二次（1件目 or フォールバック1件） -->
                  <td class="cell-addr">
                    <c:choose>
                      <c:when test="${secCount gt 0}">
                        <c:set var="s" value="${secondaries[0]}" />
                        <c:if test="${not empty s.name}">${fn:escapeXml(s.name)}<br/></c:if>
                        <c:if test="${not empty s.address}">（${fn:escapeXml(s.address)}）</c:if>
                      </c:when>
                      <c:otherwise>
                        <c:if test="${not empty secondaryPersonName}">${fn:escapeXml(secondaryPersonName)}<br/></c:if>
                        <c:if test="${not empty secondaryAddress}">（${fn:escapeXml(secondaryAddress)}）</c:if>
                      </c:otherwise>
                    </c:choose>
                  </td>

                  <!-- 固定表示 -->
                  <td>オンサイト</td>
                  <td class="text-end">1</td>
                  <td>委託契約</td>

                  <!-- 実施責任者 -->
                  <td>
                    <div>
                      <c:choose>
                        <c:when test="${secCount gt 0}"><c:out value="${secondaries[0].responsible}" /></c:when>
                        <c:otherwise><c:out value="${secondaryResponsible}" /></c:otherwise>
                      </c:choose>
                    </div>
                    <div class="text-muted small">
                      <c:choose>
                        <c:when test="${secCount gt 0}"><c:out value="${secondaries[0].subResponsible}" /></c:when>
                        <c:otherwise><c:out value="${secondarySubResponsible}" /></c:otherwise>
                      </c:choose>
                    </div>
                  </td>

                  <!-- 備考 -->
                  <td>
                    <c:choose>
                      <c:when test="${secCount gt 0}"><c:out value="${secondaries[0].note}" /></c:when>
                      <c:otherwise><c:out value="${secondaryNote}" /></c:otherwise>
                    </c:choose>
                  </td>
                </tr>

                <!-- 2件目以降 -->
                <c:if test="${secCount gt 1}">
                  <c:forEach var="i" begin="1" end="${secCount - 1}">
                    <c:set var="s" value="${secondaries[i]}" />
                    <tr>
                      <td class="cell-addr">
                        <c:if test="${not empty s.name}">${fn:escapeXml(s.name)}<br/></c:if>
                        <c:if test="${not empty s.address}">（${fn:escapeXml(s.address)}）</c:if>
                      </td>
                      <td>オンサイト</td>
                      <td class="text-end">1</td>
                      <td>委託契約</td>
                      <td>
                        <div><c:out value="${s.responsible}" /></div>
                        <div class="text-muted small"><c:out value="${s.subResponsible}" /></div>
                      </td>
                      <td><c:out value="${s.note}" /></td>
                    </tr>
                  </c:forEach>
                </c:if>
              </c:when>

              <c:otherwise>
                <tr>
                  <td class="text-center text-muted" colspan="7">表示できる委託先データがありません。</td>
                </tr>
              </c:otherwise>
            </c:choose>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <!-- 合計（太字） -->
  <c:set var="totalCount"
         value="${hasData ? (secCount gt 0 ? secCount : 1) : 0}" />
  <div class="d-flex justify-content-end mb-3">
    <div class="fw-bold">合計：<span>${totalCount}</span></div>
  </div>

  <!-- 備考（全体） -->
  <c:if test="${not empty note}">
    <div class="alert alert-secondary">
      <div class="fw-semibold mb-1">備考</div>
      <div class="small">${fn:escapeXml(note)}</div>
    </div>
  </c:if>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
