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

import dto.CustomerDTO;
import dto.CustomerMonthlyInvoiceDTO;
import dto.PivotRowDTO;

/**
 * 顧客月次請求（{@code customer_monthly_invoices}）の集計・参照・UPSERT を司る DAO。
 * tasks / assignments から「顧客×月」単位で集計し、CMI テーブルへ DRAFT で UPSERT します。
 * 参照系は論理削除（{@code deleted_at IS NULL}）を自動で考慮します。
 *
 * クラス構成：
 * 1. フィールド（SQL）
 * 2. フィールド、コンストラクタ
 * 3. メソッド（SELECT／UPSERT／サマリ）
 */
public class CustomerMonthlyInvoiceDAO extends BaseDAO {

    /** ========================
     * ① フィールド（SQL 定義）
     * ======================== */

    /** 顧客×月の売上（合算）を取得（期間指定） */
    private static final String SQL_SALES_BY_CUSTOMER_MONTH =
        "SELECT c.id AS cid, c.company_name AS cname, i.target_year_month AS ym, " +
        "       COALESCE(SUM(i.total_amount),0) AS amt " +
        "  FROM customer_monthly_invoices i " +
        "  JOIN customers c ON c.id = i.customer_id AND c.deleted_at IS NULL " +
        " WHERE i.deleted_at IS NULL " +
        "   AND i.target_year_month BETWEEN ? AND ? " +
        " GROUP BY c.id, c.company_name, i.target_year_month " +
        " ORDER BY c.company_name, i.target_year_month";

    /**
     * 当月の「顧客ごと合計」を tasks / assignments から集計（顧客単価ベース）。
     * 時間課金：{@code (base_pay_customer + increase_base_pay_customer + customer_based_incentive_for_customer) * (work_minute / 60.0)}
     */
    private static final String SQL_AGG_BY_CUSTOMER_MONTH =
        "SELECT a.customer_id, " +
        "       SUM(t.work_minute)                AS total_minutes, " +
        "       COUNT(*)                           AS task_count, " +
        "       SUM( (a.base_pay_customer + a.increase_base_pay_customer " +
        "           + a.customer_based_incentive_for_customer) * (t.work_minute / 60.0) )::numeric(12,2) AS total_amount " +
        "  FROM tasks t " +
        "  JOIN assignments a ON t.assignment_id = a.id " +
        " WHERE a.target_year_month = ? " +
        "   AND t.deleted_at IS NULL " +
        " GROUP BY a.customer_id ";

    /** 顧客×年月の UPSERT（DRAFT 固定） */
    private static final String SQL_UPSERT_CMI =
        "INSERT INTO customer_monthly_invoices (" +
        "  customer_id, target_year_month, total_amount, " +
        "  total_tasks_count, total_work_time, status" +
        ") VALUES (?,?,?,?,?, 'DRAFT') " +
        "ON CONFLICT (customer_id, target_year_month) DO UPDATE SET " +
        "  total_amount      = EXCLUDED.total_amount, " +
        "  total_tasks_count = EXCLUDED.total_tasks_count, " +
        "  total_work_time   = EXCLUDED.total_work_time, " +
        "  status            = 'DRAFT', " +
        "  updated_at        = CURRENT_TIMESTAMP";

    /** 単月の CMI（合計金額）取得 */
    private static final String SQL_SELECT_BY_COS_MONTHLY =
        "SELECT total_amount " +
        "  FROM customer_monthly_invoices " +
        " WHERE deleted_at IS NULL " +
        "   AND customer_id = ? " +
        "   AND target_year_month = ? " +
        " ORDER BY updated_at DESC " +
        " LIMIT 1";

    /**
     * 〜指定YM（但し今月/来月を上限にクランプ）の累計（件数／金額／合計稼働分）。
     * クランプの意図：未来に過度な集計が進まないようにするため。
     */
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

    /** 直近12か月（〜指定YM、降順）の CMI 一覧（customers と結合） */
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

    /** ========================
     * ② フィールド、コンストラクタ
     * ======================== */

    /**
     * コンストラクタ。
     *
     * @param conn 呼び出し側が管理する JDBC コネクション
     */
    public CustomerMonthlyInvoiceDAO(Connection conn) {
        super(conn);
    }

    /** ========================
     * ③ メソッド
     * =========================
     * SELECT（集計ビュー系）
     * ========================= */

