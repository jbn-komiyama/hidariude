-- ===============================================
-- Database Schema for Hidariude
-- ===============================================

-- UUID�����p�g��
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ===============================================
-- �e�[�u����`
-- ===============================================


-- �V�X�e���Ǘ���
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

-- �鏑�����N�}�X�^
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

-- �鏑
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

-- 2. �ڋq�e�[�u��
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
    primary_contact_id UUID, -- ��S����ID�i�ォ��FK����𒣂�j
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 3. �ڋq�S���҃e�[�u��
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

-- 4. �C���f�b�N�X
CREATE INDEX idx_customer_contacts_customer_id
    ON customer_contacts (customer_id);

-- 5. �ڋq �� �ڋq�S���� �����O���L�[
ALTER TABLE customers
  ADD CONSTRAINT fk_customers_primary_contact
  FOREIGN KEY (primary_contact_id, id)
  REFERENCES customer_contacts (id, customer_id)
  ON UPDATE CASCADE
  ON DELETE SET NULL;

-- �Ɩ������N�}�X�^
CREATE TABLE task_rank (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rank_name VARCHAR(50) NOT NULL,
    base_pay_customer DECIMAL(10,2),
    base_pay_secretary DECIMAL(10,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- �����A�T�C��
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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- �ڋq����������
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

-- �鏑�����Ɩ��T�}��
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

-- �Ɩ��Ǘ�
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
-- �����f�[�^�����X�N���v�g
-- �iDDL�͊��ɍ쐬�ςݑO��j
-- =========================

-- 1) system_admins (10��)
INSERT INTO system_admins (mail, password, name, name_ruby) VALUES
('admin1@example.com', 'password1', '�Ǘ���1', '����肵�Ⴂ��'),
('admin2@example.com', 'password2', '�Ǘ���2', '����肵���'),
('admin3@example.com', 'password3', '�Ǘ���3', '����肵�Ⴓ��'),
('admin4@example.com', 'password4', '�Ǘ���4', '����肵����'),
('admin5@example.com', 'password5', '�Ǘ���5', '����肵�Ⴒ'),
('admin6@example.com', 'password6', '�Ǘ���6', '����肵��낭'),
('admin7@example.com', 'password7', '�Ǘ���7', '����肵��Ȃ�'),
('admin8@example.com', 'password8', '�Ǘ���8', '����肵��͂�'),
('admin9@example.com', 'password9', '�Ǘ���9', '����肵�Ⴋ�イ'),
('admin10@example.com', 'password10', '�Ǘ���10', '����肵�Ⴖ�イ');

-- 2) �鏑�����N�}�X�^ (3��)
INSERT INTO secretary_rank (rank_name, description, increase_base_pay_customer, increase_base_pay_secretary) VALUES
('����', '���S��', 0, 0),
('����', '������', 100, 50),
('�㋉', '�㋉��', 200, 100);

-- 3) �鏑 (10���A�����N�͏z����)
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
  '�鏑' || g                                        AS name,
  '�Ђ���' || g                                      AS name_ruby,
  '03-1234-56' || g                                 AS phone,
  LPAD((1000000 + g)::text, 7, '0')                 AS postal_code,
  '�����s�`��e�X�g' || g                            AS address1
FROM nums n
JOIN cnt  c ON TRUE
JOIN ranks r
  ON ((n.g - 1) % c.c) + 1 = r.rn;

-- 4) �ڋq (10��)
INSERT INTO customers (company_code, company_name, mail, phone, postal_code, address1, building) VALUES
('C001', '�������Alpha',  'alpha@example.com',  '03-1111-1111', '1000001', '�����s���c����c1-1', 'Alpha�r��'),
('C002', '�������Beta',   'beta@example.com',   '03-2222-2222', '1000002', '�����s���c��ۂ̓�1-2', 'Beta�^���['),
('C003', '�������Gamma',  'gamma@example.com',  '03-3333-3333', '1000003', '�����s��������{��1-3', 'Gamma�r��'),
('C004', '�������Delta',  'delta@example.com',  '03-4444-4444', '1000004', '�����s��������1-4', 'Delta�^���['),
('C005', '�������Epsilon','epsilon@example.com','03-5555-5555','1000005','�����s�`��Z�{��1-5','Epsilon�r��'),
('C006', '�������Zeta',   'zeta@example.com',   '03-6666-6666', '1000006', '�����s�`��ԍ�1-6', 'Zeta�^���['),
('C007', '�������Eta',    'eta@example.com',    '03-7777-7777', '1000007', '�����s�V�h�搼�V�h1-7', 'Eta�r��'),
('C008', '�������Theta',  'theta@example.com',  '03-8888-8888', '1000008', '�����s�a�J��a�J1-8', 'Theta�^���['),
('C009', '�������Iota',   'iota@example.com',   '03-9999-9999', '1000009', '�����s�L����r��1-9', 'Iota�r��'),
('C010', '�������Kappa',  'kappa@example.com',  '03-0000-0000', '1000010', '�����s�i�����1-10', 'Kappa�r��');

-- 5) �ڋq�S���� (10���A�e�ڋq��1���E��S���ɐݒ�)
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
  '�S����' || g,
  '����Ƃ�����' || g,
  '090-0000-00' || g,
  '����' || g,
  TRUE  -- �e�ڋq1���Ȃ̂Ŏ�S���ɂ��Ă���
FROM numbered_customers;

-- 6) customers.primary_contact_id ��S����ID�Ŗ��߂�iB�Ă̊́j
UPDATE customers c
SET primary_contact_id = cc.id
FROM customer_contacts cc
WHERE cc.customer_id = c.id
  AND c.primary_contact_id IS NULL
  AND cc.is_primary = TRUE;

-- 7) �Ɩ������N�}�X�^ (5��)
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
    
UPDATE secretaries SET name = '���� �Ԏq', name_ruby = '�T�g�E �n�i�R' WHERE secretary_code = 'S001';
UPDATE secretaries SET name = '��� ����', name_ruby = '�X�Y�L �~�T�L' WHERE secretary_code = 'S002';
UPDATE secretaries SET name = '���� ����', name_ruby = '�^�J�n�V ���C' WHERE secretary_code = 'S003';
UPDATE secretaries SET name = '�c�� �ʍ�', name_ruby = '�^�i�J �A���J' WHERE secretary_code = 'S004';
UPDATE secretaries SET name = '�ɓ� ����', name_ruby = '�C�g�E �~�z' WHERE secretary_code = 'S005';
UPDATE secretaries SET name = '�n�� �^�R', name_ruby = '���^�i�x �}��' WHERE secretary_code = 'S006';
UPDATE secretaries SET name = '���� �b',   name_ruby = '�i�J���� ���O�~' WHERE secretary_code = 'S007';
UPDATE secretaries SET name = '���� ����', name_ruby = '�R�o���V �J�i' WHERE secretary_code = 'S008';
UPDATE secretaries SET name = '���� ���a', name_ruby = '�J�g�E �~��' WHERE secretary_code = 'S009';
UPDATE secretaries SET name = '�R�{ ����', name_ruby = '���}���g ���T' WHERE secretary_code = 'S010';

CREATE UNIQUE INDEX IF NOT EXISTS uq_assignments_ym_cust_sec_rank
ON assignments (target_year_month, customer_id, secretary_id, task_rank_id)
WHERE deleted_at IS NULL;
