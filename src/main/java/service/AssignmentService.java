package service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import dao.AssignmentDAO;
import dao.CustomerDAO;
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

public class AssignmentService extends BaseService {
	
	// =========================================================
    // 定数・共通化（パラメータ名／パス／フォーマッタ／コンバータ）
    // =========================================================
    /** 年月フォーマッタ（yyyy-MM） */
    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM"); // ★ CHANGED: フォーマッタを定数化

    // ★ CHANGED: パラメータ名を定数化（参照の一元化）
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

    // ★ CHANGED: View パスを定数化（スペルミス予防）
    private static final String VIEW_HOME          = "assignment/admin/home";
    private static final String VIEW_REGISTER      = "assignment/admin/register";
    private static final String VIEW_PM_REGISTER   = "assignment/admin/pm_register";

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

			req.setAttribute("customers", customers);
			req.setAttribute("targetYm", yearMonth);
			return "assignment/admin/home";

		} catch (RuntimeException e) {
			e.printStackTrace();
			return req.getContextPath() + req.getServletPath() + "/error";
		}
	}

	
	// =========================================================
    // 登録画面表示（通常／PM）— 内部共通化
    // =========================================================

    /** 通常のアサイン登録画面表示。 */
    public String assignmentRegister() {
        return showRegister(false); // ★ CHANGED: 共通ハンドラに委譲
    }

    /** PM（is_pm_secretary = TRUE）向けのアサイン登録画面表示。 */
    public String assignmentPMRegister() {
        return showRegister(true);  // ★ CHANGED: 共通ハンドラに委譲
    }

    /**
     * ★ CHANGED: 登録画面表示の共通ハンドラ。
     * @param pmMode true=PM画面／false=通常画面
     */
    private String showRegister(boolean pmMode) {
        String ym = req.getParameter(P_TARGET_YM);
        if (ym == null || ym.isBlank()) {
            ym = LocalDate.now().format(YM_FMT);
        }
        req.setAttribute("targetYm", ym);

        // 画面別に必要な会社IDパラメータ名が違う
        final String idParam = pmMode ? P_COMPANY_ID : P_ID;
        final String companyIdStr = req.getParameter(idParam);
        final String companyName  = req.getParameter(P_COMPANY_NAME);

        // 必須チェック（会社名／会社ID）
        if (validation.isNull("会社名", companyName) || validation.isNull("会社ID", companyIdStr)) {
            return req.getContextPath() + "/admin/assignment";
        }

        try (TransactionManager tm = new TransactionManager()) {
            // ここではID/名称のみで画面表示。必要に応じてDBから精細情報を取得してください。
            Customer customer = new Customer();
            customer.setId(UUID.fromString(companyIdStr));
            customer.setCompanyName(companyName);
            req.setAttribute("customer", customer);

            // プルダウン
            loadSecretariesToRequest(tm, pmMode);
            if (pmMode) {
                loadPMTaskRankToRequest(tm);
            } else {
                loadTaskRanksToRequest(tm);
            }

            return pmMode ? VIEW_PM_REGISTER : VIEW_REGISTER;

        } catch (RuntimeException e) {
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

	public String assignmentRegisterDone() {
		String customerIdStr = req.getParameter("customerId");
		String secretaryIdStr = req.getParameter("secretaryId");
		String taskRankIdStr = req.getParameter("taskRankId");
		String targetYearMonth = req.getParameter("targetYearMonth");

		String basePayCustomerStr = req.getParameter("basePayCustomer");
		String basePaySecretaryStr = req.getParameter("basePaySecretary");
		String increaseBasePayCustomerStr = req.getParameter("increaseBasePayCustomer");
		String increaseBasePaySecretaryStr = req.getParameter("increaseBasePaySecretary");
		String incentiveCustomerStr = req.getParameter("customerBasedIncentiveForCustomer");
		String incentiveSecretaryStr = req.getParameter("customerBasedIncentiveForSecretary");

		String status = req.getParameter("status");

		// 必須
		validation.isNull("顧客", customerIdStr);
		validation.isNull("秘書", secretaryIdStr);
		validation.isNull("業務ランク", taskRankIdStr);
		validation.isNull("対象月", targetYearMonth);

		// UUID 形式
		if (!validation.isUuid(customerIdStr))
			validation.addErrorMsg("顧客の指定が不正です");
		if (!validation.isUuid(secretaryIdStr))
			validation.addErrorMsg("秘書の指定が不正です");
		if (!validation.isUuid(taskRankIdStr))
			validation.addErrorMsg("業務ランクの指定が不正です");

		// 年月(yyyy-MM)
		if (!validation.isYearMonth(targetYearMonth))
			validation.addErrorMsg("対象月は yyyy-MM 形式で入力してください");

		// 金額（0以上の整数。小数不要の要件に合わせる）
		validation.mustBeMoneyOrZero("単価（顧客）", basePayCustomerStr);
		validation.mustBeMoneyOrZero("単価（秘書）", basePaySecretaryStr);
		validation.mustBeMoneyOrZero("増額（顧客）", increaseBasePayCustomerStr);
		validation.mustBeMoneyOrZero("増額（秘書）", increaseBasePaySecretaryStr);
		// 継続単価（任意だが、入っていたら同じチェック）
		if (!validation.isBlank(incentiveCustomerStr))
			validation.mustBeMoneyOrZero("継続単価（顧客）", incentiveCustomerStr);
		if (!validation.isBlank(incentiveSecretaryStr))
			validation.mustBeMoneyOrZero("継続単価（秘書）", incentiveSecretaryStr);

		// エラーがあれば戻す（入力値も返す）
		if (validation.hasErrorMsg()) {
			req.setAttribute("errorMsg", validation.getErrorMsg());

			// 入力値を画面に戻す（name 属性と合わせる）
			req.setAttribute("form_customerId", customerIdStr);
			req.setAttribute("form_secretaryId", secretaryIdStr);
			req.setAttribute("form_taskRankId", taskRankIdStr);
			req.setAttribute("form_targetYearMonth", targetYearMonth);
			req.setAttribute("form_basePayCustomer", basePayCustomerStr);
			req.setAttribute("form_basePaySecretary", basePaySecretaryStr);
			req.setAttribute("form_increaseBasePayCustomer", increaseBasePayCustomerStr);
			req.setAttribute("form_increaseBasePaySecretary", increaseBasePaySecretaryStr);
			req.setAttribute("form_customerBasedIncentiveForCustomer", incentiveCustomerStr);
			req.setAttribute("form_customerBasedIncentiveForSecretary", incentiveSecretaryStr);
			req.setAttribute("form_status", status);

			// 画面再表示用のプルダウンデータ（customers/secretaries/taskRanks）が必要ならここで再取得して積む
			// CustomerDAO/SecretaryDAO/TaskRankDAO を使って再セットしてください。

			return "assignment/admin/register";
		}

		HttpSession session = ((HttpServletRequest) req).getSession(false);
		LoginUser loginUser = (session == null) ? null : (LoginUser) session.getAttribute("loginUser");

		try (TransactionManager tm = new TransactionManager()) {
			UUID customerId = UUID.fromString(customerIdStr);
			UUID secretaryId = UUID.fromString(secretaryIdStr);
			UUID taskRankId = UUID.fromString(taskRankIdStr);

			BigDecimal basePayCustomer = new BigDecimal(basePayCustomerStr);
			BigDecimal basePaySecretary = new BigDecimal(basePaySecretaryStr);
			BigDecimal incPayCustomer = new BigDecimal(increaseBasePayCustomerStr);
			BigDecimal incPaySecretary = new BigDecimal(increaseBasePaySecretaryStr);
			BigDecimal incentiveCust = (incentiveCustomerStr == null || incentiveCustomerStr.isEmpty())
					? BigDecimal.ZERO
					: new BigDecimal(incentiveCustomerStr);
			BigDecimal incentiveSec = (incentiveSecretaryStr == null || incentiveSecretaryStr.isEmpty())
					? BigDecimal.ZERO
					: new BigDecimal(incentiveSecretaryStr);

			AssignmentDTO dto = new AssignmentDTO();
			dto.setAssignmentCustomerId(customerId);
			dto.setAssignmentSecretaryId(secretaryId);
			dto.setTaskRankId(taskRankId);
			dto.setTargetYearMonth(targetYearMonth);
			dto.setBasePayCustomer(basePayCustomer);
			dto.setBasePaySecretary(basePaySecretary);
			dto.setIncreaseBasePayCustomer(incPayCustomer);
			dto.setIncreaseBasePaySecretary(incPaySecretary);
			dto.setCustomerBasedIncentiveForCustomer(incentiveCust);
			dto.setCustomerBasedIncentiveForSecretary(incentiveSec);
			dto.setAssignmentStatus(status);
			//	        dto.setAssignmentCreatedBy(loginUser.getSystemAdmin().getId());

			AssignmentDAO dao = new AssignmentDAO(tm.getConnection());

		    // 事前重複チェック
		    if (dao.existsDuplicate(dto)) {
		        validation.addErrorMsg("同月・同顧客・同秘書・同ランクのアサインは既に登録済みです。");
		        req.setAttribute("errorMsg", validation.getErrorMsg());
		        return req.getContextPath() + "/admin/assignment/register";
		    }
			dao.insert(dto);
			tm.commit();
			return req.getContextPath() + "/admin/assignment";

		} catch (RuntimeException e) {
			return req.getContextPath() + req.getServletPath() + "/error";
		}
	}
	
	
	public String assignmentPMRegisterDone() {
		String customerIdStr = req.getParameter("customerId");
		String secretaryIdStr = req.getParameter("secretaryId");
		String taskRankIdStr = req.getParameter("taskRankId");
		String targetYearMonth = req.getParameter("targetYearMonth");

		String basePayCustomerStr = req.getParameter("basePayCustomer");
		String basePaySecretaryStr = req.getParameter("basePaySecretary");
		String status = req.getParameter("status");

		// 必須
		validation.isNull("顧客", customerIdStr);
		validation.isNull("秘書", secretaryIdStr);
		validation.isNull("業務ランク", taskRankIdStr);
		validation.isNull("対象月", targetYearMonth);

		// UUID 形式
		if (!validation.isUuid(customerIdStr))
			validation.addErrorMsg("顧客の指定が不正です");
		if (!validation.isUuid(secretaryIdStr))
			validation.addErrorMsg("秘書の指定が不正です");
		if (!validation.isUuid(taskRankIdStr))
			validation.addErrorMsg("業務ランクの指定が不正です");

		// 年月(yyyy-MM)
		if (!validation.isYearMonth(targetYearMonth))
			validation.addErrorMsg("対象月は yyyy-MM 形式で入力してください");

		// 金額（0以上の整数。小数不要の要件に合わせる）
		validation.mustBeMoneyOrZero("単価（顧客）", basePayCustomerStr);
		validation.mustBeMoneyOrZero("単価（秘書）", basePaySecretaryStr);
		// エラーがあれば戻す（入力値も返す）
		if (validation.hasErrorMsg()) {
			req.setAttribute("errorMsg", validation.getErrorMsg());

			// 入力値を画面に戻す（name 属性と合わせる）
			req.setAttribute("form_customerId", customerIdStr);
			req.setAttribute("form_secretaryId", secretaryIdStr);
			req.setAttribute("form_taskRankId", taskRankIdStr);
			req.setAttribute("form_targetYearMonth", targetYearMonth);
			req.setAttribute("form_basePayCustomer", basePayCustomerStr);
			req.setAttribute("form_basePaySecretary", basePaySecretaryStr);
			req.setAttribute("form_status", status);
			// 画面再表示用のプルダウンデータ（customers/secretaries/taskRanks）が必要ならここで再取得して積む
			// CustomerDAO/SecretaryDAO/TaskRankDAO を使って再セットしてください。

			return "assignment/admin/pm_register";
		}

		HttpSession session = ((HttpServletRequest) req).getSession(false);
		LoginUser loginUser = (session == null) ? null : (LoginUser) session.getAttribute("loginUser");

		try (TransactionManager tm = new TransactionManager()) {
			UUID customerId = UUID.fromString(customerIdStr);
			UUID secretaryId = UUID.fromString(secretaryIdStr);
			UUID taskRankId = UUID.fromString(taskRankIdStr);

			BigDecimal basePayCustomer = new BigDecimal(basePayCustomerStr.replaceAll(",", ""));
			BigDecimal basePaySecretary = new BigDecimal(basePaySecretaryStr.replaceAll(",", ""));
			BigDecimal incPayCustomer = new BigDecimal(0);
			BigDecimal incPaySecretary = new BigDecimal(0);
			BigDecimal incentiveCust = new BigDecimal(0);
			BigDecimal incentiveSec =  new BigDecimal(0);

			AssignmentDTO dto = new AssignmentDTO();
			dto.setAssignmentCustomerId(customerId);
			dto.setAssignmentSecretaryId(secretaryId);
			dto.setTaskRankId(taskRankId);
			dto.setTargetYearMonth(targetYearMonth);
			dto.setBasePayCustomer(basePayCustomer);
			dto.setBasePaySecretary(basePaySecretary);
			dto.setIncreaseBasePayCustomer(incPayCustomer);
			dto.setIncreaseBasePaySecretary(incPaySecretary);
			dto.setCustomerBasedIncentiveForCustomer(incentiveCust);
			dto.setCustomerBasedIncentiveForSecretary(incentiveSec);
			dto.setAssignmentStatus(status);
			//	        dto.setAssignmentCreatedBy(loginUser.getSystemAdmin().getId());

			AssignmentDAO dao = new AssignmentDAO(tm.getConnection());

		    // 事前重複チェック
		    if (dao.existsDuplicate(dto)) {
		        validation.addErrorMsg("同月・同顧客・同秘書・同ランクのアサインは既に登録済みです。");
		        req.setAttribute("errorMsg", validation.getErrorMsg());
		        return req.getContextPath() + "/admin/assignment/pm_register";
		    }
			dao.insert(dto);
			tm.commit();
			return req.getContextPath() + "/admin/assignment";

		} catch (RuntimeException e) {
			e.printStackTrace();
			return req.getContextPath() + req.getServletPath() + "/error";
		}
	}
	
	// =========================================================
    // Helper（共通化メソッド）
    // =========================================================

    /** 金額入力の共通パーサー（カンマ除去・空/blankは0、trimあり）。 */
    private BigDecimal parseMoneyOrZero(String s) { // ★ CHANGED: 新規追加（カンマ対応）
        if (s == null) return BigDecimal.ZERO;
        String t = s.trim();
        if (t.isEmpty()) return BigDecimal.ZERO;
        t = t.replace(",", "");
        return new BigDecimal(t);
    }
	
	/** 通常登録画面のフォーム値を戻す（エラー時）。 */
    private void populateFormBackForRegister(
        String customerId, String secretaryId, String taskRankId, String ym,
        String baseCust, String baseSec, String incCust, String incSec,
        String incentCust, String incentSec, String status
    ) { // ★ CHANGED: 共通化
        req.setAttribute("errorMsg", validation.getErrorMsg());
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
	
	/** 画面プルダウン：秘書一覧（PMのみ／全件）を request に積む。 */
    private void loadSecretariesToRequest(TransactionManager tm, boolean pmOnly) { // ★ CHANGED: 共通化
        SecretaryDAO secretaryDAO = new SecretaryDAO(tm.getConnection());
        List<SecretaryDTO> secretaryDTOs = pmOnly ? secretaryDAO.selectAllPM() : secretaryDAO.selectAll();
        List<Secretary> secretaries = new ArrayList<>();
        for (SecretaryDTO dto : secretaryDTOs) {
            secretaries.add(conv.toDomain(dto));
        }
        req.setAttribute("secretaries", secretaries);
    }
	
	/** 画面プルダウン：タスクランク（全件）を request に積む。 */
    private void loadTaskRanksToRequest(TransactionManager tm) { // ★ CHANGED: 共通化
        TaskRankDAO taskRankDAO = new TaskRankDAO(tm.getConnection());
        List<TaskRankDTO> taskRankDTOs = taskRankDAO.selectAll();
        List<TaskRank> taskRanks = new ArrayList<>();
        for (TaskRankDTO dto : taskRankDTOs) {
            taskRanks.add(conv.toDomain(dto));
        }
        req.setAttribute("taskRanks", taskRanks);
    }
	
	/** 画面プルダウン：PM用タスクランク（rank_no = 0 想定）を request に積む。 */
    private void loadPMTaskRankToRequest(TransactionManager tm) { // ★ CHANGED: 共通化
        TaskRankDAO taskRankDAO = new TaskRankDAO(tm.getConnection());
        TaskRankDTO taskRankDTO = taskRankDAO.selectPM();
        TaskRank taskRank = (taskRankDTO != null) ? conv.toDomain(taskRankDTO) : null;
        req.setAttribute("taskRank", taskRank);
    }
	
	
	/** 文字列群から AssignmentDTO を構築する共通ビルダー。 */
    private AssignmentDTO buildAssignmentDto(
        String customerIdStr, String secretaryIdStr, String taskRankIdStr, String targetYearMonth,
        String basePayCustomerStr, String basePaySecretaryStr,
        String increaseBasePayCustomerStr, String increaseBasePaySecretaryStr,
        String incentiveCustomerStr, String incentiveSecretaryStr,
        String status
    ) { // ★ CHANGED: 新規追加（DTO組み立て共通化）
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
}
