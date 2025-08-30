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

    // ビュー名
    private static final String VIEW_HOME                = "assignment/admin/home";
    private static final String VIEW_REGISTER            = "assignment/admin/register";
    private static final String VIEW_REGISTER_CHECK      = "assignment/admin/register_check";
    private static final String VIEW_REGISTER_DONE       = "assignment/admin/register_done";
    private static final String VIEW_PM_REGISTER         = "assignment/admin/pm_register";
    private static final String VIEW_PM_REGISTER_CHECK   = "assignment/admin/pm_register_check";
    private static final String VIEW_PM_REGISTER_DONE    = "assignment/admin/pm_register_done";

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
			return VIEW_HOME;

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
    /** 入力画面の共通表示。 */
    private String showRegister(boolean pmMode) {
        String ym = req.getParameter(P_TARGET_YM);
        if (ym == null || ym.isBlank()) ym = LocalDate.now().format(YM_FMT);
        req.setAttribute("targetYm", ym);

        final String idParam = pmMode ? P_COMPANY_ID : P_ID;
        final String companyIdStr = req.getParameter(idParam);
        final String companyName  = req.getParameter(P_COMPANY_NAME);

        if (validation.isNull("会社名", companyName) || validation.isNull("会社ID", companyIdStr)) {
            return req.getContextPath() + "/admin/assignment";
        }

        try (TransactionManager tm = new TransactionManager()) {
            Customer customer = new Customer();
            customer.setId(UUID.fromString(companyIdStr));
            customer.setCompanyName(companyName);
            req.setAttribute("customer", customer);

            loadSecretariesToRequest(tm, pmMode);
            if (pmMode) {
                loadPMTaskRankToRequest(tm);
                return VIEW_PM_REGISTER;
            } else {
                loadTaskRanksToRequest(tm);
                return VIEW_REGISTER;
            }
        } catch (RuntimeException e) {
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // ========= 確認画面（POST） =========

    /** 通常登録の確認へ。入力値をそのまま積みます。 */
    public String assignmentRegisterCheck() {
        pushFormBackToRequestForCheck(false);
        return VIEW_REGISTER_CHECK;
    }

    /** PM登録の確認へ。入力値をそのまま積みます。 */
    public String assignmentPMRegisterCheck() {
        pushFormBackToRequestForCheck(true);
        return VIEW_PM_REGISTER_CHECK;
    }

    // ========= 完了（INSERT） =========

    /** 通常登録の完了（INSERT）。 */
    public String assignmentRegisterDone() {
        return handleInsert(false);
    }

    /** PM登録の完了（INSERT）。 */
    public String assignmentPMRegisterDone() {
        return handleInsert(true);
    }

    // ========= 内部処理（INSERT共通） =========

    private String handleInsert(boolean pmMode) {
        // 取得
        String customerIdStr = req.getParameter(P_CUSTOMER_ID);
        String secretaryIdStr = req.getParameter(P_SECRETARY_ID);
        String taskRankIdStr  = req.getParameter(P_TASK_RANK_ID);
        String ym             = req.getParameter(P_TARGET_YM);
        String baseCustStr    = req.getParameter(P_BASE_CUST);
        String baseSecStr     = req.getParameter(P_BASE_SEC);
        String incCustStr     = pmMode ? "0" : req.getParameter(P_INC_CUST);
        String incSecStr      = pmMode ? "0" : req.getParameter(P_INC_SEC);
        String incentCustStr  = pmMode ? "0" : req.getParameter(P_INCENT_CUST);
        String incentSecStr   = pmMode ? "0" : req.getParameter(P_INCENT_SEC);
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
        if (!pmMode) {
            validation.mustBeMoneyOrZero("増額（顧客）", incCustStr);
            validation.mustBeMoneyOrZero("増額（秘書）", incSecStr);
            if (!validation.isBlank(incentCustStr)) validation.mustBeMoneyOrZero("継続単価（顧客）", incentCustStr);
            if (!validation.isBlank(incentSecStr))  validation.mustBeMoneyOrZero("継続単価（秘書）", incentSecStr);
        }

        if (validation.hasErrorMsg()) {
            populateFormBackForRegister(customerIdStr, secretaryIdStr, taskRankIdStr, ym,
                    baseCustStr, baseSecStr, incCustStr, incSecStr, incentCustStr, incentSecStr, status);
            try (TransactionManager tm = new TransactionManager()) {
                loadSecretariesToRequest(tm, pmMode);
                if (pmMode) loadPMTaskRankToRequest(tm); else loadTaskRanksToRequest(tm);
            }
            return pmMode ? VIEW_PM_REGISTER : VIEW_REGISTER;
        }

        // DTO 組立
        AssignmentDTO dto = buildAssignmentDto(
                customerIdStr, secretaryIdStr, taskRankIdStr, ym,
                baseCustStr, baseSecStr, incCustStr, incSecStr, incentCustStr, incentSecStr, status);

        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());

            if (dao.existsDuplicate(dto)) {
                validation.addErrorMsg("同月・同顧客・同秘書・同ランクのアサインは既に登録済みです。");
                populateFormBackForRegister(customerIdStr, secretaryIdStr, taskRankIdStr, ym,
                        baseCustStr, baseSecStr, incCustStr, incSecStr, incentCustStr, incentSecStr, status);
                loadSecretariesToRequest(tm, pmMode);
                if (pmMode) loadPMTaskRankToRequest(tm); else loadTaskRanksToRequest(tm);
                return pmMode ? VIEW_PM_REGISTER : VIEW_REGISTER;
            }

            dao.insert(dto);
            tm.commit();
            return pmMode ? VIEW_PM_REGISTER_DONE : VIEW_REGISTER_DONE;

        } catch (RuntimeException e) {
            e.printStackTrace();
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
            String customerId, String secretaryId, String taskRankId, String ym,
            String baseCust, String baseSec, String incCust, String incSec,
            String incentCust, String incentSec, String status) {
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

    /** セレクトボックス用：秘書一覧を request へ（pmOnly=true なら PM のみ）。 */
    private void loadSecretariesToRequest(TransactionManager tm, boolean pmOnly) {
        SecretaryDAO secretaryDAO = new SecretaryDAO(tm.getConnection());
        List<SecretaryDTO> sDTOs = pmOnly ? secretaryDAO.selectAllPM() : secretaryDAO.selectAll();
        List<Secretary> list = new ArrayList<>();
        for (SecretaryDTO d : sDTOs) list.add(conv.toDomain(d));
        req.setAttribute("secretaries", list);
    }

    /** セレクトボックス用：全タスクランク一覧。 */
    private void loadTaskRanksToRequest(TransactionManager tm) {
        TaskRankDAO taskRankDAO = new TaskRankDAO(tm.getConnection());
        List<TaskRankDTO> tDTOs = taskRankDAO.selectAll();
        List<TaskRank> list = new ArrayList<>();
        for (TaskRankDTO d : tDTOs) list.add(conv.toDomain(d));
        req.setAttribute("taskRanks", list);
    }

    /** 単一（PM）タスクランク。 */
    private void loadPMTaskRankToRequest(TransactionManager tm) {
        TaskRankDAO taskRankDAO = new TaskRankDAO(tm.getConnection());
        TaskRankDTO d = taskRankDAO.selectPM();
        req.setAttribute("taskRank", d != null ? conv.toDomain(d) : null);
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
}