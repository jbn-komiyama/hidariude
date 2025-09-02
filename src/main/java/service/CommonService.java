package service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import dao.SecretaryDAO;
import dao.SystemAdminDAO;
import dao.TransactionManager;
import domain.LoginUser;
import domain.Secretary;
import domain.SystemAdmin;
import dto.SecretaryDTO;
import dto.SystemAdminDTO;


public class CommonService extends BaseService{
	
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
    private static final String PATH_SECRETARY_HOME  = "/secretary/home";
    private static final String PATH_ADMIN_LOGIN     = "/admin";
    private static final String PATH_ADMIN_HOME      = "/admin/home";
    private static final String PATH_CUSTOMER_LOGIN = "/customer";
    private static final String PATH_CUSTOMER_HOME  = "/customer/home";

    // セッションキー
    private static final String ATTR_LOGIN_USER = "loginUser";

    /** ロール（どの DAO/遷移系を使うかの分岐に利用） */
    private enum Role { ADMIN, SECRETARY } 

	
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
	  
	  /** 秘書ホーム */
	    public String secretaryHome() {
	        return "common/secretary/home";
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

    /**
     * 顧客（会社/担当者）ログイン。
     * A社の山田さん/田中さん、どちらでログインしても同じ「A社のホーム」へ。
     */
//    public String customerLogin() {
//        final String loginId  = req.getParameter(P_LOGIN_ID);
//        final String password = req.getParameter(P_PASSWORD);
//
//        // 必須チェック
//        validation.isNull("ログインID", loginId);
//        validation.isNull("パスワード", password);
//        if (validation.hasErrorMsg()) {
//            req.setAttribute("errorMsg", validation.getErrorMsg());
//            return req.getContextPath() + PATH_CUSTOMER_LOGIN;
//        }
//
//        try (TransactionManager tm = new TransactionManager()) {
//
//            // ★ 担当者（社員）をメールで検索
//            //    ※ あなたの実装に合わせて DAO/メソッド名を置き換えてください
//        	CustomerContactDAO userDao = new CustomerContactDAO(tm.getConnection());
//        	CustomerContactDTO userDto = userDao.selectById(loginId);
//
//            if (userDto != null && safeEquals(userDto.getPassword(), password)) {
//
//                // ★ 会社情報を取得
//                CustomerDAO  cdao  = new CustomerDAO(tm.getConnection());
//                CustomerDTO  cdto  = cdao.selectByUUId(userDto.getCustomerId());
//                if (cdto == null) {
//                    validation.addErrorMsg("会社アカウントが見つかりません。");
//                    req.setAttribute("errorMsg", validation.getErrorMsg());
//                    return req.getContextPath() + PATH_CUSTOMER_LOGIN;
//                }
//
//                // --- セッションへ格納（誰がログインしても同じ会社を見るため、company を主として持つ）
//                LoginUser loginUser = new LoginUser();
//
//                // 最低限の会社情報（必要に応じて拡張）
//                Customer company = new Customer();
//                company.setId(cdto.getId());
//                company.setCompanyName(cdto.getCompanyName());
//                // company.set...（必要なら他項目）
//
//                // 担当者（表示用に必要なら）
//                CustomerUser person = new CustomerUser();
//                person.setId(userDto.getId());
//                person.setMail(userDto.getMail());
//                person.setName(userDto.getName());
//                // person.set...（必要なら他項目）
//
//                // ★ LoginUser へ設定（無ければ setter を追加してください）
//                loginUser.setCustomer(company);
//                loginUser.setCustomerUser(person);
//                loginUser.setAuthority(AUTH_CUSTOMER);
//
//                putLoginUserToSession(loginUser);
//                return req.getContextPath() + PATH_CUSTOMER_HOME;
//
//            } else {
//                validation.addErrorMsg("正しいログインIDとパスワードを入力してください。");
//                req.setAttribute("errorMsg", validation.getErrorMsg());
//                return req.getContextPath() + PATH_CUSTOMER_LOGIN;
//            }
//
//        } catch (RuntimeException e) {
//            return req.getContextPath() + req.getServletPath() + "/error";
//        }
//    }

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
