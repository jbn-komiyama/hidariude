package service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import dao.AssignmentDAO;
import dao.CustomerDAO;
import dao.SecretaryDAO;
import dao.TaskRankDAO;
import dao.TransactionManager;
import domain.Assignment;
import domain.AssignmentGroup;
import domain.Customer;
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
 * </ul>
 * 入力値検証は {@code Validation} を利用し、重複チェックは DAO で実施します。
 */
public class AssignmentService extends BaseService {
	
	// =========================================================
    // 定数・共通化（パラメータ名／パス／フォーマッタ／コンバータ）
    // =========================================================
    /** 年月フォーマッタ（yyyy-MM） */
    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM"); // ★ CHANGED: フォーマッタを定数化

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

    // ----- View names -----
    private static final String VIEW_HOME                = "assignment/admin/home";
    private static final String VIEW_REGISTER            = "assignment/admin/register";
    private static final String VIEW_REGISTER_CHECK      = "assignment/admin/register_check";
    private static final String VIEW_REGISTER_DONE       = "assignment/admin/register_done";
    private static final String VIEW_PM_REGISTER         = "assignment/admin/pm_register";
    private static final String VIEW_PM_REGISTER_CHECK   = "assignment/admin/pm_register_check";
    private static final String VIEW_PM_REGISTER_DONE    = "assignment/admin/pm_register_done";
    private static final String VIEW_CARRY_OVER_PREVIEW = "assignment/admin/carry_over_preview";
    private static final String VIEW_EDIT_INCENTIVE = "assignment/admin/edit_incentive";

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

    // ★ CHANGED: 変換用インスタンスをフィールド化（new の重複排除）
    private final Converter conv = new Converter();
    
	public AssignmentService(HttpServletRequest req, boolean useDB) {
		super(req, useDB);
	}

