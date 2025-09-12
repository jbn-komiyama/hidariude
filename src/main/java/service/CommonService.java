package service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import dao.AssignmentDAO;
import dao.CustomerContactDAO;
import dao.CustomerDAO;
import dao.SecretaryDAO;
import dao.SystemAdminDAO;
import dao.TaskDAO;
import dao.TransactionManager;
import domain.Assignment;
import domain.Customer;
import domain.CustomerContact;
import domain.LoginUser;
import domain.Secretary;
import domain.SystemAdmin;
import domain.Task;
import dto.AssignmentDTO;
import dto.CustomerContactDTO;
import dto.CustomerDTO;
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

	// アトリビュート
	private static final String ATTR_LOGIN_USER = "loginUser";
	private static final String ATTR_CUSTOMERS = "customers";
	private static final String ATTR_YEAR_MONTH = "yearMonth";

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
		return "common/admin/home";
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

		// 2) 今月の "YYYY-MM"
		String yearMonth = LocalDate.now(Z_TOKYO).format(YM_FMT);
		String prevYearMonth = LocalDate.now(Z_TOKYO).minusMonths(1).format(YM_FMT); // ★先月

		// 3) DAO 呼び出し → DTO を Converter で Domain に詰め替え
		try (TransactionManager tm = new TransactionManager()) {
			AssignmentDAO dao = new AssignmentDAO(tm.getConnection());
			List<CustomerDTO> list = dao.selectBySecretaryAndMonthToCustomer(secretaryId, yearMonth);

			List<Customer> customers = new ArrayList<>();
			if (list != null) {
				for (CustomerDTO cdto : list) {
					// Customer 基本情報
					Customer c = conv.toDomain(cdto);

					// assignments を個別に詰め替え
					List<Assignment> as = new ArrayList<>();
					if (cdto.getAssignmentDTOs() != null) {
						for (AssignmentDTO adto : cdto.getAssignmentDTOs()) {
							as.add(conv.toDomain(adto));
						}
					}
					c.setAssignments(as);
					customers.add(c);
				}
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

			// 4) JSP へ渡す
			req.setAttribute(ATTR_CUSTOMERS, customers);
			return "common/secretary/home";
		} catch (RuntimeException e) {
			e.printStackTrace();
			return req.getContextPath() + req.getServletPath() + "/error";
		}
	}

	/**
	 * 顧客（会社/担当者）ログイン。
	 * A社の山田さん/田中さん、どちらでログインしても同じ「A社のホーム」へ。
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
	        // 担当者をIDで検索
	        CustomerContactDAO ccDao = new CustomerContactDAO(tm.getConnection());
	        CustomerContactDTO ccDto = ccDao.selectByMail(loginId);
	        CustomerContact cc = new CustomerContact();

	        if (ccDto != null && safeEquals(ccDto.getPassword(), password)) {
	            //Domain に詰め替え（customer以外全て）
	            cc.setId(ccDto.getId());
	            cc.setCustomerId(ccDto.getCustomerId());
	            cc.setMail(ccDto.getMail());
	            cc.setPassword(ccDto.getPassword());
	            cc.setName(ccDto.getName());
	            cc.setNameRuby(ccDto.getNameRuby());
	            cc.setPhone(ccDto.getPhone());
	            cc.setDepartment(ccDto.getDepartment());
	            cc.setPrimary(ccDto.isPrimary());
	            cc.setCreatedAt(ccDto.getCreatedAt());
	            cc.setUpdatedAt(ccDto.getUpdatedAt());
	            cc.setDeletedAt(ccDto.getDeletedAt());
	            cc.setLastLoginAt(ccDto.getLastLoginAt());
	        

	            // 会社情報を取得してドメインへ詰め替え（同一会社ユーザーで共通画面を見られるようにする）
	            CustomerDAO cDao = new CustomerDAO(tm.getConnection());                         
	            CustomerDTO cDto = cDao.selectByUUId(ccDto.getCustomerId());                      
	            Customer customer = null; 
	            
	            if (cDto != null) {   
	            	System.out.println("[login] companyName = " + cDto.getCompanyName());
	                customer = new Customer();                                                  
	                customer.setId(cDto.getId());                                              
	                customer.setCompanyCode(cDto.getCompanyCode());                              
	                customer.setCompanyName(cDto.getCompanyName());                              
	                customer.setMail(cDto.getMail());                                           
	                customer.setPhone(cDto.getPhone());                                         
	                customer.setPostalCode(cDto.getPostalCode());                                
	                customer.setAddress1(cDto.getAddress1());                                    
	                customer.setAddress2(cDto.getAddress2());                                    
	                customer.setBuilding(cDto.getBuilding());                                    
	                customer.setPrimaryContactId(cDto.getPrimaryContactId());                    
	                customer.setCreatedAt(cDto.getCreatedAt());                                  
	                customer.setUpdatedAt(cDto.getUpdatedAt());                                  
	                customer.setDeletedAt(cDto.getDeletedAt());  
	                System.out.print("[login] companyName = " + customer.getCompanyName());
	            }                                                                               

	            //セッションへ格納
	            LoginUser loginUser = new LoginUser();
	            cc.setCustomer(customer);  
	            loginUser.setCustomer(customer);
	            loginUser.setCustomerContact(cc);
	            loginUser.setAuthority(AUTH_CUSTOMER);                         
	            putLoginUserToSession(loginUser);
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
		// ここではビューだけ返す。会社IDはセッションの LoginUser から取り、JSP/別サービスで会社単位のデータ読み込みに利用
		return "common/customer/home";
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
}
