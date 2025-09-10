<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>業務一覧（全件）</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-primary bg-opacity-10">

<div class="container py-4">

  <div class="d-flex justify-content-between align-items-center mb-3">
    <div>
      <h1 class="h4 mb-1">業務一覧（全件）</h1>
      <div class="text-muted small">年月：<strong>${yearMonth}</strong></div>
    </div>

    <a href="<%=request.getContextPath()%>/admin/home" class="btn btn-sm btn-outline-secondary">戻る</a>
  </div>

  <!-- タブ（ページ分割） -->
  <c:url var="urlAll" value="/admin/task/list_all">
    <c:param name="yearMonth" value="${yearMonth}"/>
    <c:param name="sec" value="${sec}"/>
    <c:param name="cust" value="${cust}"/>
  </c:url>
  <c:url var="urlUnapp" value="/admin/task/list_unapproved">
    <c:param name="yearMonth" value="${yearMonth}"/>
    <c:param name="sec" value="${sec}"/>
    <c:param name="cust" value="${cust}"/>
  </c:url>
  <c:url var="urlApp" value="/admin/task/list_approved">
    <c:param name="yearMonth" value="${yearMonth}"/>
    <c:param name="sec" value="${sec}"/>
    <c:param name="cust" value="${cust}"/>
  </c:url>
  <c:url var="urlRemand" value="/admin/task/list_remanded">
    <c:param name="yearMonth" value="${yearMonth}"/>
    <c:param name="sec" value="${sec}"/>
    <c:param name="cust" value="${cust}"/>
  </c:url>

  <ul class="nav nav-tabs mb-3">
    <li class="nav-item"><a class="nav-link active" href="${urlAll}">全件</a></li>
    <li class="nav-item"><a class="nav-link" href="${urlUnapp}">未承認</a></li>
    <li class="nav-item"><a class="nav-link" href="${urlApp}">承認済</a></li>
    <li class="nav-item"><a class="nav-link" href="${urlRemand}">差戻</a></li>
  </ul>

  <!-- フィルタ -->
  <form method="get" action="<%=request.getContextPath()%>/admin/task/list_all" class="card card-body shadow-sm mb-3">
    <div class="row g-2 align-items-center">
      <div class="col-auto">
        <input type="month" name="yearMonth" class="form-control form-control-sm" value="${yearMonth}">
      </div>
      <div class="col-auto">
        <input type="text" name="sec" class="form-control form-control-sm" placeholder="秘書名を含む" value="${sec}">
      </div>
      <div class="col-auto">
        <input type="text" name="cust" class="form-control form-control-sm" placeholder="顧客名を含む" value="${cust}">
      </div>
      <div class="col-auto">
        <button type="submit" class="btn btn-sm btn-outline-primary">表示</button>
      </div>
    </div>
  </form>

  <div class="alert alert-info">
    <span class="me-3">件数：<strong>${fn:length(tasks)}</strong></span>
    <span class="me-3">合計稼働：<strong><fmt:formatNumber value="${totalMinute/60}" type="number" maxFractionDigits="0"/></strong> 時間 <strong>${totalMinute%60}</strong> 分</span>
    <span>合計金額：<strong>
      <fmt:formatNumber value="${sumCustomer}" type="number" maxFractionDigits="0" groupingUsed="true"/> /
      <fmt:formatNumber value="${sumSecretary}" type="number" maxFractionDigits="0" groupingUsed="true"/> 円
    </strong></span>
  </div>

  <div class="card shadow-sm">
    <div class="card-header bg-light d-flex justify-content-between align-items-center">
      <span class="fw-semibold">一覧（${yearMonth}）</span>
      <span class="text-muted small">※この画面からの承認はできません。差戻のみ可能です。</span>
    </div>

    <div class="card-body p-0">
      <c:choose>
        <c:when test="${empty tasks}">
          <div class="p-4 text-center text-muted">該当データはありません。</div>
        </c:when>
        <c:otherwise>
          <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
              <thead class="table-secondary">
                <tr>
                	<th style="width: 32px;">#</th>
                  <th>秘書</th>
                  <th>顧客</th>
                  <th style="width:140px;">日付</th>
                  <th style="width:130px;">時間</th>
                  <th style="width:70px;">稼働</th>
                  <th style="width:70px;">ランク</th>
                  <th>内容</th>
                  <th style="width:100px;">単価</th>
                  <th style="width:100px;">報酬</th>
                  <th style="width:100px;">状態</th>
                  <th style="width:80px;">操作</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="t" items="${tasks}" varStatus="st">
                  <tr>
                  	<td>${st.count}</td>
                    <td>${t.assignment.secretaryName}</td>
                    <td>${t.assignment.companyName}</td>
                    <td><fmt:formatDate value="${t.workDate}" pattern="yyyy-MM-dd (E)" timeZone="Asia/Tokyo"/></td>
                    <td>
                      <fmt:formatDate value="${t.startTime}" pattern="HH:mm" timeZone="Asia/Tokyo"/> ～
                      <fmt:formatDate value="${t.endTime}" pattern="HH:mm" timeZone="Asia/Tokyo"/>
                    </td>
                    <td><fmt:formatNumber value="${t.workMinute}" type="number" maxFractionDigits="0"/>分</td>
                    <td><c:out value="${t.assignment.taskRankName != null ? t.assignment.taskRankName : '—'}"/></td>
                    <td>${fn:escapeXml(t.workContent)}</td>
                    <td>
                      <c:choose>
                        <c:when test="${t.hourFeeCustomer ne null}">
                          <fmt:formatNumber value="${t.hourFeeCustomer}" type="number" maxFractionDigits="0" groupingUsed="true"/> 円
                        </c:when><c:otherwise>—</c:otherwise>
                      </c:choose>
                    </td>
                    <td>
                      <c:choose>
                        <c:when test="${t.feeCustomer ne null}">
                          <fmt:formatNumber value="${t.feeCustomer}" type="number" maxFractionDigits="0" groupingUsed="true"/> 円
                        </c:when><c:otherwise>—</c:otherwise>
                      </c:choose>
                    </td>
                    <td>
                      <c:choose>
                      
                       <c:when test="${t.approvedAt eq null and t.hasRemander}">
                          <span class="badge bg-danger text-white" title="${fn:escapeXml(t.remandComment)}">差戻修正</span>
                        </c:when>
                        <c:when test="${t.remandedAt ne null}">
												<span class="badge text-bg-danger">差戻</span>
											</c:when>
                        <c:when test="${t.approvedAt ne null}">
                          <span class="badge text-bg-success">承認済</span>
                        </c:when>
                        <c:otherwise>
                          <span class="badge text-bg-warning">未承認</span>
                        </c:otherwise>
                      </c:choose>
                    </td>
                    <td>
                      <button type="button" class="btn btn-sm btn-outline-danger"
                              data-bs-toggle="modal" data-bs-target="#remandModal"
                              data-task-id="${t.id}">差戻</button>
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

<!-- 差戻モーダル -->
<div class="modal fade" id="remandModal" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog">
    <form method="post" action="<%=request.getContextPath()%>/admin/task/remand_done" class="modal-content">
      <input type="hidden" name="taskId" id="remandTaskId">
      <input type="hidden" name="yearMonth" value="${yearMonth}">
      <input type="hidden" name="sec" value="${sec}">
      <input type="hidden" name="cust" value="${cust}">
      <div class="modal-header">
        <h5 class="modal-title">差戻コメント</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <textarea class="form-control" name="remandComment" rows="5" placeholder="差戻理由を記入してください（相手に通知されます）" required></textarea>
      </div>
      <div class="modal-footer">
        <button type="submit" class="btn btn-danger">差戻する</button>
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">キャンセル</button>
      </div>
    </form>
  </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
document.getElementById('remandModal')?.addEventListener('show.bs.modal', function (event) {
  const btn = event.relatedTarget;
  const taskId = btn?.getAttribute('data-task-id');
  document.getElementById('remandTaskId').value = taskId || '';
});
</script>
</body>
</html>
