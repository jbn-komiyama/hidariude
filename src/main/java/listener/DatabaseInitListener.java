package listener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import util.PasswordUtil;

/**
 * アプリケーション起動時にデータベースの初期化を行うリスナー
 */
@WebListener
public class DatabaseInitListener implements ServletContextListener {

    private static final String DRIVER_NAME = "org.postgresql.Driver";
    private static final String DB_URL = "jdbc:postgresql://localhost:5433/backdesk";
    private static final String SCHEMA = "?currentSchema=public";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "password";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("=== Database Initialization Start ===");
        
        try {
            Class.forName(DRIVER_NAME);
            
            try (Connection conn = DriverManager.getConnection(DB_URL + SCHEMA, DB_USER, DB_PASSWORD)) {
                conn.setAutoCommit(false);
                
                /** テーブルが存在するかチェック */
                if (!tableExists(conn, "system_admins")) {
                    System.out.println("Tables not found. Creating database schema and initial data...");
                    
                    /** DDL実行 */
                    executeDDL(conn);
                    
                    /** 初期データ投入 */
                    insertInitialData(conn);
                    
                    conn.commit();
                    System.out.println("Database initialization completed successfully.");
                } else {
                    System.out.println("Tables already exist. Skipping initialization.");
                    /** 既存の制約を修正（口座情報の空欄を許可） */
                    updateBankTypeConstraint(conn);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error during database initialization: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== Database Initialization End ===");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        /** クリーンアップ処理が必要な場合はここに記述 */
    }

    /**
     * 既存のbank_type制約を更新して、空欄を許可する
     */
    private void updateBankTypeConstraint(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            /** 既存の制約を削除 */
            stmt.execute("ALTER TABLE secretaries DROP CONSTRAINT IF EXISTS chk_secretaries_bank_type");
            /** 新しい制約を追加（空欄を許可） */
            stmt.execute("ALTER TABLE secretaries ADD CONSTRAINT chk_secretaries_bank_type " +
                        "CHECK (bank_type IS NULL OR bank_type = '' OR bank_type IN ('普通', '当座'))");
            conn.commit();
            System.out.println("Updated bank_type constraint to allow empty values.");
        } catch (SQLException e) {
            System.err.println("Warning: Could not update bank_type constraint: " + e.getMessage());
            /** エラーが発生してもアプリケーションの起動は継続 */
        }
    }

    /**
     * 指定されたテーブルが存在するかチェック
     */
    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT EXISTS (" +
                     "  SELECT FROM information_schema.tables " +
                     "  WHERE table_schema = 'public' " +
                     "  AND table_name = ?" +
                     ")";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
    }

    /**
     * DDLを実行してテーブルを作成
     */
    private void executeDDL(Connection conn) throws Exception {
        System.out.println("Executing DDL...");
        
        List<String> ddlStatements = new ArrayList<>();
        
        /** UUID生成用拡張 */
        ddlStatements.add("CREATE EXTENSION IF NOT EXISTS pgcrypto");
        
        /** テーブル定義 */
        ddlStatements.add(
            "CREATE TABLE system_admins (" +
            "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
            "    mail VARCHAR(255) NOT NULL," +
            "    password VARCHAR(255) NOT NULL," +
            "    name VARCHAR(255) NOT NULL," +
            "    name_ruby VARCHAR(255)," +
            "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    deleted_at TIMESTAMP," +
            "    last_login_at TIMESTAMP" +
            ")"
        );
        
        ddlStatements.add(
            "CREATE TABLE secretary_rank (" +
            "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
            "    rank_name VARCHAR(255) NOT NULL," +
            "    description TEXT," +
            "    increase_base_pay_customer DECIMAL(10,2)," +
            "    increase_base_pay_secretary DECIMAL(10,2)," +
            "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    deleted_at TIMESTAMP" +
            ")"
        );
        
        ddlStatements.add(
            "CREATE TABLE secretaries (" +
            "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
            "    secretary_code VARCHAR(255) NOT NULL," +
            "    mail VARCHAR(255) NOT NULL," +
            "    password VARCHAR(255) NOT NULL," +
            "    secretary_rank_id UUID REFERENCES secretary_rank(id)," +
            "    is_pm_secretary BOOLEAN DEFAULT FALSE," +
            "    name VARCHAR(255) NOT NULL," +
            "    name_ruby VARCHAR(255)," +
            "    phone VARCHAR(50)," +
            "    postal_code VARCHAR(20)," +
            "    address1 VARCHAR(255)," +
            "    address2 VARCHAR(255)," +
            "    building VARCHAR(255)," +
            "    bank_name VARCHAR(255)," +
            "    bank_branch VARCHAR(255)," +
            "    bank_type VARCHAR(255)," +
            "    bank_account VARCHAR(255)," +
            "    bank_owner VARCHAR(255)," +
            "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    deleted_at TIMESTAMP," +
            "    last_login_at TIMESTAMP," +
            "    CONSTRAINT chk_secretaries_bank_type CHECK (bank_type IS NULL OR bank_type = '' OR bank_type IN ('普通', '当座'))" +
            ")"
        );
        
        ddlStatements.add(
            "CREATE TABLE customers (" +
            "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
            "    company_code VARCHAR(255) UNIQUE NOT NULL," +
            "    company_name VARCHAR(255) NOT NULL," +
            "    mail VARCHAR(255)," +
            "    phone VARCHAR(50)," +
            "    postal_code VARCHAR(20)," +
            "    address1 VARCHAR(255)," +
            "    address2 VARCHAR(255)," +
            "    building VARCHAR(255)," +
            "    primary_contact_id UUID," +
            "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    deleted_at TIMESTAMP" +
            ")"
        );
        
        ddlStatements.add(
            "CREATE TABLE customer_contacts (" +
            "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
            "    mail VARCHAR(255) NOT NULL," +
            "    password VARCHAR(255) NOT NULL," +
            "    customer_id UUID NOT NULL," +
            "    name VARCHAR(255) NOT NULL," +
            "    name_ruby VARCHAR(255)," +
            "    phone VARCHAR(50)," +
            "    department VARCHAR(255)," +
            "    is_primary BOOLEAN DEFAULT FALSE," +
            "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    deleted_at TIMESTAMP," +
            "    last_login_at TIMESTAMP," +
            "    CONSTRAINT fk_customer_contacts_customer" +
            "        FOREIGN KEY (customer_id)" +
            "        REFERENCES customers(id)" +
            "        ON UPDATE CASCADE" +
            "        ON DELETE CASCADE," +
            "    CONSTRAINT uq_customer_contacts_id_customer" +
            "        UNIQUE (id, customer_id)" +
            ")"
        );
        
        ddlStatements.add("CREATE INDEX idx_customer_contacts_customer_id ON customer_contacts (customer_id)");
        
        ddlStatements.add(
            "ALTER TABLE customers " +
            "  ADD CONSTRAINT fk_customers_primary_contact " +
            "  FOREIGN KEY (primary_contact_id) " +
            "  REFERENCES customer_contacts(id) " +
            "  ON UPDATE CASCADE " +
            "  ON DELETE SET NULL"
        );
        
        ddlStatements.add(
            "CREATE TABLE task_rank (" +
            "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
            "    rank_name VARCHAR(50) NOT NULL," +
            "    rank_no INTEGER," +
            "    base_pay_customer DECIMAL(10,2)," +
            "    base_pay_secretary DECIMAL(10,2)," +
            "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    deleted_at TIMESTAMP" +
            ")"
        );
        
        ddlStatements.add(
            "CREATE TABLE assignments (" +
            "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
            "    customer_id UUID REFERENCES customers(id)," +
            "    secretary_id UUID REFERENCES secretaries(id)," +
            "    task_rank_id UUID REFERENCES task_rank(id)," +
            "    target_year_month VARCHAR(7) NOT NULL," +
            "    base_pay_customer DECIMAL(10,2)," +
            "    base_pay_secretary DECIMAL(10,2)," +
            "    increase_base_pay_customer DECIMAL(10,2)," +
            "    increase_base_pay_secretary DECIMAL(10,2)," +
            "    customer_based_incentive_for_customer DECIMAL(10,2)," +
            "    customer_based_incentive_for_secretary DECIMAL(10,2)," +
            "    status VARCHAR(20)," +
            "    created_by_secretary UUID REFERENCES secretaries(id)," +
            "    created_by_system_admin UUID REFERENCES system_admins(id)," +
            "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    deleted_at TIMESTAMP" +
            ")"
        );
        
        ddlStatements.add(
            "CREATE UNIQUE INDEX uq_assignments_ym_cust_sec_rank " +
            "ON assignments (target_year_month, customer_id, secretary_id, task_rank_id) " +
            "WHERE deleted_at IS NULL"
        );
        
        ddlStatements.add(
            "CREATE TABLE customer_monthly_invoices (" +
            "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
            "    customer_id UUID REFERENCES customers(id)," +
            "    target_year_month VARCHAR(7) NOT NULL," +
            "    total_amount DECIMAL(12,2)," +
            "    total_tasks_count INTEGER," +
            "    total_work_time INTEGER," +
            "    status VARCHAR(20)," +
            "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    deleted_at TIMESTAMP," +
            "    CONSTRAINT uq_cmi_customer_month UNIQUE (customer_id, target_year_month)" +
            ")"
        );
        
        ddlStatements.add(
            "CREATE UNIQUE INDEX ux_cmi_customer_month " +
            "ON customer_monthly_invoices(customer_id, target_year_month) " +
            "WHERE deleted_at IS NULL"
        );
        
        ddlStatements.add(
            "CREATE TABLE secretary_monthly_summaries (" +
            "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
            "    secretary_id UUID REFERENCES secretaries(id)," +
            "    target_year_month VARCHAR(7) NOT NULL," +
            "    total_secretary_amount DECIMAL(12,2)," +
            "    total_tasks_count INTEGER," +
            "    total_work_time INTEGER," +
            "    finalized_at TIMESTAMP," +
            "    status VARCHAR(20)," +
            "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    deleted_at TIMESTAMP," +
            "    CONSTRAINT uq_sec_month UNIQUE (secretary_id, target_year_month)" +
            ")"
        );
        
        ddlStatements.add(
            "CREATE TABLE tasks (" +
            "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
            "    assignment_id UUID REFERENCES assignments(id)," +
            "    work_date DATE NOT NULL," +
            "    start_time TIMESTAMP," +
            "    end_time TIMESTAMP," +
            "    work_minute INTEGER," +
            "    work_content TEXT," +
            "    approved_at TIMESTAMP," +
            "    approved_by UUID REFERENCES system_admins(id) ON UPDATE CASCADE ON DELETE SET NULL," +
            "    remanded_at TIMESTAMP," +
            "    remanded_by UUID REFERENCES system_admins(id)," +
            "    remand_comment TEXT," +
            "    alerted_at TIMESTAMP," +
            "    alerted_comment TEXT," +
            "    customer_monthly_invoice_id UUID REFERENCES customer_monthly_invoices(id)," +
            "    secretary_monthly_summary_id UUID REFERENCES secretary_monthly_summaries(id)," +
            "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    deleted_at TIMESTAMP" +
            ")"
        );
        
        ddlStatements.add("CREATE INDEX IF NOT EXISTS idx_tasks_alerted_at ON tasks (alerted_at) WHERE deleted_at IS NULL");
        ddlStatements.add("COMMENT ON COLUMN tasks.alerted_at IS 'アラート日時（通知/差し戻しなどの事前警告に使用）'");
        ddlStatements.add("COMMENT ON COLUMN tasks.alerted_comment IS 'アラート理由/メモ'");
        
        /** availability_flag ドメイン */
        ddlStatements.add(
            "DO $$ " +
            "BEGIN " +
            "  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'availability_flag') THEN " +
            "    CREATE DOMAIN availability_flag AS SMALLINT CHECK (VALUE IN (0, 1, 2)); " +
            "  END IF; " +
            "END$$"
        );
        
        ddlStatements.add(
            "CREATE TABLE profiles (" +
            "  id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
            "  secretary_id UUID NOT NULL," +
            "  weekday_morning availability_flag NOT NULL DEFAULT 0," +
            "  weekday_daytime availability_flag NOT NULL DEFAULT 0," +
            "  weekday_night availability_flag NOT NULL DEFAULT 0," +
            "  saturday_morning availability_flag NOT NULL DEFAULT 0," +
            "  saturday_daytime availability_flag NOT NULL DEFAULT 0," +
            "  saturday_night availability_flag NOT NULL DEFAULT 0," +
            "  sunday_morning availability_flag NOT NULL DEFAULT 0," +
            "  sunday_daytime availability_flag NOT NULL DEFAULT 0," +
            "  sunday_night availability_flag NOT NULL DEFAULT 0," +
            "  weekday_work_hours NUMERIC(4,2)," +
            "  saturday_work_hours NUMERIC(4,2)," +
            "  sunday_work_hours NUMERIC(4,2)," +
            "  monthly_work_hours NUMERIC(5,1)," +
            "  remark TEXT," +
            "  qualification TEXT," +
            "  work_history TEXT," +
            "  academic_background TEXT," +
            "  self_introduction TEXT," +
            "  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "  deleted_at TIMESTAMP," +
            "  CONSTRAINT fk_profiles_secretary FOREIGN KEY (secretary_id) REFERENCES secretaries(id)," +
            "  CONSTRAINT uq_profiles_secretary UNIQUE (secretary_id)," +
            "  CONSTRAINT ck_weekday_hours CHECK (weekday_work_hours IS NULL OR (weekday_work_hours >= 0 AND weekday_work_hours <= 24))," +
            "  CONSTRAINT ck_saturday_hours CHECK (saturday_work_hours IS NULL OR (saturday_work_hours >= 0 AND saturday_work_hours <= 24))," +
            "  CONSTRAINT ck_sunday_hours CHECK (sunday_work_hours IS NULL OR (sunday_work_hours >= 0 AND sunday_work_hours <= 24))," +
            "  CONSTRAINT ck_monthly_hours CHECK (monthly_work_hours IS NULL OR (monthly_work_hours >= 0 AND monthly_work_hours <= 744))" +
            ")"
        );
        
        /** パスワードリセットトークンとインデックス */
        ddlStatements.add(
            "CREATE TABLE IF NOT EXISTS password_reset_tokens (" +
            "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
            "    user_type VARCHAR(20) NOT NULL CHECK (user_type IN ('admin', 'secretary', 'customer'))," +
            "    user_id UUID NOT NULL," +
            "    token VARCHAR(255) UNIQUE NOT NULL," +
            "    expires_at TIMESTAMP NOT NULL," +
            "    used_at TIMESTAMP," +
            "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );
        ddlStatements.add(
            "CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_token " +
            "ON password_reset_tokens(token)"
        );
        ddlStatements.add(
            "CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user " +
            "ON password_reset_tokens(user_type, user_id)"
        );
        
        /** 論理削除対応の部分一意インデックス */
        ddlStatements.add(
            "CREATE UNIQUE INDEX IF NOT EXISTS uq_system_admins_mail_active " +
            "ON system_admins(mail) WHERE deleted_at IS NULL"
        );
        ddlStatements.add(
            "CREATE UNIQUE INDEX IF NOT EXISTS uq_secretaries_mail_active " +
            "ON secretaries(mail) WHERE deleted_at IS NULL"
        );
        ddlStatements.add(
            "CREATE UNIQUE INDEX IF NOT EXISTS uq_secretaries_code_active " +
            "ON secretaries(secretary_code) WHERE deleted_at IS NULL"
        );
        ddlStatements.add(
            "CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_contacts_mail_active " +
            "ON customer_contacts(mail) WHERE deleted_at IS NULL"
        );
        
        /** マイグレーション履歴テーブル（今後の拡張用） */
        ddlStatements.add(
            "CREATE TABLE IF NOT EXISTS schema_migrations (" +
            "    id SERIAL PRIMARY KEY," +
            "    migration_name VARCHAR(255) UNIQUE NOT NULL," +
            "    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );
        
        /** DDL実行 */
        try (Statement stmt = conn.createStatement()) {
            for (String sql : ddlStatements) {
                stmt.execute(sql);
            }
        }
        
        System.out.println("DDL execution completed.");
    }

    /**
     * 初期データを投入
     */
    private void insertInitialData(Connection conn) throws SQLException {
        System.out.println("Inserting initial data...");
        
        /** パスワードをハッシュ化（共通パスワード: Password1） */
        String hashedPassword = PasswordUtil.hashPassword("Password1");
        
        /** 1. システム管理者（10件） */
        String sqlAdmin = "INSERT INTO system_admins (mail, password, name, name_ruby) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlAdmin)) {
            String[][] admins = {
                {"admin1@example.com", "管理者1", "かんりしゃいち"},
                {"admin2@example.com", "管理者2", "かんりしゃに"},
                {"admin3@example.com", "管理者3", "かんりしゃさん"},
                {"admin4@example.com", "管理者4", "かんりしゃよん"},
                {"admin5@example.com", "管理者5", "かんりしゃご"},
                {"admin6@example.com", "管理者6", "かんりしゃろく"},
                {"admin7@example.com", "管理者7", "かんりしゃなな"},
                {"admin8@example.com", "管理者8", "かんりしゃはち"},
                {"admin9@example.com", "管理者9", "かんりしゃきゅう"},
                {"admin10@example.com", "管理者10", "かんりしゃじゅう"}
            };
            
            for (String[] admin : admins) {
                ps.setString(1, admin[0]);
                ps.setString(2, hashedPassword);
                ps.setString(3, admin[1]);
                ps.setString(4, admin[2]);
                ps.executeUpdate();
            }
        }
        System.out.println("Inserted 10 system admins.");
        
        /** 2. 秘書ランクマスタ（3件） */
        String sqlRank = "INSERT INTO secretary_rank (rank_name, description, increase_base_pay_customer, increase_base_pay_secretary) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlRank)) {
            ps.setString(1, "初級"); ps.setString(2, "初心者"); ps.setBigDecimal(3, new java.math.BigDecimal("0")); ps.setBigDecimal(4, new java.math.BigDecimal("0")); ps.executeUpdate();
            ps.setString(1, "中級"); ps.setString(2, "中級者"); ps.setBigDecimal(3, new java.math.BigDecimal("100")); ps.setBigDecimal(4, new java.math.BigDecimal("50")); ps.executeUpdate();
            ps.setString(1, "上級"); ps.setString(2, "上級者"); ps.setBigDecimal(3, new java.math.BigDecimal("200")); ps.setBigDecimal(4, new java.math.BigDecimal("100")); ps.executeUpdate();
        }
        System.out.println("Inserted 3 secretary ranks.");
        
        /** 3. 秘書（10件）- ランクID取得して循環割当 */
        List<String> rankIds = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM secretary_rank ORDER BY rank_name")) {
            while (rs.next()) {
                rankIds.add(rs.getString("id"));
            }
        }
        
        String sqlSecretary = "INSERT INTO secretaries " +
            "(secretary_code, mail, password, secretary_rank_id, is_pm_secretary, name, name_ruby, phone, postal_code, address1) " +
            "VALUES (?, ?, ?, ?::uuid, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = conn.prepareStatement(sqlSecretary)) {
            String[][] secretaries = {
                {"S001", "secretary1@example.com", "佐藤 花子", "サトウ ハナコ"},
                {"S002", "secretary2@example.com", "鈴木 美咲", "スズキ ミサキ"},
                {"S003", "secretary3@example.com", "高橋 結衣", "タカハシ ユイ"},
                {"S004", "secretary4@example.com", "田中 彩香", "タナカ アヤカ"},
                {"S005", "secretary5@example.com", "伊藤 美穂", "イトウ ミホ"},
                {"S006", "secretary6@example.com", "渡辺 真由", "ワタナベ マユ"},
                {"S007", "secretary7@example.com", "中村 恵", "ナカムラ メグミ"},
                {"S008", "secretary8@example.com", "小林 佳奈", "コバヤシ カナ"},
                {"S009", "secretary9@example.com", "加藤 美和", "カトウ ミワ"},
                {"S010", "secretary10@example.com", "山本 理沙", "ヤマモト リサ"}
            };
            
            for (int i = 0; i < secretaries.length; i++) {
                String[] sec = secretaries[i];
                int g = i + 1;
                ps.setString(1, sec[0]);
                ps.setString(2, sec[1]);
                ps.setString(3, hashedPassword);
                ps.setString(4, rankIds.get(i % rankIds.size()));
                ps.setBoolean(5, g % 2 == 0);
                ps.setString(6, sec[2]);
                ps.setString(7, sec[3]);
                ps.setString(8, "03-1234-56" + g);
                ps.setString(9, String.format("%07d", 1000000 + g));
                ps.setString(10, "東京都港区テスト" + g);
                ps.executeUpdate();
            }
        }
        System.out.println("Inserted 10 secretaries.");
        
