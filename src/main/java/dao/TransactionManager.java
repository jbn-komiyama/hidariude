package dao;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TransactionManager implements AutoCloseable{
	private static final String DRIVER_NAME = "org.postgresql.Driver";
	private static final String DB_URL = "jdbc:postgresql://localhost:5433/hidariude";
	private static final String SCHEMA = "?currentSchema=public";
	private static final String DB_USER = "postgres";
	private static final String DB_PASSWORD = "password";
	private Connection conn;
	private boolean isCommit;
	
	public Connection getConnection(){
		if(conn == null) {
			try {
				Class.forName(DRIVER_NAME);
				conn = DriverManager.getConnection(DB_URL + SCHEMA, DB_USER, DB_PASSWORD);
				conn.setAutoCommit(false);
			} catch(ClassNotFoundException | SQLException e) {
				String message = "E:TM01 トランザクションが開始できませんでした";
				throw new TransactionException(message, e);
			}
		}
		return conn;
	}
	
	public void commit(){
		if (conn == null) {
			String message = "E:TM02 トランザクションが開始されていません";
			throw new TransactionException(message);
		} else {
			isCommit = true;
		}
	}

	public void rollback(){
		if (conn == null) {
			String message = "E:TM04 トランザクションが開始されていません";
			throw new TransactionException(message);
		} else {
			isCommit = false;
		}
	}

	@Override
	public void close(){
		if(conn != null) {
			try {
				// コミット/ロールバックを試行
				if (isCommit) {
					conn.commit();
				} else {
					conn.rollback();
				}
			} catch(SQLException e) {
				// コミット/ロールバック失敗時もエラーログを出力
				System.err.println("コミット/ロールバック中のエラー: " + e.getMessage());
				// 接続は必ずクローズするため、ここでは例外を再スローしない
			} finally {
				// 常に接続をクローズする
				try {
					conn.close();
				} catch (SQLException e) {
					System.err.println("接続クローズ中のエラー: " + e.getMessage());
					// クローズ失敗はログのみ記録し、アプリケーションは継続
				} finally {
					conn = null;
				}
			}
		}
	}
}