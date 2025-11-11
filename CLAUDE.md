# CLAUDE.md

このファイルは Claude Code がこのリポジトリで作業する際のガイダンスを提供します。

## プロジェクト概要

BackDesk は秘書・顧客・アサイン・業務を管理するための Java Servlet ベースの Web アプリケーションです。3 つのロール（システム管理者・秘書・顧客）を持つ秘書管理システムで、業務の承認ワークフロー、月次請求、報酬計算を処理します。

**技術スタック**: Java 24, Jakarta Servlet 6.1, JSP 4.0, PostgreSQL 15 (ポート 5433), Maven, Tomcat 10.1

**データベース接続**: `TransactionManager.java` に設定 - `jdbc:postgresql://localhost:5433/backdesk`

## 主要なドメイン概念

### ユーザーロール (`LoginUser.authority`)

-   **1**: システム管理者 - 秘書・顧客・アサイン・業務の全管理
-   **2**: 秘書 - 業務登録、プロフィール管理、自身の業務閲覧
-   **3**: 顧客 - 自社の業務閲覧、担当者アカウント管理

### アサインメントシステム

-   月次で 秘書 + 顧客 + 業務ランク を紐付け
-   顧客側の請求レートと秘書側の支払いレートをそれぞれ保持
-   秘書ランクに応じて基本報酬を増額可能
-   ユニーク制約: (月、顧客 ID、秘書 ID、業務ランク ID)

### 業務承認ワークフロー

1. **未承認** - 秘書が登録、管理者承認待ち
2. **承認済み** - 管理者が承認（`approved_at` 設定）、請求対象
3. **差し戻し** - 管理者がコメント付きで却下（`remanded_at` 設定）

### 報酬計算式

-   顧客支払額 = `base_pay_customer + increase_base_pay_customer + incentive`
-   秘書受取額 = `base_pay_secretary + increase_base_pay_secretary + incentive`

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

### 3 層アーキテクチャ

**1. コントローラー層** (`controller/FrontController.java`):

-   URL パターンと switch 文によるルーティング
-   セッションベース認証
-   サービス層の呼び出しとビュー解決

**2. サービス層** (`service/`):

-   `BaseService` を継承、コンストラクタ: `new XxxService(HttpServletRequest req, boolean useDB)`
-   `useDB=true` の場合、`TransactionManager` と `Connection` を管理
-   バリデーション実行（`validation/Validation` ユーティリティ）
-   DAO 層の呼び出し
-   `String` 型のビューパスを返却

**3. DAO 層** (`dao/`):

-   `BaseDAO` を継承、サービス層から `Connection` を受け取る
-   SQL は **DAO クラス内に `private static final String SQL_XXX` として定義**
-   `PreparedStatement` による JDBC 操作
-   エラー時に `DAOException` をスロー

**4. バリデーション層** (`validation/`):

-   `Validation` クラスがユーティリティとして提供
-   エラーメッセージを内部リストに蓄積
-   `hasErrorMsg()` と `getErrorMsg()` でエラーチェックと取得
-   主なメソッド:
    -   `isNull()`: 必須チェック
    -   `isUuid()`, `isYearMonth()`: 形式検証
    -   `mustBeMoneyOrZero()`: 金額チェック（カンマ除去対応）
    -   `length()`, `isNumber()`, `isInteger()`: 基本チェック
    -   `isPostalCode()`, `isPhoneNumber()`, `isEmail()`: 日本向け形式チェック
    -   `isStrongPassword()`: パスワード強度チェック（8 文字以上、英大小数字含む）

**5. ユーティリティ層** (`util/`):

-   `PasswordUtil`: BCrypt によるパスワードハッシュ化と検証
    -   `hashPassword()`: 平文をハッシュ化（コストファクタ 10）
    -   `verifyPassword()`: 平文とハッシュの照合

**6. フィルタ層** (`filter/`):

-   `@WebFilter` アノテーションによる自動登録
-   リクエスト前処理とレスポンス後処理
-   主なフィルタ:
    -   `AlertCountFilter`: 管理者画面でアラート件数を自動取得して request スコープにセット
        -   URL パターン: `/admin/*`
        -   管理者ログイン時のみ `TaskDAO.showAlert(false)` を実行
        -   ナビバーのアラートバッジ表示に使用
        -   エラー時も処理を継続（alertCount=0 をセット）

**7. リスナー層** (`listener/`):

-   `DatabaseInitListener`: アプリケーション起動時の DB 初期化
    -   `@WebListener` で自動実行
    -   `system_admins` テーブル不在時、全テーブル作成と初期データ投入
    -   `pgcrypto` 拡張による UUID 自動生成
    -   初期データ: 管理者 10 件、秘書 10 件、顧客 10 件など
    -   マイグレーション自動実行

