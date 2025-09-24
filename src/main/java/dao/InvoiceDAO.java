package dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dto.AssignmentDTO;
import dto.InvoiceDTO;
import dto.TaskDTO;

public class InvoiceDAO extends BaseDAO {

	// ========================
	// SQL 定義
	// ========================	
	private static final String SQL_SELECT_TASKS_BY_MONTH_AND_SECRETARY = "SELECT "
			+ " c.company_name,"
			+ " t.work_date,"
			+ " t.start_time,"
			+ " t.end_time,"
			+ " t.work_minute,"
			+ " t.work_content,"
			+ " t.approved_at,"
			+ " (a.base_pay_customer + a.increase_base_pay_customer + a.customer_based_incentive_for_customer) AS hourly_pay,"
			+ " tr.rank_name"
			+ " FROM tasks t INNER JOIN assignments a"
			+ " ON t.assignment_id = a.id"
			+ " INNER JOIN customers c"
			+ " ON a.customer_id = c.id"
			+ " INNER JOIN task_rank tr"
			+ " ON a.task_rank_id = tr.id"
			+ " WHERE a.target_year_month = ?"
			+ " AND a.secretary_id = ? AND t.deleted_at IS NULL "
			+ " ORDER BY t.start_time";

	private static final String SQL_SELECT_TOTAL_MINUTES_BY_COMPANY_AND_SECRETARY = "SELECT "
			+ " c.id, "
			+ " c.company_name, "
			+ " sum(t.work_minute) AS total_minute,"
			+ " (a.base_pay_customer + a.increase_base_pay_customer + a.customer_based_incentive_for_customer) AS hourly_pay,"
			+ " tr.rank_name"
			+ " FROM tasks t INNER JOIN assignments a"
			+ " ON t.assignment_id = a.id"
			+ " INNER JOIN customers c"
			+ " ON a.customer_id = c.id"
			+ " INNER JOIN task_rank tr"
			+ " ON a.task_rank_id = tr.id"
			+ " WHERE a.target_year_month = ?"
			+ " AND a.secretary_id = ? AND t.deleted_at IS NULL "
			+ " GROUP BY c.id,"
			+ " c.company_name,"
			+ " a.base_pay_customer,"
			+ " a.increase_base_pay_customer,"
			+ " a.customer_based_incentive_for_customer,"
			+ " tr.rank_name,"
			+ " tr.rank_no"
			+ " ORDER BY c.id, tr.rank_no";

	private static final String SQL_SELECT_TASKS_BY_MONTH_AND_CUSTOMER = "SELECT s.name AS secretary_name," +
			"       t.work_date, t.start_time, t.end_time, t.work_minute, t.work_content, t.approved_at," +
			"       (a.base_pay_customer + a.increase_base_pay_customer + a.customer_based_incentive_for_customer) AS hourly_pay_customer,"
			+
			"       tr.rank_name " +
			"  FROM tasks t " +
			"  JOIN assignments a ON t.assignment_id = a.id " +
			"  JOIN secretaries s  ON a.secretary_id  = s.id " +
			"  JOIN task_rank tr   ON a.task_rank_id  = tr.id " +
			" WHERE a.customer_id = ? AND t.deleted_at IS NULL " +
			"   AND t.work_date >= to_date(? || '-01','YYYY-MM-DD') " +
			"   AND t.work_date <  (to_date(? || '-01','YYYY-MM-DD') + INTERVAL '1 month') " +
			" ORDER BY t.start_time";

	private static final String SQL_SELECT_TOTAL_MINUTES_BY_SECRETARY_AND_CUSTOMER = "SELECT s.id, s.name AS secretary_name,"
			+
			"       SUM(t.work_minute) AS total_minute," +
			"       (a.base_pay_customer + a.increase_base_pay_customer + a.customer_based_incentive_for_customer) AS hourly_pay,"
			+
			"       tr.rank_name, tr.rank_no " +
			"  FROM tasks t " +
			"  JOIN assignments a ON t.assignment_id = a.id " +
			"  JOIN secretaries s  ON a.secretary_id  = s.id " +
			"  JOIN task_rank tr   ON a.task_rank_id  = tr.id " +
			" WHERE a.customer_id = ? AND t.deleted_at IS NULL " +
			"   AND t.work_date >= to_date(? || '-01','YYYY-MM-DD') " +
			"   AND t.work_date <  (to_date(? || '-01','YYYY-MM-DD') + INTERVAL '1 month') " +
			" GROUP BY s.id, s.name, a.base_pay_customer, a.increase_base_pay_customer, a.customer_based_incentive_for_customer, tr.rank_name, tr.rank_no "
			+
			" ORDER BY s.name, tr.rank_no";

