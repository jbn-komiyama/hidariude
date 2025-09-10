// TaskDAO.java
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dto.AssignmentDTO;
import dto.CustomerMonthlyInvoiceDTO;
import dto.SecretaryDTO;
import dto.SecretaryMonthlySummaryDTO;
import dto.TaskDTO;

public class TaskDAO extends BaseDAO {

	// ========================
	// SQL 定義
	// ========================

	/**
	 * 秘書ID・顧客ID・年月(YYYY-MM) でタスク取得
	 * 取得カラム: tasks.* + assignments.* + task_rank.rank_name
	 * 絞り込み: a.secretary_id, a.customer_id, a.target_year_month, 各 deleted_at IS NULL
	 * 並び順: t.start_time
	 */
	private static final String SQL_SELECT_BY_SEC_CUST_MONTH = "SELECT " +
	// ---- tasks.* (14) ----
			"  t.id                              AS t_id, " +
			"  t.assignment_id                   AS t_assignment_id, " +
			"  t.work_date                       AS t_work_date, " +
			"  t.start_time                      AS t_start_time, " +
			"  t.end_time                        AS t_end_time, " +
			"  t.work_minute                     AS t_work_minute, " +
			"  t.work_content                    AS t_work_content, " +
			"  t.approved_at                     AS t_approved_at, " +
			"  t.approved_by                     AS t_approved_by, " +
			"  t.customer_monthly_invoice_id     AS t_customer_monthly_invoice_id, " +
			"  t.secretary_monthly_summary_id    AS t_secretary_monthly_summary_id, " +
			"  t.created_at                      AS t_created_at, " +
			"  t.updated_at                      AS t_updated_at, " +
			"  t.deleted_at                      AS t_deleted_at, " +
			"  t.remanded_at                     AS t_remanded_at," +
			"  t.remanded_by                     AS t_remanded_by," +
			"  t.remand_comment                  AS t_remand_comment," +

			// ---- assignments.* (16) ----
			"  a.id                              AS a_id, " +
			"  a.customer_id                     AS a_customer_id, " +
			"  a.secretary_id                    AS a_secretary_id, " +
			"  a.task_rank_id                    AS a_task_rank_id, " +
			"  a.target_year_month               AS a_target_year_month, " +
			"  a.base_pay_customer               AS a_base_pay_customer, " +
			"  a.base_pay_secretary              AS a_base_pay_secretary, " +
			"  a.increase_base_pay_customer      AS a_increase_base_pay_customer, " +
			"  a.increase_base_pay_secretary     AS a_increase_base_pay_secretary, " +
			"  a.customer_based_incentive_for_customer  AS a_cust_incentive_for_customer, " +
			"  a.customer_based_incentive_for_secretary AS a_cust_incentive_for_secretary, " +
			"  a.status                          AS a_status, " +
			//        "  a.created_by                      AS a_created_by, " +
			"  a.created_at                      AS a_created_at, " +
			"  a.updated_at                      AS a_updated_at, " +
			"  a.deleted_at                      AS a_deleted_at, " +

			// ---- task_rank.rank_name (1) ----
			"  tr.rank_name                      AS tr_rank_name " +

			"FROM tasks t " +
			"JOIN assignments a ON a.id = t.assignment_id AND a.deleted_at IS NULL " +
			"LEFT JOIN task_rank tr ON tr.id = a.task_rank_id AND tr.deleted_at IS NULL " +
			"WHERE a.secretary_id = ? " +
			"  AND a.customer_id  = ? " +
			"  AND a.target_year_month = ? " +
			"  AND t.deleted_at IS NULL " +
			"ORDER BY t.start_time";

	private static final String SQL_SELECT_BY_MONTH = "SELECT " +
	// ---- tasks.* (14) ----
			"  t.id                              AS t_id, " +
			"  t.assignment_id                   AS t_assignment_id, " +
			"  t.work_date                       AS t_work_date, " +
			"  t.start_time                      AS t_start_time, " +
			"  t.end_time                        AS t_end_time, " +
			"  t.work_minute                     AS t_work_minute, " +
			"  t.work_content                    AS t_work_content, " +
			"  t.approved_at                     AS t_approved_at, " +
			"  t.approved_by                     AS t_approved_by, " +
			"  t.customer_monthly_invoice_id     AS t_customer_monthly_invoice_id, " +
			"  t.secretary_monthly_summary_id    AS t_secretary_monthly_summary_id, " +

			// ---- assignments.* (16) ----
			"  a.id                              AS a_id, " +
			"  a.customer_id                     AS a_customer_id, " +
			"  a.secretary_id                    AS a_secretary_id, " +
			"  a.task_rank_id                    AS a_task_rank_id, " +
			"  a.target_year_month               AS a_target_year_month, " +
			"  a.base_pay_customer               AS a_base_pay_customer, " +
			"  a.base_pay_secretary              AS a_base_pay_secretary, " +
			"  a.increase_base_pay_customer      AS a_increase_base_pay_customer, " +
			"  a.increase_base_pay_secretary     AS a_increase_base_pay_secretary, " +
			"  a.customer_based_incentive_for_customer  AS a_cust_incentive_for_customer, " +
			"  a.customer_based_incentive_for_secretary AS a_cust_incentive_for_secretary, " +

			"  a.base_pay_customer + a.increase_base_pay_customer + a.customer_based_incentive_for_customer AS a_all_pay_customer, "
			+
			"  a.base_pay_secretary + a.increase_base_pay_secretary + a.customer_based_incentive_for_secretary AS a_all_pay_secretary, "
			+

			"  a.status                          AS a_status, " +

			// ---- task_rank.rank_name (1) ----
			"  tr.rank_name                      AS tr_rank_name, " +

			// ---- customers ----
			"  c.company_name                      		AS c_company_name, " +

			// ---- secretaries ----
			"  s.name                      		AS s_name " +

