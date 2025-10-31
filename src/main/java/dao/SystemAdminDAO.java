package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dto.SystemAdminDTO;

/**
 * {@code system_admins} を扱う DAO。
 * 1コネクション上で SQL を実行する薄い永続化層です。
 * トランザクション境界（begin / commit / rollback）は呼び出し側で管理してください。
 * 実行時例外は {@link DAOException} にラップして上位へ伝播します。
 *
 * クラス構成：
 * 1. フィールド（SQL）
 * 2. フィールド、コンストラクタ
 * 3. メソッド（SELECT／INSERT/UPDATE/DELETE／重複チェック／Mapper）
 */
public class SystemAdminDAO extends BaseDAO {

    /** ========================
     * ① フィールド（SQL 定義）
     * ======================== */

    /** 共通 SELECT 句（列順は {@link #mapRow(ResultSet)} と一致させる） */
    private static final String SQL_SELECT_BASE =
        "SELECT id, mail, password, name, name_ruby, "
      + "       created_at, updated_at, deleted_at, last_login_at "
      + "  FROM system_admins ";

    /** 論理未削除＋mail一致で1件取得 */
    private static final String SQL_SELECT_BY_MAIL =
        SQL_SELECT_BASE + " WHERE deleted_at IS NULL AND mail = ?";

    /** 論理未削除＋id一致で1件取得 */
    private static final String SQL_SELECT_BY_ID =
        SQL_SELECT_BASE + " WHERE deleted_at IS NULL AND id = ?";

    /** 新規登録（id は DB 側で gen_random_uuid() 採番） */
    private static final String SQL_INSERT =
        "INSERT INTO system_admins ("
      + " id, mail, password, name, name_ruby, created_at, updated_at"
      + ") VALUES ("
      + " gen_random_uuid(), ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP"
      + ")";

    /** 基本更新（論理未削除のみ対象） */
    private static final String SQL_UPDATE =
        "UPDATE system_admins "
      + "   SET mail = ?, password = ?, name = ?, name_ruby = ?, "
      + "       updated_at = CURRENT_TIMESTAMP "
      + " WHERE id = ? AND deleted_at IS NULL";

    /** 論理削除（deleted_at に現在時刻をセット） */
    private static final String SQL_DELETE_LOGICAL =
        "UPDATE system_admins SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL";

    /** 全件（論理未削除）取得：作成日時順 */
    private static final String SQL_SELECT_ALL =
        SQL_SELECT_BASE + " WHERE deleted_at IS NULL ORDER BY created_at";

    /** mail の重複（論理未削除のみ） */
    private static final String SQL_COUNT_BY_MAIL =
        "SELECT COUNT(*) FROM system_admins WHERE deleted_at IS NULL AND mail = ?";

    /** mail の重複（自ID除外、論理未削除のみ） */
    private static final String SQL_COUNT_BY_MAIL_EXCEPT_ID =
        "SELECT COUNT(*) FROM system_admins WHERE deleted_at IS NULL AND mail = ? AND id <> ?";

    /** 最終ログイン時刻の更新 */
    private static final String SQL_UPDATE_LAST_LOGIN_AT =
        "UPDATE system_admins SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?";

    /** パスワードのみ更新（パスワードリセット用） */
    private static final String SQL_UPDATE_PASSWORD =
        "UPDATE system_admins " +
        "   SET password = ?, updated_at = CURRENT_TIMESTAMP " +
        " WHERE id = ? AND deleted_at IS NULL";

    /** ========================
     * ② フィールド、コンストラクタ
     * ======================== */

    /**
     * コンストラクタ。
     *
     * @param conn 呼び出し側が管理する JDBC コネクション
     */
    public SystemAdminDAO(Connection conn) {
        super(conn);
    }

    /** ========================
     * ③ メソッド
     * =========================
     * SELECT
     * ========================= */

