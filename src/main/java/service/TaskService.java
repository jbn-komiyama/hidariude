// TaskService.java
package service;


import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import dao.TaskDAO;
import dao.TransactionManager;
import domain.LoginUser;
import domain.Task;
import dto.TaskDTO;
import dao.AssignmentDAO;                 // ★
import dto.AssignmentDTO;                 // ★
import domain.Assignment;                 // ★

/**
 * タスク関連サービス
 */
public class TaskService extends BaseService {

    // ===== View =====
    private static final String VIEW_TASK_LIST = "task/secretary/list";
    private static final String VIEW_TASK_EDIT = "task/secretary/edit";

    // ===== Request Param =====
    private static final String P_ID          = "id";
    private static final String P_COMPANY_ID = "companyId";   // UUID文字列
    private static final String P_COMPANY_NAME = "companyName";   // UUID文字列
    private static final String P_YEAR_MONTH = "yearMonth";    // "yyyy-MM"
    private static final String P_WORK_DATE    = "workDate";   // yyyy-MM-dd
    private static final String P_START_TIME   = "startTime";  // HH:mm
    private static final String P_END_TIME     = "endTime";    // HH:mm
    private static final String P_ASSIGNMENT_ID= "assignmentId";
    private static final String P_WORK_CONTENT = "workContent";

    // ===== Request Attr =====
    private static final String A_TASKS       = "tasks";
    private static final String A_SUM       = "sum";
    private static final String A_ASSIGNMENTS  = "assignments";   // ★ 追加
    private static final String A_YEAR_MONTH  = "yearMonth";
    private static final String A_CUSTOMER_ID = "customerId";
    private static final String A_COMPANY_NAME = "companyName";
    private static final String A_COMPANY_ID = "companyId";
    private static final String A_ERROR_MSG   = "errorMsg";

    // ===== Session =====
    private static final String ATTR_LOGIN_USER = "loginUser";

    // ===== Timezone/Format =====
    private static final ZoneId Z_TOKYO = ZoneId.of("Asia/Tokyo");
    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter YMD_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HM_FMT  = DateTimeFormatter.ofPattern("HH:mm");


    // Converter を共通利用
    private final Converter conv = new Converter();

    public TaskService(HttpServletRequest req, boolean useDB) {
        super(req, useDB);
    }

