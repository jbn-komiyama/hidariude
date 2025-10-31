# nginx リバースプロキシ設定ガイド

## 概要

このドキュメントでは、Hidariude アプリケーションに nginx リバースプロキシを設定する手順を説明します。

## 前提条件

-   AlmaLinux 10 サーバー
-   root または sudo 権限
-   ドメイン `ourdesk.n-learning.jp` の DNS 設定が完了していること（A レコードがサーバーの IP アドレスを指していること）

## 1. nginx のインストール

```bash
# nginxをインストール
sudo dnf install -y nginx

# nginxのバージョン確認
nginx -v

# nginxを起動して自動起動を有効化
sudo systemctl enable nginx
sudo systemctl start nginx

# ステータス確認
sudo systemctl status nginx
```

## 2. ファイアウォール設定

```bash
# HTTP (80) と HTTPS (443) ポートを開放
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload

# ポート確認
sudo firewall-cmd --list-all
```

## 3. DNS 設定の確認

ドメイン `ourdesk.n-learning.jp` がサーバーの IP アドレスを指していることを確認：

```bash
# DNS解決確認
nslookup ourdesk.n-learning.jp
# または
dig ourdesk.n-learning.jp +short

# サーバーのIPアドレス確認
hostname -I
```

## 4. Let's Encrypt 証明書の取得（certbot インストール）

```bash
# EPELリポジトリを有効化（必要な場合）
sudo dnf install -y epel-release

# certbotをインストール
sudo dnf install -y certbot python3-certbot-nginx

# certbotのバージョン確認
certbot --version
```

## 5. 初期 nginx 設定の準備

Let's Encrypt の証明書取得前に、一時的な設定を作成：

```bash
# certbot用のディレクトリを作成
sudo mkdir -p /var/www/certbot

# 設定ファイルをコピー（一時的なHTTP設定を含む）
sudo cp nginx/hidariude.conf /etc/nginx/conf.d/hidariude.conf

# nginx設定をテスト
sudo nginx -t

# nginxを再起動
sudo systemctl restart nginx
```

## 6. Let's Encrypt 証明書の取得

```bash
# 証明書を取得（nginxプラグインを使用）
sudo certbot --nginx -d ourdesk.n-learning.jp

# 対話的な設定:
# - Email address: 証明書の有効期限通知を受け取るメールアドレスを入力
# - Agree to Terms: Y
# - Share email address: 任意（N推奨）
```

証明書取得後、certbot が自動的に nginx 設定を更新します。

## 7. 最終的な nginx 設定の適用

certbot で証明書を取得した後、完全な設定ファイルを適用：

```bash
# 設定ファイルをコピー（ログイン制限などの詳細設定を含む）
sudo cp nginx/hidariude.conf /etc/nginx/conf.d/hidariude.conf

# nginx設定をテスト
sudo nginx -t

# エラーがなければ、nginxを再読み込み
sudo systemctl reload nginx

# ステータス確認
sudo systemctl status nginx
```

## 8. 証明書の自動更新設定

Let's Encrypt 証明書は 90 日で有効期限が切れるため、自動更新を設定：

```bash
# 更新テスト（ドライラン）
sudo certbot renew --dry-run

# 自動更新の設定確認（systemd timerが自動的に設定される）
sudo systemctl list-timers | grep certbot
```

## 9. 動作確認

### HTTPS アクセステスト

```bash
# HTTPSでアクセスできるか確認
curl -I https://ourdesk.n-learning.jp

# 証明書の有効期限確認
echo | openssl s_client -connect ourdesk.n-learning.jp:443 -servername ourdesk.n-learning.jp 2>/dev/null | openssl x509 -noout -dates
```

### ログインレート制限テスト

```bash
# ログインエンドポイントへの連続リクエスト（制限をトリガー）
for i in {1..15}; do
  curl -X POST https://ourdesk.n-learning.jp/admin/login \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "email=test@example.com&password=test" \
    -w "\nHTTP Status: %{http_code}\n" \
    -s -o /dev/null
  sleep 1
done
```

15 回以上のリクエストを送信すると、11 回目以降は `429 Too Many Requests` が返されるはずです。

## 10. ログ確認

```bash
# nginxアクセスログ
sudo tail -f /var/log/nginx/hidariude_access.log

# nginxエラーログ
sudo tail -f /var/log/nginx/hidariude_error.log

# ログイン試行ログ（レート制限対象）
sudo tail -f /var/log/nginx/hidariude_login_attempts.log
```

## 設定の詳細説明

### ログイン回数制限

-   **制限対象**: `/admin/login`, `/secretary/login`, `/customer/login` への POST リクエスト
-   **レート**: 1 分間に 10 リクエスト（`rate=10r/m`）
-   **バースト**: 2 リクエストまで即座に処理可能（`burst=2`）
-   **超過時のステータス**: 429 Too Many Requests

### SSL/TLS 設定

-   **プロトコル**: TLSv1.2 および TLSv1.3 のみ
-   **セッションキャッシュ**: 10 分間保持
-   **HSTS**: 1 年間有効（サブドメイン含む）

### セキュリティヘッダー

-   `Strict-Transport-Security`: HTTPS 強制
-   `X-Frame-Options`: クリックジャッキング対策
-   `X-Content-Type-Options`: MIME タイプスニッフィング対策
-   `X-XSS-Protection`: XSS 対策

## トラブルシューティング

### nginx 設定テストエラー

```bash
# 設定ファイルの構文チェック
sudo nginx -t

# エラーが表示される場合は、設定ファイルを確認
sudo vi /etc/nginx/conf.d/hidariude.conf
```

### 証明書取得失敗

1. DNS 設定が正しいか確認：

    ```bash
    nslookup ourdesk.n-learning.jp
    ```

2. ポート 80 が開いているか確認：

    ```bash
    sudo firewall-cmd --list-all
    sudo netstat -tlnp | grep :80
    ```

3. 手動で証明書取得を試行：
    ```bash
    sudo certbot certonly --standalone -d ourdesk.n-learning.jp
    ```

### Tomcat への接続エラー

```bash
# Tomcatが起動しているか確認
sudo systemctl status tomcat

# ポート8080でリッスンしているか確認
sudo netstat -tlnp | grep :8080

# ローカルから直接アクセステスト
curl http://localhost:8080
```

### レート制限が効かない

1. nginx 設定を再読み込み：

    ```bash
    sudo systemctl reload nginx
    ```

2. ログを確認：

    ```bash
    sudo tail -f /var/log/nginx/hidariude_error.log
    ```

3. 設定ファイルの構文を確認：
    ```bash
    sudo nginx -t
    ```

## メンテナンス

### 証明書の手動更新

```bash
sudo certbot renew
sudo systemctl reload nginx
```

### nginx 設定の変更後

```bash
# 設定テスト
sudo nginx -t

# 問題がなければ再読み込み
sudo systemctl reload nginx
```

### ログローテーション

nginx のログは logrotate で自動的にローテーションされます。手動でローテーションする場合：

```bash
sudo logrotate -f /etc/logrotate.d/nginx
```

## 参考リンク

-   [nginx 公式ドキュメント](https://nginx.org/en/docs/)
-   [Let's Encrypt 公式サイト](https://letsencrypt.org/)
-   [certbot 公式ドキュメント](https://certbot.eff.org/)