        /** 4. 顧客（10件） */
        String sqlCustomer = "INSERT INTO customers (company_code, company_name, mail, phone, postal_code, address1, building) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlCustomer)) {
            String[][] customers = {
                {"C001", "株式会社Alpha", "alpha@example.com", "03-1111-1111", "1000001", "東京都千代田区千代田1-1", "Alphaビル"},
                {"C002", "株式会社Beta", "beta@example.com", "03-2222-2222", "1000002", "東京都千代田区丸の内1-2", "Betaタワー"},
                {"C003", "株式会社Gamma", "gamma@example.com", "03-3333-3333", "1000003", "東京都中央区日本橋1-3", "Gammaビル"},
                {"C004", "株式会社Delta", "delta@example.com", "03-4444-4444", "1000004", "東京都中央区銀座1-4", "Deltaタワー"},
                {"C005", "株式会社Epsilon", "epsilon@example.com", "03-5555-5555", "1000005", "東京都港区六本木1-5", "Epsilonビル"},
                {"C006", "株式会社Zeta", "zeta@example.com", "03-6666-6666", "1000006", "東京都港区赤坂1-6", "Zetaタワー"},
                {"C007", "株式会社Eta", "eta@example.com", "03-7777-7777", "1000007", "東京都新宿区西新宿1-7", "Etaビル"},
                {"C008", "株式会社Theta", "theta@example.com", "03-8888-8888", "1000008", "東京都渋谷区渋谷1-8", "Thetaタワー"},
                {"C009", "株式会社Iota", "iota@example.com", "03-9999-9999", "1000009", "東京都豊島区池袋1-9", "Iotaビル"},
                {"C010", "株式会社Kappa", "kappa@example.com", "03-0000-0000", "1000010", "東京都品川区大崎1-10", "Kappaビル"}
            };
            
