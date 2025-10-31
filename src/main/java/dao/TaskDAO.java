package dao;

import java.lang.reflect.Method;
import java.math.BigDecimal;
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

/**
 * タスク（tasks）に関するデータアクセスを担うDAO。
 *
 * 責務：
 * - タスクの検索（秘書／顧客／月別、状態別、キーワード条件）
 * - タスクの単一取得
 * - タスクの登録・更新・論理削除・承認/承認取消/差戻し
 * - ダッシュボード用の月次集計（admin / secretary / customer）
 * - 顧客×月の承認済み金額集計
 *
 * 設計メモ：
 * - 本DAOは呼び出し側から渡される {@link Connection} に依存（トランザクション境界は呼び出し側が管理）
 * - DB例外は {@link DAOException} にラップして送出
 * - JOIN先のカラムはエイリアス（t_*, a_*, tr_*, c_*, s_*）を付与し混同を防止
 */
public class TaskDAO extends BaseDAO {

	/** ========================
	 * ① フィールド（SQL 定義）
	 * ======================== */

	/**
	 * 秘書ID・顧客ID・年月(YYYY-MM)でタスク取得（tasks + assignments + task_rank）。
	 * 並び順：t.start_time
	 */
	private static final String SQL_SELECT_BY_SEC_CUST_MONTH = "SELECT "
			/** ---- tasks.* と差戻し情報 ---- */
			+ "  t.id AS t_id, t.assignment_id AS t_assignment_id, t.work_date AS t_work_date, "
			+ "  t.start_time AS t_start_time, t.end_time AS t_end_time, t.work_minute AS t_work_minute, "
			+ "  t.work_content AS t_work_content, t.approved_at AS t_approved_at, "
			+ "  t.approved_by AS t_approved_by, t.customer_monthly_invoice_id AS t_customer_monthly_invoice_id, "
			+ "  t.secretary_monthly_summary_id AS t_secretary_monthly_summary_id, t.created_at AS t_created_at, "
			+ "  t.updated_at AS t_updated_at, t.deleted_at AS t_deleted_at, "
			+ "  t.remanded_at AS t_remanded_at, t.remanded_by AS t_remanded_by, t.remand_comment AS t_remand_comment, "
			/** ---- assignments.* ---- */
			+ "  a.id AS a_id, a.customer_id AS a_customer_id, a.secretary_id AS a_secretary_id, "
			+ "  a.task_rank_id AS a_task_rank_id, a.target_year_month AS a_target_year_month, "
			+ "  a.base_pay_customer AS a_base_pay_customer, a.base_pay_secretary AS a_base_pay_secretary, "
			+ "  a.increase_base_pay_customer AS a_increase_base_pay_customer, "
			+ "  a.increase_base_pay_secretary AS a_increase_base_pay_secretary, "
			+ "  a.customer_based_incentive_for_customer AS a_cust_incentive_for_customer, "
			+ "  a.customer_based_incentive_for_secretary AS a_cust_incentive_for_secretary, "
			+ "  a.status AS a_status, a.created_at AS a_created_at, a.updated_at AS a_updated_at, a.deleted_at AS a_deleted_at, "
			/** ---- task_rank ---- */
			+ "  tr.rank_name AS tr_rank_name "
			+ "FROM tasks t "
			+ "JOIN assignments a ON a.id = t.assignment_id AND a.deleted_at IS NULL "
			+ "LEFT JOIN task_rank tr ON tr.id = a.task_rank_id AND tr.deleted_at IS NULL "
			+ "WHERE a.secretary_id = ? "
			+ "  AND a.customer_id  = ? "
			+ "  AND a.target_year_month = ? "
			+ "  AND t.deleted_at IS NULL "
			+ "ORDER BY t.start_time";

	/**
	 * 月別検索の共通ベース（WHERE: target_year_month = ? / t.deleted_at IS NULL）。
	 * ORDER BY は呼び出し側で付与。
	 */
	private static final String SQL_SELECT_BY_MONTH_BASE = "SELECT "
			+ "  t.id AS t_id, t.assignment_id AS t_assignment_id, t.work_date AS t_work_date, "
			+ "  t.start_time AS t_start_time, t.end_time AS t_end_time, t.work_minute AS t_work_minute, "
			+ "  t.work_content AS t_work_content, t.approved_at AS t_approved_at, t.approved_by AS t_approved_by, "
			+ "  t.customer_monthly_invoice_id AS t_customer_monthly_invoice_id, "
			+ "  t.secretary_monthly_summary_id AS t_secretary_monthly_summary_id, "
			+ "  a.id AS a_id, a.customer_id AS a_customer_id, a.secretary_id AS a_secretary_id, "
			+ "  a.task_rank_id AS a_task_rank_id, a.target_year_month AS a_target_year_month, "
			+ "  a.base_pay_customer AS a_base_pay_customer, a.base_pay_secretary AS a_base_pay_secretary, "
			+ "  a.increase_base_pay_customer AS a_increase_base_pay_customer, "
			+ "  a.increase_base_pay_secretary AS a_increase_base_pay_secretary, "
			+ "  a.customer_based_incentive_for_customer AS a_cust_incentive_for_customer, "
			+ "  a.customer_based_incentive_for_secretary AS a_cust_incentive_for_secretary, "
			+ "  a.base_pay_customer + a.increase_base_pay_customer + a.customer_based_incentive_for_customer AS a_all_pay_customer, "
			+ "  a.base_pay_secretary + a.increase_base_pay_secretary + a.customer_based_incentive_for_secretary AS a_all_pay_secretary, "
			+ "  a.status AS a_status, tr.rank_name AS tr_rank_name, c.company_name AS c_company_name, s.name AS s_name, "
			+ "  t.remanded_at AS t_remanded_at, t.remanded_by AS t_remanded_by, t.remand_comment AS t_remand_comment "
			+ "FROM tasks t "
			+ "JOIN assignments a ON a.id = t.assignment_id AND a.deleted_at IS NULL "
			+ "LEFT JOIN task_rank tr ON tr.id = a.task_rank_id AND tr.deleted_at IS NULL "
			+ "INNER JOIN customers c ON a.customer_id = c.id AND c.deleted_at IS NULL "
			+ "INNER JOIN secretaries s ON s.id = a.secretary_id AND s.deleted_at IS NULL "
			+ "WHERE a.target_year_month = ? AND t.deleted_at IS NULL ";

