package service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import java.util.function.Predicate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import dao.AssignmentDAO;
import dao.CustomerDAO;
import dao.ProfileDAO;
import dao.SecretaryDAO;
import dao.TaskRankDAO;
import dao.TransactionManager;
import domain.Assignment;
import domain.AssignmentGroup;
import domain.Customer;
import domain.LoginUser;
import domain.Secretary;
import domain.TaskRank;
import dto.AssignmentDTO;
import dto.CustomerDTO;
import dto.SecretaryDTO;
import dto.TaskRankDTO;

/**
 * アサイン（assignment）登録/一覧のアプリケーションサービス。
 * <ul>
 *   <li>一覧（当月）表示</li>
 *   <li>通常登録：入力 → 確認 → 完了</li>
 *   <li>PM登録：入力 → 確認 → 完了</li>
 *   <li>引継ぎプレビュー/適用、継続単価編集、削除</li>
 *   <li>（顧客）委託先一覧</li>
 * </ul>
 * <p>入力値検証は {@code Validation} を利用し、重複チェックは DAO で実施します。</p>
 */
public class AssignmentService extends BaseService {

    // =========================================================
    // ① 定数・共通化（パラメータ名／パス／フォーマッタ／コンバータ）
    // =========================================================
    /** 年月フォーマッタ（yyyy-MM） */
    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    // ----- Request params -----
    private static final String P_CUSTOMER_ID   = "customerId";
    private static final String P_SECRETARY_ID  = "secretaryId";
    private static final String P_TASK_RANK_ID  = "taskRankId";
    private static final String P_TARGET_YM     = "targetYM";
    private static final String P_BASE_CUST     = "basePayCustomer";
    private static final String P_BASE_SEC      = "basePaySecretary";
    private static final String P_INC_CUST      = "increaseBasePayCustomer";
    private static final String P_INC_SEC       = "increaseBasePaySecretary";
    private static final String P_INCENT_CUST   = "customerBasedIncentiveForCustomer";
    private static final String P_INCENT_SEC    = "customerBasedIncentiveForSecretary";
    private static final String P_STATUS        = "status";
    private static final String P_COMPANY_ID    = "companyId";
    private static final String P_COMPANY_NAME  = "companyName";
    private static final String P_ID            = "id";
    // private static final String P_ASSIGNMENT_ID = "assignmentId"; // ← 未使用のため削除

    // ----- View names -----
    private static final String VIEW_HOME                 = "assignment/admin/home";
    private static final String VIEW_REGISTER             = "assignment/admin/register";
    private static final String VIEW_REGISTER_CHECK       = "assignment/admin/register_check";
    private static final String VIEW_REGISTER_DONE        = "assignment/admin/register_done";
    private static final String VIEW_PM_REGISTER          = "assignment/admin/pm_register";
    private static final String VIEW_PM_REGISTER_CHECK    = "assignment/admin/pm_register_check";
    private static final String VIEW_PM_REGISTER_DONE     = "assignment/admin/pm_register_done";
    private static final String VIEW_CARRY_OVER_PREVIEW   = "assignment/admin/carry_over_preview";
    private static final String VIEW_EDIT_INCENTIVE       = "assignment/admin/edit_incentive";
    private static final String VIEW_OUTSOURCE_LIST       = "assignment/customer/list";

    // ----- Attribute keys -----
    private static final String A_CUSTOMERS    = "customers";
    private static final String A_TARGET_YM    = "targetYM";
    private static final String A_CUSTOMER     = "customer";
    private static final String A_SECRETARIES  = "secretaries";
    private static final String A_TASK_RANKS   = "taskRanks";
    private static final String A_TASK_RANK    = "taskRank";
    private static final String A_ERROR        = "errorMsg";
    private static final String A_MESSAGE      = "message";
    private static final String A_STATUS       = "status";

    // 確認画面・エラー戻し用（form_*）
    private static final String A_FORM_CUSTOMER_ID  = "form_customerId";
    private static final String A_FORM_SECRETARY_ID = "form_secretaryId";
    private static final String A_FORM_TASK_RANK_ID = "form_taskRankId";
    private static final String A_FORM_YM           = "form_targetYM";
    private static final String A_FORM_BASE_CUST    = "form_basePayCustomer";
    private static final String A_FORM_BASE_SEC     = "form_basePaySecretary";
    private static final String A_FORM_INC_CUST     = "form_increaseBasePayCustomer";
    private static final String A_FORM_INC_SEC      = "form_increaseBasePaySecretary";
    private static final String A_FORM_INCENT_CUST  = "form_customerBasedIncentiveForCustomer";
    private static final String A_FORM_INCENT_SEC   = "form_customerBasedIncentiveForSecretary";
    private static final String A_FORM_STATUS       = "form_status";

    // PM確認画面の hidden 受け渡し（h_*）
    private static final String A_H_CUSTOMER_ID     = "h_customerId";
    private static final String A_H_SECRETARY_ID    = "h_secretaryId";
    private static final String A_H_TASK_RANK_ID    = "h_taskRankId";
    private static final String A_H_BASE_CUST       = "h_basePayCustomer";
    private static final String A_H_BASE_SEC        = "h_basePaySecretary";

    // 変換（DTO→Domain）
    private final Converter conv = new Converter();

    // =========================================================
    // ② フィールド・コンストラクタ
    // =========================================================
    public AssignmentService(HttpServletRequest req, boolean useDB) {
        super(req, useDB);
    }

    // =========================================================
    // ③ メソッド（アクター別）
    //  ③-1. 【admin】一覧/登録/確認/完了/各種ユーティリティ
    //  ③-2. 【customer】委託先一覧
    //  （secretary 用は現状なし）
    // =========================================================

