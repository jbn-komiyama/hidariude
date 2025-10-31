package listener;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * データベースマイグレーション用インターフェース
 * 
 * マイグレーション実装のルール:
 *   - クラス名は Migration_YYYYMMDD_DescriptiveName の形式にすること
 *   - マイグレーション名はクラス名から自動生成される（例: Migration_20251029_UpdateSecretaryPayWithTax → update_secretary_pay_with_tax_20251029）
 *   - up()メソッドにマイグレーション処理を実装すること
 *   - 各マイグレーションは1回のみ実行される（schema_migrationsテーブルで管理）
 *   - マイグレーションは日付順に実行される
 * 
 * 実行判断基準:
 *   - schema_migrationsテーブルにマイグレーション名が存在しない場合のみ実行
 *   - 実行成功後、schema_migrationsテーブルにマイグレーション名と実行日時を記録
 *   - 実行失敗時はロールバックされ、記録は残らない
 */
public interface Migration {
    
    /**
     * マイグレーション処理を実行
     * 
     * @param conn データベース接続（トランザクション管理は呼び出し側で行う）
     * @throws SQLException SQL実行エラー
     */
    void up(Connection conn) throws SQLException;
    
    /**
     * マイグレーション名を取得（デフォルトはクラス名から生成）
     * 
     * @return マイグレーション名
     */
    default String getName() {
        /** クラス名から Migration_ プレフィックスを除去し、スネークケースに変換 */
        String className = this.getClass().getSimpleName();
        if (className.startsWith("Migration_")) {
            className = className.substring(10); /** "Migration_" を除去 */
        }
        
        /** キャメルケースをスネークケースに変換 */
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * マイグレーションの説明を取得
     * 
     * @return マイグレーションの説明
     */
    default String getDescription() {
        return "No description provided";
    }
}

