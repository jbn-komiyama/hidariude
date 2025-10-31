package service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dao.AssignmentDAO;
import dao.CustomerContactDAO;
import dao.CustomerDAO;
import dao.CustomerMonthlyInvoiceDAO;
import dao.InvoiceDAO;
import dao.SecretaryDAO;
import dao.SystemAdminDAO;
import dao.TaskDAO;
import dao.TransactionManager;
import domain.Customer;
import domain.CustomerContact;
import domain.LoginUser;
import domain.Secretary;
import domain.SystemAdmin;
import domain.Task;
import dto.AssignmentDTO;
import dto.CustomerContactDTO;
import dto.CustomerDTO;
import dto.InvoiceDTO;
import dto.SecretaryDTO;
import dto.SystemAdminDTO;
import dto.TaskDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import util.ConvertUtil;
import util.PasswordUtil;

/**
 * 共通サービス（admin / secretary / customer の横断機能）。
 * ログイン、ホームダッシュボード、admin のマイページ（表示・編集）を提供します。
 * 画面パス・パラメータ名・属性名は定数に集約してハードコード散在を防止しています。
 * DB アクセスは {@link TransactionManager} を用いた try-with-resources で安全に扱います。
 * 
 * 構成:
 * - 定数（パラメータ名／パス／フォーマッタ／権限）
 * - フィールド・コンストラクタ
 * - コントローラ呼び出しメソッド（admin → secretary → customer の順）
 * - ヘルパー
 * 
 * 命名・互換性:
 * - JSP からの {@code req.getParameter("...")} のキー名は既存どおり（変更なし）
 * - JSP への {@code setAttribute("...")} のキー名も既存どおり（変更なし）
 */
public class CommonService extends BaseService {

    /** =========================================================
     * ① 定数／共通化（パラメータ名／パス／フォーマッタ／権限）
     * ========================================================= */

    /** 権限（従来の数値を定数化） */
    private static final int AUTH_ADMIN     = 1;
    private static final int AUTH_SECRETARY = 2;
    private static final int AUTH_CUSTOMER  = 3;

    /** ---- Request Parameters ---- */
    private static final String P_LOGIN_ID = "loginId";
    private static final String P_PASSWORD = "password";

    /** ---- Paths (forward / redirect) ---- */
    private static final String PATH_SECRETARY_LOGIN = "/secretary";
    private static final String PATH_SECRETARY_LOGIN_FORM = "common/secretary/login";
    private static final String PATH_SECRETARY_HOME  = "/secretary/home";
    private static final String PATH_ADMIN_LOGIN     = "/admin";
    private static final String PATH_ADMIN_LOGIN_FORM = "common/admin/login";
    private static final String PATH_ADMIN_HOME      = "/admin/home";
    private static final String PATH_CUSTOMER_LOGIN  = "/customer";
    private static final String PATH_CUSTOMER_LOGIN_FORM = "common/customer/login";
    private static final String PATH_CUSTOMER_HOME   = "/customer/home";
    private static final String PATH_ADMIN_MYPAGE    = "mypage/admin/home";
    private static final String PATH_ADMIN_ID_EDIT   = "mypage/admin/edit";

    /** ---- Attributes ---- */
    private static final String ATTR_LOGIN_USER = "loginUser";
    private static final String ATTR_ADMIN      = "admin";
    private static final String ATTR_FORM       = "form";

    /** ---- Date / Time ---- */
    private static final ZoneId Z_TOKYO = ZoneId.of("Asia/Tokyo");
    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    /** =========================================================
     * ② フィールド／コンストラクタ
     * ========================================================= */

    /** DTO ↔ Domain の変換器 */
    private final ConvertUtil conv = new ConvertUtil();

    /**
     * コンストラクタ。
     * @param req   現在の {@link HttpServletRequest}
     * @param useDB DB 使用フラグ（BaseService 踏襲）
     */
    public CommonService(HttpServletRequest req, boolean useDB) {
        super(req, useDB);
    }

    /** =========================================================
     * ③ コントローラ呼び出しメソッド（アクター別）
     * ========================================================= */

