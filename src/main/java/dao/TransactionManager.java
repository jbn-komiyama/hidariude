package dao;
import java.sql.Connection;
import java.sql.SQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class TransactionManager implements AutoCloseable{
	private static final String DB_URL = "jdbc:postgresql://localhost:5433/hidariude";
	private static final String SCHEMA = "?currentSchema=public";
	private static final String DB_USER = "postgres";
	private static final String DB_PASSWORD = "password";
	
	// コネクションプール（シングルトン）
	private static HikariDataSource dataSource;
	
	static {
		try {
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(DB_URL + SCHEMA);
			config.setUsername(DB_USER);
			config.setPassword(DB_PASSWORD);
			
			// コネクションプールの設定
			config.setMaximumPoolSize(20);           // 最大接続数
			config.setMinimumIdle(5);                 // 最小アイドル接続数
			config.setConnectionTimeout(30000);       // 接続タイムアウト（30秒）
			config.setIdleTimeout(600000);            // アイドルタイムアウト（10分）
			config.setMaxLifetime(1800000);           // 最大接続生存時間（30分）
			config.setAutoCommit(false);              // 自動コミット無効
			
			// PostgreSQL最適化
			config.addDataSourceProperty("cachePrepStmts", "true");
			config.addDataSourceProperty("prepStmtCacheSize", "250");
			config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
			
			dataSource = new HikariDataSource(config);
		} catch (Exception e) {
			throw new ExceptionInInitializerError("コネクションプールの初期化に失敗しました: " + e.getMessage());
		}
	}
	
	private Connection conn;
	private boolean isCommit;
	
	public Connection getConnection(){
		if(conn == null) {
			try {
				conn = dataSource.getConnection();
				conn.setAutoCommit(false);
			} catch(SQLException e) {
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
		try {
			if(conn != null) {
				if (isCommit) {
					conn.commit();
				} else {
					conn.rollback();
				}
				conn.close();
				conn = null;
			}
		} catch(SQLException e) {
			String message = "E:TM03 トランザクション終了中にエラーが発生しました";
			throw new TransactionException(message, e);
		}
	}
}