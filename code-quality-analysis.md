# コードベース品質分析レポート
## Hidariude 秘書管理システム

**分析日:** 2025-10-28
**コードベース:** Java Servlet Web Application (62 Java ファイル分析)
**分析レベル:** 詳細分析

---

## エグゼクティブサマリー

Hidariude コードベースは、Jakarta Servlet、PostgreSQL、Apache POI を使用した、よく構造化された3層アーキテクチャを示しています。しかし、分析の結果、セキュリティ、パフォーマンス、コード品質、保守性にわたる複数のクリティカルおよび高重要度の問題が特定されました。最も重要な問題には、XSS 脆弱性、一貫性のない例外処理、ハードコードされた認証情報、セッション管理に関する懸念が含まれます。

---

## クリティカルな問題

### 1. XSS 脆弱性 - TaskService での直接的な属性割り当て
**ファイル:** `src\main\java\service\TaskService.java` (431行目)
**重要度:** クリティカル
**カテゴリ:** セキュリティ - XSS 脆弱性

```java
req.setAttribute("remandComment", req.getParameter("remandComment"));
```

**問題:** `req.getParameter("remandComment")` からのユーザー入力が、エスケープやサニタイゼーション処理なしに直接リクエスト属性に割り当てられています。これは EL 式（例：`${remandComment}`）を使用して JSP でレンダリングされる可能性があり、JSP が `<c:out>` エスケープなしでこの値を使用すると、クロスサイトスクリプティング攻撃につながります。

**リスク:** 攻撃者は `remandComment` パラメータを通じて悪意のある JavaScript コードを注入でき、タスクを閲覧する他のユーザーのブラウザで実行されます。

**推奨修正:**
```java
// オプション1: Apache Commons Lang を使用して HTML をエスケープ
String safeComment = org.apache.commons.lang3.StringEscapeUtils.escapeHtml4(
    req.getParameter("remandComment"));
req.setAttribute("remandComment", safeComment);

// オプション2: JSP で常に <c:out> を使用（よりエレガント）
// JSP内: <c:out value="${remandComment}" escapeXml="true" />

// オプション3: 安全な属性割り当て用のユーティリティメソッドを作成
private String sanitizeInput(String input) {
    if (input == null) return "";
    return input.replaceAll("[<>\"'&]", "");
}
```

**追加確認箇所:** EL 式を適切なエスケープなしで使用している類似パターンについて、すべての JSP ファイルを検索してください。JSP テンプレート内の `${remandComment}` が `<c:out escapeXml="true">` を使用していることを確認してください。

---

### 2. ハードコードされたデータベース認証情報
**ファイル:**
- `src\main\java\dao\TransactionManager.java` (8-11行目)
- `src\main\java\listener\DatabaseInitListener.java` (23-27行目)

**重要度:** クリティカル
**カテゴリ:** セキュリティ - 認証情報の露出

```java
private static final String DB_URL = "jdbc:postgresql://localhost:5433/hidariude";
private static final String DB_USER = "postgres";
private static final String DB_PASSWORD = "password";
```

**問題:** データベース認証情報がソースコードにハードコードされています。これは複数のセキュリティリスクを生み出します：
- 認証情報がバージョン管理システムに保存される
- コードへのアクセス権を持つ誰もが認証情報を閲覧できる
- 開発環境と本番環境で同じ認証情報が使用される
- コードのデプロイなしでは認証情報をローテーションできない

**リスク:**
- コードが漏洩またはアクセスされた場合のデータベース侵害
- すべての環境で同一の認証情報を使用
- セキュリティ標準（OWASP、CWE-798）への非準拠

**推奨修正:**
```java
// 環境変数またはプロパティファイルを使用
private static final String DB_URL = System.getenv("DB_URL")
    != null ? System.getenv("DB_URL")
    : "jdbc:postgresql://localhost:5433/hidariude";
private static final String DB_USER = System.getenv("DB_USER")
    != null ? System.getenv("DB_USER")
    : "postgres";
private static final String DB_PASSWORD = System.getenv("DB_PASSWORD")
    != null ? System.getenv("DB_PASSWORD")
    : "password";

// または設定ファイルを使用:
// 読み込み元: src/main/resources/application.properties
Properties props = new Properties();
props.load(TransactionManager.class.getResourceAsStream("/application.properties"));
String dbUrl = props.getProperty("db.url");
```

---

### 3. トランザクションリソースリーク - 不完全な AutoCloseable 処理
**ファイル:** `src\main\java\dao\TransactionManager.java` (47-63行目)
**重要度:** クリティカル
**カテゴリ:** リソース管理 - 潜在的なリーク

```java
@Override
public void close() {
    try {
        if(conn != null) {
            if (isCommit) {
                conn.commit();
            } else {
                conn.rollback();
            }
            conn.close();
            conn = null;
        }
    } catch(SQLException e) {
        String message = "E:TM03 トランザクション終了中にエラーが発生しました";
        throw new TransactionException(message, e);
    }
}
```

