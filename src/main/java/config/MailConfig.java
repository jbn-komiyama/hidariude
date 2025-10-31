package config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * メール送信の設定を管理するクラス。
 * <p>
 * SendGrid API を使用したメール送信に必要な設定値を提供します。
 * 設定値は.envファイルまたは環境変数から取得され、環境に応じて適切な値が使用されます。
 * </p>
 */
public class MailConfig {

    private static final Dotenv dotenv;
    
    static {
        // .envファイルを読み込む（存在しない場合はスキップ）
        dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();
    }

    /**
     * 環境変数または.envファイルから値を取得する
     * 
     * @param key 環境変数のキー
     * @return 環境変数の値
     */
    private static String getEnvValue(String key) {
        // 1. システム環境変数から取得を試みる
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        // 2. .envファイルから取得を試みる
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
     * <p>
     * 環境変数が設定されていない場合は、開発環境用のデフォルトURL
     * （http://localhost:8080/hidariude）を使用します。
     * </p>
     * <p>
     * 本番環境では以下のように.envファイルまたは環境変数を設定してください：
     * <pre>
     * .envファイルの場合:
     * APP_BASE_URL=http://ik1-224-81260.vs.sakura.ne.jp:8080/hidariude
     * 
     * または環境変数の場合:
     * export APP_BASE_URL=http://ik1-224-81260.vs.sakura.ne.jp:8080/hidariude
     * </pre>
     * </p>
     */
    public static String getBaseUrl() {
        String baseUrl = getEnvValue("APP_BASE_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            // デフォルト：開発環境用URL
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

