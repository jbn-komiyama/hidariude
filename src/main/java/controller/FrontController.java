package controller;

import java.io.IOException;
import java.sql.SQLException;

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
import service.ProfileService;
import service.SalesCostSummaryService;
import service.SecretaryService;
import service.SystemAdminService;
import service.TaskService;

/**
 * Servlet implementation class FrontController
 */
@WebServlet(urlPatterns={"/admin/*", "/secretary/*", "/customer/*"})
public class FrontController extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static String contextPath;
	private static String servletPath;
	private static String pathInfo;
	private static String nextPath;
	
	/**
	 * adminExecuteメソッド
	 * admin権限ユーザの処理をするためのメソッド
	 * @param req
	 * @param res
	 * @throws ServletException
	 * @throws IOException
	 * @throws SQLException
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
	case "/mypage"->{
		nextPath = new CommonService(req, false).adminMyPage();
	}
	case "/id_edit"->{
		nextPath = new CommonService(req, false).adminIdEditForm();
	}
	case "/id_edit_done"->{
		nextPath = new CommonService(req, false).adminIdEditSubmit();
	}
	case "/error"->{
		nextPath = "common/admin/error";
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
		
		
		
		
		case "/summary/costs"->{
			nextPath = new SalesCostSummaryService(req, true).costSummary();
		}
		case "/summary/sales"->{
			nextPath = new SalesCostSummaryService(req, true).salesSummary();
		}
		
		
		
		case "/invoice/sales"->{
			nextPath = new InvoiceService(req, true).adminInvoiceSummary();
		}
		case "/invoice/costs"->{
			nextPath = new InvoiceService(req, true).secretaryInvoiceSummary();
		}
	}
    }
    
    
	/**
	 * secretaryExecuteメソッド
	 * 秘書ユーザの処理をするためのメソッド
	 * @param req
	 * @param res
	 * @throws ServletException
	 * @throws IOException
	 * @throws SQLException
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
			 * A03 労働実績記録業務
			 * 
			 */
			
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
			
			
			case "/profile" -> {
			    nextPath = new ProfileService(req, true).view();          // 表示
			}
			case "/profile/register" -> {
			    nextPath = new ProfileService(req, true).register();      // 登録フォーム
			}
			case "/profile/register_done" -> {
			    nextPath = new ProfileService(req, true).registerDone();  // 登録実行
			}
			case "/profile/edit" -> {
			    nextPath = new ProfileService(req, true).edit();          // 変更フォーム
			}
			case "/profile/edit_done" -> {
			    nextPath = new ProfileService(req, true).editDone();      // 変更実行
			}
    	}
	}
    
	/**
	 * customerExecuteメソッド
	 * 顧客ユーザの処理をするためのメソッド
	 * @param req
	 * @param res
	 * @throws ServletException
	 * @throws IOException
	 * @throws SQLException
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
			
    	}
	}
    
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		execute(req, res);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		execute(req, res);
	}
	
	protected void execute(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		contextPath = req.getContextPath();
		servletPath = req.getServletPath();
		pathInfo = req.getPathInfo();
		nextPath = "index";
		
		// 共通セッションチェック
		HttpSession session = ((HttpServletRequest)req).getSession(false);
	    LoginUser loginUser = (session == null) ? null : (LoginUser) session.getAttribute("loginUser");
	    
	    // ルートパスまたはログインパスかどうかを判定
	    boolean isRootPath = (pathInfo == null) || pathInfo.isEmpty() || "/".equals(pathInfo);
        boolean isLoginPath = "/login".equals(pathInfo);
		
	
       
		switch(servletPath) {
			case "/admin"->{
				// ログインチェック：ルートとログインパス以外は認証必須
		        if (!isRootPath && !isLoginPath) {
		            boolean loggedIn = (loginUser != null && loginUser.getAuthority() == 1);
		            if (!loggedIn) {
		                res.sendRedirect(contextPath + "/admin");
		                return;
		            }
		        }
		        
		    	if(pathInfo == null)  nextPath = "common/admin/login";
		    	else adminExecute(req, res);
			}
			case "/secretary"->{
				// ログインチェック：ルートとログインパス以外は認証必須
		        if (!isRootPath && !isLoginPath) {
		            boolean loggedIn = (loginUser != null && loginUser.getAuthority() == 2);
		            if (!loggedIn) {
		                res.sendRedirect(contextPath + "/secretary");
		                return;
		            }
		        }
				
				if(pathInfo == null)  nextPath = "common/secretary/login";
		    	else secretaryExecute(req, res);
			}
			case "/customer"->{
				// ログインチェック：ルートとログインパス以外は認証必須
		        if (!isRootPath && !isLoginPath) {
		            boolean loggedIn = (loginUser != null && loginUser.getAuthority() == 3);
		            if (!loggedIn) {
		                res.sendRedirect(contextPath + "/customer");
		                return;
		            }
		        }
				
				if(pathInfo == null)  nextPath = "common/customer/login";
		    	else customerExecute(req, res);
			}
		}
		
		// ページ遷移
		if (res.isCommitted()) {   // ★ 追加：ファイルDLやsendErrorでレスポンスが確定していたら何もしない
		    return;
		}
		
		// ページ遷移
		char firstPath = nextPath.charAt(0);
		if(firstPath == '/') {
			// 先頭がスラッシュだとリダイレクト
			res.sendRedirect(res.encodeRedirectURL(nextPath));
		} else {
			
			RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/jsp/" + nextPath + ".jsp");
			rd.forward(req, res);
			
		}
	}

}
