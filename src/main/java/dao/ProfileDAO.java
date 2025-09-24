package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dto.ProfileDTO;

/**
 * profiles テーブルDAO。
 * <p>主に「秘書IDに紐づく1件」を前提に、UPSERTで登録/変更を担います。</p>
 */
public class ProfileDAO extends BaseDAO {

    // ============== SQL ==============
    private static final String SQL_SELECT_BY_SECRETARY =
        "SELECT id, secretary_id, " +
        " weekday_morning, weekday_daytime, weekday_night, " +
        " saturday_morning, saturday_daytime, saturday_night, " +
        " sunday_morning, sunday_daytime, sunday_night, " +
        " weekday_work_hours, saturday_work_hours, sunday_work_hours, " +
        " monthly_work_hours, remark, qualification, work_history, academic_background, self_introduction, " +
        " created_at, updated_at, deleted_at " +
        "FROM profiles WHERE deleted_at IS NULL AND secretary_id = ?";

    private static final String SQL_UPSERT =
    	    "INSERT INTO profiles ("
    	  + " id, secretary_id, "
    	  + " weekday_morning, weekday_daytime, weekday_night, "
    	  + " saturday_morning, saturday_daytime, saturday_night, "
    	  + " sunday_morning, sunday_daytime, sunday_night, "
    	  + " weekday_work_hours, saturday_work_hours, sunday_work_hours, "
    	  + " monthly_work_hours, remark, qualification, work_history, academic_background, self_introduction, "
    	  + " created_at, updated_at"
    	  + ") VALUES ("
    	  + " gen_random_uuid(), "     // id
    	  + " ?, ?, ?, ?, ?,	 ?, ?, ?, ?, ?, 	?, ?, ?, ?, ?,	 ?, ?, ?, ?,  " // ← ここが 19 個の ? になる
    	  + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP"
    	  + ") ON CONFLICT (secretary_id) DO UPDATE SET "
    	  + " weekday_morning     = EXCLUDED.weekday_morning, "
    	  + " weekday_daytime     = EXCLUDED.weekday_daytime, "
    	  + " weekday_night       = EXCLUDED.weekday_night, "
    	  + " saturday_morning    = EXCLUDED.saturday_morning, "
    	  + " saturday_daytime    = EXCLUDED.saturday_daytime, "
    	  + " saturday_night      = EXCLUDED.saturday_night, "
    	  + " sunday_morning      = EXCLUDED.sunday_morning, "
    	  + " sunday_daytime      = EXCLUDED.sunday_daytime, "
    	  + " sunday_night        = EXCLUDED.sunday_night, "
    	  + " weekday_work_hours  = EXCLUDED.weekday_work_hours, "
    	  + " saturday_work_hours = EXCLUDED.saturday_work_hours, "
    	  + " sunday_work_hours   = EXCLUDED.sunday_work_hours, "
    	  + " monthly_work_hours  = EXCLUDED.monthly_work_hours, "
    	  + " remark              = EXCLUDED.remark, "
    	  + " qualification       = EXCLUDED.qualification, "
    	  + " work_history        = EXCLUDED.work_history, "
    	  + " academic_background = EXCLUDED.academic_background, "
    	  + " self_introduction   = EXCLUDED.self_introduction, "
    	  + " updated_at          = CURRENT_TIMESTAMP";
    
    
 // ★ 置き換え：pref_score を内側で計算 → 外側で参照できるようにする
    private static final String SQL_SELECT_CANDIDATE_SECRETARY =
        "WITH sms AS (\n" +
        "  SELECT secretary_id, SUM(total_work_time) AS total_min\n" +
        "  FROM secretary_monthly_summaries\n" +
        "  WHERE target_year_month = ?\n" +                                // 1: prevYM
        "  GROUP BY secretary_id\n" +
        ")\n" +
        "SELECT * FROM (\n" +
        "  SELECT\n" +
        "    s.id AS id,\n" +
        "    s.name AS name,\n" +
        "    COALESCE(sr.rank_name, '') AS rank_name,\n" +
        "    p.weekday_morning   AS wd_m,\n" +
        "    p.weekday_daytime   AS wd_d,\n" +
        "    p.weekday_night     AS wd_n,\n" +
        "    p.saturday_morning  AS sa_m,\n" +
        "    p.saturday_daytime  AS sa_d,\n" +
        "    p.saturday_night    AS sa_n,\n" +
        "    p.sunday_morning    AS su_m,\n" +
        "    p.sunday_daytime    AS su_d,\n" +
        "    p.sunday_night      AS su_n,\n" +
        "    COALESCE(p.weekday_work_hours,  0)::int AS wd_hours,\n" +
        "    COALESCE(p.saturday_work_hours, 0)::int AS sa_hours,\n" +
        "    COALESCE(p.sunday_work_hours,   0)::int AS su_hours,\n" +
        "    COALESCE(p.monthly_work_hours,  0)::int AS total_hours,\n" +
        "    CEIL(COALESCE(sms.total_min,0)/60.0)::int AS last_month_hours,\n" +
        "    (COALESCE(p.monthly_work_hours,0) - CEIL(COALESCE(sms.total_min,0)/60.0))::int AS capacity,\n" +
        "    CASE WHEN p.weekday_morning  >= 2 THEN 2 WHEN p.weekday_morning  = 1 THEN 1 ELSE 0 END AS sc_wd_m,\n" +
        "    CASE WHEN p.weekday_daytime  >= 2 THEN 2 WHEN p.weekday_daytime  = 1 THEN 1 ELSE 0 END AS sc_wd_d,\n" +
        "    CASE WHEN p.weekday_night    >= 2 THEN 2 WHEN p.weekday_night    = 1 THEN 1 ELSE 0 END AS sc_wd_n,\n" +
        "    CASE WHEN p.saturday_morning >= 2 THEN 2 WHEN p.saturday_morning = 1 THEN 1 ELSE 0 END AS sc_sa_m,\n" +
        "    CASE WHEN p.saturday_daytime >= 2 THEN 2 WHEN p.saturday_daytime = 1 THEN 1 ELSE 0 END AS sc_sa_d,\n" +
        "    CASE WHEN p.saturday_night   >= 2 THEN 2 WHEN p.saturday_night   = 1 THEN 1 ELSE 0 END AS sc_sa_n,\n" +
        "    CASE WHEN p.sunday_morning   >= 2 THEN 2 WHEN p.sunday_morning   = 1 THEN 1 ELSE 0 END AS sc_su_m,\n" +
        "    CASE WHEN p.sunday_daytime   >= 2 THEN 2 WHEN p.sunday_daytime   = 1 THEN 1 ELSE 0 END AS sc_su_d,\n" +
        "    CASE WHEN p.sunday_night     >= 2 THEN 2 WHEN p.sunday_night     = 1 THEN 1 ELSE 0 END AS sc_su_n,\n" +
        "    (?::int)*CASE WHEN p.weekday_morning  >= 2 THEN 2 WHEN p.weekday_morning  = 1 THEN 1 ELSE 0 END +\n" + // 2
        "    (?::int)*CASE WHEN p.weekday_daytime  >= 2 THEN 2 WHEN p.weekday_daytime  = 1 THEN 1 ELSE 0 END +\n" + // 3
        "    (?::int)*CASE WHEN p.weekday_night    >= 2 THEN 2 WHEN p.weekday_night    = 1 THEN 1 ELSE 0 END +\n" + // 4
        "    (?::int)*CASE WHEN p.saturday_morning >= 2 THEN 2 WHEN p.saturday_morning = 1 THEN 1 ELSE 0 END +\n" + // 5
        "    (?::int)*CASE WHEN p.saturday_daytime >= 2 THEN 2 WHEN p.saturday_daytime = 1 THEN 1 ELSE 0 END +\n" + // 6
        "    (?::int)*CASE WHEN p.saturday_night   >= 2 THEN 2 WHEN p.saturday_night   = 1 THEN 1 ELSE 0 END +\n" + // 7
        "    (?::int)*CASE WHEN p.sunday_morning   >= 2 THEN 2 WHEN p.sunday_morning   = 1 THEN 1 ELSE 0 END +\n" + // 8
        "    (?::int)*CASE WHEN p.sunday_daytime   >= 2 THEN 2 WHEN p.sunday_daytime   = 1 THEN 1 ELSE 0 END +\n" + // 9
        "    (?::int)*CASE WHEN p.sunday_night     >= 2 THEN 2 WHEN p.sunday_night     = 1 THEN 1 ELSE 0 END\n" +   // 10
        "    AS pref_score\n" +
        "  FROM profiles p\n" +
        "  JOIN secretaries s ON s.id = p.secretary_id AND s.deleted_at IS NULL\n" +
        "  LEFT JOIN secretary_rank sr ON sr.id = s.secretary_rank_id\n" +           // ★ pluralに修正
        "  LEFT JOIN sms ON sms.secretary_id = s.id\n" +
        "  WHERE p.deleted_at IS NULL\n" +
        "    AND (NOT ? OR p.weekday_morning  IN (1,2))\n" + // 11
        "    AND (NOT ? OR p.weekday_daytime  IN (1,2))\n" + // 12
        "    AND (NOT ? OR p.weekday_night    IN (1,2))\n" + // 13
        "    AND (NOT ? OR p.saturday_morning IN (1,2))\n" + // 14
        "    AND (NOT ? OR p.saturday_daytime IN (1,2))\n" + // 15
        "    AND (NOT ? OR p.saturday_night   IN (1,2))\n" + // 16
        "    AND (NOT ? OR p.sunday_morning   IN (1,2))\n" + // 17
        "    AND (NOT ? OR p.sunday_daytime   IN (1,2))\n" + // 18
        "    AND (NOT ? OR p.sunday_night     IN (1,2))\n" + // 19
        ") q\n" +
        "ORDER BY\n" +
        "  CASE WHEN (?::int + ?::int + ?::int + ?::int + ?::int + ?::int + ?::int + ?::int + ?::int) > 0\n" +
        "       THEN q.pref_score END DESC, ";

           

