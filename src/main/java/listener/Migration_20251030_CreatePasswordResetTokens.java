package listener;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * パスワードリセット用トークンテーブルを作成するマイグレーション
 *
 * 実行内容:
 *   ・password_reset_tokens テーブルを作成（全ロール対応）
 *   ・トークン検索用のインデックスを作成
 *   ・ユーザーID検索用のインデックスを作成
 *
 * テーブル設計:
 *   ・user_type: 'admin', 'secretary', 'customer' のいずれか
 *   ・user_id: 各ロールのテーブルのID（外部キー制約なし、アプリケーション側で管理）
 *
 * 実行日: 2025-10-30
 */
public class Migration_20251030_CreatePasswordResetTokens implements Migration {

    @Override
    public void up(Connection conn) throws SQLException {
        System.out.println("  [Migration] パスワードリセットトークンテーブル作成開始...");

        String createTableSql = 
            "CREATE TABLE password_reset_tokens (" +
            "    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), " +
            "    user_type VARCHAR(20) NOT NULL CHECK (user_type IN ('admin', 'secretary', 'customer')), " +
            "    user_id UUID NOT NULL, " +
            "    token VARCHAR(255) UNIQUE NOT NULL, " +
            "    expires_at TIMESTAMP NOT NULL, " +
            "    used_at TIMESTAMP, " +
            "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")";

        String createTokenIndexSql = 
            "CREATE INDEX idx_password_reset_tokens_token " +
            "ON password_reset_tokens(token)";

        String createUserIndexSql = 
            "CREATE INDEX idx_password_reset_tokens_user " +
            "ON password_reset_tokens(user_type, user_id)";

        try (Statement stmt = conn.createStatement()) {
            // テーブル作成
            stmt.executeUpdate(createTableSql);
            System.out.println("    - password_reset_tokens テーブル作成完了");

            // インデックス作成
            stmt.executeUpdate(createTokenIndexSql);
            System.out.println("    - トークン用インデックス作成完了");

            stmt.executeUpdate(createUserIndexSql);
            System.out.println("    - ユーザー用インデックス作成完了");
        }

        System.out.println("  [Migration] パスワードリセットトークンテーブル作成完了");
    }

    @Override
    public String getName() {
        return "create_password_reset_tokens_20251030";
    }

    @Override
    public String getDescription() {
        return "パスワードリセット用トークンテーブルの作成";
    }
}

