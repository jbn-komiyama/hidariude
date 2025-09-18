package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dto.CustomerContactDTO;
import dto.CustomerDTO;

/**
 * 顧客担当者（customer_contacts）用 DAO。
 * <ul>
 *   <li>mail はグローバル UNIQUE（NOT NULL）</li>
 *   <li>password は NOT NULL（新規登録必須）</li>
 *   <li>is_primary で主担当管理（1顧客につき1名に正規化）</li>
 * </ul>
 */
public class CustomerContactDAO extends BaseDAO {

    // ========================
    // SQL 定義
    // ========================

    private static final String BASE_SELECT =
        "SELECT cc.id, cc.customer_id, cc.mail, cc.password, cc.name, cc.name_ruby, " +
        "       cc.phone, cc.department, cc.is_primary, cc.created_at, cc.updated_at, cc.deleted_at, cc.last_login_at " +
        "  FROM customer_contacts cc ";

    /** 顧客別一覧 */
    private static final String SQL_SELECT_BY_CUSTOMER =
        BASE_SELECT + " WHERE cc.deleted_at IS NULL AND cc.customer_id = ? ORDER BY cc.name";

    /** 主キー取得 */
    private static final String SQL_SELECT_BY_ID =
        BASE_SELECT + " WHERE cc.deleted_at IS NULL AND cc.id = ?";

    /** INSERT（RETURNING id） */
    private static final String SQL_INSERT_RETURNING_ID =
        "INSERT INTO customer_contacts (" +
        " id, mail, password, customer_id, name, name_ruby, phone, department, is_primary, created_at, updated_at" +
        ") VALUES (" +
        " gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP" +
        ") RETURNING id";

    /** 基本更新（password は対象外） */
    private static final String SQL_UPDATE =
        "UPDATE customer_contacts " +
        "   SET mail = ?, name = ?, name_ruby = ?, phone = ?, department = ?, is_primary = ?, " +
        "       updated_at = CURRENT_TIMESTAMP " +
        " WHERE id = ?";

    /** 論理削除 */
    private static final String SQL_DELETE_LOGICAL =
        "UPDATE customer_contacts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?";

    /** mail グローバル重複チェック */
    private static final String SQL_COUNT_MAIL =
        "SELECT COUNT(*) FROM customer_contacts WHERE deleted_at IS NULL AND mail = ?";

    /** mail グローバル重複チェック（自ID除外） */
    private static final String SQL_COUNT_MAIL_EXCEPT_ID =
        "SELECT COUNT(*) FROM customer_contacts WHERE deleted_at IS NULL AND mail = ? AND id <> ?";

    /** 主担当一括 OFF（顧客単位） */
    private static final String SQL_CLEAR_PRIMARY_FOR_CUSTOMER =
        "UPDATE customer_contacts SET is_primary = FALSE, updated_at = CURRENT_TIMESTAMP WHERE customer_id = ?";

    /** 対象を主担当にセット（true/false） */
    private static final String SQL_SET_PRIMARY_BY_ID =
        "UPDATE customer_contacts SET is_primary = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

    public CustomerContactDAO(Connection conn) { super(conn); }

    // ========================
    // SELECT
    // ========================