			"FROM tasks t " +
			"JOIN assignments a ON a.id = t.assignment_id AND a.deleted_at IS NULL " +
			"LEFT JOIN task_rank tr ON tr.id = a.task_rank_id AND tr.deleted_at IS NULL " +
			"INNER JOIN customers c ON a.customer_id = c.id AND c.deleted_at IS NULL " +
			"INNER JOIN secretaries s ON s.id = a.secretary_id AND s.deleted_at IS NULL ";
//			"WHERE a.target_year_month = ? " +
//			"  AND t.deleted_at IS NULL " +
//			"ORDER BY t.work_date,a.customer_id, t.start_time";

	// ---- 追加: 単一取得用SQL ----
	private static final String SQL_SELECT_BY_ID = "SELECT " +
	// tasks.*
			"  t.id                              AS t_id, " +
			"  t.assignment_id                   AS t_assignment_id, " +
			"  t.work_date                       AS t_work_date, " +
			"  t.start_time                      AS t_start_time, " +
			"  t.end_time                        AS t_end_time, " +
			"  t.work_minute                     AS t_work_minute, " +
			"  t.work_content                    AS t_work_content, " +
			"  t.approved_at                     AS t_approved_at, " +
			"  t.approved_by                     AS t_approved_by, " +
			"  t.customer_monthly_invoice_id     AS t_customer_monthly_invoice_id, " +
			"  t.secretary_monthly_summary_id    AS t_secretary_monthly_summary_id, " +
			"  t.created_at                      AS t_created_at, " +
			"  t.updated_at                      AS t_updated_at, " +
			"  t.deleted_at                      AS t_deleted_at, " +

			// assignments.*
			"  a.id                              AS a_id, " +
			"  a.customer_id                     AS a_customer_id, " +
			"  a.secretary_id                    AS a_secretary_id, " +
			"  a.task_rank_id                    AS a_task_rank_id, " +
			"  a.target_year_month               AS a_target_year_month, " +
			"  a.base_pay_customer               AS a_base_pay_customer, " +
			"  a.base_pay_secretary              AS a_base_pay_secretary, " +
			"  a.increase_base_pay_customer      AS a_increase_base_pay_customer, " +
			"  a.increase_base_pay_secretary     AS a_increase_base_pay_secretary, " +
			"  a.customer_based_incentive_for_customer  AS a_cust_incentive_for_customer, " +
			"  a.customer_based_incentive_for_secretary AS a_cust_incentive_for_secretary, " +
			"  a.status                          AS a_status, " +
			"  a.created_at                      AS a_created_at, " +
			"  a.updated_at                      AS a_updated_at, " +
			"  a.deleted_at                      AS a_deleted_at, " +

			// task_rank
			"  tr.rank_name                      AS tr_rank_name " +

			"FROM tasks t " +
			"JOIN assignments a ON a.id = t.assignment_id AND a.deleted_at IS NULL " +
			"LEFT JOIN task_rank tr ON tr.id = a.task_rank_id AND tr.deleted_at IS NULL " +
			"WHERE t.id = ? AND t.deleted_at IS NULL";

	// ベース（ORDER BY は後置）
	private static final String SQL_SELECT_BY_MONTH_BASE =
		    "SELECT " +
		    "  t.id AS t_id, " +
		    "  t.assignment_id AS t_assignment_id, " +
		    "  t.work_date AS t_work_date, " +
		    "  t.start_time AS t_start_time, " +
		    "  t.end_time AS t_end_time, " +
		    "  t.work_minute AS t_work_minute, " +
		    "  t.work_content AS t_work_content, " +
		    "  t.approved_at AS t_approved_at, " +
		    "  t.approved_by AS t_approved_by, " +
		    "  t.customer_monthly_invoice_id AS t_customer_monthly_invoice_id, " +
		    "  t.secretary_monthly_summary_id AS t_secretary_monthly_summary_id, " +
		    "  a.id AS a_id, " +
		    "  a.customer_id AS a_customer_id, " +
		    "  a.secretary_id AS a_secretary_id, " +
		    "  a.task_rank_id AS a_task_rank_id, " +
		    "  a.target_year_month AS a_target_year_month, " +
		    "  a.base_pay_customer AS a_base_pay_customer, " +
		    "  a.base_pay_secretary AS a_base_pay_secretary, " +
		    "  a.increase_base_pay_customer AS a_increase_base_pay_customer, " +
		    "  a.increase_base_pay_secretary AS a_increase_base_pay_secretary, " +
		    "  a.customer_based_incentive_for_customer AS a_cust_incentive_for_customer, " +
		    "  a.customer_based_incentive_for_secretary AS a_cust_incentive_for_secretary, " +
		    "  a.base_pay_customer + a.increase_base_pay_customer + a.customer_based_incentive_for_customer AS a_all_pay_customer, " +
		    "  a.base_pay_secretary + a.increase_base_pay_secretary + a.customer_based_incentive_for_secretary AS a_all_pay_secretary, " +
		    "  a.status AS a_status, " +
		    "  tr.rank_name AS tr_rank_name, " +
		    "  c.company_name AS c_company_name, " +
		    "  s.name AS s_name, " +                    // ← カンマを追加
		    "  t.remanded_at AS t_remanded_at, " +
		    "  t.remanded_by AS t_remanded_by, " +
		    "  t.remand_comment AS t_remand_comment " + // ← 末尾カンマを削除
		    "FROM tasks t " +
		    "JOIN assignments a ON a.id = t.assignment_id AND a.deleted_at IS NULL " +
		    "LEFT JOIN task_rank tr ON tr.id = a.task_rank_id AND tr.deleted_at IS NULL " +
		    "INNER JOIN customers c ON a.customer_id = c.id AND c.deleted_at IS NULL " +
		    "INNER JOIN secretaries s ON s.id = a.secretary_id AND s.deleted_at IS NULL " +
		    "WHERE a.target_year_month = ? AND t.deleted_at IS NULL ";

