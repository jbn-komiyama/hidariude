package util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * パスワードのハッシュ化と検証を行うユーティリティクラス。
 * 
 * BCryptアルゴリズムを使用してパスワードを安全に保管・検証します。
 * コストファクタはデフォルト（10）を使用しています。
 * 
 */
public class PasswordUtil {

    /**
     * 平文パスワードをBCryptでハッシュ化します。
     * 
     * @param plainPassword 平文パスワード
     * @return BCryptハッシュ値
     * @throws IllegalArgumentException plainPasswordがnullまたは空の場合
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("パスワードは必須です。");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    /**
     * 平文パスワードとハッシュ値を照合します。
     * 
     * @param plainPassword 平文パスワード
     * @param hashedPassword BCryptハッシュ値
     * @return 一致する場合true、それ以外false
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            /** ハッシュ形式が不正な場合（例: 平文が保存されている場合） */
            return false;
        }
    }
}