**8. マイグレーション層** (`listener/Migration*.java`):

-   `Migration` インターフェース: データベース変更の基底インターフェース
-   マイグレーションファイル命名規則: `Migration_YYYYMMDD_DescriptiveName.java`
-   自動実行タイミング: アプリケーション起動時
-   実行履歴管理: `schema_migrations` テーブルで管理
-   実行判定: テーブルに記録がない場合のみ実行
-   エラー時: 自動ロールバック、次回起動時に再実行
-   トランザクション管理: 呼び出し側（DatabaseInitListener）で管理
-   新規追加方法:
    1. `Migration_YYYYMMDD_YourName.java` を作成
    2. `Migration` インターフェースを実装（`up()` と `getDescription()`）
    3. `DatabaseInitListener.runMigrations()` に登録
-   詳細: README.md の Part 3 を参照

### トランザクション管理

`TransactionManager` は `AutoCloseable` を実装:

```java
try (TransactionManager tm = new TransactionManager()) {
    Connection conn = tm.getConnection();
    // DAO 操作
    tm.commit();  // 成功をマーク
} // 自動でコミット/ロールバック
```

-   自動コミット無効化された `Connection` を提供
-   明示的に `tm.commit()` を呼ぶ必要あり
-   try-with-resources で自動クローズ（commit 済みならコミット、未実行ならロールバック）

### コード構成パターン

**DTO オブジェクト** (`dto/`):

-   データベーステーブルを表現（一部は集計用の仮想オブジェクト）
-   JOIN クエリの結果を格納する平坦化された構造
-   カラムエイリアスに対応したフィールド名（例: `assignmentId`, `customerCompanyName`）
-   JDBC 互換型を使用（`java.sql.Timestamp`, `java.sql.Date`）
-   DAO 層からサービス層への受け渡し専用

**Domain オブジェクト** (`domain/`):

-   ビジネスロジック層とビュー層で使用
-   ネストした構造を持つ（例: `Task` は `Assignment` オブジェクトを含む）
-   日時型は `java.util.Date`, `LocalDateTime`, `LocalDate` を使用
-   サービス層で DTO から変換して JSP に渡される

**エラー処理**:

-   各ロール専用のエラーページ: `/WEB-INF/jsp/common/[role]/error.jsp`
-   サービスで例外をキャッチし `BaseService.REDIRECT_ERROR` へリダイレクト

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

-   Maven Clean / Maven Package
-   Tomcat Deploy / Tomcat Debug Deploy（F5 でアタッチ）

**アクセス**: http://localhost:8080/BackDesk/

**デフォルト認証情報**（全てパスワード: `Password1`）:

-   管理者: `admin1@example.com`
-   秘書: `secretary1@example.com`
-   顧客: `contact1@example.com`

### 本番デプロイ (AlmaLinux)

```bash
cd /opt/BackDesk && ./deploy.sh
```

## 開発メモ

### 新機能追加の手順

1. `FrontController` の switch 文にルート追加
2. `BaseService` を継承したサービスメソッド作成
3. `BaseDAO` を継承した DAO メソッド作成（SQL は DAO 内に定義）
4. `/WEB-INF/jsp/[role]/[feature]/` に JSP ビュー作成
5. サービスで try-with-resources による `TransactionManager` 使用を確認

### ドキュメント管理

-   **プロジェクトドキュメントは `./docs/` ディレクトリに保存**
-   アーキテクチャ設計書、品質分析、機能仕様などを Markdown で管理
-   既存: `docs/code-quality-analysis.md` - コード品質分析レポート

### Excel エクスポート

-   Apache POI 5.4.1 を使用（例: `InvoiceService.issueInvoiceExcel()`）
-   `HttpServletResponse` へ直接出力（ビュー不要）
-   `FrontController` で `response.isCommitted()` チェック必須

### データベース初期化

-   `DatabaseInitListener` が起動時に実行
-   `system_admins` テーブルが存在しない場合、全テーブル作成と初期データ投入
-   `pgcrypto` 拡張が必要（AlmaLinux は `postgresql15-contrib` パッケージ）

### データベースマイグレーション

-   データベースのスキーマやデータ変更は、マイグレーションファイルで管理
-   マイグレーションファイル: `listener/Migration_YYYYMMDD_DescriptiveName.java`
-   アプリケーション起動時に自動実行（未実行のもののみ）
-   実行履歴は `schema_migrations` テーブルで管理
-   新規作成手順:
    1. `listener/Migration_YYYYMMDD_YourName.java` を作成
    2. `Migration` インターフェースを実装
    3. `DatabaseInitListener.runMigrations()` に登録
    4. アプリケーション再起動で自動実行
