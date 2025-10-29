package service;

import java.math.BigDecimal;
import java.util.UUID;

import dao.ProfileDAO;
import dao.TransactionManager;
import dto.ProfileDTO;
import domain.LoginUser;
import domain.Profile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import util.ConvertUtil;

/**
 * 【secretary】機能：プロフィール（稼働条件）サービス
 *
 * <p>ルーティング（FrontController）：</p>
 * <ul>
 *   <li>/secretary/profile               → {@link #view()}（表示）</li>
 *   <li>/secretary/profile/register      → {@link #register()}（登録フォーム）</li>
 *   <li>/secretary/profile/register_done → {@link #registerDone()}（登録実行：UPSERT）</li>
 *   <li>/secretary/profile/edit          → {@link #edit()}（変更フォーム）</li>
 *   <li>/secretary/profile/edit_done     → {@link #editDone()}（変更実行：UPSERT）</li>
 * </ul>
 *
 * <p>バリデーションは {@link BaseService#validation} を用い、DBアクセスは {@link TransactionManager} 経由。</p>
 */
public class ProfileService extends BaseService {

    // =========================
    // ① 定数・共通化（パラメータ名／パス／フォーマッタ／コンバータ）
    // =========================

    // ----- View paths -----
    private static final String VIEW_HOME     = "profile/secretary/home";
    private static final String VIEW_REGISTER = "profile/secretary/register";
    private static final String VIEW_EDIT     = "profile/secretary/edit";

    // ----- Attribute keys -----
    private static final String A_PROFILE = "profile";
    private static final String A_ERROR   = "errorMsg"; // 既存JSPが参照しているキー名は不変

    // ----- Request parameter keys -----
    private static final String P_WM = "weekdayMorning";
    private static final String P_WD = "weekdayDaytime";
    private static final String P_WN = "weekdayNight";
    private static final String P_SM = "saturdayMorning";
    private static final String P_SD = "saturdayDaytime";
    private static final String P_SN = "saturdayNight";
    private static final String P_UM = "sundayMorning";
    private static final String P_UD = "sundayDaytime";
    private static final String P_UN = "sundayNight";

    private static final String P_WH_WD = "weekdayWorkHours";
    private static final String P_WH_ST = "saturdayWorkHours";
    private static final String P_WH_SU = "sundayWorkHours";
    private static final String P_MONTH = "monthlyWorkHours";

    private static final String P_REMARK   = "remark";
    private static final String P_QUALI    = "qualification";
    private static final String P_WORK     = "workHistory";
    private static final String P_ACADEMIC = "academicBackground";
    private static final String P_SELF     = "selfIntroduction";

    // ----- Validation ranges -----
    private static final int HOURS_MIN = 0;
    private static final int HOURS_MAX_DAILY = 24;   // 1日の上限
    private static final int HOURS_MAX_MONTH = 744;  // 31日×24h 想定上限

    // ----- Converter -----
    private final ConvertUtil conv = new ConvertUtil();

    // =========================
    // ② フィールド、コンストラクタ
    // =========================

    /**
     * コンストラクタ。
     * @param req   {@link HttpServletRequest}
     * @param useDB DB使用フラグ（本サービスでは各メソッド内でTM生成）
     */
    public ProfileService(HttpServletRequest req, boolean useDB) {
        super(req, useDB);
    }

    // =========================
    // ③ メソッド（コントローラ呼び出しメソッド：secretary用）→ ④ヘルパー
    // =========================

    // ------------------------------------------------------------------
    // 「【secretary】機能：プロフィール表示」
    // ------------------------------------------------------------------