	private static final String SQL_COUNT_BY_STATUS_SECRETARY =
		    "SELECT " +
		    // 件数
		    "  SUM(CASE WHEN t.approved_at IS NULL AND t.remanded_at IS NULL THEN 1 ELSE 0 END) AS unapproved_count, " +
		    "  SUM(CASE WHEN t.approved_at IS NOT NULL THEN 1 ELSE 0 END)                       AS approved_count, " +
		    "  SUM(CASE WHEN t.remanded_at IS NOT NULL THEN 1 ELSE 0 END)                       AS remanded_count, " +
		    "  COUNT(*)                                                                          AS total_count, " +
		    // 金額（全件）
		    "  COALESCE(SUM( " +
		    "    (COALESCE(a.base_pay_secretary,0) + COALESCE(a.increase_base_pay_secretary,0) + COALESCE(a.customer_based_incentive_for_secretary,0)) " +
		    "    * (COALESCE(t.work_minute,0)::numeric / 60) " +
		    "  ),0) AS total_amount_all, " +
		    // 金額（承認済みのみ）
		    "  COALESCE(SUM(CASE WHEN t.approved_at IS NOT NULL THEN " +
		    "    (COALESCE(a.base_pay_secretary,0) + COALESCE(a.increase_base_pay_secretary,0) + COALESCE(a.customer_based_incentive_for_secretary,0)) " +
		    "    * (COALESCE(t.work_minute,0)::numeric / 60) " +
		    "  ELSE 0 END),0) AS total_amount_approved " +
		    "FROM tasks t " +
		    "JOIN assignments a ON a.id = t.assignment_id AND a.deleted_at IS NULL " +
		    "WHERE t.deleted_at IS NULL " +
		    "  AND a.secretary_id = ? " +
		    "  AND a.target_year_month = ?";
	
	/** tasks INSERT（RETURNING id, created_at, updated_at） */
	private static final String SQL_INSERT = "INSERT INTO tasks (" +
			"  assignment_id, work_date, start_time, end_time, work_minute, work_content," +
			"  approved_at, approved_by, customer_monthly_invoice_id, secretary_monthly_summary_id" +
			") VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id, created_at, updated_at";

	/** tasks UPDATE（論理未削除のみ） */
	private static final String SQL_UPDATE = "UPDATE tasks SET " +
			"  assignment_id = ?, " +
			"  work_date = ?, " +
			"  start_time = ?, " +
			"  end_time = ?, " +
			"  work_minute = ?, " +
			"  work_content = ?, " +
			"  approved_at = ?, " +
			"  approved_by = ?, " +
			"  customer_monthly_invoice_id = ?, " +
			"  secretary_monthly_summary_id = ?, " +
			"  updated_at = CURRENT_TIMESTAMP " +
			"WHERE id = ? AND deleted_at IS NULL";

	/** tasks 論理DELETE（deleted_at を現在時刻に） */
	private static final String SQL_DELETE_LOGICAL = "UPDATE tasks SET deleted_at = CURRENT_TIMESTAMP " +
			"WHERE id = ? AND deleted_at IS NULL";
	
	private static final String SQL_SELECT_BY_SEC_MONTH_BASE =
		    SQL_SELECT_BY_MONTH_BASE + // 既存ベースSQLを流用
		    " AND a.secretary_id = ? ";

	public TaskDAO(Connection conn) {
		super(conn);
	}

	// ========================
	// SELECT
	// ========================

