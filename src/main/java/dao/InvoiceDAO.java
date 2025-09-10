package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dto.AssignmentDTO;
import dto.CustomerDTO;
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
		+ " t.work_minute,"
		+ " t.work_content,"
		+ " a.base_pay_secretary+a.increase_base_pay_secretary+a.customer_based_incentive_for_secretary hourly_pay,"
		+ " tr.rank_name"
		+ " FROM tasks t INNER JOIN assignments a"
		+ " ON t.assignment_id = a.id"
		+ " INNER JOIN customers c"
		+ " ON a.customer_id = c.id"
		+ " INNER JOIN task_rank tr"
		+ " ON a.task_rank_id = tr.id"
		+ " WHERE a.target_year_month = ?"
		+ " AND a.secretary_id = ?"
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
		+ " AND a.secretary_id = ?"
		+ " GROUP BY c.id,"
		+ " c.company_name,"
		+ " a.increase_base_pay_secretary,"
		+ " a.customer_based_incentive_for_secretary,"
		+ " a.base_pay_secretary,"
		+ " tr.rank_name,"
		+ " tr.rank_no"
		+ " ORDER BY c.id, tr.rank_no";
	
	
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
}