	/**
	 * アサインの管理ホーム（当月分一覧）を表示。
	 * ・当月にアサインが無い顧客も「PM秘書登録」行を出すため、基本は customers を起点に LEFT JOIN した結果を使う
	 * ・フィルタ（顧客名 / 秘書 / 継続≧N）も SQL 側で実施
	 * ・「継続月数の高い順」ソートは、フィルタ未指定のときのみ専用SQLを使用（全社＋継続DESC）
	 */
	public String assignmentList() {
	    String targetYM = req.getParameter(A_TARGET_YM);

	    // 参照可能な上限は「来月」までにクランプ（JST基準）
	    ZoneId Z_JST = ZoneId.of("Asia/Tokyo");
	    String ymNow  = LocalDate.now(Z_JST).format(YM_FMT);
	    String ymNext = LocalDate.now(Z_JST).plusMonths(1).format(YM_FMT);
	    if (targetYM == null || targetYM.isBlank()) targetYM = ymNow;
	    try {
	        if (YearMonth.parse(targetYM).isAfter(YearMonth.parse(ymNext))) {
	            targetYM = ymNext;
	        }
	    } catch (Exception ignore) { targetYM = ymNow; }

	    // ===== 検索条件 =====
	    String minMonthsStr   = req.getParameter("minMonths"); // 例: "6"
	    Integer minMonths     = null;
	    try {
	        if (minMonthsStr != null && !minMonthsStr.isBlank()) {
	            minMonths = Integer.valueOf(minMonthsStr);
	        }
	    } catch (Exception ignore) {}

	    String secretaryIdStr = req.getParameter("secretaryId");
	    UUID filterSecId      = (secretaryIdStr != null && validation.isUuid(secretaryIdStr))
	                            ? UUID.fromString(secretaryIdStr) : null;

	    String qCustomer      = req.getParameter("qCustomer");   // 顧客名 部分一致
	    String sort           = req.getParameter("sort");        // "months_desc" なら継続月数降順
	    boolean sortByMonthsDesc = "months_desc".equalsIgnoreCase(sort);

	    boolean hasFilters = (filterSecId != null)
	                      || (minMonths != null)
	                      || (qCustomer != null && !qCustomer.isBlank());

	    try (TransactionManager tm = new TransactionManager()) {
	        // プルダウン用：秘書一覧
	        loadSecretariesToRequest(tm, false);

	        AssignmentDAO aDao = new AssignmentDAO(tm.getConnection());

	        // ===== データ取得（customers 起点で「当月アサイン無しの顧客」も含める）=====
	        // フィルタ無しで「継続月数の高い順」を要求された場合のみ、専用SQLで継続DESCを実現
	        List<dto.CustomerDTO> cRows;
	        if (sortByMonthsDesc && !hasFilters) {
	            cRows = aDao.selectAllByMonthOrderByConsecutiveDesc(targetYM);
	        } else {
	            cRows = aDao.selectAllByMonthFiltered(targetYM, qCustomer, filterSecId, minMonths);
	            // ※ フィルタ併用時の「継続月数DESC」ソートは DB 側に実装していないため、通常順（社名→秘書名→rank→作成日）
	        }

	        // ===== DTO → Domain 整形（秘書ごとグルーピング & contMonths マップ作成）=====
	        Map<UUID, Integer> contMonths = new HashMap<>();
	        List<Customer> customers = new ArrayList<>();

	        for (CustomerDTO cdto : cRows) {
	            Customer c = new Customer();
	            c.setId(cdto.getId());
	            c.setCompanyName(cdto.getCompanyName());

	            if (cdto.getAssignmentDTOs() != null && !cdto.getAssignmentDTOs().isEmpty()) {
	                Map<UUID, AssignmentGroup> gmap = new LinkedHashMap<>();

	                for (AssignmentDTO adto : cdto.getAssignmentDTOs()) {
	                    // 継続月数マップ（a.id -> cont）。null は 0 扱い
	                    if (adto.getAssignmentId() != null) {
	                        Integer cont = adto.getConsecutiveMonths();
	                        contMonths.put(adto.getAssignmentId(), cont == null ? 0 : cont);
	                    }

	                    Assignment a = conv.toDomain(adto); // ← フィールドの Converter を使用
	                    UUID sid = (a.getSecretary() != null ? a.getSecretary().getId() : null);

	                    AssignmentGroup g = gmap.get(sid);
	                    if (g == null) {
	                        g = new AssignmentGroup();
	                        g.setSecretary(a.getSecretary()); // null の場合は JSP 側で '—' 表示
	                        gmap.put(sid, g);
	                    }
	                    g.getAssignments().add(a); // SQL の並びを保持
	                }
	                c.setAssignmentGroups(new ArrayList<>(gmap.values()));
	                // ※ アサインが1件も無い顧客は assignmentGroups を null のままにしておく
	            }

	            customers.add(c);
	        }

	        // ===== 画面用属性 =====
	        String prevYM = LocalDate.parse(targetYM + "-01").minusMonths(1).format(YM_FMT);
	        req.setAttribute("canCarryOver", ymNow.equals(targetYM) || ymNext.equals(targetYM));
	        req.setAttribute("prevYM", prevYM);
	        req.setAttribute("maxYM", ymNext);

	        req.setAttribute("contMonths", contMonths); // JSP で a.id をキーに継続月数を表示
	        req.setAttribute(A_CUSTOMERS, customers);
	        req.setAttribute(A_TARGET_YM, targetYM);

	        // 検索フォームの値戻し
	        req.setAttribute("f_minMonths",  minMonthsStr);
	        req.setAttribute("f_secretaryId", secretaryIdStr);
	        req.setAttribute("f_qCustomer",  qCustomer);
	        req.setAttribute("f_sort",       sort);

	        return "assignment/admin/home";
	    } catch (RuntimeException e) {
	        e.printStackTrace();
	        return req.getContextPath() + req.getServletPath() + "/error";
	    }
	}

	
	// =========================================================
    // 登録画面表示（通常／PM）— 内部共通化
    // =========================================================

	/** 通常のアサイン登録画面表示（従来どおり） */
	public String assignmentRegister() {
		String ym = req.getParameter(P_TARGET_YM);
        if (ym == null || ym.isBlank()) {
            ym = LocalDate.now().format(YM_FMT);
        }
        req.setAttribute(A_TARGET_YM, ym);

        final String idStr       = req.getParameter(P_ID);
        final String companyName = req.getParameter(P_COMPANY_NAME);

        if (validation.isNull("会社名", companyName) | validation.isNull("会社ID", idStr)) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            System.out.println("test");
            return req.getContextPath() + "/admin/assignment";
        }