**問題:** `conn.commit()` または `conn.rollback()` が例外をスローすると、`conn.close()` は決して呼び出されず、データベース接続が無期限に開いたままになります。これにより接続プールが枯渇します。

**リスク:**
- データベース接続プールの枯渇
- アプリケーションが応答しなくなる
- メモリリーク
- DoS 脆弱性

**推奨修正:**
```java
@Override
public void close() {
    try {
        if(conn != null) {
            try {
                if (isCommit) {
                    conn.commit();
                } else {
                    conn.rollback();
                }
            } catch(SQLException e) {
                // エラーをログに記録するが、接続を確実に閉じる
                System.err.println("コミット/ロールバック中のエラー: " + e.getMessage());
                throw new TransactionException("E:TM03 トランザクション終了中にエラーが発生しました", e);
            } finally {
                // 常に接続を閉じる
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("接続クローズ中のエラー: " + e.getMessage());
                }
            }
            conn = null;
        }
    } catch(Exception e) {
        String message = "E:TM03 トランザクション終了中にエラーが発生しました";
        throw new TransactionException(message, e);
    }
}
```

---

### 4. クリティカルフローでの Null チェックの欠如 - 潜在的な NullPointerException
**ファイル:** `src\main\java\controller\FrontController.java` (583行目)
**重要度:** クリティカル
**カテゴリ:** ランタイムエラー - NullPointerException

```java
char firstPath = nextPath.charAt(0);
```

**問題:** `nextPath` は特定の実行パスで null になる可能性がありますが、`charAt(0)` を呼び出す前に null チェックがありません。変数は523行目で `"index"` に初期化されていますが、特定のサービスメソッドは適切な処理なしに null を返す可能性があります。

**リスク:**
- NullPointerException によるアプリケーションのクラッシュ
- 完全なサービス停止
- ユーザー体験の低下

**推奨修正:**
```java
// nextPath を使用する前に null チェックを追加
if (nextPath == null || nextPath.isEmpty()) {
    nextPath = "index"; // デフォルトのフォールバック
}

char firstPath = nextPath.charAt(0);
```

または各サービスメソッドに検証を追加:
```java
// 各サービスメソッドで、null でない戻り値を保証
if (nextPath == null) {
    nextPath = REDIRECT_ERROR;
}
```

---

## 高重要度の問題

### 5. セッション固定脆弱性 - JSESSIONID の再生成の欠如
**ファイル:** `src\main\java\service\CommonService.java` (138-155、185-205、525-560行目)
**重要度:** 高
**カテゴリ:** セキュリティ - セッション管理

```java
public String secretaryLogin() {
    // ... バリデーション ...
    try (TransactionManager tm = new TransactionManager()) {
        SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
        SecretaryDTO dto = dao.selectByMail(loginId);
        if (dto != null && PasswordUtil.verifyPassword(password, dto.getPassword())) {
            // セッションが再生成なしで作成される
            LoginUser loginUser = new LoginUser();
            // ...
            putLoginUserToSession(loginUser); // 154行目
        }
    }
}

private void putLoginUserToSession(LoginUser loginUser) {
    HttpSession session = req.getSession(true); // 必要に応じて新しいセッションを作成
    session.setAttribute(ATTR_LOGIN_USER, loginUser);
}
```

**問題:** 認証成功後、コードはセッションIDを再生成しません。攻撃者は、既知のセッションIDでユーザーにログインを強制し、認証後にセッションを乗っ取るセッション固定攻撃を実行できます。

**リスク:**
- ログイン後のセッションハイジャック
- ユーザーアカウントの侵害
- ユーザーデータへの不正アクセス
- CWE-384 違反

**推奨修正:**
```java
private void putLoginUserToSession(LoginUser loginUser) {
    HttpSession oldSession = req.getSession(false);
    if (oldSession != null) {
        oldSession.invalidate(); // 古いセッションを無効化
    }

    // 新しい JSESSIONID で新しいセッションを作成
    HttpSession newSession = req.getSession(true);
    newSession.setAttribute(ATTR_LOGIN_USER, loginUser);
}
```

---

### 6. 不適切な例外処理 - printStackTrace() による例外の吸収
**ファイル:**
- `src\main\java\service\CommonService.java` (285、501、681行目)
- `src\main\java\service\TaskService.java` (273-274、443-444行目)
- `src\main\java\service\AssignmentService.java` (複数行)
- `src\main\java\listener\DatabaseInitListener.java` (60行目)

**重要度:** 高
**カテゴリ:** コード品質 - エラー処理

```java
catch (RuntimeException e) {
    e.printStackTrace(); // アンチパターン
    return req.getContextPath() + req.getServletPath() + "/error";
}
```