	/** 月別 + 秘書ID 条件のベース（ORDER BY は呼び出し側） */
	private static final String SQL_SELECT_BY_SEC_MONTH_BASE = SQL_SELECT_BY_MONTH_BASE + " AND a.secretary_id = ? ";

	/** 月次ステータス集計（秘書×月） */
	private static final String SQL_COUNT_BY_STATUS_SECRETARY = "SELECT "
			+ "  SUM(CASE WHEN t.approved_at IS NULL AND t.remanded_at IS NULL THEN 1 ELSE 0 END) AS unapproved_count, "
			+ "  SUM(CASE WHEN t.approved_at IS NOT NULL THEN 1 ELSE 0 END)                       AS approved_count, "
			+ "  SUM(CASE WHEN t.remanded_at IS NOT NULL THEN 1 ELSE 0 END)                       AS remanded_count, "
			+ "  COUNT(*)                                                                          AS total_count, "
			+ "  COALESCE(SUM( "
			+ "    (COALESCE(a.base_pay_secretary,0) + COALESCE(a.increase_base_pay_secretary,0) + COALESCE(a.customer_based_incentive_for_secretary,0)) "
			+ "    * (COALESCE(t.work_minute,0)::numeric / 60) "
			+ "  ),0) AS total_amount_all, "
			+ "  COALESCE(SUM(CASE WHEN t.approved_at IS NOT NULL THEN "
			+ "    (COALESCE(a.base_pay_secretary,0) + COALESCE(a.increase_base_pay_secretary,0) + COALESCE(a.customer_based_incentive_for_secretary,0)) "
			+ "    * (COALESCE(t.work_minute,0)::numeric / 60) "
			+ "  ELSE 0 END),0) AS total_amount_approved "
			+ "FROM tasks t "
			+ "JOIN assignments a ON a.id = t.assignment_id AND a.deleted_at IS NULL "
			+ "WHERE t.deleted_at IS NULL "
			+ "  AND a.secretary_id = ? "
			+ "  AND a.target_year_month = ?";

	/** 月次ステータス集計（管理者：全体×月） */
	private static final String SQL_COUNT_BY_STATUS_ADMIN = "SELECT "
			+ "  SUM(CASE WHEN t.approved_at IS NULL AND t.remanded_at IS NULL THEN 1 ELSE 0 END) AS unapproved_count, "
			+ "  SUM(CASE WHEN t.approved_at IS NOT NULL THEN 1 ELSE 0 END)                       AS approved_count, "
			+ "  SUM(CASE WHEN t.remanded_at IS NOT NULL THEN 1 ELSE 0 END)                       AS remanded_count, "
			+ "  COUNT(*)                                                                          AS total_count, "
			+ "  COALESCE(SUM( "
			+ "    (COALESCE(a.base_pay_secretary,0) + COALESCE(a.increase_base_pay_secretary,0) + COALESCE(a.customer_based_incentive_for_secretary,0)) "
			+ "    * (COALESCE(t.work_minute,0)::numeric / 60) "
			+ "  ),0) AS total_amount_all, "
			+ "  COALESCE(SUM(CASE WHEN t.approved_at IS NOT NULL THEN "
			+ "    (COALESCE(a.base_pay_secretary,0) + COALESCE(a.increase_base_pay_secretary,0) + COALESCE(a.customer_based_incentive_for_secretary,0)) "
			+ "    * (COALESCE(t.work_minute,0)::numeric / 60) "
			+ "  ELSE 0 END),0) AS total_amount_approved "
			+ "FROM tasks t "
			+ "JOIN assignments a ON a.id = t.assignment_id AND a.deleted_at IS NULL "
			+ "WHERE t.deleted_at IS NULL "
			+ "  AND a.target_year_month = ?";

	/** 月次ステータス集計（顧客×月：金額は顧客単価系で算出） */
	private static final String SQL_COUNT_BY_STATUS_CUSTOMER = "SELECT "
			+ "  SUM(CASE WHEN t.approved_at IS NULL AND t.remanded_at IS NULL THEN 1 ELSE 0 END) AS unapproved_count, "
			+ "  SUM(CASE WHEN t.approved_at IS NOT NULL THEN 1 ELSE 0 END)                       AS approved_count, "
			+ "  SUM(CASE WHEN t.remanded_at IS NOT NULL THEN 1 ELSE 0 END)                       AS remanded_count, "
			+ "  COUNT(*)                                                                          AS total_count, "
			+ "  COALESCE(SUM( "
			+ "    (COALESCE(a.base_pay_customer,0) + COALESCE(a.increase_base_pay_customer,0) + COALESCE(a.customer_based_incentive_for_customer,0)) "
			+ "    * (COALESCE(t.work_minute,0)::numeric / 60) "
			+ "  ),0) AS total_amount_all, "
			+ "  COALESCE(SUM(CASE WHEN t.approved_at IS NOT NULL THEN "
			+ "    (COALESCE(a.base_pay_customer,0) + COALESCE(a.increase_base_pay_customer,0) + COALESCE(a.customer_based_incentive_for_customer,0)) "
			+ "    * (COALESCE(t.work_minute,0)::numeric / 60) "
			+ "  ELSE 0 END),0) AS total_amount_approved "
			+ "FROM tasks t "
			+ "JOIN assignments a ON a.id = t.assignment_id AND a.deleted_at IS NULL "
			+ "WHERE t.deleted_at IS NULL "
			+ "  AND a.customer_id = ? "
			+ "  AND a.target_year_month = ?";

