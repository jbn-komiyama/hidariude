package service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import dao.AssignmentDAO;
import dao.CustomerDAO;
import dao.DAOException;
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
    private static final String P_TARGET_YM     = "targetYearMonth";
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

    // ----- Attribute keys -----
    private static final String A_CUSTOMERS    = "customers";
    private static final String A_TARGET_YM    = "targetYm";
    private static final String A_CUSTOMER     = "customer";
    private static final String A_SECRETARIES  = "secretaries";
    private static final String A_TASK_RANKS   = "taskRanks";
    private static final String A_TASK_RANK    = "taskRank";
    private static final String A_ERROR        = "errorMsg";
    private static final String A_MESSAGE      = "message";
    private static final String A_STATUS       = "status";
    private static final String A_FUTURE_ASSIGNMENTS = "futureAssignments";

    // 確認画面・エラー戻し用（form_*）
    private static final String A_FORM_CUSTOMER_ID  = "form_customerId";
    private static final String A_FORM_SECRETARY_ID = "form_secretaryId";
    private static final String A_FORM_TASK_RANK_ID = "form_taskRankId";
    private static final String A_FORM_YM           = "form_targetYearMonth";
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
     * @return ビュー名またはリダイレクト先
     */
	public String assignmentList() {
		try (TransactionManager tm = new TransactionManager()) {
			LocalDate today = LocalDate.now();
			String yearMonth = today.format(YM_FMT);

			CustomerDAO customerDAO = new CustomerDAO(tm.getConnection());
			List<CustomerDTO> customerDtos = customerDAO.selectAllWithAssignmentsByMonth(yearMonth);

			List<Customer> customers = new ArrayList<>();
			Converter conv = new Converter();

			for (CustomerDTO customerDTO : customerDtos) {
				Customer c = conv.toDomain(customerDTO); // 既存の Customer 変換
				// AssignmentDTO -> Assignment に変換してぶら下げ
				List<Assignment> list = new ArrayList<>();
				if (customerDTO.getAssignmentDTOs() != null) {
					for (AssignmentDTO assignmentDTO : customerDTO.getAssignmentDTOs()) {
						list.add(conv.toDomain(assignmentDTO));
					}
				}
				c.setAssignments(list);

				// ===== ここから秘書ごとにグルーピング =====
				// key: secretaryId (null可) -> AssignmentGroup
				Map<UUID, AssignmentGroup> gmap = new LinkedHashMap<>();
				for (Assignment a : list) {
					UUID secId = (a.getSecretary() != null ? a.getSecretary().getId() : null);
					AssignmentGroup g = gmap.get(secId);
					if (g == null) {
						g = new AssignmentGroup();
						g.setSecretary(a.getSecretary()); // null なら「未登録」束
						gmap.put(secId, g);
					}
					g.getAssignments().add(a);
				}

				// 並び替えたい場合（例：秘書名→タスクランク）ここでソート
				List<AssignmentGroup> groups = new ArrayList<>(gmap.values());
				// 秘書名(未登録は最後)でソート
				groups.sort((g1, g2) -> {
					String n1 = g1.getSecretary() == null ? "\uFFFF" : g1.getSecretary().getName();
					String n2 = g2.getSecretary() == null ? "\uFFFF" : g2.getSecretary().getName();
					return n1.compareTo(n2);
				});

				c.setAssignmentGroups(groups);

				customers.add(c);
			}

			req.setAttribute(A_CUSTOMERS, customers);
            req.setAttribute(A_TARGET_YM, yearMonth);
            return VIEW_HOME;

		} catch (RuntimeException e) {
			e.printStackTrace();
			return req.getContextPath() + req.getServletPath() + "/error";
		}
	}

	
	// =========================================================
    // 登録画面表示（通常／PM）— 内部共通化
    // =========================================================

	/**
	 * アサイン登録 画面表示
	 * - 顧客情報をリクエストへ
	 * - 「今月(yyyy-MM)以降」のアサイン一覧を取得してリクエストへ
	 * - 秘書/タスクランクの候補も投入
	 */
	public String assignmentRegister() throws DAOException {

	    final String idStr       = req.getParameter(P_ID);
	    final String companyName = req.getParameter(P_COMPANY_NAME);
	    try {
	        Customer customer = new Customer();
	        if (idStr != null && !idStr.isBlank()) {
	            try {
	                customer.setId(java.util.UUID.fromString(idStr));
	            } catch (IllegalArgumentException ex) {
	                throw new DAOException("不正な顧客ID形式です: " + idStr, ex);
	            }
	        }
	        customer.setCompanyName(companyName);
	        req.setAttribute(A_CUSTOMER, customer);

	        // --- 今月(yyyy-MM)以降のアサイン一覧を取得 ---
	        String fromYm = LocalDate.now()
	                .format(DateTimeFormatter.ofPattern("yyyy-MM"));

	        AssignmentDAO assignDao = new AssignmentDAO(conn);
	        List<AssignmentDTO> futureList =
	                (customer.getId() == null)
	                        ? Collections.emptyList()
	                        : assignDao.selectByCustomerFromYearMonth(customer.getId(), fromYm);
	        req.setAttribute(A_FUTURE_ASSIGNMENTS, futureList);

	        loadSecretariesToRequest(tm, false);
	        loadTaskRanksToRequest(tm);

	        tm.commit();
	        return VIEW_REGISTER;

	    } catch (Exception e) {
	        try { tm.rollback(); } catch (Exception ignore) {}
	        throw new DAOException("E:SRV-ASSIGN-REGISTER 画面表示に失敗しました。", e);
	    } finally {
	        try { tm.close(); } catch (Exception ignore) {}
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
            return req.getContextPath() + "/admin/assignment";
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
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // ========= 確認画面（POST） =========

	// ★ 修正後（名称を確認画面に表示するために DB から名称を取得して積む）
	public String assignmentRegisterCheck() {
	    // ★ 1) まず form_* を積んでおく（確認画面の hidden/表示用）
	    pushFormBackToRequestForCheck(false);

	    // ★ 2) POSTされたIDを取り出し
	    final String customerIdStr  = req.getParameter(P_CUSTOMER_ID);
	    final String secretaryIdStr = req.getParameter(P_SECRETARY_ID);
	    final String taskRankIdStr  = req.getParameter(P_TASK_RANK_ID);

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
        final String targetYearMonth     = req.getParameter(P_TARGET_YM);
        final String basePayCustomerStr  = req.getParameter(P_BASE_CUST);
        final String basePaySecretaryStr = req.getParameter(P_BASE_SEC);
        final String status              = req.getParameter(P_STATUS);

        // 必須
        validation.isNull("顧客", customerIdStr);
        validation.isNull("PM秘書", secretaryIdStr);
        validation.isNull("タスクランク", taskRankIdStr);
        validation.isNull("対象月", targetYearMonth);

        // 形式
        if (!validation.isUuid(customerIdStr))  validation.addErrorMsg("顧客の指定が不正です");
        if (!validation.isUuid(secretaryIdStr)) validation.addErrorMsg("秘書の指定が不正です");
        if (!validation.isUuid(taskRankIdStr))  validation.addErrorMsg("業務ランクの指定が不正です");
        if (!validation.isYearMonth(targetYearMonth)) {
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
            req.setAttribute(A_TARGET_YM, targetYearMonth);
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
            req.setAttribute(A_TARGET_YM, targetYearMonth);
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
    	String customerNameStr = req.getParameter(P_COMPANY_NAME);
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
            populateFormBackForRegister(customerNameStr,customerIdStr, secretaryIdStr, taskRankIdStr, ym,
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
        		customerNameStr,customerIdStr, secretaryIdStr, taskRankIdStr, ym,
                baseCustStr, baseSecStr, incCustStr, incSecStr, incentCustStr, incentSecStr, status);

        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());

            if (dao.existsDuplicate(dto)) {
                validation.addErrorMsg("同月・同顧客・同秘書・同ランクのアサインは既に登録済みです。");
                // ★ フォーム戻し＋一覧再設定
                populateFormBackForRegister(customerNameStr,customerIdStr, secretaryIdStr, taskRankIdStr, ym,
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
    	final String customerNameStr = req.getParameter(P_COMPANY_NAME);
        final String customerIdStr       = req.getParameter(P_CUSTOMER_ID);
        final String secretaryIdStr      = req.getParameter(P_SECRETARY_ID);
        final String taskRankIdStr       = req.getParameter(P_TASK_RANK_ID);
        final String targetYearMonth     = req.getParameter(P_TARGET_YM);
        final String basePayCustomerStr  = req.getParameter(P_BASE_CUST);
        final String basePaySecretaryStr = req.getParameter(P_BASE_SEC);
        final String status              = req.getParameter(P_STATUS);

        // 検証（従来どおり）
        validation.isNull("顧客", customerIdStr);
        validation.isNull("PM秘書", secretaryIdStr);
        validation.isNull("タスクランク", taskRankIdStr);
        validation.isNull("対象月", targetYearMonth);
        if (!validation.isUuid(customerIdStr))  validation.addErrorMsg("顧客の指定が不正です");
        if (!validation.isUuid(secretaryIdStr)) validation.addErrorMsg("秘書の指定が不正です");
        if (!validation.isUuid(taskRankIdStr))  validation.addErrorMsg("業務ランクの指定が不正です");
        if (!validation.isYearMonth(targetYearMonth)) {
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
            		customerNameStr,
            		customerIdStr,
                    secretaryIdStr,
                    taskRankIdStr,
                    targetYearMonth,
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
                    req.setAttribute(A_TARGET_YM, targetYearMonth);
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
        req.setAttribute("form_customerId", req.getParameter(P_CUSTOMER_ID));
        req.setAttribute("form_secretaryId", req.getParameter(P_SECRETARY_ID));
        req.setAttribute("form_taskRankId",  req.getParameter(P_TASK_RANK_ID));
        req.setAttribute("form_targetYearMonth", req.getParameter(P_TARGET_YM));
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
    		String customerName, String customerId, String secretaryId, String taskRankId, String ym,
            String baseCust, String baseSec, String incCust, String incSec,
            String incentCust, String incentSec, String status) {
        req.setAttribute("errorMsg", validation.getErrorMsg());
        req.setAttribute("form_customerName", customerName);
        req.setAttribute("form_customerId", customerId);
        req.setAttribute("form_secretaryId", secretaryId);
        req.setAttribute("form_taskRankId", taskRankId);
        req.setAttribute("form_targetYearMonth", ym);
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

    /** 文字列群から AssignmentDTO を構築。 */
    private AssignmentDTO buildAssignmentDto(
    		String customerName,String customerIdStr, String secretaryIdStr, String taskRankIdStr, String targetYearMonth,
            String basePayCustomerStr, String basePaySecretaryStr,
            String increaseBasePayCustomerStr, String increaseBasePaySecretaryStr,
            String incentiveCustomerStr, String incentiveSecretaryStr,
            String status) {

        AssignmentDTO dto = new AssignmentDTO();
        dto.setCustomerCompanyName(customerName);
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
}