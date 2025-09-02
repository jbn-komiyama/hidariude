package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dto.AssignmentDTO;
import dto.CustomerDTO;

public class AssignmentDAO extends BaseDAO {
	
	// ========================
    // SQL 定義
    // ========================

    /** 指定月の customers + assignments + task_rank + secretaries + secretary_rank をまとめて取得 */
    private static final String SQL_SELECT_BY_MONTH =
        "SELECT"
        // customers (13)
      + " c.id, c.company_code, c.company_name, c.mail, c.phone, c.postal_code, c.address1, c.address2, c.building,"
      + " c.primary_contact_id, c.created_at, c.updated_at, c.deleted_at,"
        // assignments (15)
      + " a.id, a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month,"
      + " a.base_pay_customer, a.base_pay_secretary, a.increase_base_pay_customer, a.increase_base_pay_secretary,"
      + " a.customer_based_incentive_for_customer, a.customer_based_incentive_for_secretary, a.status,"
      + " a.created_at, a.updated_at, a.deleted_at,"
        // task_rank (1)
      + " tr.rank_name,"
        // secretaries (3) 取得順に注意: id, secretary_rank_id, name
      + " s.id, s.secretary_rank_id, s.name,"
        // secretary_rank (1)
      + " sr.rank_name"
      + " FROM customers c"
      + " LEFT JOIN assignments a"
      + "   ON a.customer_id = c.id"
      + "  AND a.target_year_month = ?"
      + "  AND a.deleted_at IS NULL"
      + " LEFT JOIN task_rank tr ON tr.id = a.task_rank_id"
      + " LEFT JOIN secretaries s ON s.id = a.secretary_id AND s.deleted_at IS NULL"
      + " LEFT JOIN secretary_rank sr ON sr.id = s.secretary_rank_id"
      + " WHERE c.deleted_at IS NULL"
      + " ORDER BY c.company_name, tr.rank_name, a.created_at NULLS LAST";
    
    /** 指定秘書・指定月の assignments を顧客単位で取得（customers最小限 + task_rank名付き） */
    private static final String SQL_SELECT_BY_SECRETARY_AND_MONTH =
        "SELECT " +
        // customers (2)
        " c.id AS c_id, c.company_name, " +
        // assignments (15)
        " a.id, a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month, " +
        " a.base_pay_customer, a.base_pay_secretary, a.increase_base_pay_customer, a.increase_base_pay_secretary, " +
        " a.customer_based_incentive_for_customer, a.customer_based_incentive_for_secretary, a.status, " +
        " a.created_at, a.updated_at, a.deleted_at, " +
        // task_rank (1)
        " tr.rank_name " +
        "FROM assignments a " +
        "JOIN customers c ON c.id = a.customer_id AND c.deleted_at IS NULL " +
        "LEFT JOIN task_rank tr ON tr.id = a.task_rank_id " +
        "WHERE a.secretary_id = ? " +
        "  AND a.target_year_month = ? " +
        "  AND a.deleted_at IS NULL " +
        "ORDER BY c.company_name, tr.rank_name NULLS LAST, a.created_at";

    /** assignments の INSERT（id を RETURNING） */
    private static final String SQL_INSERT =
        "INSERT INTO assignments ("
      + " customer_id, secretary_id, task_rank_id, target_year_month,"
      + " base_pay_customer, base_pay_secretary,"
      + " increase_base_pay_customer, increase_base_pay_secretary,"
      + " customer_based_incentive_for_customer, customer_based_incentive_for_secretary,"
      + " status, created_at, updated_at"
      + ") VALUES (?,?,?,?,?,?,?,?,?,?,?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
      + " RETURNING id";

    /** 同一（年月, 顧客, 秘書, タスクランク）の重複存在チェック */
    private static final String SQL_EXISTS_DUPLICATE =
        "SELECT 1 FROM assignments"
      + " WHERE target_year_month = ? AND customer_id = ? AND secretary_id = ? AND task_rank_id = ?"
      + "   AND deleted_at IS NULL"
      + " LIMIT 1";
    
    // assignments の UPDATE（id指定、論理削除済みは対象外）
    private static final String SQL_UPDATE =
        "UPDATE assignments "
      + "   SET secretary_id = ?, "
      + "       task_rank_id = ?, "
      + "       target_year_month = ?, "
      + "       base_pay_customer = ?, "
      + "       base_pay_secretary = ?, "
      + "       increase_base_pay_customer = ?, "
      + "       increase_base_pay_secretary = ?, "
      + "       customer_based_incentive_for_customer = ?, "
      + "       customer_based_incentive_for_secretary = ?, "
      + "       status = ?, "
      + "       updated_at = CURRENT_TIMESTAMP "
      + " WHERE id = ? "
      + "   AND deleted_at IS NULL";

