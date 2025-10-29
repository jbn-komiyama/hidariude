package service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import util.ConvertUtil;
import dao.AssignmentDAO;
import dao.TaskDAO;
import dao.TransactionManager;
import domain.Assignment;
import domain.LoginUser;
import domain.Task;
import dto.AssignmentDTO;
import dto.TaskDTO;

/**
 * =========================
 * 【共通】タスク機能サービス
 * =========================
 *
 * <p>FrontController から呼び出されるコントローラ用サービスクラス。<br/>
 * アクター（権限）別にメソッドを配置：
 * <ul>
 *   <li>Secretary（秘書）… 業務の登録/編集/削除/一覧</li>
 *   <li>Admin（管理者）… 一覧（各タブ）、一括承認/取消、差戻</li>
 * </ul>
 *
 * <h3>構成</h3>
 * ① 定数・共通化（パラメータ名／パス／フォーマッタ）<br/>
 * ② フィールド、コンストラクタ<br/>
 * ③ メソッド（コントローラ呼び出しメソッド → ヘルパー）<br/>
 *
 * <h3>注意</h3>
 * - JSP から受け取るパラメータ名（req.getParameter("…")）と、
 *   JSP へ渡す setAttribute 名は既存値をそのまま使用（変更なし）<br/>
 * - FrontController から参照されない未使用メソッドは削除済み（taskList, adminTaskList）
 */
public class TaskService extends BaseService {

    // =========================================================
    // ① 定数・共通化（パラメータ名／アトリビュート／ビュー／共通Formatter等）
    // =========================================================

    // ---- View（秘書）----
    private static final String VIEW_TASK_EDIT     = "task/secretary/edit";
    private static final String VIEW_SEC_ALL       = "task/secretary/list_all";
    private static final String VIEW_SEC_APPROVED  = "task/secretary/list_approved";
    private static final String VIEW_SEC_UNAPP     = "task/secretary/list_unapproved";
    private static final String VIEW_SEC_REMAND    = "task/secretary/list_remanded";
    private static final String VIEW_TASK_REGISTER = "task/secretary/register";

    // ---- View（管理）----
    private static final String VIEW_ADMIN_ALL    = "task/admin/list_all";
    private static final String VIEW_ADMIN_UNAPP  = "task/admin/list_unapproved";
    private static final String VIEW_ADMIN_APP    = "task/admin/list_approved";
    private static final String VIEW_ADMIN_REMAND = "task/admin/list_remanded";
    private static final String VIEW_ADMIN_ALERT = "task/admin/alert";

    // ---- Request Param ----
    private static final String P_ID            = "id";
    private static final String P_COMPANY_ID    = "companyId";            // UUID文字列
    private static final String P_COMPANY_NAME  = "companyName";
    private static final String P_YEAR_MONTH    = "yearMonth";            // "yyyy-MM"
    private static final String P_STATUS        = "status";
    private static final String P_WORK_DATE     = "workDate";             // yyyy-MM-dd
    private static final String P_START_TIME    = "startTime";            // HH:mm
    private static final String P_END_TIME      = "endTime";              // HH:mm
    private static final String P_ASSIGNMENT_ID = "assignmentId";
    private static final String P_WORK_CONTENT  = "workContent";
    private static final String P_CLEAR_REMAND  = "clearRemand";          // "1" で差戻クリア
    private static final String P_SEC_NAME      = "sec";                  // 秘書名 like
    private static final String P_CUST_NAME     = "cust";                 // 顧客名 like

    // ---- Request Attr ----（既存 JSP 名を変更せず使用）
    private static final String A_TASKS            = "tasks";
    private static final String A_SUM              = "sum";
    private static final String A_COUNT            = "count";
    private static final String A_TOTAL_MINUTE     = "totalMinute";
    private static final String A_ASSIGNMENTS      = "assignments";
    private static final String A_ASSIGNMENTS_ALL  = "assignmentsAll";
    private static final String A_YEAR_MONTH       = "yearMonth";
    private static final String A_COMPANY_NAME     = "companyName";
    private static final String A_COMPANY_ID       = "companyId";
    private static final String A_ERROR_MSG        = "errorMsg";

    // ---- Session ----
    private static final String ATTR_LOGIN_USER = "loginUser";

    // ---- Timezone/Format ----
    private static final ZoneId Z_TOKYO             = ZoneId.of("Asia/Tokyo");
    private static final DateTimeFormatter YM_FMT   = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter YMD_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HM_FMT   = DateTimeFormatter.ofPattern("HH:mm");

    // =========================================================
    // ② フィールド / コンストラクタ
    // =========================================================

    // DTO→Domain 変換（共通コンバータ）
    private final ConvertUtil conv = new ConvertUtil();

    public TaskService(HttpServletRequest req, boolean useDB) {
        super(req, useDB);
    }

    // =========================================================
    // ③-1. Secretary（秘書）用：一覧（4タブ）
    // =========================================================

    // =========================
    // 「【secretary】　機能：タスク一覧（全件タブ）
    // =========================
    /**
     * 秘書向けタブ「全件」一覧表示。
     * <ul>
     *   <li>request param: yearMonth（任意）</li>
     *   <li>session: loginUser.secretary.id（必須）</li>
     *   <li>yearMonth 未指定時は Asia/Tokyo の当月</li>
     * </ul>
     * @return JSP パス（list_all）
     */
    public String secretaryTaskListAll() {
        return loadSecPage("all", VIEW_SEC_ALL);
    }

