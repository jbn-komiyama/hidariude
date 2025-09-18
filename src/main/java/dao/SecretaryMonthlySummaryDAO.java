// dao/SecretaryMonthlySummaryDAO.java
package dao;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.UUID;

import dto.PivotRowDTO;
import dto.SecretaryMonthlySummaryDTO;
import dto.SecretaryTotalsDTO;

public class SecretaryMonthlySummaryDAO extends BaseDAO {
    public SecretaryMonthlySummaryDAO(Connection conn) { super(conn); }

    private static final String SQL_TOTALS =
        "SELECT COALESCE(SUM(total_secretary_amount),0) AS amt, " +
        "       COALESCE(SUM(total_tasks_count),0)     AS cnt, " +
        "       COALESCE(SUM(total_work_time),0)       AS mins " +
        "  FROM secretary_monthly_summaries " +
        " WHERE deleted_at IS NULL AND secretary_id = ?";
    

    private static final String SQL_LAST12 =
        "SELECT id, secretary_id, target_year_month, total_secretary_amount, " +
        "       total_tasks_count, total_work_time, finalized_at, status, " +
        "       created_at, updated_at, deleted_at " +
        "  FROM secretary_monthly_summaries " +
        " WHERE deleted_at IS NULL AND secretary_id = ? " +
        "   AND target_year_month BETWEEN ? AND ? " +
        " ORDER BY target_year_month";
    
    private static final String SQL_COSTS_BY_SECRETARY_MONTH =
            "SELECT s.id AS sid, s.name AS sname, m.target_year_month AS ym, " +
            "       COALESCE(SUM(m.total_secretary_amount),0) AS amt " +
            "  FROM secretary_monthly_summaries m " +
            "  JOIN secretaries s ON s.id = m.secretary_id AND s.deleted_at IS NULL " +
            " WHERE m.deleted_at IS NULL " +
            "   AND m.target_year_month BETWEEN ? AND ? " +
            " GROUP BY s.id, s.name, m.target_year_month " +
            " ORDER BY s.name, m.target_year_month";


    /** ③ 今までの合計（DTOで返す） */
    public SecretaryTotalsDTO selectTotals(UUID secretaryId) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_TOTALS)) {
            ps.setObject(1, secretaryId);
            try (ResultSet rs = ps.executeQuery()) {
                SecretaryTotalsDTO d = new SecretaryTotalsDTO();
                if (rs.next()) {
                    d.setTotalSecretaryAmount(rs.getBigDecimal("amt"));
                    long cntL = rs.getLong("cnt");
                    d.setTotalTasksCount(rs.wasNull() ? null : Math.toIntExact(cntL));

                    long minsL = rs.getLong("mins");
                    d.setTotalWorkTime(rs.wasNull() ? null : Math.toIntExact(minsL));
                }
                return d;
            }
        } catch (SQLException e) {
            throw new DAOException("E:SMS11 合計取得に失敗しました。", e);
        }
    }


    /** ⑤ 今月までの1年分（DTOのリスト） */
    public List<SecretaryMonthlySummaryDTO> selectLast12Months(UUID secretaryId, String fromYm, String toYm) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_LAST12)) {
            ps.setObject(1, secretaryId);
            ps.setString(2, fromYm);
            ps.setString(3, toYm);
            try (ResultSet rs = ps.executeQuery()) {
                List<SecretaryMonthlySummaryDTO> list = new ArrayList<>();
                while (rs.next()) {
                    SecretaryMonthlySummaryDTO d = new SecretaryMonthlySummaryDTO();
                    d.setId(rs.getObject(1, UUID.class));
                    // d.setSecretaryId(...) がDTOにあればセット
                    d.setTargetYearMonth(rs.getString(3));
                    d.setTotalSecretaryAmount(rs.getBigDecimal(4));
                    long cntL = rs.getLong(5);
                    d.setTotalTasksCount(rs.wasNull() ? null : Math.toIntExact(cntL));

                    long minsL = rs.getLong(6);
                    d.setTotalWorkTime(rs.wasNull() ? null : Math.toIntExact(minsL));
                    d.setFinalizedAt(rs.getTimestamp(7));
                    d.setStatus(rs.getString(8));
                    d.setCreatedAt(rs.getTimestamp(9));
                    d.setUpdatedAt(rs.getTimestamp(10));
                    d.setDeletedAt(rs.getTimestamp(11));
                    list.add(d);
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DAOException("E:SMS12 直近12か月取得に失敗しました。", e);
        }
    }
    
    public List<PivotRowDTO> selectCostsBySecretaryMonth(String fromYm, String toYm, List<String> months) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_COSTS_BY_SECRETARY_MONTH)) {
            ps.setString(1, fromYm);
            ps.setString(2, toYm);

            Map<UUID, PivotRowDTO> map = new LinkedHashMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID sid = rs.getObject("sid", UUID.class);
                    String sname = rs.getString("sname");
                    String ym = rs.getString("ym");
                    BigDecimal amt = rs.getBigDecimal("amt");

                    PivotRowDTO row = map.get(sid);
                    if (row == null) {
                        row = new PivotRowDTO();
                        row.setId(sid);
                        row.setLabel(sname);
                        for (String m : months) row.getAmountByYm().put(m, BigDecimal.ZERO);
                        map.put(sid, row);
                    }
                    row.getAmountByYm().put(ym, amt == null ? BigDecimal.ZERO : amt);
                }
            }
            for (PivotRowDTO r : map.values()) {
                BigDecimal sum = BigDecimal.ZERO;
                for (String ym : months) sum = sum.add(r.getAmountByYm().getOrDefault(ym, BigDecimal.ZERO));
                r.setRowTotal(sum);
            }
            return new ArrayList<>(map.values());
        } catch (SQLException e) {
            throw new DAOException("E:SMS21 秘書×月の支出取得に失敗しました。", e);
        }
    }

}
