package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import dto.PasswordResetTokenDTO;

/**
 * {@code password_reset_tokens} テーブルを扱う DAO。
 * <p>
 * パスワードリセット用トークンの保存、取得、更新、削除を行います。
 * トランザクション境界（begin / commit / rollback）は呼び出し側で管理してください。
 * 実行時例外は {@link DAOException} にラップして上位へ伝播します。
 * </p>
 */
public class PasswordResetTokenDAO extends BaseDAO {

    // ========================
    // ① フィールド（SQL 定義）
    // ========================

    /** 共通 SELECT 句 */
    private static final String SQL_SELECT_BASE =
        "SELECT id, user_type, user_id, token, expires_at, used_at, created_at " +
        "  FROM password_reset_tokens ";

    /** トークンで1件取得 */
    private static final String SQL_SELECT_BY_TOKEN =
        SQL_SELECT_BASE + " WHERE token = ?";

    /** ユーザーの有効なトークンを取得（未使用かつ有効期限内） */
    private static final String SQL_SELECT_VALID_BY_USER =
        SQL_SELECT_BASE + 
        " WHERE user_type = ? AND user_id = ? " +
        "   AND used_at IS NULL " +
        "   AND expires_at > CURRENT_TIMESTAMP " +
        " ORDER BY created_at DESC " +
        " LIMIT 1";

    /** 新規登録 */
    private static final String SQL_INSERT =
        "INSERT INTO password_reset_tokens " +
        "(user_type, user_id, token, expires_at) " +
        "VALUES (?, ?, ?, ?)";

    /** トークンを使用済みにマーク */
    private static final String SQL_MARK_AS_USED =
        "UPDATE password_reset_tokens " +
        "   SET used_at = CURRENT_TIMESTAMP " +
        " WHERE token = ?";

    /** 期限切れトークンの削除 */
    private static final String SQL_DELETE_EXPIRED =
        "DELETE FROM password_reset_tokens " +
        " WHERE expires_at < CURRENT_TIMESTAMP";

    /** 指定ユーザーの全トークンを削除 */
    private static final String SQL_DELETE_BY_USER =
        "DELETE FROM password_reset_tokens " +
        " WHERE user_type = ? AND user_id = ?";

    // ========================
    // ② コンストラクタ
    // ========================

    /**
     * コンストラクタ。
     *
     * @param conn 呼び出し側が管理する JDBC コネクション
     */
    public PasswordResetTokenDAO(Connection conn) {
        super(conn);
    }

    // ========================
    // ③ メソッド
    // ========================

    /**
     * トークンをキーにパスワードリセットトークンを1件取得します。
     * <p>該当なしの場合は空の DTO を返します。呼び出し側で {@code id == null} などで判定してください。</p>
     *
     * @param token トークン文字列
     * @return 該当 {@link PasswordResetTokenDTO}／未存在時は空 DTO
     * @throws DAOException 取得に失敗した場合
     */
    public PasswordResetTokenDTO selectByToken(String token) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_TOKEN)) {
            ps.setString(1, token);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            // 該当なし → 空 DTO
            return new PasswordResetTokenDTO();

        } catch (SQLException e) {
            throw new DAOException("E:PR01 パスワードリセットトークンの取得に失敗しました。", e);
        }
    }

    /**
     * 指定ユーザーの有効なトークンを取得します。
     * <p>未使用（used_at IS NULL）かつ有効期限内（expires_at > 現在時刻）のトークンを検索します。</p>
     * <p>複数存在する場合は、最新のもの（created_at DESC）を返します。</p>
     * <p>該当なしの場合は空の DTO を返します。</p>
     *
     * @param userType ユーザータイプ（'admin', 'secretary', 'customer'）
     * @param userId   ユーザーID（UUID）
     * @return 有効なトークン DTO／該当なしの場合は空 DTO
     * @throws DAOException 取得に失敗した場合
     */
    public PasswordResetTokenDTO selectValidByUser(String userType, UUID userId) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_VALID_BY_USER)) {
            ps.setString(1, userType);
            ps.setObject(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            // 該当なし → 空 DTO
            return new PasswordResetTokenDTO();

        } catch (SQLException e) {
            throw new DAOException("E:PR02 有効なパスワードリセットトークンの取得に失敗しました。", e);
        }
    }

    /**
     * パスワードリセットトークンを新規登録します。
     * <ul>
     *   <li>ID は DB 側で {@code gen_random_uuid()} により採番されます。</li>
     *   <li>{@code created_at} はサーバー時刻で自動設定します。</li>
     * </ul>
     *
     * @param dto 登録するトークン（使用フィールド：userType, userId, token, expiresAt）
     * @return 影響行数（通常 1）
     * @throws DAOException INSERT に失敗した場合
     */
    public int insert(PasswordResetTokenDTO dto) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, dto.getUserType());
            ps.setObject(2, dto.getUserId());
            ps.setString(3, dto.getToken());
            ps.setTimestamp(4, dto.getExpiresAt());

            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new DAOException("E:PR21 パスワードリセットトークンの INSERT に失敗しました。", e);
        }
    }

    /**
     * トークンを使用済みにマークします。
     * <p>{@code used_at} に現在時刻を設定します。</p>
     *
     * @param token トークン文字列
     * @return 影響行数（通常 1。対象なしの場合は 0）
     * @throws DAOException UPDATE に失敗した場合
     */
    public int markAsUsed(String token) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_MARK_AS_USED)) {
            ps.setString(1, token);
            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new DAOException("E:PR22 トークンの使用済みマークに失敗しました。", e);
        }
    }

    /**
     * 期限切れトークンを削除します。
     * <p>パスワードリセット申請時に呼び出して期限切れトークンを削除します。</p>
     *
     * @return 削除された行数
     * @throws DAOException DELETE に失敗した場合
     */
    public int deleteExpiredTokens() {
        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_EXPIRED)) {
            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new DAOException("E:PR23 期限切れトークンの削除に失敗しました。", e);
        }
    }

    /**
     * 指定ユーザーの全トークンを削除します。
     * <p>パスワード再設定完了時に、そのユーザーの過去のトークンをクリーンアップするために使用します。</p>
     *
     * @param userType ユーザータイプ（'admin', 'secretary', 'customer'）
     * @param userId   ユーザーID（UUID）
     * @return 削除された行数
     * @throws DAOException DELETE に失敗した場合
     */
    public int deleteByUser(String userType, UUID userId) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_BY_USER)) {
            ps.setString(1, userType);
            ps.setObject(2, userId);
            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new DAOException("E:PR24 ユーザートークンの削除に失敗しました。", e);
        }
    }

    // ========================
    // ④ マッパー
    // ========================

    /**
     * ResultSet の1行を PasswordResetTokenDTO にマッピングします。
     *
     * @param rs ResultSet（現在行が対象）
     * @return マッピングされた DTO
     * @throws SQLException カラム取得失敗時
     */
    private PasswordResetTokenDTO mapRow(ResultSet rs) throws SQLException {
        PasswordResetTokenDTO dto = new PasswordResetTokenDTO();

        // UUID型のカラムは getObject で取得
        dto.setId((UUID) rs.getObject("id"));
        dto.setUserType(rs.getString("user_type"));
        dto.setUserId((UUID) rs.getObject("user_id"));
        dto.setToken(rs.getString("token"));
        dto.setExpiresAt(rs.getTimestamp("expires_at"));
        dto.setUsedAt(rs.getTimestamp("used_at"));
        dto.setCreatedAt(rs.getTimestamp("created_at"));

        return dto;
    }
}

