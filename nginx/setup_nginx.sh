#!/bin/bash
#####################################################################
# nginx設定スクリプト
# 
# 概要: nginxリバースプロキシのセットアップを自動化します
#      - nginxインストール
#      - ファイアウォール設定
#      - Let's Encrypt証明書取得
#      - 設定ファイルの配置
#
# 前提条件:
# - ドメイン ourdesk.n-learning.jp のDNS設定が完了していること
# - rootまたはsudo権限があること
#####################################################################

set -e  # エラー時に即座に終了

# ログ出力用の色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

# 設定
DOMAIN="ourdesk.n-learning.jp"
PROJECT_DIR="/opt/hidariude"
NGINX_CONF_DIR="/etc/nginx/conf.d"
NGINX_CONF_FILE="${NGINX_CONF_DIR}/hidariude.conf"
CERTBOT_WEBROOT="/var/www/certbot"

#####################################################################
# nginxインストール
#####################################################################

log_info "=== nginxインストール ==="

if ! command -v nginx &> /dev/null; then
    log_info "nginxをインストール中..."
    dnf install -y nginx
    log_info "nginxインストール完了"
else
    log_info "nginxは既にインストール済み: $(nginx -v 2>&1)"
fi

#####################################################################
# certbotインストール
#####################################################################

log_info "=== certbotインストール ==="

if ! command -v certbot &> /dev/null; then
    log_info "EPELリポジトリを有効化中..."
    dnf install -y epel-release || true
    
    log_info "certbotをインストール中..."
    dnf install -y certbot python3-certbot-nginx
    log_info "certbotインストール完了: $(certbot --version)"
else
    log_info "certbotは既にインストール済み: $(certbot --version)"
fi

#####################################################################
# ファイアウォール設定
#####################################################################

log_info "=== ファイアウォール設定 ==="

if systemctl is-active --quiet firewalld; then
    log_info "HTTP/HTTPSポートを開放中..."
    firewall-cmd --permanent --add-service=http || true
    firewall-cmd --permanent --add-service=https || true
    firewall-cmd --reload
    log_info "ファイアウォール設定完了"
else
    log_warn "firewalldが起動していません。手動でポート80/443を開放してください"
fi

#####################################################################
# certbot用ディレクトリ作成
#####################################################################

log_info "=== certbot用ディレクトリ作成 ==="
mkdir -p "$CERTBOT_WEBROOT"
log_info "ディレクトリ作成完了: $CERTBOT_WEBROOT"

#####################################################################
# nginx設定ファイルの配置
#####################################################################

log_info "=== nginx設定ファイルの配置 ==="

if [ ! -f "${PROJECT_DIR}/nginx/hidariude.conf" ]; then
    log_error "nginx設定ファイルが見つかりません: ${PROJECT_DIR}/nginx/hidariude.conf"
    exit 1
fi

log_info "nginx設定ファイルをコピー中..."
cp "${PROJECT_DIR}/nginx/hidariude.conf" "$NGINX_CONF_FILE"
log_info "設定ファイル配置完了: $NGINX_CONF_FILE"

# nginx設定をテスト
log_info "nginx設定をテスト中..."
if nginx -t; then
    log_info "nginx設定テスト成功"
else
    log_error "nginx設定テスト失敗"
    exit 1
fi

#####################################################################
# nginx起動
#####################################################################

log_info "=== nginx起動 ==="

systemctl enable nginx
if systemctl is-active --quiet nginx; then
    log_info "nginxを再起動中..."
    systemctl restart nginx
else
    log_info "nginxを起動中..."
    systemctl start nginx
fi

sleep 2

if systemctl is-active --quiet nginx; then
    log_info "nginx起動完了"
else
    log_error "nginx起動失敗"
    exit 1
fi

#####################################################################
# Let's Encrypt証明書取得
#####################################################################

log_info "=== Let's Encrypt証明書取得 ==="

SSL_CERT_PATH="/etc/letsencrypt/live/${DOMAIN}/fullchain.pem"

if [ -f "$SSL_CERT_PATH" ]; then
    log_info "証明書は既に存在します: $SSL_CERT_PATH"
    log_info "証明書の有効期限を確認中..."
    openssl x509 -in "$SSL_CERT_PATH" -noout -dates 2>/dev/null || true
    log_warn "証明書を再取得する場合は手動で実行してください:"
    log_warn "  certbot --nginx -d ${DOMAIN} --force-renewal"
else
    log_info "証明書を取得中..."
    log_warn "以下の対話で、メールアドレスと利用規約への同意が求められます"
    certbot --nginx -d "$DOMAIN" --non-interactive --agree-tos --email admin@${DOMAIN} || {
        log_error "証明書取得に失敗しました。DNS設定を確認してください"
        log_error "  - ドメイン ${DOMAIN} がこのサーバーのIPアドレスを指しているか確認"
        log_error "  - ポート80が外部からアクセス可能か確認"
        exit 1
    }
    log_info "証明書取得完了"
fi

#####################################################################
# 最終的なnginx設定の適用
#####################################################################

log_info "=== 最終的なnginx設定の適用 ==="

# certbotが証明書を取得した後、完全な設定ファイルを再適用
log_info "ログイン制限を含む完全な設定ファイルを適用中..."
cp "${PROJECT_DIR}/nginx/hidariude.conf" "$NGINX_CONF_FILE"

# nginx設定をテスト
if nginx -t; then
    log_info "nginx設定テスト成功"
    systemctl reload nginx
    log_info "nginx設定を再読み込みしました"
else
    log_error "nginx設定テスト失敗。設定ファイルを確認してください: $NGINX_CONF_FILE"
    exit 1
fi

#####################################################################
# 完了
#####################################################################

log_info "=== nginxセットアップ完了 ==="
log_info "ドメイン: ${DOMAIN}"
log_info "HTTPS URL: https://${DOMAIN}"
log_info ""
log_info "動作確認コマンド:"
log_info "  curl -I https://${DOMAIN}"
log_info ""
log_info "nginxログ確認:"
log_info "  tail -f /var/log/nginx/hidariude_access.log"
log_info "  tail -f /var/log/nginx/hidariude_error.log"
log_info ""
log_info "nginxステータス:"
log_info "  systemctl status nginx"
log_info ""
log_info "証明書の自動更新確認:"
log_info "  certbot renew --dry-run"

exit 0

