package dao;

import java.sql.Connection;

public class BaseDAO {
	protected Connection conn;
	
	public BaseDAO(Connection conn) {
		this.conn = conn;
	}
}
