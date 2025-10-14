// dao/SecretaryMonthlySummaryDAO.java
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

import dto.PivotRowDTO;
import dto.SecretaryMonthlySummaryDTO;
import dto.SecretaryTotalsDTO;

/**
 * 【集計DAO】秘書の月次サマリ（secretary_monthly_summaries）に関する読み取り専用DAO。
 * <p>
 * - 画面想定：<br>
 *   ・【secretary】マイページ/実績サマリ（合計・直近12ヶ月）<br>
 *   ・【admin】売上・原価サマリ（秘書×月のピボット）<br>
 * </p>
 * <p>
 * SQLは全て定数として集約（①フィールド：SQL）。<br>
 * 例外は {@link DAOException} にラップして上位（Service）へ委譲します。<br>
 * </p>
 */
public class SecretaryMonthlySummaryDAO extends BaseDAO {

    // =========================
    // ① フィールド（SQL）
    // =========================

    /** 秘書トータル（売上=取り分合計／件数／稼働分） */
    private static final String SQL_TOTALS =
        "SELECT COALESCE(SUM(total_secretary_amount),0) AS amt, " +
        "       COALESCE(SUM(total_tasks_count),0)     AS cnt, " +
        "       COALESCE(SUM(total_work_time),0)       AS mins " +
        "  FROM secretary_monthly_summaries " +
        " WHERE deleted_at IS NULL AND secretary_id = ?";

    /** 秘書の直近12か月（境界含む）の一覧（年月昇順） */
    private static final String SQL_LAST12 =
        "SELECT id, secretary_id, target_year_month, total_secretary_amount, " +
        "       total_tasks_count, total_work_time, finalized_at, status, " +
        "       created_at, updated_at, deleted_at " +
        "  FROM secretary_monthly_summaries " +
        " WHERE deleted_at IS NULL AND secretary_id = ? " +
        "   AND target_year_month BETWEEN ? AND ? " +
        " ORDER BY target_year_month";

    /** 期間内の秘書×月ごとの金額（adminダッシュボードのピボット集計で利用） */
    private static final String SQL_COSTS_BY_SECRETARY_MONTH =
        "SELECT s.id AS sid, s.name AS sname, m.target_year_month AS ym, " +
        "       COALESCE(SUM(m.total_secretary_amount),0) AS amt " +
        "  FROM secretary_monthly_summaries m " +
        "  JOIN secretaries s ON s.id = m.secretary_id AND s.deleted_at IS NULL " +
        " WHERE m.deleted_at IS NULL " +
        "   AND m.target_year_month BETWEEN ? AND ? " +
        " GROUP BY s.id, s.name, m.target_year_month " +
        " ORDER BY s.name, m.target_year_month";

    // =========================
    // ② フィールド / コンストラクタ
    // =========================

    /**
     * コンストラクタ。
     *
     * @param conn 既存のDBコネクション（トランザクションは呼出し側で管理）
     */
    public SecretaryMonthlySummaryDAO(Connection conn) {
        super(conn);
    }

    // =========================
    // ③ メソッド
    //   - アクターごとにブロック化
    // =========================

    // ---------------------------------
    // 【secretary】マイページ/サマリ
    // ---------------------------------

    // =========================
    // SELECT
    // =========================