-   詳細: README.md の Part 3 を参照

### Java コメント記述規約

このプロジェクトでは、以下のコメント記述スタイルを使用します。

**1. クラスレベルの Javadoc コメント**

-   すべての public クラスには Javadoc コメント（`/** */`）を記述
-   クラスの責務、概要、設計メモを記載
-   `{@link}` タグで関連クラスへの参照を明記

```java
/**
 * タスク（tasks）に関するデータアクセスを担うDAO。
 *
 * 責務：
 * - タスクの検索（秘書／顧客／月別、状態別、キーワード条件）
 * - タスクの単一取得
 * - タスクの登録・更新・論理削除・承認/承認取消/差戻し
 *
 * 設計メモ：
 * - 本DAOは呼び出し側から渡される {@link Connection} に依存
 * - DB例外は {@link DAOException} にラップして送出
 */
public class TaskDAO extends BaseDAO {
```

**2. メソッドレベルの Javadoc コメント**

-   すべての public メソッドには Javadoc コメントを記述
-   `@param`: パラメータの説明（必須）
-   `@return`: 戻り値の説明（戻り値がある場合）
-   `@throws`: 例外の説明（例外をスローする場合）
-   メソッドの動作、前提条件、注意事項を記載

```java
/**
 * 必須チェック。空／null ならエラーメッセージを積み、true を返します。
 * @param label 表示名（例: "会社名"）
 * @param value 入力値
 * @return 必須エラーがあれば true
 */
public boolean isNull(String label, String value) {
```

**3. フィールドのコメント**

-   定数、private static final フィールドにはコメントを記述
-   Javadoc スタイル（`/** */`）を使用
-   1 行で説明できる場合は 1 行コメント、複数行の説明が必要な場合は複数行コメント

```java
/** 年月フォーマッタ（yyyy-MM） */
private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

/**
 * 秘書ID・顧客ID・年月(YYYY-MM)でタスク取得（tasks + assignments + task_rank）。
 * 並び順：t.start_time
 */
private static final String SQL_SELECT_BY_SEC_CUST_MONTH = "SELECT ...";
```

**4. SQL 定義のコメント**

-   SQL 定数（`SQL_XXX`）の直前に Javadoc コメントで説明を記述
-   複数行にわたる SQL は、内部で `/** ---- セクション名 ---- */` を使用して構造化
-   複雑な条件や JOIN にはインラインコメント（`/** */`）で説明を追加

```java
/**
 * 秘書ID・顧客ID・年月(YYYY-MM)でタスク取得（tasks + assignments + task_rank）。
 * 並び順：t.start_time
 */
private static final String SQL_SELECT_BY_SEC_CUST_MONTH = "SELECT "
        /** ---- tasks.* と差戻し情報 ---- */
        + "  t.id AS t_id, t.assignment_id AS t_assignment_id, ..."
        /** ---- assignments.* ---- */
        + "  a.id AS a_id, a.customer_id AS a_customer_id, ..."
        + "WHERE a.secretary_id = ? "
        + "  AND t.work_date <  (to_date(?, 'YYYY-MM') + INTERVAL '1 month') " + /** 翌月月初 */
        + " ORDER BY t.work_date DESC";
```

**5. インラインコメント**

-   コード内の説明には `/** */` または `//` を使用
-   `/** */` は複数行にわたる説明や、より重要な説明に使用
-   `//` は簡潔な 1 行の説明に使用（使用頻度は低め）

```java
/** 数字のみ */
if (!t.matches("^\\d+$")) {
    errors.add(label + " は 0 以上の整数で入力してください。");
    return;
}
```

**6. セクション区切りコメント**

-   クラス内の構造化されたセクションを明示するために使用
-   `/** ======================== */` 形式で区切り線を使用
-   例: フィールド定義、コンストラクタ、メソッド（アクター別）などのセクション

```java
/** ========================
 * ① フィールド（SQL 定義）
 * ======================== */

/** ========================
 * ② フィールド／コンストラクタ
 * ======================== */

/** ========================
 * ③ メソッド（アクター別）
 * ---------- secretary 用 ----------
 * ========================= */
```

**7. コメント記述のベストプラクティス**

-   **明確性**: コードを読むだけで理解できる場合はコメント不要。複雑なロジックや意図が明確でない場合にのみコメントを追加
-   **日本語**: すべてのコメントは日本語で記述
-   **簡潔性**: 必要最小限の情報を提供し、冗長な説明は避ける
-   **保守性**: コード変更時は、関連するコメントも必ず更新する
-   **Javadoc タグ**: `{@link}`, `{@code}` などのタグを適切に使用して、IDE の支援を活用
