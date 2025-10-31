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

/**
 * 請求まわり（明細・集計・月次サマリ）を扱う DAO。
 *
 * 責務：
 * - 秘書／顧客／管理者の文脈でのタスク明細取得
 * - 分単位の集計と料金（時給×分/60）の計算結果のDTO詰め
 * - 秘書の月次サマリ（upsert）
 *
 * 設計：
 * - 本DAOは渡された {@link Connection} にのみ依存し、トランザクションは呼出側で管理
 * - DB例外は {@link DAOException} にラップして送出
 * - 金額計算の丸めは原則 {@link RoundingMode#HALF_UP}（要件に応じ変更）
 */
public class InvoiceDAO extends BaseDAO {

    /** =========================================================
     * ① フィールド（SQL）
     * ========================================================= */

    private static final String SQL_SELECT_TASKS_BY_MONTH_AND_SECRETARY =
        "SELECT "
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

    private static final String SQL_SELECT_TOTAL_MINUTES_BY_COMPANY_AND_SECRETARY =
        "SELECT "
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

    private static final String SQL_SELECT_TASKS_BY_MONTH_AND_CUSTOMER =
        "SELECT s.name AS secretary_name,"
      + "       t.work_date, t.start_time, t.end_time, t.work_minute, t.work_content, t.approved_at,"
      + "       (a.base_pay_customer + a.increase_base_pay_customer + a.customer_based_incentive_for_customer) AS hourly_pay_customer,"
      + "       tr.rank_name "
      + "  FROM tasks t "
      + "  JOIN assignments a ON t.assignment_id = a.id "
      + "  JOIN secretaries s  ON a.secretary_id  = s.id "
      + "  JOIN task_rank tr   ON a.task_rank_id  = tr.id "
      + " WHERE a.customer_id = ? AND t.deleted_at IS NULL "
      + "   AND t.work_date >= to_date(? || '-01','YYYY-MM-DD') "
      + "   AND t.work_date <  (to_date(? || '-01','YYYY-MM-DD') + INTERVAL '1 month') "
      + " ORDER BY t.start_time";

    private static final String SQL_SELECT_TOTAL_MINUTES_BY_SECRETARY_AND_CUSTOMER =
        "SELECT s.id, s.name AS secretary_name,"
      + "       SUM(t.work_minute) AS total_minute,"
      + "       (a.base_pay_customer + a.increase_base_pay_customer + a.customer_based_incentive_for_customer) AS hourly_pay,"
      + "       tr.rank_name, tr.rank_no "
      + "  FROM tasks t "
      + "  JOIN assignments a ON t.assignment_id = a.id "
      + "  JOIN secretaries s  ON a.secretary_id  = s.id "
      + "  JOIN task_rank tr   ON a.task_rank_id  = tr.id "
      + " WHERE a.customer_id = ? AND t.deleted_at IS NULL "
      + "   AND t.work_date >= to_date(? || '-01','YYYY-MM-DD') "
      + "   AND t.work_date <  (to_date(? || '-01','YYYY-MM-DD') + INTERVAL '1 month') "
      + " GROUP BY s.id, s.name, a.base_pay_customer, a.increase_base_pay_customer, a.customer_based_incentive_for_customer, tr.rank_name, tr.rank_no "
      + " ORDER BY s.name, tr.rank_no";

    private static final String SQL_UPSERT_MONTHLY_SUMMARY =
        "INSERT INTO secretary_monthly_summaries ("
      + "  secretary_id, target_year_month, total_secretary_amount, "
      + "  total_tasks_count, total_work_time, finalized_at, status"
      + ") VALUES (?,?,?,?,?,?,?) "
      + "ON CONFLICT (secretary_id, target_year_month) DO UPDATE SET "
      + "  total_secretary_amount = EXCLUDED.total_secretary_amount, "
      + "  total_tasks_count     = EXCLUDED.total_tasks_count, "
      + "  total_work_time       = EXCLUDED.total_work_time, "
      + "  finalized_at          = EXCLUDED.finalized_at, "
      + "  status                = EXCLUDED.status, "
      + "  updated_at            = CURRENT_TIMESTAMP";