    /**
     * 「プロフィール表示」
     * <ul>
     *   <li>セッション中の秘書IDを取得し、該当プロフィールを取得して {@code profile} としてJSPに渡す。</li>
     *   <li>認証情報が無い場合は共通エラーへ遷移。</li>
     * </ul>
     * @return 表示ビュー（/WEB-INF/jsp/profile/secretary/home.jsp）
     */
    public String view() {
        UUID myId = currentSecretaryId();
        if (myId == null) return errorAuth();
        try (TransactionManager tm = new TransactionManager()) {
            ProfileDAO dao = new ProfileDAO(tm.getConnection());
            ProfileDTO d = dao.selectBySecretaryId(myId);
            Profile p = conv.toDomain(d);
            req.setAttribute(A_PROFILE, p);
            return VIEW_HOME;
        } catch (RuntimeException e) {
            validation.addErrorMsg("プロフィールの取得に失敗しました。");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // ------------------------------------------------------------------
    // 「【secretary】機能：プロフィール登録（フォーム表示→登録実行）」
    // ------------------------------------------------------------------

    /**
     * 「プロフィール登録フォーム」表示。
     * <ul>
     *   <li>未ログインなら共通エラーへ。</li>
     *   <li>フォーム戻し用に、直前の入力値（パラメータ）を同名属性で詰め替える。</li>
     * </ul>
     * @return 登録ビュー（/WEB-INF/jsp/profile/secretary/register.jsp）
     */
    public String register() {
        UUID myId = currentSecretaryId();
        if (myId == null) return errorAuth();
        pushFormBackToRequest();
        return VIEW_REGISTER;
    }

    /**
     * 「プロフィール登録」実行（UPSERT）。
     * <ul>
     *   <li>request param からDTOを組み立て、可否(0/1/2)と就業時間の妥当性を検証。</li>
     *   <li>エラー時は {@code errorMsg} とフォーム値を戻して登録画面へ。</li>
     *   <li>成功時は UPSERT → コミット → {@code /secretary/profile} にリダイレクト。</li>
     * </ul>
     * @return リダイレクト先 または 登録ビュー
     */
    public String registerDone() {
        UUID myId = currentSecretaryId();
        if (myId == null) return errorAuth();

        ProfileDTO d = buildDtoFromParams(myId);
        validate(d);
        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            pushFormBackToRequest();
            return VIEW_REGISTER;
        }

        try (TransactionManager tm = new TransactionManager()) {
            new ProfileDAO(tm.getConnection()).upsert(d);
            tm.commit();
            return req.getContextPath() + "/secretary/profile";
        } catch (RuntimeException e) {
            validation.addErrorMsg("プロフィール登録に失敗しました。");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            pushFormBackToRequest();
            return VIEW_REGISTER;
        }
    }

    // ------------------------------------------------------------------
    // 「【secretary】機能：プロフィール編集（フォーム表示→変更実行）」
    // ------------------------------------------------------------------

    /**
     * 「プロフィール編集フォーム」表示。
     * <ul>
     *   <li>該当プロフィールを取得して {@code profile} として渡し、加えてフォーム戻し値も詰める。</li>
     *   <li>未ログインなら共通エラーへ。</li>
     * </ul>
     * @return 編集ビュー（/WEB-INF/jsp/profile/secretary/edit.jsp）
     */
    public String edit() {
        UUID myId = currentSecretaryId();
        if (myId == null) return errorAuth();
        try (TransactionManager tm = new TransactionManager()) {
            ProfileDAO dao = new ProfileDAO(tm.getConnection());
            ProfileDTO dto = dao.selectBySecretaryId(myId);
            req.setAttribute(A_PROFILE, conv.toDomain(dto));
            pushFormBackToRequest(); // 入力エラーから戻ってきた際の値保全
            return VIEW_EDIT;
        } catch (RuntimeException e) {
            validation.addErrorMsg("プロフィールの取得に失敗しました。");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /**
     * 「プロフィール変更」実行（UPSERT）。
     * <ul>
     *   <li>request param からDTOを構築→妥当性検証→UPSERT。</li>
     *   <li>エラー時は {@code errorMsg} とフォーム値を戻して編集画面へ。</li>
     *   <li>成功時はコミット後、{@code /secretary/profile} にリダイレクト。</li>
     * </ul>
     * @return リダイレクト先 または 編集ビュー
     */
    public String editDone() {
        UUID myId = currentSecretaryId();
        if (myId == null) return errorAuth();

        ProfileDTO d = buildDtoFromParams(myId);
        validate(d);
        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            pushFormBackToRequest();
            return VIEW_EDIT;
        }

        try (TransactionManager tm = new TransactionManager()) {
            new ProfileDAO(tm.getConnection()).upsert(d);
            tm.commit();
            return req.getContextPath() + "/secretary/profile";
        } catch (RuntimeException e) {
            validation.addErrorMsg("プロフィール更新に失敗しました。");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            pushFormBackToRequest();
            return VIEW_EDIT;
        }
    }

    // =========================
    // ④ ヘルパー
    // =========================

    /**
     * リクエストパラメータから {@link ProfileDTO} を構築する。
     * <p>数値項目は安全にパースし、エラーは {@code validation} に積み上げる。</p>
     * @param myId セッション中の秘書ID
     * @return 構築済み DTO
     */
    private ProfileDTO buildDtoFromParams(UUID myId) {
        ProfileDTO d = new ProfileDTO();
        d.setSecretaryId(myId);

        // 稼働可否（0:不可 / 1:可 / 2:応相談）を想定
        d.setWeekdayMorning(i(P_WM));
        d.setWeekdayDaytime(i(P_WD));
        d.setWeekdayNight(i(P_WN));
        d.setSaturdayMorning(i(P_SM));
        d.setSaturdayDaytime(i(P_SD));
        d.setSaturdayNight(i(P_SN));
        d.setSundayMorning(i(P_UM));
        d.setSundayDaytime(i(P_UD));
        d.setSundayNight(i(P_UN));

        // 就業可能時間（h）
        d.setWeekdayWorkHours(dec(P_WH_WD));
        d.setSaturdayWorkHours(dec(P_WH_ST));
        d.setSundayWorkHours(dec(P_WH_SU));
        d.setMonthlyWorkHours(dec(P_MONTH));

        // フリーテキスト系
        d.setRemark(req.getParameter(P_REMARK));
        d.setQualification(req.getParameter(P_QUALI));
        d.setWorkHistory(req.getParameter(P_WORK));
        d.setAcademicBackground(req.getParameter(P_ACADEMIC));
        d.setSelfIntroduction(req.getParameter(P_SELF));
        return d;
    }

    /**
     * 稼働可否(0/1/2)と就業時間の妥当性を検証する。
     * @param d 入力DTO
     */
    private void validate(ProfileDTO d) {
        checkFlag(d.getWeekdayMorning(),  "平日(朝)");
        checkFlag(d.getWeekdayDaytime(),  "平日(日中)");
        checkFlag(d.getWeekdayNight(),    "平日(夜)");
        checkFlag(d.getSaturdayMorning(), "土曜(朝)");
        checkFlag(d.getSaturdayDaytime(), "土曜(日中)");
        checkFlag(d.getSaturdayNight(),   "土曜(夜)");
        checkFlag(d.getSundayMorning(),   "日曜(朝)");
        checkFlag(d.getSundayDaytime(),   "日曜(日中)");
        checkFlag(d.getSundayNight(),     "日曜(夜)");

        checkHours(d.getWeekdayWorkHours(),  "平日就業可能時間", HOURS_MIN, HOURS_MAX_DAILY);
        checkHours(d.getSaturdayWorkHours(), "土曜就業可能時間", HOURS_MIN, HOURS_MAX_DAILY);
        checkHours(d.getSundayWorkHours(),   "日曜就業可能時間", HOURS_MIN, HOURS_MAX_DAILY);
        checkHours(d.getMonthlyWorkHours(),  "月の就業時間数",   HOURS_MIN, HOURS_MAX_MONTH);
    }

    /**
     * 稼働可否の整数フラグを検証（null/範囲外→エラー）。
     * @param v 値
     * @param label UI表示用ラベル
     */
    private void checkFlag(Integer v, String label) {
        if (v == null || v < 0 || v > 2) {
            validation.addErrorMsg(label + " は 0/1/2 で入力してください。");
        }
    }

    /**
     * 時間系 BigDecimal の範囲検証（nullは未入力として許可）。
     * @param v 値
     * @param label ラベル
     * @param min 最小
     * @param max 最大
     */
    private void checkHours(BigDecimal v, String label, int min, int max) {
        if (v == null) return;
        if (v.compareTo(new BigDecimal(min)) < 0 || v.compareTo(new BigDecimal(max)) > 0) {
            validation.addErrorMsg(label + " は " + min + "〜" + max + " の範囲で入力してください。");
        }
    }

    /**
     * 整数パラメータの安全パース。失敗時は 0 を返す。
     * @param p パラメータ名
     * @return 取得値 or 0
     */
    private Integer i(String p) {
        try {
            return Integer.valueOf(req.getParameter(p));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * BigDecimal パラメータの安全パース。空は null、失敗はバリデーションエラー。
     * @param p パラメータ名
     * @return 取得値 or null
     */
    private BigDecimal dec(String p) {
        try {
            String s = req.getParameter(p);
            if (s == null || s.isBlank()) return null;
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            validation.addErrorMsg(p + " は数値で入力してください。");
            return null;
        }
    }

    /**
     * 権限エラー（未ログイン等）の共通処理。
     * @return 共通エラーページ
     */
    private String errorAuth() {
        validation.addErrorMsg("ログイン情報が見つかりません。");
        req.setAttribute(A_ERROR, validation.getErrorMsg());
        return req.getContextPath() + req.getServletPath() + "/error";
    }

    /**
     * セッションから秘書ユーザのUUIDを取得。
     * @return 秘書ID or null
     */
    private UUID currentSecretaryId() {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object u = session.getAttribute("loginUser");
        if (u instanceof LoginUser lu && lu.getSecretary() != null) {
            return lu.getSecretary().getId();
        }
        return null;
    }

    /**
     * 直前のフォーム入力値を、同名の属性として詰め直す（JSPでの再表示用）。
     * <p>属性キー名はパラメータ名と同一で、既存JSPの参照を壊さない。</p>
     */
    private void pushFormBackToRequest() {
        String[] names = {
            P_WM, P_WD, P_WN, P_SM, P_SD, P_SN, P_UM, P_UD, P_UN,
            P_WH_WD, P_WH_ST, P_WH_SU, P_MONTH,
            P_REMARK, P_QUALI, P_WORK, P_ACADEMIC, P_SELF
        };
        for (String n : names) {
            req.setAttribute(n, req.getParameter(n));
        }
    }
}