	private static final String SQL_UPSERT_MONTHLY_SUMMARY = "INSERT INTO secretary_monthly_summaries (" +
			"  secretary_id, target_year_month, total_secretary_amount, " +
			"  total_tasks_count, total_work_time, finalized_at, status" +
			") VALUES (?,?,?,?,?,?,?) " +
			"ON CONFLICT (secretary_id, target_year_month) DO UPDATE SET " +
			"  total_secretary_amount = EXCLUDED.total_secretary_amount, " +
			"  total_tasks_count     = EXCLUDED.total_tasks_count, " +
			"  total_work_time       = EXCLUDED.total_work_time, " +
			"  finalized_at          = EXCLUDED.finalized_at, " +
			"  status                = EXCLUDED.status, " +
			"  updated_at            = CURRENT_TIMESTAMP";

	// InvoiceDAO に追記
	private static final String SQL_ADMIN_LINES = "SELECT c.id AS customer_id, c.company_name, s.name AS secretary_name, tr.rank_name, "
			+ "       SUM(t.work_minute) AS total_minute, "
			+ "       (a.base_pay_customer + a.increase_base_pay_customer + a.customer_based_incentive_for_customer) AS hourly_pay "
			+ "  FROM tasks t "
			+ "  JOIN assignments a ON t.assignment_id = a.id "
			+ "  JOIN customers   c ON a.customer_id   = c.id "
			+ "  JOIN secretaries s ON a.secretary_id  = s.id "
			+ "  JOIN task_rank  tr ON a.task_rank_id  = tr.id "
			+ " WHERE a.target_year_month = ? AND t.deleted_at IS NULL "
			+ " GROUP BY c.id, c.company_name, s.name, hourly_pay, tr.rank_name, tr.rank_no "
			+ " ORDER BY c.company_name, s.name, tr.rank_no";
	
	// 〜指定YM（実効は今月or来月Clamp）の合計
    private static final String SQL_SUM_UPTO_YM =
        "WITH eff AS ( " +
        "  SELECT CASE " +
        "    WHEN ? > to_char(date_trunc('month', current_date) + interval '1 month','YYYY-MM') " +
        "    THEN to_char(date_trunc('month', current_date) + interval '1 month','YYYY-MM') " +
        "    ELSE ? END AS ym " +
        ") " +
        "SELECT COALESCE(sum(total_amount),0) AS sum_amount, " +
        "       COUNT(*) AS cnt, " +
        "       COALESCE(sum(total_work_time),0) AS sum_work " +
        "  FROM customer_monthly_invoices " +
        " WHERE deleted_at IS NULL AND customer_id = ? " +
        "   AND target_year_month <= (SELECT ym FROM eff)";

    // 直近12か月（〜指定YM）
    private static final String SQL_LAST12_UPTO_YM =
        "WITH eff AS ( " +
        "  SELECT CASE " +
        "    WHEN ? > to_char(date_trunc('month', current_date) + interval '1 month','YYYY-MM') " +
        "    THEN to_char(date_trunc('month', current_date) + interval '1 month','YYYY-MM') " +
        "    ELSE ? END AS ym " +
        ") " +
        "SELECT target_year_month, total_amount, total_tasks_count, total_work_time " +
        "  FROM customer_monthly_invoices " +
        " WHERE deleted_at IS NULL AND customer_id = ? " +
        "   AND target_year_month >= to_char((to_date((SELECT ym FROM eff)||'-01','YYYY-MM-DD') - interval '11 months'),'YYYY-MM') " +
        "   AND target_year_month <= (SELECT ym FROM eff) " +
        " ORDER BY target_year_month DESC";
    
