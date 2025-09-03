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
		case "/home"->{
			nextPath = new CommonService(req, false).adminHome();
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
			nextPath = new SystemAdminService(req, true).systemAdminRegisterDone();
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
			
			case "/home"->{
				nextPath = new CommonService(req, true).secretaryHome();
			}
			

			/**
			 * A02 業務管理業務
			 * 
			 */
			
			case "/task/list"->{
				nextPath = new TaskService(req, true).taskList();
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
				nextPath = new CommonService(req, true).secretaryLogin();
			}
			
			case "/home"->{
				nextPath = "common/secretary/home";
			}
			
			/**
			 * A02 秘書管理業務
			 * 
			 */
			
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
	    
	    boolean isAdminRoot = (pathInfo == null) || pathInfo.isEmpty() || "/".equals(pathInfo);
        boolean isLoginPath = "/login".equals(pathInfo);
		
		
		switch(servletPath) {
			case "/admin"->{
				boolean loggedIn = (loginUser != null && loginUser.getAuthority() == 1);
		        if (!loggedIn && (!isAdminRoot && !isLoginPath)) {
		            res.sendRedirect(contextPath + "/admin");
		            return;
		        }
		        
		    	if(pathInfo == null)  nextPath = "common/admin/login";
		    	else adminExecute(req, res);
			}
			case "/secretary"->{
				boolean loggedIn = (loginUser != null && loginUser.getAuthority() == 2);
		        if (!loggedIn && (!isAdminRoot && !isLoginPath)) {
		            res.sendRedirect(contextPath + "/secretary");
		            return;
		        }
				
				if(pathInfo == null)  nextPath = "common/secretary/login";
		    	else secretaryExecute(req, res);
			}
			case "/customer"->{
				boolean loggedIn = (loginUser != null && loginUser.getAuthority() == 3);
		        if (!loggedIn && (!isAdminRoot && !isLoginPath)) {
		            res.sendRedirect(contextPath + "/customer");
		            return;
		        }
				
				if(pathInfo == null)  nextPath = "common/customer/login";
		    	else customerExecute(req, res);
			}
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
