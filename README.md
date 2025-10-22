# Hidariude - Java Web アプリケーション

## 概要

このプロジェクトは Java Servlet ベースの Web アプリケーションで、VS Code での開発環境構築をサポートしています。

## 目次

-   [Part 1: Windows ローカル開発環境](#part-1-windows-ローカル開発環境)
-   [Part 2: AlmaLinux 本番デプロイ](#part-2-almalinux-本番デプロイ)
-   [技術スタック](#技術スタック)
-   [更新履歴](#更新履歴)

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

## プロジェクト構成

```
hidariude/
├── src/
│   ├── main/
│   │   ├── java/          # Java ソースコード
│   │   └── webapp/        # Web リソース (JSP, CSS, JS等)
│   │       └── WEB-INF/
│   │           └── web.xml
│   └── test/
│       └── java/          # テストコード
├── .vscode/               # VS Code 設定ファイル
│   ├── settings.json      # プロジェクト設定
│   ├── launch.json        # デバッグ設定
│   └── tasks.json         # Maven/Tomcat タスク
├── pom.xml               # Maven 設定
└── README.md            # このファイル
```

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

### 2. VS Code での初期設定

VS Code でプロジェクトを開くと、自動的に以下が実行されます：

-   Java プロジェクトとして認識
-   Maven 依存関係の解決
-   プロジェクト設定の適用

### 3. Maven 依存関係の更新

**Ctrl+Shift+P** でコマンドパレットを開き：

```
Java: Reload Projects
```

を実行してください。

## 使用方法

### 開発手順

VS Code 上でタスクを実行するだけで開発環境を構築できます。

**Ctrl+Shift+P** → **Tasks: Run Task** から以下の手順でタスクを実行：

1. **Maven Clean** - ビルド成果物をクリーンアップ
2. **Maven Package** - WAR ファイルを生成
3. **Tomcat Deploy** - Tomcat サーバーで起動

### 各タスクの詳細

**Ctrl+Shift+P** → **Tasks: Run Task** から以下のタスクを選択できます：

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
http://localhost:8080/hidariude/
```

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

# 正常な場合、以下のような出力が表示されます：
# 12345 Launcher -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:8000
```

### デバッグセッションの終了

1. デバッグツールバーの**停止**ボタン（赤い四角）をクリック
2. またはターミナルで **Ctrl+C** を押して Tomcat を停止

### 実際のデバッグ操作

#### ブレークポイントで停止したら：

-   **F10** (Step Over): 次の行へ
-   **F11** (Step Into): メソッド内部へ
-   **Shift+F11** (Step Out): メソッドから抜ける
-   **F5** (Continue): 次のブレークポイントまで実行
-   **変数ビュー**: 左側のパネルで変数の値を確認
-   **デバッグコンソール**: 式を評価して値を確認

## データベース設定

### PostgreSQL セットアップ

#### 1. PostgreSQL ポート設定

このプロジェクトは PostgreSQL のポート **5433** を使用します。デフォルトの 5432 から変更する場合は以下の手順で設定してください：

##### Windows での設定手順

1. `postgresql.conf` を開く（通常の場所）：

    ```
    C:\Program Files\PostgreSQL\15\data\postgresql.conf
    ```

2. `port` の設定を変更：

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

#### 2. データベースとユーザーの作成

pgAdmin を起動し、以下の SQL を実行してデータベースとユーザーを作成してください：

```sql
-- PostgreSQL に接続して実行
CREATE DATABASE hidariude;
CREATE USER postgres WITH PASSWORD 'password';
ALTER USER postgres WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE hidariude TO postgres;
```

#### 3. テーブル作成とダミーデータ投入

**pgAdmin** で `hidariude` データベースに接続し、`src/main/sql/hoshiiro.sql` を実行してください：

1. pgAdmin の左側ツリーで **Servers** → **PostgreSQL 15** → **Databases** → **hidariude** を選択
2. 上部メニューから **Tools** → **Query Tool** を開く
3. ファイルメニューから **Open File** を選択
4. プロジェクトの `src/main/sql/hoshiiro.sql` を開く
5. **実行ボタン**（▶ アイコン）をクリック

このスクリプトは以下を実行します：

-   全テーブルの作成（system_admins, secretaries, customers, assignments, tasks など）
-   ダミーデータの投入（管理者 10 件、秘書 10 件、顧客 10 件など）
-   過去 24 ヶ月分の月次サマリデータの生成

**注意**: `hoshiiro.sql` は既存のテーブルを削除してから再作成するため、データがリセットされます。

#### 4. 接続設定の確認

データベース接続設定は `src/main/java/dao/TransactionManager.java` で管理されています：

```java
private static final String DB_URL = "jdbc:postgresql://localhost:5433/hidariude";
private static final String SCHEMA = "?currentSchema=public";
private static final String DB_USER = "postgres";
private static final String DB_PASSWORD = "password";
```

**注意**:

-   このプロジェクトはポート **5433** を使用します
-   スキーマは **public** を使用します
-   ポートやユーザー名などを変更した場合は `TransactionManager.java` を編集してください

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
echo $PATH  # Linux/Mac
echo %PATH% # Windows
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
    # Windowsの場合
    sc query postgresql-x64-15
    ```

2. **ポート 5433 で稼働しているか確認**

    ```bash
    netstat -ano | findstr :5433
    ```

    ポートが異なる場合は、上記の「PostgreSQL ポート設定」を参照してください。

3. **データベースとユーザーが作成されているか確認**

    - pgAdmin で接続し、`hidariude` データベースが存在するか確認
    - `postgres` ユーザーが存在し、パスワードが `password` であることを確認

4. **テーブルが作成されているか確認**

    - pgAdmin の Query Tool で以下を実行：

    ```sql
    SELECT table_name FROM information_schema.tables
    WHERE table_schema = 'public'
    ORDER BY table_name;
    ```

    テーブルが存在しない場合は、上記の「テーブル作成とダミーデータ投入」を参照して `hoshiiro.sql` を実行してください。

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

## デプロイ手順

### 1. リポジトリのクローン

```bash
cd /opt
git clone <repository-url> hidariude
cd hidariude
```

### 2. データベースの初期化

初回のみ、またはデータベースをリセット(変更内容を反映)する場合に実行：

```bash
# 更新内容を破棄して最新のコードを取得
cd /opt/hidariude
git restore . # 変更内容を破棄
git pull origin deploy

# データベースの初期化
chmod +x init_database.sh
./init_database.sh
```

このスクリプトは以下を実行します：

-   既存テーブルの削除（確認プロンプトあり）
-   hoshiiro.sql の実行（テーブル作成, ダミーデータ投入）

### 3. アプリケーションのデプロイ

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

-   Git リポジトリの更新（git pull）
-   Maven ビルド（clean package）
-   Tomcat の停止
-   既存 WAR ファイルの削除
-   新しい WAR ファイルのデプロイ
-   Tomcat の起動

### 4. アプリケーションへのアクセス

デプロイ完了後、以下の URL でアクセス可能：

```
http://localhost:8080/hidariude
http://<サーバーのIPアドレス>:8080/hidariude

curl -i http://localhost:8080/hidariude
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

# 停止
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

**注意**: Java アプリケーションは`localhost:5433`に TCP 接続します。`pg_hba.conf`で以下の設定が必要です：

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

# ポート8080を開放
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --reload
```

### デプロイ失敗時の対処

1. Tomcat ログを確認

```bash
tail -100 /opt/tomcat/apache-tomcat-10.1.46/logs/catalina.out
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

---

## 技術スタック

-   **Java 24** - プログラミング言語
-   **Jakarta Servlet 6.1.0** - Web フレームワーク
-   **Jakarta JSP 4.0.0** - ビューテクノロジー
-   **JSTL 3.0.1** - JSP 標準タグライブラリ
-   **PostgreSQL 42.6.0** - データベースドライバー
-   **Apache POI 5.4.1** - Excel ファイル処理
-   **Apache Maven** - ビルドツール
-   **Apache Tomcat** - アプリケーションサーバー

---

## 更新履歴

### v0.0.1-SNAPSHOT

-   初期プロジェクト構成
-   VS Code 開発環境対応
-   Maven + Tomcat 統合設定
-   AlmaLinux 10 デプロイスクリプト追加
