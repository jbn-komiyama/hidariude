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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

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

public class CommonService extends BaseService {

	// =========================================================
	// 定数／内部型
	// =========================================================

	/** 権限（従来の数値を定数化） */
	private static final int AUTH_ADMIN = 1;
	private static final int AUTH_SECRETARY = 2;
	private static final int AUTH_CUSTOMER = 3;

	// リクエストパラメータ名
	private static final String P_LOGIN_ID = "loginId";
	private static final String P_PASSWORD = "password";

	// 画面パス
	private static final String PATH_SECRETARY_LOGIN = "/secretary";
	private static final String PATH_SECRETARY_HOME = "/secretary/home";
	private static final String PATH_ADMIN_LOGIN = "/admin";
	private static final String PATH_ADMIN_HOME = "/admin/home";
	private static final String PATH_CUSTOMER_LOGIN = "/customer";
	private static final String PATH_CUSTOMER_HOME = "/customer/home";
	private static final String PATH_ADMIN_MYPAGE = "common/admin/mypage";
	private static final String PATH_ADMIN_ID_EDIT = "common/admin/id_edit";

	// アトリビュート
	private static final String ATTR_LOGIN_USER = "loginUser";
	private static final String ATTR_ADMIN = "admin";
	private static final String ATTR_FORM = "form";

	private static final ZoneId Z_TOKYO = ZoneId.of("Asia/Tokyo");
	private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

	/** ロール（どの DAO/遷移系を使うかの分岐に利用） */
	private enum Role {
		ADMIN, SECRETARY,CUSTOMER
	}

	private final Converter conv = new Converter();

	public CommonService(HttpServletRequest req, boolean useDB) {
		super(req, useDB);
	}

	// =========================================================
	// Public APIs
	// =========================================================

	/**
	 * 秘書ログイン。
	 * @return 遷移先
	 */
	public String secretaryLogin() { // ★ REVERT: Role/共通ハンドラを廃止し、個別実装に戻す
		final String loginId = req.getParameter(P_LOGIN_ID);
		final String password = req.getParameter(P_PASSWORD);

		// 必須チェック（元実装どおり）
		validation.isNull("ログインID", loginId);
		validation.isNull("パスワード", password);

		if (validation.hasErrorMsg()) {
			req.setAttribute("errorMsg", validation.getErrorMsg());
			return req.getContextPath() + PATH_SECRETARY_LOGIN;
		}

		try (TransactionManager tm = new TransactionManager()) {
			SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
			SecretaryDTO dto = dao.selectByMail(loginId);
			

			if (dto != null && safeEquals(dto.getPassword(), password)) {
				LoginUser loginUser = new LoginUser();

				Secretary sec = new Secretary();
				sec.setId(dto.getId());
				sec.setMail(dto.getMail());
				sec.setName(dto.getName());

				loginUser.setSecretary(sec);
				loginUser.setAuthority(AUTH_SECRETARY);

				putLoginUserToSession(loginUser);
				return req.getContextPath() + PATH_SECRETARY_HOME;
			} else {
				validation.addErrorMsg("正しいログインIDとパスワードを入力してください。");
				req.setAttribute("errorMsg", validation.getErrorMsg());
				return req.getContextPath() + PATH_SECRETARY_LOGIN;
			}
		} catch (RuntimeException e) {
			return req.getContextPath() + req.getServletPath() + "/error";
		}
	}

	/**
	 * 管理者ログイン。
	 * @return 遷移先
	 */
	public String adminLogin() { // ★ REVERT: Role/共通ハンドラを廃止し、個別実装に戻す
		final String loginId = req.getParameter(P_LOGIN_ID);
		final String password = req.getParameter(P_PASSWORD);

		// 必須チェック（元実装どおり）
		validation.isNull("ログインID", loginId);
		validation.isNull("パスワード", password);

		if (validation.hasErrorMsg()) {
			req.setAttribute("errorMsg", validation.getErrorMsg());
			return req.getContextPath() + PATH_ADMIN_LOGIN;
		}

		try (TransactionManager tm = new TransactionManager()) {
			SystemAdminDAO dao = new SystemAdminDAO(tm.getConnection());
			SystemAdminDTO dto = dao.selectByMail(loginId);

			if (dto != null && safeEquals(dto.getPassword(), password)) {
				LoginUser loginUser = new LoginUser();

				// 元実装どおり：SystemAdmin の詳細を格納
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
			} else {
				validation.addErrorMsg("正しいログインIDとパスワードを入力してください。");
				req.setAttribute("errorMsg", validation.getErrorMsg());
				return req.getContextPath() + PATH_ADMIN_LOGIN;
			}
		} catch (RuntimeException e) {
			return req.getContextPath() + req.getServletPath() + "/error";
		}
	}