**問題:**
1. `e.printStackTrace()` は stderr に出力を送信し、ロギングシステムには送信しない
2. 構造化されたロギングがない（ログレベル、タイムスタンプ、コンテキストなし）
3. エラートラッキングや監視機能がない
4. セキュリティリスク：本番環境で例外の詳細が機密情報を漏洩する可能性
5. 本番環境で問題をデバッグすることが困難

**リスク:**
- 本番環境でのエラー情報の損失
- リクエスト間でエラーを関連付けられない
- セキュリティ情報の開示
- コンプライアンス違反（監査証跡）

**推奨修正:**
```java
import java.util.logging.Logger;
import java.util.logging.Level;

private static final Logger logger = Logger.getLogger(CommonService.class.getName());

catch (RuntimeException e) {
    logger.log(Level.SEVERE, "管理者ホーム処理中のエラー", e);
    req.setAttribute("errorMsg", "予期せぬエラーが発生しました。管理者にお問い合わせください。");
    return REDIRECT_ERROR;
}
```

または SLF4J のような適切なロギングフレームワークを使用:
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger logger = LoggerFactory.getLogger(CommonService.class);

catch (RuntimeException e) {
    logger.error("処理中のエラー", e);
    req.setAttribute("errorMsg", "エラーが発生しました。サポートにお問い合わせください。");
    return REDIRECT_ERROR;
}
```

---

### 7. 一貫性のない認証チェック - 認可ロジックの欠陥
**ファイル:** `src\main\java\controller\FrontController.java` (537-547行目)
**重要度:** 高
**カテゴリ:** セキュリティ - 認証/認可

```java
case "/admin"->{
    // ログインチェック：ルートとログインパス以外は認証必須
    if (!isRootPath && !isLoginPath) {
        boolean loggedIn = (loginUser != null && loginUser.getAuthority() == 1);
        if (!loggedIn) {
            res.sendRedirect(contextPath + "/admin");
            return;
        }
    }

    if(pathInfo == null)  nextPath = "common/admin/login";
    else adminExecute(req, res);
}
```

**問題:** チェックはユーザーが `authority == 1` を持っているかどうかのみを確認します。しかし、秘書（authority 2）または顧客（authority 3）が以前のログインから有効なセッションを持っている場合、特定の URL を作成することで管理者ページにアクセスできる可能性があります。

**リスク:**
- クロスロール権限昇格
- 機密機能への不正アクセス
- データの窃盗または改ざん

**推奨修正:**
```java
case "/admin"->{
    if (!isRootPath && !isLoginPath) {
        // ユーザーがログインしており、正しいロールを持っていることを確認
        boolean isAuthorizedAdmin = (loginUser != null
                                    && loginUser.getAuthority() == 1
                                    && loginUser.getSystemAdmin() != null
                                    && loginUser.getSystemAdmin().getId() != null);
        if (!isAuthorizedAdmin) {
            // 不正なアクセス試行をログに記録
            logger.warn("セッションからの不正な管理者アクセス試行: {}",
                       session != null ? session.getId() : "no-session");
            res.sendRedirect(contextPath + "/admin");
            return;
        }
    }

    if(pathInfo == null)  nextPath = "common/admin/login";
    else adminExecute(req, res);
}
```

---

### 8. 動的クエリ構築における SQL インジェクションリスク
**ファイル:** `src\main\java\dao\TaskDAO.java` (53-80行目および類似パターン)
**重要度:** 高
**カテゴリ:** セキュリティ - SQL インジェクション

コードベースのほとんどは `PreparedStatement` を正しく使用していますが、複雑なクエリ構築には潜在的なリスクがあります：

```java
private static final String SQL_SELECT_BY_SEC_CUST_MONTH = "SELECT ... WHERE ... ?";
// PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_SEC_CUST_MONTH);
// ps.setString(1, secretaryId); // これは正しい
```

**問題:** 現在の実装はパラメータ化されたクエリを正しく使用していますが、文字列連結の可能性がある複雑な SQL 文字列は脆弱性を導入する可能性があります。また、開発者が文字列連結を使用して新しいクエリを追加した場合、コードベースは脆弱になります。

**リスク:**
- 新しいクエリが文字列連結を使用する場合の SQL インジェクション
- データベースの侵害
- 不正なデータアクセス

**推奨修正:**
1. **コードレビューチェックリスト** - すべての新しい SQL クエリが以下を満たすことを確認：
   - プレースホルダー（?）を使用した `PreparedStatement` を使用
   - ユーザー入力を直接連結しない
   - 明確性のために名前付きパラメータを使用

2. **静的解析の追加** - 以下のようなツールを使用：
   - FindBugs/SpotBugs（SQL インジェクションパターンを検出）
   - OWASP Dependency-Check

---

## 中程度の重要度の問題

### 9. 未検証のリダイレクト - オープンリダイレクト脆弱性
**ファイル:** `src\main\java\controller\FrontController.java` (586行目)
**重要度:** 中
**カテゴリ:** セキュリティ - オープンリダイレクト

```java
res.sendRedirect(res.encodeRedirectURL(nextPath));
```

**問題:** `nextPath` は内部で生成されますが、サービスメソッドがユーザー制御の入力を返すように影響される可能性がある場合、これはオープンリダイレクト脆弱性を生み出す可能性があります。

**リスク:**
- フィッシング攻撃
- アカウントハイジャック
- マルウェア配布

**推奨修正:**
```java
// リダイレクトターゲットを検証
private static final List<String> ALLOWED_REDIRECT_PREFIXES = Arrays.asList(
    "/admin/", "/secretary/", "/customer/", "/index"
);