    /**
     * 【secretary】秘書の通算サマリ値（取り分合計・タスク件数・稼働分）を取得します。
     * <p>
     * - 集計対象は {@code deleted_at IS NULL} のみ。<br>
     * - NULL になる可能性のある合計値は SQL 側で COALESCE により 0 を返します。<br>
     * - DTO 側の件数/分は {@code int} 相当に丸めています（Long→int、安全な範囲想定）。
     * </p>
     *
     * @param secretaryId 秘書ID
     * @return 合計値を格納した {@link SecretaryTotalsDTO}。データが無い場合でも 0 埋めで返却。
     * @throws DAOException 取得に失敗した場合
     */
    public SecretaryTotalsDTO selectTotals(UUID secretaryId) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_TOTALS)) {
            // 1) 入力秘書IDで集計
            ps.setObject(1, secretaryId);

            try (ResultSet rs = ps.executeQuery()) {
                SecretaryTotalsDTO d = new SecretaryTotalsDTO();
                if (rs.next()) {
                    // 金額は BigDecimal のまま（COALESCEで0保証）
                    d.setTotalSecretaryAmount(rs.getBigDecimal("amt"));

                    // 件数・分は long で読み、int に安全に縮小（データ設計上オーバーフロー非想定）
                    long cntL  = rs.getLong("cnt");
                    d.setTotalTasksCount(rs.wasNull() ? null : Math.toIntExact(cntL));

                    long minsL = rs.getLong("mins");
                    d.setTotalWorkTime(rs.wasNull() ? null : Math.toIntExact(minsL));
                }
                return d;
            }
        } catch (SQLException e) {
            // 上位(Service)がハンドリングしやすいようにDAO例外へ変換
            throw new DAOException("E:SMS11 合計取得に失敗しました。", e);
        }
    }

    /**
     * 【secretary】指定期間（境界値含む）の直近12か月サマリを年月昇順で取得します。
     * <p>
     * - 期間は {@code target_year_month BETWEEN fromYm AND toYm} でインクルーシブ。<br>
     * - 列はDTOに必要な範囲でマッピング。件数/稼働分はlong→intに縮小。<br>
     * - ソートは {@code target_year_month ASC} で表示側の折れ線/棒グラフに使いやすくします。
     * </p>
     *
     * @param secretaryId 秘書ID
     * @param fromYm      期間開始（yyyy-MM, 含む）
     * @param toYm        期間終了（yyyy-MM, 含む）
     * @return {@link SecretaryMonthlySummaryDTO} のリスト（期間内が0件なら空リスト）
     * @throws DAOException 取得に失敗した場合
     */
    public List<SecretaryMonthlySummaryDTO> selectLast12Months(UUID secretaryId, String fromYm, String toYm) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_LAST12)) {
            // 1) 主キー + 期間境界
            ps.setObject(1, secretaryId);
            ps.setString(2, fromYm);
            ps.setString(3, toYm);

            try (ResultSet rs = ps.executeQuery()) {
                List<SecretaryMonthlySummaryDTO> list = new ArrayList<>();

                while (rs.next()) {
                    // 2) 行をDTOへ詰め替え
                    SecretaryMonthlySummaryDTO d = new SecretaryMonthlySummaryDTO();
                    d.setId(rs.getObject(1, UUID.class));
                    // d.setSecretaryId(rs.getObject(2, UUID.class)); // DTOにフィールドがある場合のみ使用
                    d.setTargetYearMonth(rs.getString(3));
                    d.setTotalSecretaryAmount(rs.getBigDecimal(4));

                    long cntL  = rs.getLong(5);
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

    // ---------------------------------
    // 【admin】ダッシュボード/サマリ
    // ---------------------------------

    /**
     * 【admin】期間内（fromYm～toYm）の「秘書×月」の金額をピボット用に取得します。
     * <p>
     * - ベース集計は SQL 側で {@code SUM(total_secretary_amount)}、秘書×月で GROUP BY。<br>
     * - Java 側で {@link PivotRowDTO} に整形（行=秘書、列=monthsの各yyyy-MM、セル=金額）。<br>
     * - 存在しない月のセルは 0 で初期化します（表崩れ防止）。<br>
     * - 行合計（rowTotal）も Java 側で計算してセットします。<br>
     * - 利用想定：管理者の原価/売上集計画面での月別秘書軸ピボット表示。
     * </p>
     *
     * @param fromYm  集計期間開始（yyyy-MM, 含む）
     * @param toYm    集計期間終了（yyyy-MM, 含む）
     * @param months  列順制御用の月配列（表示したいyyyy-MMの順序）。未返却の月も 0 で埋めます。
     * @return ピボット行のリスト（秘書名昇順、同一秘書内は月昇順で集計）
     * @throws DAOException 取得に失敗した場合
     */
    public List<PivotRowDTO> selectCostsBySecretaryMonth(String fromYm, String toYm, List<String> months) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_COSTS_BY_SECRETARY_MONTH)) {
            // 1) 期間境界（インクルーシブ）
            ps.setString(1, fromYm);
            ps.setString(2, toYm);

            // 2) 結果格納マップ（key=秘書ID）
            Map<UUID, PivotRowDTO> map = new LinkedHashMap<>();

            try (ResultSet rs = ps.executeQuery()) {
                // 3) SQL結果を一旦「秘書×月の明細」として読み込み
                while (rs.next()) {
                    UUID sid   = rs.getObject("sid", UUID.class);
                    String nm  = rs.getString("sname");
                    String ym  = rs.getString("ym");
                    BigDecimal amt = rs.getBigDecimal("amt"); // COALESCE済

                    // 3-1) 行が無ければ初期化（months分の列を0で作っておく）
                    PivotRowDTO row = map.get(sid);
                    if (row == null) {
                        row = new PivotRowDTO();
                        row.setId(sid);
                        row.setLabel(nm);
                        for (String m : months) {
                            row.getAmountByYm().put(m, BigDecimal.ZERO);
                        }
                        map.put(sid, row);
                    }

                    // 3-2) 対象月セルを上書き
                    row.getAmountByYm().put(ym, (amt == null) ? BigDecimal.ZERO : amt);
                }
            }

            // 4) 行合計（rowTotal）を算出してセット（表示ヘッダの合計列用）
            for (PivotRowDTO r : map.values()) {
                BigDecimal sum = BigDecimal.ZERO;
                for (String ym : months) {
                    sum = sum.add(r.getAmountByYm().getOrDefault(ym, BigDecimal.ZERO));
                }
                r.setRowTotal(sum);
            }

            // 5) 表示順は SQL で秘書名昇順に出ているため、そのまま保持
            return new ArrayList<>(map.values());

        } catch (SQLException e) {
            throw new DAOException("E:SMS21 秘書×月の支出取得に失敗しました。", e);
        }
    }
}