	/** 管理者ホーム */
	public String adminHome() {
	    // ログイン確認（名前表示に使うだけ）
	    HttpSession session = req.getSession(false);
	    String adminName = "";
	    if (session != null) {
	        LoginUser lu = (LoginUser) session.getAttribute("loginUser");
	        if (lu != null && lu.getSystemAdmin() != null) {
	            adminName = lu.getSystemAdmin().getName();
	        }
	    }

	    // 今月 / 先月
	    String yearMonth      = LocalDate.now(Z_TOKYO).format(YM_FMT);
	    String prevYearMonth  = LocalDate.now(Z_TOKYO).minusMonths(1).format(YM_FMT);

	    try (TransactionManager tm = new TransactionManager()) {
	        TaskDAO tdao = new TaskDAO(tm.getConnection());

	        // 今月
	        TaskDTO tdThis = tdao.selectCountsForAdminMonth(yearMonth);
	        Task tThis = new Task();
	        tThis.setUnapproved(tdThis.getUnapproved());
	        tThis.setApproved(tdThis.getApproved());
	        tThis.setRemanded(tdThis.getRemanded());
	        tThis.setTotal(tdThis.getTotal());
	        tThis.setSumAmountApproved(tdThis.getTotalAmountApproved()); // 合計金額（承認済み）

	        // 先月
	        TaskDTO tdPrev = tdao.selectCountsForAdminMonth(prevYearMonth);
	        Task tPrev = new Task();
	        tPrev.setUnapproved(tdPrev.getUnapproved());
	        tPrev.setApproved(tdPrev.getApproved());
	        tPrev.setRemanded(tdPrev.getRemanded());
	        tPrev.setTotal(tdPrev.getTotal());
	        tPrev.setSumAmountApproved(tdPrev.getTotalAmountApproved());

	        // JSPへ
	        req.setAttribute("task", tThis);
	        req.setAttribute("taskPrev", tPrev);
	        req.setAttribute("yearMonth", yearMonth);
	        req.setAttribute("prevYearMonth", prevYearMonth);
	        req.setAttribute("adminName", adminName);

	        return "common/admin/home";
	    } catch (RuntimeException e) {
	        e.printStackTrace();
	        return req.getContextPath() + req.getServletPath() + "/error";
	    }
	}
	
	// --- マイページ表示 ---
	public String adminMyPage() {
	    // ログインチェック
	    HttpSession session = req.getSession(false);
	    if (session == null) return req.getContextPath() + PATH_ADMIN_LOGIN; // 既存の定数
	    LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
	    if (lu == null || lu.getSystemAdmin() == null || lu.getSystemAdmin().getId() == null) {
	        return req.getContextPath() + PATH_ADMIN_LOGIN;
	    }

	    // そのまま Domain を JSP に渡す
	    req.setAttribute(ATTR_ADMIN, lu.getSystemAdmin());
	    return PATH_ADMIN_MYPAGE;
	}

	// --- 編集フォーム表示（GET） ---
	public String adminIdEditForm() {
	    HttpSession session = req.getSession(false);
	    if (session == null) return req.getContextPath() + PATH_ADMIN_LOGIN;
	    LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
	    if (lu == null || lu.getSystemAdmin() == null || lu.getSystemAdmin().getId() == null) {
	        return req.getContextPath() + PATH_ADMIN_LOGIN;
	    }

	    // 既存値をフォーム初期値へ
	    Map<String, String> form = new LinkedHashMap<>();
	    form.put("mail", lu.getSystemAdmin().getMail());
	    form.put("name", lu.getSystemAdmin().getName());
	    form.put("nameRuby", lu.getSystemAdmin().getNameRuby());
	    req.setAttribute(ATTR_FORM, form);

	    return PATH_ADMIN_ID_EDIT;
	}