private boolean isValidRedirect(String path) {
    if (path == null || path.isEmpty()) return false;
    return ALLOWED_REDIRECT_PREFIXES.stream()
        .anyMatch(path::startsWith);
}

// 使用例:
if (firstPath == '/' && isValidRedirect(nextPath)) {
    res.sendRedirect(res.encodeRedirectURL(nextPath));
} else {
    req.getRequestDispatcher("/WEB-INF/jsp/" + nextPath + ".jsp")
        .forward(req, res);
}
```

---

### 10. 弱いセッション設定
**ファイル:** `src\main\webapp\WEB-INF\web.xml` (22行目)
**重要度:** 中
**カテゴリ:** セキュリティ - セッション管理

```xml
<cookie-config>
    <http-only>true</http-only>
    <secure>false</secure>
</cookie-config>
```

**問題:**
1. `secure="false"` は、セッションクッキーが HTTP（暗号化なし）で送信されることを意味します
2. 本番環境では、これによりネットワーク傍受によるセッションハイジャックが可能になります
3. CSRF 保護のための SameSite 属性がありません

**リスク:**
- ネットワークスニッフィングによるセッションハイジャック
- 中間者攻撃
- CSRF 攻撃

**推奨修正:**
```xml
<session-config>
    <session-timeout>30</session-timeout>
    <cookie-config>
        <http-only>true</http-only>
        <secure>true</secure>
        <same-site>Strict</same-site>
    </cookie-config>
    <tracking-mode>COOKIE</tracking-mode>
</session-config>
```

---

### 11. すべてのパスでデータベース接続がクローズされない
**ファイル:** `src\main\java\listener\DatabaseInitListener.java` (74-87行目)
**重要度:** 中
**カテゴリ:** リソース管理

```java
private void updateBankTypeConstraint(Connection conn) {
    try (Statement stmt = conn.createStatement()) {
        // ... ステートメントを実行 ...
        conn.commit();
        System.out.println("Updated bank_type constraint...");
    } catch (SQLException e) {
        System.err.println("Warning: Could not update bank_type constraint: " + e.getMessage());
        // エラーが発生するが例外が吸収される
    }
}
```

**問題:** Statement は適切にクローズされますが、`conn.commit()` が例外をスローした場合、接続は一貫性のない状態のまま残る可能性があります。アプリケーションの起動は失敗しませんが、データベースの状態が破損する可能性があります。

**リスク:**
- 不完全なスキーマ初期化
- アプリケーションライフサイクルの後の段階でランタイムエラー
- 一貫性のないデータベース状態

**推奨修正:**
```java
private void updateBankTypeConstraint(Connection conn) {
    try (Statement stmt = conn.createStatement()) {
        stmt.execute("ALTER TABLE secretaries DROP CONSTRAINT IF EXISTS chk_secretaries_bank_type");
        stmt.execute("ALTER TABLE secretaries ADD CONSTRAINT chk_secretaries_bank_type " +
                    "CHECK (bank_type IS NULL OR bank_type = '' OR bank_type IN ('普通', '当座'))");
        try {
            conn.commit();
            System.out.println("Updated bank_type constraint to allow empty values.");
        } catch (SQLException commitError) {
            conn.rollback();
            logger.warn("bank_type 制約更新のコミットに失敗", commitError);
            throw new RuntimeException("データベース初期化に失敗", commitError);
        }
    } catch (SQLException e) {
        logger.error("bank_type 制約更新中のエラー", e);
        throw new RuntimeException("データベース初期化に失敗", e);
    }
}
```

---

### 12. N+1 クエリ問題 - ループ内の複数 DAO 呼び出し
**ファイル:** `src\main\java\service\CommonService.java` (452-470行目)
**重要度:** 中
**カテゴリ:** パフォーマンス

```java
List<AssignmentDTO> adtosFlat =
    new AssignmentDAO(tm.getConnection())
        .selectBySecretaryAndMonthToAssignment(secretaryId, yearMonth);
List<Map<String, Object>> assignRows = new ArrayList<>();
for (AssignmentDTO a : adtosFlat) {
    // ... 各 DTO を処理 ...
    assignRows.add(row);
}
```

**問題:** `selectBySecretaryAndMonthToAssignment()` が関連データに対して個別のクエリを実行する場合、これは N+1 クエリ問題になります。各アサインメントが追加のクエリをトリガーする可能性があります。

**リスク:**
- ページロード時間の遅延
- データベースサーバーの過負荷
- スケーラビリティの問題

**推奨修正:**
```java
// DAO メソッドが単一の JOIN クエリを使用していることを確認
// そうでない場合は、以下にリファクタリング:
AssignmentDAO asgDao = new AssignmentDAO(tm.getConnection());
List<AssignmentDTO> adtosFlat = asgDao
    .selectBySecretaryAndMonthToAssignmentWithJoins(secretaryId, yearMonth);

