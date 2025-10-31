package dao;

import java.sql.Connection;

/**
 * DAO基底クラス
 * データベース接続を保持し、各DAOクラスで継承して使用します
 */
public class BaseDAO {
	/** データベース接続 */
	protected Connection conn;
	
	/**
	 * コンストラクタ
	 * 
	 * @param conn データベース接続
	 */
	public BaseDAO(Connection conn) {
		this.conn = conn;
	}
}
