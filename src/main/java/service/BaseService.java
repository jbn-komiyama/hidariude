package service;
import java.sql.Connection;
import jakarta.servlet.http.HttpServletRequest;
import util.ValidationUtil;
import dao.TransactionManager;

/**
 * サービス層の基底クラス
 */
public class BaseService {
	protected HttpServletRequest req;
	protected TransactionManager tm;
	protected Connection conn;
	protected ValidationUtil validation = new ValidationUtil();
    /** ===== Error */
    public static String REDIRECT_ERROR;
	
	public BaseService(HttpServletRequest req, boolean useDB) {
		this.req = req;
		if(useDB) {
			this.tm = new TransactionManager();
			this.conn = tm.getConnection();
		}
		REDIRECT_ERROR = req.getContextPath() + req.getServletPath() + "/common/error";
	}
}
