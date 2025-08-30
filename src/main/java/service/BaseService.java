package service;
import java.sql.Connection;
import jakarta.servlet.http.HttpServletRequest;

import dao.TransactionManager;
import validation.Validation;

public class BaseService {
	protected HttpServletRequest req;
	protected TransactionManager tm;
	protected Connection conn;
	protected Validation validation = new Validation();
	
	public BaseService(HttpServletRequest req, boolean useDB) {
		this.req = req;
		if(useDB) {
			this.tm = new TransactionManager();
			this.conn = tm.getConnection();
		}
	}
}
