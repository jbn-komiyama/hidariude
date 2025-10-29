# CLAUDE.md

このファイルは Claude Code がこのリポジトリで作業する際のガイダンスを提供します。

## プロジェクト概要

Hidariude は秘書・顧客・アサイン・業務を管理するための Java Servlet ベースの Web アプリケーションです。3つのロール（システム管理者・秘書・顧客）を持つ秘書管理システムで、業務の承認ワークフロー、月次請求、報酬計算を処理します。

**技術スタック**: Java 24, Jakarta Servlet 6.1, JSP 4.0, PostgreSQL 15 (ポート 5433), Maven, Tomcat 10.1

**データベース接続**: `TransactionManager.java` に設定 - `jdbc:postgresql://localhost:5433/hidariude`

## 主要なドメイン概念

### ユーザーロール (`LoginUser.authority`)
- **1**: システム管理者 - 秘書・顧客・アサイン・業務の全管理
- **2**: 秘書 - 業務登録、プロフィール管理、自身の業務閲覧
- **3**: 顧客 - 自社の業務閲覧、担当者アカウント管理

### アサインメントシステム
- 月次で 秘書 + 顧客 + 業務ランク を紐付け
- 顧客側の請求レートと秘書側の支払いレートをそれぞれ保持
- 秘書ランクに応じて基本報酬を増額可能
- ユニーク制約: (月、顧客ID、秘書ID、業務ランクID)

### 業務承認ワークフロー
1. **未承認** - 秘書が登録、管理者承認待ち
2. **承認済み** - 管理者が承認（`approved_at` 設定）、請求対象
3. **差し戻し** - 管理者がコメント付きで却下（`remanded_at` 設定）

### 報酬計算式
- 顧客支払額 = `base_pay_customer + increase_base_pay_customer + incentive`
- 秘書受取額 = `base_pay_secretary + increase_base_pay_secretary + incentive`

## アーキテクチャ概要

### フロントコントローラーパターン
単一の `FrontController` サーブレットが全リクエストを処理:

**URL パターン**: `/admin/*`, `/secretary/*`, `/customer/*`

**リクエストフロー**:
1. `FrontController.execute()` がリクエスト受信
2. `servletPath` でロール判定、`pathInfo` でアクション特定
3. セッションの `LoginUser` で認証チェック
4. ロール別の execute メソッドにルーティング（`adminExecute()` など）
5. サービス層がビジネスロジック実行
6. ビューパス文字列を返却（相対パス=forward、絶対パス=redirect）
7. JSP へフォワード: `/WEB-INF/jsp/[role]/[feature]/[page].jsp`

### 3層アーキテクチャ

**1. コントローラー層** (`controller/FrontController.java`):
- URL パターンと switch 文によるルーティング
- セッションベース認証
- サービス層の呼び出しとビュー解決

**2. サービス層** (`service/`):
- `BaseService` を継承、コンストラクタ: `new XxxService(HttpServletRequest req, boolean useDB)`
- `useDB=true` の場合、`TransactionManager` と `Connection` を管理
- バリデーション実行（`validation/Validation` ユーティリティ）
- DAO 層の呼び出し
- `String` 型のビューパスを返却

**3. DAO 層** (`dao/`):
- `BaseDAO` を継承、サービス層から `Connection` を受け取る
- SQL は **DAO クラス内に `private static final String SQL_XXX` として定義**
- `PreparedStatement` による JDBC 操作
- エラー時に `DAOException` をスロー

**4. バリデーション層** (`validation/`):
- `Validation` クラスがユーティリティとして提供
- エラーメッセージを内部リストに蓄積
- `hasErrorMsg()` と `getErrorMsg()` でエラーチェックと取得
- 主なメソッド:
  - `isNull()`: 必須チェック
  - `isUuid()`, `isYearMonth()`: 形式検証
  - `mustBeMoneyOrZero()`: 金額チェック（カンマ除去対応）
  - `length()`, `isNumber()`, `isInteger()`: 基本チェック
  - `isPostalCode()`, `isPhoneNumber()`, `isEmail()`: 日本向け形式チェック
  - `isStrongPassword()`: パスワード強度チェック（8文字以上、英大小数字含む）

