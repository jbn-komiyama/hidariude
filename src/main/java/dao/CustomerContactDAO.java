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
 * 顧客担当者（<code>customer_contacts</code>）用 DAO。
 * <ul>
 *   <li><code>mail</code> は全体で UNIQUE（NOT NULL）</li>
 *   <li><code>password</code> は NOT NULL（新規登録必須）</li>
 *   <li><code>is_primary</code> で主担当管理（1顧客につき1名へ正規化）</li>
 * </ul>
 * <p>
 * 例外は {@link DAOException} にラップして上位に伝播します。
 * 取得系は論理削除（<code>deleted_at IS NULL</code>）を自動で除外します。
 * </p>
 *
 * <h2>クラス構成</h2>
 * <ol>
 *   <li>フィールド（SQL 文）</li>
 *   <li>フィールド、コンストラクタ</li>
 *   <li>メソッド（SELECT／CUD／重複チェック／主担当制御／マッパ）</li>
 * </ol>
 */
public class CustomerContactDAO extends BaseDAO {

    // ========================
    // ① フィールド（SQL 定義）
    // ========================

    /** 一覧・単体取得の共通 SELECT 句 */
    private static final String BASE_SELECT =
        "SELECT cc.id, cc.customer_id, cc.mail, cc.password, cc.name, cc.name_ruby, " +
        "       cc.phone, cc.department, cc.is_primary, cc.created_at, cc.updated_at, cc.deleted_at, cc.last_login_at " +
        "  FROM customer_contacts cc ";

    /** 顧客別（論理未削除のみ） */
    private static final String SQL_SELECT_BY_CUSTOMER =
        BASE_SELECT + " WHERE cc.deleted_at IS NULL AND cc.customer_id = ? ORDER BY cc.name";

    /** 主キー指定（論理未削除のみ） */
    private static final String SQL_SELECT_BY_ID =
        BASE_SELECT + " WHERE cc.deleted_at IS NULL AND cc.id = ?";

    /** INSERT（生成IDを返す：RETURNING id） */
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

    /** 論理削除（deleted_at を現在時刻に設定） */
    private static final String SQL_DELETE_LOGICAL =
        "UPDATE customer_contacts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?";

    /** mail グローバル重複チェック（論理未削除のみ） */
    private static final String SQL_COUNT_MAIL =
        "SELECT COUNT(*) FROM customer_contacts WHERE deleted_at IS NULL AND mail = ?";

    /** mail グローバル重複チェック（自ID除外＆論理未削除のみ） */
    private static final String SQL_COUNT_MAIL_EXCEPT_ID =
        "SELECT COUNT(*) FROM customer_contacts WHERE deleted_at IS NULL AND mail = ? AND id <> ?";

    /** 主担当一括 OFF（顧客単位） */
    private static final String SQL_CLEAR_PRIMARY_FOR_CUSTOMER =
        "UPDATE customer_contacts SET is_primary = FALSE, updated_at = CURRENT_TIMESTAMP WHERE customer_id = ?";

    /** 対象行の主担当フラグ更新 */
    private static final String SQL_SET_PRIMARY_BY_ID =
        "UPDATE customer_contacts SET is_primary = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

    // ========================
    // ② フィールド、コンストラクタ
    // ========================

    /**
     * コンストラクタ。
     *
     * @param conn 呼び出し側で管理される JDBC コネクション
     */
    public CustomerContactDAO(Connection conn) { 
        super(conn); 
    }

    // ========================
    // ③ メソッド
    // ========================
    // -------- SELECT --------

