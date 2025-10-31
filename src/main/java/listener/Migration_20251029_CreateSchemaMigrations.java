package listener;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * マイグレーション管理用テーブルを作成するマイグレーション
 * 
 * 作成テーブル:
 *   - schema_migrations - マイグレーション実行履歴を管理
 * 
 * 実行日: 2025-10-29
 */
public class Migration_20251029_CreateSchemaMigrations implements Migration {
    
    @Override
    public void up(Connection conn) throws SQLException {
        System.out.println("  [Migration] Creating schema_migrations table...");
        
        String sql = "CREATE TABLE IF NOT EXISTS schema_migrations (" +
                     "    id SERIAL PRIMARY KEY," +
                     "    migration_name VARCHAR(255) UNIQUE NOT NULL," +
                     "    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                     ")";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("    - schema_migrations table created successfully");
        }
        
        System.out.println("  [Migration] Migration table setup completed.");
    }
    
    @Override
    public String getDescription() {
        return "マイグレーション管理用テーブルを作成";
    }
}

