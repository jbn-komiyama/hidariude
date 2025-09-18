// dao/CustomerMonthlyInvoiceDAO.java
package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class CustomerMonthlyInvoiceDAO extends BaseDAO {

    public CustomerMonthlyInvoiceDAO(Connection conn) { super(conn); }

    
	// ========================
    // SQL 定義
    // ========================

    /** 指定月の customers + assignments + task_rank + secretaries + secretary_rank をまとめて取得 */
    private static final String SQL_SELECT_BY_COS_MONTHLY =
    		 "SELECT total_amount " +
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
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new DAOException("E:CMI01 月次請求（CMI）の取得に失敗しました。", e);
        }
        return null; // 該当なし
    }
}