        try (TransactionManager tm = new TransactionManager()) {
            // 顧客（固定表示）
            Customer customer = new Customer();
            customer.setId(UUID.fromString(idStr));
            customer.setCompanyName(companyName);
            req.setAttribute(A_CUSTOMER, customer);

            // ★ ヘルパー使用：秘書（全件）とタスクランク（全件）
            loadSecretariesToRequest(tm, false);
            loadTaskRanksToRequest(tm);

            return VIEW_REGISTER;
        } catch (RuntimeException e) {
        	e.printStackTrace();
        	return req.getContextPath() + req.getServletPath() + "/error";
        }
	}
	
	/** PM（is_pm_secretary = TRUE）向けのアサイン登録画面表示 */
	public String assignmentPMRegister() {
        String ym = req.getParameter(P_TARGET_YM);
        if (ym == null || ym.isBlank()) {
            ym = LocalDate.now().format(YM_FMT);
        }
        req.setAttribute(A_TARGET_YM, ym);

        final String companyIdStr = req.getParameter(P_COMPANY_ID);
        final String companyName  = req.getParameter(P_COMPANY_NAME);

        if (validation.isNull("会社名", companyName) | validation.isNull("会社ID", companyIdStr)) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return REDIRECT_ERROR;
        }

        try (TransactionManager tm = new TransactionManager()) {
            // 顧客（固定表示）
            Customer customer = new Customer();
            customer.setId(UUID.fromString(companyIdStr));
            customer.setCompanyName(companyName);
            req.setAttribute(A_CUSTOMER, customer);

            // ★ ヘルパー使用：PM秘書のみプルダウン
            loadSecretariesToRequest(tm, true);

            // ランクPを取得して固定表示
            loadPMTaskRankToRequest(tm);

            return VIEW_PM_REGISTER;
        } catch (RuntimeException e) {
            return REDIRECT_ERROR;
        }
    }

    // ========= 確認画面（POST） =========

	// ★ 修正後（名称を確認画面に表示するために DB から名称を取得して積む）
	public String assignmentRegisterCheck() {
	    // ★ 1) まず form_* を積んでおく（確認画面の hidden/表示用）
	    pushFormBackToRequestForCheck(false);

	    // ★ 2) POSTされたIDを取り出し
	    final String customerIdStr  = req.getParameter(P_ID);
	    final String secretaryIdStr = req.getParameter(P_SECRETARY_ID);
	    final String taskRankIdStr  = req.getParameter(P_TASK_RANK_ID);
	    final String targetYM  = req.getParameter(P_TARGET_YM);
	    
	    // ★ 3) 軽い検証（IDはUUID想定）
	    if (!validation.isUuid(customerIdStr)) validation.addErrorMsg("顧客の指定が不正です");
	    if (!validation.isUuid(secretaryIdStr)) validation.addErrorMsg("秘書の指定が不正です");
	    if (!validation.isUuid(taskRankIdStr))  validation.addErrorMsg("業務ランクの指定が不正です");

	    if (validation.hasErrorMsg()) {
	        // ★ 4) 入力画面に戻す際、必要なプルダウンを再ロード
	        try (TransactionManager tm = new TransactionManager()) {
	            loadSecretariesToRequest(tm, false);
	            loadTaskRanksToRequest(tm);
	            // 顧客名も可能なら再取得して固定表示
	            if (validation.isUuid(customerIdStr)) {
	                CustomerDAO cdao = new CustomerDAO(tm.getConnection());
	                CustomerDTO cdto = cdao.selectByUUId(UUID.fromString(customerIdStr));
	                if (cdto != null) req.setAttribute(A_CUSTOMER, conv.toDomain(cdto));
	            }
	        }
	        req.setAttribute(A_ERROR, validation.getErrorMsg());
	        return VIEW_REGISTER; // ★ 入力画面に戻す
	    }

	    // ★ 5) 確認画面表示用の名称を取得して積む
	    try (TransactionManager tm = new TransactionManager()) {
	        CustomerDAO  cdao  = new CustomerDAO(tm.getConnection());
	        SecretaryDAO sdao  = new SecretaryDAO(tm.getConnection());
	        TaskRankDAO  trdao = new TaskRankDAO(tm.getConnection());

	        CustomerDTO  cdto  = cdao.selectByUUId(UUID.fromString(customerIdStr));
	        SecretaryDTO sdto  = sdao.selectByUUId(UUID.fromString(secretaryIdStr));

	        // ★ TaskRank は id 指定で1件取得APIがない前提のため、selectAllから絞り込み
	        TaskRankDTO trdto = null;
	        for (TaskRankDTO d : trdao.selectAll()) {
	            if (d.getId().equals(UUID.fromString(taskRankIdStr))) { trdto = d; break; }
	        }

	        // ★ 取得できたものを request にセット（JSP側が参照するキー名に注意）
	        if (cdto != null)  req.setAttribute(A_CUSTOMER,  conv.toDomain(cdto));   // ${customer.companyName}
	        if (sdto != null)  req.setAttribute("secretary", conv.toDomain(sdto));   // ${secretary.name}
	        if (trdto != null) req.setAttribute(A_TASK_RANK, conv.toDomain(trdto));  // ${taskRank.rankName}
	    } catch (RuntimeException e) {
	        return req.getContextPath() + req.getServletPath() + "/error";
	    }

	    return VIEW_REGISTER_CHECK;
	}

    // ====== PM用 確認画面 ======
    /**
     * PMアサイン登録の確認画面表示。
     * <p>入力値の軽いバリデーションを行い、確認用の表示データを整えてから
     *  "assignment/admin/pm_register_check" を返します。</p>
     * <ul>
     *   <li>単価（顧客/秘書）は入力値（hidden）をそのまま使用</li>
     *   <li>増額/継続インセンティブはPM仕様で0固定（非表示）</li>
     * </ul>
     */
    public String assignmentPMRegisterCheck() {
        final String customerIdStr       = req.getParameter(P_CUSTOMER_ID);
        final String secretaryIdStr      = req.getParameter(P_SECRETARY_ID);
        final String taskRankIdStr       = req.getParameter(P_TASK_RANK_ID);
        final String targetYM     = req.getParameter(P_TARGET_YM);
        final String basePayCustomerStr  = req.getParameter(P_BASE_CUST);
        final String basePaySecretaryStr = req.getParameter(P_BASE_SEC);
        final String status              = req.getParameter(P_STATUS);

        // 必須
        validation.isNull("顧客", customerIdStr);
        validation.isNull("PM秘書", secretaryIdStr);
        validation.isNull("タスクランク", taskRankIdStr);
        validation.isNull("対象月", targetYM);

        // 形式
        if (!validation.isUuid(customerIdStr))  validation.addErrorMsg("顧客の指定が不正です");
        if (!validation.isUuid(secretaryIdStr)) validation.addErrorMsg("秘書の指定が不正です");
        if (!validation.isUuid(taskRankIdStr))  validation.addErrorMsg("業務ランクの指定が不正です");
        if (!validation.isYearMonth(targetYM)) {
            validation.addErrorMsg("対象月は yyyy-MM 形式で入力してください");
        }
        validation.mustBeMoneyOrZero("単価（顧客）", basePayCustomerStr);
        validation.mustBeMoneyOrZero("単価（秘書）", basePaySecretaryStr);

        if (validation.hasErrorMsg()) {
        	// 入力エラー→画面再表示に必要なデータを再ロード
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            try (TransactionManager tm = new TransactionManager()) {
                // 顧客（名称表示用）
                CustomerDAO cdao = new CustomerDAO(tm.getConnection());
                CustomerDTO cdto = cdao.selectByUUId(UUID.fromString(customerIdStr));
                req.setAttribute(A_CUSTOMER, conv.toDomain(cdto));

                // PM秘書
                loadSecretariesToRequest(tm, true);

                // ランクP
                loadPMTaskRankToRequest(tm);
            } catch (RuntimeException ignore) { }

            // フォーム値復元
            req.setAttribute(A_TARGET_YM, targetYM);
            req.setAttribute(A_STATUS, status);
            req.setAttribute(A_FORM_SECRETARY_ID, secretaryIdStr);
            return VIEW_PM_REGISTER;
        }

        // 確認画面の表示情報を取得（名称など）
        try (TransactionManager tm = new TransactionManager()) {
            CustomerDAO cdao = new CustomerDAO(tm.getConnection());
            SecretaryDAO sdao = new SecretaryDAO(tm.getConnection());
            TaskRankDAO trdao = new TaskRankDAO(tm.getConnection());

            CustomerDTO cdto = cdao.selectByUUId(UUID.fromString(customerIdStr));
            SecretaryDTO sdto = sdao.selectByUUId(UUID.fromString(secretaryIdStr));
            TaskRankDTO trdto = trdao.selectPM(); // ランクP固定

            req.setAttribute(A_CUSTOMER,  conv.toDomain(cdto));
            req.setAttribute(A_TASK_RANK, conv.toDomain(trdto));
            req.setAttribute("secretary", conv.toDomain(sdto)); // ここは画面で secretary を参照している想定

            // 表示 & hidden 引き継ぎ
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


    // ========= 完了（INSERT） =========

    /** 通常登録の完了（INSERT）。 */
    public String assignmentRegisterDone() {
    	 // 値取得
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

     // 検証
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
            // ★ ヘルパーでフォーム値戻し
            populateFormBackForRegister(customerIdStr, secretaryIdStr, taskRankIdStr, ym,
                    baseCustStr, baseSecStr, incCustStr, incSecStr, incentCustStr, incentSecStr, status);
            try (TransactionManager tm = new TransactionManager()) {
                // ★ ヘルパーでリスト再設定
                loadSecretariesToRequest(tm, false);
                loadTaskRanksToRequest(tm);
            }
            return VIEW_REGISTER;
        }

        // ★ ヘルパーで DTO 構築
        AssignmentDTO dto = buildAssignmentDto(
                customerIdStr, secretaryIdStr, taskRankIdStr, ym,
                baseCustStr, baseSecStr, incCustStr, incSecStr, incentCustStr, incentSecStr, status);

        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());

            if (dao.existsDuplicate(dto)) {
                validation.addErrorMsg("同月・同顧客・同秘書・同ランクのアサインは既に登録済みです。");
                // ★ フォーム戻し＋一覧再設定
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
    
    
    // ====== PM用 確定（登録） ======
    /**
     * PMアサイン登録の確定処理。
     * <ul>
     *   <li>単価（顧客/秘書）は hidden から受け取り</li>
     *   <li>増額／継続インセンティブは 0 円固定（非表示）</li>
     *   <li>重複チェック（同月・同顧客・同秘書・同ランク）</li>
     * </ul>
     */
    public String assignmentPMRegisterDone() {
        final String customerIdStr       = req.getParameter(P_CUSTOMER_ID);
        final String secretaryIdStr      = req.getParameter(P_SECRETARY_ID);
        final String taskRankIdStr       = req.getParameter(P_TASK_RANK_ID);
        final String targetYM     = req.getParameter(P_TARGET_YM);
        final String basePayCustomerStr  = req.getParameter(P_BASE_CUST);
        final String basePaySecretaryStr = req.getParameter(P_BASE_SEC);
        final String status              = req.getParameter(P_STATUS);

        // 検証（従来どおり）
        validation.isNull("顧客", customerIdStr);
        validation.isNull("PM秘書", secretaryIdStr);
        validation.isNull("タスクランク", taskRankIdStr);
        validation.isNull("対象月", targetYM);
        if (!validation.isUuid(customerIdStr))  validation.addErrorMsg("顧客の指定が不正です");
        if (!validation.isUuid(secretaryIdStr)) validation.addErrorMsg("秘書の指定が不正です");
        if (!validation.isUuid(taskRankIdStr))  validation.addErrorMsg("業務ランクの指定が不正です");
        if (!validation.isYearMonth(targetYM)) {
            validation.addErrorMsg("対象月は yyyy-MM 形式で入力してください");
        }
        validation.mustBeMoneyOrZero("単価（顧客）", basePayCustomerStr);
        validation.mustBeMoneyOrZero("単価（秘書）", basePaySecretaryStr);

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        try (TransactionManager tm = new TransactionManager()) {
            // ★ CHANGED: ヘルパーでDTOを構築（PM仕様：増額/継続は "0" 固定）
            AssignmentDTO dto = buildAssignmentDto(
                    customerIdStr,
                    secretaryIdStr,
                    taskRankIdStr,
                    targetYM,
                    basePayCustomerStr,
                    basePaySecretaryStr,
                    "0", "0", "0", "0",   // ← PMは0固定
                    status
            );

            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());

            // 重複チェック（同月・同顧客・同秘書・同ランク）
            if (dao.existsDuplicate(dto)) {
                validation.addErrorMsg("同月・同顧客・同秘書・同ランクのアサインは既に登録済みです。");
                req.setAttribute(A_ERROR, validation.getErrorMsg());

                try {
                    // 入力画面再表示のため最低限を再ロード
                    CustomerDAO cdao = new CustomerDAO(tm.getConnection());
                    CustomerDTO cdto = cdao.selectByUUId(UUID.fromString(customerIdStr));
                    req.setAttribute(A_CUSTOMER, conv.toDomain(cdto));

                    loadSecretariesToRequest(tm, true);
                    loadPMTaskRankToRequest(tm);
                    req.setAttribute(A_TARGET_YM, targetYM);
                    req.setAttribute(A_STATUS,    status); // 任意：ステータスも戻す
                } catch (RuntimeException ignore) { }
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

    

    // ========= Helper =========

    /** 金額文字列を BigDecimal に変換（空/blank→0、カンマ除去）。 */
    private BigDecimal parseMoneyOrZero(String s) {
        if (s == null) return BigDecimal.ZERO;
        String t = s.trim();
        if (t.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(t.replace(",", ""));
        }
    /** 確認画面用：入力値を form_* 名でリクエストに積む。 */
    private void pushFormBackToRequestForCheck(boolean pmMode) {
        req.setAttribute("form_customerId", req.getParameter(P_ID));
        req.setAttribute("form_secretaryId", req.getParameter(P_SECRETARY_ID));
        req.setAttribute("form_taskRankId",  req.getParameter(P_TASK_RANK_ID));
        req.setAttribute("form_targetYM", req.getParameter(P_TARGET_YM));
        req.setAttribute("form_basePayCustomer",  req.getParameter(P_BASE_CUST));
        req.setAttribute("form_basePaySecretary", req.getParameter(P_BASE_SEC));
        if (!pmMode) {
            req.setAttribute("form_increaseBasePayCustomer", req.getParameter(P_INC_CUST));
            req.setAttribute("form_increaseBasePaySecretary", req.getParameter(P_INC_SEC));
            req.setAttribute("form_customerBasedIncentiveForCustomer", req.getParameter(P_INCENT_CUST));
            req.setAttribute("form_customerBasedIncentiveForSecretary", req.getParameter(P_INCENT_SEC));
        } else {
            req.setAttribute("form_increaseBasePayCustomer", "0");
            req.setAttribute("form_increaseBasePaySecretary", "0");
            req.setAttribute("form_customerBasedIncentiveForCustomer", "0");
            req.setAttribute("form_customerBasedIncentiveForSecretary", "0");
        }
        req.setAttribute("form_status", req.getParameter(P_STATUS));
    }

    /** エラー時、入力フォームの値を戻す。 */
    private void populateFormBackForRegister(
            String customerId, String secretaryId, String taskRankId, String ym,
            String baseCust, String baseSec, String incCust, String incSec,
            String incentCust, String incentSec, String status) {
        req.setAttribute("errorMsg", validation.getErrorMsg());
        req.setAttribute("form_customerId", customerId);
        req.setAttribute("form_secretaryId", secretaryId);
        req.setAttribute("form_taskRankId", taskRankId);
        req.setAttribute("form_targetYM", ym);
        req.setAttribute("form_basePayCustomer", baseCust);
        req.setAttribute("form_basePaySecretary", baseSec);
        req.setAttribute("form_increaseBasePayCustomer", incCust);
        req.setAttribute("form_increaseBasePaySecretary", incSec);
        req.setAttribute("form_customerBasedIncentiveForCustomer", incentCust);
        req.setAttribute("form_customerBasedIncentiveForSecretary", incentSec);
        req.setAttribute("form_status", status);
    }

    /** セレクトボックス用：秘書一覧を request へ（pmOnly=true なら PM のみ）。 */
    private void loadSecretariesToRequest(TransactionManager tm, boolean pmOnly) {
        SecretaryDAO secretaryDAO = new SecretaryDAO(tm.getConnection());
        List<SecretaryDTO> sDTOs = pmOnly ? secretaryDAO.selectAllPM() : secretaryDAO.selectAll();
        List<Secretary> list = new ArrayList<>();
        for (SecretaryDTO d : sDTOs) list.add(conv.toDomain(d));
        req.setAttribute(A_SECRETARIES, list);
    }

    /** セレクトボックス用：全タスクランク一覧。 */
    private void loadTaskRanksToRequest(TransactionManager tm) {
        TaskRankDAO taskRankDAO = new TaskRankDAO(tm.getConnection());
        List<TaskRankDTO> tDTOs = taskRankDAO.selectAll();
        List<TaskRank> list = new ArrayList<>();
        for (TaskRankDTO d : tDTOs) list.add(conv.toDomain(d));
        req.setAttribute(A_TASK_RANKS, list);
    }

    /**
     * PM用：「ランクP」を1件選び、taskRank として request に渡す。
     * ※ TaskRankDAO.selectAll() から rankName="P" を検索
     */
    private void loadPMTaskRankToRequest(TransactionManager tm) {
        TaskRankDAO trdao = new TaskRankDAO(tm.getConnection());
        TaskRankDTO trd = trdao.selectPM();
        if (trd == null) {
            throw new RuntimeException("TaskRankが取得できませんでした。");
        }
        req.setAttribute(A_TASK_RANK, conv.toDomain(trd));
    }
    
    /** 引継ぎプレビュー（先月→対象月）。候補のみ表示、重複（同顧客×同秘書×同ランク×対象月）は除外 */
    public String assignmentCarryOverPreview() {
        final String toYM   = req.getParameter("toYM");   // 対象月（今月 or 来月）
        final String fromYM = req.getParameter("fromYM"); // 先月（= toYM - 1）
        if (!validation.isYearMonth(toYM) || !validation.isYearMonth(fromYM)) {
            return req.getContextPath() + "/admin/assignment?targetYM=" + (toYM != null ? toYM : "");
        }

        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());
            // 1) 先月にあるが対象月に未登録の候補を取得
            List<AssignmentDTO> candidates = dao.selectCarryOverCandidates(fromYM, toYM);

            // 2) 継続月数（秘書×顧客の“連続”月数。fromYM を末尾とした連続カウント）
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

    /** 引継ぎ適用（チェック済みIDを対象月に一括INSERT） */
    public String assignmentCarryOverApply() {
        final String toYM = req.getParameter("toYM");
        if (!validation.isYearMonth(toYM)) {
            return req.getContextPath() + "/admin/assignment";
        }
        String[] ids = req.getParameterValues("assignmentId");
        if (ids == null || ids.length == 0) {
            // 何も選ばれていなければ一覧へ
            return req.getContextPath() + "/admin/assignment/carry_over_preview?toYM=" + toYM +
                   "&fromYM=" + java.time.LocalDate.parse(toYM + "-01").minusMonths(1).format(YM_FMT);
        }

        // 文字列→UUID
        java.util.List<UUID> idList = new java.util.ArrayList<>();
        for (String s : ids) try { idList.add(UUID.fromString(s)); } catch (Exception ignore) {}

        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());
            // 1) 元データをまとめて取得
            List<AssignmentDTO> rows = dao.selectByIds(idList);

            int inserted = 0;
            for (AssignmentDTO src : rows) {
                // 2) toYM へコピー用DTO作成（単価等は元の値そのまま、年月のみ変更）
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
            // 完了後は対象月の一覧へ
            return req.getContextPath() + "/admin/assignment?targetYM=" + toYM;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }
    
    /** 継続単価の編集画面（顧客/秘書/年月/ランクは変更不可・表示のみ） */
    public String assignmentEditIncentiveForm() {
        final String idStr = req.getParameter("id");
        final String ym    = req.getParameter(A_TARGET_YM); // 呼び出し元一覧への戻り用
        if (!validation.isUuid(idStr)) {
            req.setAttribute(A_ERROR, java.util.List.of("不正なアサインIDです。"));
            return req.getContextPath() + "/admin/assignment?targetYM=" + (ym != null ? ym : "");
        }

        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());
            AssignmentDTO d = dao.selectOneWithNames(UUID.fromString(idStr));
            if (d == null) {
                req.setAttribute(A_ERROR, java.util.List.of("対象のアサインが見つかりません。"));
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

    /** 継続単価の更新（顧客×秘書×対象月の全レコードを一括更新） */
    public String assignmentEditIncentiveUpdate() {
        final String idStr   = req.getParameter("id");
        final String ymBack  = req.getParameter(A_TARGET_YM);
        final String incCust = req.getParameter(P_INCENT_CUST);
        final String incSec  = req.getParameter(P_INCENT_SEC);

        if (!validation.isUuid(idStr)) {
            req.setAttribute(A_ERROR, java.util.List.of("不正なアサインIDです。"));
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

            // キー特定
            AssignmentDTO cur = dao.selectOne(UUID.fromString(idStr));
            if (cur == null) {
                req.setAttribute(A_ERROR, java.util.List.of("対象のアサインが見つかりません。"));
                return req.getContextPath() + "/admin/assignment?targetYM=" + (ymBack != null ? ymBack : "");
            }

            // 入力値→BigDecimal
            var custVal = parseMoneyOrZero(incCust);
            var secVal  = parseMoneyOrZero(incSec);

            // ① 当月（=編集対象の年月）へ一括反映（顧客×秘書×年月）
            dao.updateIncentivesByPairAndMonth(
                cur.getAssignmentCustomerId(),
                cur.getAssignmentSecretaryId(),
                cur.getTargetYearMonth(),
                custVal, secVal
            );

            // ② 翌月が「登録されていれば」同様に反映
            //    → UPDATE を試み、0件ならスルー（＝未登録）
            YearMonth thisYm = YearMonth.parse(cur.getTargetYearMonth());   // "yyyy-MM"
            String nextYm    = thisYm.plusMonths(1).toString();             // "yyyy-MM"

            dao.updateIncentivesByPairAndMonth(
                cur.getAssignmentCustomerId(),
                cur.getAssignmentSecretaryId(),
                nextYm,
                custVal, secVal
            );
            // ※ update 件数は戻り値で取れるので、必要なら画面に「翌月にも反映(n件)」など表示可能

            tm.commit();

            // 一覧は編集対象の月へ戻す（要件に合わせて nextYm へ戻す等でもOK）
            return req.getContextPath() + "/admin/assignment?targetYM=" + cur.getTargetYearMonth();
        } catch (RuntimeException e) {
            e.printStackTrace();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /** 削除（紐づくタスクが無い場合のみ論理削除） */
    public String assignmentDelete() {
        final String idStr  = req.getParameter("id");
        final String ymBack = req.getParameter(A_TARGET_YM);
        if (!validation.isUuid(idStr)) {
            req.setAttribute(A_ERROR, java.util.List.of("不正なアサインIDです。"));
            return req.getContextPath() + "/admin/assignment?targetYM=" + (ymBack != null ? ymBack : "");
        }

        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());
            UUID id = UUID.fromString(idStr);

            // 一覧戻り用に年月を先に取っておく
            AssignmentDTO cur = dao.selectOne(id);
            String ym = (cur != null ? cur.getTargetYearMonth() : ymBack);

            // 紐づくタスクがあるなら削除不可
            if (dao.hasTasks(id)) {
                req.setAttribute(A_ERROR, java.util.List.of("このアサインにはタスクが登録されています。削除できません。"));
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

    /** 文字列群から AssignmentDTO を構築。 */
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
    
    /** fromYM を末尾とした「秘書×顧客」の連続月数を数える（同ランクか否かは不問） */
    private int countConsecutiveMonths(TransactionManager tm, UUID secretaryId, UUID customerId, String fromYM) {
        AssignmentDAO dao = new AssignmentDAO(tm.getConnection());
        List<String> months = dao.selectMonthsForPairUpTo(secretaryId, customerId, fromYM);
        Set<String> set = new HashSet<>(months);

        YearMonth ym = YearMonth.parse(fromYM);
        int count = 0;
        while (set.contains(ym.toString())) { // "yyyy-MM"
            count++;
            ym = ym.minusMonths(1);
        }
        return count;
    }
}