// またはキャッシングを追加:
@Cacheable(value = "assignments", key = "#secretaryId + #yearMonth")
public List<AssignmentDTO> getAssignmentsForSecretary(UUID secretaryId, String yearMonth) {
    return asgDao.selectBySecretaryAndMonthToAssignment(secretaryId, yearMonth);
}
```

---

## コード品質の問題

### 13. コードの重複 - 類似したログインメソッド
**ファイル:**
- `src\main\java\service\CommonService.java` (126-163、174-214、514-568行目)

**重要度:** 中
**カテゴリ:** コード品質 - DRY 原則

3つのログインメソッドはほぼ同一の構造を持っています：
- `secretaryLogin()` - 126-163行目
- `adminLogin()` - 174-214行目
- `customerLogin()` - 514-568行目

```java
public String secretaryLogin() {
    final String loginId  = req.getParameter(P_LOGIN_ID);
    final String password = req.getParameter(P_PASSWORD);

    validation.isNull("ログインID", loginId);
    validation.isNull("パスワード", password);
    if (validation.hasErrorMsg()) {
        req.setAttribute("errorMsg", validation.getErrorMsg());
        return PATH_SECRETARY_LOGIN_FORM;
    }

    try (TransactionManager tm = new TransactionManager()) {
        // 類似パターンが3回繰り返される...
    }
}
```

**問題:** コードの繰り返しはメンテナンスの悪夢につながり、1つのメソッドが更新された場合に一貫性のない動作を引き起こします。

**推奨修正:**
```java
private interface LoginHandler {
    Object findUser(String email) throws DAOException;
    String validate(String email, Object user, String password);
    LoginUser createLoginUser(Object user, int authority);
}

private String executeLogin(String loginIdParam, String passwordParam,
                           String errorFormPath, String successPath,
                           int requiredAuthority, LoginHandler handler) {
    final String loginId  = req.getParameter(loginIdParam);
    final String password = req.getParameter(passwordParam);

    validation.isNull("ログインID", loginId);
    validation.isNull("パスワード", password);
    if (validation.hasErrorMsg()) {
        req.setAttribute("errorMsg", validation.getErrorMsg());
        return errorFormPath;
    }

    try (TransactionManager tm = new TransactionManager()) {
        Object user = handler.findUser(loginId);
        String validationMsg = handler.validate(loginId, user, password);

        if (validationMsg == null) {
            LoginUser loginUser = handler.createLoginUser(user, requiredAuthority);
            putLoginUserToSession(loginUser);
            return successPath;
        }

        req.setAttribute("errorMsg", validationMsg);
        return errorFormPath;
    } catch (RuntimeException e) {
        return REDIRECT_ERROR;
    }
}
```

---

### 14. 長いメソッド - 単一責任原則の違反
**ファイル:** `src\main\java\service\TaskService.java` (244-277、291-370行目)
**重要度:** 中
**カテゴリ:** コード品質 - 保守性

`taskRegisterDone()` のようなメソッドは100行以上あり、複数の責任を処理しています：
1. パラメータの抽出
2. バリデーション
3. 日付/時刻の解析
4. DAO 操作
5. トランザクション管理

**推奨修正:** より小さな焦点を絞ったメソッドに分割：
```java
public String taskRegisterDone() {
    TaskRegistrationDTO dto = extractAndValidateParameters();
    if (dto == null) return REDIRECT_ERROR;

    return saveTask(dto);
}

private TaskRegistrationDTO extractAndValidateParameters() {
    // 抽出と検証
}

private String saveTask(TaskRegistrationDTO dto) {
    // 永続化を処理
}
```

---

### 15. マジックナンバーと文字列
**ファイル:** 複数のファイル
**重要度:** 中
**カテゴリ:** コード品質 - 保守性

例:
- `loginUser.getAuthority() == 1` （"1"とは何か？システム管理者ロール）
- `req.setCharacterEncoding("UTF-8")` （ハードコードされた文字セット）
- `"E:TM01"`, `"E:TM02"` （エラーコード）
- web.xml の `30` （分単位のセッションタイムアウト）

**推奨修正:** 定数を使用：
```java
public static final class AuthorityLevel {
    public static final int SYSTEM_ADMIN = 1;
    public static final int SECRETARY = 2;
    public static final int CUSTOMER = 3;
}

public static final class ErrorCode {
    public static final String TRANSACTION_START_FAILED = "E:TM01";
    public static final String TRANSACTION_NOT_STARTED = "E:TM02";
    public static final String TRANSACTION_END_FAILED = "E:TM03";
}

