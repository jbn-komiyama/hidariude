package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
		+ " a.base_pay_secretary+a.increase_base_pay_secretary+a.customer_based_incentive_for_secretary hourly_pay,"
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
		+ " sum(t.work_minute) total_minute,"
		+ " a.base_pay_secretary+a.increase_base_pay_secretary+a.customer_based_incentive_for_secretary hourly_pay,"
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
		+ " a.increase_base_pay_secretary,"
		+ " a.customer_based_incentive_for_secretary,"
		+ " a.base_pay_secretary,"
		+ " tr.rank_name,"
		+ " tr.rank_no"
		+ " ORDER BY c.id, tr.rank_no";
	
	private static final String SQL_SELECT_TASKS_BY_MONTH_AND_CUSTOMER =
		    "SELECT s.name AS secretary_name," +
		    "       t.work_date, t.start_time, t.end_time, t.work_minute, t.work_content, t.approved_at," +
		    "       (a.base_pay_customer + a.increase_base_pay_customer + a.customer_based_incentive_for_customer) AS hourly_pay_customer," +
		    "       tr.rank_name " +
		    "  FROM tasks t " +
		    "  JOIN assignments a ON t.assignment_id = a.id " +
		    "  JOIN secretaries s  ON a.secretary_id  = s.id " +
		    "  JOIN task_rank tr   ON a.task_rank_id  = tr.id " +
		    " WHERE a.target_year_month = ? AND a.customer_id = ? AND t.deleted_at IS NULL " +
		    " ORDER BY t.start_time";

		private static final String SQL_SELECT_TOTAL_MINUTES_BY_SECRETARY_AND_CUSTOMER =
		    "SELECT s.id, s.name AS secretary_name," +
		    "       SUM(t.work_minute) AS total_minute," +
		    "       (a.base_pay_customer + a.increase_base_pay_customer + a.customer_based_incentive_for_customer) AS hourly_pay," +
		    "       tr.rank_name, tr.rank_no " +
		    "  FROM tasks t " +
		    "  JOIN assignments a ON t.assignment_id = a.id " +
		    "  JOIN secretaries s  ON a.secretary_id  = s.id " +
		    "  JOIN task_rank tr   ON a.task_rank_id  = tr.id " +
		    " WHERE a.target_year_month = ? AND a.customer_id = ? AND t.deleted_at IS NULL " +
		    " GROUP BY s.id, s.name, a.base_pay_customer, a.increase_base_pay_customer, a.customer_based_incentive_for_customer, tr.rank_name, tr.rank_no " +
		    " ORDER BY s.name, tr.rank_no";
		
	private static final String SQL_UPSERT_MONTHLY_SUMMARY =
		    "INSERT INTO secretary_monthly_summaries (" +
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
	
	
	public InvoiceDAO(Connection conn) {
		super(conn);
	}
	
	// ========================
    // SELECT
    // ========================

	public List<TaskDTO> selectTasksByMonthAndSecretary(UUID secretaryId, String targetYearMonth){
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
	                asg.setCustomerCompanyName(rs.getString("company_name"));          // c.company_name
	                asg.setHourlyPaySecretary(rs.getBigDecimal("hourly_pay"));        // a.base+inc+incentive (as hourly_pay)
	                asg.setTaskRankName(rs.getString("rank_name"));                    // tr.rank_name
	                asg.setTargetYearMonth(targetYearMonth);                           // パラメータから
	                asg.setAssignmentSecretaryId(secretaryId);                         // パラメータから
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

	public List<InvoiceDTO> selectTotalMinutesByCompanyAndSecretary(UUID secretaryId, String targetYearMonth){
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
	                dto.setFee(fee);               // ★ setFeeでtotalFeeに加算する設計ならこれで積み上がる
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
	
	public List<TaskDTO> selectTasksByMonthAndCustomer(UUID customerId, String targetYM) {
	    final List<TaskDTO> list = new ArrayList<>();
	    try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_TASKS_BY_MONTH_AND_CUSTOMER)) {
	        ps.setString(1, targetYM);
	        ps.setObject(2, customerId);
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
	        throw new RuntimeException("E:INV-C01 顧客用タスク明細取得に失敗しました", e);
	    }
	    return list;
	}

	public List<InvoiceDTO> selectTotalMinutesBySecretaryAndCustomer(UUID customerId, String targetYM) {
	    final List<InvoiceDTO> list = new ArrayList<>();
	    try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_TOTAL_MINUTES_BY_SECRETARY_AND_CUSTOMER)) {
	        ps.setString(1, targetYM);
	        ps.setObject(2, customerId);
	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                InvoiceDTO dto = new InvoiceDTO();
	                // 顧客向けでは「最左列=秘書名」で表示したいので流用
	                dto.setCustomerId((UUID) rs.getObject("id"));                // ← secretary_id を格納
	                dto.setCustomerCompanyName(rs.getString("secretary_name"));  // ← 表示用に秘書名を入れる
	                int totalMin = rs.getInt("total_minute");
	                dto.setTotalMinute(totalMin);
	                var hourlyPay = rs.getBigDecimal("hourly_pay");
	                dto.setHourlyPay(hourlyPay);
	                dto.setTaskRankName(rs.getString("rank_name"));
	                dto.setTargetYM(targetYM);

	                var fee = hourlyPay.multiply(java.math.BigDecimal.valueOf(totalMin))
	                                   .divide(java.math.BigDecimal.valueOf(60), 0, java.math.RoundingMode.HALF_UP);
	                dto.setFee(fee);
	                list.add(dto);
	            }
	        }
	    } catch (SQLException e) {
	        throw new RuntimeException("E:INV-C02 顧客用集計取得に失敗しました", e);
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
	        java.math.BigDecimal totalAmount,
	        int totalTasks,
	        int totalMinutes,
	        java.sql.Timestamp finalizedAt,
	        String status
	) {
	    try (PreparedStatement ps = conn.prepareStatement(SQL_UPSERT_MONTHLY_SUMMARY)) {
	        int i = 1;
	        ps.setObject(i++, secretaryId);
	        ps.setString(i++, targetYM);
	        if (totalAmount == null) {
	            ps.setNull(i++, java.sql.Types.NUMERIC);
	        } else {
	            ps.setBigDecimal(i++, totalAmount);
	        }
	        ps.setInt(i++, totalTasks);
	        ps.setInt(i++, totalMinutes);
	        if (finalizedAt == null) {
	            ps.setNull(i++, java.sql.Types.TIMESTAMP);
	        } else {
	            ps.setTimestamp(i++, finalizedAt);
	        }
	        if (status == null || status.isBlank()) {
	            ps.setNull(i++, java.sql.Types.VARCHAR);
	        } else {
	            ps.setString(i++, status);
	        }
	        return ps.executeUpdate();
	    } catch (SQLException e) {
	        throw new DAOException("E:INV99 月次サマリUPSERTに失敗しました。", e);
	    }

	}
}