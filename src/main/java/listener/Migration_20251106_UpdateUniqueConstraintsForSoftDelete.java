package listener;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 論理削除対応のUNIQUE制約を部分一意インデックスに変更するマイグレーション
 *
 * 実行内容:
 *   - system_admins.mail のUNIQUE制約を部分一意インデックスに変更
 *   - secretaries.mail のUNIQUE制約を部分一意インデックスに変更
 *   - secretaries.secretary_code のUNIQUE制約を部分一意インデックスに変更
 *   - customer_contacts.mail のUNIQUE制約を部分一意インデックスに変更
 *
 * 実行日: 2025-11-06
 */
public class Migration_20251106_UpdateUniqueConstraintsForSoftDelete implements Migration {

    @Override
    public void up(Connection conn) throws SQLException {
        System.out.println("  [Migration] 論理削除対応UNIQUE制約の変更開始...");

        try (Statement stmt = conn.createStatement()) {
            // 1. system_admins.mail のUNIQUE制約を削除し、部分一意インデックスを作成
            System.out.println("    - system_admins.mail の制約を変更中...");
            stmt.execute("DO $$ " +
                "BEGIN " +
                "  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'system_admins_mail_key') THEN " +
                "    ALTER TABLE system_admins DROP CONSTRAINT system_admins_mail_key; " +
                "  END IF; " +
                "END $$");
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_system_admins_mail_active " +
                "ON system_admins(mail) WHERE deleted_at IS NULL");
            System.out.println("      system_admins.mail の部分一意インデックス作成完了");

            // 2. secretaries.mail のUNIQUE制約を削除し、部分一意インデックスを作成
            System.out.println("    - secretaries.mail の制約を変更中...");
            stmt.execute("DO $$ " +
                "BEGIN " +
                "  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'secretaries_mail_key') THEN " +
                "    ALTER TABLE secretaries DROP CONSTRAINT secretaries_mail_key; " +
                "  END IF; " +
                "END $$");
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_secretaries_mail_active " +
                "ON secretaries(mail) WHERE deleted_at IS NULL");
            System.out.println("      secretaries.mail の部分一意インデックス作成完了");

            // 3. secretaries.secretary_code のUNIQUE制約を削除し、部分一意インデックスを作成
            System.out.println("    - secretaries.secretary_code の制約を変更中...");
            stmt.execute("DO $$ " +
                "BEGIN " +
                "  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'secretaries_secretary_code_key') THEN " +
                "    ALTER TABLE secretaries DROP CONSTRAINT secretaries_secretary_code_key; " +
                "  END IF; " +
                "END $$");
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_secretaries_code_active " +
                "ON secretaries(secretary_code) WHERE deleted_at IS NULL");
            System.out.println("      secretaries.secretary_code の部分一意インデックス作成完了");

            // 4. customer_contacts.mail のUNIQUE制約を削除し、部分一意インデックスを作成
            System.out.println("    - customer_contacts.mail の制約を変更中...");
            stmt.execute("DO $$ " +
                "BEGIN " +
                "  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'customer_contacts_mail_key') THEN " +
                "    ALTER TABLE customer_contacts DROP CONSTRAINT customer_contacts_mail_key; " +
                "  END IF; " +
                "END $$");
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_contacts_mail_active " +
                "ON customer_contacts(mail) WHERE deleted_at IS NULL");
            System.out.println("      customer_contacts.mail の部分一意インデックス作成完了");
        }

        System.out.println("  [Migration] 論理削除対応UNIQUE制約の変更完了");
    }

    @Override
    public String getDescription() {
        return "論理削除対応のUNIQUE制約を部分一意インデックスに変更";
    }
}