	 // 管理者向け：秘書支払ライン（対象月）
	 // 秘書 × 顧客 × ランクごとに合計分（分）と秘書時給（秘書取り分）を返す
	 private static final String SQL_ADMIN_COST_LINES =
	     "SELECT " +
	     "  s.id            AS secretary_id, " +
	     "  s.name          AS secretary_name, " +
	     "  c.id            AS customer_id, " +
	     "  c.company_name  AS company_name, " +
	     "  tr.rank_name    AS rank_name, " +
	     "  tr.rank_no      AS rank_no, " +
	     "  SUM(t.work_minute) AS total_minute, " +
	     "  (a.base_pay_secretary + a.increase_base_pay_secretary + a.customer_based_incentive_for_secretary) AS hourly_pay_sec " +
	     "FROM tasks t " +
	     "JOIN assignments a  ON t.assignment_id = a.id " +
	     "JOIN customers c    ON a.customer_id   = c.id " +
	     "JOIN secretaries s  ON a.secretary_id  = s.id " +
	     "JOIN task_rank tr   ON a.task_rank_id  = tr.id " +
	     "WHERE a.target_year_month = ? " +
	     "  AND t.deleted_at IS NULL " +
	     "GROUP BY s.id, s.name, c.id, c.company_name, hourly_pay_sec, tr.rank_name, tr.rank_no " +
	     "ORDER BY s.name, c.company_name, tr.rank_no";

	public InvoiceDAO(Connection conn) {
		super(conn);
	}

	// ========================
	// SELECT
	// ========================

