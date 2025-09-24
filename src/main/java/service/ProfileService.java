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

/**
 * プロフィール（稼働条件）用サービス。
 * ルーティング:
 *  - /secretary/profile              : 表示
 *  - /secretary/profile/register     : 登録フォーム表示
 *  - /secretary/profile/register_done: 登録実行（UPSERT）
 *  - /secretary/profile/edit         : 変更フォーム表示
 *  - /secretary/profile/edit_done    : 変更実行（UPSERT）
 */
public class ProfileService extends BaseService {

    private static final String VIEW_HOME     = "profile/secretary/home";
    private static final String VIEW_REGISTER = "profile/secretary/register";
    private static final String VIEW_EDIT     = "profile/secretary/edit";

    // param names
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

    private static final String A_PROFILE = "profile";
    private final Converter conv = new Converter();

    public ProfileService(HttpServletRequest req, boolean useDB) { super(req, useDB); }

    /** 表示 */
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
            req.setAttribute("errorMsg", validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /** 登録フォーム表示（未登録時の初期値=不可/0） */
    public String register() {
        UUID myId = currentSecretaryId();
        if (myId == null) return errorAuth();
        pushFormBackToRequest();
        return VIEW_REGISTER;
    }

    /** 登録実行（UPSERT） */
    public String registerDone() {
        UUID myId = currentSecretaryId();
        if (myId == null) return errorAuth();

        ProfileDTO d = buildDtoFromParams(myId);
        validate(d);
        if (validation.hasErrorMsg()) {
            req.setAttribute("errorMsg", validation.getErrorMsg());
            pushFormBackToRequest();
            return VIEW_REGISTER;
        }

        try (TransactionManager tm = new TransactionManager()) {
            new ProfileDAO(tm.getConnection()).upsert(d);
            tm.commit();
            return req.getContextPath() + "/secretary/profile";
        } catch (RuntimeException e) {
        	e.printStackTrace();
            validation.addErrorMsg("プロフィール登録に失敗しました。");
            req.setAttribute("errorMsg", validation.getErrorMsg());
            pushFormBackToRequest();
            return VIEW_REGISTER;
        }
    }

    /** 変更フォーム表示 */
    public String edit() {
        UUID myId = currentSecretaryId();
        if (myId == null) return errorAuth();
        try (TransactionManager tm = new TransactionManager()) {
            ProfileDAO dao = new ProfileDAO(tm.getConnection());
            ProfileDTO dto = dao.selectBySecretaryId(myId);
            req.setAttribute(A_PROFILE, conv.toDomain(dto));
            pushFormBackToRequest();
            return VIEW_EDIT;
        } catch (RuntimeException e) {
            validation.addErrorMsg("プロフィールの取得に失敗しました。");
            req.setAttribute("errorMsg", validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /** 変更実行（UPSERT） */
    public String editDone() {
        UUID myId = currentSecretaryId();
        if (myId == null) return errorAuth();

        ProfileDTO d = buildDtoFromParams(myId);
        validate(d);
        if (validation.hasErrorMsg()) {
            req.setAttribute("errorMsg", validation.getErrorMsg());
            pushFormBackToRequest();
            return VIEW_EDIT;
        }

        try (TransactionManager tm = new TransactionManager()) {
            new ProfileDAO(tm.getConnection()).upsert(d);
            tm.commit();
            return req.getContextPath() + "/secretary/profile";
        } catch (RuntimeException e) {
            validation.addErrorMsg("プロフィール更新に失敗しました。");
            req.setAttribute("errorMsg", validation.getErrorMsg());
            pushFormBackToRequest();
            return VIEW_EDIT;
        }
    }

    // -------- helpers --------

    private ProfileDTO buildDtoFromParams(UUID myId) {
        ProfileDTO d = new ProfileDTO();
        d.setSecretaryId(myId);
        d.setWeekdayMorning(i(P_WM)); d.setWeekdayDaytime(i(P_WD)); d.setWeekdayNight(i(P_WN));
        d.setSaturdayMorning(i(P_SM)); d.setSaturdayDaytime(i(P_SD)); d.setSaturdayNight(i(P_SN));
        d.setSundayMorning(i(P_UM)); d.setSundayDaytime(i(P_UD)); d.setSundayNight(i(P_UN));
        d.setWeekdayWorkHours(dec(P_WH_WD));
        d.setSaturdayWorkHours(dec(P_WH_ST));
        d.setSundayWorkHours(dec(P_WH_SU));
        d.setMonthlyWorkHours(dec(P_MONTH));
        d.setRemark(req.getParameter(P_REMARK));
        d.setQualification(req.getParameter(P_QUALI));
        d.setWorkHistory(req.getParameter(P_WORK));
        d.setAcademicBackground(req.getParameter(P_ACADEMIC));
        d.setSelfIntroduction(req.getParameter(P_SELF));
        return d;
    }

    /** 稼働可否(0/1/2)と時間の妥当性をチェック */
    private void validate(ProfileDTO d) {
        checkFlag(d.getWeekdayMorning(), "平日(朝)");
        checkFlag(d.getWeekdayDaytime(), "平日(日中)");
        checkFlag(d.getWeekdayNight(), "平日(夜)");
        checkFlag(d.getSaturdayMorning(), "土曜(朝)");
        checkFlag(d.getSaturdayDaytime(), "土曜(日中)");
        checkFlag(d.getSaturdayNight(), "土曜(夜)");
        checkFlag(d.getSundayMorning(), "日曜(朝)");
        checkFlag(d.getSundayDaytime(), "日曜(日中)");
        checkFlag(d.getSundayNight(), "日曜(夜)");

        checkHours(d.getWeekdayWorkHours(), "平日就業可能時間", 0, 24);
        checkHours(d.getSaturdayWorkHours(), "土曜就業可能時間", 0, 24);
        checkHours(d.getSundayWorkHours(), "日曜就業可能時間", 0, 24);
        checkHours(d.getMonthlyWorkHours(), "月の就業時間数", 0, 744);
    }

    private void checkFlag(Integer v, String label) {
        if (v == null || v < 0 || v > 2) {
            validation.addErrorMsg(label + " は 0/1/2 で入力してください。");
        }
    }

    private void checkHours(BigDecimal v, String label, int min, int max) {
        if (v == null) return;
        if (v.compareTo(new BigDecimal(min)) < 0 || v.compareTo(new BigDecimal(max)) > 0) {
            validation.addErrorMsg(label + " は " + min + "〜" + max + " の範囲で入力してください。");
        }
    }

    private Integer i(String p) {
        try { return Integer.valueOf(req.getParameter(p)); } catch (Exception e) { return 0; }
    }

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

    private String errorAuth() {
        validation.addErrorMsg("ログイン情報が見つかりません。");
        req.setAttribute("errorMsg", validation.getErrorMsg());
        return req.getContextPath() + req.getServletPath() + "/error";
    }

    private UUID currentSecretaryId() {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object u = session.getAttribute("loginUser");
        if (u instanceof LoginUser lu && lu.getSecretary() != null) return lu.getSecretary().getId();
        return null;
    }

    private void pushFormBackToRequest() {
        String[] names = {
            P_WM,P_WD,P_WN,P_SM,P_SD,P_SN,P_UM,P_UD,P_UN,
            P_WH_WD,P_WH_ST,P_WH_SU,P_MONTH,P_REMARK,P_QUALI,P_WORK,P_ACADEMIC,P_SELF
        };
        for (String n : names) req.setAttribute(n, req.getParameter(n));
    }
}