	// --- 編集送信（POST） ---
	public String adminIdEditSubmit() {
	    HttpSession session = req.getSession(false);
	    if (session == null) return req.getContextPath() + PATH_ADMIN_LOGIN;
	    LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
	    if (lu == null || lu.getSystemAdmin() == null || lu.getSystemAdmin().getId() == null) {
	        return req.getContextPath() + PATH_ADMIN_LOGIN;
	    }

	    UUID adminId = lu.getSystemAdmin().getId();

	    // 入力取得
	    String mail = req.getParameter("mail");
	    String password = req.getParameter("password"); // 空なら据え置き
	    String name = req.getParameter("name");
	    String nameRuby = req.getParameter("nameRuby");

	    // 最低限のバリデーション
	    validation.isNull("メールアドレス", mail);
	    validation.isNull("氏名", name);

	    // フォーム値（エラー時の戻し用）
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

	        // 自分以外でメール重複が無いか
	        if (dao.mailExistsExceptId(mail, adminId)) {
	            req.setAttribute("errorMsg", "入力されたメールアドレスは既に使用されています。");
	            return PATH_ADMIN_ID_EDIT;
	        }

	        // 既存情報取得
	        SystemAdminDTO current = dao.selectById(adminId);
	        if (current.getId() == null) {
	            req.setAttribute("errorMsg", "対象の管理者が見つかりません。");
	            return PATH_ADMIN_ID_EDIT;
	        }

	        // 更新DTO作成（パスワード空なら据え置き）
	        SystemAdminDTO upd = new SystemAdminDTO();
	        upd.setId(adminId);
	        upd.setMail(mail);
	        upd.setPassword( (password == null || password.isBlank()) ? current.getPassword() : password );
	        upd.setName(name);
	        upd.setNameRuby(nameRuby);

	        int cnt = dao.update(upd);
	        if (cnt != 1) {
	            req.setAttribute("errorMsg", "更新に失敗しました。");
	            return PATH_ADMIN_ID_EDIT;
	        }

	        tm.commit();

	        // セッション内の Domain も更新
	        lu.getSystemAdmin().setMail(mail);
	        lu.getSystemAdmin().setName(name);
	        lu.getSystemAdmin().setNameRuby(nameRuby);
	        if (password != null && !password.isBlank()) {
	            lu.getSystemAdmin().setPassword(password);
	        }
	        session.setAttribute(ATTR_LOGIN_USER, lu);

	        // マイページへ遷移（成功メッセージ）
	        req.setAttribute("successMsg", "アカウント情報を更新しました。");
	        req.setAttribute(ATTR_ADMIN, lu.getSystemAdmin());
	        return req.getContextPath() + "/admin/mypage";

	    } catch (RuntimeException e) {
	        e.printStackTrace();
	        req.setAttribute("errorMsg", "予期せぬエラーが発生しました。");
	        return PATH_ADMIN_ID_EDIT;
	    }
	}

	public String secretaryHome() {
		// 1) セッションから secretaryId を取得
		HttpSession session = req.getSession(false);
		if (session == null) {
			return req.getContextPath() + PATH_SECRETARY_LOGIN;
		}
		LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
		if (lu == null || lu.getSecretary() == null || lu.getSecretary().getId() == null) {
			return req.getContextPath() + PATH_SECRETARY_LOGIN;
		}
		UUID secretaryId = lu.getSecretary().getId();
		String secretaryName = lu.getSecretary().getName();

		// 2) 今月の "YYYY-MM"
		String yearMonth = LocalDate.now(Z_TOKYO).format(YM_FMT);
		String prevYearMonth = LocalDate.now(Z_TOKYO).minusMonths(1).format(YM_FMT); 

		// 3) DAO 呼び出し → DTO を Converter で Domain に詰め替え
		try (TransactionManager tm = new TransactionManager()) {

			// ★ ここから：フラットな行を作る（顧客で束ねない表示用）
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
	            row.put("company", a.getCustomerCompanyName()); // 顧客
	            row.put("rank",   a.getTaskRankName());         // タスクランク
	            row.put("base",   base);                        // 基本単価
	            row.put("incRank",incRank);                     // 増額(ランク)
	            row.put("incCont",incCont);                     // 増額(継続)
	            row.put("total",  total);                       // 合計単価
	            assignRows.add(row);
	        }
	        
			
			TaskDAO tdao = new TaskDAO(tm.getConnection());
			// 今月
	        TaskDTO tdto = tdao.selectCountsForSecretaryMonth(secretaryId, yearMonth);
	        Task task = new Task();
	        task.setUnapproved(tdto.getUnapproved());
	        task.setApproved(tdto.getApproved());
	        task.setRemanded(tdto.getRemanded());
	        task.setTotal(tdto.getTotal());
	        task.setSumAmountApproved(tdto.getTotalAmountApproved());   // ★合計金額（承認済）

	        // ★先月
	        TaskDTO tdtoPrev = tdao.selectCountsForSecretaryMonth(secretaryId, prevYearMonth);
	        Task taskPrev = new Task();
	        taskPrev.setUnapproved(tdtoPrev.getUnapproved());
	        taskPrev.setApproved(tdtoPrev.getApproved());
	        taskPrev.setRemanded(tdtoPrev.getRemanded());
	        taskPrev.setTotal(tdtoPrev.getTotal());
	        taskPrev.setSumAmountApproved(tdtoPrev.getTotalAmountApproved());

	        // JSP へ
	        req.setAttribute("task", task);                 // 今月
	        req.setAttribute("taskPrev", taskPrev);         // ★先月
	        req.setAttribute("yearMonth", yearMonth);
	        req.setAttribute("prevYearMonth", prevYearMonth); // ★先月のYYYY-MM
	        req.setAttribute("secretaryName", secretaryName);

			// 4) JSP へ渡す
//			req.setAttribute(ATTR_CUSTOMERS, customers);
	        req.setAttribute("assignRows", assignRows); // ★ 新規：フラット行
			return "common/secretary/home";
		} catch (RuntimeException e) {
			e.printStackTrace();
			return req.getContextPath() + req.getServletPath() + "/error";
		}
	}

	/**
	 * 顧客ログイン。
	 * セッションに loginUser.customer（会社） と loginUser.customerContact（担当者）を積む。
	 * @return 遷移先
	 */
	public String customerLogin() {
	    final String loginId  = req.getParameter(P_LOGIN_ID);
	    final String password = req.getParameter(P_PASSWORD);

	    // 必須チェック
	    validation.isNull("ログインID", loginId);
	    validation.isNull("パスワード", password);
	    if (validation.hasErrorMsg()) {
	        req.setAttribute("errorMsg", validation.getErrorMsg());
	        return req.getContextPath() + PATH_CUSTOMER_LOGIN;
	    }

	    try (TransactionManager tm = new TransactionManager()) {
	        CustomerContactDAO ccDao = new CustomerContactDAO(tm.getConnection());
	        CustomerDAO cDao         = new CustomerDAO(tm.getConnection());
	        Converter conv           = new Converter();

	        // ログインID=mail で担当者取得
	        CustomerContactDTO ccDto = ccDao.selectByMail(loginId);

	        // パスワード照合
	        if (ccDto != null && safeEquals(ccDto.getPassword(), password)) {

	            // 担当者Domain
	            CustomerContact cc = conv.toDomain(ccDto);

	            // 顧客IDを確実に取得（DTO直下 or FKフィールド）
	            UUID customerId =
	                (ccDto.getCustomerDTO() != null && ccDto.getCustomerDTO().getId() != null)
	                    ? ccDto.getCustomerDTO().getId()
	                    : cc.getCustomerId();
	            if (customerId == null) {
	                validation.addErrorMsg("担当者に紐づく会社情報が見つかりません。");
	                req.setAttribute("errorMsg", validation.getErrorMsg());
	                return req.getContextPath() + PATH_CUSTOMER_LOGIN;
	            }

	            // 会社Domain（会社名をJSPに出すため必須）
	            CustomerDTO cDto = cDao.selectByUUId(customerId);
	            Customer customer = conv.toDomain(cDto);

	            // セッションへ格納（JSPは sessionScope.loginUser.customer / customerContact を参照）
	            LoginUser loginUser = new LoginUser();
	            loginUser.setCustomer(customer);
	            loginUser.setCustomerContact(cc);
	            loginUser.setAuthority(AUTH_CUSTOMER);
	            putLoginUserToSession(loginUser);

	            // 顧客ホームへ（例：/customer/home → 顧客ホームJSPにフォワード）
	            return req.getContextPath() + PATH_CUSTOMER_HOME;
	        } else {
	            validation.addErrorMsg("正しいログインIDとパスワードを入力してください。");
	            req.setAttribute("errorMsg", validation.getErrorMsg());
	            return req.getContextPath() + PATH_CUSTOMER_LOGIN;
	        }
	    } catch (RuntimeException e) {
	        return req.getContextPath() + req.getServletPath() + "/error";
	    }
	}


	/** 顧客ホーム（会社単位で同一の画面を表示） */
	public String customerHome() {

	    // ログイン確認
	    HttpSession session = req.getSession(false);
	    if (session == null) return req.getContextPath() + PATH_CUSTOMER_LOGIN;
	    LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
	    if (lu == null || lu.getCustomer() == null || lu.getCustomer().getId() == null)
	        return req.getContextPath() + PATH_CUSTOMER_LOGIN;

	    UUID customerId = lu.getCustomer().getId();

	    // 今月と過去3か月
	    YearMonth ymNow   = YearMonth.now(Z_TOKYO);
	    YearMonth ymPrev1 = ymNow.minusMonths(1);
	    YearMonth ymPrev2 = ymNow.minusMonths(2);
	    YearMonth ymPrev3 = ymNow.minusMonths(3);

	    // JSP 用（月数値/文字列）
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

	        // ★ 未承認件数は work_date 基準で毎月タスクを読み、approvedAt==null をカウント
	        MonthStat statNow   = loadCustomerMonthStatByWorkDate(customerId, ymNow,   invDao);
	        MonthStat statPrev1 = loadCustomerMonthStatByWorkDate(customerId, ymPrev1, invDao);
	        MonthStat statPrev2 = loadCustomerMonthStatByWorkDate(customerId, ymPrev2, invDao);
	        MonthStat statPrev3 = loadCustomerMonthStatByWorkDate(customerId, ymPrev3, invDao);

	        // ★ 金額合計：今月/先月は InvoiceDAO の fee を合算（work_date基準）
	        statNow.setTotal(sumFee(invDao, customerId, ymNow));
	        statPrev1.setTotal(sumFee(invDao, customerId, ymPrev1));

	        // ★ 2か月前/3か月前は確定テーブル（必要あれば同様に差し替え可）
	        BigDecimal amt2 = cmiDao.selectTotalAmountByCustomerAndMonth(customerId, ymPrev2.format(YM_FMT));
	        BigDecimal amt3 = cmiDao.selectTotalAmountByCustomerAndMonth(customerId, ymPrev3.format(YM_FMT));
	        statPrev2.setTotal(amt2 != null ? amt2 : BigDecimal.ZERO);
	        statPrev3.setTotal(amt3 != null ? amt3 : BigDecimal.ZERO);

	        // JSP へ
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

	/** work_date 基準でその月の未承認件数を数える（approvedAt==null） */
	private MonthStat loadCustomerMonthStatByWorkDate(UUID customerId, YearMonth ym, InvoiceDAO invDao) {
	    final String ymStr = ym.format(YM_FMT);
	    MonthStat s = new MonthStat();
	    s.setYm(ymStr);

	    List<TaskDTO> tasks = invDao.selectTasksByMonthAndCustomer(customerId, ymStr);
	    int unapproved = 0;
	    if (tasks != null) {
	        for (TaskDTO t : tasks) {
	            if (t.getApprovedAt() == null) unapproved++;
	        }
	    }
	    s.setUnapproved(unapproved);
	    s.setTotal(null); // 金額は呼び出し側で設定
	    return s;
	}

	/** work_date 基準でその月の fee を合算 */
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





	// =========================================================
	// Small helpers
	// =========================================================

	/** セッションへ LoginUser を格納（セッション新規作成あり）。 */
	private void putLoginUserToSession(LoginUser loginUser) {
		HttpSession session = req.getSession(true);
		session.setAttribute(ATTR_LOGIN_USER, loginUser);
	}

	/** null セーフな equals。 */
	private boolean safeEquals(String a, String b) {
		return (a == null) ? (b == null) : a.equals(b);
	}
	
	private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

	// 表示用の小さなDTO（クラス内の重複を排除して1つに）
	public static class MonthStat {
	    private String ym;
	    private Integer unapproved;
	    private BigDecimal total;
	    public String getYm(){return ym;} public void setYm(String ym){this.ym=ym;}
	    public Integer getUnapproved(){return unapproved;} public void setUnapproved(Integer u){this.unapproved=u;}
	    public BigDecimal getTotal(){return total;} public void setTotal(BigDecimal t){this.total=t;}
	}

	// 未承認件数だけ Task から取る（金額は呼び出し側で設定）
	private MonthStat loadCustomerMonthStat(UUID customerId, YearMonth ym, TaskDAO tdao) {
	    final String ymStr = ym.format(YM_FMT);
	    MonthStat s = new MonthStat();
	    s.setYm(ymStr);

	    TaskDTO dto = tdao.selectCountsForCustomerMonth(customerId, ymStr);
	    s.setUnapproved(dto != null ? dto.getUnapproved() : 0);
	    s.setTotal(null);
	    return s;
	}



	
	
	
}

