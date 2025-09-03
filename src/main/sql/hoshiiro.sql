-- ===============================================
-- Database Schema for Hidariude
-- ===============================================

-- UUID生成用拡張
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ===============================================
-- テーブル定義
-- ===============================================


-- システム管理者
CREATE TABLE system_admins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mail VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    name_ruby VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    last_login_at TIMESTAMP
);

-- 秘書ランクマスタ
CREATE TABLE secretary_rank (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rank_name VARCHAR(255) NOT NULL,
    description TEXT,
    increase_base_pay_customer DECIMAL(10,2),
    increase_base_pay_secretary DECIMAL(10,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 秘書
CREATE TABLE secretaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    secretary_code VARCHAR(255) UNIQUE NOT NULL,
    mail VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    secretary_rank_id UUID REFERENCES secretary_rank(id),
    is_pm_secretary BOOLEAN DEFAULT FALSE,
    name VARCHAR(255) NOT NULL,
    name_ruby VARCHAR(255),
    phone VARCHAR(50),
    postal_code VARCHAR(20),
    address1 VARCHAR(255),
    address2 VARCHAR(255),
    building VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    last_login_at TIMESTAMP
);

-- 2. 顧客テーブル
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_code VARCHAR(255) UNIQUE NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    mail VARCHAR(255),
    phone VARCHAR(50),
    postal_code VARCHAR(20),
    address1 VARCHAR(255),
    address2 VARCHAR(255),
    building VARCHAR(255),
    primary_contact_id UUID, -- 主担当者ID（後からFK制約を張る）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 3. 顧客担当者テーブル
CREATE TABLE customer_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mail VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    customer_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    name_ruby VARCHAR(255),
    phone VARCHAR(50),
    department VARCHAR(255),
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    last_login_at TIMESTAMP,
    CONSTRAINT fk_customer_contacts_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT uq_customer_contacts_id_customer
        UNIQUE (id, customer_id)
);

-- 4. インデックス
CREATE INDEX idx_customer_contacts_customer_id
    ON customer_contacts (customer_id);

-- 5. 顧客 → 顧客担当者 複合外部キー
ALTER TABLE customers
  ADD CONSTRAINT fk_customers_primary_contact
  FOREIGN KEY (primary_contact_id, id)
  REFERENCES customer_contacts (id, customer_id)
  ON UPDATE CASCADE
  ON DELETE SET NULL;