    /**
     * システム管理者の一覧を取得します（論理未削除のみ、作成日時昇順）。
     *
     * @return {@link SystemAdminDTO} のリスト（0件可）
     * @throws DAOException 取得に失敗した場合
     */
    public List<SystemAdminDTO> selectAll() {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            List<SystemAdminDTO> list = new ArrayList<>();
            /** 1行ずつ DTO へマップ */
            while (rs.next()) list.add(mapRow(rs));
            return list;

        } catch (SQLException e) {
            throw new DAOException("E:A01 system_admins 全件取得に失敗しました。", e);
        }
    }

    /**
     * メールアドレスをキーにシステム管理者を1件取得します（論理削除は除外）。
     * 該当なしの場合は空の DTO を返します。呼び出し側で {@code id == null} などで判定してください。
     *
     * @param mail メールアドレス
     * @return 該当 {@link SystemAdminDTO}／未存在時は空 DTO
     * @throws DAOException 取得に失敗した場合
     */
    public SystemAdminDTO selectByMail(String mail) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_MAIL)) {
            ps.setString(1, mail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return new SystemAdminDTO(); /** 互換のため空 DTO を返す */
            }
        } catch (SQLException e) {
            throw new DAOException("E:A11 system_admins メール検索に失敗しました。", e);
        }
    }

    /**
     * 主キー（UUID）をキーにシステム管理者を1件取得します（論理削除は除外）。
     * 該当なしの場合は空の DTO を返します。
     *
     * @param id 管理者ID（UUID）
     * @return 該当 {@link SystemAdminDTO}／未存在時は空 DTO
     * @throws DAOException 取得に失敗した場合
     */
    public SystemAdminDTO selectById(UUID id) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_ID)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return new SystemAdminDTO();
            }
        } catch (SQLException e) {
            throw new DAOException("E:A12 system_admins ID検索に失敗しました。", e);
        }
    }

    /** =========================
     * INSERT / UPDATE / DELETE
     * ========================= */

    /**
     * システム管理者を新規登録します。
     * - ID は DB 側で {@code gen_random_uuid()} により採番されます。
     * - {@code created_at / updated_at} はサーバー時刻で自動設定します。
     *
     * @param dto 登録する管理者（使用フィールド：mail, password, name, name_ruby）
     * @return 影響行数（通常 1）
     * @throws DAOException INSERT に失敗した場合
     */
    public int insert(SystemAdminDTO dto) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            /** 順にバインド（SQL の「?」と順序を一致させる） */
            ps.setString(1, dto.getMail());
            ps.setString(2, dto.getPassword());
            ps.setString(3, dto.getName());
            ps.setString(4, dto.getNameRuby());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:A21 system_admins INSERT に失敗しました。", e);
        }
    }

    /**
     * システム管理者の基本情報を更新します（論理削除済みは対象外）。
     * {@code updated_at} はサーバー時刻で更新されます。
     *
     * @param dto 更新対象（必須：id）／使用フィールド：mail, password, name, name_ruby
     * @return 影響行数（通常 1。対象なしの場合は 0）
     * @throws DAOException UPDATE に失敗した場合
     */
    public int update(SystemAdminDTO dto) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, dto.getMail());
            ps.setString(2, dto.getPassword());
            ps.setString(3, dto.getName());
            ps.setString(4, dto.getNameRuby());
            ps.setObject(5, dto.getId());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:A22 system_admins UPDATE に失敗しました。", e);
        }
    }

    /**
     * システム管理者を論理削除します（{@code deleted_at = CURRENT_TIMESTAMP}）。
     *
     * @param id 管理者ID（UUID）
     * @throws DAOException 論理削除に失敗した場合
     */
    public void delete(UUID id) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_LOGICAL)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:A23 system_admins 論理DELETE に失敗しました。", e);
        }
    }

    /** =========================
     * 重複チェック
     * ========================= */

    /**
     * メールアドレスの存在有無をチェックします（論理未削除のみ対象）。
     *
     * @param mail メールアドレス
     * @return {@code true}: 既に存在 ／ {@code false}: 未使用
     * @throws DAOException 照会に失敗した場合
     */
    public boolean mailExists(String mail) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_BY_MAIL)) {
            ps.setString(1, mail);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) >= 1;
            }
        } catch (SQLException e) {
            throw new DAOException("E:A41 system_admins.mail 重複チェックに失敗しました。", e);
        }
    }

    /**
     * メールアドレスの存在有無をチェックします（自IDを除外し、論理未削除のみ対象）。
     * 編集時の一意制約チェックに利用します。
     *
     * @param mail      メールアドレス
     * @param excludeId 除外する管理者ID（自身のID）
     * @return {@code true}:（自分以外で）既に存在 ／ {@code false}: 未使用
     * @throws DAOException 照会に失敗した場合
     */
    public boolean mailExistsExceptId(String mail, UUID excludeId) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_BY_MAIL_EXCEPT_ID)) {
            ps.setString(1, mail);
            ps.setObject(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) >= 1;
            }
        } catch (SQLException e) {
            throw new DAOException("E:A42 system_admins.mail(除外) 重複チェックに失敗しました。", e);
        }
    }

    /**
     * 最終ログイン時刻を現在時刻で更新します。
     *
     * @param id 管理者ID
     * @throws DAOException 更新に失敗した場合
     */
    public void updateLastLoginAt(UUID id) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_LAST_LOGIN_AT)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:A43 system_admins.last_login_at 更新に失敗しました。", e);
        }
    }

    /**
     * パスワードのみを更新します（パスワードリセット用）。
     * {@code updated_at} はサーバー時刻で更新されます。
     * 論理削除済みのレコードは対象外です。
     *
     * @param id             管理者ID（UUID）
     * @param hashedPassword ハッシュ化されたパスワード
     * @return 影響行数（通常 1。対象なしの場合は 0）
     * @throws DAOException UPDATE に失敗した場合
     */
    public int updatePassword(UUID id, String hashedPassword) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PASSWORD)) {
            ps.setString(1, hashedPassword);
            ps.setObject(2, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:A25 system_admins.password の更新に失敗しました。", e);
        }
    }

    /** =========================
     * ResultSet -> DTO 変換
     * ========================= */

    /**
     * 1レコードを {@link SystemAdminDTO} へ詰め替えます。
     * 列順は {@link #SQL_SELECT_BASE} と一致させています。
     * 例外は {@link DAOException} にラップして上位へ伝播します。
     *
     * @param rs SELECT 実行結果の現在行
     * @return マッピング済み DTO
     * @throws DAOException JDBC アクセスに失敗した場合
     */
    private SystemAdminDTO mapRow(ResultSet rs) {
        try {
            SystemAdminDTO dto = new SystemAdminDTO();
            /** 1列目から順にマッピング（パフォーマンス重視で getObject(列番号) を使用） */
            dto.setId(rs.getObject(1, UUID.class));
            dto.setMail(rs.getString(2));
            dto.setPassword(rs.getString(3));
            dto.setName(rs.getString(4));
            dto.setNameRuby(rs.getString(5));
            dto.setCreatedAt(rs.getTimestamp(6));
            dto.setUpdatedAt(rs.getTimestamp(7));
            dto.setDeletedAt(rs.getTimestamp(8));
            dto.setLastLoginAt(rs.getTimestamp(9));
            return dto;
        } catch (SQLException e) {
            throw new DAOException("E:A51 ResultSet→SystemAdminDTO 変換に失敗しました。", e);
        }
    }
}
