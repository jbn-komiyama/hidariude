package controller;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import domain.LoginUser;
import service.AssignmentService;
import service.CommonService;
import service.ContactService;
import service.CustomerService;
import service.InvoiceService;
import service.PasswordResetService;
import service.ProfileService;
import service.SalesCostSummaryService;
import service.SecretaryService;
import service.SystemAdminService;
import service.TaskService;

/**
 * フロントコントローラーのServlet実装クラス
 * すべてのリクエストを受け取り、ロールに応じて適切な処理に分岐します
 */
@WebServlet(urlPatterns={"/admin/*", "/secretary/*", "/customer/*"})
public class FrontController extends HttpServlet {
	/**
	 * シリアライズ用のバージョン番号
	 * HttpServletはSerializableインターフェースを実装しているため必要
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * アプリケーションのコンテキストパス
	 * 例) アプリケーションが "http://example.com/myapp/admin/home" で動作している場合
	 *     contextPath = "/myapp"
	 * 用途: リダイレクト時のURL構築に使用
	 */
	private static String contextPath;
	
	/**
	 * サーブレットのパス（ロールを識別するパス）
	 * 例) URL "http://example.com/myapp/admin/home" の場合
	 *     servletPath = "/admin"
	 * 取り得る値: "/admin", "/secretary", "/customer"
	 * 用途: どのロール（管理者/秘書/顧客）の処理かを判定するために使用
	 */
	private static String servletPath;
	
	/**
	 * パス情報（具体的な機能パス）
	 * 例) URL "http://example.com/myapp/admin/home" の場合
	 *     pathInfo = "/home"
	 * 例) URL "http://example.com/myapp/admin/login" の場合
	 *     pathInfo = "/login"
	 * nullの場合: ルートパスにアクセスした状態（例: /admin のみ）
	 * 用途: どの機能（ログイン/ホーム/編集など）を実行するかを判定するために使用
	 */
	private static String pathInfo;
	
	/**
	 * 次に遷移する画面のパス
	 * executeメソッド内で各処理メソッド（adminExecute, secretaryExecute等）によって設定される
	 * 
	 * 設定例:
	 * - "common/admin/login" → /WEB-INF/jsp/common/admin/login.jsp を表示（フォワード）
	 * - "/admin/home" → /admin/home へリダイレクト
	 * 
	 * 初期値: "index"（デフォルト値、通常は各処理で上書きされる）
	 * 用途: 処理完了後の画面遷移先を決定するために使用
	 */
	private static String nextPath;