    /** 管理者用：顧客請求ライン（対象月・顧客×秘書×ランクで分数集計・顧客課金時給） */
    private static final String SQL_ADMIN_LINES =
        "SELECT c.id AS customer_id, c.company_name, s.name AS secretary_name, tr.rank_name, "
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

    /** 管理者用：秘書支払ライン（対象月・秘書×顧客×ランクで分数集計・秘書取り分時給） */
    private static final String SQL_ADMIN_COST_LINES =
        "SELECT "
      + "  s.id            AS secretary_id, "
      + "  s.name          AS secretary_name, "
      + "  c.id            AS customer_id, "
      + "  c.company_name  AS company_name, "
      + "  tr.rank_name    AS rank_name, "
      + "  tr.rank_no      AS rank_no, "
      + "  SUM(t.work_minute) AS total_minute, "
      + "  (a.base_pay_secretary + a.increase_base_pay_secretary + a.customer_based_incentive_for_secretary) AS hourly_pay_sec "
      + "FROM tasks t "
      + "JOIN assignments a  ON t.assignment_id = a.id "
      + "JOIN customers c    ON a.customer_id   = c.id "
      + "JOIN secretaries s  ON a.secretary_id  = s.id "
      + "JOIN task_rank tr   ON a.task_rank_id  = tr.id "
      + "WHERE a.target_year_month = ? "
      + "  AND t.deleted_at IS NULL "
      + "GROUP BY s.id, s.name, c.id, c.company_name, hourly_pay_sec, tr.rank_name, tr.rank_no "
      + "ORDER BY s.name, c.company_name, tr.rank_no";

    /** =========================================================
     * ② フィールド／コンストラクタ
     * ========================================================= */

    /**
     * コンストラクタ。
     *
     * @param conn 呼び出し側から供給されるDBコネクション（トランザクション境界は呼び出し側で管理）
     */
    public InvoiceDAO(Connection conn) {
        super(conn);
    }

    /** =========================================================
     * ③ メソッド
     *   ├─ SELECT（admin / customer / secretary 順）
     *   ├─ UPSERT（月次サマリ）
     *   └─ ヘルパーなし（DTO詰めは都度記述）
     * =========================================================
     *
     * =========================
     * SELECT（secretary 用）
     * ========================= */

