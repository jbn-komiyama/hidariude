// dao/CustomerMonthlyInvoiceDAO.java（追記/新設）
package dao;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import java.util.UUID;

import dto.PivotRowDTO;

public class CustomerMonthlyInvoiceDAO extends BaseDAO {
    public CustomerMonthlyInvoiceDAO(Connection conn){ super(conn); }

    // 指定範囲で「顧客×月」の金額をまとめて取得
    private static final String SQL_SALES_BY_CUSTOMER_MONTH =
        "SELECT c.id AS cid, c.company_name AS cname, i.target_year_month AS ym, " +
        "       COALESCE(SUM(i.total_amount),0) AS amt " +
        "  FROM customer_monthly_invoices i " +
        "  JOIN customers c ON c.id = i.customer_id AND c.deleted_at IS NULL " +
        " WHERE i.deleted_at IS NULL " +
        "   AND i.target_year_month BETWEEN ? AND ? " +
        " GROUP BY c.id, c.company_name, i.target_year_month " +
        " ORDER BY c.company_name, i.target_year_month";

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
                        for (String m : months) row.getAmountByYm().put(m, BigDecimal.ZERO);
                        map.put(cid, row);
                    }
                    row.getAmountByYm().put(ym, amt == null ? BigDecimal.ZERO : amt);
                }
            }
            // 行合計を計算
            for (PivotRowDTO r : map.values()) {
                java.math.BigDecimal sum = BigDecimal.ZERO;
                for (String ym : months) sum = sum.add(r.getAmountByYm().getOrDefault(ym, BigDecimal.ZERO));
                r.setRowTotal(sum);
            }
            return new ArrayList<>(map.values());
        } catch (SQLException e) {
            throw new DAOException("E:CMI21 顧客×月の売上取得に失敗しました。", e);
        }
    }
}