            for (String[] customer : customers) {
                ps.setString(1, customer[0]);
                ps.setString(2, customer[1]);
                ps.setString(3, customer[2]);
                ps.setString(4, customer[3]);
                ps.setString(5, customer[4]);
                ps.setString(6, customer[5]);
                ps.setString(7, customer[6]);
                ps.executeUpdate();
            }
        }
        System.out.println("Inserted 10 customers.");
        
        /** 5. 顧客担当者（10件） */
        List<String> customerIds = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM customers ORDER BY company_code")) {
            while (rs.next()) {
                customerIds.add(rs.getString("id"));
            }
        }
        
        String sqlContact = "INSERT INTO customer_contacts " +
            "(mail, password, customer_id, name, name_ruby, phone, department, is_primary) " +
            "VALUES (?, ?, ?::uuid, ?, ?, ?, ?, ?)";
        
        List<String> contactIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sqlContact, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < customerIds.size(); i++) {
                int g = i + 1;
                ps.setString(1, "contact" + g + "@example.com");
                ps.setString(2, hashedPassword);
                ps.setString(3, customerIds.get(i));
                ps.setString(4, "担当者" + g);
                ps.setString(5, "たんとうしゃ" + g);
                ps.setString(6, "090-0000-00" + g);
                ps.setString(7, "部署" + g);
                ps.setBoolean(8, true);
                ps.executeUpdate();
                
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        contactIds.add(rs.getString(1));
                    }
                }
            }
        }
        System.out.println("Inserted 10 customer contacts.");
        
        /** 6. customers.primary_contact_id を更新 */
        String sqlUpdateCustomer = "UPDATE customers SET primary_contact_id = ?::uuid WHERE id = ?::uuid";
        try (PreparedStatement ps = conn.prepareStatement(sqlUpdateCustomer)) {
            for (int i = 0; i < customerIds.size(); i++) {
                ps.setString(1, contactIds.get(i));
                ps.setString(2, customerIds.get(i));
                ps.executeUpdate();
            }
        }
        System.out.println("Updated customer primary contacts.");
        
        /** 7. 業務ランクマスタ（5件） */
        String sqlTaskRank = "INSERT INTO task_rank (rank_name, rank_no, base_pay_customer, base_pay_secretary) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlTaskRank)) {
            ps.setString(1, "P"); ps.setInt(2, 0); ps.setBigDecimal(3, new java.math.BigDecimal("2500")); ps.setBigDecimal(4, new java.math.BigDecimal("1500")); ps.executeUpdate();
            ps.setString(1, "A"); ps.setInt(2, 10); ps.setBigDecimal(3, new java.math.BigDecimal("2500")); ps.setBigDecimal(4, new java.math.BigDecimal("1500")); ps.executeUpdate();
            ps.setString(1, "B"); ps.setInt(2, 20); ps.setBigDecimal(3, new java.math.BigDecimal("2300")); ps.setBigDecimal(4, new java.math.BigDecimal("1400")); ps.executeUpdate();
            ps.setString(1, "C"); ps.setInt(2, 30); ps.setBigDecimal(3, new java.math.BigDecimal("2200")); ps.setBigDecimal(4, new java.math.BigDecimal("1300")); ps.executeUpdate();
            ps.setString(1, "D"); ps.setInt(2, 40); ps.setBigDecimal(3, new java.math.BigDecimal("2000")); ps.setBigDecimal(4, new java.math.BigDecimal("1100")); ps.executeUpdate();
        }
        System.out.println("Inserted 5 task ranks.");
        
        System.out.println("Initial data insertion completed.");
    }
}