// 使用例:
if (loginUser.getAuthority() == AuthorityLevel.SYSTEM_ADMIN) {
    // ...
}
```

---

### 16. Javadoc コメントの欠如
**ファイル:** ほとんどの Java ファイル
**重要度:** 中
**カテゴリ:** コード品質 - ドキュメンテーション

多くの public メソッドに適切な JavaDoc がありません。例：
```java
public String secretaryLogin() {
    // JavaDoc なし - これは何を返すか？いつ呼び出されるか？
}
```

**推奨修正:** 包括的な JavaDoc を追加：
```java
/**
 * メールアドレスとパスワードで秘書ユーザーを認証します。
 *
 * <p>以下のステップを実行します:
 * <ol>
 *   <li>ログイン認証情報を検証</li>
 *   <li>一致する秘書をデータベースで照会</li>
 *   <li>BCrypt を使用してパスワードを検証</li>
 *   <li>セッションを作成して保存</li>
 * </ol>
 * </p>
 *
 * @return 成功時はホームページへのフォワードパス、失敗時はログインフォーム
 * @throws ServiceException データベース操作が失敗した場合
 * @see SecretaryDAO#selectByMail(String)
 */
public String secretaryLogin() {
    // ...
}
```

---

### 17. 一貫性のない Null 処理
**ファイル:** `src\main\java\service\CommonService.java` (300-308行目)
**重要度:** 中
**カテゴリ:** コード品質 - 堅牢性

```java
public String adminMyPage() {
    HttpSession session = req.getSession(false);
    if (session == null) return req.getContextPath() + PATH_ADMIN_LOGIN;
    LoginUser lu = (LoginUser) session.getAttribute(ATTR_LOGIN_USER);
    if (lu == null || lu.getSystemAdmin() == null || lu.getSystemAdmin().getId() == null) {
        return req.getContextPath() + PATH_ADMIN_LOGIN;
    }
    // ...
}
```

**問題:** 複数のネストされた null チェックは面倒でエラーが発生しやすくなります。一部のメソッドはこのパターンを使用し、他のメソッドは使用しません。

**推奨修正:**
```java
// ヘルパーメソッドを作成
private boolean isValidAdminSession(LoginUser loginUser) {
    return loginUser != null
        && loginUser.getSystemAdmin() != null
        && loginUser.getSystemAdmin().getId() != null;
}

// または Java Optional を使用
private String adminMyPage() {
    return Optional.ofNullable(req.getSession(false))
        .map(s -> (LoginUser) s.getAttribute(ATTR_LOGIN_USER))
        .filter(this::isValidAdminSession)
        .map(lu -> {
            req.setAttribute(ATTR_ADMIN, lu.getSystemAdmin());
            return PATH_ADMIN_MYPAGE;
        })
        .orElse(req.getContextPath() + PATH_ADMIN_LOGIN);
}
```

---

### 18. 未使用のコード - コメントアウトされたメソッド
**ファイル:** `src\main\java\service\AssignmentService.java`
**重要度:** 低（保守性の観点からは中）
**カテゴリ:** コード品質

```java
//        try (TransactionManager tm = new TransactionManager()) {
//            // ... コメントアウトされたコード ...
//        }
```

**問題:** コメントアウトされたコードは、それを使用すべきかどうかについて混乱を生み、リファクタリングを困難にします。

**推奨修正:**
- コメントアウトされたコードを削除
- 履歴的に重要な場合は、Git 履歴を使用して取得
- フィーチャーフラグの代わりにバージョン管理を使用

---

## セキュリティの問題（追加）

### 19. CSRF トークンが実装されていない
**ファイル:** ほとんどの JSP ファイル
**重要度:** 中
**カテゴリ:** セキュリティ - CSRF

すべてのフォームに CSRF トークン保護が欠けているようです：
```jsp
<form method="post" action="<%=request.getContextPath()%>/admin/login">
    <!-- CSRF トークンの hidden input がない -->
</form>
```

**リスク:**
- クロスサイトリクエストフォージェリ攻撃
- 不正な状態変更操作

**推奨修正:**
```java
// サービス層で
UUID csrfToken = UUID.randomUUID();
req.getSession().setAttribute("csrfToken", csrfToken.toString());
req.setAttribute("csrfToken", csrfToken.toString());

// JSP で
<form method="post" action="...">
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <!-- その他のフィールド -->
</form>

