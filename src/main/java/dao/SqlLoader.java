package dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SqlLoader {
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private SqlLoader() {}

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
