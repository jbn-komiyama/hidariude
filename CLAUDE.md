# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

Hidariude は秘書・顧客・アサイン・業務を管理するための Java Servlet ベースの Web アプリケーションです。システム管理者、秘書、顧客の3つのユーザーロールを持つ秘書管理システムです。

## よく使う開発コマンド

### ビルドとテスト
```bash
# ビルド成果物をクリーンアップ
mvn clean

# WAR ファイルを生成（コンパイルも含む）
mvn package

# テストをスキップしてビルド
mvn package -DskipTests
```

### Tomcat でのローカル開発
```bash
# 組み込み Tomcat にデプロイ（開発用）
mvn cargo:run

# デバッグモード（ポート 8000 でリッスン）
mvnDebug cargo:run
```

### VS Code タスク
Ctrl+Shift+P → Tasks: Run Task から選択:
- **Maven Clean** - ビルド成果物をクリーンアップ
- **Maven Package** - WAR ファイルを生成
- **Tomcat Deploy** - 開発サーバーを起動
- **Tomcat Debug Deploy** - デバッグモードで起動、その後 F5 でデバッガーをアタッチ

### 本番デプロイ (AlmaLinux)
```bash
cd /opt/hidariude
./deploy.sh
```

## アーキテクチャ概要

### フロントコントローラーパターン
アプリケーションは単一の `FrontController` サーブレットで全てのリクエストを処理します:
- **URL パターン**: `/admin/*`, `/secretary/*`, `/customer/*`
- **ルーティング**: `servletPath`（ロール）と `pathInfo`（アクション）に基づく
- **セッション管理**: セッションに保存された `LoginUser` によるロールベース認証
- **ビュー解決**: `/WEB-INF/jsp/[role]/[feature]/[page].jsp` の JSP ファイル

リクエストフロー:
1. リクエスト → `FrontController.execute()`
2. ロール判定（`/admin`, `/secretary`, `/customer`）
3. 認証チェック（ログインパスとルートパス以外）
4. 適切な execute メソッドへルーティング（`adminExecute()`, `secretaryExecute()`, `customerExecute()`）
5. サービス層がビジネスロジックを処理
6. JSP へフォワードまたはリダイレクト

### 3層アーキテクチャ

**コントローラー層** (`controller/FrontController.java`):
- 全ての HTTP リクエストを処理する単一のサーブレット
- URL パターンに基づくルーティング
- セッションベースの認証

**サービス層** (`service/`):
- `BaseService` を継承し、`TransactionManager` と DB コネクションを管理
- ビジネスロジックとバリデーション
- トランザクション管理（try-with-resources で commit/rollback）
- フォワード/リダイレクト用のビューパス文字列を返す

**DAO 層** (`dao/`):
- `BaseDAO` を継承し、サービス層から `Connection` を受け取る
- `SqlLoader` を使用してクラスパスから SQL を読み込み
- エラー時に `DAOException` をスロー
- `PreparedStatement` による直接 JDBC 操作

### トランザクション管理パターン

アプリケーションは `AutoCloseable` を実装したカスタム `TransactionManager` を使用します:

```java
try (TransactionManager tm = new TransactionManager()) {
    Connection conn = tm.getConnection();
    // ... DAO 操作 ...
    tm.commit();  // コミット対象としてマーク
} // 自動でコミットまたはロールバック
```

- Connection は自動コミットされない
- トランザクションを成功させるには明示的に `tm.commit()` を呼び出す必要がある
- try-with-resources で自動クローズ（マークされていればコミット、そうでなければロールバック）
- 全ての DAO が同じ `Connection` インスタンスを受け取り、トランザクションを共有

### SQL 管理

SQL 文は外部ファイルとして保存され、`SqlLoader` 経由で読み込まれます:
- 配置場所: `src/main/resources/sql/`（DAO パッケージ構造をミラーリング）
- 一度読み込まれ `ConcurrentHashMap` にキャッシュされる
- DAO は `SqlLoader.load("sql/path/to/file.sql")` を呼び出す

### データベーススキーマ

主要テーブル:
- **system_admins** - システム管理者ユーザー
- **secretaries** - ランク付き秘書ユーザー
- **secretary_rank** - 秘書のスキルレベルと報酬レート
- **customers** - 顧客企業
- **customer_contacts** - 顧客担当者アカウント（企業の担当者）
- **assignments** - 月次の秘書-顧客アサインと報酬レート
- **task_rank** - 業務難易度レベル（P, A, B, C, D）
- **tasks** - 承認ワークフロー付き業務記録
- **profiles** - 秘書の稼働可能時間と自己紹介
- **customer_monthly_invoices** - 月次請求サマリー
- **secretary_monthly_summaries** - 月次秘書支払いサマリー

全テーブルの共通仕様:
- UUID 主キー（`gen_random_uuid()`）
- 論理削除パターン（`deleted_at TIMESTAMP`）
- 監査フィールド（`created_at`, `updated_at`）

### ユーザーロールと認証

3つのロールタイプ（`LoginUser.authority` に保存）:
- **1**: システム管理者 - 全システム管理権限
- **2**: 秘書 - 業務登録、プロフィール管理
- **3**: 顧客 - 業務閲覧、担当者管理

パスワードは BCrypt でハッシュ化されます（`util.PasswordUtil`）。

デフォルト認証情報（全て共通パスワード "Password1"）:
- 管理者: `admin1@example.com`
- 秘書: `secretary1@example.com`
- 顧客: `contact1@example.com`

### データベース初期化

`DatabaseInitListener` がアプリケーション起動時に実行されます:
- `system_admins` テーブルの存在をチェック
- 存在しない場合、全テーブルを作成して初期データを投入
- UUID 生成には `pgcrypto` 拡張が必要
- AlmaLinux では `postgresql15-contrib` パッケージが必要

