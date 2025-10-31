# Hidariude - Java Web アプリケーション

## 概要

このプロジェクトは Java Servlet ベースの Web アプリケーションで、VS Code での開発環境構築をサポートしています。

## 目次

-   [Part 1: Windows ローカル開発環境](#part-1-windows-ローカル開発環境)
-   [Part 2: AlmaLinux 本番デプロイ](#part-2-almalinux-本番デプロイ)
-   [Part 3: データベースマイグレーション](#part-3-データベースマイグレーション)

---

# Part 1: Windows ローカル開発環境

## 前提条件

以下のソフトウェアがインストール済みであることを確認してください：

### 必須環境

-   **Java 24** 以上
-   **Apache Maven** 3.6.3 以上
-   **Apache Tomcat** 10.1 以上
-   **PostgreSQL** 12 以上（ポート 5433 で稼働）
-   **Visual Studio Code**

### 動作確認環境

-   Windows 11
-   Java 24.0.2
-   Maven 3.9.11
-   Tomcat 10.1.x
-   PostgreSQL 15.x（ポート 5433）

---

## 前提条件が整っていない場合：インストールと初期設定

> ここから順番に実施してください。途中でエラーになったら、この章の最後にある「動作確認」のコマンドで確認しながら進めると安全です。

### 0. 事前メモ

-   以降、特に指定が無ければ **64bit Windows** を想定しています。
-   **管理者権限**でインストーラの実行を推奨します。
-   PATH（環境変数）を変更したら **一度ターミナル／VS Code を閉じて再起動**してください。

### 1. Git をインストールする

インストーラをダウンロードして実行：

```
https://github.com/git-for-windows/git/releases/download/v2.51.1.windows.1/Git-2.51.1-64-bit.exe
```

インストール後、動作確認（PowerShell または コマンドプロンプト）：

```powershell
git --version
```

### 2. VS Code をインストールする

インストーラをダウンロードして実行：

```
https://go.microsoft.com/fwlink/p/?linkid=2216501&clcid=0x411
```

起動後、拡張機能から **Japanese Language Pack for Visual Studio Code** をインストールして日本語化しておくと便利です。

### 3. 下記 4 つのツールを「ダウンロード」フォルダに保存する

| ツール   | URL                                                                                                                                                                                                                       |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| postgres | https://sbp.enterprisedb.com/getfile.jsp?fileid=1259798&_gl=1*awj4kt*_gcl_au*MTM5NDc2Njc5Ni4xNzYxMDIzMDM5*_ga*MTE1NDgyMzI4LjE3NjEwMjMwMzk.*_ga_ND3EP1ME7G*czE3NjEwMjMwMzgkbzEkZzAkdDE3NjEwMjMwMzgkajYwJGwwJGgxNzUxMzM3NDE |
| jdk      | https://download.oracle.com/java/24/archive/jdk-24.0.2_windows-x64_bin.exe                                                                                                                                                |
| maven    | https://dlcdn.apache.org/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.zip                                                                                                                                        |
| tomcat   | https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.48/bin/apache-tomcat-10.1.48.exe                                                                                                                                          |

### 4. 4 つのファイルをインストールする

-   **PostgreSQL**

    -   セットアップ中の選択肢：
        -   ポート番号：**5433**
        -   スーパーユーザ（postgres）パスワード：**password**
    -   インストール完了後、サービスが起動していることを確認してください。

-   **JDK（Oracle JDK 24.0.2）**

    -   既定パスにインストール（例：`C:\Program Files\Java\jdk-24`）

-   **Maven**

    -   ZIP を解凍して、**`C:\Program Files`** に展開  
        例：`C:\Program Files\apache-maven-3.9.11`

-   **Tomcat**
    -   インストーラで **Tomcat 10.1.48** をインストール  
        例：`C:\Program Files\Apache Software Foundation\Tomcat 10.1`

### 5. 環境変数（Path など）の設定

1. **Windows 検索** →「**システムの詳細設定の表示**」を開く
2. **詳細設定**タブ → **環境変数(N)...** をクリック
3. 下記を設定

**(A) システム環境変数の Path に新規追加**

-   `%CATALINA_HOME%\bin`
-   `%JAVA_HOME%\bin`
-   `C:\Program Files\apache-maven-3.9.11\bin`  
    （※実際に展開した Maven のパスをコピーして貼り付け）

**(B) システム環境変数に新規追加**

-   変数名：`CATALINA_HOME`  
    値：`C:\Program Files\Apache Software Foundation\Tomcat 10.1`

-   変数名：`JAVA_HOME`  
    値：`C:\Program Files\Java\jdk-24`

### 6. 動作確認（必ず実行）

Powershell を管理者として実行します。：

> java の確認

```powershell
java -version
```

実行結果

```powershell
java version "24.0.2" 2025-07-15
Java(TM) SE Runtime Environment (build 24.0.2+12-54)
Java HotSpot(TM) 64-Bit Server VM (build 24.0.2+12-54, mixed mode, sharing)
```

---

> maven の確認

```powershell
mvn -version
```

の実行結果

```powershell
Apache Maven 3.9.11 (3e54c93a704957b63ee3494413a2b544fd3d825b)
Maven home: C:\Program Files\apache-maven-3.9.11
Java version: 24.0.2, vendor: Oracle Corporation, runtime: C:\Program Files\Java\jdk-24
Default locale: ja_JP, platform encoding: UTF-8
OS name: "windows 11", version: "10.0", arch: "amd64", family: "windows"
```

---

> Tomcat の確認

```powershell
version.bat
```

実行結果

```powershell
Using CATALINA_BASE:   "C:\Program Files\Apache Software Foundation\Tomcat 10.1"
Using CATALINA_HOME:   "C:\Program Files\Apache Software Foundation\Tomcat 10.1"
Using CATALINA_TMPDIR: "C:\Program Files\Apache Software Foundation\Tomcat 10.1\temp"
Using JRE_HOME:        "C:\Program Files\Java\jdk-24"
Using CLASSPATH:       "C:\Program Files\Apache Software Foundation\Tomcat 10.1\bin\bootstrap.jar;C:\Program Files\Apache Software Foundation\Tomcat 10.1\bin\tomcat-juli.jar"
Using CATALINA_OPTS:   " -Dfile.encoding=UTF-8"
Server version: Apache Tomcat/10.1.44
Server built:   Aug 4 2025 13:14:17 UTC
Server number:  10.1.44.0
OS Name:        Windows 11
OS Version:     10.0
Architecture:   amd64
JVM Version:    24.0.2+12-54
JVM Vendor:     Oracle Corporation
```

-   いずれかで「コマンドが見つかりません」と出る場合、**環境変数 Path** の設定ミスか、**再起動忘れ**の可能性が高いです。

### 7. PostgreSQL データベースセットアップ

> PostgreSQL のインストールが完了したら、アプリケーション用のデータベースとユーザーを設定します。

#### (1) ポート設定の確認

このプロジェクトは PostgreSQL のポート **5433** を使用します。インストール時に 5433 を指定していれば、この手順はスキップできます。

ポート設定を確認・変更する場合：

1. `postgresql.conf` を開く（通常の場所）：

    ```
    C:\Program Files\PostgreSQL\15\data\postgresql.conf
    ```

2. `port` の設定を確認・変更：

    ```conf
    # 変更前
    #port = 5432

    # 変更後
    port = 5433
    ```

3. PostgreSQL サービスを再起動：

    - **サービスアプリ**を開く（`services.msc`）
    - **postgresql-x64-15** を右クリック → **再起動**

4. ポート変更を確認：
    ```bash
    netstat -ano | findstr :5433
    ```

#### (2) データベースとユーザーの作成

pgAdmin を起動し、以下の SQL を実行してデータベースとユーザーを作成してください：

```sql
-- PostgreSQL に接続して実行
CREATE DATABASE hidariude;
CREATE USER postgres WITH PASSWORD 'password';
ALTER USER postgres WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE hidariude TO postgres;
```

#### (3) 接続設定の確認

データベース接続設定は `src/main/java/dao/TransactionManager.java` で管理されています：

```java
private static final String DB_URL = "jdbc:postgresql://localhost:5433/hidariude";
private static final String SCHEMA = "?currentSchema=public";
private static final String DB_USER = "postgres";
private static final String DB_PASSWORD = "password";
```

> **注意**
>
> -   このプロジェクトはポート **5433** を使用します
> -   スキーマは **public** を使用します
> -   ポートやユーザー名などを変更した場合は `TransactionManager.java` を編集してください

---

## VS Code 拡張機能

以下の拡張機能をインストールしてください：

1. **Extension Pack for Java** (Microsoft)

    ```
    vscjava.vscode-java-pack
    ```

2. **Java Server Pages** (Patrik Thorsson)
    ```
    pthorsson.vscode-jsp
    ```

---

## プロジェクト構成

```
hidariude/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── config/         # アプリケーション設定
│   │   │   ├── controller/     # フロントコントローラー
│   │   │   ├── dao/            # データアクセス層
│   │   │   ├── domain/         # ドメインオブジェクト
│   │   │   ├── dto/            # データ転送オブジェクト
│   │   │   ├── filter/         # サーブレットフィルタ
│   │   │   ├── listener/       # サーブレットリスナー・マイグレーション
│   │   │   ├── service/        # ビジネスロジック
│   │   │   └── util/           # ユーティリティクラス
│   │   └── webapp/        # Web リソース (JSP, CSS, JS等)
│   │       └── WEB-INF/
│   │           ├── jsp/        # JSP ファイル
│   │           └── web.xml     # Web アプリケーション設定
│   └── test/
│       └── java/          # テストコード
├── .vscode/               # VS Code 設定ファイル
│   ├── settings.json      # プロジェクト設定
│   ├── launch.json        # デバッグ設定
│   └── tasks.json         # Maven/Tomcat タスク
├── docs/                  # プロジェクトドキュメント
│   ├── コード分析/        # Claude Code によるコード分析
│   └── 機能定義/          # 機能仕様書
├── .env                   # 環境変数設定ファイル
├── .gitignore             # Git 除外設定
├── pom.xml                # Maven 設定
├── CLAUDE.md              # Claude Code 向けガイド
├── Git運用ルール.md        # Git 運用ルール
├── README.md              # このファイル
└── deploy.sh              # デプロイスクリプト
```

---

## 開発環境セットアップ

### 1. プロジェクトのクローン・開始

```bash
# リポジトリのクローン
git clone https://github.com/jbn-komiyama/hidariude.git

# プロジェクトディレクトリに移動
cd hidariude

# VS Code でプロジェクトを開く
code .
```

### 2. 環境変数の設定（.env ファイル）

プロジェクトルートに `.env` ファイルを作成し、必要な環境変数を設定してください。

#### .env ファイルの作成方法

1. プロジェクトルートに `.env` ファイルを作成
2. 以下の内容を記述して保存：

```properties
# SendGrid API設定
SENDGRID_API_KEY=your_sendgrid_api_key_here(※管理者に確認してください)

# アプリケーション設定
# 開発環境: http://localhost:8080
# 本番環境: http://ik1-224-81260.vs.sakura.ne.jp:8080
APP_BASE_URL=http://localhost:8080
```

3. `SENDGRID_API_KEY` に実際の SendGrid API キーを設定してください

> **重要**:
>
> -   `.env` ファイルは Git にコミットされません（`.gitignore` に登録済み）
> -   本番環境では、実際の SendGrid API キーと適切な `APP_BASE_URL` を設定してください
> -   `.env.example` ファイルがテンプレートとして用意されています

#### 環境変数の説明

| 変数名             | 説明                                   | デフォルト値            |
| ------------------ | -------------------------------------- | ----------------------- |
| `SENDGRID_API_KEY` | SendGrid のメール送信 API キー（必須） | なし                    |
| `APP_BASE_URL`     | アプリケーションのベース URL           | `http://localhost:8080` |

#### 本番環境での設定

本番環境では、以下のいずれかの方法で環境変数を設定できます：

1. **.env ファイルを使用**（推奨）:

    ```bash
    cd /opt/hidariude
    vi .env
    # 上記の内容を記述
    ```

2. **システム環境変数を使用**:
    ```bash
    export SENDGRID_API_KEY=your_actual_key_here
    export APP_BASE_URL=http://ik1-224-81260.vs.sakura.ne.jp:8080
    ```

### 3. VS Code での初期設定

VS Code でプロジェクトを開くと、自動的に以下が実行されます：

-   Java プロジェクトとして認識
-   Maven 依存関係の解決
-   プロジェクト設定の適用

### 4. Maven 依存関係の更新

**Ctrl+Shift+P** でコマンドパレットを開き：

```
Java: Reload Projects
```

を実行してください。

> **注意**: 初回は dotenv-java ライブラリのダウンロードに時間がかかる場合があります。

### 5. データベースの初期化

データベースのテーブル作成と初期データ投入は、**初回 Tomcat デプロイ時に自動的に実行されます**。

初回起動時に以下が自動実行されます：

-   全テーブルの作成（system_admins, secretaries, customers, assignments, tasks など）
-   初期データの投入（管理者 10 件、秘書 10 件、顧客 10 件など）
-   すべてのパスワードは BCrypt でハッシュ化されて保存されます

**初期ログイン情報**:

-   システム管理者: `admin1@example.com` / `Password1`
-   秘書: `secretary1@example.com` / `Password1`
-   顧客: `contact1@example.com` / `Password1`

> **注意**:
>
> -   初期化は `system_admins` テーブルが存在しない場合のみ実行されます
> -   データベースをリセットしたい場合は、pgAdmin で全テーブルを削除してから Tomcat を再起動してください
> -   初期化処理の詳細は `src/main/java/listener/DatabaseInitListener.java` を参照してください

---

## 使用方法

### 開発手順（推奨）

VS Code 上でタスクを実行するだけで開発環境を構築できます。

#### 方法 1: ワンクリックでビルド＆デプロイ（最も簡単）

**Ctrl+Shift+B** を押すだけで、以下の 3 つのタスクが自動的に順次実行されます：

1. **Maven Clean** - ビルド成果物をクリーンアップ
2. **Maven Package** - WAR ファイルを生成
3. **Tomcat Deploy** - Tomcat サーバーで起動

または、**Ctrl+Shift+P** → **Tasks: Run Task** → **Build and Deploy** を選択しても同じです。

#### 方法 2: 個別タスクを手動実行

**Ctrl+Shift+P** → **Tasks: Run Task** から各タスクを個別に実行することもできます。

### 各タスクの詳細

**Ctrl+Shift+P** → **Tasks: Run Task** から以下のタスクを選択できます：

#### Build and Deploy（推奨）

```
Build and Deploy
```

-   Maven Clean → Maven Package → Tomcat Deploy を自動的に順次実行
-   **Ctrl+Shift+B** のショートカットキーで実行可能
-   通常の開発ではこのタスクを使用することを推奨

#### Maven Clean

```
Maven Clean
```

-   ビルド成果物をクリーンアップ

#### Maven Package

```
Maven Package
```

-   WAR ファイルを生成（自動的にコンパイルも実行されます）

#### Tomcat Deploy

```
Tomcat Deploy
```

-   Tomcat サーバーを起動してアプリケーションをデプロイ（cargo:run）

### アプリケーションへのアクセス

Tomcat 起動後、以下の URL でアプリケーションにアクセスできます：

```
http://localhost:8080
```

---

## デバッグ

### VS Code でのデバッグ（推奨方法）

#### 方法 1: タスクからデバッグ起動（最も簡単）

1. **Ctrl+Shift+P** → **Tasks: Run Task** → **Tomcat Debug Deploy** を選択
2. ターミナルに `Listening for transport dt_socket at address: 8000` と表示されるまで待つ（約 10 秒）
3. **F5** を押すか、**実行とデバッグ**ビュー（Ctrl+Shift+D）から **Debug (Attach)** を起動
4. デバッガーが接続されると、VS Code のステータスバーがオレンジ色になります

#### 方法 2: ターミナルから手動起動

1. **新しいターミナル**を開く（Ctrl+Shift+`）
2. 以下のコマンドを実行：

    ```bash
    mvnDebug cargo:run
    ```

3. `Listening for transport dt_socket at address: 8000` と表示されたら準備完了
4. **F5** を押してデバッガーをアタッチ

### ブレークポイントの設定

Java ファイルの行番号左側（左マージン）をクリックして赤丸のブレークポイントを設定できます。

### デバッグが接続できない場合のチェックリスト

#### 1. ポート 8000 が使用中

```bash
# Windowsの場合
netstat -ano | findstr :8000

# 使用中の場合、プロセスを終了
taskkill /PID <プロセスID> /F
```

#### 2. Tomcat が完全に起動していない

-   ターミナルに `Listening for transport dt_socket at address: 8000` が表示されるまで**必ず待つ**
-   起動には 10〜30 秒かかる場合があります

#### 3. デバッグ設定の確認

`.vscode/launch.json` に以下の設定があることを確認：

```json
{
    "type": "java",
    "name": "Debug (Attach)",
    "request": "attach",
    "hostName": "localhost",
    "port": 8000
}
```

#### 4. Java プロセスの確認

```bash
# Windowsの場合
jps -v | findstr 8000

# 正常な場合の例：
# 12345 Launcher -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:8000
```

### デバッグセッションの終了

1. デバッグツールバーの**停止**ボタン（赤い四角）をクリック
2. またはターミナルで **Ctrl+C** を押して Tomcat を停止

### 実際のデバッグ操作

-   **F10** (Step Over): 次の行へ
-   **F11** (Step Into): メソッド内部へ
-   **Shift+F11** (Step Out): メソッドから抜ける
-   **F5** (Continue): 次のブレークポイントまで実行
-   **変数ビュー**: 左側のパネルで変数の値を確認
-   **デバッグコンソール**: 式を評価して値を確認

---

## トラブルシューティング

### Java バージョンエラー

```bash
# Java バージョン確認
java -version
javac -version

# VS Code でのJava設定確認
Ctrl+Shift+P → "Java: Configure Java Runtime"
```

### Maven コマンドが見つからない

```bash
# Maven インストール確認
mvn -version

# PATH 環境変数の確認
echo %PATH%   # Windows
```

### Tomcat ポートエラー

`pom.xml` でポート番号を変更できます：

```xml
<configuration>
  <port>8081</port>  <!-- 8080から変更 -->
  <path>/hidariude</path>
</configuration>
```

### PostgreSQL 接続エラー

アプリケーション起動時にデータベース接続エラーが発生する場合は、以下を確認してください：

1. **PostgreSQL サービスが起動しているか確認**

    ```bash
    sc query postgresql-x64-15
    ```

2. **ポート 5433 で稼働しているか確認**

    ```bash
    netstat -ano | findstr :5433
    ```

3. **データベースとユーザーが作成されているか確認**

    - pgAdmin で `hidariude` データベースが存在するか確認
    - `postgres` ユーザーが存在し、パスワードが `password` であることを確認
    - 作成手順は「**7. PostgreSQL データベースセットアップ**」を参照

4. **テーブルが作成されているか確認**

    ```sql
    SELECT table_name FROM information_schema.tables
    WHERE table_schema = 'public'
    ORDER BY table_name;
    ```

    - テーブルが存在しない場合は、Tomcat を起動すると自動的に作成されます
    - または「**4. データベースの初期化**」を参照

5. **接続設定の確認**
    - `src/main/java/dao/TransactionManager.java` のポート、ユーザー名、パスワードが正しいか確認

---

# Part 2: AlmaLinux 本番デプロイ

## 前提条件

以下の環境が構築済みであること：

-   **Java 24** - `/usr/lib/jvm/jdk-24.0.2-oracle-x64`
-   **Apache Maven 3.9.11** - `/opt/apache-maven-3.9.11`
-   **Apache Tomcat 10.1.46** - `/opt/tomcat/apache-tomcat-10.1.46`
    -   systemctl で自動起動設定済み
-   **PostgreSQL 15** - ポート 5433 で稼働
    -   データベース `hidariude` 作成済み
    -   ユーザー `postgres` / パスワード `password`
    -   systemctl で自動起動設定済み
    -   TCP 接続（localhost:5433）が許可されていること（pg_hba.conf）
    -   **postgresql15-contrib パッケージ**がインストール済み（UUID 生成用の pgcrypto 拡張に必要）
-   **nginx** - リバースプロキシと HTTPS 化用（推奨）
-   **ドメイン** - `ourdesk.n-learning.jp` の DNS 設定が完了していること（A レコード）

### PostgreSQL の追加設定（重要）

AlmaLinux では、PostgreSQL の拡張機能パッケージを別途インストールする必要があります：

```bash
# postgresql-contrib パッケージをインストール
sudo dnf install -y postgresql15-contrib

# PostgreSQL を再起動
sudo systemctl restart postgresql-15

# pgcrypto 拡張が利用可能か確認
sudo -u postgres psql -p 5433 -d hidariude -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"
sudo -u postgres psql -p 5433 -d hidariude -c "SELECT gen_random_uuid();"
```

> **重要**: `postgresql15-contrib` がインストールされていないと、テーブル作成時に以下のエラーが発生します：
>
> ```
> ERROR: extension "pgcrypto" is not available
> Detail: Could not open extension control file "/usr/pgsql-15/share/extension/pgcrypto.control"
> ```
>
> このエラーが発生した場合は、上記のコマンドで `postgresql15-contrib` をインストールしてください。

## nginx リバースプロキシ設定（推奨）

HTTPS 化とログイン回数制限を実装するため、nginx リバースプロキシを設定します。

詳細な手順は `nginx/README_NGINX.md` を参照してください。

### クイックセットアップ

```bash
# 1. nginxとcertbotをインストール
sudo dnf install -y nginx certbot python3-certbot-nginx

# 2. ファイアウォール設定
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload

# 3. certbot用ディレクトリを作成
sudo mkdir -p /var/www/certbot

# 4. nginx設定ファイルをコピー
cd /opt/hidariude
sudo cp nginx/hidariude.conf /etc/nginx/conf.d/hidariude.conf

# 5. nginx設定をテスト
sudo nginx -t

# 6. nginxを起動・有効化
sudo systemctl enable nginx
sudo systemctl start nginx

# 7. Let's Encrypt証明書を取得
sudo certbot --nginx -d ourdesk.n-learning.jp

# 8. 最終的な設定ファイルを再適用（ログイン制限を含む）
sudo cp nginx/hidariude.conf /etc/nginx/conf.d/hidariude.conf
sudo nginx -t
sudo systemctl reload nginx
```

### 設定内容

-   **HTTPS 化**: Let's Encrypt 証明書を使用した SSL/TLS 暗号化
-   **ログイン回数制限**: `/admin/login`, `/secretary/login`, `/customer/login` への POST リクエストを 1 分間に 10 回までに制限
-   **セキュリティヘッダー**: HSTS、XSS 対策、クリックジャッキング対策など

詳細は `nginx/README_NGINX.md` を参照してください。

## デプロイ手順

### 1. リポジトリのクローン

```bash
cd /opt
git clone <repository-url> hidariude
cd hidariude
```

### 2. 環境変数の設定（.env ファイル）

本番環境でも `.env` ファイルを使用して環境変数を設定します。

```bash
# プロジェクトルートに.envファイルを作成
cd /opt/hidariude
vi .env
```

以下の内容を記述：

```properties
# SendGrid API設定
SENDGRID_API_KEY=your_actual_sendgrid_api_key_here

# アプリケーション設定（本番環境URL）
# nginxリバースプロキシを使用する場合はHTTPS URLに変更
APP_BASE_URL=https://ourdesk.n-learning.jp
```

> **重要**:
>
> -   `SENDGRID_API_KEY` には実際の SendGrid API キーを設定してください
> -   `.env` ファイルのパーミッションを適切に設定することを推奨します：
>     ```bash
>     chmod 600 .env
>     ```

### 3. データベースの初期化

データベースのテーブル作成と初期データ投入は、**初回 Tomcat デプロイ時に自動的に実行されます**。

初回起動時に以下が自動実行されます：

-   全テーブルの作成（system_admins, secretaries, customers, assignments, tasks など）
-   初期データの投入（管理者 10 件、秘書 10 件、顧客 10 件など）
-   すべてのパスワードは BCrypt でハッシュ化されて保存されます

**初期ログイン情報**:

-   システム管理者: `admin1@example.com` / `Password1`
-   秘書: `secretary1@example.com` / `Password1`
-   顧客: `contact1@example.com` / `Password1`

> **注意**:
>
> -   初期化は `system_admins` テーブルが存在しない場合のみ実行されます
> -   データベースをリセットしたい場合は、PostgreSQL で全テーブルを削除してから Tomcat を再起動してください
> -   初期化処理は `DatabaseInitListener` により自動実行されます（`src/main/java/listener/DatabaseInitListener.java`）

### 4. アプリケーションのデプロイ

```bash
# 更新内容を破棄して最新のコードを取得
cd /opt/hidariude
git restore . # 変更内容を破棄
git pull origin deploy

# アプリケーションのデプロイ
chmod +x deploy.sh
./deploy.sh
```

このスクリプトは以下を実行します：

-   PostgreSQL の接続設定確認と調整（max_connections）
-   Git リポジトリの更新（git pull）
-   Maven ビルド（clean package）
-   Tomcat の停止
-   既存 WAR ファイルの削除
-   新しい WAR ファイルのデプロイ
-   Tomcat の起動

初回デプロイ時は、Tomcat 起動後に `DatabaseInitListener` が自動的にデータベースを初期化します。

### 5. アプリケーションへのアクセス

デプロイ完了後、以下の URL でアクセス可能：

**nginx リバースプロキシ経由（推奨）**:

```
https://ourdesk.n-learning.jp
```

**直接アクセス（開発・デバッグ用）**:

```
http://localhost:8080
http://<サーバーのIPアドレス>:8080
```

**動作確認**:

```bash
# HTTPSアクセス確認
curl -I https://ourdesk.n-learning.jp

# 証明書確認
echo | openssl s_client -connect ourdesk.n-learning.jp:443 -servername ourdesk.n-learning.jp 2>/dev/null | openssl x509 -noout -dates
```

## トラブルシューティング

### デプロイスクリプトの実行権限エラー

```bash
chmod +x deploy.sh init_database.sh
```

### Tomcat ログの確認

```bash
# リアルタイムログ
tail -f /opt/tomcat/apache-tomcat-10.1.46/logs/catalina.out

# エラーログ
tail -f /opt/tomcat/apache-tomcat-10.1.46/logs/catalina.$(date +%Y-%m-%d).log
```

### Tomcat の手動操作

```bash
# ステータス確認
systemctl status tomcat

# 起動
systemctl start tomcat

# 止める
systemctl stop tomcat

# 再起動
systemctl restart tomcat
```

### PostgreSQL の確認

```bash
# ステータス確認
systemctl status postgresql-15

# データベース接続確認（peer認証）
sudo -u postgres psql -p 5433 -d hidariude

# テーブル一覧
sudo -u postgres psql -p 5433 -d hidariude -c "\dt"

# TCP接続確認（Javaアプリケーションが使用）
PGPASSWORD=password psql -h localhost -p 5433 -U postgres -d hidariude -c "SELECT 1;"
```

> **注意**: Java アプリケーションは `localhost:5433` に TCP 接続します。`pg_hba.conf` で以下の設定が必要です：

```
# IPv4 local connections:
host    hidariude       postgres        127.0.0.1/32            scram-sha-256
```

設定後は PostgreSQL を再起動：

```bash
systemctl restart postgresql-15
```

### ポート開放（ファイアウォール）

外部からアクセスする場合：

```bash
# ファイアウォール確認
firewall-cmd --list-all

# nginxを使用する場合（推奨）
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https
firewall-cmd --reload

# または、直接Tomcatにアクセスする場合（開発・デバッグ用のみ）
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --reload
```

> **注意**: 本番環境では、直接ポート 8080 を開放せず、nginx リバースプロキシ経由でアクセスすることを推奨します。

### デプロイ失敗時の対処

1. Tomcat ログを確認

```bash
tail -100 /opt/tomcat/apache-tomcat-10.1.46/logs/catalina.out

# データベース初期化のログを確認
tail -100 /opt/tomcat/apache-tomcat-10.1.46/logs/catalina.out | grep -A 20 "Database Initialization"
```

2. ビルドログを確認（Maven エラー）

```bash
cd /opt/hidariude
mvn clean package
```

3. データベース接続エラーの場合

```bash
# PostgreSQL稼働確認
systemctl status postgresql-15

# 接続テスト
sudo -u postgres psql -p 5433 -d hidariude -c "SELECT 1;"
```

4. pgcrypto 拡張機能エラーの場合

ログに `ERROR: extension "pgcrypto" is not available` が表示される場合：

```bash
# postgresql-contrib パッケージをインストール
sudo dnf install -y postgresql15-contrib

# PostgreSQL を再起動
sudo systemctl restart postgresql-15

# 再デプロイ
cd /opt/hidariude
./deploy.sh
```

5. nginx 設定エラーの場合

```bash
# nginx設定をテスト
sudo nginx -t

# nginxログを確認
sudo tail -f /var/log/nginx/hidariude_error.log

# nginx設定ファイルを確認
sudo vi /etc/nginx/conf.d/hidariude.conf

# 設定変更後は再読み込み
sudo nginx -t && sudo systemctl reload nginx
```

6. ログイン回数制限のテスト

レート制限が正しく機能しているか確認：

```bash
# 連続リクエストを送信（11回目以降で429エラーが返るはず）
for i in {1..15}; do
  curl -X POST https://ourdesk.n-learning.jp/admin/login \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "email=test@example.com&password=test" \
    -w "\nHTTP Status: %{http_code}\n" \
    -s -o /dev/null
  sleep 1
done

# ログイン試行ログを確認
sudo tail -f /var/log/nginx/hidariude_login_attempts.log
```

---

# Part 3: データベースマイグレーション

## 概要

このプロジェクトでは、データベースのスキーマやデータの変更を管理するためにマイグレーションシステムを使用しています。各マイグレーションは独立した Java ファイルとして管理され、アプリケーション起動時に自動実行されます。

## マイグレーション実行の判断基準

マイグレーションは以下の基準で自動的に実行されます：

1. **自動実行タイミング**: アプリケーション起動時に自動実行
2. **実行判定**: `schema_migrations` テーブルに記録がない場合のみ実行
3. **実行順序**: クラス名の日付順（`Migration_YYYYMMDD_*`）にソートして実行
4. **成功時**: `schema_migrations` テーブルにマイグレーション名と実行日時を記録
5. **失敗時**: ロールバックされ、記録は残らない（次回起動時に再実行される）

## マイグレーションファイルの場所

すべてのマイグレーションファイルは `src/main/java/listener/` ディレクトリに配置されます。

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

`src/main/java/listener/DatabaseInitListener.java` の `runMigrations()` メソッド内に、新しいマイグレーションを追加します。

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
Applying migration: 20251029_create_schema_migrations
  Description: マイグレーション管理用テーブルを作成
  [Migration] Creating schema_migrations table...
    - schema_migrations table created successfully
  [Migration] Migration table setup completed.
Migration applied successfully: 20251029_create_schema_migrations
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

---