    // =========================
    // 「【secretary】　機能：タスク一覧（承認済タブ）
    // =========================
    /**
     * 秘書向けタブ「承認済」一覧表示。
     * @return JSP パス（list_approved）
     */
    public String secretaryTaskListApproved() {
        return loadSecPage("approved", VIEW_SEC_APPROVED);
    }

    // =========================
    // 「【secretary】　機能：タスク一覧（未承認タブ）
    // =========================
    /**
     * 秘書向けタブ「未承認」一覧表示。
     * @return JSP パス（list_unapproved）
     */
    public String secretaryTaskListUnapproved() {
        return loadSecPage("unapproved", VIEW_SEC_UNAPP);
    }

    // =========================
    // 「【secretary】　機能：タスク一覧（差戻タブ）
    // =========================
    /**
     * 秘書向けタブ「差戻」一覧表示。
     * @return JSP パス（list_remanded）
     */
    public String secretaryTaskListRemanded() {
        return loadSecPage("remanded", VIEW_SEC_REMAND);
    }

    /**
     * （秘書共通）一覧ローディング共通処理。
     * @param status   "all" / "approved" / "unapproved" / "remanded"
     * @param viewName 戻り JSP
     */
    private String loadSecPage(String status, String viewName) {
        final String ym = Optional.ofNullable(req.getParameter(P_YEAR_MONTH))
                .filter(s -> !s.isBlank())
                .orElse(LocalDate.now(Z_TOKYO).format(YM_FMT));

        // 認証（秘書）
        HttpSession session = req.getSession(false);
        if (session == null) {
            req.setAttribute(A_ERROR_MSG, "セッションが切れました。");
            return REDIRECT_ERROR;
        }
        LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
        UUID secretaryId = (lu != null && lu.getSecretary() != null) ? lu.getSecretary().getId() : null;
        if (secretaryId == null) {
            req.setAttribute(A_ERROR_MSG, "ログイン情報が見つかりません。");
            return REDIRECT_ERROR;
        }

        // 取得
        try (TransactionManager tm = new TransactionManager()) {
            TaskDAO dao = new TaskDAO(tm.getConnection());
            List<TaskDTO> dtos = dao.selectBySecretaryAndMonth(secretaryId, ym, status);

            List<Task> tasks = new ArrayList<>(dtos.size());
            BigDecimal sum = BigDecimal.ZERO;
            int totalMinute = 0;

            for (TaskDTO d : dtos) {
                Task t = conv.toDomain(d);
                totalMinute += (t.getWorkMinute() == null ? 0 : t.getWorkMinute());
                if (t.getFee() != null) sum = sum.add(t.getFee());
                tasks.add(t);
            }

            // JSP へ
            req.setAttribute(A_TASKS, tasks);
            req.setAttribute(A_SUM, sum);
            req.setAttribute(A_TOTAL_MINUTE, totalMinute);
            req.setAttribute(A_COUNT, tasks.size());
            req.setAttribute(A_YEAR_MONTH, ym);
            return viewName;

        } catch (RuntimeException e) {
            req.setAttribute(A_ERROR_MSG, "一覧の取得に失敗しました。");
            e.printStackTrace();
            return REDIRECT_ERROR;
        }
    }

    // =========================================================
    // ③-2. Secretary（秘書）用：登録/編集/削除
    // =========================================================