	/**
	 * GETメソッド
	 * 
	 * @param req HTTPリクエスト
	 * @param res HTTPレスポンス
	 * @throws ServletException Servlet例外
	 * @throws IOException IO例外
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		execute(req, res);
	}

	/**
	 * POSTメソッド
	 * 
	 * @param req HTTPリクエスト
	 * @param res HTTPレスポンス
	 * @throws ServletException Servlet例外
	 * @throws IOException IO例外
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		execute(req, res);
	}
	
	/**
	 * executeメソッド
	 * リクエストを受け取り、ロールに応じて処理を分岐する
	 * 
	 * @param req HTTPリクエスト
	 * @param res HTTPレスポンス
	 * @throws ServletException Servlet例外
	 * @throws IOException IO例外
	 */
	protected void execute(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		contextPath = req.getContextPath();
		servletPath = req.getServletPath();
		pathInfo = req.getPathInfo();
		nextPath = "index";
		
		/**
		 * ========================================
		 * 【認証フロー：ステップ1】セッション情報の取得
		 * ========================================
		 * 既存のセッションがあるか確認し、ログインユーザー情報を取得
		 * falseを指定しているため、セッションが存在しない場合は新規作成しない
		 */
		HttpSession session = ((HttpServletRequest)req).getSession(false);
	    LoginUser loginUser = (session == null) ? null : (LoginUser) session.getAttribute("loginUser");
	    
	    /**
	     * ========================================
	     * 【認証フロー：ステップ2】認証不要パスの判定
	     * ========================================
	     * 以下のパスは認証チェックをスキップする（誰でもアクセス可能）：
	     * - ルートパス（pathInfo == null または "/"）: 例) /admin, /secretary, /customer
	     *   → 認証状態に関わらずログイン画面へ遷移
	     * - ログインパス（/login）: 例) /admin/login, /secretary/login
	     *   → ログイン処理を行うため認証不要
	     * - パスワードリセットパス（/password_reset/*）: 例) /admin/password_reset
	     *   → パスワードリセット処理のため認証不要
	     */
	    boolean isRootPath = (pathInfo == null) || pathInfo.isEmpty() || "/".equals(pathInfo);
        boolean isLoginPath = "/login".equals(pathInfo);
        boolean isPasswordResetPath = (pathInfo != null && pathInfo.startsWith("/password_reset"));
		
		/**
		 * ========================================
		 * 【認証フロー：ステップ3】ロール別の処理分岐
		 * ========================================
		 */
		switch(servletPath) {
			case "/admin"->{
				/**
				 * ========================================
				 * 【認証フロー：ステップ4】管理者権限の認証チェック
				 * ========================================
				 * ルートパス・ログインパス・パスワードリセット以外のパスにアクセスする場合：
				 * 1. セッションにログインユーザーが存在するかチェック
				 * 2. ユーザーの権限が管理者（authority == 1）かチェック
				 * 
				 * 認証NGの場合：
				 * → 管理者のルートパス（/admin）にリダイレクト
				 * → ルートパスは認証不要なので、自動的にログイン画面へ遷移
				 * 
				 * 認証OKの場合：
				 * → 以降の処理（adminExecute）を継続
				 */
		        if (!isRootPath && !isLoginPath && !isPasswordResetPath) {
		            boolean loggedIn = (loginUser != null && loginUser.getAuthority() == 1);
		            if (!loggedIn) {
		                /** 未認証または権限不一致：ログイン画面へリダイレクト */
		                res.sendRedirect(contextPath + "/admin");
		                return;
		            }
		        }
		        
		    	/**
		    	 * 【画面遷移：ルートパスの場合】
		    	 * pathInfo == null の場合はルートパス（例: /admin）にアクセスした状態
		    	 * → ログイン画面を表示（認証済みユーザーでも一旦ログイン画面を表示）
		    	 * 
		    	 * 【画面遷移：その他のパスの場合】
		    	 * pathInfo が存在する場合は具体的な機能パス（例: /admin/home）
		    	 * → 各機能の処理メソッド（adminExecute）を呼び出し
		    	 */
		    	if(pathInfo == null)  nextPath = "common/admin/login";
		    	else adminExecute(req, res);
			}
			case "/secretary"->{
				/**
				 * ========================================
				 * 【認証フロー：ステップ4】秘書権限の認証チェック
				 * ========================================
				 * ルートパス・ログインパス・パスワードリセット以外のパスにアクセスする場合：
				 * 1. セッションにログインユーザーが存在するかチェック
				 * 2. ユーザーの権限が秘書（authority == 2）かチェック
				 * 
				 * 認証NGの場合：
				 * → 秘書のルートパス（/secretary）にリダイレクト
				 * → ルートパスは認証不要なので、自動的にログイン画面へ遷移
				 * 
				 * 認証OKの場合：
				 * → 以降の処理（secretaryExecute）を継続
				 */
		        if (!isRootPath && !isLoginPath && !isPasswordResetPath) {
		            boolean loggedIn = (loginUser != null && loginUser.getAuthority() == 2);
		            if (!loggedIn) {
		                /** 未認証または権限不一致：ログイン画面へリダイレクト */
		                res.sendRedirect(contextPath + "/secretary");
		                return;
		            }
		        }
				
				/**
				 * 【画面遷移：ルートパスの場合】
				 * pathInfo == null の場合はルートパス（例: /secretary）にアクセスした状態
				 * → ログイン画面を表示
				 * 
				 * 【画面遷移：その他のパスの場合】
				 * pathInfo が存在する場合は具体的な機能パス（例: /secretary/home）
				 * → 各機能の処理メソッド（secretaryExecute）を呼び出し
				 */
				if(pathInfo == null)  nextPath = "common/secretary/login";
		    	else secretaryExecute(req, res);
			}
			case "/customer"->{
				/**
				 * ========================================
				 * 【認証フロー：ステップ4】顧客権限の認証チェック
				 * ========================================
				 * ルートパス・ログインパス・パスワードリセット以外のパスにアクセスする場合：
				 * 1. セッションにログインユーザーが存在するかチェック
				 * 2. ユーザーの権限が顧客（authority == 3）かチェック
				 * 
				 * 認証NGの場合：
				 * → 顧客のルートパス（/customer）にリダイレクト
				 * → ルートパスは認証不要なので、自動的にログイン画面へ遷移
				 * 
				 * 認証OKの場合：
				 * → 以降の処理（customerExecute）を継続
				 */
		        if (!isRootPath && !isLoginPath && !isPasswordResetPath) {
		            boolean loggedIn = (loginUser != null && loginUser.getAuthority() == 3);
		            if (!loggedIn) {
		                /** 未認証または権限不一致：ログイン画面へリダイレクト */
		                res.sendRedirect(contextPath + "/customer");
		                return;
		            }
		        }
				
				/**
				 * 【画面遷移：ルートパスの場合】
				 * pathInfo == null の場合はルートパス（例: /customer）にアクセスした状態
				 * → ログイン画面を表示
				 * 
				 * 【画面遷移：その他のパスの場合】
				 * pathInfo が存在する場合は具体的な機能パス（例: /customer/home）
				 * → 各機能の処理メソッド（customerExecute）を呼び出し
				 */
				if(pathInfo == null)  nextPath = "common/customer/login";
		    	else customerExecute(req, res);
			}
		}
		
		/**
		 * ========================================
		 * 【認証フロー：ステップ5】ページ遷移処理
		 * ========================================
		 */
		
		/**
		 * レスポンス確定チェック
		 * 既にリダイレクトやファイルダウンロードなどでレスポンスが確定している場合は、
		 * 以降の処理を実行しない（二重遷移を防ぐ）
		 */
		if (res.isCommitted()) {
		    /** ファイルDLやsendErrorでレスポンスが確定していたら何もしない */
		    return;
		}

		/**
		 * nextPath の妥当性チェック
		 * 各処理メソッドで nextPath が設定されなかった場合のフォールバック処理
		 */
		if (nextPath == null || nextPath.isEmpty()) {
			/** デフォルトのエラーページにリダイレクト */
			nextPath = contextPath + servletPath + "/error";
			res.sendRedirect(nextPath);
			return;
		}

		/**
		 * ページ遷移の実行
		 * - nextPath が "/" で始まる場合 → リダイレクト（別のURLへ転送）
		 *   例) "/admin/home" → http://domain/context/admin/home へリダイレクト
		 * - nextPath が "/" で始まらない場合 → フォワード（JSPへ転送）
		 *   例) "common/admin/login" → /WEB-INF/jsp/common/admin/login.jsp を表示
		 */
		char firstPath = nextPath.charAt(0);
		if(firstPath == '/') {
			/** 先頭がスラッシュだとリダイレクト */
			res.sendRedirect(res.encodeRedirectURL(nextPath));
		} else {
			/** JSPへフォワード（認証済み画面やログイン画面を表示） */
			RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/jsp/" + nextPath + ".jsp");
			rd.forward(req, res);
		}
	}
	
