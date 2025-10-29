# データベースマイグレーション ガイド

## 概要

このプロジェクトでは、データベースのスキーマやデータの変更を管理するためにマイグレーションシステムを使用しています。

## マイグレーション実行の判断基準

マイグレーションは以下の基準で自動的に実行されます：

1. **自動実行タイミング**: アプリケーション起動時に自動実行
2. **実行判定**: `schema_migrations` テーブルに記録がない場合のみ実行
3. **実行順序**: クラス名の日付順（`Migration_YYYYMMDD_*`）にソートして実行
4. **成功時**: `schema_migrations` テーブルにマイグレーション名と実行日時を記録
5. **失敗時**: ロールバックされ、記録は残らない（次回起動時に再実行される）

## 新しいマイグレーションの作成方法

### ステップ 1: マイグレーションファイルを作成

`src/main/java/listener/` ディレクトリに新しいファイルを作成します。

**ファイル名の命名規則**: `Migration_YYYYMMDD_DescriptiveName.java`

例:

-   `Migration_20251029_UpdateSecretaryPayWithTax.java`
-   `Migration_20251030_AddNewColumn.java`
-   `Migration_20251101_RemoveOldTable.java`

### ステップ 2: Migration インターフェースを実装

```java
package listener;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * マイグレーションの説明をここに記述
 *
 * 更新対象:
 *   ・変更内容1
 *   ・変更内容2
 *
 * 実行日: YYYY-MM-DD
 */
public class Migration_YYYYMMDD_YourMigrationName implements Migration {

    @Override
    public void up(Connection conn) throws SQLException {
        System.out.println("  [Migration] マイグレーション処理開始...");

        // ここにSQL処理を記述
        String sql = "ALTER TABLE your_table ADD COLUMN new_column VARCHAR(255)";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("    - テーブル変更完了");
        }

        System.out.println("  [Migration] マイグレーション処理完了");
    }

    @Override
    public String getDescription() {
        return "マイグレーションの簡単な説明";
    }
}
```

### ステップ 3: DatabaseInitListener に登録

`DatabaseInitListener.java` の `runMigrations()` メソッド内に、新しいマイグレーションを追加します。

```java
// Step 2: 通常のマイグレーション一覧を定義（手動で追加）
List<Migration> migrations = new ArrayList<>();
migrations.add(new Migration_20251029_CreateSchemaMigrations()); // 必須
migrations.add(new Migration_20251029_UpdateSecretaryPayWithTax());
migrations.add(new Migration_YYYYMMDD_YourMigrationName()); // ← ここに追加

// 今後のマイグレーションをここに追加
```

### ステップ 4: アプリケーションを再起動

アプリケーションを再起動すると、新しいマイグレーションが自動的に実行されます。

## マイグレーション作成のベストプラクティス

### 1. マイグレーション名の付け方

-   日付は必ず `YYYYMMDD` 形式にする
-   説明部分は英語で、キャメルケースで記述する
-   複数の単語をつなげる場合はキャメルケースを使用

**良い例**:

-   `Migration_20251029_AddUserEmailIndex`
-   `Migration_20251030_UpdateCustomerSchema`
-   `Migration_20251101_RemoveDeprecatedColumns`

**悪い例**:

-   `Migration_add_column.java` (日付がない)
-   `Migration_20251029.java` (説明がない)
-   `Migration_2025-10-29_AddColumn.java` (日付形式が違う)

### 2. マイグレーション内容の原則

-   **1 マイグレーション = 1 つの論理的変更**: 関連する変更をまとめる
-   **冪等性は不要**: 各マイグレーションは 1 回のみ実行されることが保証されている
-   **ロールバック処理は不要**: 失敗時は自動的にロールバックされる
-   **トランザクション管理は不要**: 呼び出し側で管理される

### 3. SQL の書き方

```java
@Override
public void up(Connection conn) throws SQLException {
    // 複数のSQL文を実行する場合
    try (Statement stmt = conn.createStatement()) {
        // テーブル作成
        stmt.execute("CREATE TABLE IF NOT EXISTS new_table (id UUID PRIMARY KEY)");

        // インデックス作成
        stmt.execute("CREATE INDEX idx_new_table_id ON new_table(id)");

        // データ更新
        int count = stmt.executeUpdate("UPDATE existing_table SET status = 'active'");
        System.out.println("    - Updated " + count + " records");
    }
}
```

### 4. エラーハンドリング