    public ProfileDAO(Connection conn) { super(conn); }

    // ============== SELECT ==============

    /**
     * 秘書IDでプロフィールを1件取得します。
     * @param secretaryId 秘書ID
     * @return 見つかればDTO、なければ null
     * @throws DAOException DB失敗時
     */
    public ProfileDTO selectBySecretaryId(UUID secretaryId) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_SECRETARY)) {
            ps.setObject(1, secretaryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                ProfileDTO d = new ProfileDTO();
                int i = 1;
                d.setId((UUID) rs.getObject(i++));
                d.setSecretaryId((UUID) rs.getObject(i++));
                d.setWeekdayMorning(rs.getInt(i++));
                d.setWeekdayDaytime(rs.getInt(i++));
                d.setWeekdayNight(rs.getInt(i++));
                d.setSaturdayMorning(rs.getInt(i++));
                d.setSaturdayDaytime(rs.getInt(i++));
                d.setSaturdayNight(rs.getInt(i++));
                d.setSundayMorning(rs.getInt(i++));
                d.setSundayDaytime(rs.getInt(i++));
                d.setSundayNight(rs.getInt(i++));
                d.setWeekdayWorkHours(rs.getBigDecimal(i++));
                d.setSaturdayWorkHours(rs.getBigDecimal(i++));
                d.setSundayWorkHours(rs.getBigDecimal(i++));
                d.setMonthlyWorkHours(rs.getBigDecimal(i++));
                d.setRemark(rs.getString(i++));
                d.setQualification(rs.getString(i++));
                d.setWorkHistory(rs.getString(i++));
                d.setAcademicBackground(rs.getString(i++));
                d.setSelfIntroduction(rs.getString(i++));
                d.setCreatedAt(rs.getTimestamp(i++));
                d.setUpdatedAt(rs.getTimestamp(i++));
                d.setDeletedAt(rs.getTimestamp(i++));
                return d;
            }
        } catch (SQLException e) {
            throw new DAOException("E:PRF-01 プロフィール取得に失敗しました。", e);
        }
    }
    
    
    /**
     * アサイン登録画面用の「秘書候補」一覧を返す。
     * - profiles 登録済み秘書のみ
     * - 〇/△ のみ抽出（指定列のみ）
     * - 「〇→△」優先はフィルタ指定がある時のみ適用
     * - 先月稼働（切上げ）と余力を算出
     * - 指定ソート + name ASC
     */
    public List<Map<String, Object>> selectSecretaryCandidatesForRegister(
            String targetYM,
            boolean fWdM, boolean fWdD, boolean fWdN,
            boolean fSaM, boolean fSaD, boolean fSaN,
            boolean fSuM, boolean fSuD, boolean fSuN,
            String sortKey, boolean desc) {

        String prevYM = YearMonth.parse(targetYM).minusMonths(1).toString();

        String sortCol = switch (sortKey == null ? "" : sortKey) {
            case "wdHours"    -> "wd_hours";
            case "saHours"    -> "sa_hours";
            case "suHours"    -> "su_hours";
            case "totalHours" -> "total_hours";
            case "lastMonth"  -> "last_month_hours";
            case "capacity"   -> "capacity";
            default           -> "capacity";
        };
        String dir = desc ? "DESC" : "ASC";
        
     // ★ q. を付けて外側でソート
        String sql = SQL_SELECT_CANDIDATE_SECRETARY + "q." + sortCol + " " + dir + ", q.name ASC";

        List<Map<String, Object>> list = new ArrayList<>();
//        String sql = SQL_SELECT_CANDIDATE_SECRETARY + sortCol + " " + dir + ", name ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, prevYM);        // 1: 先月(YYYY-MM)

            // pref_score の重み（ON=1/OFF=0）
            ps.setInt(i++, fWdM ? 1 : 0);     // 2
            ps.setInt(i++, fWdD ? 1 : 0);     // 3
            ps.setInt(i++, fWdN ? 1 : 0);     // 4
            ps.setInt(i++, fSaM ? 1 : 0);     // 5
            ps.setInt(i++, fSaD ? 1 : 0);     // 6
            ps.setInt(i++, fSaN ? 1 : 0);     // 7
            ps.setInt(i++, fSuM ? 1 : 0);     // 8
            ps.setInt(i++, fSuD ? 1 : 0);     // 9
            ps.setInt(i++, fSuN ? 1 : 0);     // 10

            // WHERE の ON/OFF
            ps.setBoolean(i++, fWdM);         // 11
            ps.setBoolean(i++, fWdD);         // 12
            ps.setBoolean(i++, fWdN);         // 13
            ps.setBoolean(i++, fSaM);         // 14
            ps.setBoolean(i++, fSaD);         // 15
            ps.setBoolean(i++, fSaN);         // 16
            ps.setBoolean(i++, fSuM);         // 17
            ps.setBoolean(i++, fSuD);         // 18
            ps.setBoolean(i++, fSuN);         // 19

            // ORDER BY の「指定あり」判定用（sum>0）
            ps.setInt(i++, fWdM ? 1 : 0);     // 20
            ps.setInt(i++, fWdD ? 1 : 0);     // 21
            ps.setInt(i++, fWdN ? 1 : 0);     // 22
            ps.setInt(i++, fSaM ? 1 : 0);     // 23
            ps.setInt(i++, fSaD ? 1 : 0);     // 24
            ps.setInt(i++, fSaN ? 1 : 0);     // 25
            ps.setInt(i++, fSuM ? 1 : 0);     // 26
            ps.setInt(i++, fSuD ? 1 : 0);     // 27
            ps.setInt(i++, fSuN ? 1 : 0);     // 28

            try (ResultSet rs = ps.executeQuery()) {
            	
                while (rs.next()) {
                	System.out.println("hoge");
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",        rs.getObject("id"));
                    m.put("name",      rs.getString("name"));
                    m.put("rankName",  rs.getString("rank_name"));

                    m.put("wdAm", rs.getInt("wd_m"));
                    m.put("wdDay", rs.getInt("wd_d"));
                    m.put("wdNight", rs.getInt("wd_n"));
                    m.put("saAm", rs.getInt("sa_m"));
                    m.put("saDay", rs.getInt("sa_d"));
                    m.put("saNight", rs.getInt("sa_n"));
                    m.put("suAm", rs.getInt("su_m"));
                    m.put("suDay", rs.getInt("su_d"));
                    m.put("suNight", rs.getInt("su_n"));

                    m.put("wdHours",    rs.getInt("wd_hours"));
                    m.put("saHours",    rs.getInt("sa_hours"));
                    m.put("suHours",    rs.getInt("su_hours"));
                    m.put("totalHours", rs.getInt("total_hours"));
                    m.put("lastMonth",  rs.getInt("last_month_hours"));
                    m.put("capacity",    rs.getInt("capacity"));
                    m.put("pref_score",  rs.getInt("pref_score"));
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("E:PRF-03 秘書候補一覧の取得に失敗しました。", e);
        }
        return list;
    }

    // ============== UPSERT ==============

    /**
     * secretary_id をキーに UPSERT（登録/更新）します。
     * @param d 保存対象DTO（secretaryId必須）
     * @return 影響行数
     * @throws DAOException DB失敗時
     */
    public int upsert(ProfileDTO d) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPSERT)) {
            int i = 1;
            ps.setObject(i++, d.getSecretaryId());
            ps.setObject(i++, safeInt(d.getWeekdayMorning()));
            ps.setObject(i++, safeInt(d.getWeekdayDaytime()));
            ps.setObject(i++, safeInt(d.getWeekdayNight()));
            ps.setObject(i++, safeInt(d.getSaturdayMorning()));
            ps.setObject(i++, safeInt(d.getSaturdayDaytime()));
            ps.setObject(i++, safeInt(d.getSaturdayNight()));
            ps.setObject(i++, safeInt(d.getSundayMorning()));
            ps.setObject(i++, safeInt(d.getSundayDaytime()));
            ps.setObject(i++, safeInt(d.getSundayNight()));
            ps.setBigDecimal(i++, nz(d.getWeekdayWorkHours()));
            ps.setBigDecimal(i++, nz(d.getSaturdayWorkHours()));
            ps.setBigDecimal(i++, nz(d.getSundayWorkHours()));
            ps.setBigDecimal(i++, nz(d.getMonthlyWorkHours()));
            ps.setString(i++, d.getRemark());
            ps.setString(i++, d.getQualification());
            ps.setString(i++, d.getWorkHistory());
            ps.setString(i++, d.getAcademicBackground());
            ps.setString(i++, d.getSelfIntroduction());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:PRF-02 プロフィールUPSERTに失敗しました。", e);
        }
    }

    private Integer safeInt(Integer v) { return v == null ? Integer.valueOf(0) : v; }
    private java.math.BigDecimal nz(BigDecimal v) { return v == null ? java.math.BigDecimal.ZERO : v; }
}