	/**
	 * adminExecuteメソッド
	 * admin権限ユーザの処理をするためのメソッド
	 * 
	 * 【注意】このメソッドは認証チェックを通過した後のみ呼び出されます
	 * - ルートパス（/admin）にアクセスした場合はこのメソッドは呼ばれず、ログイン画面を表示
	 * - 認証不要パス（/login, /password_reset/*）にアクセスした場合もこのメソッドが呼ばれます
	 * - その他のパスは認証チェックを通過した場合のみこのメソッドが呼ばれます
	 * 
	 * @param req HTTPリクエスト
	 * @param res HTTPレスポンス
	 * @throws ServletException Servlet例外
	 * @throws IOException IO例外
	 */
    protected void adminExecute(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    	switch(pathInfo){
			/**
			 * A01 共通
			 */
			case "/login"->{
				nextPath = new CommonService(req, true).adminLogin();
			}
			case "/logout"->{
				nextPath = new CommonService(req, false).adminLogout();
			}
			case "/home"->{
				nextPath = new CommonService(req, false).adminHome();
			}
			case "/error"->{
				nextPath = "common/admin/error";
			}

			/**
			 * A04 マイページ編集業務
			 * 
			 */
            case "/mypage/home"->{
                nextPath = new CommonService(req, true).adminMyPageHome();
            }
            case "/mypage/edit"->{
                nextPath = new CommonService(req, true).adminMyPageEdit();
            }
            case "/mypage/edit_check"->{
                nextPath = new CommonService(req, true).adminMyPageEditCheck();
            }
            case "/mypage/edit_done"->{
                nextPath = new CommonService(req, true).adminMyPageEditDone();
            }

			/**
			 * A01_93 パスワードリセット
			 */
			case "/password_reset"->{
				nextPath = new PasswordResetService(req, false).showResetRequestForm("admin");
			}
			case "/password_reset/request"->{
				nextPath = new PasswordResetService(req, true).processResetRequest("admin");
			}
			case "/password_reset/form"->{
				nextPath = new PasswordResetService(req, true).showResetForm("admin");
			}
			case "/password_reset/reset"->{
				nextPath = new PasswordResetService(req, true).processPasswordReset("admin");
			}


			/**
			 * A02 秘書管理業務
			 * 
			 */
			case "/secretary"->{
				nextPath = new SecretaryService(req, true).secretaryList();
			}
			case "/secretary/register"->{
				nextPath = new SecretaryService(req, true).secretaryRegister();
			}
			case "/secretary/register_check"->{
				nextPath = new SecretaryService(req, false).secretaryRegisterCheck();
			}
			case "/secretary/register_done"->{
				nextPath = new SecretaryService(req, true).secretaryRegisterDone();
			}
			case "/secretary/edit"->{
				nextPath = new SecretaryService(req, true).secretaryEdit();
			}
			case "/secretary/edit_check"->{
				nextPath = new SecretaryService(req, false).secretaryEditCheck();
			}
			case "/secretary/edit_done"->{
				nextPath = new SecretaryService(req, true).secretaryEditDone();
			}
			case "/secretary/delete"->{
				nextPath = new SecretaryService(req, true).secretaryDelete();
			}
			case "/secretary/detail"->{
				nextPath = new SecretaryService(req, true).secretaryDetail();
			}
			
			
			/**
			 * A03 顧客管理業務
			 * 
			 */
			case "/customer"->{
				nextPath = new CustomerService(req, true).customerList();
			}
			case "/customer/register"->{
				nextPath = new CustomerService(req, false).customerRegister();
			}
			case "/customer/register_check"->{
				nextPath = new CustomerService(req, true).customerRegisterCheck();
			}
			case "/customer/register_done"->{
				nextPath = new CustomerService(req, true).customerRegisterDone();
			}
			case "/customer/edit"->{
				nextPath = new CustomerService(req, true).customerEdit();
			}
			case "/customer/edit_check"->{
				nextPath = new CustomerService(req, true).customerEditCheck();
			}
			case "/customer/edit_done"->{
				nextPath = new CustomerService(req, true).customerEditDone();
			}
			case "/customer/delete"->{
				nextPath = new CustomerService(req, true).customerDelete();
			}
			case "/customer/detail"->{
				nextPath = new CustomerService(req, true).customerDetail();
			}
			
			/**
			 * A0X 顧客担当者関連業務
			 * 
			 */
			case "/contact"->{
				nextPath = new ContactService(req, true).contactList();
			}
			case "/contact/register"->{
				nextPath = new ContactService(req, false).contactRegister();
			}
			case "/contact/register_check"->{
				nextPath = new ContactService(req, true).contactRegisterCheck();
			}
			case "/contact/register_done"->{
				nextPath = new ContactService(req, true).contactRegisterDone();
			}
			case "/contact/edit"->{
				nextPath = new ContactService(req, true).contactEdit();
			}
			case "/contact/edit_check"->{
				nextPath = new ContactService(req, true).contactEditCheck();
			}
			case "/contact/edit_done"->{
				nextPath = new ContactService(req, true).contactEditDone();
			}
			case "/contact/delete"->{
				nextPath = new ContactService(req, true).contactDelete();
			}
			
			/**
			 * A04 アサイン管理業務
			 * 
			 */
			case "/assignment"->{
				nextPath = new AssignmentService(req, true).assignmentList();
			}
			case "/assignment/register"->{
				nextPath = new AssignmentService(req, true).assignmentRegister();
			}
			case "/assignment/register_check"->{
				nextPath = new AssignmentService(req, true).assignmentRegisterCheck();
			}
			case "/assignment/register_done"->{
				nextPath = new AssignmentService(req, true).assignmentRegisterDone();
			}
			case "/assignment/pm_register"->{
				nextPath = new AssignmentService(req, true).assignmentPMRegister();
			}
			case "/assignment/pm_register_check"->{
				nextPath = new AssignmentService(req, true).assignmentPMRegisterCheck();
			}
			case "/assignment/pm_register_done"->{
				nextPath = new AssignmentService(req, true).assignmentPMRegisterDone();
			}
			case "/assignment/carry_over_preview"->{
				nextPath = new AssignmentService(req, true).assignmentCarryOverPreview();
			}
			case "/assignment/carry_over_apply"->{
				nextPath = new AssignmentService(req, true).assignmentCarryOverApply();
			}
			case "/assignment/edit"->{
				nextPath = new AssignmentService(req, true).assignmentEditIncentiveForm();
			}
			case "/assignment/edit_update"->{
				nextPath = new AssignmentService(req, true).assignmentEditIncentiveUpdate();
			}
			case "/assignment/delete"->{
				nextPath = new AssignmentService(req, true).assignmentDelete();
			}
			
			/**
			 * A05 システム管理者管理業務
			 * 
			 */
			case "/system_admin"->{
				nextPath = new SystemAdminService(req, true).systemAdminList();
			}
			case "/system_admin/register"->{
				nextPath = new SystemAdminService(req, true).systemAdminRegister();
			}
			case "/system_admin/register_check"->{
				nextPath = new SystemAdminService(req, true).systemAdminRegisterCheck();
			}
			case "/system_admin/register_done"->{
				nextPath = new SystemAdminService(req, true).systemAdminRegisterDone();
			}
			case "/system_admin/edit"->{
				nextPath = new SystemAdminService(req, true).systemAdminEdit();
			}
			case "/system_admin/edit_check"->{
				nextPath = new SystemAdminService(req, true).systemAdminEditCheck();
			}
			case "/system_admin/edit_done"->{
				nextPath = new SystemAdminService(req, true).systemAdminEditDone();
			}
			case "/system_admin/delete"->{
				nextPath = new SystemAdminService(req, true).systemAdminDelete();
			}
			
			/**
			 * A05 タスク管理業務
			 * 
			 */
			case "/task/list_all"->{
				nextPath = new TaskService(req, true).adminTaskListAll();
			}
			case "/task/list_unapproved"->{
				nextPath = new TaskService(req, true).adminTaskListUnapproved();
			}
			case "/task/list_approved"->{
				nextPath = new TaskService(req, true).adminTaskListApproved();
			}
			case "/task/list_remanded"->{
				nextPath = new TaskService(req, true).adminTaskListRemanded();
			}
			case "/task/approve_bulk"->{
				nextPath = new TaskService(req, true).adminTaskApproveBulk();
			}
			case "/task/unapprove_bulk"->{
				nextPath = new TaskService(req, true).adminTaskUnapproveBulk();
			}
			case "/task/remand_done"->{
				nextPath = new TaskService(req, true).adminTaskRemandDone();
			}
			case "/task/alert"->{
				nextPath = new TaskService(req, true).adminTaskAlertList();
			}
			case "/task/alert_delete"->{
				nextPath = new TaskService(req, true).adminAlertDelete();
			}
			
			/**
			 * A06 売上・コストサマリー業務
			 * 
			 */
			case "/summary/costs"->{
				nextPath = new SalesCostSummaryService(req, true).costSummary();
			}
			case "/summary/sales"->{
				nextPath = new SalesCostSummaryService(req, true).salesSummary();
			}
			
			/**
			 * A07 請求業務
			 * 
			 */
			case "/invoice/sales"->{
				nextPath = new InvoiceService(req, true).adminInvoiceSummary();
			}
			case "/invoice/costs"->{
				nextPath = new InvoiceService(req, true).secretaryInvoiceSummary();
			}
			
			/**
			 * A08 マスタ管理業務
			 * 
			 */
			case "/master"->{
				nextPath = new CommonService(req, true).adminMasterList();
			}

			
			/**
			 * それ以外
			 * 
			 */
			default -> {
				/** 未定義のパスの場合はエラーページへ */
				nextPath = contextPath + "/admin/error";
			}
		}
    }
    
    
	/**
	 * secretaryExecuteメソッド
	 * 秘書ユーザの処理をするためのメソッド
	 * 
	 * 【注意】このメソッドは認証チェックを通過した後のみ呼び出されます
	 * - ルートパス（/secretary）にアクセスした場合はこのメソッドは呼ばれず、ログイン画面を表示
	 * - 認証不要パス（/login, /password_reset/*）にアクセスした場合もこのメソッドが呼ばれます
	 * - その他のパスは認証チェックを通過した場合のみこのメソッドが呼ばれます
	 * 
	 * @param req HTTPリクエスト
	 * @param res HTTPレスポンス
	 * @throws ServletException Servlet例外
	 * @throws IOException IO例外
	 */
    protected void secretaryExecute(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    	switch(pathInfo){
			/**
			 * A01 共通
			 */
			case "/login"->{
				nextPath = new CommonService(req, true).secretaryLogin();
			}
			case "/logout"->{
				nextPath = new CommonService(req, false).secretaryLogout();
			}
			case "/home"->{
				nextPath = new CommonService(req, true).secretaryHome();
			}
			case "/error"->{
				nextPath = "common/secretary/error";
			}
	
			/**
			 * パスワードリセット
			 */
			case "/password_reset"->{
				nextPath = new PasswordResetService(req, false).showResetRequestForm("secretary");
			}
			case "/password_reset/request"->{
				nextPath = new PasswordResetService(req, true).processResetRequest("secretary");
			}
			case "/password_reset/form"->{
				nextPath = new PasswordResetService(req, true).showResetForm("secretary");
			}
			case "/password_reset/reset"->{
				nextPath = new PasswordResetService(req, true).processPasswordReset("secretary");
			}

			/**
			 * A02 業務管理業務
			 * 
			 */
			case "/task/register"->{
				nextPath = new TaskService(req, true).taskRegister();
			}
			case "/task/register_done"->{
				nextPath = new TaskService(req, true).taskRegisterDone();
			}
			case "/task/edit"->{
				nextPath = new TaskService(req, true).taskEdit();
			}
			case "/task/edit_done"->{
				nextPath = new TaskService(req, true).taskEditDone();
			}
			case "/task/delete_done"->{
				nextPath = new TaskService(req, true).taskDeleteDone();
			}
			case "/task/list_all"->{
				nextPath = new TaskService(req, true).secretaryTaskListAll();
			}
			case "/task/list_approved"->{
				nextPath = new TaskService(req, true).secretaryTaskListApproved();
			}
			case "/task/list_unapproved"->{
				nextPath = new TaskService(req, true).secretaryTaskListUnapproved();
			}
			case "/task/list_remanded"->{
				nextPath = new TaskService(req, true).secretaryTaskListRemanded();
			}
			
			/**
			 * A04 マイページ編集業務
			 * 
			 */
			case "/mypage/home"->{
				nextPath = new SecretaryService(req, true).myPageList();
			}
			
			case "/mypage/edit"->{
				nextPath = new SecretaryService(req, true).myPageEdit();
			}
			case "/mypage/edit_check"->{
				nextPath = new SecretaryService(req, true).myPageEditCheck();
			}
			case "/mypage/edit_done"->{
				nextPath = new SecretaryService(req, true).myPageEditDone();
			}
			
			/**
			 * A05 請求業務
			 * 
			 */
			case "/invoice"->{
				nextPath = new InvoiceService(req, true).invoiceSummery();
			}
			case "/invoice/issue"->{
				new InvoiceService(req, true).issueInvoiceExcel(res);
				return;
			}
			
			/**
			 * A08 プロフィール業務
			 * 
			 */
			case "/profile" -> {
			    nextPath = new ProfileService(req, true).view();          /** 表示 */
			}
			case "/profile/register" -> {
			    nextPath = new ProfileService(req, true).register();      /** 登録フォーム */
			}
			case "/profile/register_done" -> {
			    nextPath = new ProfileService(req, true).registerDone();  /** 登録実行 */
			}
			case "/profile/edit" -> {
			    nextPath = new ProfileService(req, true).edit();          /** 変更フォーム */
			}
			case "/profile/edit_done" -> {
			    nextPath = new ProfileService(req, true).editDone();      /** 変更実行 */
			}

			/**
			 * それ以外
			 * 
			 */
			default -> {
				/** 未定義のパスの場合はエラーページへ */
				nextPath = contextPath + "/secretary/error";
			}
    	}
	}
    
