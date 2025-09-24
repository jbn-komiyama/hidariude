<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>

<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <title>顧客詳細</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf"%>

<div class="container py-4">
  <c:set var="cust" value="${customer}" />
  <c:set var="ym" value="${yearMonth}" />

  <div class="d-flex align-items-center mb-3">
    <h1 class="h3 me-3 text-primary">顧客詳細</h1>
    <span class="text-muted small">ID: <code><c:out value="${cust.id}" /></code></span>
  </div>

  <!-- ① 顧客情報 -->
  <div class="card border-primary mb-4">
    <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
      <span>① 顧客情報</span>
      <div class="d-flex gap-2">
        <a class="btn btn-sm btn-light" href="${pageContext.request.contextPath}/admin/customer/edit?id=${cust.id}">編集</a>
        <a class="btn btn-sm btn-outline-light"
           href="${pageContext.request.contextPath}/admin/customer/delete?id=${cust.id}"
           onclick="return confirm('削除しますか？この操作は取り消せません。');">削除</a>
      </div>
    </div>
    <div class="card-body bg-white">
      <div class="table-responsive">
        <table class="table table-sm align-middle">
          <tbody>
          <tr>
            <th class="table-primary" style="width:12rem;">顧客コード</th>
            <td><c:out value="${empty cust.companyCode ? '-' : cust.companyCode}" /></td>
            <th class="table-primary" style="width:8rem;">会社名</th>
            <td><c:out value="${cust.companyName}" /></td>
            <th class="table-primary" style="width:8rem;">メール</th>
            <td>
              <c:choose>
                <c:when test="${not empty cust.mail}">
                  <a href="mailto:${cust.mail}"><c:out value="${cust.mail}" /></a>
                </c:when>
                <c:otherwise>-</c:otherwise>
              </c:choose>
            </td>
          </tr>
          <tr>
            <th class="table-primary">電話</th>
            <td><c:out value="${empty cust.phone ? '-' : cust.phone}" /></td>
            <th class="table-primary">住所</th>
            <td colspan="3">
              <c:choose>
                <c:when test="${not empty cust.postalCode or not empty cust.address1 or not empty cust.address2 or not empty cust.building}">
                  〒<c:out value="${empty cust.postalCode ? '-' : cust.postalCode}" />
                  <c:if test="${not empty cust.address1}">&nbsp;<c:out value="${cust.address1}" /></c:if>
                  <c:if test="${not empty cust.address2}">&nbsp;<c:out value="${cust.address2}" /></c:if>
                  <c:if test="${not empty cust.building}">&nbsp;<c:out value="${cust.building}" /></c:if>
                </c:when>
                <c:otherwise>-</c:otherwise>
              </c:choose>
            </td>
          </tr>
          <tr>
            <th class="table-primary">主担当者</th>
            <td>
              <c:set var="__primaryName" value="" />
              <c:forEach var="p" items="${cust.customerContacts}">
                <c:if test="${cust.primaryContactId != null and p.id == cust.primaryContactId}">
                  <c:set var="__primaryName" value="${p.name}" />
                </c:if>
              </c:forEach>
              <c:choose>
                <c:when test="${not empty __primaryName}">
                  <c:out value="${__primaryName}" />
                  <span class="badge text-bg-secondary ms-1">主担当</span>
                </c:when>
                <c:otherwise>-</c:otherwise>
              </c:choose>
            </td>
            <th class="table-primary">登録日</th>
            <td><fmt:formatDate value="${cust.createdAt}" pattern="yyyy-MM-dd HH:mm" /></td>
            <th class="table-primary">最終更新</th>
            <td><fmt:formatDate value="${cust.updatedAt}" pattern="yyyy-MM-dd HH:mm" /></td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <!-- ② 顧客の会社の担当者一覧 -->
  <div class="card border-primary mb-4">
    <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
      <span>② 顧客の会社の担当者一覧</span>
      <a class="btn btn-sm btn-light" href="${pageContext.request.contextPath}/admin/customer_contact/register?customerId=${cust.id}">担当者追加</a>
    </div>
    <div class="card-body bg-white">
      <c:choose>
        <c:when test="${empty cust.customerContacts}">
          <div class="text-muted">担当者は登録されていません。</div>
        </c:when>
        <c:otherwise>
          <div class="table-responsive">
            <table class="table table-striped table-hover table-sm align-middle">
              <thead class="table-primary">
              <tr>
                <th>名前</th>
                <th>名前カナ</th>
                <th>部署名</th>
                <th>連絡先メール</th>
                <th>電話番号</th>
                <th class="text-center">主担当</th>
                <th>最終ログイン</th>
              </tr>
              </thead>
              <tbody>
              <c:forEach var="p" items="${cust.customerContacts}">
                <tr>
                  <td><c:out value="${p.name}" /></td>
                  <td><c:out value="${empty p.nameRuby ? '-' : p.nameRuby}" /></td>
                  <td><c:out value="${empty p.department ? '-' : p.department}" /></td>
                  <td>
                    <c:choose>
                      <c:when test="${not empty p.mail}">
                        <a href="mailto:${p.mail}"><c:out value="${p.mail}" /></a>
                      </c:when>
                      <c:otherwise>-</c:otherwise>
                    </c:choose>
                  </td>
                  <td><c:out value="${empty p.phone ? '-' : p.phone}" /></td>
                  <td class="text-center">
                    <c:if test="${cust.primaryContactId != null and p.id == cust.primaryContactId}">
                      <span class="badge text-bg-success">主</span>
                    </c:if>
                  </td>
                  <td>
                    <c:choose>
                      <c:when test="${not empty p.lastLoginAt}">
                        <fmt:formatDate value="${p.lastLoginAt}" pattern="yyyy-MM-dd HH:mm" />
                      </c:when>
                      <c:otherwise>-</c:otherwise>
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

  <!-- ③ 今月のアサイン情報 -->
  <div class="card border-primary mb-4">
    <div class="card-header bg-primary text-white">
      ③ 今月のアサイン情報（<c:out value="${ym}" />）
    </div>
    <div class="card-body bg-white">
      <c:choose>
        <c:when test="${empty assignmentsThisMonth}">
          <div class="text-muted">今月のアサインはありません。</div>
        </c:when>
        <c:otherwise>
          <div class="table-responsive">
            <table class="table table-striped table-hover table-sm align-middle">
              <thead class="table-primary">
              <tr>
                <th>秘書名</th>
                <th>ランク</th>
                <th>月</th>
                <th class="text-end">基本(顧客)</th>
                <th class="text-end">基本(秘書)</th>
                <th class="text-end">継続(顧客)</th>
                <th class="text-end">継続(秘書)</th>
                <th class="text-end">ランク増(顧客)</th>
                <th class="text-end">ランク増(秘書)</th>
                <th class="text-end">合計(顧客)</th>
                <th class="text-end">合計(秘書)</th>
                <th class="text-end">継続月数</th>
              </tr>
              </thead>
              <tbody>
              <c:forEach var="a" items="${assignmentsThisMonth}">
                <c:set var="bpc"  value="${a.basePayCustomer ne null ? a.basePayCustomer : 0}" />
                <c:set var="bps"  value="${a.basePaySecretary ne null ? a.basePaySecretary : 0}" />
                <c:set var="cic"  value="${a.customerBasedIncentiveForCustomer ne null ? a.customerBasedIncentiveForCustomer : 0}" />
                <c:set var="cis"  value="${a.customerBasedIncentiveForSecretary ne null ? a.customerBasedIncentiveForSecretary : 0}" />
                <c:set var="ibpc" value="${a.increaseBasePayCustomer ne null ? a.increaseBasePayCustomer : 0}" />
                <c:set var="ibps" value="${a.increaseBasePaySecretary ne null ? a.increaseBasePaySecretary : 0}" />
                <c:set var="custTotal" value="${bpc + cic + ibpc}" />
                <c:set var="secTotal"  value="${bps + cis + ibps}" />
                <tr>
                  <td><c:out value="${a.secretaryName}" /></td>
                  <td><c:out value="${empty a.taskRankName ? '-' : a.taskRankName}" /></td>
                  <td class="text-nowrap"><c:out value="${a.targetYearMonth}" /></td>
                  <td class="text-end"><fmt:formatNumber value="${a.basePayCustomer}" pattern="#,##0" /></td>
                  <td class="text-end"><fmt:formatNumber value="${a.basePaySecretary}" pattern="#,##0" /></td>
                  <td class="text-end"><fmt:formatNumber value="${a.customerBasedIncentiveForCustomer}" pattern="#,##0" /></td>
                  <td class="text-end"><fmt:formatNumber value="${a.customerBasedIncentiveForSecretary}" pattern="#,##0" /></td>
                  <td class="text-end"><fmt:formatNumber value="${a.increaseBasePayCustomer}" pattern="#,##0" /></td>
                  <td class="text-end"><fmt:formatNumber value="${a.increaseBasePaySecretary}" pattern="#,##0" /></td>
                  <td class="text-end"><fmt:formatNumber value="${custTotal}" pattern="#,##0" /></td>
                  <td class="text-end"><fmt:formatNumber value="${secTotal}" pattern="#,##0" /></td>
                  <td class="text-end"><c:out value="${a.consecutiveMonths ne null ? a.consecutiveMonths : 0}" /></td>
                </tr>
              </c:forEach>
              </tbody>
            </table>
          </div>
        </c:otherwise>
      </c:choose>
    </div>
  </div>

  <div class="row g-4">
    <!-- ④ 今までの合計金額（customer_monthly_invoices） -->
    <div class="col-12 col-lg-6">
      <div class="card border-primary h-100">
        <div class="card-header bg-primary text-white">④ 今までの合計（customer_monthly_invoices）</div>
        <div class="card-body bg-white">
          <div class="table-responsive">
            <table class="table table-sm align-middle">
              <tbody>
              <tr>
                <th class="table-primary" style="width:14rem;">請求合計金額</th>
                <td class="text-end"><fmt:formatNumber value="${invoiceTotalAmount}" pattern="#,##0" /></td>
              </tr>
              <tr>
                <th class="table-primary">件数</th>
                <td class="text-end"><fmt:formatNumber value="${invoiceTotalCount}" pattern="#,##0" /></td>
              </tr>
              <tr>
                <th class="table-primary">総時間(分)</th>
                <td class="text-end"><fmt:formatNumber value="${invoiceTotalWork}" pattern="#,##0" /></td>
              </tr>
              </tbody>
            </table>
          </div>
          <div class="text-muted small">※ 集計は customer_monthly_invoices を参照</div>
        </div>
      </div>
    </div>

    <!-- ⑥ 今月までの1年分の請求情報（customer_monthly_invoices） -->
    <div class="col-12 col-lg-6">
      <div class="card border-primary h-100">
        <div class="card-header bg-primary text-white">⑥ 今月までの1年分（customer_monthly_invoices）</div>
        <div class="card-body bg-white">
          <c:choose>
            <c:when test="${empty invoicesLast12}">
              <div class="text-muted">データがありません。</div>
            </c:when>
            <c:otherwise>
              <div class="table-responsive">
                <table class="table table-striped table-hover table-sm align-middle">
                  <thead class="table-primary">
                  <tr>
                    <th>年月</th>
                    <th class="text-end">請求合計金額</th>
                    <th class="text-end">件数</th>
                    <th class="text-end">総時間(分)</th>
                  </tr>
                  </thead>
                  <tbody>
                  <c:forEach var="m" items="${invoicesLast12}">
                    <tr>
                      <td class="text-nowrap"><c:out value="${m.targetYearMonth}" /></td>
                      <td class="text-end"><fmt:formatNumber value="${m.totalAmount}" pattern="#,##0" /></td>
                      <td class="text-end"><fmt:formatNumber value="${m.totalTasksCount}" pattern="#,##0" /></td>
                      <td class="text-end"><fmt:formatNumber value="${m.totalWorkTime}" pattern="#,##0" /></td>
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
  </div>

  <!-- ⑤ 今月までのアサイン情報（最新月→過去月） -->
  <div class="card border-primary my-4">
    <div class="card-header bg-primary text-white">⑤ 今月までのアサイン情報（最新月→過去月）</div>
    <div class="card-body bg-white">
      <c:choose>
        <c:when test="${empty assignmentsHistory}">
          <div class="text-muted">データがありません。</div>
        </c:when>
        <c:otherwise>
          <div class="table-responsive">
            <table class="table table-striped table-hover table-sm align-middle">
              <thead class="table-primary">
              <tr>
                <th>秘書名</th>
                <th>ランク</th>
                <th>月</th>
                <th class="text-end">基本(顧客)</th>
                <th class="text-end">基本(秘書)</th>
                <th class="text-end">継続(顧客)</th>
                <th class="text-end">継続(秘書)</th>
                <th class="text-end">ランク増(顧客)</th>
                <th class="text-end">ランク増(秘書)</th>
                <th class="text-end">合計(顧客)</th>
                <th class="text-end">合計(秘書)</th>
              </tr>
              </thead>
              <tbody>
              <c:forEach var="a" items="${assignmentsHistory}">
                <c:set var="bpc"  value="${a.basePayCustomer ne null ? a.basePayCustomer : 0}" />
                <c:set var="bps"  value="${a.basePaySecretary ne null ? a.basePaySecretary : 0}" />
                <c:set var="cic"  value="${a.customerBasedIncentiveForCustomer ne null ? a.customerBasedIncentiveForCustomer : 0}" />
                <c:set var="cis"  value="${a.customerBasedIncentiveForSecretary ne null ? a.customerBasedIncentiveForSecretary : 0}" />
                <c:set var="ibpc" value="${a.increaseBasePayCustomer ne null ? a.increaseBasePayCustomer : 0}" />
                <c:set var="ibps" value="${a.increaseBasePaySecretary ne null ? a.increaseBasePaySecretary : 0}" />
                <c:set var="custTotal" value="${bpc + cic + ibpc}" />
                <c:set var="secTotal"  value="${bps + cis + ibps}" />
                <tr>
                  <td><c:out value="${a.secretaryName}" /></td>
                  <td><c:out value="${empty a.taskRankName ? '-' : a.taskRankName}" /></td>
                  <td class="text-nowrap"><c:out value="${a.targetYearMonth}" /></td>
                  <td class="text-end"><fmt:formatNumber value="${a.basePayCustomer}" pattern="#,##0" /></td>
                  <td class="text-end"><fmt:formatNumber value="${a.basePaySecretary}" pattern="#,##0" /></td>
                  <td class="text-end"><fmt:formatNumber value="${a.customerBasedIncentiveForCustomer}" pattern="#,##0" /></td>
                  <td class="text-end"><fmt:formatNumber value="${a.customerBasedIncentiveForSecretary}" pattern="#,##0" /></td>
                  <td class="text-end"><fmt:formatNumber value="${a.increaseBasePayCustomer}" pattern="#,##0" /></td>
                  <td class="text-end"><fmt:formatNumber value="${a.increaseBasePaySecretary}" pattern="#,##0" /></td>
                  <td class="text-end"><fmt:formatNumber value="${custTotal}" pattern="#,##0" /></td>
                  <td class="text-end"><fmt:formatNumber value="${secTotal}" pattern="#,##0" /></td>
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
