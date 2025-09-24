// dao/CustomerMonthlyInvoiceDAO.java（追記/新設）
package dao;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import java.util.UUID;

import dto.CustomerDTO;
import dto.CustomerMonthlyInvoiceDTO;
import dto.PivotRowDTO;

public class CustomerMonthlyInvoiceDAO extends BaseDAO {

	public CustomerMonthlyInvoiceDAO(Connection conn) {
		super(conn);
	}

	// 指定範囲で「顧客×月」の金額をまとめて取得
	private static final String SQL_SALES_BY_CUSTOMER_MONTH = "SELECT c.id AS cid, c.company_name AS cname, i.target_year_month AS ym, "
			+
			"       COALESCE(SUM(i.total_amount),0) AS amt " +
			"  FROM customer_monthly_invoices i " +
			"  JOIN customers c ON c.id = i.customer_id AND c.deleted_at IS NULL " +
			" WHERE i.deleted_at IS NULL " +
			"   AND i.target_year_month BETWEEN ? AND ? " +
			" GROUP BY c.id, c.company_name, i.target_year_month " +
			" ORDER BY c.company_name, i.target_year_month";

	// ① 当月の「顧客ごと合計」を tasks/assignments から集計
	//    - 顧客単価ベース（customer向け単価）で金額を算出
	private static final String SQL_AGG_BY_CUSTOMER_MONTH = ""
			+ "SELECT a.customer_id, "
			+ "       SUM(t.work_minute)                AS total_minutes, "
			+ "       COUNT(*)                           AS task_count, "
			+ "       SUM( (a.base_pay_customer + a.increase_base_pay_customer "
			+ "           + a.customer_based_incentive_for_customer) * (t.work_minute / 60.0) )::numeric(12,2) AS total_amount "
			+ "  FROM tasks t "
			+ "  JOIN assignments a ON t.assignment_id = a.id "
			+ " WHERE a.target_year_month = ? "
			+ "   AND t.deleted_at IS NULL "
			+ " GROUP BY a.customer_id ";

	// ② UPSERT
	private static final String SQL_UPSERT_CMI = ""
			+ "INSERT INTO customer_monthly_invoices ("
			+ "  customer_id, target_year_month, total_amount, "
			+ "  total_tasks_count, total_work_time, status"
			+ ") VALUES (?,?,?,?,?, 'DRAFT') "
			+ "ON CONFLICT (customer_id, target_year_month) DO UPDATE SET "
			+ "  total_amount      = EXCLUDED.total_amount, "
			+ "  total_tasks_count = EXCLUDED.total_tasks_count, "
			+ "  total_work_time   = EXCLUDED.total_work_time, "
			+ "  status            = 'DRAFT', "
			+ "  updated_at        = CURRENT_TIMESTAMP";
	
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

    // 直近12か月（〜指定YM）を DTO で返す
    private static final String SQL_LAST12_UPTO_YM =
        "WITH eff AS ( " +
        "  SELECT CASE " +
        "    WHEN ? > to_char(date_trunc('month', current_date) + interval '1 month','YYYY-MM') " +
        "    THEN to_char(date_trunc('month', current_date) + interval '1 month','YYYY-MM') " +
        "    ELSE ? END AS ym " +
        ") " +
        "SELECT " +
        "  cmi.id, cmi.customer_id, cmi.target_year_month, " +
        "  cmi.total_amount, cmi.total_tasks_count, cmi.total_work_time, cmi.status, " +
        "  cmi.created_at, cmi.updated_at, cmi.deleted_at, " +
        "  c.company_code, c.company_name " +
        "FROM customer_monthly_invoices cmi " +
        "JOIN customers c ON c.id = cmi.customer_id " +
        "WHERE cmi.deleted_at IS NULL " +
        "  AND cmi.customer_id = ? " +
        "  AND cmi.target_year_month >= to_char((to_date((SELECT ym FROM eff)||'-01','YYYY-MM-DD') - interval '11 months'),'YYYY-MM') " +
        "  AND cmi.target_year_month <= (SELECT ym FROM eff) " +
        "ORDER BY cmi.target_year_month DESC";
    
    /** 合計の簡易サマリー */
    public static final class Summary {
        public BigDecimal totalAmount = BigDecimal.ZERO;
        public int count;
        public int totalWorkMinutes;
    }