	/**
	 * customerExecuteメソッド
	 * 顧客ユーザの処理をするためのメソッド
	 * 
	 * 【注意】このメソッドは認証チェックを通過した後のみ呼び出されます
	 * - ルートパス（/customer）にアクセスした場合はこのメソッドは呼ばれず、ログイン画面を表示
	 * - 認証不要パス（/login, /password_reset/*）にアクセスした場合もこのメソッドが呼ばれます
	 * - その他のパスは認証チェックを通過した場合のみこのメソッドが呼ばれます
	 * 
	 * @param req HTTPリクエスト
	 * @param res HTTPレスポンス
	 * @throws ServletException Servlet例外
	 * @throws IOException IO例外
	 */
    protected void customerExecute(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    	switch(pathInfo){
			/**
			 * A01 共通
			 */
			case "/login"->{
				nextPath = new CommonService(req, true).customerLogin();
			}
			case "/logout"->{
				nextPath = new CommonService(req, false).customerLogout();
			}
			case "/home"->{
				nextPath = new CommonService(req, true).customerHome();
			}
			case "/error"->{
				nextPath = "common/customer/error";
			}
	
			/**
			 * パスワードリセット
			 */
			case "/password_reset"->{
				nextPath = new PasswordResetService(req, false).showResetRequestForm("customer");
			}
			case "/password_reset/request"->{
				nextPath = new PasswordResetService(req, true).processResetRequest("customer");
			}
			case "/password_reset/form"->{
				nextPath = new PasswordResetService(req, true).showResetForm("customer");
			}
			case "/password_reset/reset"->{
				nextPath = new PasswordResetService(req, true).processPasswordReset("customer");
			}
			
			/**
			 * A02 顧客担当者管理業務
			 * 
			 */
			case "/contact/register"->{
				nextPath = new ContactService(req, false).contactRegister();
			}
			case "/contact/register_check"->{
				nextPath = new ContactService(req, true).contactRegisterCheck();
			}
			case "/contact/register_done"->{
				nextPath = new ContactService(req, true).contactRegisterDone();
			}
			case "/contact/edit"->{
				nextPath = new ContactService(req, true).contactEdit();
			}
			case "/contact/edit_check"->{
				nextPath = new ContactService(req, true).contactEditCheck();
			}
			case "/contact/edit_done"->{
				nextPath = new ContactService(req, true).contactEditDone();
			}
			case "/contact/delete"->{
				nextPath = new ContactService(req, true).contactDelete();
			}
			
			/**
			 * A03 請求サマリー業務
			 * 
			 */
			case "/invoice" -> {
			    nextPath = new InvoiceService(req, true).customerInvoiceSummary();
			}
			
			/**
			 * A04 顧客ページ（マイページ）編集業務
			 * 
			 */
			case "/mypage/home"->{
				nextPath = new ContactService(req, true).myPageList();
			}
			case "/mypage/edit"->{
			    nextPath = new ContactService(req, true).myPageEdit();
			}
			case "/mypage/edit_check"->{
			    nextPath = new ContactService(req, true).myPageEditCheck();
			}
			case "/mypage/edit_done"->{
			    nextPath = new ContactService(req, true).myPageEditDone();
			}

			/**
			 * A05 委託先業務
			 * 
			 */
			case "/assignment/list"->{
				nextPath = new AssignmentService(req, true).outsourceList();
			}
			case "/task/list" -> {
				nextPath = new TaskService(req, true).customerTaskList();
			}
			case "/task/alert" -> {
				nextPath = new TaskService(req, true).customerTaskAlert();
			}
			case "/assignment/profile"->{
				nextPath = new AssignmentService(req, true).secretaryProfile();
			}

			/**
			 * それ以外
			 * 
			 */
			default -> {
				/** 未定義のパスの場合はエラーページへ */
				nextPath = contextPath + "/customer/error";
			}

    	}
	}
}
