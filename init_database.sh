#!/bin/bash
#####################################################################
# Hidariude データベース初期化スクリプト (AlmaLinux 10)
# 
# 概要: PostgreSQLデータベースにDDLとダミーデータを投入します
#
# 前提条件:
# - PostgreSQL 15 がポート5433で稼働中
# - データベース「hidariude」が作成済み
# - ユーザー「postgres」にパスワード「password」が設定済み
#####################################################################

set -e  # エラー時に即座に終了

# ログ出力用の色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ログ関数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_sql() {
    echo -e "${BLUE}[SQL]${NC} $1"
}

#####################################################################
# データベース設定
#####################################################################

DB_HOST="localhost"
DB_PORT="5433"
DB_NAME="hidariude"
DB_USER="postgres"
DB_PASSWORD="password"
PGPASSWORD="$DB_PASSWORD"
export PGPASSWORD

# プロジェクト設定
PROJECT_DIR="/opt/hidariude"
SQL_FILE="$PROJECT_DIR/src/main/sql/hoshiiro.sql"

#####################################################################
# 事前確認
#####################################################################

log_info "=== データベース初期化スクリプト ==="
log_info ""

# PostgreSQL確認
if ! systemctl is-active --quiet postgresql-15; then
    log_error "PostgreSQL が起動していません"
    log_info "起動してください: systemctl start postgresql-15"
    exit 1
fi
log_info "PostgreSQL ステータス: $(systemctl is-active postgresql-15)"

# psqlコマンド確認
if ! command -v psql &> /dev/null; then
    log_error "psql コマンドが見つかりません"
    log_info "PostgreSQLクライアントをインストールしてください"
    exit 1
fi

# SQLファイル確認
if [ ! -f "$SQL_FILE" ]; then
    log_error "SQLファイルが見つかりません: $SQL_FILE"
    exit 1
fi
log_info "SQLファイル: $SQL_FILE"

#####################################################################
# データベース接続確認
#####################################################################

log_info "=== データベース接続確認 ==="

if ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1; then
    log_error "データベースに接続できません"
    log_error "接続情報を確認してください:"
    log_error "  ホスト: $DB_HOST"
    log_error "  ポート: $DB_PORT"
    log_error "  データベース: $DB_NAME"
    log_error "  ユーザー: $DB_USER"
    exit 1
fi

log_info "データベース接続成功"

#####################################################################
# 初期化確認
#####################################################################

log_warn ""
log_warn "================================================"
log_warn "警告: このスクリプトは既存のテーブルを削除します"
log_warn "================================================"
log_warn "データベース: $DB_NAME@$DB_HOST:$DB_PORT"
log_warn ""

# 既存テーブルの確認
TABLE_COUNT=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "
    SELECT COUNT(*) 
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_type = 'BASE TABLE';
" | xargs)

if [ "$TABLE_COUNT" -gt 0 ]; then
    log_warn "既存のテーブル数: $TABLE_COUNT"
    log_warn ""
    log_warn "既存のテーブル一覧:"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public' 
        ORDER BY tablename;
    "
    log_warn ""
fi

# 実行確認（インタラクティブモード）
if [ -t 0 ]; then
    read -p "続行しますか？ [y/N]: " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "処理を中断しました"
        exit 0
    fi
else
    log_warn "非インタラクティブモードで実行します（5秒後に開始）"
    sleep 5
fi

#####################################################################
# 既存テーブルの削除
#####################################################################

log_info "=== 既存テーブルの削除 ==="

if [ "$TABLE_COUNT" -gt 0 ]; then
    log_info "既存のテーブルを削除中..."
    
    # テーブル削除順序（外部キー制約を考慮）
    TABLES=(
        "tasks"
        "secretary_monthly_summaries"
        "customer_monthly_invoices"
        "assignments"
        "profiles"
        "customer_contacts"
        "customers"
        "secretaries"
        "secretary_rank"
        "task_rank"
        "system_admins"
    )
    
    for TABLE in "${TABLES[@]}"; do
        TABLE_EXISTS=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "
            SELECT EXISTS (
                SELECT 1 FROM information_schema.tables 
                WHERE table_schema = 'public' 
                AND table_name = '$TABLE'
            );
        " | xargs)
        
        if [ "$TABLE_EXISTS" = "t" ]; then
            log_sql "DROP TABLE IF EXISTS $TABLE CASCADE"
            psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "DROP TABLE IF EXISTS $TABLE CASCADE;" > /dev/null
        fi
    done
    
    # ドメインの削除
    log_sql "DROP DOMAIN IF EXISTS availability_flag CASCADE"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "DROP DOMAIN IF EXISTS availability_flag CASCADE;" > /dev/null 2>&1 || true
    
    log_info "既存テーブルの削除完了"
else
    log_info "削除対象のテーブルはありません"
fi

#####################################################################
# DDL実行
#####################################################################

log_info "=== DDLの実行 ==="

log_info "SQLファイルを実行中: $SQL_FILE"

# SQLファイルを実行
if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$SQL_FILE" > /tmp/sql_output.log 2>&1; then
    log_info "DDL実行完了"
else
    log_error "DDL実行中にエラーが発生しました"
    log_error "詳細ログ: /tmp/sql_output.log"
    cat /tmp/sql_output.log
    exit 1
fi

#####################################################################
# 実行結果確認
#####################################################################

log_info "=== 実行結果確認 ==="

# テーブル数確認
NEW_TABLE_COUNT=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "
    SELECT COUNT(*) 
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_type = 'BASE TABLE';
" | xargs)

log_info "作成されたテーブル数: $NEW_TABLE_COUNT"
log_info ""

# テーブル一覧とレコード数
log_info "テーブル一覧とレコード数:"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
    SELECT 
        schemaname,
        tablename,
        (xpath('/row/cnt/text()', xml_count))[1]::text::int as row_count
    FROM (
        SELECT 
            table_schema as schemaname,
            table_name as tablename,
            table_name,
            query_to_xml(format('SELECT COUNT(*) as cnt FROM %I.%I', table_schema, table_name), false, true, '') as xml_count
        FROM information_schema.tables
        WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
    ) t
    ORDER BY tablename;
"

#####################################################################
# 初期データ確認
#####################################################################

log_info "=== 初期データ確認 ==="

# システム管理者
ADMIN_COUNT=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM system_admins;" | xargs)
log_info "システム管理者: $ADMIN_COUNT 件"

# 秘書
SECRETARY_COUNT=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM secretaries;" | xargs)
log_info "秘書: $SECRETARY_COUNT 件"

# 顧客
CUSTOMER_COUNT=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM customers;" | xargs)
log_info "顧客: $CUSTOMER_COUNT 件"

# 顧客担当者
CONTACT_COUNT=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM customer_contacts;" | xargs)
log_info "顧客担当者: $CONTACT_COUNT 件"

#####################################################################
# 完了
#####################################################################

log_info ""
log_info "=== データベース初期化完了 ==="
log_info "データベース: $DB_NAME"
log_info "ホスト: $DB_HOST:$DB_PORT"
log_info "テーブル数: $NEW_TABLE_COUNT"
log_info ""
log_info "接続確認:"
log_info "  psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME"
log_info ""
log_info "次のステップ:"
log_info "  ./deploy.sh を実行してアプリケーションをデプロイしてください"

# パスワード環境変数をクリア
unset PGPASSWORD

exit 0