**5. ユーティリティ層** (`util/`):
- `PasswordUtil`: BCrypt によるパスワードハッシュ化と検証
  - `hashPassword()`: 平文をハッシュ化（コストファクタ10）
  - `verifyPassword()`: 平文とハッシュの照合

**6. リスナー層** (`listener/`):
- `DatabaseInitListener`: アプリケーション起動時のDB初期化
  - `@WebListener` で自動実行
  - `system_admins` テーブル不在時、全テーブル作成と初期データ投入
  - `pgcrypto` 拡張による UUID 自動生成
  - 初期データ: 管理者10件、秘書10件、顧客10件など

### トランザクション管理

`TransactionManager` は `AutoCloseable` を実装:

```java
try (TransactionManager tm = new TransactionManager()) {
    Connection conn = tm.getConnection();
    // DAO 操作
    tm.commit();  // 成功をマーク
} // 自動でコミット/ロールバック
```

- 自動コミット無効化された `Connection` を提供
- 明示的に `tm.commit()` を呼ぶ必要あり
- try-with-resources で自動クローズ（commit 済みならコミット、未実行ならロールバック）

### コード構成パターン

**DTO オブジェクト** (`dto/`):
- データベーステーブルを表現（一部は集計用の仮想オブジェクト）
- JOIN クエリの結果を格納する平坦化された構造
- カラムエイリアスに対応したフィールド名（例: `assignmentId`, `customerCompanyName`）
- JDBC 互換型を使用（`java.sql.Timestamp`, `java.sql.Date`）
- DAO 層からサービス層への受け渡し専用

**Domain オブジェクト** (`domain/`):
- ビジネスロジック層とビュー層で使用
- ネストした構造を持つ（例: `Task` は `Assignment` オブジェクトを含む）
- 日時型は `java.util.Date`, `LocalDateTime`, `LocalDate` を使用
- サービス層で DTO から変換して JSP に渡される

**エラー処理**:
- 各ロール専用のエラーページ: `/WEB-INF/jsp/common/[role]/error.jsp`
- サービスで例外をキャッチし `BaseService.REDIRECT_ERROR` へリダイレクト

## よく使う開発コマンド

### ビルド
```bash
mvn clean package              # WAR 生成
mvn package -DskipTests        # テストスキップ
```

### ローカル開発
```bash
mvn cargo:run                  # 開発サーバー起動
mvnDebug cargo:run             # デバッグモード（ポート 8000）
```

**VS Code タスク**: Ctrl+Shift+P → Tasks: Run Task から選択
- Maven Clean / Maven Package
- Tomcat Deploy / Tomcat Debug Deploy（F5 でアタッチ）

**アクセス**: http://localhost:8080/hidariude/

**デフォルト認証情報**（全てパスワード: `Password1`）:
- 管理者: `admin1@example.com`
- 秘書: `secretary1@example.com`
- 顧客: `contact1@example.com`

### 本番デプロイ (AlmaLinux)
```bash
cd /opt/hidariude && ./deploy.sh
```

## 開発メモ

### 新機能追加の手順
1. `FrontController` の switch 文にルート追加
2. `BaseService` を継承したサービスメソッド作成
3. `BaseDAO` を継承した DAO メソッド作成（SQL は DAO 内に定義）
4. `/WEB-INF/jsp/[role]/[feature]/` に JSP ビュー作成
5. サービスで try-with-resources による `TransactionManager` 使用を確認

### ドキュメント管理
- **プロジェクトドキュメントは `./docs/` ディレクトリに保存**
- アーキテクチャ設計書、品質分析、機能仕様などを Markdown で管理
- 既存: `docs/code-quality-analysis.md` - コード品質分析レポート

### Excel エクスポート
- Apache POI 5.4.1 を使用（例: `InvoiceService.issueInvoiceExcel()`）
- `HttpServletResponse` へ直接出力（ビュー不要）
- `FrontController` で `response.isCommitted()` チェック必須

### データベース初期化
- `DatabaseInitListener` が起動時に実行
- `system_admins` テーブルが存在しない場合、全テーブル作成と初期データ投入
- `pgcrypto` 拡張が必要（AlmaLinux は `postgresql15-contrib` パッケージ）
