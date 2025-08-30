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
 * <p>本クラスは 1 つの {@link Connection} 上で SQL を実行する薄い永続化層です。
 * トランザクション境界（begin/commit/rollback）は呼び出し側で管理してください。</p>
 */
public class SystemAdminDAO extends BaseDAO {

    // ========================
    // SQL 定義
    // ========================
    private static final String SQL_SELECT_BASE =
        "SELECT id, mail, password, name, name_ruby, "
      + "       created_at, updated_at, deleted_at, last_login_at "
      + "  FROM system_admins ";

    private static final String SQL_SELECT_BY_MAIL =
        SQL_SELECT_BASE + " WHERE deleted_at IS NULL AND mail = ?";

    private static final String SQL_SELECT_BY_ID =
        SQL_SELECT_BASE + " WHERE deleted_at IS NULL AND id = ?";

    private static final String SQL_INSERT =
        "INSERT INTO system_admins ("
      + " id, mail, password, name, name_ruby, created_at, updated_at"
      + ") VALUES ("
      + " gen_random_uuid(), ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP"
      + ")";

    private static final String SQL_UPDATE =
        "UPDATE system_admins "
      + "   SET mail = ?, password = ?, name = ?, name_ruby = ?, "
      + "       updated_at = CURRENT_TIMESTAMP "
      + " WHERE id = ? AND deleted_at IS NULL";

    private static final String SQL_DELETE_LOGICAL =
        "UPDATE system_admins SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL";
    
    private static final String SQL_SELECT_ALL =
        SQL_SELECT_BASE + " WHERE deleted_at IS NULL ORDER BY created_at";

    private static final String SQL_COUNT_BY_MAIL =
        "SELECT COUNT(*) FROM system_admins WHERE deleted_at IS NULL AND mail = ?";

    private static final String SQL_COUNT_BY_MAIL_EXCEPT_ID =
        "SELECT COUNT(*) FROM system_admins WHERE deleted_at IS NULL AND mail = ? AND id <> ?";

    public SystemAdminDAO(Connection conn) {
        super(conn);
    }
    
    // 一覧
    public List<SystemAdminDTO> selectAll() {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            List<SystemAdminDTO> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new DAOException("E:A01 system_admins 全件取得に失敗しました。", e);
        }
    }

    /**
     * メールアドレスでシステム管理者を1件取得します（論理削除は除外）。
     *
     * @param mail メールアドレス
     * @return 該当があれば {@link SystemAdminDTO}、なければ空のDTO
     * @throws DAOException DBアクセスに失敗した場合
     */
    public SystemAdminDTO selectByMail(String mail) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_MAIL)) {
            ps.setString(1, mail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return new SystemAdminDTO();
            }
        } catch (SQLException e) {
            throw new DAOException("E:A11 system_admins メール検索に失敗しました。", e);
        }
    }

    /**
     * UUID（主キー）でシステム管理者を1件取得します（論理削除は除外）。
     *
     * @param id 管理者ID
     * @return 該当があれば {@link SystemAdminDTO}、なければ空のDTO
     * @throws DAOException DBアクセスに失敗した場合
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

    /**
     * システム管理者を新規登録します。
     * <p>ID は DB 側で {@code gen_random_uuid()} を採番します。</p>
     *
     * @param dto 登録する管理者（mail / password / name / name_ruby を使用）
     * @return 影響行数
     * @throws DAOException DBアクセスに失敗した場合
     */
    public int insert(SystemAdminDTO dto) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
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
     *
     * @param dto 更新対象（id 必須）
     * @return 影響行数
     * @throws DAOException DBアクセスに失敗した場合
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
     * @param id 管理者ID
     * @throws DAOException DBアクセスに失敗した場合
     */
    public void delete(UUID id) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_LOGICAL)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:A23 system_admins 論理DELETE に失敗しました。", e);
        }
    }
    
    // メール重複チェック
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

    // 自ID除外のメール重複チェック
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

    // ========================
    // ResultSet -> DTO 変換
    // ========================

    /** SELECT句（SQL_SELECT_BASE）に合わせて1行をDTOへ詰め替えます。 */
    private SystemAdminDTO mapRow(ResultSet rs) {
        try {
            SystemAdminDTO dto = new SystemAdminDTO();
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