    // =========================
    // 「【secretary】　機能：登録画面（プルダウン用アサイン読込）
    // =========================
    /**
     * 業務登録画面の表示。
     * <ul>
     *   <li>request param: yearMonth（任意）</li>
     *   <li>session: loginUser.secretary.id（必須）</li>
     *   <li>表示用：当月（=yearMonth）の「自分×全顧客」アサイン一覧を付与（assignmentsAll）</li>
     * </ul>
     * @return JSP パス（register）
     */
    public String taskRegister() {
        final String ym = Optional.ofNullable(req.getParameter(P_YEAR_MONTH))
                .filter(s -> !s.isBlank())
                .orElse(LocalDate.now(Z_TOKYO).format(YM_FMT));

        HttpSession session = req.getSession(false);
        if (session == null) {
            req.setAttribute(A_ERROR_MSG, "セッションが切れました。再ログインしてください。");
            return REDIRECT_ERROR;
        }
        LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
        if (lu == null || lu.getSecretary() == null || lu.getSecretary().getId() == null) {
            req.setAttribute(A_ERROR_MSG, "ログイン情報が見つかりません。");
            return REDIRECT_ERROR;
        }
        final UUID secretaryId = lu.getSecretary().getId();

        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDAO asgDao = new AssignmentDAO(tm.getConnection());
            List<AssignmentDTO> asgDtos = asgDao.selectBySecretaryAndMonthToAssignment(secretaryId, ym);

            List<Assignment> assignmentsAll = new ArrayList<>(asgDtos.size());
            for (AssignmentDTO ad : asgDtos) assignmentsAll.add(conv.toDomain(ad));

            req.setAttribute(A_ASSIGNMENTS_ALL, assignmentsAll);
            req.setAttribute(A_YEAR_MONTH, ym);
            return VIEW_TASK_REGISTER;

        } catch (RuntimeException e) {
            e.printStackTrace();
            req.setAttribute(A_ERROR_MSG, "登録画面の表示に失敗しました。");
            return REDIRECT_ERROR;
        }
    }

    // =========================
    // 「【secretary】　機能：登録（ダイレクト保存）
    // =========================
    /**
     * 業務登録（確認画面なし・ダイレクト保存）。
     * <ul>
     *   <li>request param（必須）: companyId, assignmentId, workDate, startTime, endTime, workContent</li>
     *   <li>request param（任意）: companyName, yearMonth（未指定時は当月）</li>
     *   <li>server側で workMinute を再計算し、年月一致（表示中 yearMonth）もチェック</li>
     * </ul>
     * 成功後：未承認一覧へ PRG（<code>/task/list_unapproved?status=approved&yearMonth=…</code>）
     */
    public String taskRegisterDone() {
        final String companyIdStr   = req.getParameter(P_COMPANY_ID);
        final String yearMonth      = Optional.ofNullable(req.getParameter(P_YEAR_MONTH))
                .filter(s -> !s.isBlank())
                .orElse(LocalDate.now(Z_TOKYO).format(YM_FMT));
        final String workDateStr    = req.getParameter(P_WORK_DATE);
        final String startStr       = req.getParameter(P_START_TIME);
        final String endStr         = req.getParameter(P_END_TIME);
        final String assignmentIdStr= req.getParameter(P_ASSIGNMENT_ID);
        final String workContent    = req.getParameter(P_WORK_CONTENT);

        // 入力チェック
        if (!validation.isUuid(companyIdStr))  validation.addErrorMsg("顧客IDが不正です。");
        if (!validation.isUuid(assignmentIdStr)) validation.addErrorMsg("アサインIDが不正です。");
        if (workDateStr == null || startStr == null || endStr == null || workContent == null || workContent.isBlank()) {
            validation.addErrorMsg("必須項目が未入力です。");
        }

        // セッション（秘書）
        HttpSession session = req.getSession(false);
        if (session == null) validation.addErrorMsg("セッションが切れました。再ログインしてください。");
        if (validation.hasErrorMsg()) { req.setAttribute(A_ERROR_MSG, validation.getErrorMsg()); return REDIRECT_ERROR; }

        final UUID assignmentId= UUID.fromString(assignmentIdStr);

        // 分計算＆年月範囲
        LocalDate workDate; LocalTime st; LocalTime et;
        try {
            workDate = LocalDate.parse(workDateStr, YMD_FMT);
            st = LocalTime.parse(startStr, HM_FMT);
            et = LocalTime.parse(endStr, HM_FMT);
        } catch (Exception ex) {
            validation.addErrorMsg("日付/時刻の形式が不正です。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return REDIRECT_ERROR;
        }
        if (!et.isAfter(st)) validation.addErrorMsg("終了時刻は開始時刻より後にしてください。");
        if (!workDate.format(YM_FMT).equals(yearMonth)) {
            validation.addErrorMsg("日付は表示中の年月（" + yearMonth + "）の範囲で選択してください。");
        }
        if (validation.hasErrorMsg()) { req.setAttribute(A_ERROR_MSG, validation.getErrorMsg()); return REDIRECT_ERROR; }

        final int workMinute = (int) Duration.between(st, et).toMinutes();

        // 保存
        try (TransactionManager tm = new TransactionManager()) {
            TaskDAO taskDao = new TaskDAO(tm.getConnection());

            TaskDTO dto = new TaskDTO();
            AssignmentDTO ad = new AssignmentDTO();
            ad.setAssignmentId(assignmentId);
            dto.setAssignment(ad);

            dto.setWorkDate(Date.valueOf(workDate));
            dto.setStartTime(Timestamp.valueOf(workDate.atTime(st)));
            dto.setEndTime(Timestamp.valueOf(workDate.atTime(et)));
            dto.setWorkMinute(workMinute);
            dto.setWorkContent(workContent);

            UUID newId = taskDao.insert(dto);
            if (newId == null) throw new RuntimeException("INSERT が完了しませんでした。");

            tm.commit();

            // 一覧へ（会社名は JSP 側で利用／ここでは URL パラメータに含めない方針を踏襲）
            return req.getContextPath() + req.getServletPath()
                    + "/task/list_unapproved?status=approved&yearMonth=" + yearMonth;

        } catch (RuntimeException e) {
            validation.addErrorMsg("登録に失敗しました。入力内容を確認してください。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            e.printStackTrace();
            return REDIRECT_ERROR;
        }
    }

    // =========================
    // 「【secretary】　機能：編集画面表示
    // =========================
    /**
     * 業務編集画面の表示。
     * <ul>
     *   <li>request param（必須）: id, companyId</li>
     *   <li>request param（任意）: companyName, yearMonth（未指定時は当月）</li>
     *   <li>当月の「自分×顧客」アサイン一覧を付与（assignments）</li>
     *   <li>差戻中なら初期状態で解除チェック ON（hidden: clearRemand）</li>
     * </ul>
     * @return JSP パス（edit）
     */
    public String taskEdit() {
        final String idStr        = req.getParameter(P_ID);
        final String companyIdStr = req.getParameter(P_COMPANY_ID);
        final String companyName  = req.getParameter(P_COMPANY_NAME);
        String yearMonth          = req.getParameter(P_YEAR_MONTH);

        if (!validation.isUuid(idStr))        validation.addErrorMsg("編集対象IDが不正です。");
        if (!validation.isUuid(companyIdStr)) validation.addErrorMsg("顧客IDが不正です。");
        if (yearMonth == null || yearMonth.isBlank()) yearMonth = LocalDate.now(Z_TOKYO).format(YM_FMT);
        if (validation.hasErrorMsg()) { req.setAttribute(A_ERROR_MSG, validation.getErrorMsg()); return REDIRECT_ERROR; }

        // 認証（秘書）
        HttpSession session = req.getSession(false);
        if (session == null) { req.setAttribute(A_ERROR_MSG, "セッションが切れました。再ログインしてください。"); return REDIRECT_ERROR; }
        LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
        if (lu == null || lu.getSecretary() == null || lu.getSecretary().getId() == null) {
            req.setAttribute(A_ERROR_MSG, "ログイン情報が見つかりません。"); return REDIRECT_ERROR;
        }
        UUID secretaryId = lu.getSecretary().getId();
        UUID customerId  = UUID.fromString(companyIdStr);
        UUID taskId      = UUID.fromString(idStr);

        try (TransactionManager tm = new TransactionManager()) {
            TaskDAO dao = new TaskDAO(tm.getConnection());

            // 対象1件
            TaskDTO dto = dao.selectById(taskId);
            if (dto == null) { req.setAttribute(A_ERROR_MSG, "対象の業務が見つかりません。"); return REDIRECT_ERROR; }

            // 所有チェック
            UUID dtoSecretaryId = (dto.getAssignment() != null) ? dto.getAssignment().getAssignmentSecretaryId() : null;
            UUID dtoCustomerId  = (dto.getAssignment() != null) ? dto.getAssignment().getAssignmentCustomerId()  : null;
            if (!secretaryId.equals(dtoSecretaryId) || !customerId.equals(dtoCustomerId)) {
                req.setAttribute(A_ERROR_MSG, "この業務を編集する権限がありません。");
                return REDIRECT_ERROR;
            }

            // 表示用 Domain
            Task task = conv.toDomain(dto);

            // 当月の自分×顧客のアサイン一覧
            AssignmentDAO asgDao = new AssignmentDAO(tm.getConnection());
            List<AssignmentDTO> asgDtos = asgDao.selectBySecretaryAndCustomerAndMonth(secretaryId, customerId, yearMonth);
            List<Assignment> assignments = new ArrayList<>(asgDtos.size());
            for (AssignmentDTO ad : asgDtos) assignments.add(conv.toDomain(ad));

            // 差戻解除 初期ON（差戻中 or ?clearRemand=1）
            boolean clearRemandOnSave =
                    "1".equals(Optional.ofNullable(req.getParameter(P_CLEAR_REMAND)).orElse(""))
                 || (task.getRemandedAt() != null);
            req.setAttribute("clearRemand", clearRemandOnSave);
            req.setAttribute("remandComment", req.getParameter("remandComment"));

            // JSP へ
            req.setAttribute("task", task);
            req.setAttribute(A_ASSIGNMENTS, assignments);
            req.setAttribute(A_COMPANY_ID, customerId);
            req.setAttribute(A_COMPANY_NAME, companyName);
            req.setAttribute(A_YEAR_MONTH, yearMonth);
            req.setAttribute(P_ID, idStr);
            return VIEW_TASK_EDIT;

        } catch (RuntimeException e) {
            req.setAttribute(A_ERROR_MSG, "編集画面の表示に失敗しました。");
            e.printStackTrace();
            return REDIRECT_ERROR;
        }
    }

    // =========================
    // 「【secretary】　機能：更新（ダイレクト更新）
    // =========================
    /**
     * 業務更新の実行。
     * <ul>
     *   <li>request param（必須）: id, companyId, assignmentId, workDate, startTime, endTime, workContent</li>
     *   <li>request param（任意）: companyName, yearMonth（未指定時は当月）</li>
     *   <li>差戻解除: clearRemand=1 の場合は remanded_at をクリア</li>
     * </ul>
     * 成功後：未承認一覧へ PRG（<code>/task/list_unapproved?status=approved&yearMonth=…</code>）
     */
    public String taskEditDone() {
        final String idStr        = req.getParameter(P_ID);
        final String companyIdStr = req.getParameter(P_COMPANY_ID);
        final String yearMonth    = Optional.ofNullable(req.getParameter(P_YEAR_MONTH))
                .filter(s -> !s.isBlank())
                .orElse(LocalDate.now(Z_TOKYO).format(YM_FMT));
        final String workDateStr  = req.getParameter(P_WORK_DATE);
        final String startStr     = req.getParameter(P_START_TIME);
        final String endStr       = req.getParameter(P_END_TIME);
        final String assignmentIdStr = req.getParameter(P_ASSIGNMENT_ID);
        final String workContent  = req.getParameter(P_WORK_CONTENT);

        if (!validation.isUuid(idStr))            validation.addErrorMsg("対象IDが不正です。");
        if (!validation.isUuid(companyIdStr))     validation.addErrorMsg("顧客IDが不正です。");
        if (!validation.isUuid(assignmentIdStr))  validation.addErrorMsg("アサインIDが不正です。");
        if (workDateStr == null || startStr == null || endStr == null || workContent == null || workContent.isBlank()) {
            validation.addErrorMsg("必須項目が未入力です。");
        }
        if (validation.hasErrorMsg()) { req.setAttribute(A_ERROR_MSG, validation.getErrorMsg()); return REDIRECT_ERROR; }

        final UUID taskId      = UUID.fromString(idStr);
        final UUID assignmentId= UUID.fromString(assignmentIdStr);

        LocalDate workDate; LocalTime st; LocalTime et;
        try {
            workDate = LocalDate.parse(workDateStr, YMD_FMT);
            st = LocalTime.parse(startStr, HM_FMT);
            et = LocalTime.parse(endStr, HM_FMT);
        } catch (Exception ex) {
            validation.addErrorMsg("日付/時刻の形式が不正です。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return REDIRECT_ERROR;
        }
        if (!et.isAfter(st))                       validation.addErrorMsg("終了時刻は開始時刻より後にしてください。");
        if (!workDate.format(YM_FMT).equals(yearMonth)) {
            validation.addErrorMsg("日付は表示中の年月（" + yearMonth + "）の範囲で選択してください。");
        }
        if (validation.hasErrorMsg()) { req.setAttribute(A_ERROR_MSG, validation.getErrorMsg()); return REDIRECT_ERROR; }

        final int workMinute = (int) Duration.between(st, et).toMinutes();

        try (TransactionManager tm = new TransactionManager()) {
            TaskDAO taskDao = new TaskDAO(tm.getConnection());

            TaskDTO dto = new TaskDTO();
            dto.setId(taskId);
            AssignmentDTO ad = new AssignmentDTO();
            ad.setAssignmentId(assignmentId);
            dto.setAssignment(ad);

            dto.setWorkDate(Date.valueOf(workDate));
            dto.setStartTime(Timestamp.valueOf(workDate.atTime(st)));
            dto.setEndTime(Timestamp.valueOf(workDate.atTime(et)));
            dto.setWorkMinute(workMinute);
            dto.setWorkContent(workContent);

            int updated = taskDao.update(dto);
            if (updated == 0) throw new RuntimeException("UPDATE の対象が見つかりませんでした。");

            // 差戻解除
            boolean clearRemand = "1".equals(Optional.ofNullable(req.getParameter(P_CLEAR_REMAND)).orElse(""));
            if (clearRemand) taskDao.clearRemandedAt(taskId);

            tm.commit();

            return req.getContextPath() + req.getServletPath()
                    + "/task/list_unapproved?status=approved&yearMonth=" + yearMonth;

        } catch (RuntimeException e) {
            validation.addErrorMsg("更新に失敗しました。入力内容をご確認ください。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            e.printStackTrace();
            return REDIRECT_ERROR;
        }
    }

    // =========================
    // 「【secretary】　機能：論理削除
    // =========================
    /**
     * 業務の論理削除。
     * <ul>
     *   <li>request param（必須）: id</li>
     *   <li>request param（任意）: companyId, companyName, yearMonth（戻り先で利用）</li>
     * </ul>
     * 成功後：未承認一覧へ PRG（<code>/task/list_unapproved?status=approved&yearMonth=…</code>）
     */
    public String taskDeleteDone() {
        final String idStr        = req.getParameter(P_ID);
        final String yearMonth    = req.getParameter(P_YEAR_MONTH);

        if (!validation.isUuid(idStr)) {
            validation.addErrorMsg("削除対象IDが不正です。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return REDIRECT_ERROR;
        }
        final UUID taskId = UUID.fromString(idStr);

        try (TransactionManager tm = new TransactionManager()) {
            TaskDAO dao = new TaskDAO(tm.getConnection());
            int updated = dao.delete(taskId);
            if (updated == 0) {
                validation.addErrorMsg("対象の業務が見つからないか、すでに削除されています。");
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                return REDIRECT_ERROR;
            }
            tm.commit();

            return req.getContextPath() + req.getServletPath()
                    + "/task/list_unapproved?status=approved&yearMonth=" + yearMonth;

        } catch (RuntimeException e) {
            validation.addErrorMsg("削除に失敗しました。やり直してください。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            e.printStackTrace();
            return REDIRECT_ERROR;
        }
    }

    // =========================================================
    // ③-3. Admin（管理者）用：一覧タブ＋一括操作
    // =========================================================

    // =========================
    // 「【admin】　機能：一覧（全件）
    // =========================
    /**
     * 管理者向け「全件」タブの一覧表示。
     * <ul>
     *   <li>request param: yearMonth（任意）、sec（任意：秘書名部分一致）、cust（任意：顧客名部分一致）</li>
     * </ul>
     */
    public String adminTaskListAll() {
        String ym   = Optional.ofNullable(req.getParameter(P_YEAR_MONTH)).filter(s -> !s.isBlank())
                .orElse(LocalDate.now(Z_TOKYO).format(YM_FMT));
        String sec  = Optional.ofNullable(req.getParameter(P_SEC_NAME)).orElse("");
        String cust = Optional.ofNullable(req.getParameter(P_CUST_NAME)).orElse("");

        loadAdminList(ym, "all", sec, cust, req);
        req.setAttribute(A_YEAR_MONTH, ym);
        req.setAttribute(P_SEC_NAME, sec);
        req.setAttribute(P_CUST_NAME, cust);
        return VIEW_ADMIN_ALL;
    }

    // =========================
    // 「【admin】　機能：一覧（未承認）
    // =========================
    /**
     * 管理者向け「未承認」タブの一覧表示。
     */
    public String adminTaskListUnapproved() {
        String ym   = Optional.ofNullable(req.getParameter(P_YEAR_MONTH)).filter(s -> !s.isBlank())
                .orElse(LocalDate.now(Z_TOKYO).format(YM_FMT));
        String sec  = Optional.ofNullable(req.getParameter(P_SEC_NAME)).orElse("");
        String cust = Optional.ofNullable(req.getParameter(P_CUST_NAME)).orElse("");

        loadAdminList(ym, "unapproved", sec, cust, req);
        req.setAttribute(A_YEAR_MONTH, ym);
        req.setAttribute(P_SEC_NAME, sec);
        req.setAttribute(P_CUST_NAME, cust);
        return VIEW_ADMIN_UNAPP;
    }

    // =========================
    // 「【admin】　機能：一覧（承認済）
    // =========================
    /**
     * 管理者向け「承認済」タブの一覧表示。
     */
    public String adminTaskListApproved() {
        String ym   = Optional.ofNullable(req.getParameter(P_YEAR_MONTH)).filter(s -> !s.isBlank())
                .orElse(LocalDate.now(Z_TOKYO).format(YM_FMT));
        String sec  = Optional.ofNullable(req.getParameter(P_SEC_NAME)).orElse("");
        String cust = Optional.ofNullable(req.getParameter(P_CUST_NAME)).orElse("");

        loadAdminList(ym, "approved", sec, cust, req);
        req.setAttribute(A_YEAR_MONTH, ym);
        req.setAttribute(P_SEC_NAME, sec);
        req.setAttribute(P_CUST_NAME, cust);
        return VIEW_ADMIN_APP;
    }

    // =========================
    // 「【admin】　機能：一覧（差戻）
    // =========================
    /**
     * 管理者向け「差戻」タブの一覧表示。
     */
    public String adminTaskListRemanded() {
        String ym   = Optional.ofNullable(req.getParameter(P_YEAR_MONTH)).filter(s -> !s.isBlank())
                .orElse(LocalDate.now(Z_TOKYO).format(YM_FMT));
        String sec  = Optional.ofNullable(req.getParameter(P_SEC_NAME)).orElse("");
        String cust = Optional.ofNullable(req.getParameter(P_CUST_NAME)).orElse("");

        loadAdminList(ym, "remanded", sec, cust, req);
        req.setAttribute(A_YEAR_MONTH, ym);
        req.setAttribute(P_SEC_NAME, sec);
        req.setAttribute(P_CUST_NAME, cust);
        return VIEW_ADMIN_REMAND;
    }

    // =========================
    // 「【admin】　機能：一括承認
    // =========================
    /**
     * 管理者：選択タスクの一括承認。
     * <ul>
     *   <li>request param: taskIds（複数）, yearMonth（任意）, status（任意）</li>
     *   <li>承認済は DAO 側でスキップ想定</li>
     * </ul>
     * @return 一覧（未承認タブ）へ PRG（年月＆ステータス維持）
     */
    public String adminTaskApproveBulk() {
        final String[] idParams = req.getParameterValues("taskIds");
        final String yearMonth = Optional.ofNullable(req.getParameter(P_YEAR_MONTH))
                .filter(s -> !s.isBlank())
                .orElse(LocalDate.now(Z_TOKYO).format(YM_FMT));
        final String status = Optional.ofNullable(req.getParameter(P_STATUS))
                .filter(s -> !s.isBlank())
                .orElse("all");

        if (idParams == null || idParams.length == 0) {
            validation.addErrorMsg("承認対象が選択されていません。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return  adminTaskListUnapproved();
        }

        final List<UUID> ids = new ArrayList<>();
        for (String s : idParams) {
            try { ids.add(UUID.fromString(s)); } catch (Exception ignore) {}
        }
        if (ids.isEmpty()) {
            validation.addErrorMsg("承認対象IDが不正です。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return adminTaskListUnapproved();
        }

        UUID approverId = null;
        HttpSession session = req.getSession(false);
        if (session != null) {
            LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
            if (lu != null && lu.getSystemAdmin() != null) {
                approverId = lu.getSystemAdmin().getId();
            }
        }

        try (TransactionManager tm = new TransactionManager()) {
            TaskDAO dao = new TaskDAO(tm.getConnection());
            for (UUID id : ids) {
                try { dao.approve(id, approverId); }
                catch (RuntimeException ex) { ex.printStackTrace(); }
            }
            tm.commit();
        } catch (RuntimeException e) {
            validation.addErrorMsg("一括承認に失敗しました。やり直してください。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        return req.getContextPath() + "/admin/task/list_unapproved?yearMonth=" + yearMonth + "&status=" + status;
    }

    // =========================
    // 「【admin】　機能：一括承認取消
    // =========================
    /**
     * 管理者：選択タスクの一括承認取消。
     * <ul>
     *   <li>request param: taskIds（複数）, yearMonth, sec, cust（絞込維持）</li>
     * </ul>
     * @return 承認済一覧へ PRG（絞込維持）
     */
    public String adminTaskUnapproveBulk() {
        final String[] idParams = req.getParameterValues("taskIds");
        final String ym   = Optional.ofNullable(req.getParameter(P_YEAR_MONTH))
                .filter(s -> !s.isBlank())
                .orElse(LocalDate.now(Z_TOKYO).format(YM_FMT));
        final String sec  = Optional.ofNullable(req.getParameter(P_SEC_NAME)).orElse("");
        final String cust = Optional.ofNullable(req.getParameter(P_CUST_NAME)).orElse("");

        if (idParams == null || idParams.length == 0) {
            validation.addErrorMsg("対象が選択されていません。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return adminTaskListApproved();
        }

        final List<UUID> ids = new ArrayList<>();
        for (String s : idParams) { try { ids.add(UUID.fromString(s)); } catch (Exception ignore) {} }

        try (TransactionManager tm = new TransactionManager()) {
            TaskDAO dao = new TaskDAO(tm.getConnection());
            for (UUID id : ids) dao.unapprove(id);
            tm.commit();
        } catch (RuntimeException e) {
            validation.addErrorMsg("承認取消に失敗しました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
        return req.getContextPath() + "/admin/task/list_approved?yearMonth=" + ym
                + "&sec=" + urlEnc(sec) + "&cust=" + urlEnc(cust);
    }

    // =========================
    // 「【admin】　機能：差戻
    // =========================
    /**
     * 管理者：単票差戻。
     * <ul>
     *   <li>request param: taskId（必須）, remandComment（任意）, yearMonth/sec/cust（戻り先維持）</li>
     *   <li>差戻ユーザは systemAdmin.id を採用</li>
     * </ul>
     * @return 全件一覧へ PRG（絞込維持）
     */
    public String adminTaskRemandDone() {
        final String idStr   = req.getParameter("taskId");
        final String comment = Optional.ofNullable(req.getParameter("remandComment")).orElse("");
        final String ym      = Optional.ofNullable(req.getParameter(P_YEAR_MONTH)).orElse("");
        final String sec     = Optional.ofNullable(req.getParameter(P_SEC_NAME)).orElse("");
        final String cust    = Optional.ofNullable(req.getParameter(P_CUST_NAME)).orElse("");

        if (!validation.isUuid(idStr)) {
            validation.addErrorMsg("差戻対象IDが不正です。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        UUID remanderId = null;
        HttpSession session = req.getSession(false);
        if (session != null) {
            LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
            if (lu != null && lu.getSystemAdmin() != null) remanderId = lu.getSystemAdmin().getId();
        }

        try (TransactionManager tm = new TransactionManager()) {
            TaskDAO dao = new TaskDAO(tm.getConnection());
            dao.remand(UUID.fromString(idStr), remanderId, comment);
            tm.commit();
        } catch (RuntimeException e) {
            validation.addErrorMsg("差戻に失敗しました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
        return req.getContextPath() + "/admin/task/list_all?yearMonth=" + urlEnc(ym)
                + "&sec=" + urlEnc(sec) + "&cust=" + urlEnc(cust);
    }
    
    
    
    // =========================
    // 「【admin】　機能：一覧（アラート）
    // =========================
    /**
     * 管理者向け：顧客からの確認申請（アラート）一覧。
     * TaskDAO#showAlert(false) を呼び、DTO→Domain へ詰替えて JSP へ渡す。
     *
     * @return ビュー名 "task/admin/alert"
     */
    public String adminTaskAlertList() {
        try (TransactionManager tm = new TransactionManager()) {
            TaskDAO dao = new TaskDAO(tm.getConnection());
            // false: 件数絞りなし（設計どおり）
            List<TaskDTO> dtos = dao.showAlert(false);

            // 共通コンバータで Domain に詰替え
            List<Task> tasks = new ArrayList<>();
            for(TaskDTO dto : dtos) {
            	tasks.add(conv.toDomain(dto));
            }
            		

            // JSP へ
            req.setAttribute("tasks", tasks);
            return VIEW_ADMIN_ALERT;

        } catch (RuntimeException e) {
            e.printStackTrace();
            req.setAttribute("errorMsg", List.of("アラート一覧の取得に失敗しました。"));
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }
    
    
    // =========================
    // 「【admin】　機能：削除（アラート）
    // =========================
    /**
     * 管理者：アラート取消（alerted_at を NULL）。
     * <ul>
     *   <li>request param: id（必須）</li>
     *   <li>成功後：アラート一覧へリダイレクト</li>
     * </ul>
     */
    public String adminAlertDelete() {
        final String idStr = req.getParameter(P_ID); // "id"
        if (!validation.isUuid(idStr)) {
            req.setAttribute(A_ERROR_MSG, "対象IDが不正です。");
            return req.getContextPath() + req.getServletPath() + "/error";
        }
        final UUID taskId = UUID.fromString(idStr);

        try (TransactionManager tm = new TransactionManager()) {
            TaskDAO dao = new TaskDAO(tm.getConnection());
            int updated = dao.alertDelete(taskId);
            if (updated == 0) {
                req.setAttribute(A_ERROR_MSG, "対象のタスクが見つかりません。");
                return req.getContextPath() + req.getServletPath() + "/error";
            }
            tm.commit();
            // 一覧へ（/admin/task/alert を想定：adminTaskAlertList()）
            return req.getContextPath() + "/admin/task/alert";
        } catch (RuntimeException e) {
            e.printStackTrace();
            req.setAttribute(A_ERROR_MSG, "アラート取消に失敗しました。");
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }
    
    // =========================
    // customer 用
    // =========================

    /**
     * 顧客ユーザ向け：タスク一覧を表示します（見た目のみの「確認」ボタン付き）。
     * <ul>
     *   <li>ログイン中の顧客（customerId）に紐づく最新タスクを取得</li>
     *   <li>JSP へは属性名 {@code tasks} で渡します</li>
     *   <li>開始・終了時刻や日付は JSP 側でフォーマットします</li>
     * </ul>
     *
     * <p>取得する列（Map キー例）：
     * secretaryName / workDate / startAt / endAt / workMinutes / rankName / content
     * / unitPriceCustomer / unitPriceSecretary / taskId</p>
     *
     * @return ビュー名 "task/customer/list"
     */
    public String customerTaskList() {
    	
    	String ymParam = req.getParameter("ym");
        String ym;
        if (ymParam != null && ymParam.matches("\\d{4}-\\d{2}")) {
            ym = ymParam;
        } else {
            ym = YearMonth.now(ZoneId.systemDefault()).toString(); // "YYYY-MM"
        }
    	
        try (TransactionManager tm = new TransactionManager()) {
            // セッションからログインユーザを取得
            HttpSession session = req.getSession(false);
            LoginUser loginUser = (session == null) ? null : (LoginUser) session.getAttribute("loginUser");

            // 顧客IDを特定（LoginUser に顧客IDが無い場合は request param "customerId" を許容）
            UUID customerId = null;
            if (loginUser != null && loginUser.getCustomer().getId() != null) {
                customerId = loginUser.getCustomer().getId();
            } else {
                String p = req.getParameter("customerId");
                if (p != null && !p.isBlank()) {
                    try { customerId = UUID.fromString(p); } catch (IllegalArgumentException ignore) {}
                }
            }
            if (customerId == null) {
                // セッション切れや不正時はエラーページ
                req.setAttribute("errorMsg", List.of("顧客情報が見つかりませんでした。再ログインしてください。"));
                return req.getContextPath() + req.getServletPath() + "/error";
            }

            TaskDAO dao = new TaskDAO(tm.getConnection());
            // 必要に応じて limit を調整（ここでは直近 200 件）
            List<TaskDTO> tdto = dao.selectCustomerTasksForList(customerId, ym);
            List<Task> tasks = new ArrayList<>();
            for(TaskDTO dto : tdto) {
            	tasks.add(conv.toDomain(dto));
            }

            req.setAttribute("selectedYm", ym);
            req.setAttribute("tasks", tasks);
            return "task/customer/list";
        } catch (RuntimeException e) {
            // 例外時はエラーへ
        	e.printStackTrace();
            req.setAttribute("errorMsg", List.of("タスク一覧の取得に失敗しました。"));
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }
    
    /**
     * 顧客ユーザ向け：タスクの「確認申請（アラート）」を受け付けます。
     *
     * <p>JSP 側フォーム（POST）から以下のパラメータを受け取り、DAO を呼び出します。</p>
     * <ul>
     *   <li>id … タスク UUID（必須）</li>
     *   <li>comment … 入力ダイアログで入力されたコメント（任意）</li>
     * </ul>
     *
     * <h4>挙動</h4>
     * <ol>
     *   <li>パラメータ検証（id の UUID 形式）</li>
     *   <li>{@link TaskDAO#alert(String, java.util.UUID)} を実行</li>
     *   <li>成功時：顧客のタスク一覧へ PRG（<code>/customer/task/list</code>）</li>
     *   <li>失敗時：エラーページへ遷移</li>
     * </ol>
     *
     * @return 一覧へのリダイレクト URL またはエラー遷移先
     */
    public String customerTaskAlert() {
        final String idStr   = req.getParameter("id");
        final String comment = req.getParameter("comment"); // null 可

        // ---- 入力検証（id 必須）----
        if (!validation.isUuid(idStr)) {
            req.setAttribute("errorMsg", List.of("対象IDが不正です。"));
            return req.getContextPath() + req.getServletPath() + "/error";
        }
        final UUID taskId = UUID.fromString(idStr);

        // ---- 更新実行 ----
        try (TransactionManager tm = new TransactionManager()) {
            TaskDAO dao = new TaskDAO(tm.getConnection());
            int updated = dao.alert(comment, taskId);
            if (updated == 0) {
                req.setAttribute("errorMsg", List.of("対象のタスクが見つかりません。"));
                return req.getContextPath() + req.getServletPath() + "/error";
            }
            tm.commit();
            // 成功時は一覧へ（年月は未指定なら当月を一覧側でデフォルト処理）
            return req.getContextPath() + "/customer/task/list";
        } catch (RuntimeException e) {
            e.printStackTrace();
            req.setAttribute("errorMsg", List.of("確認申請の更新に失敗しました。"));
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    

    // =========================================================
    // ③-4. 内部ヘルパー
    // =========================================================

    /**
     * 管理者一覧（各タブ共通）ロードヘルパ。
     * 集計値（sumSecretary, sumCustomer, totalMinute, count）を request に格納。
     */
    private List<Task> loadAdminList(String yearMonth, String status, String secLike, String custLike,
                                     HttpServletRequest req) {
        try (TransactionManager tm = new TransactionManager()) {
            TaskDAO dao = new TaskDAO(tm.getConnection());
            List<TaskDTO> dtos = dao.selectByMonth(yearMonth, status, secLike, custLike);

            List<Task> tasks = new ArrayList<>(dtos.size());
            BigDecimal sumCustomer = BigDecimal.ZERO;
            BigDecimal sumSecretary = BigDecimal.ZERO;
            int totalMinute = 0;

            for (TaskDTO d : dtos) {
                Task t = conv.toDomain(d);
                totalMinute += (t.getWorkMinute() == null ? 0 : t.getWorkMinute());
                if (t.getFee() != null)         sumSecretary = sumSecretary.add(t.getFee());
                if (t.getFeeCustomer() != null) sumCustomer  = sumCustomer.add(t.getFeeCustomer());
                tasks.add(t);
            }

            // setAttribute 名は既存 JSP に合わせる（変更しない）
            req.setAttribute(A_TASKS, tasks);
            req.setAttribute("sumSecretary", sumSecretary);
            req.setAttribute("sumCustomer", sumCustomer);
            req.setAttribute(A_TOTAL_MINUTE, totalMinute);
            req.setAttribute(A_COUNT, tasks.size());
            return tasks;
        }
    }

    // URL エンコード（UTF-8）
    private static String urlEnc(String s) {
        try {
            return java.net.URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

}