	/**
	 * 秘書ID・顧客ID・年月で tasks を取得します。
	 * @param secretaryId 秘書ID
	 * @param customerId 顧客ID
	 * @param yearMonth "YYYY-MM"
	 * @return List<TaskDTO>
	 * @throws DAOException 取得時エラー
	 */
	public List<TaskDTO> selectBySecretaryAndCustomerAndMonth(UUID secretaryId, UUID customerId, String yearMonth) {
		List<TaskDTO> list = new ArrayList<>();

		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_SEC_CUST_MONTH)) {
			int p = 1;
			ps.setObject(p++, secretaryId);
			ps.setObject(p++, customerId);
			ps.setString(p++, yearMonth);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					TaskDTO t = new TaskDTO();

					// ---- tasks.* ----
					t.setId(rs.getObject("t_id", UUID.class));

					AssignmentDTO ad = new AssignmentDTO();
					ad.setAssignmentId(rs.getObject("a_id", UUID.class)); // set now; also set below
					t.setAssignment(ad);

					t.setWorkDate(rs.getDate("t_work_date"));
					t.setStartTime(rs.getTimestamp("t_start_time"));
					t.setEndTime(rs.getTimestamp("t_end_time"));
					t.setWorkMinute(rs.getObject("t_work_minute", Integer.class));
					t.setWorkContent(rs.getString("t_work_content"));
					t.setApprovedAt(rs.getTimestamp("t_approved_at"));

					UUID approvedById = rs.getObject("t_approved_by", UUID.class);
					if (approvedById != null) {
						SecretaryDTO s = new SecretaryDTO();
						s.setId(approvedById);
						t.setApprovedBy(s);
					}

					UUID cmiId = rs.getObject("t_customer_monthly_invoice_id", UUID.class);
					if (cmiId != null) {
						CustomerMonthlyInvoiceDTO cmi = new CustomerMonthlyInvoiceDTO();
						cmi.setId(cmiId);
						t.setCustomerMonthlyInvoice(cmi);
					}

					UUID smsId = rs.getObject("t_secretary_monthly_summary_id", UUID.class);
					if (smsId != null) {
						SecretaryMonthlySummaryDTO sms = new SecretaryMonthlySummaryDTO();
						sms.setId(smsId);
						t.setSecretaryMonthlySummary(sms);
					}

					t.setCreatedAt(rs.getTimestamp("t_created_at"));
					t.setUpdatedAt(rs.getTimestamp("t_updated_at"));
					t.setDeletedAt(rs.getTimestamp("t_deleted_at"));

					// ---- assignments.* を TaskDTO#assignment に詰める ----
					ad.setAssignmentId(rs.getObject("a_id", UUID.class));
					ad.setAssignmentCustomerId(rs.getObject("a_customer_id", UUID.class));
					ad.setAssignmentSecretaryId(rs.getObject("a_secretary_id", UUID.class));
					ad.setTaskRankId(rs.getObject("a_task_rank_id", UUID.class));
					ad.setTargetYearMonth(rs.getString("a_target_year_month"));
					ad.setBasePayCustomer(rs.getBigDecimal("a_base_pay_customer"));
					ad.setBasePaySecretary(rs.getBigDecimal("a_base_pay_secretary"));
					ad.setIncreaseBasePayCustomer(rs.getBigDecimal("a_increase_base_pay_customer"));
					ad.setIncreaseBasePaySecretary(rs.getBigDecimal("a_increase_base_pay_secretary"));
					ad.setCustomerBasedIncentiveForCustomer(rs.getBigDecimal("a_cust_incentive_for_customer"));
					ad.setCustomerBasedIncentiveForSecretary(rs.getBigDecimal("a_cust_incentive_for_secretary"));
					ad.setAssignmentStatus(rs.getString("a_status"));
					//                    ad.setAssignmentCreatedBy(rs.getObject("a_created_by", UUID.class));
					ad.setAssignmentCreatedAt(rs.getTimestamp("a_created_at"));
					ad.setAssignmentUpdatedAt(rs.getTimestamp("a_updated_at"));
					ad.setAssignmentDeletedAt(rs.getTimestamp("a_deleted_at"));

					// ---- task_rank.rank_name ----
					ad.setTaskRankName(rs.getString("tr_rank_name"));

					list.add(t);
				}
			}
		} catch (SQLException e) {
			throw new DAOException("E:TS11 tasks 取得に失敗しました。", e);
		}

		return list;
	}
	
	public List<TaskDTO> selectBySecretaryAndMonth(UUID secretaryId, String yearMonth, String status) {
	    StringBuilder sql = new StringBuilder(SQL_SELECT_BY_SEC_MONTH_BASE);

	    // ステータス絞り込み
	    switch (status == null ? "all" : status) {
	        case "approved":
	            sql.append(" AND t.approved_at IS NOT NULL ");
	            break;
	        case "unapproved":
	            sql.append(" AND t.approved_at IS NULL AND t.remanded_at IS NULL ");
	            break;
	        case "remanded":
	            sql.append(" AND t.remanded_at IS NOT NULL ");
	            break;
	        default: /* all */ break;
	    }

	    // 会社名→日付→開始時刻で並び替え（JSPでグループ化しやすい）
	    sql.append(" ORDER BY c.company_name, t.work_date, t.start_time ");

	    List<TaskDTO> list = new ArrayList<>();
	    try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
	        int p = 1;
	        ps.setString(p++, yearMonth);
	        ps.setObject(p++, secretaryId);
	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                TaskDTO t = new TaskDTO();

	                // --- 以下は既存 selectByMonth と同じ詰め替え ---
	                t.setId(rs.getObject("t_id", UUID.class));

	                AssignmentDTO ad = new AssignmentDTO();
	                ad.setAssignmentId(rs.getObject("a_id", UUID.class));
	                t.setAssignment(ad);

	                t.setWorkDate(rs.getDate("t_work_date"));
	                t.setStartTime(rs.getTimestamp("t_start_time"));
	                t.setEndTime(rs.getTimestamp("t_end_time"));
	                t.setWorkMinute(rs.getObject("t_work_minute", Integer.class));
	                t.setWorkContent(rs.getString("t_work_content"));
	                t.setApprovedAt(rs.getTimestamp("t_approved_at"));
	                t.setRemandedAt(rs.getTimestamp("t_remanded_at"));
	                t.setRemandedBy(rs.getObject("t_remanded_by", UUID.class));
	                t.setRemandComment(rs.getString("t_remand_comment"));

	                UUID approvedById = rs.getObject("t_approved_by", UUID.class);
	                if (approvedById != null) {
	                    SecretaryDTO s = new SecretaryDTO();
	                    s.setId(approvedById);
	                    t.setApprovedBy(s);
	                }

	                ad.setAssignmentCustomerId(rs.getObject("a_customer_id", UUID.class));
	                ad.setAssignmentSecretaryId(rs.getObject("a_secretary_id", UUID.class));
	                ad.setTaskRankId(rs.getObject("a_task_rank_id", UUID.class));
	                ad.setTargetYearMonth(rs.getString("a_target_year_month"));
	                ad.setBasePayCustomer(rs.getBigDecimal("a_base_pay_customer"));
	                ad.setBasePaySecretary(rs.getBigDecimal("a_base_pay_secretary"));
	                ad.setIncreaseBasePayCustomer(rs.getBigDecimal("a_increase_base_pay_customer"));
	                ad.setIncreaseBasePaySecretary(rs.getBigDecimal("a_increase_base_pay_secretary"));
	                ad.setCustomerBasedIncentiveForCustomer(rs.getBigDecimal("a_cust_incentive_for_customer"));
	                ad.setCustomerBasedIncentiveForSecretary(rs.getBigDecimal("a_cust_incentive_for_secretary"));

	                // 時給合算（エイリアス）
	                ad.setHourlyPaySecretary(rs.getBigDecimal("a_all_pay_secretary"));
	                ad.setHourlyPayCustomer(rs.getBigDecimal("a_all_pay_customer"));

	                ad.setAssignmentStatus(rs.getString("a_status"));
	                ad.setTaskRankName(rs.getString("tr_rank_name"));
	                ad.setCustomerCompanyName(rs.getString("c_company_name"));
	                ad.setSecretaryName(rs.getString("s_name"));

	                list.add(t);
	            }
	        }
	    } catch (SQLException e) {
	        throw new DAOException("E:TS11 tasks 取得に失敗しました。(sec+month)", e);
	    }
	    return list;
	}

	public List<TaskDTO> selectByMonth(String yearMonth) {
		List<TaskDTO> list = new ArrayList<>();

		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_MONTH)) {
			int p = 1;
			ps.setString(p++, yearMonth);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					TaskDTO t = new TaskDTO();

					// ---- tasks.* ----
					t.setId(rs.getObject("t_id", UUID.class));

					AssignmentDTO ad = new AssignmentDTO();
					ad.setAssignmentId(rs.getObject("a_id", UUID.class)); // set now; also set below
					t.setAssignment(ad);

					t.setWorkDate(rs.getDate("t_work_date"));
					t.setStartTime(rs.getTimestamp("t_start_time"));
					t.setEndTime(rs.getTimestamp("t_end_time"));
					t.setWorkMinute(rs.getObject("t_work_minute", Integer.class));
					t.setWorkContent(rs.getString("t_work_content"));
					t.setApprovedAt(rs.getTimestamp("t_approved_at"));
					t.setRemandedAt(rs.getTimestamp("t_remanded_at"));
					t.setRemandedBy(rs.getObject("t_remanded_by", UUID.class));
					t.setRemandComment(rs.getString("t_remand_comment"));

					UUID approvedById = rs.getObject("t_approved_by", UUID.class);
					if (approvedById != null) {
						SecretaryDTO s = new SecretaryDTO();
						s.setId(approvedById);
						t.setApprovedBy(s);
					}

					UUID cmiId = rs.getObject("t_customer_monthly_invoice_id", UUID.class);
					if (cmiId != null) {
						CustomerMonthlyInvoiceDTO cmi = new CustomerMonthlyInvoiceDTO();
						cmi.setId(cmiId);
						t.setCustomerMonthlyInvoice(cmi);
					}

					UUID smsId = rs.getObject("t_secretary_monthly_summary_id", UUID.class);
					if (smsId != null) {
						SecretaryMonthlySummaryDTO sms = new SecretaryMonthlySummaryDTO();
						sms.setId(smsId);
						t.setSecretaryMonthlySummary(sms);
					}
					// ---- assignments.* を TaskDTO#assignment に詰める ----
					ad.setAssignmentCustomerId(rs.getObject("a_customer_id", UUID.class));
					ad.setAssignmentSecretaryId(rs.getObject("a_secretary_id", UUID.class));
					ad.setTaskRankId(rs.getObject("a_task_rank_id", UUID.class));
					ad.setTargetYearMonth(rs.getString("a_target_year_month"));
					ad.setBasePayCustomer(rs.getBigDecimal("a_base_pay_customer"));
					ad.setBasePaySecretary(rs.getBigDecimal("a_base_pay_secretary"));
					ad.setIncreaseBasePayCustomer(rs.getBigDecimal("a_increase_base_pay_customer"));
					ad.setIncreaseBasePaySecretary(rs.getBigDecimal("a_increase_base_pay_secretary"));
					ad.setCustomerBasedIncentiveForCustomer(rs.getBigDecimal("a_cust_incentive_for_customer"));
					ad.setCustomerBasedIncentiveForSecretary(rs.getBigDecimal("a_cust_incentive_for_secretary"));
					ad.setHourlyPaySecretary(rs.getBigDecimal("a_all_pay_secretary"));
					ad.setHourlyPayCustomer(rs.getBigDecimal("a_all_pay_customer"));
					ad.setAssignmentStatus(rs.getString("a_status"));

					// ---- task_rank.rank_name ----
					ad.setTaskRankName(rs.getString("tr_rank_name"));
					ad.setCustomerCompanyName(rs.getString("c_company_name"));
					ad.setSecretaryName(rs.getString("s_name"));

					list.add(t);
				}
			}
		} catch (SQLException e) {
			throw new DAOException("E:TS11 tasks 取得に失敗しました。", e);
		}

		return list;
	}

	public List<TaskDTO> selectByMonth(String yearMonth, String status) {
		// status に応じて WHERE を足す
		StringBuilder sql = new StringBuilder(SQL_SELECT_BY_MONTH_BASE);
		if ("approved".equals(status)) {
			sql.append(" AND t.approved_at IS NOT NULL ");
		} else if ("unapproved".equals(status)) {
			sql.append(" AND t.approved_at IS NULL ");
		}
		sql.append(" ORDER BY t.work_date, a.customer_id, t.start_time ");

		List<TaskDTO> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			ps.setString(1, yearMonth);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					TaskDTO t = new TaskDTO();

					// --- 以下は既存 selectByMonth の詰め替え処理と同じ ---
					t.setId(rs.getObject("t_id", UUID.class));

					dto.AssignmentDTO ad = new dto.AssignmentDTO();
					ad.setAssignmentId(rs.getObject("a_id", UUID.class));
					t.setAssignment(ad);

					t.setWorkDate(rs.getDate("t_work_date"));
					t.setStartTime(rs.getTimestamp("t_start_time"));
					t.setEndTime(rs.getTimestamp("t_end_time"));
					t.setWorkMinute(rs.getObject("t_work_minute", Integer.class));
					t.setWorkContent(rs.getString("t_work_content"));
					t.setApprovedAt(rs.getTimestamp("t_approved_at"));
					t.setRemandedAt(rs.getTimestamp("t_remanded_at"));
					t.setRemandedBy(rs.getObject("t_remanded_by", UUID.class));
					t.setRemandComment(rs.getString("t_remand_comment"));

					UUID approvedById = rs.getObject("t_approved_by", UUID.class);
					if (approvedById != null) {
						dto.SecretaryDTO s = new dto.SecretaryDTO();
						s.setId(approvedById);
						t.setApprovedBy(s);
					}

					ad.setAssignmentCustomerId(rs.getObject("a_customer_id", UUID.class));
					ad.setAssignmentSecretaryId(rs.getObject("a_secretary_id", UUID.class));
					ad.setTaskRankId(rs.getObject("a_task_rank_id", UUID.class));
					ad.setTargetYearMonth(rs.getString("a_target_year_month"));
					ad.setBasePayCustomer(rs.getBigDecimal("a_base_pay_customer"));
					ad.setBasePaySecretary(rs.getBigDecimal("a_base_pay_secretary"));
					ad.setIncreaseBasePayCustomer(rs.getBigDecimal("a_increase_base_pay_customer"));
					ad.setIncreaseBasePaySecretary(rs.getBigDecimal("a_increase_base_pay_secretary"));
					ad.setCustomerBasedIncentiveForCustomer(rs.getBigDecimal("a_cust_incentive_for_customer"));
					ad.setCustomerBasedIncentiveForSecretary(rs.getBigDecimal("a_cust_incentive_for_secretary"));
					ad.setHourlyPaySecretary(rs.getBigDecimal("a_all_pay_secretary"));
					ad.setHourlyPayCustomer(rs.getBigDecimal("a_all_pay_customer"));
					ad.setAssignmentStatus(rs.getString("a_status"));
					ad.setTaskRankName(rs.getString("tr_rank_name"));
					ad.setCustomerCompanyName(rs.getString("c_company_name"));
					ad.setSecretaryName(rs.getString("s_name"));

					list.add(t);
				}
			}
		} catch (java.sql.SQLException e) {
			throw new DAOException("E:TS11 tasks 取得に失敗しました。", e);
		}
		return list;
	}

	public List<TaskDTO> selectByMonth(String yearMonth, String status,
			String secretaryNameLike, String customerNameLike) {
		StringBuilder sql = new StringBuilder(SQL_SELECT_BY_MONTH_BASE);

		// ステータス
		switch (status) {
		case "approved":
			sql.append(" AND t.approved_at IS NOT NULL ");
			break;
		case "unapproved":
			sql.append(" AND t.approved_at IS NULL ");
			break;
		case "remanded":
			sql.append(" AND t.remanded_at IS NOT NULL ");
			break;
		default:
			/* all */ break;
		}

		// 文字列検索（部分一致・大文字小文字無視）
		if (secretaryNameLike != null && !secretaryNameLike.isBlank()) {
			sql.append(" AND s.name ILIKE ? ");
		}
		if (customerNameLike != null && !customerNameLike.isBlank()) {
			sql.append(" AND c.company_name ILIKE ? ");
		}

		sql.append(" ORDER BY t.work_date, a.customer_id, t.start_time ");

		List<TaskDTO> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			int p = 1;
			ps.setString(p++, yearMonth);
			if (secretaryNameLike != null && !secretaryNameLike.isBlank()) {
				ps.setString(p++, "%" + secretaryNameLike + "%");
			}
			if (customerNameLike != null && !customerNameLike.isBlank()) {
				ps.setString(p++, "%" + customerNameLike + "%");
			}
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					TaskDTO t = new TaskDTO();
					// --- 以下は既存 selectByMonth の詰め替え処理と同じ ---
					t.setId(rs.getObject("t_id", UUID.class));

					dto.AssignmentDTO ad = new dto.AssignmentDTO();
					ad.setAssignmentId(rs.getObject("a_id", UUID.class));
					t.setAssignment(ad);

					t.setWorkDate(rs.getDate("t_work_date"));
					t.setStartTime(rs.getTimestamp("t_start_time"));
					t.setEndTime(rs.getTimestamp("t_end_time"));
					t.setWorkMinute(rs.getObject("t_work_minute", Integer.class));
					t.setWorkContent(rs.getString("t_work_content"));
					t.setApprovedAt(rs.getTimestamp("t_approved_at"));
					t.setRemandedAt(rs.getTimestamp("t_remanded_at"));
					t.setRemandedBy(rs.getObject("t_remanded_by", UUID.class));
					t.setRemandComment(rs.getString("t_remand_comment"));

					UUID approvedById = rs.getObject("t_approved_by", UUID.class);
					if (approvedById != null) {
						dto.SecretaryDTO s = new dto.SecretaryDTO();
						s.setId(approvedById);
						t.setApprovedBy(s);
					}

					ad.setAssignmentCustomerId(rs.getObject("a_customer_id", UUID.class));
					ad.setAssignmentSecretaryId(rs.getObject("a_secretary_id", UUID.class));
					ad.setTaskRankId(rs.getObject("a_task_rank_id", UUID.class));
					ad.setTargetYearMonth(rs.getString("a_target_year_month"));
					ad.setBasePayCustomer(rs.getBigDecimal("a_base_pay_customer"));
					ad.setBasePaySecretary(rs.getBigDecimal("a_base_pay_secretary"));
					ad.setIncreaseBasePayCustomer(rs.getBigDecimal("a_increase_base_pay_customer"));
					ad.setIncreaseBasePaySecretary(rs.getBigDecimal("a_increase_base_pay_secretary"));
					ad.setCustomerBasedIncentiveForCustomer(rs.getBigDecimal("a_cust_incentive_for_customer"));
					ad.setCustomerBasedIncentiveForSecretary(rs.getBigDecimal("a_cust_incentive_for_secretary"));
					ad.setHourlyPaySecretary(rs.getBigDecimal("a_all_pay_secretary"));
					ad.setHourlyPayCustomer(rs.getBigDecimal("a_all_pay_customer"));
					ad.setAssignmentStatus(rs.getString("a_status"));
					ad.setTaskRankName(rs.getString("tr_rank_name"));
					ad.setCustomerCompanyName(rs.getString("c_company_name"));
					ad.setSecretaryName(rs.getString("s_name"));

					list.add(t);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DAOException("E:TS11 tasks 取得に失敗しました。", e);
		}
		return list;
	}
	
	/** 単票承認取消（承認前に戻す） */
	public int unapprove(UUID taskId) {
	    final String sql =
	        "UPDATE tasks " +
	        "   SET approved_at = NULL, approved_by = NULL, updated_at = NOW() " +
	        " WHERE id = ? AND deleted_at IS NULL";
	    try (PreparedStatement ps = conn.prepareStatement(sql)) {
	        ps.setObject(1, taskId);
	        return ps.executeUpdate();
	    } catch (SQLException e) {
	        throw new RuntimeException("E:TASK-UNAPPROVE 失敗", e);
	    }
	}

	/** 差戻し（承認済なら承認情報をクリア） */
	public int remand(UUID taskId, UUID remandedBy, String comment) {
	    final String sql =
	        "UPDATE tasks " +
	        "   SET remanded_at = NOW(), remanded_by = ?, remand_comment = ?, " +
	        "       approved_at = NULL, approved_by = NULL, updated_at = NOW() " +
	        " WHERE id = ? AND deleted_at IS NULL";
	    try (PreparedStatement ps = conn.prepareStatement(sql)) {
	        if (remandedBy != null) ps.setObject(1, remandedBy); else ps.setNull(1, Types.OTHER);
	        ps.setString(2, comment);
	        ps.setObject(3, taskId);
	        return ps.executeUpdate();
	    } catch (SQLException e) {
	        throw new RuntimeException("E:TASK-REMAND 失敗", e);
	    }
	}


	// ---- 追加: 単一取得メソッド ----
	public TaskDTO selectById(UUID taskId) {
		if (taskId == null) {
			throw new DAOException("E:TS10 id が未設定です。");
		}

		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_ID)) {
			ps.setObject(1, taskId);

			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next())
					return null;

				TaskDTO t = new TaskDTO();

				// tasks.*
				t.setId(rs.getObject("t_id", UUID.class));

				AssignmentDTO ad = new AssignmentDTO();
				ad.setAssignmentId(rs.getObject("a_id", UUID.class));
				t.setAssignment(ad);

				t.setWorkDate(rs.getDate("t_work_date"));
				t.setStartTime(rs.getTimestamp("t_start_time"));
				t.setEndTime(rs.getTimestamp("t_end_time"));
				t.setWorkMinute(rs.getObject("t_work_minute", Integer.class));
				t.setWorkContent(rs.getString("t_work_content"));
				t.setApprovedAt(rs.getTimestamp("t_approved_at"));

				UUID approvedById = rs.getObject("t_approved_by", UUID.class);
				if (approvedById != null) {
					SecretaryDTO s = new SecretaryDTO();
					s.setId(approvedById);
					t.setApprovedBy(s);
				}

				UUID cmiId = rs.getObject("t_customer_monthly_invoice_id", UUID.class);
				if (cmiId != null) {
					CustomerMonthlyInvoiceDTO cmi = new CustomerMonthlyInvoiceDTO();
					cmi.setId(cmiId);
					t.setCustomerMonthlyInvoice(cmi);
				}

				UUID smsId = rs.getObject("t_secretary_monthly_summary_id", UUID.class);
				if (smsId != null) {
					SecretaryMonthlySummaryDTO sms = new SecretaryMonthlySummaryDTO();
					sms.setId(smsId);
					t.setSecretaryMonthlySummary(sms);
				}

				t.setCreatedAt(rs.getTimestamp("t_created_at"));
				t.setUpdatedAt(rs.getTimestamp("t_updated_at"));
				t.setDeletedAt(rs.getTimestamp("t_deleted_at"));

				// assignments.* -> t.assignment
				ad.setAssignmentId(rs.getObject("a_id", UUID.class));
				ad.setAssignmentCustomerId(rs.getObject("a_customer_id", UUID.class));
				ad.setAssignmentSecretaryId(rs.getObject("a_secretary_id", UUID.class));
				ad.setTaskRankId(rs.getObject("a_task_rank_id", UUID.class));
				ad.setTargetYearMonth(rs.getString("a_target_year_month"));
				ad.setBasePayCustomer(rs.getBigDecimal("a_base_pay_customer"));
				ad.setBasePaySecretary(rs.getBigDecimal("a_base_pay_secretary"));
				ad.setIncreaseBasePayCustomer(rs.getBigDecimal("a_increase_base_pay_customer"));
				ad.setIncreaseBasePaySecretary(rs.getBigDecimal("a_increase_base_pay_secretary"));
				ad.setCustomerBasedIncentiveForCustomer(rs.getBigDecimal("a_cust_incentive_for_customer"));
				ad.setCustomerBasedIncentiveForSecretary(rs.getBigDecimal("a_cust_incentive_for_secretary"));
				ad.setAssignmentStatus(rs.getString("a_status"));
				ad.setAssignmentCreatedAt(rs.getTimestamp("a_created_at"));
				ad.setAssignmentUpdatedAt(rs.getTimestamp("a_updated_at"));
				ad.setAssignmentDeletedAt(rs.getTimestamp("a_deleted_at"));

				// task_rank
				ad.setTaskRankName(rs.getString("tr_rank_name"));

				return t;
			}
		} catch (SQLException e) {
			throw new DAOException("E:TS12 tasks 単一取得に失敗しました。", e);
		}
	}

	/**
	 * タスクを新規登録します。
	 * 必須: assignment.id, workDate, startTime, endTime, workMinute, workContent
	 * 任意: approvedAt/By, customerMonthlyInvoiceId, secretaryMonthlySummaryId
	 *
	 * @param dto TaskDTO
	 * @return 採番された tasks.id
	 */
	public UUID insert(TaskDTO dto) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
			int i = 1;

			// assignment_id
			UUID assignmentId = (dto.getAssignment() != null) ? dto.getAssignment().getAssignmentId() : null;
			if (assignmentId == null) {
				throw new DAOException("E:TS21 assignmentId が未設定です。");
			}
			ps.setObject(i++, assignmentId);

			// work_date / start_time / end_time / work_minute / work_content
			if (dto.getWorkDate() == null)
				throw new DAOException("E:TS22 workDate が未設定です。");
			ps.setDate(i++, new java.sql.Date(dto.getWorkDate().getTime()));

			ps.setTimestamp(i++, dto.getStartTime());
			ps.setTimestamp(i++, dto.getEndTime());

			if (dto.getWorkMinute() == null)
				throw new DAOException("E:TS23 workMinute が未設定です。");
			ps.setInt(i++, dto.getWorkMinute());

			ps.setString(i++, dto.getWorkContent());

			// approved_at / approved_by / customer_monthly_invoice_id / secretary_monthly_summary_id
			if (dto.getApprovedAt() != null) {
				ps.setTimestamp(i++, dto.getApprovedAt());
			} else {
				ps.setNull(i++, Types.TIMESTAMP);
			}

			if (dto.getApprovedBy() != null && dto.getApprovedBy().getId() != null) {
				ps.setObject(i++, dto.getApprovedBy().getId());
			} else {
				ps.setNull(i++, Types.OTHER);
			}

			if (dto.getCustomerMonthlyInvoice() != null && dto.getCustomerMonthlyInvoice().getId() != null) {
				ps.setObject(i++, dto.getCustomerMonthlyInvoice().getId());
			} else {
				ps.setNull(i++, Types.OTHER);
			}

			if (dto.getSecretaryMonthlySummary() != null && dto.getSecretaryMonthlySummary().getId() != null) {
				ps.setObject(i++, dto.getSecretaryMonthlySummary().getId());
			} else {
				ps.setNull(i++, Types.OTHER);
			}

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getObject(1, UUID.class); // id
				}
				return null;
			}
		} catch (SQLException e) {
			throw new DAOException("E:TS24 tasks INSERT に失敗しました。", e);
		}
	}

	/**
	 * タスクを更新します（論理未削除のみ対象）。
	 * 必須: id, assignment.id, workDate, startTime, endTime, workMinute, workContent
	 * 任意: approvedAt/By, customerMonthlyInvoiceId, secretaryMonthlySummaryId
	 *
	 * @param dto TaskDTO
	 * @return 影響行数（0 の場合は未更新）
	 */
	public int update(TaskDTO dto) {
		if (dto.getId() == null) {
			throw new DAOException("E:TS31 id が未設定です。");
		}
		UUID assignmentId = (dto.getAssignment() != null) ? dto.getAssignment().getAssignmentId() : null;
		if (assignmentId == null) {
			throw new DAOException("E:TS32 assignmentId が未設定です。");
		}
		if (dto.getWorkDate() == null) {
			throw new DAOException("E:TS33 workDate が未設定です。");
		}
		if (dto.getStartTime() == null || dto.getEndTime() == null) {
			throw new DAOException("E:TS34 startTime/endTime が未設定です。");
		}
		if (dto.getWorkMinute() == null) {
			throw new DAOException("E:TS35 workMinute が未設定です。");
		}
		if (dto.getWorkContent() == null) {
			throw new DAOException("E:TS36 workContent が未設定です。");
		}

		try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
			int i = 1;

			// 必須
			ps.setObject(i++, assignmentId);
			ps.setDate(i++, new java.sql.Date(dto.getWorkDate().getTime()));
			ps.setTimestamp(i++, dto.getStartTime());
			ps.setTimestamp(i++, dto.getEndTime());
			ps.setInt(i++, dto.getWorkMinute());
			ps.setString(i++, dto.getWorkContent());

			// 任意（NULL可）
			if (dto.getApprovedAt() != null) {
				ps.setTimestamp(i++, dto.getApprovedAt());
			} else {
				ps.setNull(i++, Types.TIMESTAMP);
			}

			if (dto.getApprovedBy() != null && dto.getApprovedBy().getId() != null) {
				ps.setObject(i++, dto.getApprovedBy().getId());
			} else {
				ps.setNull(i++, Types.OTHER);
			}

			if (dto.getCustomerMonthlyInvoice() != null && dto.getCustomerMonthlyInvoice().getId() != null) {
				ps.setObject(i++, dto.getCustomerMonthlyInvoice().getId());
			} else {
				ps.setNull(i++, Types.OTHER);
			}

			if (dto.getSecretaryMonthlySummary() != null && dto.getSecretaryMonthlySummary().getId() != null) {
				ps.setObject(i++, dto.getSecretaryMonthlySummary().getId());
			} else {
				ps.setNull(i++, Types.OTHER);
			}

			// WHERE id = ? AND deleted_at IS NULL
			ps.setObject(i++, dto.getId());

			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:TS37 tasks UPDATE に失敗しました。", e);
		}
	}

	/**
	 * タスクを論理削除します（deleted_at を現在時刻に更新）。
	 * @param id tasks.id
	 * @return 影響行数（0 の場合は未削除）
	 */
	public int delete(UUID id) {
		if (id == null) {
			throw new DAOException("E:TS41 id が未設定です。");
		}
		try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_LOGICAL)) {
			ps.setObject(1, id);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:TS42 tasks 論理DELETE に失敗しました。", e);
		}
	}

	public int approve(UUID taskId, UUID approvedBy) {
		final String sql = "UPDATE tasks " +
				"   SET approved_at = NOW(), approved_by = ?, updated_at = NOW() " +
				" WHERE id = ? AND deleted_at IS NULL AND approved_at IS NULL";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			if (approvedBy != null)
				ps.setObject(1, approvedBy);
			else
				ps.setNull(1, Types.OTHER);
			ps.setObject(2, taskId);
			return ps.executeUpdate(); // 1: 更新、0: 既に承認済み等
		} catch (SQLException e) {
			throw new RuntimeException("E:TASK-APPROVE 更新に失敗", e);
		}
	}
	
	public TaskDTO selectCountsForSecretaryMonth(UUID secretaryId, String yearMonth) {
		TaskDTO r = new TaskDTO();
        try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_BY_STATUS_SECRETARY)) {
            int p = 1;
            ps.setObject(p++, secretaryId);
            ps.setString(p++, yearMonth);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    r.setUnapproved(rs.getInt("unapproved_count"));
                    r.setApproved(rs.getInt("approved_count")); 
                    r.setRemanded(rs.getInt("remanded_count"));
                    r.setTotal(rs.getInt("total_count"));
                    r.setTotalAmountAll(rs.getBigDecimal("total_amount_all"));
                    r.setTotalAmountApproved(rs.getBigDecimal("total_amount_approved"));
                }
            }
        } catch (SQLException e) {
        	e.printStackTrace();
            throw new DAOException("E:TS21 タスク件数の集計に失敗しました。", e);
        }
        return r;
    }
	
	public int clearRemandedAt(UUID taskId) {
	    final String sql =
	        "UPDATE tasks " +
	        "   SET remanded_at = NULL, updated_at = NOW() " +
	        " WHERE id = ? AND deleted_at IS NULL";
	    try (PreparedStatement ps = conn.prepareStatement(sql)) {
	        ps.setObject(1, taskId);
	        return ps.executeUpdate();
	    } catch (SQLException e) {
	        throw new RuntimeException("E:TASK-CLEAR-REMANDED_AT 失敗", e);
	    }
	}
}
