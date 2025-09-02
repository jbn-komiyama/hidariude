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
import dao.SecretaryDAO;
import dao.SystemAdminDAO;
import dao.TransactionManager;
import domain.Assignment;
import domain.Customer;
import domain.LoginUser;
import domain.Secretary;
import domain.SystemAdmin;
import dto.AssignmentDTO;
import dto.CustomerDTO;
import dto.SecretaryDTO;
import dto.SystemAdminDTO;


public class TaskService extends BaseService{
	
	// =========================================================
    // 定数／内部型
    // =========================================================

    /** 権限（従来の数値を定数化） */
    private static final int AUTH_ADMIN = 1;     // 保持していた数値と同じ
    private static final int AUTH_SECRETARY = 2; // 保持していた数値と同じ

    // リクエストパラメータ名
    private static final String P_LOGIN_ID = "loginId";
    private static final String P_PASSWORD = "password";

    // 画面パス
    private static final String PATH_SECRETARY_LOGIN = "/secretary";
    private static final String PATH_SECRETARY_HOME  = "/secretary/home";
    private static final String PATH_ADMIN_LOGIN     = "/admin";
    private static final String PATH_ADMIN_HOME      = "/admin/home";

    // アトリビュート
    private static final String ATTR_LOGIN_USER = "loginUser";
    private static final String ATTR_CUSTOMERS = "customers";
    private static final String ATTR_YEAR_MONTH = "yearMonth";
    
    private static final ZoneId Z_TOKYO = ZoneId.of("Asia/Tokyo");
    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    /** ロール（どの DAO/遷移系を使うかの分岐に利用） */
    private enum Role { ADMIN, SECRETARY } 
    
    private final Converter conv = new Converter();

	
	public TaskService(HttpServletRequest req, boolean useDB) {
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

                // 元実装どおり：Secretary ドメインは mail のみ保持
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
        	System.out.println("1");
            return req.getContextPath() + PATH_SECRETARY_LOGIN;
        }
        LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
        System.out.println(lu);
        if (lu == null || lu.getSecretary() == null || lu.getSecretary().getId() == null) {
        	System.out.println("2");
        	return req.getContextPath() + PATH_SECRETARY_LOGIN;
        }
        UUID secretaryId = lu.getSecretary().getId();

        // 2) 今月の "YYYY-MM"
        String yearMonth = LocalDate.now(Z_TOKYO).format(YM_FMT);

        // 3) DAO 呼び出し → DTO を Converter で Domain に詰め替え
        try (TransactionManager tm = new TransactionManager()) {
            AssignmentDAO dao = new AssignmentDAO(tm.getConnection());
            List<CustomerDTO> list = dao.selectBySecretaryAndMonth(secretaryId, yearMonth);

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

            // 4) JSP へ渡す
            req.setAttribute(ATTR_CUSTOMERS, customers);
            req.setAttribute(ATTR_YEAR_MONTH, yearMonth);
            System.out.println("3");
            return "common/secretary/home";
        } catch (RuntimeException e) {
            return req.getContextPath() + req.getServletPath() + "/error";
        }
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