-- 業務ランクマスタ
CREATE TABLE task_rank (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rank_name VARCHAR(50) NOT NULL,
    base_pay_customer DECIMAL(10,2),
    base_pay_secretary DECIMAL(10,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 月次アサイン
CREATE TABLE assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customers(id),
    secretary_id UUID REFERENCES secretaries(id),
    task_rank_id UUID REFERENCES task_rank(id),
    target_year_month VARCHAR(7) NOT NULL,
    base_pay_customer DECIMAL(10,2),
    base_pay_secretary DECIMAL(10,2),
    increase_base_pay_customer DECIMAL(10,2),
    increase_base_pay_secretary DECIMAL(10,2),
    customer_based_incentive_for_customer DECIMAL(10,2),
    customer_based_incentive_for_secretary DECIMAL(10,2),
    status VARCHAR(20),
    created_by UUID REFERENCES secretaries(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 顧客月次請求書
CREATE TABLE customer_monthly_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customers(id),
    target_year_month VARCHAR(7) NOT NULL,
    total_amount DECIMAL(12,2),
    total_tasks_count INTEGER,
    total_work_time INTEGER,
    status VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 秘書月次業務サマリ
CREATE TABLE secretary_monthly_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    secretary_id UUID REFERENCES secretaries(id),
    target_year_month VARCHAR(7) NOT NULL,
    total_secretary_amount DECIMAL(12,2),
    total_tasks_count INTEGER,
    total_work_time INTEGER,
    finalized_at TIMESTAMP,
    status VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 業務管理
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID REFERENCES assignments(id),
    work_date DATE NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    work_minute INTEGER,
    work_content TEXT,
    approved_at TIMESTAMP,
    approved_by UUID REFERENCES secretaries(id),
    customer_monthly_invoice_id UUID REFERENCES customer_monthly_invoices(id),
    secretary_monthly_summary_id UUID REFERENCES secretary_monthly_summaries(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);



-- =========================
-- 初期データ投入スクリプト
-- （DDLは既に作成済み前提）
-- =========================

-- 1) system_admins (10件)
INSERT INTO system_admins (mail, password, name, name_ruby) VALUES
('admin1@example.com', 'password1', '管理者1', 'かんりしゃいち'),
('admin2@example.com', 'password2', '管理者2', 'かんりしゃに'),
('admin3@example.com', 'password3', '管理者3', 'かんりしゃさん'),
('admin4@example.com', 'password4', '管理者4', 'かんりしゃよん'),
('admin5@example.com', 'password5', '管理者5', 'かんりしゃご'),
('admin6@example.com', 'password6', '管理者6', 'かんりしゃろく'),
('admin7@example.com', 'password7', '管理者7', 'かんりしゃなな'),
('admin8@example.com', 'password8', '管理者8', 'かんりしゃはち'),
('admin9@example.com', 'password9', '管理者9', 'かんりしゃきゅう'),
('admin10@example.com', 'password10', '管理者10', 'かんりしゃじゅう');

-- 2) 秘書ランクマスタ (3件)
INSERT INTO secretary_rank (rank_name, description, increase_base_pay_customer, increase_base_pay_secretary) VALUES
('初級', '初心者', 0, 0),
('中級', '中級者', 100, 50),
('上級', '上級者', 200, 100);

-- 3) 秘書 (10件、ランクは循環割当)
WITH
ranks AS (
  SELECT id, ROW_NUMBER() OVER (ORDER BY rank_name) AS rn
  FROM secretary_rank
),
cnt AS (
  SELECT COUNT(*)::int AS c FROM ranks
),
nums AS (
  SELECT generate_series(1,10) AS g
)
INSERT INTO secretaries
(secretary_code, mail, password, secretary_rank_id, is_pm_secretary,
 name, name_ruby, phone, postal_code, address1)
SELECT
  'S' || LPAD(g::text, 3, '0')                      AS secretary_code,
  'secretary' || g || '@example.com'                AS mail,
  'password' || g                                   AS password,
  r.id                                              AS secretary_rank_id,
  CASE WHEN g % 2 = 0 THEN TRUE ELSE FALSE END      AS is_pm_secretary,
  '秘書' || g                                        AS name,
  'ひしょ' || g                                      AS name_ruby,
  '03-1234-56' || g                                 AS phone,
  LPAD((1000000 + g)::text, 7, '0')                 AS postal_code,
  '東京都港区テスト' || g                            AS address1
FROM nums n
JOIN cnt  c ON TRUE
JOIN ranks r
  ON ((n.g - 1) % c.c) + 1 = r.rn;

-- 4) 顧客 (10件)
INSERT INTO customers (company_code, company_name, mail, phone, postal_code, address1, building) VALUES
('C001', '株式会社Alpha',  'alpha@example.com',  '03-1111-1111', '1000001', '東京都千代田区千代田1-1', 'Alphaビル'),
('C002', '株式会社Beta',   'beta@example.com',   '03-2222-2222', '1000002', '東京都千代田区丸の内1-2', 'Betaタワー'),
('C003', '株式会社Gamma',  'gamma@example.com',  '03-3333-3333', '1000003', '東京都中央区日本橋1-3', 'Gammaビル'),
('C004', '株式会社Delta',  'delta@example.com',  '03-4444-4444', '1000004', '東京都中央区銀座1-4', 'Deltaタワー'),
('C005', '株式会社Epsilon','epsilon@example.com','03-5555-5555','1000005','東京都港区六本木1-5','Epsilonビル'),
('C006', '株式会社Zeta',   'zeta@example.com',   '03-6666-6666', '1000006', '東京都港区赤坂1-6', 'Zetaタワー'),
('C007', '株式会社Eta',    'eta@example.com',    '03-7777-7777', '1000007', '東京都新宿区西新宿1-7', 'Etaビル'),
('C008', '株式会社Theta',  'theta@example.com',  '03-8888-8888', '1000008', '東京都渋谷区渋谷1-8', 'Thetaタワー'),
('C009', '株式会社Iota',   'iota@example.com',   '03-9999-9999', '1000009', '東京都豊島区池袋1-9', 'Iotaビル'),
('C010', '株式会社Kappa',  'kappa@example.com',  '03-0000-0000', '1000010', '東京都品川区大崎1-10', 'Kappaビル');