// サービスバリデーションで
String providedToken = req.getParameter("csrfToken");
String sessionToken = (String) req.getSession().getAttribute("csrfToken");
if (!providedToken.equals(sessionToken)) {
    throw new SecurityException("CSRF トークンが無効です");
}
```

---

### 20. パスワード変更の監査がない
**ファイル:** `src\main\java\service\CommonService.java` (386-410行目)
**重要度:** 中
**カテゴリ:** セキュリティ - 監査証跡

管理者がパスワードを更新する際、監査ログが作成されません：
```java
if (password != null && !password.isBlank()) {
    if (!validation.isStrongPassword(password)) {
        // バリデーション...
    }
    upd.setPassword(PasswordUtil.hashPassword(password));
    // 監査ログなし
}
```

**リスク:**
- パスワードを変更した人を追跡できない
- セキュリティインシデントを追跡できない
- コンプライアンス違反

**推奨修正:**
```java
upd.setPassword(PasswordUtil.hashPassword(password));
logger.info("管理者 {} がユーザー {} のパスワードを変更しました",
    getCurrentAdminId(), adminId);
// または監査レコードを挿入
new AuditLogDAO(conn).logPasswordChange(adminId, getCurrentAdminId(), LocalDateTime.now());
```

---

## パフォーマンスの問題

### 21. データベースインデックスの欠如
**ファイル:** `src\main\java\listener\DatabaseInitListener.java`
**重要度:** 中
**カテゴリ:** パフォーマンス

```java
ddlStatements.add("CREATE INDEX IF NOT EXISTS idx_tasks_alerted_at ON tasks (alerted_at) WHERE deleted_at IS NULL");
```

**問題:** tasks テーブルに対して明示的に作成されているインデックスは1つだけです。頻繁に照会される列に対するインデックスが欠落しています：
- `tasks.assignment_id`
- `assignments.secretary_id`
- `assignments.customer_id`
- `tasks.work_date`
- `assignments.target_year_month`

**リスク:**
- クエリの遅延
- フルテーブルスキャン
- 高いデータベース負荷

**推奨修正:**
```java
// DDL にインデックスを追加
ddlStatements.add("CREATE INDEX idx_tasks_assignment_id ON tasks(assignment_id) WHERE deleted_at IS NULL");
ddlStatements.add("CREATE INDEX idx_tasks_work_date ON tasks(work_date) WHERE deleted_at IS NULL");
ddlStatements.add("CREATE INDEX idx_assignments_secretary_id ON assignments(secretary_id) WHERE deleted_at IS NULL");
ddlStatements.add("CREATE INDEX idx_assignments_customer_id ON assignments(customer_id) WHERE deleted_at IS NULL");
ddlStatements.add("CREATE INDEX idx_assignments_target_year_month ON assignments(target_year_month) WHERE deleted_at IS NULL");
ddlStatements.add("CREATE INDEX idx_tasks_approved_at ON tasks(approved_at) WHERE deleted_at IS NULL");
```

---

### 22. 大きな IN 句のリスク - 潜在的な N+1 またはメモリの問題
**ファイル:** `src\main\java\dao\TaskDAO.java`
**重要度:** 中
**カテゴリ:** パフォーマンス

クエリが無制限のリストで `IN` 句を使用する場合：
```java
// 潜在的なリスク - リストが大きい場合
"WHERE task_id IN (?, ?, ?, ...)" // 1000+ 項目？
```

**リスク:**
- クエリパフォーマンスの劣化
- 接続タイムアウト
- メモリ枯渇

**推奨修正:**
```java
// 大きなデータセットにはページネーションを使用
// またはバッチ処理:
int BATCH_SIZE = 100;
for (int i = 0; i < taskIds.size(); i += BATCH_SIZE) {
    List<UUID> batch = taskIds.subList(i,
        Math.min(i + BATCH_SIZE, taskIds.size()));
    // バッチを処理
}
```

---

## リファクタリングの機会

### 23. データベース設定をプロパティファイルに抽出
**現状:** ソース内にハードコード
**推奨:** 外部設定を使用

```properties
# src/main/resources/application.properties
db.url=jdbc:postgresql://localhost:5433/hidariude
db.user=postgres
db.password=${DB_PASSWORD}
db.timeout=30000
```

---

### 24. ロギングフレームワークの実装
**現状:** `e.printStackTrace()` と `System.out.println()`
**推奨:** Logback または Log4j2 で SLF4J を使用

```java
// 依存関係を追加
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.9</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.11</version>
</dependency>
```

---

### 25. 例外階層の実装
**現状:** ジェネリック `RuntimeException` のキャッチ
**推奨:** カスタム例外を作成

```java
public abstract class ApplicationException extends RuntimeException {
    private final String errorCode;
    public ApplicationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

public class AuthenticationException extends ApplicationException {
    public AuthenticationException(String message) {
        super("AUTH_FAILED", message);
    }
}
```

---

### 26. 依存性注入の実装
**現状:** どこでも新しいインスタンスを作成
**推奨:** Spring Framework または CDI を使用

```java
// 削減:
new TransactionManager()
new AssignmentDAO()
new TaskDAO()

// 使用:
@Autowired
private TransactionManager transactionManager;

@Autowired
private AssignmentDAO assignmentDAO;
```

---

### 27. バリデーションを別のレイヤーに抽出
**現状:** ビジネスロジックと混在したバリデーション
**推奨:** バリデーションサービスを作成

```java
public interface TaskValidator {
    ValidationResult validateTaskRegistration(TaskRegistrationRequest request);
}

public class TaskValidationService implements TaskValidator {
    public ValidationResult validateTaskRegistration(TaskRegistrationRequest request) {
        // 集中化されたバリデーションロジック
    }
}
```

---

## サマリーテーブル

| ID | カテゴリ | 重要度 | 問題 | ファイル | 行 |
|---|----------|----------|-------|------|---------|
| 1 | セキュリティ - XSS | クリティカル | setAttribute での非エスケープユーザー入力 | TaskService.java | 431 |
| 2 | セキュリティ - 認証情報 | クリティカル | ハードコードされた DB 認証情報 | TransactionManager.java, DatabaseInitListener.java | 8-11, 23-27 |
| 3 | リソース管理 | クリティカル | close() での接続リーク | TransactionManager.java | 47-63 |
| 4 | ランタイムエラー | クリティカル | nextPath での null チェック欠如 | FrontController.java | 583 |
| 5 | セキュリティ - セッション | 高 | セッション固定脆弱性 | CommonService.java | 138-155, 185-205, 525-560 |
| 6 | エラー処理 | 高 | 不適切な例外処理 | 複数ファイル | セクション参照 |
| 7 | セキュリティ - 認証/認可 | 高 | 一貫性のない認証チェック | FrontController.java | 537-547 |
| 8 | セキュリティ - SQL インジェクション | 高 | 潜在的な SQL インジェクションリスク | TaskDAO.java | 53-80 |
| 9 | セキュリティ - オープンリダイレクト | 中 | 未検証のリダイレクト | FrontController.java | 586 |
| 10 | セキュリティ - セッション設定 | 中 | 弱いセッション設定 | web.xml | 22 |
| 11 | リソース管理 | 中 | DB 接続の問題 | DatabaseInitListener.java | 74-87 |
| 12 | パフォーマンス | 中 | N+1 クエリ問題 | CommonService.java | 452-470 |
| 13 | コード品質 - DRY | 中 | コードの重複 | CommonService.java | 126-163, 174-214, 514-568 |
| 14 | コード品質 - サイズ | 中 | 長いメソッド | TaskService.java | 複数 |
| 15 | コード品質 - マジック | 中 | マジックナンバー/文字列 | 複数ファイル | 複数 |
| 16 | コード品質 - ドキュメント | 中 | JavaDoc の欠如 | 複数ファイル | 複数 |
| 17 | コード品質 - Null | 中 | 一貫性のない null 処理 | CommonService.java | 300-308 |
| 18 | コード品質 - クリーンアップ | 低 | コメントアウトされたコード | AssignmentService.java | 複数 |
| 19 | セキュリティ - CSRF | 中 | CSRF トークンなし | すべての JSP | 複数 |
| 20 | セキュリティ - 監査 | 中 | パスワード変更監査なし | CommonService.java | 386-410 |
| 21 | パフォーマンス | 中 | DB インデックスの欠如 | DatabaseInitListener.java | すべての DDL |
| 22 | パフォーマンス | 中 | 大きな IN 句のリスク | TaskDAO.java | 複数 |

---

## 推奨事項の優先順位

### 即座に対応（次回リリース）
1. **XSS 脆弱性の修正** (問題 #1)
2. **ハードコードされた認証情報の削除** (問題 #2)
3. **接続リークの修正** (問題 #3)
4. **null チェックの追加** (問題 #4)
5. **セッション再生成の実装** (問題 #5)
6. **適切なロギングの実装** (問題 #6)

### 短期（1-2ヶ月）
1. CSRF トークン保護の実装
2. パスワード変更監査ログの追加
3. 適切な例外階層の実装
4. 設定をプロパティファイルに抽出
5. 重複ログインコードのリファクタリング
6. データベースインデックスの追加

### 長期（3-6ヶ月）
1. 依存性注入フレームワークの実装
2. バリデーションレイヤーの抽出
3. 監査証跡システムの実装
4. API 認証（OAuth2/JWT）の追加
5. リクエスト/レスポンスログの実装
6. レート制限の追加

---

## テスト推奨事項

1. **セキュリティテスト:**
   - OWASP ZAP スキャン
   - SQL インジェクションテスト
   - XSS ペイロードテスト
   - セッションハイジャックテスト

2. **パフォーマンステスト:**
   - JMeter での負荷テスト
   - クエリパフォーマンスプロファイリング
   - 接続プールの監視

3. **ユニットテスト:**
   - テストカバレッジの追加（現在は0%と思われる）
   - データベースレイヤーのモック
   - 認証フローのテスト

4. **統合テスト:**
   - エンドツーエンドのユーザーフロー
   - マルチロールシナリオテスト
   - データベーストランザクションテスト

---

**レポート終了**

**対象:** Hidariude 秘書管理システム
**分析日:** 2025-10-28
**分析システム:** Claude Code Analysis System