	/** 顧客ごと×月の売上をピボット行のリストで返す（12か月分） */
	public List<PivotRowDTO> selectSalesByCustomerMonth(String fromYm, String toYm, List<String> months) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_SALES_BY_CUSTOMER_MONTH)) {
			ps.setString(1, fromYm);
			ps.setString(2, toYm);

			Map<UUID, PivotRowDTO> map = new LinkedHashMap<>();
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					UUID cid = rs.getObject("cid", UUID.class);
					String cname = rs.getString("cname");
					String ym = rs.getString("ym");
					BigDecimal amt = rs.getBigDecimal("amt");

					PivotRowDTO row = map.get(cid);
					if (row == null) {
						row = new PivotRowDTO();
						row.setId(cid);
						row.setLabel(cname);
						// 先に全月を0で初期化しておく（列が穴あきでも0表示にするため）
						for (String m : months)
							row.getAmountByYm().put(m, BigDecimal.ZERO);
						map.put(cid, row);
					}
					row.getAmountByYm().put(ym, amt == null ? BigDecimal.ZERO : amt);
				}
			}
			// 行合計を計算
			for (PivotRowDTO r : map.values()) {
				java.math.BigDecimal sum = BigDecimal.ZERO;
				for (String ym : months)
					sum = sum.add(r.getAmountByYm().getOrDefault(ym, BigDecimal.ZERO));
				r.setRowTotal(sum);
			}
			return new ArrayList<>(map.values());
		} catch (SQLException e) {
			throw new DAOException("E:CMI21 顧客×月の売上取得に失敗しました。", e);
		}
	}

	// ========================
	// SQL 定義
	// ========================

	/** 指定月の customers + assignments + task_rank + secretaries + secretary_rank をまとめて取得 */
	private static final String SQL_SELECT_BY_COS_MONTHLY = "SELECT total_amount " +
			"  FROM customer_monthly_invoices " +
			" WHERE deleted_at IS NULL " +
			"   AND customer_id = ? " +
			"   AND target_year_month = ? " +
			" ORDER BY updated_at DESC " +
			" LIMIT 1";

	/**
	 * 請求書（customer_monthly_invoices）に紐づくタスクをもとに、
	 * 指定顧客・指定YYYY-MMの請求合計（顧客単価ベース）を返す。
	 * 行がない場合は null を返す（＝データなし判定用）。
	 */
	public BigDecimal selectTotalAmountByCustomerAndMonth(UUID customerId, String yearMonth) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_COS_MONTHLY)) {
			ps.setObject(1, customerId);
			ps.setString(2, yearMonth);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getBigDecimal(1);
			}
		} catch (SQLException e) {
			throw new DAOException("E:CMI01 月次請求（CMI）の取得に失敗しました。", e);
		}
		return null; // 該当なし
	}

	/** 指定YYYY-MMの顧客月次請求を tasks から集計し、CMI に DRAFT で upsert */
	public int upsertByMonthFromTasks(String yearMonth) {
		try (
				PreparedStatement psSel = conn.prepareStatement(SQL_AGG_BY_CUSTOMER_MONTH);
				PreparedStatement psIns = conn.prepareStatement(SQL_UPSERT_CMI)) {
			psSel.setString(1, yearMonth);

			int totalAffected = 0;
			try (ResultSet rs = psSel.executeQuery()) {
				while (rs.next()) {
					UUID customerId = rs.getObject("customer_id", UUID.class);
					int totalMinutes = rs.getInt("total_minutes");
					int taskCount = rs.getInt("task_count");
					BigDecimal amount = rs.getBigDecimal("total_amount");
					if (amount == null)
						amount = BigDecimal.ZERO;

					int i = 1;
					psIns.setObject(i++, customerId);
					psIns.setString(i++, yearMonth);
					psIns.setBigDecimal(i++, amount);
					psIns.setInt(i++, taskCount);
					psIns.setInt(i++, totalMinutes);

					totalAffected += psIns.executeUpdate();
				}
			}
			return totalAffected;
		} catch (SQLException e) {
			throw new DAOException("E:CMI-UP01 顧客月次請求のUPSERTに失敗しました。", e);
		}
	}
	
	 /** ④ 今までの合計（〜指定YM。指定YMは今月/来月にクランプ） */
    public Summary selectSummaryUpToYm(UUID customerId, String upToYm) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SUM_UPTO_YM)) {
            int p = 1;
            ps.setString(p++, upToYm);
            ps.setString(p++, upToYm);
            ps.setObject(p++, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                Summary s = new Summary();
                if (rs.next()) {
                    s.totalAmount      = rs.getBigDecimal("sum_amount");
                    s.count            = rs.getInt("cnt");
                    s.totalWorkMinutes = rs.getInt("sum_work");
                }
                return s;
            }
        } catch (SQLException e) {
            throw new DAOException("E:CMI01 顧客合計（〜今月）取得に失敗しました。", e);
        }
    }

    /** ⑥ 今月までの1年分の請求（最新月→降順）を DTO で返す */
    public List<CustomerMonthlyInvoiceDTO> selectLast12UpToYm(UUID customerId, String upToYm) {
        List<CustomerMonthlyInvoiceDTO> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_LAST12_UPTO_YM)) {
            int p = 1;
            ps.setString(p++, upToYm);
            ps.setString(p++, upToYm);
            ps.setObject(p++, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int i = 1;
                    CustomerMonthlyInvoiceDTO d = new CustomerMonthlyInvoiceDTO();
                    d.setId(rs.getObject(i++, UUID.class));

                    // CustomerDTO（最低限の項目のみ詰める）
                    UUID cid = rs.getObject(i++, UUID.class);
                    CustomerDTO c = new CustomerDTO();
                    c.setId(cid);
                    d.setCustomer(c);

                    d.setTargetYearMonth(rs.getString(i++));
                    d.setTotalAmount(rs.getBigDecimal(i++));
                    d.setTotalTasksCount((Integer) rs.getObject(i++));
                    d.setTotalWorkTime((Integer) rs.getObject(i++));
                    d.setStatus(rs.getString(i++));
                    d.setCreatedAt(rs.getTimestamp(i++));
                    d.setUpdatedAt(rs.getTimestamp(i++));
                    d.setDeletedAt(rs.getTimestamp(i++));

                    // customers 由来（任意：あれば便利）
                    String companyCode = rs.getString(i++);
                    String companyName = rs.getString(i++);
                    c.setCompanyCode(companyCode);
                    c.setCompanyName(companyName);

                    list.add(d);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("E:CMI02 直近1年分の請求取得に失敗しました。", e);
        }
        return list;
    }
}
