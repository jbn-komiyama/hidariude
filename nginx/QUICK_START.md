# nginx リバースプロキシ クイックスタートガイド

## 必要なコマンド（まとめ）

### 1. セットアップスクリプトの実行（推奨）

```bash
# プロジェクトディレクトリに移動
cd /opt/hidariude

# セットアップスクリプトに実行権限を付与
chmod +x nginx/setup_nginx.sh

# セットアップスクリプトを実行（rootまたはsudo）
sudo ./nginx/setup_nginx.sh
```

### 2. 手動セットアップ（ステップバイステップ）

#### ステップ 1: nginx と certbot のインストール

```bash
sudo dnf install -y nginx certbot python3-certbot-nginx
```

#### ステップ 2: ファイアウォール設定

```bash
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

#### ステップ 3: certbot 用ディレクトリ作成

```bash
sudo mkdir -p /var/www/certbot
```

#### ステップ 4: nginx 設定ファイルの配置

```bash
cd /opt/hidariude
sudo cp nginx/hidariude.conf /etc/nginx/conf.d/hidariude.conf
sudo nginx -t
```

#### ステップ 5: nginx 起動

```bash
sudo systemctl enable nginx
sudo systemctl start nginx
sudo systemctl status nginx
```

#### ステップ 6: Let's Encrypt 証明書取得

```bash
sudo certbot --nginx -d ourdesk.n-learning.jp
```

**対話的な設定**:

-   Email address: 証明書の有効期限通知を受け取るメールアドレスを入力
-   Agree to Terms: Y
-   Share email address: 任意（N 推奨）

#### ステップ 7: 最終的な設定ファイルを再適用

```bash
cd /opt/hidariude
sudo cp nginx/hidariude.conf /etc/nginx/conf.d/hidariude.conf
sudo nginx -t
sudo systemctl reload nginx
```

## DNS 設定

ドメイン `ourdesk.n-learning.jp` の DNS 設定を確認：

```bash
# DNS解決確認
nslookup ourdesk.n-learning.jp
# または
dig ourdesk.n-learning.jp +short

# サーバーのIPアドレス確認
hostname -I
```

DNS の A レコードがサーバーの IP アドレスを指していることを確認してください。

## 動作確認

### HTTPS アクセステスト

```bash
# HTTPSでアクセスできるか確認
curl -I https://ourdesk.n-learning.jp

# 証明書の有効期限確認
echo | openssl s_client -connect ourdesk.n-learning.jp:443 -servername ourdesk.n-learning.jp 2>/dev/null | openssl x509 -noout -dates
```

### ログインレート制限テスト

```bash
# ログインエンドポイントへの連続POSTリクエスト
# 11回目以降で429エラーが返されるはず
for i in {1..15}; do
  curl -X POST https://ourdesk.n-learning.jp/admin/login \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "email=test@example.com&password=test" \
    -w "\nHTTP Status: %{http_code}\n" \
    -s -o /dev/null
  sleep 1
done
```

## ログ確認

```bash
# nginxアクセスログ
sudo tail -f /var/log/nginx/hidariude_access.log

# nginxエラーログ
sudo tail -f /var/log/nginx/hidariude_error.log

# ログイン試行ログ（レート制限対象）
sudo tail -f /var/log/nginx/hidariude_login_attempts.log
```

## よくある問題

### 証明書取得失敗

1. DNS 設定を確認
2. ポート 80 が開いているか確認
3. 手動で証明書取得を試行：
    ```bash
    sudo certbot certonly --standalone -d ourdesk.n-learning.jp
    ```

### nginx 設定エラー

```bash
# 設定をテスト
sudo nginx -t

# エラーがある場合は設定ファイルを確認
sudo vi /etc/nginx/conf.d/hidariude.conf
```

### Tomcat への接続エラー

```bash
# Tomcatが起動しているか確認
sudo systemctl status tomcat

# ポート8080でリッスンしているか確認
sudo netstat -tlnp | grep :8080
```

## 設定内容の詳細

### ログイン回数制限

-   **対象エンドポイント**: `/admin/login`, `/secretary/login`, `/customer/login`
-   **対象メソッド**: POST リクエストのみ
-   **制限**: 1 分間に 10 リクエスト（`rate=10r/m`）
-   **バースト**: 2 リクエストまで即座に処理可能（`burst=2`）
-   **超過時のステータス**: 429 Too Many Requests

### SSL/TLS 設定

-   **プロトコル**: TLSv1.2 および TLSv1.3 のみ
-   **セッションキャッシュ**: 10 分間保持
-   **HSTS**: 1 年間有効（サブドメイン含む）

## 証明書の自動更新

Let's Encrypt 証明書は 90 日で有効期限が切れるため、自動更新を設定：

```bash
# 更新テスト（ドライラン）
sudo certbot renew --dry-run

# 自動更新の設定確認（systemd timerが自動的に設定される）
sudo systemctl list-timers | grep certbot
```

certbot は自動的に systemd timer を設定するため、手動での設定は通常不要です。