	public List<TaskDTO> selectTasksByMonthAndSecretary(UUID secretaryId, String targetYearMonth) {
		final List<TaskDTO> list = new ArrayList<>();

		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_TASKS_BY_MONTH_AND_SECRETARY)) {
			ps.setString(1, targetYearMonth);
			ps.setObject(2, secretaryId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					TaskDTO dto = new TaskDTO();

					// tasks
					dto.setWorkDate(rs.getDate("work_date"));
					// TIME型 → Timestampへ変換
					dto.setStartTime(rs.getTimestamp("start_time"));
					dto.setEndTime(rs.getTimestamp("end_time"));
					dto.setWorkMinute(rs.getObject("work_minute", Integer.class));
					dto.setWorkContent(rs.getString("work_content"));
					dto.setApprovedAt(rs.getTimestamp("approved_at"));

					// assignment（表示用の付帯情報を AssignmentDTO に寄せる）
					AssignmentDTO asg = new AssignmentDTO();
					asg.setCustomerCompanyName(rs.getString("company_name")); // c.company_name
					asg.setHourlyPayCustomer(rs.getBigDecimal("hourly_pay")); // a.base+inc+incentive (as hourly_pay)
					asg.setTaskRankName(rs.getString("rank_name")); // tr.rank_name
					asg.setTargetYearMonth(targetYearMonth); // パラメータから
					asg.setAssignmentSecretaryId(secretaryId); // パラメータから
					dto.setAssignment(asg);

					list.add(dto);
				}
			}
			return list;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("E:INV01 タスク明細取得に失敗しました", e);
		}

	}

	public List<InvoiceDTO> selectTotalMinutesByCompanyAndSecretary(UUID secretaryId, String targetYearMonth) {
		final List<InvoiceDTO> list = new ArrayList<>();

		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_TOTAL_MINUTES_BY_COMPANY_AND_SECRETARY)) {
			ps.setString(1, targetYearMonth);
			ps.setObject(2, secretaryId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					InvoiceDTO dto = new InvoiceDTO();
					dto.setCustomerId((UUID) rs.getObject("id"));
					dto.setCustomerCompanyName(rs.getString("company_name"));

					int totalMin = rs.getInt("total_minute");
					dto.setTotalMinute(totalMin);

					BigDecimal hourlyPay = rs.getBigDecimal("hourly_pay");
					dto.setHourlyPay(hourlyPay);

					dto.setTaskRankName(rs.getString("rank_name"));
					dto.setTargetYM(targetYearMonth);

					// 合計金額: 時給×合計分/60（円未満切り上げ/切り捨て等は要件に合わせて調整）
					BigDecimal fee = hourlyPay
							.multiply(BigDecimal.valueOf(totalMin))
							.divide(BigDecimal.valueOf(60), 0, java.math.RoundingMode.HALF_UP);
					dto.setFee(fee); // ★ setFeeでtotalFeeに加算する設計ならこれで積み上がる
					// dto.setTotalFee(fee);       // 「合計のみ」を持たせたい場合はこっちに置き換え

					list.add(dto);
				}
			}
			return list;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DAOException("E:INV02 会社別集計取得に失敗しました", e);
		}
	}

	// 顧客用：タスク明細（work_date を対象月で絞り込み）
	public List<TaskDTO> selectTasksByMonthAndCustomer(UUID customerId, String targetYM) {
		final List<TaskDTO> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_TASKS_BY_MONTH_AND_CUSTOMER)) {
			int p = 1;
			ps.setObject(p++, customerId); // 1) a.customer_id = ?
			ps.setString(p++, targetYM); // 2) t.work_date >= to_date(?||'-01',...)
			ps.setString(p++, targetYM); // 3) t.work_date <  (to_date(?||'-01',...)+1 month)

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					TaskDTO dto = new TaskDTO();
					dto.setWorkDate(rs.getDate("work_date"));
					dto.setStartTime(rs.getTimestamp("start_time"));
					dto.setEndTime(rs.getTimestamp("end_time"));
					dto.setWorkMinute(rs.getObject("work_minute", Integer.class));
					dto.setWorkContent(rs.getString("work_content"));
					dto.setApprovedAt(rs.getTimestamp("approved_at"));

					AssignmentDTO asg = new AssignmentDTO();
					asg.setSecretaryName(rs.getString("secretary_name"));
					asg.setHourlyPayCustomer(rs.getBigDecimal("hourly_pay_customer"));
					asg.setTaskRankName(rs.getString("rank_name"));
					asg.setTargetYearMonth(targetYM);
					asg.setAssignmentCustomerId(customerId);
					dto.setAssignment(asg);

					list.add(dto);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("E:INV-C01 顧客用タスク明細取得に失敗しました（work_date基準）", e);
		}
		return list;
	}

	// 顧客用：秘書×ランク集計（work_date を対象月で絞り込み）
	public List<InvoiceDTO> selectTotalMinutesBySecretaryAndCustomer(UUID customerId, String targetYM) {
		final List<InvoiceDTO> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_TOTAL_MINUTES_BY_SECRETARY_AND_CUSTOMER)) {
			int p = 1;
			ps.setObject(p++, customerId); // 1) a.customer_id = ?
			ps.setString(p++, targetYM); // 2) t.work_date >= ...
			ps.setString(p++, targetYM); // 3) t.work_date <  ...

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					InvoiceDTO dto = new InvoiceDTO();
					dto.setCustomerId((UUID) rs.getObject("id")); // secretary_id を格納
					dto.setCustomerCompanyName(rs.getString("secretary_name")); // 表示用に秘書名
					int totalMin = rs.getInt("total_minute");
					dto.setTotalMinute(totalMin);
					var hourlyPay = rs.getBigDecimal("hourly_pay");
					dto.setHourlyPay(hourlyPay);
					dto.setTaskRankName(rs.getString("rank_name"));
					dto.setTargetYM(targetYM);

					var fee = hourlyPay
							.multiply(java.math.BigDecimal.valueOf(totalMin))
							.divide(java.math.BigDecimal.valueOf(60), 0, java.math.RoundingMode.HALF_UP);
					dto.setFee(fee);

					list.add(dto);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("E:INV-C02 顧客用集計取得に失敗しました（work_date基準）", e);
		}
		return list;
	}

	/**
	 * 秘書×年月の月次サマリをUPSERT（存在しなければINSERT、あればUPDATE）します。
	 *
	 * @param secretaryId 秘書ID
	 * @param targetYM    "yyyy-MM"
	 * @param totalAmount 総額（秘書取り分）
	 * @param totalTasks  タスク件数
	 * @param totalMinutes 合計稼働分（分）
	 * @param finalizedAt  確定日時（確定でなければ null）
	 * @param status      任意（不要なら null）
	 * @return 影響行数
	 */
	public int upsertSecretaryMonthlySummary(
			UUID secretaryId,
			String targetYM,
			BigDecimal totalAmount,
			int totalTasks,
			int totalMinutes,
			Timestamp finalizedAt,
			String status) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_UPSERT_MONTHLY_SUMMARY)) {
			int i = 1;
			ps.setObject(i++, secretaryId);
			ps.setString(i++, targetYM);
			if (totalAmount == null) {
				ps.setNull(i++, Types.NUMERIC);
			} else {
				ps.setBigDecimal(i++, totalAmount);
			}
			ps.setInt(i++, totalTasks);
			ps.setInt(i++, totalMinutes);
			if (finalizedAt == null) {
				ps.setNull(i++, Types.TIMESTAMP);
			} else {
				ps.setTimestamp(i++, finalizedAt);
			}
			if (status == null || status.isBlank()) {
				ps.setNull(i++, Types.VARCHAR);
			} else {
				ps.setString(i++, status);
			}
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:INV99 月次サマリUPSERTに失敗しました。", e);
		}

	}

	public List<InvoiceDTO> selectAdminLines(String targetYM) {
		final List<InvoiceDTO> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(SQL_ADMIN_LINES)) {
			ps.setString(1, targetYM);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					InvoiceDTO d = new InvoiceDTO();
					d.setCustomerId((UUID) rs.getObject("customer_id"));
					d.setCustomerCompanyName(rs.getString("company_name"));
					d.setSecretaryName(rs.getString("secretary_name")); // ★ 追加
					d.setTaskRankName(rs.getString("rank_name"));
					int mins = rs.getInt("total_minute");
					d.setTotalMinute(mins);
					var hourly = rs.getBigDecimal("hourly_pay");
					d.setHourlyPay(hourly);
					var fee = hourly.multiply(BigDecimal.valueOf(mins))
							.divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP);
					d.setFee(fee);
					list.add(d);
				}
			}
		} catch (SQLException e) {
			throw new DAOException("E:INV-ADM01 管理者向け明細取得に失敗しました。", e);
		}
		return list;
	}
	
	public List<InvoiceDTO> selectAdminCostLines(String targetYM) {
	    final List<InvoiceDTO> list = new ArrayList<>();
	    try (PreparedStatement ps = conn.prepareStatement(SQL_ADMIN_COST_LINES)) {
	        ps.setString(1, targetYM);
	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                InvoiceDTO d = new InvoiceDTO();
	                // 使い回し： secretaryName / customerCompanyName / taskRankName / totalMinute / hourlyPay / fee
	                // 追加で secretary_id, customer_id が必要なら DTO にフィールドがあれば詰めてください
	                d.setSecretaryName(rs.getString("secretary_name"));
	                d.setCustomerCompanyName(rs.getString("company_name"));
	                d.setTaskRankName(rs.getString("rank_name"));

	                int mins = rs.getInt("total_minute");
	                d.setTotalMinute(mins);

	                var hourly = rs.getBigDecimal("hourly_pay_sec"); // 秘書取り分の時給
	                d.setHourlyPay(hourly);

	                // 料金 = 時給 × 分 / 60（0円未満は出ない想定。丸めはHALF_UP）
	                var fee = (hourly == null ? java.math.BigDecimal.ZERO :
	                        hourly.multiply(java.math.BigDecimal.valueOf(mins))
	                              .divide(java.math.BigDecimal.valueOf(60), 0, java.math.RoundingMode.HALF_UP));
	                d.setFee(fee);

	                d.setTargetYM(targetYM);
	                list.add(d);
	            }
	        }
	    } catch (SQLException e) {
	        throw new DAOException("E:INV-ADM-COST 管理者向け秘書支払明細取得に失敗しました。", e);
	    }
	    return list;
	}
}