<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>

<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <title>秘書詳細</title>
  <!-- Bootstrap 5 -->
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jsp/_parts/admin/navbar.jspf"%>
<div class="container py-4">
  <c:set var="sec" value="${secretary}" />
  <c:set var="ym" value="${yearMonth}" />

  <div class="d-flex align-items-center mb-3">
    <h1 class="h3 me-3 text-primary">秘書詳細</h1>
    <span class="text-muted small">ID: <code><c:out value="${sec.id}" /></code></span>
  </div>

  <!-- ① 秘書情報 -->
  <div class="card border-primary mb-4">
    <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
      <span>① 秘書情報</span>
      <div class="d-flex gap-2">
        <a class="btn btn-sm btn-light" href="${pageContext.request.contextPath}/admin/secretary/edit?id=${sec.id}">
          編集
        </a>
        <a class="btn btn-sm btn-outline-light"
           href="${pageContext.request.contextPath}/admin/secretary/delete?id=${sec.id}"
           onclick="return confirm('削除しますか？この操作は取り消せません。');">削除</a>
      </div>
    </div>
    <div class="card-body bg-white">
      <div class="table-responsive">
        <table class="table table-sm align-middle">
          <tbody>
          <tr>
            <th class="table-primary" style="width:12rem;">秘書コード</th>
            <td><c:out value="${empty sec.secretaryCode ? '-' : sec.secretaryCode}" /></td>
            <th class="table-primary" style="width:8rem;">氏名</th>
            <td><c:out value="${sec.name}" /></td>
            <th class="table-primary" style="width:8rem;">ランク</th>
            <td><c:out value="${sec.secretaryRank != null ? sec.secretaryRank.rankName : '-'}" /></td>
          </tr>
          <tr>
            <th class="table-primary">PM対応</th>
            <td>
              <c:choose>
                <c:when test="${sec.pmSecretary}">
                  <span class="badge text-bg-success">可</span>
                </c:when>
                <c:otherwise>
                  <span class="badge text-bg-secondary">不可</span>
                </c:otherwise>
              </c:choose>
            </td>
            <th class="table-primary">連絡先</th>
            <td colspan="3">
              <div>メール：<a href="mailto:${sec.mail}"><c:out value="${sec.mail}" /></a></div>
              <div>電話：<c:out value="${empty sec.phone ? '-' : sec.phone}" /></div>
            </td>
          </tr>
          <tr>
            <th class="table-primary">住所</th>
            <td colspan="5">
              <c:choose>
                <c:when test="${not empty sec.postalCode or not empty sec.address1 or not empty sec.address2 or not empty sec.building}">
                  〒<c:out value="${empty sec.postalCode ? '-' : sec.postalCode}" />
                  <c:out value="${empty sec.address1 ? '' : ' ' += sec.address1}" />
                  <c:out value="${empty sec.address2 ? '' : ' ' += sec.address2}" />
                  <c:out value="${empty sec.building ? '' : ' ' += sec.building}" />
                </c:when>
                <c:otherwise>-</c:otherwise>
              </c:choose>
            </td>
          </tr>
          <tr>
            <th class="table-primary">登録日</th>
            <td>
              <fmt:formatDate value="${sec.createdAt}" pattern="yyyy-MM-dd HH:mm" />
            </td>
            <th class="table-primary">最終ログイン</th>
            <td colspan="3">
              <c:choose>
                <c:when test="${not empty sec.lastLoginAt}">
                  <fmt:formatDate value="${sec.lastLoginAt}" pattern="yyyy-MM-dd HH:mm" />
                </c:when>
                <c:otherwise>-</c:otherwise>
              </c:choose>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <!-- ② 今月のアサイン情報 -->
  <div class="card border-primary mb-4">
    <div class="card-header bg-primary text-white">
      ② 今月のアサイン情報（<c:out value="${ym}" />）
    </div>
    <div class="card-body bg-white">
      <c:choose>
        <c:when test="${empty assignThisMonth}">
          <div class="text-muted">今月のアサインはありません。</div>
        </c:when>
        <c:otherwise>
          <div class="table-responsive">
            <table class="table table-striped table-hover table-sm align-middle">
              <thead class="table-primary">
              <tr>
                <th>顧客名</th>
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
              <c:forEach var="a" items="${assignThisMonth}">
                <!-- nullを0にしてから合計を作る（JSP ELはElvis演算子をサポートしないため三項演算子で対応） -->
                <c:set var="bpc" value="${a.basePayCustomer ne null ? a.basePayCustomer : 0}" />
                <c:set var="bps" value="${a.basePaySecretary ne null ? a.basePaySecretary : 0}" />
                <c:set var="cic" value="${a.customerBasedIncentiveForCustomer ne null ? a.customerBasedIncentiveForCustomer : 0}" />
                <c:set var="cis" value="${a.customerBasedIncentiveForSecretary ne null ? a.customerBasedIncentiveForSecretary : 0}" />
                <c:set var="ibpc" value="${a.increaseBasePayCustomer ne null ? a.increaseBasePayCustomer : 0}" />
                <c:set var="ibps" value="${a.increaseBasePaySecretary ne null ? a.increaseBasePaySecretary : 0}" />
                <c:set var="custTotal" value="${bpc + cic + ibpc}" />
                <c:set var="secTotal"  value="${bps + cis + ibps}" />

                <tr>
                  <td><c:out value="${a.customerCompanyName}" /></td>
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

                  <td class="text-end">
                    <c:out value="${a.consecutiveMonths ne null ? a.consecutiveMonths : 0}" />
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

  <div class="row g-4">
    <!-- ③ 今までの合計金額 -->
    <div class="col-12 col-lg-6">
      <div class="card border-primary h-100">
        <div class="card-header bg-primary text-white">
          ③ 今までの合計（secretary_monthly_summaries）
        </div>
        <div class="card-body bg-white">
          <div class="table-responsive">
            <table class="table table-sm align-middle">
              <tbody>
              <tr>
                <th class="table-primary" style="width:14rem;">請求合計金額</th>
                <td class="text-end"><fmt:formatNumber value="${totals.totalSecretaryAmount}" pattern="#,##0" /></td>
              </tr>
              <tr>
                <th class="table-primary">件数</th>
                <td class="text-end"><fmt:formatNumber value="${totals.totalTasksCount}" pattern="#,##0" /></td>
              </tr>
              <tr>
                <th class="table-primary">総時間(分)</th>
                <td class="text-end"><fmt:formatNumber value="${totals.totalWorkTime}" pattern="#,##0" /></td>
              </tr>
              </tbody>
            </table>
          </div>
          <div class="text-muted small">※ 集計は secretary_monthly_summaries を参照</div>
        </div>
      </div>
    </div>

    <!-- ⑤ 今月までの1年分の請求情報 -->
    <div class="col-12 col-lg-6">
      <div class="card border-primary h-100">
        <div class="card-header bg-primary text-white">
          ⑤ 今月までの1年分（secretary_monthly_summaries）
        </div>
        <div class="card-body bg-white">
          <c:choose>
            <c:when test="${empty last12}">
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
                  <c:forEach var="m" items="${last12}">
                    <tr>
                      <td class="text-nowrap"><c:out value="${m.targetYearMonth}" /></td>
                      <td class="text-end"><fmt:formatNumber value="${m.totalSecretaryAmount}" pattern="#,##0" /></td>
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

  <!-- ④ 今月までのアサイン情報（最新月→過去月） -->
  <div class="card border-primary my-4">
    <div class="card-header bg-primary text-white">
      ④ 今月までのアサイン情報（最新月→過去月）
    </div>
    <div class="card-body bg-white">
      <c:choose>
        <c:when test="${empty assignUptoMonth}">
          <div class="text-muted">データがありません。</div>
        </c:when>
        <c:otherwise>
          <div class="table-responsive">
            <table class="table table-striped table-hover table-sm align-middle">
              <thead class="table-primary">
              <tr>
                <th>顧客名</th>
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
              <c:forEach var="a" items="${assignUptoMonth}">
                <c:set var="bpc"  value="${a.basePayCustomer ne null ? a.basePayCustomer : 0}" />
                <c:set var="bps"  value="${a.basePaySecretary ne null ? a.basePaySecretary : 0}" />
                <c:set var="cic"  value="${a.customerBasedIncentiveForCustomer ne null ? a.customerBasedIncentiveForCustomer : 0}" />
                <c:set var="cis"  value="${a.customerBasedIncentiveForSecretary ne null ? a.customerBasedIncentiveForSecretary : 0}" />
                <c:set var="ibpc" value="${a.increaseBasePayCustomer ne null ? a.increaseBasePayCustomer : 0}" />
                <c:set var="ibps" value="${a.increaseBasePaySecretary ne null ? a.increaseBasePaySecretary : 0}" />
                <c:set var="custTotal" value="${bpc + cic + ibpc}" />
                <c:set var="secTotal"  value="${bps + cis + ibps}" />

                <tr>
                  <td><c:out value="${a.customerCompanyName}" /></td>
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