	/** 今月の顧客向け承認済み金額（顧客単価×時間・approvedのみ） */
	static final String SQL_APPROVED_AMOUNT_BY_CUSTOMER_MONTH = "SELECT COALESCE(SUM( "
			+ "  (COALESCE(a.base_pay_customer,0) + COALESCE(a.increase_base_pay_customer,0) + COALESCE(a.customer_based_incentive_for_customer,0)) "
			+ "  * (COALESCE(t.work_minute,0)::numeric / 60) "
			+ "),0) AS amount "
			+ "FROM tasks t "
			+ "JOIN assignments a ON a.id = t.assignment_id AND a.deleted_at IS NULL "
			+ "WHERE t.deleted_at IS NULL "
			+ "  AND a.customer_id = ? "
			+ "  AND a.target_year_month = ? "
			+ "  AND t.approved_at IS NOT NULL";

	/** tasks INSERT（RETURNING id, created_at, updated_at） */
	private static final String SQL_INSERT = "INSERT INTO tasks ("
			+ "  assignment_id, work_date, start_time, end_time, work_minute, work_content, "
			+ "  approved_at, approved_by, customer_monthly_invoice_id, secretary_monthly_summary_id"
			+ ") VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id, created_at, updated_at";

	/** tasks UPDATE（論理未削除のみ） */
	private static final String SQL_UPDATE = "UPDATE tasks SET "
			+ "  assignment_id = ?, "
			+ "  work_date = ?, "
			+ "  start_time = ?, "
			+ "  end_time = ?, "
			+ "  work_minute = ?, "
			+ "  work_content = ?, "
			+ "  approved_at = ?, "
			+ "  approved_by = ?, "
			+ "  customer_monthly_invoice_id = ?, "
			+ "  secretary_monthly_summary_id = ?, "
			+ "  updated_at = CURRENT_TIMESTAMP "
			+ "WHERE id = ? AND deleted_at IS NULL";

	/** tasks 論理DELETE（deleted_at を現在時刻に） */
	private static final String SQL_DELETE_LOGICAL = "UPDATE tasks SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL";

	/** 単一取得用SQL（tasks + assignments + task_rank） */
	private static final String SQL_SELECT_BY_ID = "SELECT "
			+ "  t.id AS t_id, t.assignment_id AS t_assignment_id, t.work_date AS t_work_date, "
			+ "  t.start_time AS t_start_time, t.end_time AS t_end_time, t.work_minute AS t_work_minute, "
			+ "  t.work_content AS t_work_content, t.approved_at AS t_approved_at, t.approved_by AS t_approved_by, "
			+ "  t.customer_monthly_invoice_id AS t_customer_monthly_invoice_id, "
			+ "  t.secretary_monthly_summary_id AS t_secretary_monthly_summary_id, "
			+ "  t.created_at AS t_created_at, t.updated_at AS t_updated_at, t.deleted_at AS t_deleted_at, "
			+ "  a.id AS a_id, a.customer_id AS a_customer_id, a.secretary_id AS a_secretary_id, "
			+ "  a.task_rank_id AS a_task_rank_id, a.target_year_month AS a_target_year_month, "
			+ "  a.base_pay_customer AS a_base_pay_customer, a.base_pay_secretary AS a_base_pay_secretary, "
			+ "  a.increase_base_pay_customer AS a_increase_base_pay_customer, "
			+ "  a.increase_base_pay_secretary AS a_increase_base_pay_secretary, "
			+ "  a.customer_based_incentive_for_customer AS a_cust_incentive_for_customer, "
			+ "  a.customer_based_incentive_for_secretary AS a_cust_incentive_for_secretary, "
			+ "  a.status AS a_status, a.created_at AS a_created_at, a.updated_at AS a_updated_at, a.deleted_at AS a_deleted_at, "
			+ "  tr.rank_name AS tr_rank_name "
			+ "FROM tasks t "
			+ "JOIN assignments a ON a.id = t.assignment_id AND a.deleted_at IS NULL "
			+ "LEFT JOIN task_rank tr ON tr.id = a.task_rank_id AND tr.deleted_at IS NULL "
			+ "WHERE t.id = ? AND t.deleted_at IS NULL";

	/** 顧客向けタスク一覧（秘書名・ランク名・単価を含む） */
	private static final String SQL_SELECT_CUSTOMER_TASKS_FOR_LIST = "SELECT t.id                               AS task_id, "
			+
			"       t.work_date                        AS work_date, " +
			"       t.start_time                       AS start_time, " +
			"       t.end_time                         AS end_time, " +
			"       t.work_minute                      AS work_minute, " +
			"       t.work_content                     AS work_content, " +
			"       t.alerted_at                     AS alerted_at, " +
			"       t.alerted_comment                     AS alerted_comment, " +
			"  t.remanded_by AS t_remanded_by," +
			"  t.remanded_at AS t_remanded_at," +
			"  t.remand_comment AS t_remand_comment, " +
			"       s.name                             AS secretary_name, " +
			"       tr.rank_name                       AS rank_name, " +
			"       " + /** ===== 単価（顧客向け）===== */
			"       ( COALESCE(a.base_pay_customer, 0) " +
			"       + COALESCE(a.increase_base_pay_customer, 0) " +
			"       + COALESCE(a.customer_based_incentive_for_customer, 0) ) AS unit_price_customer, " +
			"       " + /** ===== コスト（単価 × 分/60）===== */
			"       ( ( COALESCE(a.base_pay_customer, 0) " +
			"         + COALESCE(a.increase_base_pay_customer, 0) " +
			"         + COALESCE(a.customer_based_incentive_for_customer, 0) ) " +
			"         * (COALESCE(t.work_minute,0)::numeric / 60) " +
			"       ) AS cost_customer " +
			"  FROM tasks t " +
			"  JOIN assignments a    ON a.id = t.assignment_id AND a.deleted_at IS NULL " +
			"  JOIN secretaries s    ON s.id = a.secretary_id  AND s.deleted_at IS NULL " +
			"  LEFT JOIN task_rank tr ON tr.id = a.task_rank_id AND tr.deleted_at IS NULL " +
			" WHERE t.deleted_at IS NULL " +
			"   AND a.customer_id = ? " +
			"   AND t.work_date >= to_date(?, 'YYYY-MM') " + /** ym の月初 */
			"   AND t.work_date <  (to_date(?, 'YYYY-MM') + INTERVAL '1 month') " + /** 翌月月初 */
			" ORDER BY t.work_date DESC, t.start_time DESC ";