    /**
     * タスク一覧表示
     * - リクエスト: companyId（必須）, yearMonth（任意）
     * - セッション: loginUser.secretary.id（必須）
     * - yearMonth 未指定時は Asia/Tokyo で現在の "yyyy-MM"
     * - 取得結果は DTO→Domain へ詰め替え、request に格納
     */
    public String taskList() {
        // --- 1) リクエストから取得 ---
        final String companyIdStr = req.getParameter(P_COMPANY_ID);
        final String companyName = req.getParameter(P_COMPANY_NAME);
        String yearMonth = req.getParameter(P_YEAR_MONTH);
        

        if (!validation.isUuid(companyIdStr)) {
            validation.addErrorMsg("顧客IDが不正です。");
        }
        if (yearMonth == null || yearMonth.isBlank()) {
            yearMonth = LocalDate.now(Z_TOKYO).format(YM_FMT); // "yyyy-MM"
        }

        // --- 2) セッションから自分の秘書ID ---
        HttpSession session = req.getSession(false);
        if (session == null) {
            validation.addErrorMsg("セッションが切れました。再ログインしてください。");
        }

        UUID secretaryId = null;
        if (session != null) {
            LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
            if (lu == null || lu.getSecretary() == null || lu.getSecretary().getId() == null) {
                validation.addErrorMsg("ログイン情報が見つかりません。");
            } else {
                secretaryId = lu.getSecretary().getId();
            }
        }

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        final UUID customerId = UUID.fromString(companyIdStr);

        // --- 3) 取得 ---
        try (TransactionManager tm = new TransactionManager()) {
            TaskDAO dao = new TaskDAO(tm.getConnection());
            List<TaskDTO> dtos = dao.selectBySecretaryAndCustomerAndMonth(secretaryId, customerId, yearMonth);

            // --- 4) Converter で詰め替え ---
            List<Task> tasks = new ArrayList<>(dtos.size());
            BigDecimal sum = BigDecimal.ZERO;
            for (TaskDTO d : dtos) {
            	Task t = conv.toDomain(d);
            	BigDecimal hourFee = BigDecimal.ZERO;

            	if (t.getAssignment().getCustomerBasedIncentiveForSecretary() != null) {
            		hourFee = hourFee.add(t.getAssignment().getCustomerBasedIncentiveForSecretary());
            	}
            	if (t.getAssignment().getBasePaySecretary() != null) {
            		hourFee = hourFee.add(t.getAssignment().getBasePaySecretary());
            	}
            	if (t.getAssignment().getIncreaseBasePaySecretary() != null) {
            		hourFee = hourFee.add(t.getAssignment().getIncreaseBasePaySecretary());
            	}
            	t.setHourFee(hourFee);
            	
                tasks.add(t);
                if (t.getFee() != null) {
                	sum = sum.add(t.getFee());
            	}
            }
            
            // --- assignments 取得（★ 追加） ---
            AssignmentDAO asgDao = new AssignmentDAO(tm.getConnection());  // 同一コネクションで実行
            List<AssignmentDTO> asgDtos =
                asgDao.selectBySecretaryAndCustomerAndMonth(secretaryId, customerId, yearMonth);

            List<Assignment> assignments = new ArrayList<>(asgDtos.size());
            for (AssignmentDTO ad : asgDtos) {
                assignments.add(conv.toDomain(ad)); // ※ Converter に AssignmentDTO→Assignment が必要
            }

            // --- 5) JSP へ引き渡し ---
            req.setAttribute(A_TASKS, tasks);
            req.setAttribute(A_SUM, sum);
            req.setAttribute(A_ASSIGNMENTS, assignments); // ★ 追加
            req.setAttribute(A_YEAR_MONTH, yearMonth);
            req.setAttribute(A_CUSTOMER_ID, customerId);
            req.setAttribute(A_COMPANY_NAME, companyName);
            return VIEW_TASK_LIST;

        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }
    
    /**
     * 業務登録（確認画面なし・ダイレクト保存）
     * フォームから受け取り、サーバ側で workMinute を再計算して保存。
     * 成功後はそのまま taskList() を再表示。
     */
    public String taskRegisterDone() {
        // ---- 1) パラメータ取得 ----
        final String companyIdStr  = req.getParameter(P_COMPANY_ID);
        final String companyName   = req.getParameter(P_COMPANY_NAME);
        final String yearMonth     = Optional.ofNullable(req.getParameter(P_YEAR_MONTH))
                                             .filter(s -> !s.isBlank())
                                             .orElse(LocalDate.now(Z_TOKYO).format(YM_FMT));
        final String workDateStr   = req.getParameter(P_WORK_DATE);
        final String startStr      = req.getParameter(P_START_TIME);
        final String endStr        = req.getParameter(P_END_TIME);
        final String assignmentIdStr = req.getParameter(P_ASSIGNMENT_ID);
        final String workContent   = req.getParameter(P_WORK_CONTENT);

        // ---- 2) バリデーション ----
        if (!validation.isUuid(companyIdStr)) {
            validation.addErrorMsg("顧客IDが不正です。");
        }
        if (!validation.isUuid(assignmentIdStr)) {
            validation.addErrorMsg("アサインIDが不正です。");
        }
        if (workDateStr == null || startStr == null || endStr == null || workContent == null || workContent.isBlank()) {
            validation.addErrorMsg("必須項目が未入力です。");
        }

        // セッションから秘書ID
        HttpSession session = req.getSession(false);
        if (session == null) {
            validation.addErrorMsg("セッションが切れました。再ログインしてください。");
        }
        UUID secretaryId = null;
        if (session != null) {
            LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
            if (lu == null || lu.getSecretary() == null || lu.getSecretary().getId() == null) {
                validation.addErrorMsg("ログイン情報が見つかりません。");
            } else {
                secretaryId = lu.getSecretary().getId();
            }
        }

        // 早期エラー
        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        final UUID customerId   = UUID.fromString(companyIdStr);
        final UUID assignmentId = UUID.fromString(assignmentIdStr);

        // ---- 3) サーバ側で計算＆月範囲チェック ----
        LocalDate workDate;
        LocalTime st, et;
        try {
            workDate = LocalDate.parse(workDateStr, YMD_FMT);
            st = LocalTime.parse(startStr, HM_FMT);
            et = LocalTime.parse(endStr, HM_FMT);
        } catch (Exception ex) {
            validation.addErrorMsg("日付/時刻の形式が不正です。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        // 同日内前提・終了 > 開始
        if (!et.isAfter(st)) {
            validation.addErrorMsg("終了時刻は開始時刻より後にしてください。");
        }

        // yearMonth 制約（当月のみ）
        String ymOfWorkDate = workDate.format(YM_FMT);
        if (!ymOfWorkDate.equals(yearMonth)) {
            validation.addErrorMsg("日付は表示中の年月（" + yearMonth + "）の範囲で選択してください。");
        }

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        int workMinute = (int) Duration.between(st, et).toMinutes();

        // ---- 4) 登録 ----
        try (TransactionManager tm = new TransactionManager()) {
            TaskDAO taskDao = new TaskDAO(tm.getConnection());

            TaskDTO dto = new TaskDTO();
            AssignmentDTO ad = new AssignmentDTO();
            ad.setAssignmentId(assignmentId);
            dto.setAssignment(ad);

            // 日付・時刻（TIMESTAMP は日付と結合）
            dto.setWorkDate(Date.valueOf(workDate));
            dto.setStartTime(Timestamp.valueOf(workDate.atTime(st)));
            dto.setEndTime(Timestamp.valueOf(workDate.atTime(et)));
            dto.setWorkMinute(workMinute);
            dto.setWorkContent(workContent);

            UUID newId = taskDao.insert(dto);
            if (newId == null) {
                throw new RuntimeException("INSERT が完了しませんでした。");
            }

            tm.commit();

            // 一覧へ戻す（同じ会社・年月）
            // パラメータは既に req に残っているが、念のため再設定
            req.setAttribute(A_COMPANY_NAME, companyName);
            req.setAttribute(A_YEAR_MONTH, yearMonth);
            req.setAttribute(A_CUSTOMER_ID, customerId);
            
            String nextPath = req.getContextPath() + req.getServletPath() + "/task/list" + "?" 
                    + A_COMPANY_ID + "=" + customerId 
                    + "&" + A_COMPANY_NAME + "=" + URLEncoder.encode(companyName, StandardCharsets.UTF_8)
                    + "&" + A_YEAR_MONTH + "=" + yearMonth;

            return nextPath;

        } catch (RuntimeException e) {
            validation.addErrorMsg("登録に失敗しました。入力内容を確認してください。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }
    
    /** 編集画面表示（/secretary/task/edit） */
    public String taskEdit() {
        final String idStr        = req.getParameter(P_ID);
        final String companyIdStr = req.getParameter(P_COMPANY_ID);
        final String companyName  = req.getParameter(P_COMPANY_NAME);
        String yearMonth          = req.getParameter(P_YEAR_MONTH);

        if (!validation.isUuid(idStr))        validation.addErrorMsg("編集対象IDが不正です。");
        if (!validation.isUuid(companyIdStr)) validation.addErrorMsg("顧客IDが不正です。");
        if (yearMonth == null || yearMonth.isBlank()) {
            yearMonth = LocalDate.now(Z_TOKYO).format(YM_FMT);
        }
        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return REDIRECT_ERROR;
        }

        // ログイン中の秘書ID
        HttpSession session = req.getSession(false);
        if (session == null) {
            validation.addErrorMsg("セッションが切れました。再ログインしてください。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return REDIRECT_ERROR;
        }
        LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
        if (lu == null || lu.getSecretary() == null || lu.getSecretary().getId() == null) {
            validation.addErrorMsg("ログイン情報が見つかりません。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return REDIRECT_ERROR;
        }
        UUID secretaryId = lu.getSecretary().getId();
        UUID customerId  = UUID.fromString(companyIdStr);
        UUID taskId      = UUID.fromString(idStr);

        try (TransactionManager tm = new TransactionManager()) {
            // ★ ここを必ず「宣言＋利用」でセットにする（変数名 dao に統一）
            TaskDAO dao = new TaskDAO(tm.getConnection());

            // 対象タスク 1件取得
            TaskDTO dto = dao.selectById(taskId);
            if (dto == null) {
                validation.addErrorMsg("対象の業務が見つかりません。");
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                return REDIRECT_ERROR;
            }

            // 所有チェック（秘書ID/顧客IDが一致するか）
            UUID dtoSecretaryId = (dto.getAssignment() != null) ? dto.getAssignment().getAssignmentSecretaryId() : null;
            UUID dtoCustomerId  = (dto.getAssignment() != null) ? dto.getAssignment().getAssignmentCustomerId()  : null;
            if (!secretaryId.equals(dtoSecretaryId) || !customerId.equals(dtoCustomerId)) {
                validation.addErrorMsg("この業務を編集する権限がありません。");
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                return REDIRECT_ERROR;
            }

            // 年月未指定ならタスクの日付から補完
            if (yearMonth == null || yearMonth.isBlank()) {
                if (dto.getWorkDate() != null) {
                    yearMonth = dto.getWorkDate().toLocalDate().format(YM_FMT);
                } else {
                    yearMonth = LocalDate.now(Z_TOKYO).format(YM_FMT);
                }
            }

            // 画面用ドメイン
            Task task = conv.toDomain(dto);

            // ドロップダウン：当月の自分×顧客のアサイン一覧
            AssignmentDAO asgDao = new AssignmentDAO(tm.getConnection());
            List<AssignmentDTO> asgDtos =
                asgDao.selectBySecretaryAndCustomerAndMonth(secretaryId, customerId, yearMonth);
            List<Assignment> assignments = new ArrayList<>(asgDtos.size());
            for (AssignmentDTO ad : asgDtos) assignments.add(conv.toDomain(ad));

            // JSP へ受け渡し
            req.setAttribute("task", task);
            req.setAttribute(A_ASSIGNMENTS, assignments);
            req.setAttribute(A_COMPANY_ID, customerId);
            req.setAttribute(A_COMPANY_NAME, companyName);
            req.setAttribute(A_YEAR_MONTH, yearMonth);
            req.setAttribute(P_ID, idStr);

            return VIEW_TASK_EDIT;
        } catch (RuntimeException e) {
            validation.addErrorMsg("編集画面の表示に失敗しました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            e.printStackTrace();
            return REDIRECT_ERROR;
        }
    }
    
    /** 更新実行（/secretary/task/update_done）→ 更新後 list へ戻るURLを返す */
    public String taskEditDone() {
        // 1) パラメータ
        final String idStr           = req.getParameter(P_ID);
        final String companyIdStr    = req.getParameter(P_COMPANY_ID);
        final String companyName     = req.getParameter(P_COMPANY_NAME);
        final String yearMonth       = Optional.ofNullable(req.getParameter(P_YEAR_MONTH))
                                               .filter(s -> !s.isBlank())
                                               .orElse(LocalDate.now(Z_TOKYO).format(YM_FMT));
        final String workDateStr     = req.getParameter(P_WORK_DATE);
        final String startStr        = req.getParameter(P_START_TIME);
        final String endStr          = req.getParameter(P_END_TIME);
        final String assignmentIdStr = req.getParameter(P_ASSIGNMENT_ID);
        final String workContent     = req.getParameter(P_WORK_CONTENT);

        // 2) バリデーション
        if (!validation.isUuid(idStr))            validation.addErrorMsg("対象IDが不正です。");
        if (!validation.isUuid(companyIdStr))     validation.addErrorMsg("顧客IDが不正です。");
        if (!validation.isUuid(assignmentIdStr))  validation.addErrorMsg("アサインIDが不正です。");
        if (workDateStr == null || startStr == null || endStr == null ||
            workContent == null || workContent.isBlank()) {
            validation.addErrorMsg("必須項目が未入力です。");
        }
        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return REDIRECT_ERROR;
        }

        final UUID taskId       = UUID.fromString(idStr);
        final UUID customerId   = UUID.fromString(companyIdStr);
        final UUID assignmentId = UUID.fromString(assignmentIdStr);

        // 3) サーバ側で分計算 & 月範囲チェック
        LocalDate workDate;
        LocalTime st, et;
        try {
            workDate = LocalDate.parse(workDateStr, YMD_FMT);
            st = LocalTime.parse(startStr, HM_FMT);
            et = LocalTime.parse(endStr, HM_FMT);
        } catch (Exception ex) {
            validation.addErrorMsg("日付/時刻の形式が不正です。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return REDIRECT_ERROR;
        }
        if (!et.isAfter(st)) {
            validation.addErrorMsg("終了時刻は開始時刻より後にしてください。");
        }
        if (!workDate.format(YM_FMT).equals(yearMonth)) {
            validation.addErrorMsg("日付は表示中の年月（" + yearMonth + "）の範囲で選択してください。");
        }
        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return REDIRECT_ERROR;
        }
        int workMinute = (int) Duration.between(st, et).toMinutes();

        // 4) UPDATE
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

            tm.commit();

            // 5) 一覧へ戻る（会社名はURLエンコード）
            return req.getContextPath() + req.getServletPath() + "/task/list"
                 + "?" + A_COMPANY_ID   + "=" + customerId
                 + "&" + A_COMPANY_NAME + "=" + URLEncoder.encode(
                        Optional.ofNullable(companyName).orElse(""), StandardCharsets.UTF_8)
                 + "&" + A_YEAR_MONTH   + "=" + yearMonth;

        } catch (RuntimeException e) {
            validation.addErrorMsg("更新に失敗しました。入力内容をご確認ください。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            e.printStackTrace();
            return REDIRECT_ERROR;
        }
    }
    
    /**
     * タスク論理削除（/secretary/task/delete_done）
     * - 必須: id（UUID）
     * - 任意: companyId, companyName, yearMonth（指定あれば一覧に戻すために使用）
     * 成功時は PRG 的に一覧へリダイレクト（パラメータが無ければ秘書ホーム等にフォールバック）
     */
    public String taskDeleteDone() {
        final String idStr         = req.getParameter(P_ID);
        final String companyIdStr  = req.getParameter(P_COMPANY_ID);
        final String companyName   = req.getParameter(P_COMPANY_NAME);
        final String yearMonth     = req.getParameter(P_YEAR_MONTH);


        // --- 入力チェック ---
        if (!validation.isUuid(idStr)) {
            validation.addErrorMsg("削除対象IDが不正です。");
        }
        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return REDIRECT_ERROR;
        }

        final UUID taskId = UUID.fromString(idStr);

        // --- 削除実行 ---
        try (TransactionManager tm = new TransactionManager()) {
            TaskDAO dao = new TaskDAO(tm.getConnection());
            int updated = dao.delete(taskId); // deleted_at を現在時刻に更新

            if (updated == 0) {
                // 既に削除済み or 存在しないケース
                validation.addErrorMsg("対象の業務が見つからないか、すでに削除されています。");
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                return REDIRECT_ERROR;
            }

            tm.commit();
            
            String nextPath = req.getContextPath() + req.getServletPath() + "/task/list" + "?" 
                    + A_COMPANY_ID + "=" + companyIdStr 
                    + "&" + A_COMPANY_NAME + "=" + URLEncoder.encode(companyName, StandardCharsets.UTF_8)
                    + "&" + A_YEAR_MONTH + "=" + yearMonth;

            // --- 戻り先を決定（companyId等があれば一覧へ） ---
            return nextPath;

        } catch (RuntimeException e) {
            validation.addErrorMsg("削除に失敗しました。やり直してください。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            e.printStackTrace();
            return REDIRECT_ERROR;
        }
    }
}