    /**
     * 顧客IDで担当者一覧を取得します（論理未削除のみ／氏名昇順）。
     *
     * @param customerId 顧客ID（必須）
     * @return 該当担当者 {@link CustomerContactDTO} のリスト（0件可）
     * @throws DAOException SQL 実行時にエラーが発生した場合
     */
    public List<CustomerContactDTO> selectByCustomerId(UUID customerId) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_CUSTOMER)) {
            ps.setObject(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                List<CustomerContactDTO> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs, 1)); // カラム先頭を1として順次マップ
                return list;
            }
        } catch (SQLException e) {
            throw new DAOException("E:CC11 顧客別の担当者一覧取得に失敗しました。", e);
        }
    }

    /**
     * 担当者IDで1件取得します（論理未削除のみ）。
     * <p>見つからない場合は空の DTO（フィールド未セット）を返します。</p>
     *
     * @param id 担当者ID（必須）
     * @return 該当 {@link CustomerContactDTO}／未存在時は空 DTO
     * @throws DAOException SQL 実行時にエラーが発生した場合
     */
    public CustomerContactDTO selectById(UUID id) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_ID)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs, 1);
                return new CustomerContactDTO(); // 未存在は空DTOで呼び出し側の互換性維持
            }
        } catch (SQLException e) {
            throw new DAOException("E:CC12 担当者単一取得に失敗しました。", e);
        }
    }

    /**
     * メールアドレスで1件取得します（論理未削除のみ）。
     * <p>見つからない場合は空の DTO（フィールド未セット）を返します。</p>
     *
     * @param mail メールアドレス（UNIQUE／必須）
     * @return 該当 {@link CustomerContactDTO}／未存在時は空 DTO
     * @throws DAOException SQL 実行時にエラーが発生した場合
     */
    public CustomerContactDTO selectByMail(String mail) {
        final String sql = BASE_SELECT + " WHERE cc.deleted_at IS NULL AND cc.mail = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs, 1); // 取得位置1から BASE_SELECT の並びで詰める
                }
                return new CustomerContactDTO();
            }
        } catch (SQLException e) {
            throw new DAOException("E:CC14 customer_contacts メールによる単一取得に失敗しました。", e);
        }
    }

    // ---- INSERT / UPDATE / DELETE ----

    /**
     * 新規登録を行い、生成されたID（UUID）を返します。
     * <ul>
     *   <li><code>mail</code> は UNIQUE かつ NOT NULL</li>
     *   <li><code>password</code> は NOT NULL</li>
     *   <li><code>customer_id</code> は外部キー</li>
     *   <li><code>is_primary</code> は初期フラグとして受け取ります（主担当正規化は上位で制御）</li>
     * </ul>
     *
     * @param dto 登録内容（必須フィールドは呼び出し側で事前検証）
     * @return 生成された主キー（UUID）
     * @throws DAOException INSERT または RETURNING 取得に失敗した場合
     */
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

    /**
     * 基本情報を更新します（<b>password は対象外</b>）。
     * <p><code>updated_at</code> はサーバー時刻で更新されます。</p>
     *
     * @param dto 更新内容（<code>id</code> 必須）
     * @return 影響行数（通常 1）
     * @throws DAOException UPDATE に失敗した場合
     */
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

    /**
     * 論理削除を行います（<code>deleted_at</code> に現在時刻を設定）。
     *
     * @param id 担当者ID
     * @throws DAOException UPDATE（論理削除）に失敗した場合
     */
    public void delete(UUID id) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_LOGICAL)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:CC32 論理DELETE に失敗しました。", e);
        }
    }

    // ---- 重複チェック（mail はグローバル）----

    /**
     * メールアドレスの存在有無をチェックします（論理未削除のみ対象）。
     *
     * @param mail メールアドレス
     * @return true: 既に存在／false: 未使用
     * @throws DAOException SQL 実行時にエラーが発生した場合
     */
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

    /**
     * メールアドレスの存在有無をチェックします（自IDを除外／論理未削除のみ対象）。
     *
     * @param mail      メールアドレス
     * @param excludeId 除外する担当者ID（自身）
     * @return true:（自分以外で）既に存在／false: 未使用
     * @throws DAOException SQL 実行時にエラーが発生した場合
     */
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

    // ---- 主担当制御（1顧客＝1名に正規化）----

    /**
     * 指定顧客の全担当者の主担当フラグを一括で OFF にします。
     * <p>主担当の正規化（1顧客＝1名）を行う前処理として利用します。</p>
     *
     * @param customerId 顧客ID
     * @throws DAOException SQL 実行時にエラーが発生した場合
     */
    public void clearPrimaryForCustomer(UUID customerId) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_CLEAR_PRIMARY_FOR_CUSTOMER)) {
            ps.setObject(1, customerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:CC51 主担当の一括解除に失敗しました。", e);
        }
    }

    /**
     * 指定担当者の主担当フラグを更新します。
     *
     * @param contactId  担当者ID
     * @param isPrimary  設定する主担当フラグ（true: 主担当／false: 主担当解除）
     * @throws DAOException SQL 実行時にエラーが発生した場合
     */
    public void setPrimaryById(UUID contactId, boolean isPrimary) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SET_PRIMARY_BY_ID)) {
            ps.setBoolean(1, isPrimary);
            ps.setObject(2, contactId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:CC52 主担当フラグの更新に失敗しました。", e);
        }
    }

    // -------- Mapper --------

    /**
     * {@link ResultSet} の現在行を {@link CustomerContactDTO} にマッピングします。
     * <p>
     * 先頭カラムのインデックス（1 始まり）を指定し、<br>
     * {@link #BASE_SELECT} のカラム順に従って DTO へ詰めます。
     * </p>
     *
     * @param rs  結果セット（現在行が有効であること）
     * @param i   先頭カラムのインデックス（1 始まり）
     * @return マッピング済み DTO
     * @throws SQLException JDBC アクセスエラー
     */
    private CustomerContactDTO map(ResultSet rs, int i) throws SQLException {
        CustomerContactDTO dto = new CustomerContactDTO();
        dto.setId(rs.getObject(i++, UUID.class));

        // customer_id は CustomerDTO と重複保持（利便性のため両方にセット）
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
}