	/** 顧客の確認申請を記録（alerted_at と alerted_comment を更新） */
	private static final String SQL_ALERT = "UPDATE tasks SET alerted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP, alerted_comment = ? WHERE id = ?";

	/** 顧客からのアラート一覧取得用 */
	private static final String SQL_SELECT_ALERT_LIST = "SELECT "
			+ "  t.id AS t_id, "
			+ "  c.company_name AS c_company_name, "
			+ "  s.name AS s_name, "
			+ "  t.work_date AS t_work_date, "
			+ "  t.start_time AS t_start_time, "
			+ "  t.end_time AS t_end_time, "
			+ "  t.work_minute AS t_work_minute, "
			+ "  tr.rank_name AS tr_rank_name, "
			+ "  t.work_content AS t_work_content, "
			+ "  t.alerted_at AS t_alerted_at, "
			+ "  t.alerted_comment AS t_alerted_comment,"
			+ "  t.remanded_by AS t_remanded_by,"
			+ "  t.remanded_at AS t_remanded_at,"
			+ "  t.remand_comment AS t_remand_comment "
			+ "FROM tasks t "
			+ "INNER JOIN assignments a ON t.assignment_id = a.id AND a.deleted_at IS NULL "
			+ "INNER JOIN secretaries s ON s.id = a.secretary_id AND s.deleted_at IS NULL "
			+ "INNER JOIN customers  c ON c.id = a.customer_id AND c.deleted_at IS NULL "
			+ "INNER JOIN task_rank tr ON a.task_rank_id = tr.id AND tr.deleted_at IS NULL "
			+ "WHERE t.alerted_at IS NOT NULL AND t.deleted_at IS NULL "
			+ "ORDER BY t.alerted_at";
	
	/** アラート取消（alerted_at を NULL, updated_at を現在時刻） */
	private static final String SQL_ALERT_DELETE =
	    "UPDATE tasks SET alerted_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

	/** ========================
	 * ② フィールド／コンストラクタ
	 * ======================== */

	/**
	 * コンストラクタ。
	 *
	 * @param conn 呼び出し側が管理するDBコネクション
	 */
	public TaskDAO(Connection conn) {
		super(conn);
	}

	/** ========================
	 * ③ メソッド（アクター別）
	 * ---------- secretary 用 ----------
	 * =========================
	 * SELECT
	 * ========================= */

	/**
	 * 秘書 × 顧客 × 年月でタスク一覧を取得します。
	 * tasks／assignments／task_rank を結合し、開始時刻で昇順に並べます。
	 *
	 * @param secretaryId 秘書ID
	 * @param customerId  顧客ID
	 * @param yearMonth   対象年月（"YYYY-MM"）
	 * @return 該当 {@link TaskDTO} のリスト（0件時は空）
	 * @throws DAOException DBアクセスに失敗した場合
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

					/** tasks.* */
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

					// approved_by
					UUID approvedById = rs.getObject("t_approved_by", UUID.class);
					if (approvedById != null) {
						SecretaryDTO s = new SecretaryDTO();
						s.setId(approvedById);
						t.setApprovedBy(s);
					}

					// 月締テーブルの参照ID（null許容）
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

					/** assignments.* */
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
					ad.setTaskRankName(rs.getString("tr_rank_name"));

					/** 差戻し情報 */
					t.setRemandedAt(rs.getTimestamp("t_remanded_at"));
					t.setRemandedBy(rs.getObject("t_remanded_by", UUID.class));
					t.setRemandComment(rs.getString("t_remand_comment"));