    /**
     * 【secretary】対象月の自身のタスク明細を取得します（assignment.target_year_month で絞込）。
     * - 論理削除済みタスク（t.deleted_at IS NOT NULL）は除外
     * - rank名／顧客名／顧客への時給（hourly_pay）も明細に同梱
     *
     * @param secretaryId 秘書ID
     * @param targetYearMonth 対象年月（yyyy-MM）
     * @return タスク明細リスト（0件時は空）
     * @throws DAOException DBエラー時
     */
    public List<TaskDTO> selectTasksByMonthAndSecretary(UUID secretaryId, String targetYearMonth) {
        final List<TaskDTO> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_TASKS_BY_MONTH_AND_SECRETARY)) {
            ps.setString(1, targetYearMonth);
            ps.setObject(2, secretaryId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TaskDTO dto = new TaskDTO();

                    /** tasks 本体 */
                    dto.setWorkDate(rs.getDate("work_date"));
                    dto.setStartTime(rs.getTimestamp("start_time")); /** TIME→Timestamp として扱う */
                    dto.setEndTime(rs.getTimestamp("end_time"));
                    dto.setWorkMinute(rs.getObject("work_minute", Integer.class));
                    dto.setWorkContent(rs.getString("work_content"));
                    dto.setApprovedAt(rs.getTimestamp("approved_at"));

                    /** 表示用付帯：Assignment 情報を内包 */
                    AssignmentDTO asg = new AssignmentDTO();
                    asg.setCustomerCompanyName(rs.getString("company_name")); /** 顧客名 */
                    asg.setHourlyPayCustomer(rs.getBigDecimal("hourly_pay")); /** 顧客課金時給 */
                    asg.setTaskRankName(rs.getString("rank_name"));
                    asg.setTargetYearMonth(targetYearMonth);
                    asg.setAssignmentSecretaryId(secretaryId);
                    dto.setAssignment(asg);

                    list.add(dto);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException("E:INV01 タスク明細取得に失敗しました（秘書）", e);
        }
    }

    /**
     * 【secretary】対象月の会社別合計（分・時給・ランク）を取得します。
     * - 合計金額は {@code hourlyPay × totalMinute ÷ 60} を {@link RoundingMode#HALF_UP} で四捨五入
     * - 戻りの {@link InvoiceDTO} は company 情報＋集計値を持ちます
     *
     * @param secretaryId 秘書ID
     * @param targetYearMonth 対象年月（yyyy-MM）
     * @return 会社別集計のリスト
     * @throws DAOException DBエラー時
     */
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

                    /** 合計金額: 時給×分/60（HALF_UP） */
                    BigDecimal fee = hourlyPay
                        .multiply(BigDecimal.valueOf(totalMin))
                        .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP);
                    dto.setFee(fee);

                    list.add(dto);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException("E:INV02 会社別集計取得に失敗しました（秘書）", e);
        }
    }

    /** =========================
     * SELECT（customer 用）
     * ========================= */

    /**
     * 【customer】対象月のタスク明細を取得します（work_date 基準で月境界を判定）。
     * - 論理削除済みタスクは除外
     * - 秘書名／ランク名／顧客課金時給（hourly_pay_customer）を同梱
     *
     * @param customerId 顧客ID
     * @param targetYM   対象年月（yyyy-MM）
     * @return タスク明細のリスト
     * @throws DAOException DBエラー時
     */
    public List<TaskDTO> selectTasksByMonthAndCustomer(UUID customerId, String targetYM) {
        final List<TaskDTO> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_TASKS_BY_MONTH_AND_CUSTOMER)) {
            int p = 1;
            ps.setObject(p++, customerId);
            ps.setString(p++, targetYM);
            ps.setString(p++, targetYM);

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
            throw new DAOException("E:INV-C01 顧客用タスク明細取得に失敗しました（work_date基準）", e);
        }
        return list;
    }

    /**
     * 【customer】対象月の「秘書×ランク」集計を取得します（work_date 基準）。
     * - 分合計のほか、料金=時給×分/60（HALF_UP）をDTOへ設定
     * - DTOの {@code customerId} には「秘書ID」を詰めています（画面表示用の都合）
     *
     * @param customerId 顧客ID
     * @param targetYM   対象年月（yyyy-MM）
     * @return 秘書×ランクごとの集計リスト
     * @throws DAOException DBエラー時
     */
    public List<InvoiceDTO> selectTotalMinutesBySecretaryAndCustomer(UUID customerId, String targetYM) {
        final List<InvoiceDTO> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_TOTAL_MINUTES_BY_SECRETARY_AND_CUSTOMER)) {
            int p = 1;
            ps.setObject(p++, customerId);
            ps.setString(p++, targetYM);
            ps.setString(p++, targetYM);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InvoiceDTO dto = new InvoiceDTO();
                    /** 表示用の都合で、secretary_id を customerId フィールドへ格納している点に注意 */
                    dto.setCustomerId((UUID) rs.getObject("id")); /** secretary_id */
                    dto.setCustomerCompanyName(rs.getString("secretary_name")); /** 表示用に秘書名 */
                    int totalMin = rs.getInt("total_minute");
                    dto.setTotalMinute(totalMin);

                    BigDecimal hourlyPay = rs.getBigDecimal("hourly_pay");
                    dto.setHourlyPay(hourlyPay);
                    dto.setTaskRankName(rs.getString("rank_name"));
                    dto.setTargetYM(targetYM);

                    BigDecimal fee = hourlyPay
                        .multiply(BigDecimal.valueOf(totalMin))
                        .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP);
                    dto.setFee(fee);

                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("E:INV-C02 顧客用集計取得に失敗しました（work_date基準）", e);
        }
        return list;
    }

    /** =========================
     * SELECT（admin 用）
     * ========================= */

    /**
     * 【admin】対象月の「顧客請求ライン」を取得します。
     * - 顧客×秘書×ランクごとに分合計を集計し、顧客課金時給で金額を算出
     * - 明細は {@link InvoiceDTO}（顧客ID/顧客名/秘書名/ランク/分/時給/金額/対象YM）
     *
     * @param targetYM 対象年月（yyyy-MM）
     * @return 顧客請求ラインのリスト
     * @throws DAOException DBエラー時
     */
    public List<InvoiceDTO> selectAdminLines(String targetYM) {
        final List<InvoiceDTO> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_ADMIN_LINES)) {
            ps.setString(1, targetYM);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InvoiceDTO d = new InvoiceDTO();
                    d.setCustomerId((UUID) rs.getObject("customer_id"));
                    d.setCustomerCompanyName(rs.getString("company_name"));
                    d.setSecretaryName(rs.getString("secretary_name"));
                    d.setTaskRankName(rs.getString("rank_name"));

                    int mins = rs.getInt("total_minute");
                    d.setTotalMinute(mins);

                    BigDecimal hourly = rs.getBigDecimal("hourly_pay");
                    d.setHourlyPay(hourly);

                    BigDecimal fee = hourly
                        .multiply(BigDecimal.valueOf(mins))
                        .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP);
                    d.setFee(fee);

                    d.setTargetYM(targetYM);
                    list.add(d);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("E:INV-ADM01 管理者向け明細取得に失敗しました。", e);
        }
        return list;
    }

    /**
     * 【admin】対象月の「秘書支払ライン」を取得します。
     * - 秘書×顧客×ランク単位に分合計を集計し、秘書取り分の時給で金額を算出
     * - 明細は {@link InvoiceDTO}（秘書名/顧客名/ランク/分/時給/金額/対象YM）
     *
     * @param targetYM 対象年月（yyyy-MM）
     * @return 秘書支払ラインのリスト
     * @throws DAOException DBエラー時
     */
    public List<InvoiceDTO> selectAdminCostLines(String targetYM) {
        final List<InvoiceDTO> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_ADMIN_COST_LINES)) {
            ps.setString(1, targetYM);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InvoiceDTO d = new InvoiceDTO();
                    d.setSecretaryName(rs.getString("secretary_name"));
                    d.setCustomerCompanyName(rs.getString("company_name"));
                    d.setTaskRankName(rs.getString("rank_name"));

                    int mins = rs.getInt("total_minute");
                    d.setTotalMinute(mins);

                    BigDecimal hourly = rs.getBigDecimal("hourly_pay_sec"); /** 秘書取り分 */
                    d.setHourlyPay(hourly);

                    /** 金額 = 時給 × 分 / 60（HALF_UP） */
                    BigDecimal fee = (hourly == null)
                        ? BigDecimal.ZERO
                        : hourly.multiply(BigDecimal.valueOf(mins))
                                .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP);
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

    /** =========================
     * UPSERT（月次サマリ）
     * ========================= */

    /**
     * 秘書×年月の月次サマリを UPSERT（存在しなければ INSERT、あれば UPDATE）します。
     *
     * @param secretaryId  秘書ID
     * @param targetYM     対象年月（yyyy-MM）
     * @param totalAmount  総額（秘書取り分）。未設定は {@code NULL}
     * @param totalTasks   タスク件数
     * @param totalMinutes 合計稼働分（分）
     * @param finalizedAt  確定日時（未確定なら {@code NULL}）
     * @param status       任意（不要なら {@code NULL}）
     * @return 影響行数（INSERT/UPDATE いずれも 1 を想定）
     * @throws DAOException DBエラー時
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
}
