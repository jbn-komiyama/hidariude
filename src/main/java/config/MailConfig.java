package config;

import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * メール送信の設定を管理するクラス。
 * SendGrid API を使用したメール送信に必要な設定値を提供します。
 * 設定値は.envファイルまたは環境変数から取得され、環境に応じて適切な値が使用されます。
 */
public class MailConfig {

    private static final Dotenv dotenv;
    
    static {
        /** .envファイルを読み込む（存在しない場合はスキップ） */
        var builder = Dotenv.configure().ignoreIfMissing();
        
        /** .envファイルのパスを明示的に指定（複数の場所を順番に試す） */
        String envFileDir = findEnvFileDirectory();
        if (envFileDir != null) {
            builder = builder.directory(envFileDir);
        }
        
        dotenv = builder.load();
    }
    
    /**
     * .envファイルがあるディレクトリを探す（複数の場所を順番に確認）
     * 
     * @return .envファイルが見つかったディレクトリのパス、見つからない場合はnull
     */
    private static String findEnvFileDirectory() {
        /** 1. システムプロパティまたは環境変数から指定されたディレクトリを試す */
        String customDir = System.getProperty("hidariude.env.dir");
        if (customDir == null || customDir.isEmpty()) {
            customDir = System.getenv("HIDARIUDE_ENV_DIR");
        }
        if (customDir == null || customDir.isEmpty()) {
            /** ファイルパスが指定されている場合も対応 */
            String customPath = System.getProperty("hidariude.env.path");
            if (customPath == null || customPath.isEmpty()) {
                customPath = System.getenv("HIDARIUDE_ENV_PATH");
            }
            if (customPath != null && !customPath.isEmpty()) {
                Path path = Paths.get(customPath);
                if (Files.exists(path) && Files.isRegularFile(path)) {
                    return path.getParent().toString();
                }
                /** ディレクトリパスとして解釈 */
                if (Files.exists(path) && Files.isDirectory(path)) {
                    customDir = customPath;
                }
            }
        }
        if (customDir != null && !customDir.isEmpty()) {
            Path dirPath = Paths.get(customDir);
            Path envFile = dirPath.resolve(".env");
            if (Files.exists(envFile) && Files.isRegularFile(envFile)) {
                return dirPath.toString();
            }
        }
        
        /** 2. 本番環境のデフォルトパス（/opt/hidariude/.env）を試す */
        Path productionPath = Paths.get("/opt/hidariude/.env");
        if (Files.exists(productionPath) && Files.isRegularFile(productionPath)) {
            return productionPath.getParent().toString();
        }
        
        /** 3. カレントディレクトリの.envを試す（開発環境用） */
        String currentDir = System.getProperty("user.dir");
        Path currentDirPath = Paths.get(currentDir, ".env");
        if (Files.exists(currentDirPath) && Files.isRegularFile(currentDirPath)) {
            return currentDir;
        }
        
        /** 4. クラスパスから見つかった場合は、その親ディレクトリを探す（開発環境でMavenプロジェクトルートから実行される場合） */
        try {
            String classPath = MailConfig.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath();
            
            /** WARファイル内の場合は、Tomcatの作業ディレクトリから探す */
            if (classPath.contains(".war") || classPath.contains("WEB-INF")) {
                /** Tomcatから実行される場合、/opt/hidariude/.envを再確認 */
                Path warProductionPath = Paths.get("/opt/hidariude/.env");
                if (Files.exists(warProductionPath) && Files.isRegularFile(warProductionPath)) {
                    return warProductionPath.getParent().toString();
                }
            }
        } catch (Exception e) {
            /** エラーが発生しても続行 */
        }
        
        /** .envファイルが見つからない場合はnullを返す（ignoreIfMissingで処理される） */
        return null;
    }

    /**
     * 環境変数または.envファイルから値を取得する
     * 
     * @param key 環境変数のキー
     * @return 環境変数の値
     */
    private static String getEnvValue(String key) {
        /** 1. システム環境変数から取得を試みる */
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        /** 2. .envファイルから取得を試みる */
        if (dotenv != null) {
            value = dotenv.get(key);
        }
        
        return value;
    }

    /**
     * SendGrid API キー（環境変数 SENDGRID_API_KEY または.envファイルから取得）
     */
    public static String getSendGridApiKey() {
        String apiKey = getEnvValue("SENDGRID_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException(
                "SENDGRID_API_KEY 環境変数が設定されていません。" +
                ".envファイルまたは環境変数に SendGrid API キーを設定してください。"
            );
        }
        return apiKey;
    }

    /**
     * メール送信元アドレス
     */
    public static String getFromEmail() {
        return "info@jibun-note.co.jp";
    }

    /**
     * メール送信元名
     */
    public static String getFromName() {
        return "Hidariude";
    }

    /**
     * アプリケーションのベースURL（環境変数 APP_BASE_URL または.envファイルから取得）
     * 環境変数が設定されていない場合は、開発環境用のデフォルトURL
     * （http://localhost:8080/hidariude）を使用します。
     * 本番環境では以下のように.envファイルまたは環境変数を設定してください：
     * .envファイルの場合: APP_BASE_URL=http://ik1-224-81260.vs.sakura.ne.jp:8080/hidariude
     * または環境変数の場合: export APP_BASE_URL=http://ik1-224-81260.vs.sakura.ne.jp:8080/hidariude
     * 
     * @return ベースURL
     */
    public static String getBaseUrl() {
        String baseUrl = getEnvValue("APP_BASE_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            /** デフォルト：開発環境用URL */
            return "http://localhost:8080/hidariude";
        }
        return baseUrl;
    }

    /**
     * パスワードリセット用のURLを生成します。
     * 
     * @param token    パスワードリセット用トークン
     * @param userType ユーザータイプ（'admin', 'secretary', 'customer'）
     * @return パスワードリセット用の完全なURL
     */
    public static String getPasswordResetUrl(String token, String userType) {
        String path = switch (userType) {
            case "admin" -> "/admin/password_reset/form";
            case "secretary" -> "/secretary/password_reset/form";
            case "customer" -> "/customer/password_reset/form";
            default -> "/admin/password_reset/form";
        };
        return getBaseUrl() + path + "?token=" + token;
    }
}

