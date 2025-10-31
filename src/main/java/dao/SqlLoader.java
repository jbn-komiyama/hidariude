package dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SQLファイルを読み込むユーティリティクラス
 * クラスパス上のSQLファイルを読み込み、キャッシュして再利用します
 */
public final class SqlLoader {
    /** SQLファイルのキャッシュ */
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    /**
     * コンストラクタ（インスタンス化不可）
     */
    private SqlLoader() {}

    /**
     * 指定されたパスのSQLファイルを読み込みます
     * 一度読み込んだファイルはキャッシュされ、以降はキャッシュから返されます
     *
     * @param path クラスパス上のSQLファイルのパス
     * @return SQLファイルの内容
     * @throws IllegalStateException SQLファイルが見つからない場合
     * @throws UncheckedIOException ファイル読み込みに失敗した場合
     */
    public static String load(String path) {
        return CACHE.computeIfAbsent(path, p -> {
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(p)) {
                if (in == null) {
                    throw new IllegalStateException("SQL file not found on classpath: " + p);
                }
                byte[] bytes = in.readAllBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
