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
    .th-sub { font-size:.8em; color:#0d6efd; opacity:.9;}
    .cell-addr { white-space:normal; }
  </style>
</head>
<body class="bg-primary bg-opacity-10">
<%@ include file="/WEB-INF/jsp/_parts/customer/navbar.jspf" %>

<div class="container py-4">
  <div class="mb-3">
    <h1 class="h4 mb-1">連絡体制図 兼 第三者委託先一覧</h1>
    <div class="text-muted small"><strong>${yearMonth}</strong> 現在</div>
  </div>

  <!-- ===== 第三者委託先一覧（行＝一次/二次） ===== -->
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
              <th style="width:200px;">実施責任者</th>
              <th style="width:20%;">備考</th>
            </tr>
          </thead>
          <tbody>
            <!-- 一次行（必ず1行、二次列は空にする） -->
            <tr>
              <td class="cell-addr">
                OurDesk株式会社<br/>
                (東京都港区南青山1-15-27 YMビル1階)
              </td>
              <td></td>
              <td>オンサイト</td>
              <td class="text-end">1</td>
              <td>委託契約</td>
              <td>井上 万菜子</td>
              <td></td>
            </tr>

            <!-- 二次行（複数可）。一次列は空にする -->
            <c:set var="secCount" value="${empty secondaries ? 0 : fn:length(secondaries)}" />
            <c:choose>
              <c:when test="${secCount gt 0}">
                <c:forEach var="s" items="${secondaries}">
                  <c:url var="profileUrl" value="/customer/assignment/profile">
                    <c:param name="secretaryName" value="${s.name}" />
                  </c:url>
                  <tr>
                    <td></td>
                    <td class="cell-addr">
                      <a href="${profileUrl}">
                        <c:if test="${not empty s.name}">
                          ${fn:escapeXml(s.name)}<br/>
                        </c:if>
                      </a>
                      <c:if test="${not empty s.address}">
                        （${fn:escapeXml(s.address)}）
                      </c:if>
                    </td>
                    <td>オンサイト</td>
                    <td class="text-end">1</td>
                    <td>委託契約</td>
                    <td>
                      <a href="${profileUrl}"><c:out value="${s.name}" /></a>
                    </td>
                    <td><c:out value="${s.note}" /></td>
                  </tr>
                </c:forEach>
              </c:when>
              <c:otherwise>
                <c:set var="secCount" value="0" />
              </c:otherwise>
            </c:choose>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <!-- 合計（太字）：一次(1) + 二次件数 -->
  <c:set var="totalCount" value="${1 + secCount}" />
  <div class="d-flex justify-content-end mb-3">
    <div class="fw-bold">合計要員数：<span>${totalCount}</span></div>
  </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