    // assignments の 論理DELETE（id指定）
    private static final String SQL_DELETE_LOGICAL =
        "UPDATE assignments "
      + "   SET deleted_at = CURRENT_TIMESTAMP "
      + " WHERE id = ? "
      + "   AND deleted_at IS NULL";
    
	
	public AssignmentDAO(Connection conn) {
		super(conn);
	}
	
	// ========================
    // SELECT
    // ========================

    /**
     * 指定年月（yyyy-MM）のアサイン情報を、顧客ごとに束ねて取得します。
     * <p>顧客は削除されていないもののみ、アサインは同月・未削除のものを LEFT JOIN します。</p>
     *
     * @param yearMonth 例: {@code "2025-08"}
     * @return 顧客ごとに {@link AssignmentDTO} を格納した {@link CustomerDTO} のリスト
     * @throws DAOException 取得時にエラーが発生した場合
     */
    public List<CustomerDTO> selectAllByMonth(String yearMonth) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_MONTH)) {
            ps.setString(1, yearMonth);

            try (ResultSet rs = ps.executeQuery()) {
                Map<UUID, CustomerDTO> customerMap = new LinkedHashMap<>();

                while (rs.next()) {
                    int i = 1;

                    // ---- customers (13)
                    UUID cId = rs.getObject(i++, UUID.class);
                    CustomerDTO c = customerMap.get(cId);
                    if (c == null) {
                        c = new CustomerDTO();
                        c.setId(cId);
                        c.setCompanyCode(rs.getString(i++));
                        c.setCompanyName(rs.getString(i++));
                        c.setMail(rs.getString(i++));
                        c.setPhone(rs.getString(i++));
                        c.setPostalCode(rs.getString(i++));
                        c.setAddress1(rs.getString(i++));
                        c.setAddress2(rs.getString(i++));
                        c.setBuilding(rs.getString(i++));
                        c.setPrimaryContactId(rs.getObject(i++, UUID.class));
                        c.setCreatedAt(rs.getTimestamp(i++));
                        c.setUpdatedAt(rs.getTimestamp(i++));
                        c.setDeletedAt(rs.getTimestamp(i++));
                        c.setAssignmentDTOs(new ArrayList<>());
                        customerMap.put(cId, c);
                    } else {
                        // 既読分（company_code..deleted_at の12列）をスキップ
                        i += 12;
                    }

                    // ---- assignments (15)
                    UUID aId = rs.getObject(i++, UUID.class);
                    if (aId != null) {
                        AssignmentDTO ad = new AssignmentDTO();
                        ad.setAssignmentId(aId);
                        ad.setAssignmentCustomerId(rs.getObject(i++, UUID.class));
                        ad.setAssignmentSecretaryId(rs.getObject(i++, UUID.class));
                        ad.setTaskRankId(rs.getObject(i++, UUID.class));
                        ad.setTargetYearMonth(rs.getString(i++));
                        ad.setBasePayCustomer(rs.getBigDecimal(i++));
                        ad.setBasePaySecretary(rs.getBigDecimal(i++));
                        ad.setIncreaseBasePayCustomer(rs.getBigDecimal(i++));
                        ad.setIncreaseBasePaySecretary(rs.getBigDecimal(i++));
                        ad.setCustomerBasedIncentiveForCustomer(rs.getBigDecimal(i++));
                        ad.setCustomerBasedIncentiveForSecretary(rs.getBigDecimal(i++));
                        ad.setAssignmentStatus(rs.getString(i++));
                        ad.setAssignmentCreatedAt(rs.getTimestamp(i++));
                        ad.setAssignmentUpdatedAt(rs.getTimestamp(i++));
                        ad.setAssignmentDeletedAt(rs.getTimestamp(i++));

                        // ---- task_rank (1)
                        ad.setTaskRankName(rs.getString(i++));

                        // ---- secretaries (3)
                        ad.setSecretaryId(rs.getObject(i++, UUID.class));
                        ad.setSecretaryRankId(rs.getObject(i++, UUID.class));
                        ad.setSecretaryName(rs.getString(i++));

                        // ---- secretary_rank (1)
                        ad.setSecretaryRankName(rs.getString(i++));

                        c.getAssignmentDTOs().add(ad);
                    } else {
                        // a.id が NULL（当該月にアサインなし）の場合は読み飛ばしでOK
                        // i はこの行内でのみ使用されるため、明示的スキップは不要
                    }
                }

                return new ArrayList<>(customerMap.values());
            }
        } catch (SQLException e) {
            throw new DAOException("E:AS11 指定月の assignments 取得に失敗しました。", e);
        }
    }
    
    /**
     * 指定した秘書ID・年月（yyyy-MM）のアサイン情報を顧客単位で取得します。
     * <p>顧客は {@code id, company_name} のみをセットし、その配下に {@link AssignmentDTO} を格納します。</p>
     *
     * @param secretaryId 秘書ID（{@link UUID}）
     * @param yearMonth   年月（例: "2025-09"）
     * @return 顧客ごとに assignments を束ねた {@link CustomerDTO} のリスト（会社名昇順）
     * @throws DAOException 取得時にエラーが発生した場合
     */
    public List<CustomerDTO> selectBySecretaryAndMonth(UUID secretaryId, String yearMonth) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_SECRETARY_AND_MONTH)) {
            int p = 1;
            ps.setObject(p++, secretaryId);
            ps.setString(p++, yearMonth);

            try (ResultSet rs = ps.executeQuery()) {
                Map<UUID, CustomerDTO> customerMap = new LinkedHashMap<>();

                while (rs.next()) {
                    int i = 1;

                    // ---- customers (2)
                    UUID cId = rs.getObject(i++, UUID.class);
                    String companyName = rs.getString(i++);

                    CustomerDTO c = customerMap.get(cId);
                    if (c == null) {
                        c = new CustomerDTO();
                        c.setId(cId);
                        c.setCompanyName(companyName);
                        c.setAssignmentDTOs(new ArrayList<>());
                        customerMap.put(cId, c);
                    }

                    // ---- assignments (15)
                    UUID aId = rs.getObject(i++, UUID.class);
                    AssignmentDTO ad = new AssignmentDTO();
                    ad.setAssignmentId(aId);
                    ad.setAssignmentCustomerId(rs.getObject(i++, UUID.class));
                    ad.setAssignmentSecretaryId(rs.getObject(i++, UUID.class));
                    ad.setTaskRankId(rs.getObject(i++, UUID.class));
                    ad.setTargetYearMonth(rs.getString(i++));
                    ad.setBasePayCustomer(rs.getBigDecimal(i++));
                    ad.setBasePaySecretary(rs.getBigDecimal(i++));
                    ad.setIncreaseBasePayCustomer(rs.getBigDecimal(i++));
                    ad.setIncreaseBasePaySecretary(rs.getBigDecimal(i++));
                    ad.setCustomerBasedIncentiveForCustomer(rs.getBigDecimal(i++));
                    ad.setCustomerBasedIncentiveForSecretary(rs.getBigDecimal(i++));
                    ad.setAssignmentStatus(rs.getString(i++));
                    ad.setAssignmentCreatedAt(rs.getTimestamp(i++));
                    ad.setAssignmentUpdatedAt(rs.getTimestamp(i++));
                    ad.setAssignmentDeletedAt(rs.getTimestamp(i++));

                    // ---- task_rank (1)
                    ad.setTaskRankName(rs.getString(i++));

                    // 顧客配下に格納
                    c.getAssignmentDTOs().add(ad);
                }

                return new ArrayList<>(customerMap.values());
            }
        } catch (SQLException e) {
            throw new DAOException("E:AS12 指定秘書・指定月の assignments 取得に失敗しました。", e);
        }
    }

    // ========================
    // INSERT
    // ========================

    /**
     * アサインを新規登録し、採番された {@code id} を返します。
     * <p>一部の金額カラムは NULL を許容します。</p>
     *
     * @param dto 登録対象
     * @return 新規採番された {@link UUID}（失敗時や RETURNING 無返却なら {@code null}）
     * @throws DAOException 登録時にエラーが発生した場合
     */
    public UUID insert(AssignmentDTO dto) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            int i = 1;
            // 必須
            ps.setObject(i++, dto.getAssignmentCustomerId());
            ps.setObject(i++, dto.getAssignmentSecretaryId());
            ps.setObject(i++, dto.getTaskRankId());
            ps.setString(i++, dto.getTargetYearMonth());

            // 金額（NULL許容）
            setBigDecimalOrNull(ps, i++, dto.getBasePayCustomer());
            setBigDecimalOrNull(ps, i++, dto.getBasePaySecretary());
            setBigDecimalOrNull(ps, i++, dto.getIncreaseBasePayCustomer());
            setBigDecimalOrNull(ps, i++, dto.getIncreaseBasePaySecretary());
            setBigDecimalOrNull(ps, i++, dto.getCustomerBasedIncentiveForCustomer());
            setBigDecimalOrNull(ps, i++, dto.getCustomerBasedIncentiveForSecretary());

            // status（NULL許容）
            if (dto.getAssignmentStatus() == null || dto.getAssignmentStatus().isEmpty()) {
                ps.setNull(i++, java.sql.Types.VARCHAR);
            } else {
                ps.setString(i++, dto.getAssignmentStatus());
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1, UUID.class) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("E:AS21 assignments INSERT に失敗しました。", e);
        }
    }

    // ========================
    // EXISTS（重複チェック）
    // ========================

    /**
     * 同一（年月・顧客・秘書・タスクランク）のアサインが存在するかを判定します。
     *
     * @param dto チェック対象
     * @return 重複が存在すれば {@code true}
     * @throws DAOException 判定時にエラーが発生した場合
     */
    public boolean existsDuplicate(AssignmentDTO dto) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_EXISTS_DUPLICATE)) {
            int i = 1;
            ps.setString(i++, dto.getTargetYearMonth());
            ps.setObject(i++, dto.getAssignmentCustomerId());
            ps.setObject(i++, dto.getAssignmentSecretaryId());
            ps.setObject(i++, dto.getTaskRankId());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DAOException("E:AS31 アサイン重複チェックに失敗しました。", e);
        }
    }
    
    // ========================
    // UPDATE
    // ========================
    
    /**
     * アサインを更新します（論理削除済みは対象外）。
     * <p>NULL 許容の金額カラム／status は NULL をセット可能です。</p>
     *
     * @param dto 更新対象（assignmentId 必須）
     * @return 影響行数
     * @throws DAOException 更新時にエラーが発生した場合、または必須IDが未設定の場合
     */
    public int update(AssignmentDTO dto) {
        if (dto.getAssignmentId() == null) {
            throw new DAOException("E:AS40 assignmentId が未設定です。");
        }

        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            int i = 1;
            ps.setObject(i++, dto.getAssignmentSecretaryId());
            ps.setObject(i++, dto.getTaskRankId());
            ps.setString(i++, dto.getTargetYearMonth());

            setBigDecimalOrNull(ps, i++, dto.getBasePayCustomer());
            setBigDecimalOrNull(ps, i++, dto.getBasePaySecretary());
            setBigDecimalOrNull(ps, i++, dto.getIncreaseBasePayCustomer());
            setBigDecimalOrNull(ps, i++, dto.getIncreaseBasePaySecretary());
            setBigDecimalOrNull(ps, i++, dto.getCustomerBasedIncentiveForCustomer());
            setBigDecimalOrNull(ps, i++, dto.getCustomerBasedIncentiveForSecretary());

            if (dto.getAssignmentStatus() == null || dto.getAssignmentStatus().isEmpty()) {
                ps.setNull(i++, java.sql.Types.VARCHAR);
            } else {
                ps.setString(i++, dto.getAssignmentStatus());
            }

            ps.setObject(i++, dto.getAssignmentId()); // WHERE id = ?

            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:AS41 assignments UPDATE に失敗しました。", e);
        }
    }
    
    
    // ========================
    // DELETE
    // ========================

    /**
     * アサインを論理削除します（deleted_at を現在時刻に更新）。
     *
     * @param id assignments.id
     * @return 影響行数
     * @throws DAOException 削除時にエラーが発生した場合
     */
    public int delete(UUID id) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_LOGICAL)) {
            ps.setObject(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:AS51 assignments 論理DELETE に失敗しました。", e);
        }
    }
    

    // ========================
    // Helper
    // ========================

    /**
     * {@code BigDecimal} を NULL 許容でセットします。
     *
     * @param ps    PreparedStatement
     * @param index パラメータ位置
     * @param val   値（NULL 可）
     * @throws SQLException セット時に発生
     */
    private void setBigDecimalOrNull(PreparedStatement ps, int index, java.math.BigDecimal val) throws SQLException {
        if (val == null) {
            ps.setNull(index, java.sql.Types.NUMERIC);
        } else {
            ps.setBigDecimal(index, val);
        }
    }
}