```java
@Override
public void up(Connection conn) throws SQLException {
    // エラーが発生する可能性のある処理
    try (Statement stmt = conn.createStatement()) {
        stmt.execute("ALTER TABLE my_table ADD COLUMN new_col VARCHAR(255)");
    } catch (SQLException e) {
        // エラーログを出力してから再スロー
        System.err.println("    - Error: " + e.getMessage());
        throw e; // 必ず再スローすること（ロールバックのため）
    }
}
```

## マイグレーション実行ログの確認

アプリケーション起動時のログで、マイグレーションの実行状況を確認できます：

```
=== Database Initialization Start ===
Tables already exist. Skipping initialization.
Checking for pending migrations...
Migration table not found. Creating...
Applying migration: 00000000_create_schema_migrations
  Description: マイグレーション管理用テーブルを作成
  [Migration] Creating schema_migrations table...
    - schema_migrations table created successfully
  [Migration] Migration table setup completed.
Migration applied successfully: 00000000_create_schema_migrations
Applying migration: 20251029_update_secretary_pay_with_tax
  Description: 秘書向け金額を税込み（+10%）に更新
  [Migration] Updating secretary pay amounts to include 10% tax...
    - Updated 3 secretary rank records (increase_base_pay_secretary * 1.1)
    - Updated 5 task rank records (base_pay_secretary * 1.1)
  [Migration] Secretary pay amounts updated successfully.
Migration applied successfully: 20251029_update_secretary_pay_with_tax
Migration check completed: 2 applied, 0 skipped.
=== Database Initialization End ===
```

## データベースの確認

### マイグレーション実行履歴を確認

```sql
SELECT * FROM schema_migrations ORDER BY applied_at DESC;
```

結果例:

```
id | migration_name                           | applied_at
---+------------------------------------------+-------------------------
 1 | 20251029_create_schema_migrations        | 2025-10-29 10:30:00.000
 2 | 20251029_update_secretary_pay_with_tax   | 2025-10-29 10:30:01.123
```

### マイグレーション実行を手動でリセット（開発環境のみ）

特定のマイグレーションを再実行したい場合（**注意: 本番環境では実行しないこと**）:

```sql
-- 特定のマイグレーションの記録を削除
DELETE FROM schema_migrations WHERE migration_name = '20251029_update_secretary_pay_with_tax';

-- すべてのマイグレーション記録を削除（schema_migrationsテーブルを含む）
DROP TABLE schema_migrations;
```

テーブルを削除すると、次回起動時に `Migration_20251029_CreateSchemaMigrations` から再実行されます。

## トラブルシューティング

### マイグレーションが実行されない

1. `schema_migrations` テーブルを確認
2. マイグレーション名が正しいか確認（`Migration.getName()` が返す値）
3. `DatabaseInitListener.java` に登録されているか確認

### マイグレーション実行中にエラーが発生

1. エラーログを確認
2. SQL 文の構文を確認
3. データベースの状態を確認（テーブル/カラムの存在など）
4. エラーが解消されたら、アプリケーションを再起動（自動的に再実行される）

### マイグレーションを取り消したい

マイグレーションの取り消しは、新しいマイグレーションを作成して対応します：

```java
// 例: カラムを削除するマイグレーション
public class Migration_20251030_RevertAddColumn implements Migration {
    @Override
    public void up(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE my_table DROP COLUMN new_column");
        }
    }

    @Override
    public String getDescription() {
        return "前回追加したカラムを削除";
    }
}
```

## 既存マイグレーション一覧

### Migration_20251029_CreateSchemaMigrations

-   **目的**: マイグレーション管理用テーブルを作成
-   **実行日**: システム初期化時（最初に実行）
-   **変更内容**: `schema_migrations` テーブルを作成
-   **注意**: このマイグレーションは必須であり、削除しないでください

### Migration_20251029_UpdateSecretaryPayWithTax

-   **目的**: 秘書向け金額を税込み（+10%）に更新
-   **実行日**: 2025-10-29
-   **変更内容**:
    -   `secretary_rank.increase_base_pay_secretary` を 1.1 倍に更新
    -   `task_rank.base_pay_secretary` を 1.1 倍に更新

## まとめ

-   マイグレーションは起動時に自動実行される
-   1 マイグレーション = 1 ファイルで管理
-   クラス名は `Migration_YYYYMMDD_DescriptiveName` 形式
-   `DatabaseInitListener.java` に登録を忘れずに
-   エラー時は自動ロールバックされ、次回起動時に再実行される
-   `Migration_20251029_CreateSchemaMigrations` は必須（削除しない）
