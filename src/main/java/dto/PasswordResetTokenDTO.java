package dto;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * パスワードリセットトークンのDTO。
 * {@code password_reset_tokens} テーブルと1対1で対応します。
 * パスワードリセット機能で使用するトークン情報を保持します。
 * 全ロール（admin / secretary / customer）に対応しています。
 */
public class PasswordResetTokenDTO {

    /** トークンID */
    private UUID id;

    /** ユーザータイプ（'admin', 'secretary', 'customer'） */
    private String userType;

    /** ユーザーID */
    private UUID userId;

    /** リセット用トークン（UUID形式） */
    private String token;

    /** 有効期限 */
    private Timestamp expiresAt;

    /** 使用日時（NULL = 未使用） */
    private Timestamp usedAt;

    /** 作成日時 */
    private Timestamp createdAt;

    /** =========================================================
     * コンストラクタ
     * ========================================================= */

    /**
     * デフォルトコンストラクタ。
     */
    public PasswordResetTokenDTO() {
    }

    /**
     * 全フィールド指定のコンストラクタ。
     *
     * @param id         トークンID
     * @param userType   ユーザータイプ
     * @param userId     ユーザーID
     * @param token      リセット用トークン
     * @param expiresAt  有効期限
     * @param usedAt     使用日時
     * @param createdAt  作成日時
     */
    public PasswordResetTokenDTO(
        UUID id,
        String userType,
        UUID userId,
        String token,
        Timestamp expiresAt,
        Timestamp usedAt,
        Timestamp createdAt
    ) {
        this.id = id;
        this.userType = userType;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdAt = createdAt;
    }

    /** =========================================================
     * Getter / Setter
     * ========================================================= */

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Timestamp getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Timestamp usedAt) {
        this.usedAt = usedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    /** =========================================================
     * ヘルパーメソッド
     * ========================================================= */

    /**
     * トークンが未使用かどうかを判定します。
     *
     * @return 未使用の場合true、使用済みの場合false
     */
    public boolean isUnused() {
        return usedAt == null;
    }

    /**
     * トークンが有効期限内かどうかを判定します。
     *
     * @return 有効期限内の場合true、期限切れの場合false
     */
    public boolean isValid() {
        if (expiresAt == null) {
            return false;
        }
        return expiresAt.after(new Timestamp(System.currentTimeMillis()));
    }

    @Override
    public String toString() {
        return "PasswordResetTokenDTO{" +
               "id=" + id +
               ", userType='" + userType + '\'' +
               ", userId=" + userId +
               ", token='" + token + '\'' +
               ", expiresAt=" + expiresAt +
               ", usedAt=" + usedAt +
               ", createdAt=" + createdAt +
               '}';
    }
}