					list.add(t);
				}
			}
		} catch (SQLException e) {
			throw new DAOException("E:TS11 tasks 取得に失敗しました。", e);
		}
		return list;
	}

	/**
	 * 秘書 × 年月（＋状態）でタスク一覧を取得します。
	 * 状態は {@code approved|unapproved|remanded|all} を受け付けます。
	 *
	 * @param secretaryId 秘書ID
	 * @param yearMonth   対象年月（"YYYY-MM"）
	 * @param status      状態フィルタ（null/空は all 扱い）
	 * @return タスク一覧（会社名→日付→開始時刻で昇順）
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public List<TaskDTO> selectBySecretaryAndMonth(UUID secretaryId, String yearMonth, String status) {
		StringBuilder sql = new StringBuilder(SQL_SELECT_BY_SEC_MONTH_BASE);
		/** 状態による絞り込み */
		switch (status == null ? "all" : status) {
		case "approved" -> sql.append(" AND t.approved_at IS NOT NULL ");
		case "unapproved" -> sql.append(" AND t.approved_at IS NULL AND t.remanded_at IS NULL ");
		case "remanded" -> sql.append(" AND t.remanded_at IS NOT NULL ");
		default -> {
			/** all */ }
		}
		sql.append(" ORDER BY c.company_name, t.work_date, t.start_time ");

		List<TaskDTO> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			int p = 1;
			ps.setString(p++, yearMonth);
			ps.setObject(p++, secretaryId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					TaskDTO t = new TaskDTO();

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

					/** 合算時給（表示補助） */
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

	/** ---------- admin 用 ---------- */

	/**
	 * 対象年月のタスク一覧を取得します（管理者用、全体）。
	 * 内部の共通ベースSQL（{@code SQL_SELECT_BY_MONTH_BASE}）を使用し、日付→顧客→開始時刻で並べます。
	 *
	 * @param yearMonth 対象年月（"YYYY-MM"）
	 * @return {@link TaskDTO} のリスト
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public List<TaskDTO> selectByMonth(String yearMonth) {
		String sql = SQL_SELECT_BY_MONTH_BASE + " ORDER BY t.work_date, a.customer_id, t.start_time ";
		List<TaskDTO> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, yearMonth);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					TaskDTO t = new TaskDTO();

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
			throw new DAOException("E:TS11 tasks 取得に失敗しました。", e);
		}
		return list;
	}

	/**
	 * 対象年月のタスク一覧（状態フィルタつき）を取得します（管理者用、全体）。
	 *
	 * @param yearMonth 対象年月（"YYYY-MM"）
	 * @param status    状態（{@code approved|unapproved|remanded|all}）
	 * @return タスク一覧
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public List<TaskDTO> selectByMonth(String yearMonth, String status) {
		StringBuilder sql = new StringBuilder(SQL_SELECT_BY_MONTH_BASE);
		if ("approved".equals(status)) {
			sql.append(" AND t.approved_at IS NOT NULL ");
		} else if ("unapproved".equals(status)) {
			sql.append(" AND t.approved_at IS NULL ");
		} else if ("remanded".equals(status)) {
			sql.append(" AND t.remanded_at IS NOT NULL ");
		}
		sql.append(" ORDER BY t.work_date, a.customer_id, t.start_time ");

		List<TaskDTO> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			ps.setString(1, yearMonth);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					TaskDTO t = new TaskDTO();

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
			throw new DAOException("E:TS11 tasks 取得に失敗しました。", e);
		}
		return list;
	}

	/**
	 * 対象年月のタスク一覧（状態＋キーワード：秘書名/会社名）を取得します（管理者用、全体）。
	 * 文字列条件は部分一致・大文字小文字無視（ILIKE）。
	 *
	 * @param yearMonth         対象年月（"YYYY-MM"）
	 * @param status            状態（approved|unapproved|remanded|all）
	 * @param secretaryNameLike 秘書名の部分一致（null/空で無効）
	 * @param customerNameLike  会社名の部分一致（null/空で無効）
	 * @return タスク一覧
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public List<TaskDTO> selectByMonth(String yearMonth, String status,
			String secretaryNameLike, String customerNameLike) {
		StringBuilder sql = new StringBuilder(SQL_SELECT_BY_MONTH_BASE);

		/** 状態フィルタ */
		switch (status) {
		case "approved" -> sql.append(" AND t.approved_at IS NOT NULL ");
		case "unapproved" -> sql.append(" AND t.approved_at IS NULL ");
		case "remanded" -> sql.append(" AND t.remanded_at IS NOT NULL ");
		default -> {
			/** all */ }
		}

		/** 文字列フィルタ */
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
			throw new DAOException("E:TS11 tasks 取得に失敗しました。", e);
		}
		return list;
	}

	/** =========================
	 * 単一取得
	 * ========================= */

	/**
	 * tasks.id で単一タスクを取得します（論理未削除のみ）。
	 *
	 * @param taskId タスクID
	 * @return 該当 {@link TaskDTO}（なければ {@code null}）
	 * @throws DAOException 引数未設定またはDBアクセスに失敗した場合
	 */
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

				/** tasks.* */
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

				/** assignments.* */
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

				/** task_rank */
				ad.setTaskRankName(rs.getString("tr_rank_name"));

				return t;
			}
		} catch (SQLException e) {
			throw new DAOException("E:TS12 tasks 単一取得に失敗しました。", e);
		}
	}

	/** =========================
	 * SELECT（customer 用）
	 * ========================= */

	/**
	 * 顧客向けタスク一覧を取得します（最新順）。
	 * - 単価 = assignments.base_pay_customer + assignments.increase_base_pay_customer + assignments.customer_based_incentive_for_customer
	 * - コスト = 単価 × tasks.work_minute / 60（四捨五入、整数円）
	 * - 戻り値は List&lt;TaskDTO&gt;
	 *
	 * @param customerId 顧客ID
	 * @param ym         対象年月（YYYY-MM）
	 * @return タスクDTOのリスト
	 * @throws DAOException 取得時エラー
	 */
	public List<TaskDTO> selectCustomerTasksForList(UUID customerId, String ym) {

		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_CUSTOMER_TASKS_FOR_LIST)) {

			int i = 1;
			ps.setObject(i++, customerId);
			ps.setString(i++, ym);
			ps.setString(i++, ym);

			List<TaskDTO> list = new ArrayList<>();
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					TaskDTO d = new TaskDTO();
					d.setId(rs.getObject("task_id", UUID.class));
					d.setWorkDate(rs.getDate("work_date")); /** java.sql.Date */
					d.setStartTime(rs.getTimestamp("start_time")); /** java.sql.Timestamp */
					d.setEndTime(rs.getTimestamp("end_time")); /** java.sql.Timestamp */

					Integer mins = rs.getObject("work_minute") == null ? null : rs.getInt("work_minute");
					d.setWorkMinute(mins);

					d.setWorkContent(rs.getString("work_content"));
					d.setSecretaryName(rs.getString("secretary_name"));
					d.setAlertedAt(rs.getTimestamp("alerted_at"));
					d.setAlertComment(rs.getString("alerted_comment"));

		            d.setRemandedBy(rs.getObject("t_remanded_by", UUID.class));
		            d.setRemandedAt(rs.getTimestamp("t_remanded_at"));
		            d.setRemandComment(rs.getString("t_remand_comment"));
					AssignmentDTO adto = new AssignmentDTO();
					adto.setSecretaryName(rs.getString("secretary_name"));
					adto.setTaskRankName(rs.getString("rank_name"));

					BigDecimal unitPrice = rs.getBigDecimal("unit_price_customer");
					if (unitPrice == null)
						unitPrice = BigDecimal.ZERO;
					adto.setHourlyPayCustomer(unitPrice);
					d.setAssignment(adto);

					/** TaskDTO にコスト用フィールドがある場合（例：setCostCustomer） */
					try {
						Method m = TaskDTO.class.getMethod("setCostCustomer", BigDecimal.class);
						m.invoke(d, rs.getBigDecimal("cost_customer"));
					} catch (NoSuchMethodException ignore) {
						/** フィールドが無い場合は JSP 側で算出（EL）する前提 */
					} catch (Exception ex) {
						throw new SQLException("TaskDTO への costCustomer セットに失敗", ex);
					}

					list.add(d);
				}
			}
			return list;

		} catch (SQLException e) {
			throw new DAOException("E:TS-CUST-LIST 顧客向けタスク一覧の取得に失敗しました。", e);
		}
	}

	/** =========================
	 * INSERT / UPDATE / DELETE
	 * ========================= */

	/**
	 * タスクを新規登録します。
	 * 必須：assignment.id, workDate, startTime, endTime, workMinute, workContent
	 * 任意：approvedAt/By, customerMonthlyInvoiceId, secretaryMonthlySummaryId
	 *
	 * @param dto 登録対象 {@link TaskDTO}
	 * @return 採番された {@code tasks.id}
	 * @throws DAOException 入力不足またはDBアクセスに失敗した場合
	 */
	public UUID insert(TaskDTO dto) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
			int i = 1;

			/** assignment_id（必須） */
			UUID assignmentId = (dto.getAssignment() != null) ? dto.getAssignment().getAssignmentId() : null;
			if (assignmentId == null) {
				throw new DAOException("E:TS21 assignmentId が未設定です。");
			}
			ps.setObject(i++, assignmentId);

			/** work_date / start_time / end_time / work_minute / work_content（必須） */
			if (dto.getWorkDate() == null)
				throw new DAOException("E:TS22 workDate が未設定です。");
			ps.setDate(i++, new java.sql.Date(dto.getWorkDate().getTime()));

			if (dto.getStartTime() == null || dto.getEndTime() == null) {
				throw new DAOException("E:TS22 startTime / endTime が未設定です。");
			}
			ps.setTimestamp(i++, dto.getStartTime());
			ps.setTimestamp(i++, dto.getEndTime());

			if (dto.getWorkMinute() == null)
				throw new DAOException("E:TS23 workMinute が未設定です。");
			ps.setInt(i++, dto.getWorkMinute());

			ps.setString(i++, dto.getWorkContent());

			/** 任意（NULL許容） */
			if (dto.getApprovedAt() != null)
				ps.setTimestamp(i++, dto.getApprovedAt());
			else
				ps.setNull(i++, Types.TIMESTAMP);
			if (dto.getApprovedBy() != null && dto.getApprovedBy().getId() != null)
				ps.setObject(i++, dto.getApprovedBy().getId());
			else
				ps.setNull(i++, Types.OTHER);
			if (dto.getCustomerMonthlyInvoice() != null && dto.getCustomerMonthlyInvoice().getId() != null)
				ps.setObject(i++, dto.getCustomerMonthlyInvoice().getId());
			else
				ps.setNull(i++, Types.OTHER);
			if (dto.getSecretaryMonthlySummary() != null && dto.getSecretaryMonthlySummary().getId() != null)
				ps.setObject(i++, dto.getSecretaryMonthlySummary().getId());
			else
				ps.setNull(i++, Types.OTHER);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getObject(1, UUID.class); /** RETURNING id */
				}
				return null;
			}
		} catch (SQLException e) {
			throw new DAOException("E:TS24 tasks INSERT に失敗しました。", e);
		}
	}

	/**
	 * タスクを更新します（論理未削除のみ）。
	 * 必須：id, assignment.id, workDate, startTime, endTime, workMinute, workContent
	 * 任意：approvedAt/By, customerMonthlyInvoiceId, secretaryMonthlySummaryId
	 *
	 * @param dto 更新対象 {@link TaskDTO}
	 * @return 影響行数（0 の場合は未更新）
	 * @throws DAOException 入力不足またはDBアクセスに失敗した場合
	 */
	public int update(TaskDTO dto) {
		if (dto.getId() == null)
			throw new DAOException("E:TS31 id が未設定です。");
		UUID assignmentId = (dto.getAssignment() != null) ? dto.getAssignment().getAssignmentId() : null;
		if (assignmentId == null)
			throw new DAOException("E:TS32 assignmentId が未設定です。");
		if (dto.getWorkDate() == null)
			throw new DAOException("E:TS33 workDate が未設定です。");
		if (dto.getStartTime() == null || dto.getEndTime() == null)
			throw new DAOException("E:TS34 startTime/endTime が未設定です。");
		if (dto.getWorkMinute() == null)
			throw new DAOException("E:TS35 workMinute が未設定です。");
		if (dto.getWorkContent() == null)
			throw new DAOException("E:TS36 workContent が未設定です。");

		try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
			int i = 1;

			/** 必須 */
			ps.setObject(i++, assignmentId);
			ps.setDate(i++, new java.sql.Date(dto.getWorkDate().getTime()));
			ps.setTimestamp(i++, dto.getStartTime());
			ps.setTimestamp(i++, dto.getEndTime());
			ps.setInt(i++, dto.getWorkMinute());
			ps.setString(i++, dto.getWorkContent());

			/** 任意（NULL可） */
			if (dto.getApprovedAt() != null)
				ps.setTimestamp(i++, dto.getApprovedAt());
			else
				ps.setNull(i++, Types.TIMESTAMP);
			if (dto.getApprovedBy() != null && dto.getApprovedBy().getId() != null)
				ps.setObject(i++, dto.getApprovedBy().getId());
			else
				ps.setNull(i++, Types.OTHER);
			if (dto.getCustomerMonthlyInvoice() != null && dto.getCustomerMonthlyInvoice().getId() != null)
				ps.setObject(i++, dto.getCustomerMonthlyInvoice().getId());
			else
				ps.setNull(i++, Types.OTHER);
			if (dto.getSecretaryMonthlySummary() != null && dto.getSecretaryMonthlySummary().getId() != null)
				ps.setObject(i++, dto.getSecretaryMonthlySummary().getId());
			else
				ps.setNull(i++, Types.OTHER);

			// WHERE
			ps.setObject(i++, dto.getId());

			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:TS37 tasks UPDATE に失敗しました。", e);
		}
	}

	/**
	 * タスクを論理削除します（{@code deleted_at = CURRENT_TIMESTAMP}）。
	 *
	 * @param id tasks.id
	 * @return 影響行数（0 の場合は未削除）
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public int delete(UUID id) {
		if (id == null)
			throw new DAOException("E:TS41 id が未設定です。");
		try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_LOGICAL)) {
			ps.setObject(1, id);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:TS42 tasks 論理DELETE に失敗しました。", e);
		}
	}
	
	/**
	 * 顧客からのアラート一覧を取得します。
	 * 引数なし／List&lt;TaskDTO&gt; 返却。
	 *
	 * @param flg 10件制限フラグ（true の場合は LIMIT 10）
	 * @return アラート一覧（TaskDTOのリスト）
	 * @throws DAOException 取得に失敗した場合
	 */
	public List<TaskDTO> showAlert(boolean flg) {
	    List<TaskDTO> list = new ArrayList<>();
	    String sql = SQL_SELECT_ALERT_LIST + (flg ? " LIMIT 10" : ""); 
	    try (PreparedStatement ps = conn.prepareStatement(sql);
	         ResultSet rs = ps.executeQuery()) {
	        while (rs.next()) {
	            TaskDTO t = new TaskDTO();
	            t.setId(rs.getObject("t_id", UUID.class));
	            t.setWorkDate(rs.getDate("t_work_date"));
	            t.setStartTime(rs.getTimestamp("t_start_time"));
	            t.setEndTime(rs.getTimestamp("t_end_time"));
	            t.setWorkMinute(rs.getObject("t_work_minute", Integer.class));
	            t.setWorkContent(rs.getString("t_work_content"));
	            t.setAlertedAt(rs.getTimestamp("t_alerted_at"));
	            t.setAlertComment(rs.getString("t_alerted_comment"));
	            t.setRemandedBy(rs.getObject("t_remanded_by", UUID.class));
	            t.setRemandedAt(rs.getTimestamp("t_remanded_at"));
	            t.setRemandComment(rs.getString("t_remand_comment"));

	            /** 会社名・ランクは AssignmentDTO 側で保持 */
	            AssignmentDTO ad = new AssignmentDTO();
	            ad.setCustomerCompanyName(rs.getString("c_company_name"));
	            ad.setTaskRankName(rs.getString("tr_rank_name"));
	            ad.setSecretaryName(rs.getString("s_name"));
	            t.setAssignment(ad);

	            list.add(t);
	        }
	    } catch (SQLException e) {
	        throw new DAOException("E:TS-ALERT-LIST 顧客アラート一覧の取得に失敗しました。", e);
	    }
	    return list;
	}

	/** =========================
	 * ステータス操作（承認 / 取消 / 差戻し）
	 * ========================= */

	/**
	 * タスクを承認します（未承認のみ対象）。
	 *
	 * @param taskId     タスクID
	 * @param approvedBy 承認者（null可）
	 * @return 影響行数（1:成功, 0:既に承認済み or 不在）
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public int approve(UUID taskId, UUID approvedBy) {
		final String sql = "UPDATE tasks "
				+ "   SET approved_at = NOW(), approved_by = ?, updated_at = NOW() "
				+ " WHERE id = ? AND deleted_at IS NULL AND approved_at IS NULL";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			if (approvedBy != null)
				ps.setObject(1, approvedBy);
			else
				ps.setNull(1, Types.OTHER);
			ps.setObject(2, taskId);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:TASK-APPROVE 更新に失敗", e);
		}
	}

	/**
	 * タスクの承認を取り消します（承認前の状態へ戻す）。
	 *
	 * @param taskId タスクID
	 * @return 影響行数
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public int unapprove(UUID taskId) {
		final String sql = "UPDATE tasks "
				+ "   SET approved_at = NULL, approved_by = NULL, updated_at = NOW() "
				+ " WHERE id = ? AND deleted_at IS NULL";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setObject(1, taskId);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:TASK-UNAPPROVE 失敗", e);
		}
	}

	/**
	 * タスクを差戻しにします（承認情報はクリア）。
	 *
	 * @param taskId     タスクID
	 * @param remandedBy 差戻し実行者ID（null可）
	 * @param comment    差戻しコメント（null可）
	 * @return 影響行数
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public int remand(UUID taskId, UUID remandedBy, String comment) {
		final String sql = "UPDATE tasks "
				+ "   SET remanded_at = NOW(), remanded_by = ?, remand_comment = ?, "
				+ "       approved_at = NULL, approved_by = NULL, updated_at = NOW() "
				+ " WHERE id = ? AND deleted_at IS NULL";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			if (remandedBy != null)
				ps.setObject(1, remandedBy);
			else
				ps.setNull(1, Types.OTHER);
			ps.setString(2, comment);
			ps.setObject(3, taskId);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:TASK-REMAND 失敗", e);
		}
	}

	/**
	 * 差戻し日時（{@code remanded_at}）をクリアします。
	 *
	 * @param taskId タスクID
	 * @return 影響行数
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public int clearRemandedAt(UUID taskId) {
		final String sql = "UPDATE tasks "
				+ "   SET remanded_at = NULL, updated_at = NOW() "
				+ " WHERE id = ? AND deleted_at IS NULL";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setObject(1, taskId);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:TASK-CLEAR-REMANDED_AT 失敗", e);
		}
	}

	/** ---------- dashboard / summary 用 ---------- */

	/**
	 * （secretary 用）秘書 × 年月の件数・金額サマリを取得します。
	 * 未承認／承認済／差戻し件数、全体金額と承認済み金額を返します。
	 *
	 * @param secretaryId 秘書ID
	 * @param yearMonth   対象年月（"YYYY-MM"）
	 * @return サマリ値を格納した {@link TaskDTO}（unapproved/approved/remanded/total/totalAmountAll/totalAmountApproved）
	 * @throws DAOException DBアクセスに失敗した場合
	 */
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
			throw new DAOException("E:TS21 タスク件数の集計に失敗しました。", e);
		}
		return r;
	}

	/**
	 * （admin 用）全体 × 年月の件数・金額サマリを取得します。
	 *
	 * @param yearMonth 対象年月（"YYYY-MM"）
	 * @return サマリ値を格納した {@link TaskDTO}
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public TaskDTO selectCountsForAdminMonth(String yearMonth) {
		TaskDTO r = new TaskDTO();
		try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_BY_STATUS_ADMIN)) {
			ps.setString(1, yearMonth);
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
			throw new DAOException("E:TS22 管理者向けタスク件数の集計に失敗しました。", e);
		}
		return r;
	}

	/**
	 * （customer 用）顧客 × 年月の件数・金額サマリを取得します。
	 * 金額は顧客側の単価体系で算出します。
	 *
	 * @param customerId 顧客ID
	 * @param yearMonth  対象年月（"YYYY-MM"）
	 * @return サマリ値を格納した {@link TaskDTO}
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public TaskDTO selectCountsForCustomerMonth(UUID customerId, String yearMonth) {
		TaskDTO r = new TaskDTO();
		try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_BY_STATUS_CUSTOMER)) {
			ps.setObject(1, customerId);
			ps.setString(2, yearMonth);
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
			throw new DAOException("E:TS23 顧客×月タスク集計に失敗しました。", e);
		}
		return r;
	}

	/**
	 * （customer 用）顧客 × 年月の承認済み金額を取得します。
	 * 承認済み（approved_at IS NOT NULL）のみ合算します。
	 *
	 * @param customerId 顧客ID
	 * @param yearMonth  対象年月（"YYYY-MM"）
	 * @return 承認済み金額（該当なしは 0）
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public BigDecimal selectApprovedAmountForCustomerMonth(UUID customerId, String yearMonth) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_APPROVED_AMOUNT_BY_CUSTOMER_MONTH)) {
			ps.setObject(1, customerId);
			ps.setString(2, yearMonth);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getBigDecimal("amount");
			}
		} catch (SQLException e) {
			throw new DAOException("E:TS25 顧客×月の承認済み金額集計に失敗しました。", e);
		}
		return BigDecimal.ZERO; /** 行なしでも0を返す */
	}

	/**
	 * 顧客からの「確認申請（アラート）」を記録します。
	 *
	 * 以下のカラムを更新します：
	 * - {@code alerted_at} … 現在時刻（{@code CURRENT_TIMESTAMP}）
	 * - {@code alerted_comment} … 入力コメント（null 可。保存時そのまま指定）
	 *
	 * トランザクション：
	 * 本メソッドはコネクションに対して更新系 SQL を実行します。
	 * commit/rollback は呼び出し側（サービス層等）で管理してください。
	 *
	 * @param comment 入力コメント（null 可。null のまま保存されます）
	 * @param id      対象タスクID（UUID）
	 * @return 影響行数（通常 1。該当なしは 0）
	 * @throws DAOException DB アクセスに失敗した場合
	 */
	public int alert(String comment, UUID id) {
		if (id == null) {
			throw new DAOException("E:TS-ALERT 引数 id が未設定です。");
		}
		try (PreparedStatement ps = conn.prepareStatement(SQL_ALERT)) {
			/** alerted_comment */
			ps.setString(1, comment); /** null 指定も可：そのまま DB に入る */
			/** WHERE id = ? */
			ps.setObject(2, id);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:TS-ALERT 確認申請の更新に失敗しました。", e);
		}
	}
	
	/**
	 * アラートを取り消します（alerted_at を NULL にする）。
	 *
	 * @param id tasks.id（UUID）
	 * @return 影響行数（1 が通常）
	 * @throws DAOException 更新に失敗した場合
	 */
	public int alertDelete(UUID id) {
	    if (id == null) throw new DAOException("E:TS-ALERT-DEL 引数 id が未設定です。");
	    try (PreparedStatement ps = conn.prepareStatement(SQL_ALERT_DELETE)) {
	        ps.setObject(1, id);
	        return ps.executeUpdate();
	    } catch (SQLException e) {
	        throw new DAOException("E:TS-ALERT-DEL 更新に失敗しました。", e);
	    }
	}
}