    public List<CustomerContactDTO> selectByCustomerId(UUID customerId) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_CUSTOMER)) {
            ps.setObject(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                List<CustomerContactDTO> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs, 1));
                return list;
            }
        } catch (SQLException e) {
            throw new DAOException("E:CC11 顧客別の担当者一覧取得に失敗しました。", e);
        }
    }

    public CustomerContactDTO selectById(UUID id) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_ID)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs, 1);
                return new CustomerContactDTO();
            }
        } catch (SQLException e) {
            throw new DAOException("E:CC12 担当者単一取得に失敗しました。", e);
        }
    }

 // ★ ADDED: メールアドレスで 1件取得（なければ空DTOを返す）
 // ★ 修正：メールアドレスで 1件取得（全カラムを map で詰める）
    public CustomerContactDTO selectByMail(String mail) {
        final String sql = BASE_SELECT + " WHERE cc.deleted_at IS NULL AND cc.mail = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // 他の取得系（selectById / selectByCustomerId）と同じマッピング
                    return map(rs, 1);
                }
                return new CustomerContactDTO();
            }
        } catch (SQLException e) {
            throw new DAOException("E:CC14 customer_contacts メールによる単一取得に失敗しました。", e);
        }
    }

    
    // ========================
    // INSERT / UPDATE / DELETE
    // ========================

    /** 新規登録（生成IDを返す） */
    public UUID insertReturningId(CustomerContactDTO dto) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_RETURNING_ID)) {
            ps.setString(1, dto.getMail());                     // UNIQUE NOT NULL
            ps.setString(2, dto.getPassword());                 // NOT NULL
            ps.setObject(3, dto.getCustomerDTO().getId());      // FK
            ps.setString(4, dto.getName());                     // NOT NULL
            ps.setString(5, dto.getNameRuby());
            ps.setString(6, dto.getPhone());
            ps.setString(7, dto.getDepartment());
            ps.setBoolean(8, dto.isPrimary());                  // 初期 is_primary
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getObject(1, UUID.class);
                throw new DAOException("E:CC21 RETURNING id が取得できませんでした。", null);
            }
        } catch (SQLException e) {
            throw new DAOException("E:CC22 INSERT に失敗しました。", e);
        }
    }

    /** 基本更新（password 以外） */
    public int update(CustomerContactDTO dto) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, dto.getMail());
            ps.setString(2, dto.getName());
            ps.setString(3, dto.getNameRuby());
            ps.setString(4, dto.getPhone());
            ps.setString(5, dto.getDepartment());
            ps.setBoolean(6, dto.isPrimary());
            ps.setObject(7, dto.getId());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:CC31 UPDATE に失敗しました。", e);
        }
    }

    /** 論理削除 */
    public void delete(UUID id) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_LOGICAL)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:CC32 論理DELETE に失敗しました。", e);
        }
    }

    // ========================
    // 重複チェック（mail はグローバル）
    // ========================

    public boolean mailExists(String mail) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_MAIL)) {
            ps.setString(1, mail);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) >= 1;
            }
        } catch (SQLException e) {
            throw new DAOException("E:CC41 mail 重複チェックに失敗しました。", e);
        }
    }

    public boolean mailExistsExceptId(String mail, UUID excludeId) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_MAIL_EXCEPT_ID)) {
            ps.setString(1, mail);
            ps.setObject(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) >= 1;
            }
        } catch (SQLException e) {
            throw new DAOException("E:CC42 mail（除外付き）重複チェックに失敗しました。", e);
        }
    }

    // ========================
    // 主担当制御（1顧客＝1名）
    // ========================

    /** 顧客の主担当フラグを全て OFF にする。 */
    public void clearPrimaryForCustomer(UUID customerId) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_CLEAR_PRIMARY_FOR_CUSTOMER)) {
            ps.setObject(1, customerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:CC51 主担当の一括解除に失敗しました。", e);
        }
    }

    /** 指定IDの is_primary をセット。 */
    public void setPrimaryById(UUID contactId, boolean isPrimary) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SET_PRIMARY_BY_ID)) {
            ps.setBoolean(1, isPrimary);
            ps.setObject(2, contactId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:CC52 主担当フラグの更新に失敗しました。", e);
        }
    }

    // ========================
    // Mapper
    // ========================

    private CustomerContactDTO map(ResultSet rs, int i) throws SQLException {
        CustomerContactDTO dto = new CustomerContactDTO();
        dto.setId(rs.getObject(i++, UUID.class));

        UUID cid = rs.getObject(i++, UUID.class);
        CustomerDTO c = new CustomerDTO();
        c.setId(cid);
        dto.setCustomerDTO(c);
        dto.setCustomerId(cid);
        dto.setMail(rs.getString(i++));
        dto.setPassword(rs.getString(i++));
        dto.setName(rs.getString(i++));
        dto.setNameRuby(rs.getString(i++));
        dto.setPhone(rs.getString(i++));
        dto.setDepartment(rs.getString(i++));
        dto.setPrimary(rs.getBoolean(i++)); // is_primary
        dto.setCreatedAt(rs.getTimestamp(i++));
        dto.setUpdatedAt(rs.getTimestamp(i++));
        dto.setDeletedAt(rs.getTimestamp(i++));
        dto.setLastLoginAt(rs.getTimestamp(i++));
        return dto;
    }
    
 // ★ ADDED: ResultSet -> DTO 変換（BASE_SELECTの並びに準拠）
    private CustomerContactDTO resultSetToCustomerContactDTO(ResultSet rs) {
        try {
            CustomerContactDTO dto = new CustomerContactDTO();
            dto.setId(rs.getObject(1, UUID.class));
            dto.setCustomerId(rs.getObject(2, UUID.class));
            dto.setMail(rs.getString(3));
            dto.setPassword(rs.getString(4));
            dto.setName(rs.getString(5));
            dto.setNameRuby(rs.getString(6));
            dto.setPhone(rs.getString(7));
            dto.setDepartment(rs.getString(8));
            dto.setPrimary(rs.getBoolean(9));           // is_primary
            dto.setCreatedAt(rs.getTimestamp(10));
            dto.setUpdatedAt(rs.getTimestamp(11));
            dto.setDeletedAt(rs.getTimestamp(12));
            dto.setLastLoginAt(rs.getTimestamp(13));
            return dto;
        } catch (SQLException e) {
            throw new DAOException("E:CC51 ResultSet→CustomerContactDTO 変換に失敗しました。", e);
        }
    }

}