-- 5) 顧客担当者 (10件、各顧客に1名・主担当に設定)
WITH numbered_customers AS (
  SELECT id, ROW_NUMBER() OVER (ORDER BY company_code) AS g
  FROM customers
)
INSERT INTO customer_contacts
(mail, password, customer_id, name, name_ruby, phone, department, is_primary)
SELECT
  'contact' || g || '@example.com',
  'password' || g,
  id,
  '担当者' || g,
  'たんとうしゃ' || g,
  '090-0000-00' || g,
  '部署' || g,
  TRUE  -- 各顧客1名なので主担当にしておく
FROM numbered_customers;

-- 6) customers.primary_contact_id を担当者IDで埋める（B案の肝）
UPDATE customers c
SET primary_contact_id = cc.id
FROM customer_contacts cc
WHERE cc.customer_id = c.id
  AND c.primary_contact_id IS NULL
  AND cc.is_primary = TRUE;

-- 7) 業務ランクマスタ (5件)
INSERT INTO task_rank (rank_name, base_pay_customer, base_pay_secretary) VALUES
('P', 2500, 1500),
('A', 2500, 1500),
('B', 2300, 1400),
('C', 2200, 1300),
('D', 2000, 1100);

ALTER TABLE assignments
    RENAME COLUMN created_by TO created_by_secretary;
ALTER TABLE assignments
    ADD COLUMN created_by_system_admin UUID REFERENCES system_admins(id);
    
UPDATE secretaries SET name = '佐藤 花子', name_ruby = 'サトウ ハナコ' WHERE secretary_code = 'S001';
UPDATE secretaries SET name = '鈴木 美咲', name_ruby = 'スズキ ミサキ' WHERE secretary_code = 'S002';
UPDATE secretaries SET name = '高橋 結衣', name_ruby = 'タカハシ ユイ' WHERE secretary_code = 'S003';
UPDATE secretaries SET name = '田中 彩香', name_ruby = 'タナカ アヤカ' WHERE secretary_code = 'S004';
UPDATE secretaries SET name = '伊藤 美穂', name_ruby = 'イトウ ミホ' WHERE secretary_code = 'S005';
UPDATE secretaries SET name = '渡辺 真由', name_ruby = 'ワタナベ マユ' WHERE secretary_code = 'S006';
UPDATE secretaries SET name = '中村 恵',   name_ruby = 'ナカムラ メグミ' WHERE secretary_code = 'S007';
UPDATE secretaries SET name = '小林 佳奈', name_ruby = 'コバヤシ カナ' WHERE secretary_code = 'S008';
UPDATE secretaries SET name = '加藤 美和', name_ruby = 'カトウ ミワ' WHERE secretary_code = 'S009';
UPDATE secretaries SET name = '山本 理沙', name_ruby = 'ヤマモト リサ' WHERE secretary_code = 'S010';

CREATE UNIQUE INDEX IF NOT EXISTS uq_assignments_ym_cust_sec_rank
ON assignments (target_year_month, customer_id, secretary_id, task_rank_id)
WHERE deleted_at IS NULL;

ALTER TABLE task_rank
ADD COLUMN rank_no INTEGER;
UPDATE task_rank SET rank_no = 0 WHERE rank_name = 'P';
UPDATE task_rank SET rank_no = 10 WHERE rank_name = 'A';
UPDATE task_rank SET rank_no = 20 WHERE rank_name = 'B';
UPDATE task_rank SET rank_no = 30 WHERE rank_name = 'C';
UPDATE task_rank SET rank_no = 40 WHERE rank_name = 'D';