    // =========================
    // 【admin】機能：一覧（当月）
    // =========================
    /**
     * 「アサイン一覧（当月）」の表示。
     * <ul>
     *   <li>request param: {@code targetYM}（無指定なら当月、最大で翌月まで）</li>
     *   <li>フィルタ：顧客名（部分一致）/ 秘書 / 継続≧N（DAO 側で処理）</li>
     *   <li>ソート：フィルタ無しかつ指定「months_desc」のときのみ継続月数降順SQLを使用</li>
     *   <li>顧客起点で LEFT JOIN し、当月アサイン無しの顧客行も表示</li>
     * </ul>
     * @return JSP: {@code assignment/admin/home}
     */
    public String assignmentList() {
        String targetYM = req.getParameter(A_TARGET_YM);

        ZoneId Z_JST = ZoneId.of("Asia/Tokyo");
        String ymNow  = LocalDate.now(Z_JST).format(YM_FMT);
        String ymNext = LocalDate.now(Z_JST).plusMonths(1).format(YM_FMT);
        if (targetYM == null || targetYM.isBlank()) targetYM = ymNow;
        try {
            if (YearMonth.parse(targetYM).isAfter(YearMonth.parse(ymNext))) {
                targetYM = ymNext;
            }
        } catch (Exception ignore) { targetYM = ymNow; }

        String minMonthsStr   = req.getParameter("minMonths");
        Integer minMonths     = null;
        try { if (minMonthsStr != null && !minMonthsStr.isBlank()) minMonths = Integer.valueOf(minMonthsStr); }
        catch (Exception ignore) {}

        String secretaryIdStr = req.getParameter("secretaryId");
        UUID filterSecId = (secretaryIdStr != null && validation.isUuid(secretaryIdStr))
                ? UUID.fromString(secretaryIdStr) : null;

        String qCustomer      = req.getParameter("qCustomer");
        String sort           = req.getParameter("sort");        // "months_desc" など
        boolean sortByMonthsDesc = "months_desc".equalsIgnoreCase(sort);

        boolean hasFilters = (filterSecId != null)
                || (minMonths != null)
                || (qCustomer != null && !qCustomer.isBlank());

        try (TransactionManager tm = new TransactionManager()) {
            // セレクトボックス：秘書
            loadSecretariesToRequest(tm, false);

            AssignmentDAO aDao = new AssignmentDAO(tm.getConnection());

            // データ取得
            List<CustomerDTO> cRows;
            if (sortByMonthsDesc && !hasFilters) {
                cRows = aDao.selectAllByMonthOrderByConsecutiveDesc(targetYM);
            } else {
                cRows = aDao.selectAllByMonthFiltered(targetYM, qCustomer, filterSecId, minMonths);
            }

            // DTO→Domain 整形（秘書単位でグルーピング）
            Map<UUID, Integer> contMonths = new HashMap<>();
            List<Customer> customers = new ArrayList<>();

            for (CustomerDTO cdto : cRows) {
                Customer c = new Customer();
                c.setId(cdto.getId());
                c.setCompanyName(cdto.getCompanyName());

                if (cdto.getAssignmentDTOs() != null && !cdto.getAssignmentDTOs().isEmpty()) {
                    Map<UUID, AssignmentGroup> gmap = new LinkedHashMap<>();
                    for (AssignmentDTO adto : cdto.getAssignmentDTOs()) {
                        if (adto.getAssignmentId() != null) {
                            Integer cont = adto.getConsecutiveMonths();
                            contMonths.put(adto.getAssignmentId(), cont == null ? 0 : cont);
                        }
                        Assignment a = conv.toDomain(adto);
                        UUID sid = (a.getSecretary() != null ? a.getSecretary().getId() : null);

                        AssignmentGroup g = gmap.get(sid);
                        if (g == null) {
                            g = new AssignmentGroup();
                            g.setSecretary(a.getSecretary());
                            gmap.put(sid, g);
                        }
                        g.getAssignments().add(a);
                    }
                    c.setAssignmentGroups(new ArrayList<>(gmap.values()));
                }
                customers.add(c);
            }

            // 画面用属性
            String prevYM = LocalDate.parse(targetYM + "-01").minusMonths(1).format(YM_FMT);
            req.setAttribute("canCarryOver", ymNow.equals(targetYM) || ymNext.equals(targetYM));
            req.setAttribute("prevYM", prevYM);
            req.setAttribute("maxYM", ymNext);

            req.setAttribute("contMonths", contMonths);
            req.setAttribute(A_CUSTOMERS, customers);
            req.setAttribute(A_TARGET_YM, targetYM);

            // フォーム反映
            req.setAttribute("f_minMonths",  minMonthsStr);
            req.setAttribute("f_secretaryId", secretaryIdStr);
            req.setAttribute("f_qCustomer",  qCustomer);
            req.setAttribute("f_sort",       sort);

            return VIEW_HOME;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // 【admin】機能：登録フォーム表示（通常）
    // =========================
    /**
     * 「通常アサイン登録」フォームの表示。
     * <ul>
     *   <li>必須：{@code id}（顧客ID）, {@code companyName}</li>
     *   <li>フィルタ/ソートはリクエスト（GET）で受け取り、<br>
     *       {@code ProfileDAO#selectSecretaryCandidatesForRegister()} に渡してサーバー側で実現</li>
     *   <li>対象月 {@code targetYM} は無ければ当月</li>
     *   <li>候補は profiles 登録済みの秘書のみ、〇/△のみ抽出、pref_score で「〇→△」優先</li>
     * </ul>
     * @return JSP: {@code assignment/admin/register}
     */
    public String assignmentRegister() {
        String ym = req.getParameter(P_TARGET_YM);
        if (ym == null || ym.isBlank()) ym = LocalDate.now().format(YM_FMT);
        req.setAttribute(A_TARGET_YM, ym);

        final String idStr       = req.getParameter(P_ID);
        final String companyName = req.getParameter(P_COMPANY_NAME);
        if (validation.isNull("会社名", companyName) | validation.isNull("会社ID", idStr)) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return req.getContextPath() + "/admin/assignment";
        }

        // チェックボックス受け取り（JSPのnameと一致）
        Predicate<String> truthy = v -> v != null &&
                (v.equalsIgnoreCase("1") || v.equalsIgnoreCase("on")
                        || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes"));

        boolean wdAm    = truthy.test(req.getParameter("wdAm"));
        boolean wdDay   = truthy.test(req.getParameter("wdDay"));
        boolean wdNight = truthy.test(req.getParameter("wdNight"));
        boolean saAm    = truthy.test(req.getParameter("saAm"));
        boolean saDay   = truthy.test(req.getParameter("saDay"));
        boolean saNight = truthy.test(req.getParameter("saNight"));
        boolean suAm    = truthy.test(req.getParameter("suAm"));
        boolean suDay   = truthy.test(req.getParameter("suDay"));
        boolean suNight = truthy.test(req.getParameter("suNight"));

        String sortKey = req.getParameter("sortKey");   // wdHours / saHours / suHours / totalHours / lastMonth / capacity
        String sortDir = req.getParameter("sortDir");   // asc / desc
        boolean desc = !"asc".equalsIgnoreCase(sortDir);

        try (TransactionManager tm = new TransactionManager()) {
            // 顧客（固定表示）
            Customer customer = new Customer();
            customer.setId(UUID.fromString(idStr));
            customer.setCompanyName(companyName);
            req.setAttribute(A_CUSTOMER, customer);

            // プルダウン
            loadSecretariesToRequest(tm, false);
            loadTaskRanksToRequest(tm);
            
            AssignmentDAO adao = new AssignmentDAO(tm.getConnection());
            List<AssignmentDTO> adtos = adao.selectThisMonthByCustomerWithContRank(customer.getId(), ym);
            List<Assignment> futureAssignments = new ArrayList<>();
            for(AssignmentDTO adto : adtos) {
            	futureAssignments.add(conv.toDomain(adto));
            }
            
            req.setAttribute("futureAssignments", futureAssignments);
            
            // 候補（サーバー側：SQLでフィルタ/ソート）
            ProfileDAO pdao = new ProfileDAO(tm.getConnection());
            List<Map<String, Object>> candidates =
                    pdao.selectSecretaryCandidatesForRegister(
                            ym,
                            wdAm, wdDay, wdNight,
                            saAm, saDay, saNight,
                            suAm, suDay, suNight,
                            sortKey, desc
                    );

            req.setAttribute("secretaryCandidates", candidates);

            // 画面の状態戻し
            req.setAttribute("wdAm", wdAm);
            req.setAttribute("wdDay", wdDay);
            req.setAttribute("wdNight", wdNight);
            req.setAttribute("saAm", saAm);
            req.setAttribute("saDay", saDay);
            req.setAttribute("saNight", saNight);
            req.setAttribute("suAm", suAm);
            req.setAttribute("suDay", suDay);
            req.setAttribute("suNight", suNight);
            req.setAttribute("sortKey", sortKey);
            req.setAttribute("sortDir", (desc ? "desc" : "asc"));

            return VIEW_REGISTER;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // 【admin】機能：登録フォーム表示（PM）
    // =========================
    /**
     * 「PMアサイン登録」フォームの表示。
     * <ul>
     *   <li>必須：{@code companyId}（顧客ID）, {@code companyName}</li>
     *   <li>PM秘書のみプルダウン表示（is_pm_secretary = true）</li>
     *   <li>タスクランク P を固定表示</li>
     *   <li>対象月 {@code targetYM} は無ければ当月</li>
     * </ul>
     * @return JSP: {@code assignment/admin/pm_register}
     */
    public String assignmentPMRegister() {
        String ym = req.getParameter(P_TARGET_YM);
        if (ym == null || ym.isBlank()) ym = LocalDate.now().format(YM_FMT);
        req.setAttribute(A_TARGET_YM, ym);

        final String companyIdStr = req.getParameter(P_COMPANY_ID);
        final String companyName  = req.getParameter(P_COMPANY_NAME);
        if (validation.isNull("会社名", companyName) | validation.isNull("会社ID", companyIdStr)) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        try (TransactionManager tm = new TransactionManager()) {
            Customer customer = new Customer();
            customer.setId(UUID.fromString(companyIdStr));
            customer.setCompanyName(companyName);
            req.setAttribute(A_CUSTOMER, customer);

            loadSecretariesToRequest(tm, true);  // PMのみ
            loadPMTaskRankToRequest(tm);         // ランクP固定

            return VIEW_PM_REGISTER;
        } catch (RuntimeException e) {
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // 【admin】機能：登録確認（通常）
    // =========================
    /**
     * 「通常アサイン登録」確認画面。
     * <ul>
     *   <li>form_* を request に積み、ID 形式の軽微な検証を実施</li>
     *   <li>名称（顧客・秘書・ランク）は DB から取得して表示用に設定</li>
     * </ul>
     * @return JSP: {@code assignment/admin/register_check}（エラー時は入力画面へ戻す）
     */
    public String assignmentRegisterCheck() {
        // form_* をいったん積む
        pushFormBackToRequestForCheck(false);

        // POST 値
        final String customerIdStr  = req.getParameter(P_ID);           // ※ 画面の name=id を受け取る仕様
        final String secretaryIdStr = req.getParameter(P_SECRETARY_ID);
        final String taskRankIdStr  = req.getParameter(P_TASK_RANK_ID);
        final String targetYM       = req.getParameter(P_TARGET_YM);

        // 軽い検証
        if (!validation.isUuid(customerIdStr)) validation.addErrorMsg("顧客の指定が不正です");
        if (!validation.isUuid(secretaryIdStr)) validation.addErrorMsg("秘書の指定が不正です");
        if (!validation.isUuid(taskRankIdStr))  validation.addErrorMsg("業務ランクの指定が不正です");

        if (validation.hasErrorMsg()) {
            try (TransactionManager tm = new TransactionManager()) {
                loadSecretariesToRequest(tm, false);
                loadTaskRanksToRequest(tm);
                if (validation.isUuid(customerIdStr)) {
                    CustomerDAO cdao = new CustomerDAO(tm.getConnection());
                    CustomerDTO cdto = cdao.selectByUUId(UUID.fromString(customerIdStr));
                    if (cdto != null) req.setAttribute(A_CUSTOMER, conv.toDomain(cdto));
                }
            }
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return VIEW_REGISTER;
        }

        // 表示名称取得
        try (TransactionManager tm = new TransactionManager()) {
            CustomerDAO  cdao  = new CustomerDAO(tm.getConnection());
            SecretaryDAO sdao  = new SecretaryDAO(tm.getConnection());
            TaskRankDAO  trdao = new TaskRankDAO(tm.getConnection());

            CustomerDTO  cdto  = cdao.selectByUUId(UUID.fromString(customerIdStr));
            SecretaryDTO sdto  = sdao.selectByUUId(UUID.fromString(secretaryIdStr));

            TaskRankDTO trdto = null;
            for (TaskRankDTO d : trdao.selectAll()) {
                if (d.getId().equals(UUID.fromString(taskRankIdStr))) { trdto = d; break; }
            }

            if (cdto != null)  req.setAttribute(A_CUSTOMER,  conv.toDomain(cdto));
            if (sdto != null)  req.setAttribute("secretary", conv.toDomain(sdto));
            if (trdto != null) req.setAttribute(A_TASK_RANK, conv.toDomain(trdto));
        } catch (RuntimeException e) {
            return req.getContextPath() + req.getServletPath() + "/error";
        }
        return VIEW_REGISTER_CHECK;
    }

    // =========================
    // 【admin】機能：登録確認（PM）
    // =========================
    /**
     * 「PMアサイン登録」確認画面。
     * <ul>
     *   <li>単価（顧客/秘書）は hidden をそのまま使用</li>
     *   <li>増額/継続インセンティブは PM 仕様で 0 固定（非表示）</li>
     * </ul>
     * @return JSP: {@code assignment/admin/pm_register_check}
     */
    public String assignmentPMRegisterCheck() {
        final String customerIdStr       = req.getParameter(P_CUSTOMER_ID);
        final String secretaryIdStr      = req.getParameter(P_SECRETARY_ID);
        final String taskRankIdStr       = req.getParameter(P_TASK_RANK_ID);
        final String targetYM            = req.getParameter(P_TARGET_YM);
        final String basePayCustomerStr  = req.getParameter(P_BASE_CUST);
        final String basePaySecretaryStr = req.getParameter(P_BASE_SEC);
        final String status              = req.getParameter(P_STATUS);

        validation.isNull("顧客", customerIdStr);
        validation.isNull("PM秘書", secretaryIdStr);
        validation.isNull("タスクランク", taskRankIdStr);
        validation.isNull("対象月", targetYM);

        if (!validation.isUuid(customerIdStr))  validation.addErrorMsg("顧客の指定が不正です");
        if (!validation.isUuid(secretaryIdStr)) validation.addErrorMsg("秘書の指定が不正です");
        if (!validation.isUuid(taskRankIdStr))  validation.addErrorMsg("業務ランクの指定が不正です");
        if (!validation.isYearMonth(targetYM))  validation.addErrorMsg("対象月は yyyy-MM 形式で入力してください");
        validation.mustBeMoneyOrZero("単価（顧客）", basePayCustomerStr);
        validation.mustBeMoneyOrZero("単価（秘書）", basePaySecretaryStr);

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            try (TransactionManager tm = new TransactionManager()) {
                CustomerDAO cdao = new CustomerDAO(tm.getConnection());
                CustomerDTO cdto = cdao.selectByUUId(UUID.fromString(customerIdStr));
                req.setAttribute(A_CUSTOMER, conv.toDomain(cdto));
                loadSecretariesToRequest(tm, true);
                loadPMTaskRankToRequest(tm);
            } catch (RuntimeException ignore) {}
            req.setAttribute(A_TARGET_YM, targetYM);
            req.setAttribute(A_STATUS, status);
            req.setAttribute(A_FORM_SECRETARY_ID, secretaryIdStr);
            return VIEW_PM_REGISTER;
        }

        try (TransactionManager tm = new TransactionManager()) {
            CustomerDAO cdao = new CustomerDAO(tm.getConnection());
            SecretaryDAO sdao = new SecretaryDAO(tm.getConnection());
            TaskRankDAO trdao = new TaskRankDAO(tm.getConnection());

            CustomerDTO cdto = cdao.selectByUUId(UUID.fromString(customerIdStr));
            SecretaryDTO sdto = sdao.selectByUUId(UUID.fromString(secretaryIdStr));
            TaskRankDTO trdto = trdao.selectPM();

            req.setAttribute(A_CUSTOMER,  conv.toDomain(cdto));
            req.setAttribute(A_TASK_RANK, conv.toDomain(trdto));
            req.setAttribute("secretary", conv.toDomain(sdto));

            req.setAttribute(A_TARGET_YM, targetYM);
            req.setAttribute(A_STATUS,    status);
            req.setAttribute(A_H_CUSTOMER_ID, customerIdStr);
            req.setAttribute(A_H_SECRETARY_ID, secretaryIdStr);
            req.setAttribute(A_H_TASK_RANK_ID, taskRankIdStr);
            req.setAttribute(A_H_BASE_CUST,    basePayCustomerStr);
            req.setAttribute(A_H_BASE_SEC,     basePaySecretaryStr);

            return VIEW_PM_REGISTER_CHECK;
        } catch (RuntimeException e) {
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // 【admin】機能：登録完了（通常）
    // =========================
    /**
     * 「通常アサイン登録」の確定（INSERT）。
     * <ul>
     *   <li>重複チェック：同月・同顧客・同秘書・同ランク</li>
     *   <li>エラー時はフォーム値を復元し、リスト再設定</li>
     * </ul>
     * @return JSP: {@code assignment/admin/register_done}（エラー時は入力画面）
     */
    public String assignmentRegisterDone() {
        String customerIdStr = req.getParameter(P_CUSTOMER_ID);
        String secretaryIdStr = req.getParameter(P_SECRETARY_ID);
        String taskRankIdStr  = req.getParameter(P_TASK_RANK_ID);
        String ym             = req.getParameter(P_TARGET_YM);
        String baseCustStr    = req.getParameter(P_BASE_CUST);
        String baseSecStr     = req.getParameter(P_BASE_SEC);
        String incCustStr     = req.getParameter(P_INC_CUST);
        String incSecStr      = req.getParameter(P_INC_SEC);
        String incentCustStr  = req.getParameter(P_INCENT_CUST);
        String incentSecStr   = req.getParameter(P_INCENT_SEC);
        String status         = req.getParameter(P_STATUS);

        validation.isNull("顧客", customerIdStr);
        validation.isNull("秘書", secretaryIdStr);
        validation.isNull("業務ランク", taskRankIdStr);
        validation.isNull("対象月", ym);
        if (!validation.isUuid(customerIdStr)) validation.addErrorMsg("顧客の指定が不正です");
        if (!validation.isUuid(secretaryIdStr)) validation.addErrorMsg("秘書の指定が不正です");
        if (!validation.isUuid(taskRankIdStr))  validation.addErrorMsg("業務ランクの指定が不正です");
        if (!validation.isYearMonth(ym))        validation.addErrorMsg("対象月は yyyy-MM 形式で入力してください");

        validation.mustBeMoneyOrZero("単価（顧客）", baseCustStr);
        validation.mustBeMoneyOrZero("単価（秘書）", baseSecStr);
        validation.mustBeMoneyOrZero("増額（顧客）", incCustStr);
        validation.mustBeMoneyOrZero("増額（秘書）", incSecStr);
        if (!validation.isBlank(incentCustStr)) validation.mustBeMoneyOrZero("継続単価（顧客）", incentCustStr);
        if (!validation.isBlank(incentSecStr))  validation.mustBeMoneyOrZero("継続単価（秘書）", incentSecStr);

        if (validation.hasErrorMsg()) {
            populateFormBackForRegister(customerIdStr, secretaryIdStr, taskRankIdStr, ym,
                    baseCustStr, baseSecStr, incCustStr, incSecStr, incentCustStr, incentSecStr, status);
            try (TransactionManager tm = new TransactionManager()) {
                loadSecretariesToRequest(tm, false);
                loadTaskRanksToRequest(tm);
            }
            return VIEW_REGISTER;
        }

        AssignmentDTO dto = buildAssignmentDto(
                customerIdStr, secretaryIdStr, taskRankIdStr, ym,
                baseCustStr, baseSecStr, incCustStr, incSecStr, incentCustStr, incentSecStr, status);

        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());

            if (dao.existsDuplicate(dto)) {
                validation.addErrorMsg("同月・同顧客・同秘書・同ランクのアサインは既に登録済みです。");
                populateFormBackForRegister(customerIdStr, secretaryIdStr, taskRankIdStr, ym,
                        baseCustStr, baseSecStr, incCustStr, incSecStr, incentCustStr, incentSecStr, status);
                loadSecretariesToRequest(tm, false);
                loadTaskRanksToRequest(tm);
                return VIEW_REGISTER;
            }

            dao.insert(dto);
            tm.commit();
            return VIEW_REGISTER_DONE;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // 【admin】機能：登録完了（PM）
    // =========================
    /**
     * 「PMアサイン登録」の確定（INSERT）。
     * <ul>
     *   <li>増額/継続インセンティブは 0 固定</li>
     *   <li>重複チェック：同月・同顧客・同秘書・同ランク</li>
     * </ul>
     * @return JSP: {@code assignment/admin/pm_register_done}（重複時は入力へリダイレクト）
     */
    public String assignmentPMRegisterDone() {
        final String customerIdStr       = req.getParameter(P_CUSTOMER_ID);
        final String secretaryIdStr      = req.getParameter(P_SECRETARY_ID);
        final String taskRankIdStr       = req.getParameter(P_TASK_RANK_ID);
        final String targetYM            = req.getParameter(P_TARGET_YM);
        final String basePayCustomerStr  = req.getParameter(P_BASE_CUST);
        final String basePaySecretaryStr = req.getParameter(P_BASE_SEC);
        final String status              = req.getParameter(P_STATUS);

        validation.isNull("顧客", customerIdStr);
        validation.isNull("PM秘書", secretaryIdStr);
        validation.isNull("タスクランク", taskRankIdStr);
        validation.isNull("対象月", targetYM);
        if (!validation.isUuid(customerIdStr))  validation.addErrorMsg("顧客の指定が不正です");
        if (!validation.isUuid(secretaryIdStr)) validation.addErrorMsg("秘書の指定が不正です");
        if (!validation.isUuid(taskRankIdStr))  validation.addErrorMsg("業務ランクの指定が不正です");
        if (!validation.isYearMonth(targetYM))  validation.addErrorMsg("対象月は yyyy-MM 形式で入力してください");
        validation.mustBeMoneyOrZero("単価（顧客）", basePayCustomerStr);
        validation.mustBeMoneyOrZero("単価（秘書）", basePaySecretaryStr);

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDTO dto = buildAssignmentDto(
                    customerIdStr, secretaryIdStr, taskRankIdStr, targetYM,
                    basePayCustomerStr, basePaySecretaryStr,
                    "0", "0", "0", "0", status // PM 固定
            );

            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());
            if (dao.existsDuplicate(dto)) {
                validation.addErrorMsg("同月・同顧客・同秘書・同ランクのアサインは既に登録済みです。");
                req.setAttribute(A_ERROR, validation.getErrorMsg());

                try {
                    CustomerDAO cdao = new CustomerDAO(tm.getConnection());
                    CustomerDTO cdto = cdao.selectByUUId(UUID.fromString(customerIdStr));
                    req.setAttribute(A_CUSTOMER, conv.toDomain(cdto));
                    loadSecretariesToRequest(tm, true);
                    loadPMTaskRankToRequest(tm);
                    req.setAttribute(A_TARGET_YM, targetYM);
                    req.setAttribute(A_STATUS,    status);
                } catch (RuntimeException ignore) {}
                return VIEW_PM_REGISTER;
            }

            dao.insert(dto);
            tm.commit();

            req.setAttribute(A_MESSAGE, "PMアサインの登録が完了しました。");
            return VIEW_PM_REGISTER_DONE;
        } catch (RuntimeException e) {
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // 【admin】機能：引継ぎプレビュー
    // =========================
    /**
     * 「引継ぎプレビュー」（先月→対象月）。
     * <ul>
     *   <li>request param: {@code toYM}（対象月）, {@code fromYM}（先月）</li>
     *   <li>対象月に未登録のもののみ候補表示</li>
     *   <li>候補行に対して、{@code fromYM} を末尾とする「連続月数」を計算して併せて表示</li>
     * </ul>
     * @return JSP: {@code assignment/admin/carry_over_preview}
     */
    public String assignmentCarryOverPreview() {
        final String toYM   = req.getParameter("toYM");
        final String fromYM = req.getParameter("fromYM");
        if (!validation.isYearMonth(toYM) || !validation.isYearMonth(fromYM)) {
            return req.getContextPath() + "/admin/assignment?targetYM=" + (toYM != null ? toYM : "");
        }

        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());
            List<AssignmentDTO> candidates = dao.selectCarryOverCandidates(fromYM, toYM);

            Map<UUID,Integer> contMonths = new HashMap<>();
            for (AssignmentDTO a : candidates) {
                int n = countConsecutiveMonths(tm, a.getAssignmentSecretaryId(), a.getAssignmentCustomerId(), fromYM);
                contMonths.put(a.getAssignmentId(), n);
            }

            req.setAttribute("fromYM", fromYM);
            req.setAttribute("toYM",   toYM);
            req.setAttribute("candidates", candidates);
            req.setAttribute("contMonths", contMonths);
            return VIEW_CARRY_OVER_PREVIEW;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // 【admin】機能：引継ぎ適用
    // =========================
    /**
     * 「引継ぎ適用」（チェック済み候補を対象月に一括INSERT）。
     * <ul>
     *   <li>request param: {@code toYM}（対象月）, {@code assignmentId[]}（候補ID群）</li>
     *   <li>重複（同顧客×同秘書×同ランク×対象月）はスキップ</li>
     * </ul>
     * @return 一覧へリダイレクト（対象月）
     */
    public String assignmentCarryOverApply() {
        final String toYM = req.getParameter("toYM");
        if (!validation.isYearMonth(toYM)) {
            return req.getContextPath() + "/admin/assignment";
        }
        String[] ids = req.getParameterValues("assignmentId");
        if (ids == null || ids.length == 0) {
            return req.getContextPath()
                    + "/admin/assignment/carry_over_preview?toYM=" + toYM
                    + "&fromYM=" + LocalDate.parse(toYM + "-01").minusMonths(1).format(YM_FMT);
        }

        List<UUID> idList = new ArrayList<>();
        for (String s : ids) {
            try { idList.add(UUID.fromString(s)); } catch (Exception ignore) {}
        }

        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());
            List<AssignmentDTO> rows = dao.selectByIds(idList);

            int inserted = 0;
            for (AssignmentDTO src : rows) {
                AssignmentDTO dto = new AssignmentDTO();
                dto.setAssignmentCustomerId(src.getAssignmentCustomerId());
                dto.setAssignmentSecretaryId(src.getAssignmentSecretaryId());
                dto.setTaskRankId(src.getTaskRankId());
                dto.setTargetYearMonth(toYM);
                dto.setBasePayCustomer(src.getBasePayCustomer());
                dto.setBasePaySecretary(src.getBasePaySecretary());
                dto.setIncreaseBasePayCustomer(src.getIncreaseBasePayCustomer());
                dto.setIncreaseBasePaySecretary(src.getIncreaseBasePaySecretary());
                dto.setCustomerBasedIncentiveForCustomer(src.getCustomerBasedIncentiveForCustomer());
                dto.setCustomerBasedIncentiveForSecretary(src.getCustomerBasedIncentiveForSecretary());
                dto.setAssignmentStatus(src.getAssignmentStatus());

                if (!dao.existsDuplicate(dto)) {
                    dao.insert(dto);
                    inserted++;
                }
            }
            tm.commit();
            return req.getContextPath() + "/admin/assignment?targetYM=" + toYM;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // 【admin】機能：継続単価 編集フォーム
    // =========================
    /**
     * 「継続単価」編集フォーム表示。
     * <ul>
     *   <li>request param: {@code id}（アサインID）, {@code targetYM}（戻り先用）</li>
     *   <li>顧客/秘書/年月/ランクは表示のみ（変更不可）</li>
     * </ul>
     * @return JSP: {@code assignment/admin/edit_incentive}
     */
    public String assignmentEditIncentiveForm() {
        final String idStr = req.getParameter("id");
        final String ym    = req.getParameter(A_TARGET_YM);
        if (!validation.isUuid(idStr)) {
            req.setAttribute(A_ERROR, List.of("不正なアサインIDです。"));
            return req.getContextPath() + "/admin/assignment?targetYM=" + (ym != null ? ym : "");
        }

        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());
            AssignmentDTO d = dao.selectOneWithNames(UUID.fromString(idStr));
            if (d == null) {
                req.setAttribute(A_ERROR, List.of("対象のアサインが見つかりません。"));
                return req.getContextPath() + "/admin/assignment?targetYM=" + (ym != null ? ym : "");
            }
            req.setAttribute("row", d);
            req.setAttribute(A_TARGET_YM, ym != null ? ym : d.getTargetYearMonth());
            return VIEW_EDIT_INCENTIVE;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // 【admin】機能：継続単価 更新
    // =========================
    /**
     * 「継続単価」更新実行（顧客×秘書×対象月の全レコード一括更新）。
     * <ul>
     *   <li>request param: {@code id}, {@code targetYM}, {@code customerBasedIncentiveForCustomer}, {@code customerBasedIncentiveForSecretary}</li>
     *   <li>翌月に同ペアが登録済みなら、その月にも同値を反映（UPDATE件数0ならスルー）</li>
     * </ul>
     * @return 一覧へリダイレクト（編集対象の月）
     */
    public String assignmentEditIncentiveUpdate() {
        final String idStr   = req.getParameter("id");
        final String ymBack  = req.getParameter(A_TARGET_YM);
        final String incCust = req.getParameter(P_INCENT_CUST);
        final String incSec  = req.getParameter(P_INCENT_SEC);

        if (!validation.isUuid(idStr)) {
            req.setAttribute(A_ERROR, List.of("不正なアサインIDです。"));
            return req.getContextPath() + "/admin/assignment?targetYM=" + (ymBack != null ? ymBack : "");
        }
        validation.mustBeMoneyOrZero("継続単価（顧客）", incCust);
        validation.mustBeMoneyOrZero("継続単価（秘書）", incSec);
        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return assignmentEditIncentiveForm();
        }

        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());

            AssignmentDTO cur = dao.selectOne(UUID.fromString(idStr));
            if (cur == null) {
                req.setAttribute(A_ERROR, List.of("対象のアサインが見つかりません。"));
                return req.getContextPath() + "/admin/assignment?targetYM=" + (ymBack != null ? ymBack : "");
            }

            BigDecimal custVal = parseMoneyOrZero(incCust);
            BigDecimal secVal  = parseMoneyOrZero(incSec);

            dao.updateIncentivesByPairAndMonth(
                    cur.getAssignmentCustomerId(),
                    cur.getAssignmentSecretaryId(),
                    cur.getTargetYearMonth(),
                    custVal, secVal
            );

            YearMonth thisYm = YearMonth.parse(cur.getTargetYearMonth());
            String nextYm    = thisYm.plusMonths(1).toString();

            dao.updateIncentivesByPairAndMonth(
                    cur.getAssignmentCustomerId(),
                    cur.getAssignmentSecretaryId(),
                    nextYm,
                    custVal, secVal
            );

            tm.commit();
            return req.getContextPath() + "/admin/assignment?targetYM=" + cur.getTargetYearMonth();
        } catch (RuntimeException e) {
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // 【admin】機能：削除
    // =========================
    /**
     * アサインの削除（論理削除）。紐づくタスクがある場合は削除不可。
     * <ul>
     *   <li>request param: {@code id}, {@code targetYM}</li>
     *   <li>紐づくタスク存在時はエラーメッセージをセットして一覧へ戻す</li>
     * </ul>
     * @return 一覧へリダイレクト
     */
    public String assignmentDelete() {
        final String idStr  = req.getParameter("id");
        final String ymBack = req.getParameter(A_TARGET_YM);
        if (!validation.isUuid(idStr)) {
            req.setAttribute(A_ERROR, List.of("不正なアサインIDです。"));
            return req.getContextPath() + "/admin/assignment?targetYM=" + (ymBack != null ? ymBack : "");
        }

        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());
            UUID id = UUID.fromString(idStr);

            AssignmentDTO cur = dao.selectOne(id);
            String ym = (cur != null ? cur.getTargetYearMonth() : ymBack);

            if (dao.hasTasks(id)) {
                req.setAttribute(A_ERROR, List.of("このアサインにはタスクが登録されています。削除できません。"));
                return req.getContextPath() + "/admin/assignment?targetYM=" + (ym != null ? ym : "");
            }

            dao.delete(id);
            tm.commit();
            return req.getContextPath() + "/admin/assignment?targetYM=" + (ym != null ? ym : "");
        } catch (RuntimeException e) {
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // 【customer】機能：委託先一覧
    // =========================
    /**
     * （顧客向け）当月の「委託先一覧」表示。
     * <ul>
     *   <li>ログイン中の顧客IDをセッションから取得</li>
     *   <li>対象月は JST の当月固定</li>
     *   <li>当月に該当顧客へ紐づく秘書を重複無しで一覧化</li>
     * </ul>
     * @return JSP: {@code assignment/customer/list}
     */
    public String outsourceList() {
        UUID customerId = currentCustomerId();
        if (customerId == null) {
            req.setAttribute(A_ERROR, List.of("ログインが必要です。"));
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        String yearMonth = LocalDate.now(ZoneId.of("Asia/Tokyo")).format(YM_FMT);

        List<Map<String, Object>> secondaries = Collections.emptyList();
        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());
            secondaries = dao.selectSecretariesByCustomerAndMonth(customerId, yearMonth);
        } catch (RuntimeException ignore) {}

        req.setAttribute("yearMonth", yearMonth);
        req.setAttribute("secondaries", secondaries);
        req.setAttribute("note", null);
        return VIEW_OUTSOURCE_LIST;
    }

    // =========================================================
    // ④ ヘルパー
    // =========================================================

    /**
     * 金額文字列を {@link BigDecimal} に変換（空/blank→0、カンマ除去）。
     * @param s 金額文字列
     * @return 変換結果（null 相当は 0）
     */
    private BigDecimal parseMoneyOrZero(String s) {
        if (s == null) return BigDecimal.ZERO;
        String t = s.trim();
        if (t.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(t.replace(",", ""));
    }

    /**
     * 確認画面用：入力値を form_* 名でリクエストに積む。
     * @param pmMode PM 確認（増額/継続=0 固定）なら true
     */
    private void pushFormBackToRequestForCheck(boolean pmMode) {
        req.setAttribute(A_FORM_CUSTOMER_ID, req.getParameter(P_ID));
        req.setAttribute(A_FORM_SECRETARY_ID, req.getParameter(P_SECRETARY_ID));
        req.setAttribute(A_FORM_TASK_RANK_ID, req.getParameter(P_TASK_RANK_ID));
        req.setAttribute(A_FORM_YM, req.getParameter(P_TARGET_YM));
        req.setAttribute(A_FORM_BASE_CUST,  req.getParameter(P_BASE_CUST));
        req.setAttribute(A_FORM_BASE_SEC,   req.getParameter(P_BASE_SEC));
        if (!pmMode) {
            req.setAttribute(A_FORM_INC_CUST,  req.getParameter(P_INC_CUST));
            req.setAttribute(A_FORM_INC_SEC,   req.getParameter(P_INC_SEC));
            req.setAttribute(A_FORM_INCENT_CUST, req.getParameter(P_INCENT_CUST));
            req.setAttribute(A_FORM_INCENT_SEC,  req.getParameter(P_INCENT_SEC));
        } else {
            req.setAttribute(A_FORM_INC_CUST,  "0");
            req.setAttribute(A_FORM_INC_SEC,   "0");
            req.setAttribute(A_FORM_INCENT_CUST, "0");
            req.setAttribute(A_FORM_INCENT_SEC,  "0");
        }
        req.setAttribute(A_FORM_STATUS, req.getParameter(P_STATUS));
    }
    
    /**
     * AssignmentDTO の組立ヘルパー（内部利用）。
     * <ul>
     *   <li>文字列ID（顧客/秘書/ランク）を UUID へ変換</li>
     *   <li>金額文字列は空やカンマ含みも 0 として BigDecimal 化</li>
     *   <li>対象月・ステータス等をそのまま DTO に詰める</li>
     * </ul>
     * 呼び出し元：
     * <ul>
     *   <li>assignmentRegisterDone（通常登録）</li>
     *   <li>assignmentPMRegisterDone（PM登録・増額/継続は "0" 固定で渡す）</li>
     * </ul>
     */
    private AssignmentDTO buildAssignmentDto(
            String customerIdStr, String secretaryIdStr, String taskRankIdStr, String targetYearMonth,
            String basePayCustomerStr, String basePaySecretaryStr,
            String increaseBasePayCustomerStr, String increaseBasePaySecretaryStr,
            String incentiveCustomerStr, String incentiveSecretaryStr,
            String status) {
        AssignmentDTO dto = new AssignmentDTO();
        dto.setAssignmentCustomerId(UUID.fromString(customerIdStr));
        dto.setAssignmentSecretaryId(UUID.fromString(secretaryIdStr));
        dto.setTaskRankId(UUID.fromString(taskRankIdStr));
        dto.setTargetYearMonth(targetYearMonth);
        dto.setBasePayCustomer(parseMoneyOrZero(basePayCustomerStr));
        dto.setBasePaySecretary(parseMoneyOrZero(basePaySecretaryStr));
        dto.setIncreaseBasePayCustomer(parseMoneyOrZero(increaseBasePayCustomerStr));
        dto.setIncreaseBasePaySecretary(parseMoneyOrZero(increaseBasePaySecretaryStr));
        dto.setCustomerBasedIncentiveForCustomer(parseMoneyOrZero(incentiveCustomerStr));
        dto.setCustomerBasedIncentiveForSecretary(parseMoneyOrZero(incentiveSecretaryStr));
        dto.setAssignmentStatus(status);
        return dto;
    }


    /**
     * エラー時、入力フォームの値を戻す。
     */
    private void populateFormBackForRegister(
            String customerId, String secretaryId, String taskRankId, String ym,
            String baseCust, String baseSec, String incCust, String incSec,
            String incentCust, String incentSec, String status) {
        req.setAttribute(A_ERROR, validation.getErrorMsg());
        req.setAttribute(A_FORM_CUSTOMER_ID, customerId);
        req.setAttribute(A_FORM_SECRETARY_ID, secretaryId);
        req.setAttribute(A_FORM_TASK_RANK_ID, taskRankId);
        req.setAttribute(A_FORM_YM, ym);
        req.setAttribute(A_FORM_BASE_CUST, baseCust);
        req.setAttribute(A_FORM_BASE_SEC,  baseSec);
        req.setAttribute(A_FORM_INC_CUST,  incCust);
        req.setAttribute(A_FORM_INC_SEC,   incSec);
        req.setAttribute(A_FORM_INCENT_CUST, incentCust);
        req.setAttribute(A_FORM_INCENT_SEC,  incentSec);
        req.setAttribute(A_FORM_STATUS, status);
    }

    /**
     * セレクトボックス用：秘書一覧を request へ（pmOnly=true なら PM のみ）。
     * @param tm トランザクション
     */
    private void loadSecretariesToRequest(TransactionManager tm, boolean pmOnly) {
        SecretaryDAO secretaryDAO = new SecretaryDAO(tm.getConnection());
        List<SecretaryDTO> sDTOs = pmOnly ? secretaryDAO.selectAllPM() : secretaryDAO.selectAll();
        List<Secretary> list = new ArrayList<>();
        for (SecretaryDTO d : sDTOs) list.add(conv.toDomain(d));
        req.setAttribute(A_SECRETARIES, list);
    }

    /**
     * セレクトボックス用：全タスクランク一覧を request へ。
     */
    private void loadTaskRanksToRequest(TransactionManager tm) {
        TaskRankDAO taskRankDAO = new TaskRankDAO(tm.getConnection());
        List<TaskRankDTO> tDTOs = taskRankDAO.selectAll();
        List<TaskRank> list = new ArrayList<>();
        for (TaskRankDTO d : tDTOs) list.add(conv.toDomain(d));
        req.setAttribute(A_TASK_RANKS, list);
    }

    /**
     * PM 用：「ランクP」を1件選び、taskRank として request に渡す。
     * 取得できなければ例外。
     */
    private void loadPMTaskRankToRequest(TransactionManager tm) {
        TaskRankDAO trdao = new TaskRankDAO(tm.getConnection());
        TaskRankDTO trd = trdao.selectPM();
        if (trd == null) throw new RuntimeException("TaskRankが取得できませんでした。");
        req.setAttribute(A_TASK_RANK, conv.toDomain(trd));
    }

    /**
     * fromYM を末尾とした「秘書×顧客」の連続月数を数える（同ランクか否かは不問）。
     * @return 連続月数
     */
    private int countConsecutiveMonths(TransactionManager tm, UUID secretaryId, UUID customerId, String fromYM) {
        AssignmentDAO dao = new AssignmentDAO(tm.getConnection());
        List<String> months = dao.selectMonthsForPairUpTo(secretaryId, customerId, fromYM);
        Set<String> set = new HashSet<>(months);

        YearMonth ym = YearMonth.parse(fromYM);
        int count = 0;
        while (set.contains(ym.toString())) {
            count++;
            ym = ym.minusMonths(1);
        }
        return count;
    }

    /**
     * ログイン中の担当者が属する会社ID（顧客ID）をセッションから取得。
     * @return 顧客ID、未ログイン/不一致は null
     */
    private UUID currentCustomerId() {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object user = session.getAttribute("loginUser");
        if (user instanceof LoginUser loginUser && loginUser.getCustomerContact() != null) {
            return loginUser.getCustomerContact().getCustomerId();
        }
        return null;
    }
}