    /**
     * 指定範囲（{@code fromYm}〜{@code toYm}）の「顧客×月」売上をピボット行 DTO として取得します。
     * 返却は顧客ごと 1 行（{@link PivotRowDTO}）で、列は {@code months} に与えた yyyy-MM をキーにした
     * 金額マップ（存在しない月は 0 初期化）、および行合計 {@code rowTotal} を含みます。
     *
     * @param fromYm  期間開始（含む, yyyy-MM）
     * @param toYm    期間終了（含む, yyyy-MM）
     * @param months  列見出しにしたい年月（yyyy-MM）の並び（表示順のため必須）
     * @return 顧客行（ピボット）リスト。0件可
     * @throws DAOException 集計取得に失敗した場合
     */
    public List<PivotRowDTO> selectSalesByCustomerMonth(String fromYm, String toYm, List<String> months) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SALES_BY_CUSTOMER_MONTH)) {
            ps.setString(1, fromYm);
            ps.setString(2, toYm);

            /** 顧客IDごとに行を保持（LinkedHashMap で安定順） */
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
                        /** 列穴あきを避けるため、先に全月を 0 埋め */
                        for (String m : months) {
                            row.getAmountByYm().put(m, BigDecimal.ZERO);
                        }
                        map.put(cid, row);
                    }
                    row.getAmountByYm().put(ym, amt == null ? BigDecimal.ZERO : amt);
                }
            }
            /** 行合計 */
            for (PivotRowDTO r : map.values()) {
                BigDecimal sum = BigDecimal.ZERO;
                for (String ym : months) {
                    sum = sum.add(r.getAmountByYm().getOrDefault(ym, BigDecimal.ZERO));
                }
                r.setRowTotal(sum);
            }
            return new ArrayList<>(map.values());
        } catch (SQLException e) {
            throw new DAOException("E:CMI21 顧客×月の売上取得に失敗しました。", e);
        }
    }

    /** =========================
     * SELECT（単月参照）
     * ========================= */

    /**
     * 指定顧客・指定年月（yyyy-MM）の CMI 合計金額を返します。
     * レコードが存在しない場合は {@code null} を返します。
     *
     * @param customerId 顧客ID
     * @param yearMonth  年月（yyyy-MM）
     * @return 合計金額／存在しなければ null
     * @throws DAOException 取得に失敗した場合
     */
    public BigDecimal selectTotalAmountByCustomerAndMonth(UUID customerId, String yearMonth) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_COS_MONTHLY)) {
            ps.setObject(1, customerId);
            ps.setString(2, yearMonth);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("E:CMI01 月次請求（CMI）の取得に失敗しました。", e);
        }
        return null; /** 該当なし */
    }

    /** =========================
     * UPSERT（集計→CMI）
     * ========================= */

    /**
     * 指定年月（yyyy-MM）のタスク群を assignments と結合して集計し、
     * 顧客×年月の CMI を DRAFT で UPSERT します。
     * - 金額は顧客単価ベースで算出
     * - 対象タスクは {@code deleted_at IS NULL} のみ
     * - UPSERT 後、影響行数（INSERT/UPDATE 合計）を返す
     *
     * @param yearMonth 対象年月（yyyy-MM）
     * @return 影響行数（INSERT + UPDATE の合計）
     * @throws DAOException 集計または UPSERT に失敗した場合
     */
    public int upsertByMonthFromTasks(String yearMonth) {
        try (PreparedStatement psSel = conn.prepareStatement(SQL_AGG_BY_CUSTOMER_MONTH);
             PreparedStatement psIns = conn.prepareStatement(SQL_UPSERT_CMI)) {

            psSel.setString(1, yearMonth);

            int totalAffected = 0;
            try (ResultSet rs = psSel.executeQuery()) {
                while (rs.next()) {
                    UUID customerId = rs.getObject("customer_id", UUID.class);
                    int totalMinutes = rs.getInt("total_minutes");
                    int taskCount = rs.getInt("task_count");
                    BigDecimal amount = rs.getBigDecimal("total_amount");
                    if (amount == null) amount = BigDecimal.ZERO;

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

    /** =========================
     * SUMMARY / REPORT
     * ========================= */

    /**
     * 〜指定年月（yyyy-MM）までの累計サマリーを返します。
     * 指定年月は「今月または来月」を上限にクランプされます（未来集計の走り過ぎ防止）。
     * 返却の {@link Summary#totalAmount} / {@link Summary#count} / {@link Summary#totalWorkMinutes}
     * を利用してヘッダKPI等に表示できます。
     *
     * @param customerId 顧客ID
     * @param upToYm     集計上限（yyyy-MM）
     * @return 累計サマリー（件数・金額・合計稼働分）
     * @throws DAOException 取得に失敗した場合
     */
    public Summary selectSummaryUpToYm(UUID customerId, String upToYm) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SUM_UPTO_YM)) {
            int p = 1;
            ps.setString(p++, upToYm); /** クランプ評価用1 */
            ps.setString(p++, upToYm); /** クランプ評価用2 */
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

    /**
     * 指定年月（yyyy-MM）までの直近12か月分の CMI を降順で取得します（最新月→過去）。
     * 返却 DTO は customers の会社コード・会社名も最低限セットします。
     * フロントの折れ線・カラムチャートや表明細に適用可能です。
     *
     * @param customerId 顧客ID
     * @param upToYm     上限年月（yyyy-MM） ※内部で今月/来月にクランプ
     * @return 最新月から 12 か月分の {@link CustomerMonthlyInvoiceDTO} リスト
     * @throws DAOException 取得に失敗した場合
     */
    public List<CustomerMonthlyInvoiceDTO> selectLast12UpToYm(UUID customerId, String upToYm) {
        List<CustomerMonthlyInvoiceDTO> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_LAST12_UPTO_YM)) {
            int p = 1;
            ps.setString(p++, upToYm); /** クランプ評価用1 */
            ps.setString(p++, upToYm); /** クランプ評価用2 */
            ps.setObject(p++, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int i = 1;
                    CustomerMonthlyInvoiceDTO d = new CustomerMonthlyInvoiceDTO();
                    d.setId(rs.getObject(i++, UUID.class));

                    /** 顧客最小情報（ID/コード/名称） */
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

                    /** customers 由来 */
                    c.setCompanyCode(rs.getString(i++));
                    c.setCompanyName(rs.getString(i++));

                    list.add(d);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("E:CMI02 直近1年分の請求取得に失敗しました。", e);
        }
        return list;
    }

    /** =========================
     * 内部型
     * ========================= */

    /**
     * 合計サマリー（件数／金額／合計稼働分）を表す簡易 DTO。
     */
    public static final class Summary {
        /** 金額合計（null を返さず 0 初期化） */
        public BigDecimal totalAmount = BigDecimal.ZERO;
        /** レコード件数 */
        public int count;
        /** 合計稼働分（分） */
        public int totalWorkMinutes;
    }
}