## データベース設定

`TransactionManager.java` の接続設定:
```java
DB_URL: jdbc:postgresql://localhost:5433/hidariude
SCHEMA: public
USER: postgres
PASSWORD: password
```

**重要**: PostgreSQL はポート 5433 を使用します（デフォルトの 5432 ではありません）。

## 主要なドメイン概念

### アサインメントシステム
- アサインメントは、特定の月（YYYY-MM）に対して 秘書 + 顧客 + 業務ランク をリンクします
- 各アサインメントには顧客（請求）と秘書（支払い）の両方の基本報酬レートがあります
- 基本報酬は秘書ランクに応じて増額できます
- ユニーク制約: (月、顧客、秘書、業務ランク) ごとに1つのアサインメント

### 業務承認ワークフロー
業務には3つの状態があります:
1. **未承認** - 秘書が提出、管理者のレビュー待ち
2. **承認済み** - 管理者が承認（`approved_at` が設定される）
3. **差し戻し** - 管理者がコメント付きで却下（`remanded_at` が設定される）

業務には顧客通知用のアラート（`alerted_at`）も設定できます。

### 報酬計算
- 顧客支払額: `base_pay_customer + increase_base_pay_customer + インセンティブ`
- 秘書受取額: `base_pay_secretary + increase_base_pay_secretary + インセンティブ`
- レートはアサインメントごとに保存され、月次で調整可能

## コード構成パターン

### サービスクラス
全てのサービスは `BaseService` を継承します:
- コンストラクタ: `new SomeService(HttpServletRequest req, boolean useDB)`
- `useDB=true` の場合、`TransactionManager` と `Connection` を作成
- `req.getParameter()` でリクエストパラメータにアクセス
- `req.setAttribute()` で JSP 用のリクエスト属性を設定
- `String` 型のビューパスを返す（フォワードは相対パス、リダイレクトは `/` で始まる絶対パス）

### バリデーション
- `validation/` パッケージの `Validation` ユーティリティクラス
- サービスは DAO 操作の前にバリデーションメソッドを呼び出す
- エラーはリクエストスコープに保存され JSP で表示される

### DTO vs ドメイン
- **ドメインオブジェクト** (`domain/`): データベーステーブルにマッピング
- **DTO オブジェクト** (`dto/`): ビュー層用のカスタムオブジェクト（結合データ、サマリー）

### エラー処理
- 各ロールごとに専用のエラーページ: `common/[role]/error.jsp`
- `BaseService.REDIRECT_ERROR` にリダイレクトパスが含まれる
- サービスは例外をキャッチしてエラーページにリダイレクト

## 開発メモ

### 新機能の追加手順
1. `FrontController` の switch 文にルートを追加
2. `BaseService` を継承するサービスメソッドを作成
3. 必要に応じて `BaseDAO` を継承する DAO メソッドを作成
4. SQL を `src/main/resources/sql/` ディレクトリに保存
5. 適切なロールのサブディレクトリに JSP ビューを作成
6. トランザクション用に try-with-resources で囲むことを忘れずに

### ローカルでのテスト
1. PostgreSQL がポート 5433 で動作していることを確認
2. `mvn clean package` でビルド
3. VS Code タスク「Tomcat Deploy」を使用するか、`mvn cargo:run` を実行
4. `http://localhost:8080/hidariude/` にアクセス
5. デバッグの場合: 「Tomcat Debug Deploy」タスクを使用し、「Listening for transport dt_socket at address: 8000」が表示されるまで待ってから F5 を押す

### Excel エクスポート
Excel 生成には Apache POI (5.4.1) を使用:
- 例: `InvoiceService.issueInvoiceExcel()`
- `HttpServletResponse` を通じて直接ファイルを返す（ビューのフォワードなし）
- FrontController で `response.isCommitted()` をチェックする必要がある

## デプロイメモ

### AlmaLinux 本番環境
- Java 24 インストール先: `/usr/lib/jvm/jdk-24.0.2-oracle-x64`
- Maven 3.9.11 インストール先: `/opt/apache-maven-3.9.11`
- Tomcat 10.1.46 インストール先: `/opt/tomcat/apache-tomcat-10.1.46`
- PostgreSQL 15 ポート 5433
- pgcrypto 拡張には `postgresql15-contrib` が必要

### デプロイスクリプト (`deploy.sh`)
自動デプロイプロセス:
1. 環境を検証（Java, Maven, Tomcat, PostgreSQL）
2. PostgreSQL の max_connections を 200 に確認/調整
3. Git から最新コードを取得
4. `mvn clean package -DskipTests` を実行
5. Tomcat サービスを停止
6. 古い WAR とデプロイディレクトリを削除
7. 新しい WAR を `webapps/hidariude.war` にコピー
8. Tomcat サービスを起動
9. アプリケーションのデプロイ完了を待機

### Tomcat サービス管理
```bash
systemctl status tomcat
systemctl start tomcat
systemctl stop tomcat
systemctl restart tomcat

# ログ確認
tail -f /opt/tomcat/apache-tomcat-10.1.46/logs/catalina.out
```

## 技術スタック

- **Java 24** - プログラミング言語
- **Jakarta Servlet 6.1.0** - Web フレームワーク
- **Jakarta JSP 4.0.0** - ビューテクノロジー
- **JSTL 3.0.1** - JSP 標準タグライブラリ
- **PostgreSQL JDBC 42.6.0** - データベースドライバー
- **Apache POI 5.4.1** - Excel ファイル処理
- **jBCrypt 0.4** - パスワードハッシュ化
- **Maven 3.9.11** - ビルドツール
- **Tomcat 10.1** - アプリケーションサーバー
- **Cargo Maven Plugin 1.10.14** - 開発サーバー