    /** =========================
     * 「【secretary】 機能：ログイン」
     * ========================= */
    /**
     * 秘書ログイン。
     * - loginId / password: request param（必須）
     * - 成功時：セッションに {@code loginUser.secretary} と {@code authority=2} を設定し、{@code /secretary/home} へリダイレクト
     * - 失敗時：{@code errorMsg} をセットし {@code /secretary} へリダイレクト
     */
    public String secretaryLogin() {
        final String loginId  = req.getParameter(P_LOGIN_ID);
        final String password = req.getParameter(P_PASSWORD);

        // 必須チェック
        validation.isNull("ログインID", loginId);
        validation.isNull("パスワード", password);
        if (validation.hasErrorMsg()) {
            req.setAttribute("errorMsg", validation.getErrorMsg());
            return PATH_SECRETARY_LOGIN_FORM;
        }

        try (TransactionManager tm = new TransactionManager()) {
            SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
            SecretaryDTO dto = dao.selectByMail(loginId);
            if (dto != null && PasswordUtil.verifyPassword(password, dto.getPassword())) {
                /** 最終ログイン時刻を更新 */
                dao.updateLastLoginAt(dto.getId());
                tm.commit();
                
                /** セッションへ格納（JSP は sessionScope.loginUser.secretary を参照） */
                LoginUser loginUser = new LoginUser();
                Secretary sec = new Secretary();
                sec.setId(dto.getId());
                sec.setMail(dto.getMail());
                sec.setName(dto.getName());
                loginUser.setSecretary(sec);
                loginUser.setAuthority(AUTH_SECRETARY);
                putLoginUserToSession(loginUser);
                return req.getContextPath() + PATH_SECRETARY_HOME;
            }
            validation.addErrorMsg("メールアドレス、パスワードの組み合わせが間違っています");
            req.setAttribute("errorMsg", validation.getErrorMsg());
            return PATH_SECRETARY_LOGIN_FORM;
        } catch (RuntimeException e) {
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /** =========================
     * 「【admin】 機能：ログイン」
     * ========================= */
    /**
     * 管理者ログイン。
     * - loginId / password: request param（必須）
     * - 成功時：セッションに {@code loginUser.systemAdmin} と {@code authority=1} を設定し、{@code /admin/home} へ
     * - 失敗時：{@code errorMsg} をセットし {@code /admin} へ
     */
    public String adminLogin() {
        final String loginId  = req.getParameter(P_LOGIN_ID);
        final String password = req.getParameter(P_PASSWORD);

        validation.isNull("ログインID", loginId);
        validation.isNull("パスワード", password);
        if (validation.hasErrorMsg()) {
            req.setAttribute("errorMsg", validation.getErrorMsg());
            return PATH_ADMIN_LOGIN_FORM;
        }

        try (TransactionManager tm = new TransactionManager()) {
            SystemAdminDAO dao = new SystemAdminDAO(tm.getConnection());
            SystemAdminDTO dto = dao.selectByMail(loginId);
            if (dto != null && PasswordUtil.verifyPassword(password, dto.getPassword())) {
                /** 最終ログイン時刻を更新 */
                dao.updateLastLoginAt(dto.getId());
                tm.commit();
                
                LoginUser loginUser = new LoginUser();
                SystemAdmin admin = new SystemAdmin();
                admin.setId(dto.getId());
                admin.setMail(dto.getMail());
                admin.setName(dto.getName());
                admin.setNameRuby(dto.getNameRuby());
                admin.setCreatedAt(dto.getCreatedAt());
                admin.setUpdatedAt(dto.getUpdatedAt());
                admin.setDeletedAt(dto.getDeletedAt());
                admin.setLastLoginAt(dto.getLastLoginAt());
                loginUser.setSystemAdmin(admin);
                loginUser.setAuthority(AUTH_ADMIN);
                putLoginUserToSession(loginUser);
                return req.getContextPath() + PATH_ADMIN_HOME;
            }
            validation.addErrorMsg("メールアドレス、パスワードの組み合わせが間違っています");
            req.setAttribute("errorMsg", validation.getErrorMsg());
            return PATH_ADMIN_LOGIN_FORM;
        } catch (RuntimeException e) {
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /** =========================
     * 「【admin】 機能：マスタ管理」
     * ========================= */
    /**
     * マスタ管理画面。
     * - 表示用データ：業務ランク一覧、秘書ランク一覧
     * - setAttribute:
     *   'taskRanks', 'secretaryRanks'
     */
    public String adminMasterList() {
        try (TransactionManager tm = new TransactionManager()) {
            dao.TaskRankDAO taskRankDAO = new dao.TaskRankDAO(tm.getConnection());
            dao.SecretaryDAO secretaryDAO = new dao.SecretaryDAO(tm.getConnection());

            /** 業務ランク一覧を取得 */
            List<dto.TaskRankDTO> taskRankDTOs = taskRankDAO.selectAll();
            List<domain.TaskRank> taskRanks = new ArrayList<>();
            for (dto.TaskRankDTO dto : taskRankDTOs) {
                domain.TaskRank taskRank = new domain.TaskRank();
                taskRank.setId(dto.getId());
                taskRank.setRankName(dto.getRankName());
                taskRank.setBasePayCustomer(dto.getBasePayCustomer());
                taskRank.setBasePaySecretary(dto.getBasePaySecretary());
                taskRank.setCreatedAt(dto.getCreatedAt());
                taskRank.setUpdatedAt(dto.getUpdatedAt());
                taskRank.setDeletedAt(dto.getDeletedAt());
                taskRanks.add(taskRank);
            }

            /** 秘書ランク一覧を取得 */
            List<dto.SecretaryRankDTO> secretaryRankDTOs = secretaryDAO.selectRankAll();
            List<domain.SecretaryRank> secretaryRanks = new ArrayList<>();
            for (dto.SecretaryRankDTO dto : secretaryRankDTOs) {
                domain.SecretaryRank secretaryRank = new domain.SecretaryRank();
                secretaryRank.setId(dto.getId());
                secretaryRank.setRankName(dto.getRankName());
                secretaryRank.setDescription(dto.getDescription());
                secretaryRank.setIncreaseBasePayCustomer(dto.getIncreaseBasePayCustomer());
                secretaryRank.setIncreaseBasePaySecretary(dto.getIncreaseBasePaySecretary());
                secretaryRank.setCreatedAt(dto.getCreatedAt());
                secretaryRank.setUpdatedAt(dto.getUpdatedAt());
                secretaryRank.setDeletedAt(dto.getDeletedAt());
                secretaryRanks.add(secretaryRank);
            }

            /** JSP へ */
            req.setAttribute("taskRanks", taskRanks);
            req.setAttribute("secretaryRanks", secretaryRanks);
            return "master/admin/home";
        } catch (RuntimeException e) {
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /** =========================
     * 「【admin】 機能：ホーム」
     * ========================= */
    /**
     * 管理者ホーム。
     * - 表示用データ：今月/先月のタスク集計（未承認・承認済・差戻し・総数・承認済金額）
     * - setAttribute:
     *   'task', 'taskPrev', 'yearMonth', 'prevYearMonth', 'adminName'
     */
    public String adminHome() {
        /** 表示名（セッション任意） */
        HttpSession session = req.getSession(false);
        String adminName = "";
        if (session != null) {
            LoginUser lu = (LoginUser) session.getAttribute("loginUser");
            if (lu != null && lu.getSystemAdmin() != null) adminName = lu.getSystemAdmin().getName();
        }

        /** 今月 / 先月の YYYY-MM */
        String yearMonth     = LocalDate.now(Z_TOKYO).format(YM_FMT);
        String prevYearMonth = LocalDate.now(Z_TOKYO).minusMonths(1).format(YM_FMT);

        try (TransactionManager tm = new TransactionManager()) {
            TaskDAO tdao = new TaskDAO(tm.getConnection());
            SecretaryDAO sdao = new SecretaryDAO(tm.getConnection());
            CustomerDAO  cdao = new CustomerDAO(tm.getConnection());

            /** 今月 */
            TaskDTO tdThis = tdao.selectCountsForAdminMonth(yearMonth);
            Task tThis = new Task();
            tThis.setUnapproved(tdThis.getUnapproved());
            tThis.setApproved(tdThis.getApproved());
            tThis.setRemanded(tdThis.getRemanded());
            tThis.setTotal(tdThis.getTotal());
            tThis.setSumAmountApproved(tdThis.getTotalAmountApproved());

            /** 先月 */
            TaskDTO tdPrev = tdao.selectCountsForAdminMonth(prevYearMonth);
            Task tPrev = new Task();
            tPrev.setUnapproved(tdPrev.getUnapproved());
            tPrev.setApproved(tdPrev.getApproved());
            tPrev.setRemanded(tdPrev.getRemanded());
            tPrev.setTotal(tdPrev.getTotal());
            tPrev.setSumAmountApproved(tdPrev.getTotalAmountApproved());
            
            List<TaskDTO> tdto = tdao.showAlert(true);
            List<Task> alerts = new ArrayList<>();
            for(TaskDTO dto : tdto) {
            	alerts.add(conv.toDomain(dto));
            }

            
            /** 直近10件をそれぞれ取得 */
            List<Map<String,Object>> recentSecretaries = sdao.selectRecent10WithProfileFlag();
            List<Map<String,Object>> recentCustomers   = cdao.selectRecent10();

            /** JSP へ渡す（新規属性名。既存の属性名は変更しません） */
            req.setAttribute("recentSecretaries", recentSecretaries);
            req.setAttribute("recentCustomers",   recentCustomers);

            /** JSP へ */
            req.setAttribute("task", tThis);
            req.setAttribute("taskPrev", tPrev);
            req.setAttribute("yearMonth", yearMonth);
            req.setAttribute("prevYearMonth", prevYearMonth);
            req.setAttribute("adminName", adminName);
            req.setAttribute("alerts",            alerts);
            return "common/admin/home";
        } catch (RuntimeException e) {
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /** =========================
     * 「【admin】 機能：マイページ表示」
     * ========================= */
    /**
     * 管理者マイページ表示。
     * - セッション必須：{@code loginUser.systemAdmin}
     * - setAttribute: 'admin'
     * - 未ログイン時：/admin へリダイレクト
     */
    public String adminMyPage() {
        HttpSession session = req.getSession(false);
        if (session == null) return req.getContextPath() + PATH_ADMIN_LOGIN;
        LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
        if (lu == null || lu.getSystemAdmin() == null || lu.getSystemAdmin().getId() == null) {
            return req.getContextPath() + PATH_ADMIN_LOGIN;
        }
        req.setAttribute(ATTR_ADMIN, lu.getSystemAdmin());
        return PATH_ADMIN_MYPAGE;
    }

    /** =========================
     * 「【admin】 機能：マイページ編集（画面）」
     * ========================= */
    /**
     * 管理者ID編集フォーム表示。
     * - セッション必須：{@code loginUser.systemAdmin}
     * - 既存値（mail/name/nameRuby）を 'form' に詰めて JSP 初期表示に利用
     */
    public String adminIdEditForm() {
        HttpSession session = req.getSession(false);
        if (session == null) return req.getContextPath() + PATH_ADMIN_LOGIN;
        LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
        if (lu == null || lu.getSystemAdmin() == null || lu.getSystemAdmin().getId() == null) {
            return req.getContextPath() + PATH_ADMIN_LOGIN;
        }
        Map<String, String> form = new LinkedHashMap<>();
        form.put("mail",     lu.getSystemAdmin().getMail());
        form.put("name",     lu.getSystemAdmin().getName());
        form.put("nameRuby", lu.getSystemAdmin().getNameRuby());
        req.setAttribute(ATTR_FORM, form);
        return PATH_ADMIN_ID_EDIT;
    }

    /** =========================
     * 「【admin】 機能：ID情報編集（送信）」
     * ========================= */
    /**
     * 管理者ID編集の送信処理。
     * - 入力：mail（必須）, name（必須）, nameRuby, password（空なら据え置き）
     * - メール重複チェック（自ID除外）
     * - 成功時：セッションの admin 情報も更新し、/admin/mypage へリダイレクト（successMsg 設定）
     */
    public String adminIdEditSubmit() {
        HttpSession session = req.getSession(false);
        if (session == null) return req.getContextPath() + PATH_ADMIN_LOGIN;
        LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
        if (lu == null || lu.getSystemAdmin() == null || lu.getSystemAdmin().getId() == null) {
            return req.getContextPath() + PATH_ADMIN_LOGIN;
        }
        UUID adminId = lu.getSystemAdmin().getId();

        /** 入力 */
        String mail     = req.getParameter("mail");
        String password = req.getParameter("password"); /** 空なら据え置き */
        String name     = req.getParameter("name");
        String nameRuby = req.getParameter("nameRuby");

        validation.isNull("メールアドレス", mail);
        validation.isNull("氏名", name);

        Map<String, String> form = new LinkedHashMap<>();
        form.put("mail", mail);
        form.put("name", name);
        form.put("nameRuby", nameRuby);
        req.setAttribute(ATTR_FORM, form);

        if (validation.hasErrorMsg()) {
            req.setAttribute("errorMsg", validation.getErrorMsg());
            return PATH_ADMIN_ID_EDIT;
        }

        try (TransactionManager tm = new TransactionManager()) {
            SystemAdminDAO dao = new SystemAdminDAO(tm.getConnection());

            /** 自分以外でメール重複が無いか */
            if (dao.mailExistsExceptId(mail, adminId)) {
                req.setAttribute("errorMsg", "入力されたメールアドレスは既に使用されています。");
                return PATH_ADMIN_ID_EDIT;
            }

            SystemAdminDTO current = dao.selectById(adminId);
            if (current.getId() == null) {
                req.setAttribute("errorMsg", "対象の管理者が見つかりません。");
                return PATH_ADMIN_ID_EDIT;
            }

            /** 更新 DTO（パスワードは空なら据え置き） */
            SystemAdminDTO upd = new SystemAdminDTO();
            upd.setId(adminId);
            upd.setMail(mail);
            if (password != null && !password.isBlank()) {
                /** パスワード変更時は強度チェック */
                if (!validation.isStrongPassword(password)) {
                    req.setAttribute("errorMsg", validation.getErrorMsg());
                    req.setAttribute(ATTR_FORM, form);
                    return PATH_ADMIN_ID_EDIT;
                }
                upd.setPassword(PasswordUtil.hashPassword(password));
            } else {
                upd.setPassword(current.getPassword());
            }
            upd.setName(name);
            upd.setNameRuby(nameRuby);

            int cnt = dao.update(upd);
            if (cnt != 1) {
                req.setAttribute("errorMsg", "更新に失敗しました。");
                return PATH_ADMIN_ID_EDIT;
            }

            tm.commit();

            /** セッションの Domain も更新 */
            lu.getSystemAdmin().setMail(mail);
            lu.getSystemAdmin().setName(name);
            lu.getSystemAdmin().setNameRuby(nameRuby);
            session.setAttribute(ATTR_LOGIN_USER, lu);

            req.setAttribute("successMsg", "アカウント情報を更新しました。");
            req.setAttribute(ATTR_ADMIN, lu.getSystemAdmin());
            return req.getContextPath() + "/admin/mypage";
        } catch (RuntimeException e) {
            e.printStackTrace();
            req.setAttribute("errorMsg", "予期せぬエラーが発生しました。");
            return PATH_ADMIN_ID_EDIT;
        }
    }

    /** =========================
     * 「【secretary】 機能：ホーム」
     * ========================= */
    /**
     * 秘書ホーム。
     * - セッション必須：{@code loginUser.secretary.id}
     * - 表示：当月/先月のタスク集計、当月アサインのフラット行一覧（単価内訳）
     * - setAttribute:
     *   'task', 'taskPrev', 'yearMonth', 'prevYearMonth', 'secretaryName', 'assignRows'
     */
    public String secretaryHome() {
        HttpSession session = req.getSession(false);
        if (session == null) return req.getContextPath() + PATH_SECRETARY_LOGIN;
        LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
        if (lu == null || lu.getSecretary() == null || lu.getSecretary().getId() == null) {
            return req.getContextPath() + PATH_SECRETARY_LOGIN;
        }
        UUID secretaryId = lu.getSecretary().getId();
        String secretaryName = lu.getSecretary().getName();

        String yearMonth     = LocalDate.now(Z_TOKYO).format(YM_FMT);
        String prevYearMonth = LocalDate.now(Z_TOKYO).minusMonths(1).format(YM_FMT);

        try (TransactionManager tm = new TransactionManager()) {
            /** 当月アサインのフラット行（会社名／ランク／単価内訳） */
            List<AssignmentDTO> adtosFlat =
                new AssignmentDAO(tm.getConnection())
                    .selectBySecretaryAndMonthToAssignment(secretaryId, yearMonth);
            List<Map<String, Object>> assignRows = new ArrayList<>();
            for (AssignmentDTO a : adtosFlat) {
                BigDecimal base    = nz(a.getBasePaySecretary());
                BigDecimal incRank = nz(a.getIncreaseBasePaySecretary());
                BigDecimal incCont = nz(a.getCustomerBasedIncentiveForSecretary());
                BigDecimal total   = base.add(incRank).add(incCont);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("company", a.getCustomerCompanyName());
                row.put("rank",    a.getTaskRankName());
                row.put("base",    base);
                row.put("incRank", incRank);
                row.put("incCont", incCont);
                row.put("total",   total);
                assignRows.add(row);
            }

            /** タスク集計（今月／先月） */
            TaskDAO tdao = new TaskDAO(tm.getConnection());
            TaskDTO tdto     = tdao.selectCountsForSecretaryMonth(secretaryId, yearMonth);
            TaskDTO tdtoPrev = tdao.selectCountsForSecretaryMonth(secretaryId, prevYearMonth);

            Task task = new Task();
            task.setUnapproved(tdto.getUnapproved());
            task.setApproved(tdto.getApproved());
            task.setRemanded(tdto.getRemanded());
            task.setTotal(tdto.getTotal());
            task.setSumAmountApproved(tdto.getTotalAmountApproved());

            Task taskPrev = new Task();
            taskPrev.setUnapproved(tdtoPrev.getUnapproved());
            taskPrev.setApproved(tdtoPrev.getApproved());
            taskPrev.setRemanded(tdtoPrev.getRemanded());
            taskPrev.setTotal(tdtoPrev.getTotal());
            taskPrev.setSumAmountApproved(tdtoPrev.getTotalAmountApproved());

            /** JSP へ */
            req.setAttribute("task", task);
            req.setAttribute("taskPrev", taskPrev);
            req.setAttribute("yearMonth", yearMonth);
            req.setAttribute("prevYearMonth", prevYearMonth);
            req.setAttribute("secretaryName", secretaryName);
            req.setAttribute("assignRows", assignRows);
            return "common/secretary/home";
        } catch (RuntimeException e) {
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /** =========================
     * 「【customer】 機能：ログイン」
     * ========================= */
    /**
     * 顧客ログイン。
     * - loginId / password: request param（必須）
     * - 成功時：セッションに {@code loginUser.customer} と {@code loginUser.customerContact}、{@code authority=3} を設定し、{@code /customer/home} へ
     * - 失敗時：{@code errorMsg} をセットし {@code /customer} へ
     */
    public String customerLogin() {
        final String loginId  = req.getParameter(P_LOGIN_ID);
        final String password = req.getParameter(P_PASSWORD);

        validation.isNull("ログインID", loginId);
        validation.isNull("パスワード", password);
        if (validation.hasErrorMsg()) {
            req.setAttribute("errorMsg", validation.getErrorMsg());
            return PATH_CUSTOMER_LOGIN_FORM;
        }

        try (TransactionManager tm = new TransactionManager()) {
            CustomerContactDAO ccDao = new CustomerContactDAO(tm.getConnection());
            CustomerDAO cDao         = new CustomerDAO(tm.getConnection());

            CustomerContactDTO ccDto = ccDao.selectByMail(loginId);
            if (ccDto != null && PasswordUtil.verifyPassword(password, ccDto.getPassword())) {
                /** 最終ログイン時刻を更新 */
                ccDao.updateLastLoginAt(ccDto.getId());
                tm.commit();
                
                /** 担当者 Domain */
                CustomerContact cc = conv.toDomain(ccDto);

                /** 顧客IDを確実に取得 */
                UUID customerId =
                    (ccDto.getCustomerDTO() != null && ccDto.getCustomerDTO().getId() != null)
                        ? ccDto.getCustomerDTO().getId()
                        : cc.getCustomerId();
                if (customerId == null) {
                    validation.addErrorMsg("担当者に紐づく会社情報が見つかりません。");
                    req.setAttribute("errorMsg", validation.getErrorMsg());
                    return PATH_CUSTOMER_LOGIN_FORM;
                }

                /** 会社 Domain */
                CustomerDTO cDto = cDao.selectByUUId(customerId);
                Customer customer = conv.toDomain(cDto);

                /** セッションへ格納 */
                LoginUser loginUser = new LoginUser();
                loginUser.setCustomer(customer);
                loginUser.setCustomerContact(cc);
                loginUser.setAuthority(AUTH_CUSTOMER);
                putLoginUserToSession(loginUser);

                return req.getContextPath() + PATH_CUSTOMER_HOME;
            }
            validation.addErrorMsg("メールアドレス、パスワードの組み合わせが間違っています");
            req.setAttribute("errorMsg", validation.getErrorMsg());
            return PATH_CUSTOMER_LOGIN_FORM;
        } catch (RuntimeException e) {
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /** =========================
     * 「【admin】 機能：ログアウト」
     * ========================= */
    /**
     * 管理者ログアウト。
     * - セッションを無効化し、ログイン画面（/admin）へリダイレクト
     */
    public String adminLogout() {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return req.getContextPath() + PATH_ADMIN_LOGIN;
    }

    /** =========================
     * 「【secretary】 機能：ログアウト」
     * ========================= */
    /**
     * 秘書ログアウト。
     * - セッションを無効化し、ログイン画面（/secretary）へリダイレクト
     */
    public String secretaryLogout() {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return req.getContextPath() + PATH_SECRETARY_LOGIN;
    }

    /** =========================
     * 「【customer】 機能：ログアウト」
     * ========================= */
    /**
     * 顧客ログアウト。
     * - セッションを無効化し、ログイン画面（/customer）へリダイレクト
     */
    public String customerLogout() {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return req.getContextPath() + PATH_CUSTOMER_LOGIN;
    }

    /** =========================
     * 「【customer】 機能：ホーム」
     * ========================= */
    /**
     * 顧客ホーム。
     * - セッション必須：{@code loginUser.customer.id}
     * - 表示：今月～過去3か月（計4か月）の未承認数・金額合計
     *   - 未承認数：work_date 基準でタスクの approvedAt==null をカウント
     *   - 金額合計：今月・先月は InvoiceDAO の fee 合計、2～3か月前は確定テーブル {@link CustomerMonthlyInvoiceDAO}
     * - setAttribute:
     *   'm0','m1','m2','m3','ymNow','ymPrev1','ymPrev2','ymPrev3',
     *   'statNow','statPrev1','statPrev2','statPrev3'
     */
    public String customerHome() {
        /** ログイン確認 */
        HttpSession session = req.getSession(false);
        if (session == null) return req.getContextPath() + PATH_CUSTOMER_LOGIN;
        LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
        if (lu == null || lu.getCustomer() == null || lu.getCustomer().getId() == null)
            return req.getContextPath() + PATH_CUSTOMER_LOGIN;

        UUID customerId = lu.getCustomer().getId();

        /** 今月と過去3か月 */
        YearMonth ymNow   = YearMonth.now(Z_TOKYO);
        YearMonth ymPrev1 = ymNow.minusMonths(1);
        YearMonth ymPrev2 = ymNow.minusMonths(2);
        YearMonth ymPrev3 = ymNow.minusMonths(3);

        /** JSP 用（月数値/文字列） */
        req.setAttribute("m0", ymNow.getMonthValue());
        req.setAttribute("m1", ymPrev1.getMonthValue());
        req.setAttribute("m2", ymPrev2.getMonthValue());
        req.setAttribute("m3", ymPrev3.getMonthValue());
        req.setAttribute("ymNow",   ymNow.format(YM_FMT));
        req.setAttribute("ymPrev1", ymPrev1.format(YM_FMT));
        req.setAttribute("ymPrev2", ymPrev2.format(YM_FMT));
        req.setAttribute("ymPrev3", ymPrev3.format(YM_FMT));

        try (TransactionManager tm = new TransactionManager()) {
            CustomerMonthlyInvoiceDAO cmiDao = new CustomerMonthlyInvoiceDAO(tm.getConnection());
            InvoiceDAO invDao = new InvoiceDAO(tm.getConnection());

            /** 未承認件数（work_date 基準） */
            MonthStat statNow   = loadCustomerMonthStatByWorkDate(customerId, ymNow,   invDao);
            MonthStat statPrev1 = loadCustomerMonthStatByWorkDate(customerId, ymPrev1, invDao);
            MonthStat statPrev2 = loadCustomerMonthStatByWorkDate(customerId, ymPrev2, invDao);
            MonthStat statPrev3 = loadCustomerMonthStatByWorkDate(customerId, ymPrev3, invDao);

            /** 金額合計：今月/先月は InvoiceDAO の fee を合算（work_date 基準） */
            statNow.setTotal(sumFee(invDao, customerId, ymNow));
            statPrev1.setTotal(sumFee(invDao, customerId, ymPrev1));

            /** 2～3か月前は確定テーブル */
            BigDecimal amt2 = cmiDao.selectTotalAmountByCustomerAndMonth(customerId, ymPrev2.format(YM_FMT));
            BigDecimal amt3 = cmiDao.selectTotalAmountByCustomerAndMonth(customerId, ymPrev3.format(YM_FMT));
            statPrev2.setTotal(amt2 != null ? amt2 : BigDecimal.ZERO);
            statPrev3.setTotal(amt3 != null ? amt3 : BigDecimal.ZERO);

            /** JSP へ */
            req.setAttribute("statNow",   statNow);
            req.setAttribute("statPrev1", statPrev1);
            req.setAttribute("statPrev2", statPrev2);
            req.setAttribute("statPrev3", statPrev3);
            return "common/customer/home";
        } catch (RuntimeException e) {
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /** =========================================================
     * ④ ヘルパー
     * ========================================================= */

    /** セッションへ LoginUser を格納（セッション新規作成あり） */
    private void putLoginUserToSession(LoginUser loginUser) {
        HttpSession session = req.getSession(true);
        session.setAttribute(ATTR_LOGIN_USER, loginUser);
    }

    /** null を 0 にするユーティリティ */
    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    /** -------------------------
     * 顧客ホーム用のミニ DTO / 集計関数
     * ------------------------- */

    /** 顧客ホームの月次統計（未承認件数・金額合計） */
    public static class MonthStat {
        private String ym;
        private Integer unapproved;
        private BigDecimal total;
        public String getYm(){return ym;} public void setYm(String ym){this.ym=ym;}
        public Integer getUnapproved(){return unapproved;} public void setUnapproved(Integer u){this.unapproved=u;}
        public BigDecimal getTotal(){return total;} public void setTotal(BigDecimal t){this.total=t;}
    }

    /**
     * work_date 基準でその月の未承認件数を数える（approvedAt==null）
     * @param customerId 顧客ID
     * @param ym 対象年月
     * @param invDao InvoiceDAO
     */
    private MonthStat loadCustomerMonthStatByWorkDate(UUID customerId, YearMonth ym, InvoiceDAO invDao) {
        final String ymStr = ym.format(YM_FMT);
        MonthStat s = new MonthStat();
        s.setYm(ymStr);
        /** 未承認数は Task 明細の方が正確なケースもあるが、本件は invDao から取得できる TaskDTO 群で代替 */
        List<TaskDTO> tasks = invDao.selectTasksByMonthAndCustomer(customerId, ymStr);
        int unapproved = 0;
        if (tasks != null) {
            for (TaskDTO t : tasks) {
                if (t.getApprovedAt() == null) unapproved++;
            }
        }
        s.setUnapproved(unapproved);
        s.setTotal(null); /** 金額は呼び出し側で設定 */
        return s;
    }

    /**
     * work_date 基準でその月の fee を合算。
     * @param invDao InvoiceDAO
     * @param customerId 顧客ID
     * @param ym 年月
     * @return 合計金額（null なし）
     */
    private BigDecimal sumFee(InvoiceDAO invDao, UUID customerId, YearMonth ym) {
        String ymStr = ym.format(YM_FMT);
        List<InvoiceDTO> rows =
            invDao.selectTotalMinutesBySecretaryAndCustomer(customerId, ymStr);
        BigDecimal sum = BigDecimal.ZERO;
        if (rows != null) {
            for (InvoiceDTO d : rows) {
                if (d.getFee() != null) sum = sum.add(d.getFee());
            }
        }
        return sum;
    }

}
