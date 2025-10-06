# Hidariude - Java Web アプリケーション

## 概要

このプロジェクトは Java Servlet ベースの Web アプリケーションで、VS Code での開発環境構築をサポートしています。

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

2. **Tomcat for Java** (Wei Shen)
    ```
    adashen.vscode-tomcat
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
http://localhost:8080/hidariude
```

## デバッグ

### VS Code でのデバッグ

1. ターミナルでデバッグ用 Tomcat を起動：

    ```bash
    mvnDebug cargo:run
    ```

2. **F5** を押してデバッグを開始
3. **Debug (Attach)** 設定が自動選択される
4. ポート 8000 で Tomcat に接続

### ブレークポイントの設定

Java ファイルの行番号左側をクリックしてブレークポイントを設定できます。

## データベース設定

### PostgreSQL セットアップ

#### 事前準備

PostgreSQL で以下のデータベースとユーザーを作成してください：

```sql
-- PostgreSQL に接続して実行
CREATE DATABASE hidariude;
CREATE USER postgres WITH PASSWORD 'password';
ALTER USER postgres WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE hidariude TO postgres;
```

#### 接続設定

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
-   データベース接続情報を変更する場合は `TransactionManager.java` を編集してください

## 技術スタック

-   **Java 24** - プログラミング言語
-   **Jakarta Servlet 6.1.0** - Web フレームワーク
-   **Jakarta JSP 4.0.0** - ビューテクノロジー
-   **JSTL 3.0.1** - JSP 標準タグライブラリ
-   **PostgreSQL 42.6.0** - データベースドライバー
-   **Apache POI 5.4.1** - Excel ファイル処理
-   **Apache Maven** - ビルドツール
-   **Apache Tomcat** - アプリケーションサーバー

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

-   PostgreSQL サービスが起動しているか確認
-   データベースとユーザーが作成されているか確認
-   **hidariude データベース**が作成されているか確認
-   **postgres ユーザー**が作成されているか確認
-   接続設定（ホスト、**ポート 5433**、認証情報）を確認

データベース作成コマンド：

```sql
CREATE DATABASE hidariude;
CREATE USER postgres WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE hidariude TO postgres;
```

## 開発ガイドライン

### ディレクトリ構成

-   **Java クラス**: `src/main/java/`
-   **JSP ファイル**: `src/main/webapp/`
-   **CSS/JS**: `src/main/webapp/assets/`
-   **設定ファイル**: `src/main/resources/`

### コーディング規約

-   エンコーディング: **UTF-8**
-   インデント: **スペース 4 個**
-   Java 命名規約に準拠

## ライセンス

このプロジェクトは開発用テンプレートです。

---

## AlmaLinux 10 へのデプロイ

### 前提条件

以下の環境が構築済みであること：

-   **Java 24** - `/usr/lib/jvm/jdk-24.0.2-oracle-x64`
-   **Apache Maven 3.9.11** - `/opt/apache-maven-3.9.11`
-   **Apache Tomcat 10.1.46** - `/opt/tomcat/apache-tomcat-10.1.46`
    -   systemctl で自動起動設定済み
-   **PostgreSQL 15** - ポート 5433 で稼働
    -   データベース `hidariude` 作成済み
    -   ユーザー `postgres` / パスワード `password`
    -   systemctl で自動起動設定済み

### デプロイ手順

#### 1. リポジトリのクローン

```bash
cd /opt
git clone <repository-url> hidariude
cd hidariude
```

#### 2. データベースの初期化

初回のみ、またはデータベースをリセットする場合に実行：

```bash
chmod +x init_database.sh
./init_database.sh
```

このスクリプトは以下を実行します：

-   既存テーブルの削除（確認プロンプトあり）
-   DDL の実行（テーブル作成）
-   ダミーデータの投入

#### 3. アプリケーションのデプロイ

```bash
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

#### 4. アプリケーションへのアクセス

デプロイ完了後、以下の URL でアクセス可能：

```
http://localhost:8080/hidariude
http://<サーバーのIPアドレス>:8080/hidariude
```

### トラブルシューティング（AlmaLinux）

#### デプロイスクリプトの実行権限エラー

```bash
chmod +x deploy.sh init_database.sh
```

#### Tomcat ログの確認

```bash
# リアルタイムログ
tail -f /opt/tomcat/apache-tomcat-10.1.46/logs/catalina.out

# エラーログ
tail -f /opt/tomcat/apache-tomcat-10.1.46/logs/catalina.$(date +%Y-%m-%d).log
```

#### Tomcat の手動操作

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

#### PostgreSQL の確認

```bash
# ステータス確認
systemctl status postgresql-15

# データベース接続確認
psql -h localhost -p 5433 -U postgres -d hidariude

# テーブル一覧
psql -h localhost -p 5433 -U postgres -d hidariude -c "\dt"
```

#### ポート開放（ファイアウォール）

外部からアクセスする場合：

```bash
# ファイアウォール確認
firewall-cmd --list-all

# ポート8080を開放
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --reload
```

#### デプロイ失敗時の対処

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
psql -h localhost -p 5433 -U postgres -d hidariude -c "SELECT 1;"
```

### 自動デプロイ（オプション）

cron で定期的にデプロイする場合：

```bash
# crontab編集
crontab -e

# 例：毎日深夜2時にデプロイ
0 2 * * * /opt/hidariude/deploy.sh >> /var/log/hidariude-deploy.log 2>&1
```

---

## 更新履歴

### v0.0.1-SNAPSHOT

-   初期プロジェクト構成
-   VS Code 開発環境対応
-   Maven + Tomcat 統合設定
-   AlmaLinux 10 デプロイスクリプト追加
