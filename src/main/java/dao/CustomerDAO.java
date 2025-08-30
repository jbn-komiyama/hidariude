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
import dto.CustomerContactDTO;
import dto.CustomerDTO;


/**
 * Customers（顧客）に関するDBアクセスを担うDAO。
 * <p>
 * ・顧客の取得／登録／更新／論理削除<br>
 * ・顧客に紐づく担当者（customer_contacts）の読取<br>
 * ・顧客とアサイン（assignments）の月次結合取得<br>
 * などを提供する。
 * </p>
 *
 * <h3>設計メモ</h3>
 * <ul>
 *   <li>トランザクション管理は {@link BaseDAO} 側の {@code Connection} に依存。</li>
 *   <li>本クラスは {@link SQLException} を受け取り {@link DAOException} にラップして投げ直す。</li>
 *   <li>ResultSet→DTO 変換はプライベートメソッドで一元化。</li>
 * </ul>
 *
 * @author Komiyama
 * @since 2025/08/30
 */
public class CustomerDAO extends BaseDAO {
	
	// ========================
    // SQL 定義（customers 起点）
    // ========================
	
	/** 顧客基本 + 担当者（LEFT JOIN）取得のベースSQL */
	private static final String SQL_SELECT_BASIC = 
			"SELECT "
			// customers (13 cols)
	      + " c.id, c.company_code, c.company_name, c.mail, c.phone, "
	      + " c.postal_code, c.address1, c.address2, c.building, "
	      + " c.primary_contact_id, c.created_at, c.updated_at, c.deleted_at, "
	      	// customer_contacts (10 cols)
	      + " cc.id, cc.mail, cc.name, cc.name_ruby, cc.phone, cc.department, "
	      + " cc.created_at, cc.updated_at, cc.deleted_at, cc.last_login_at "
	      + " FROM customers c LEFT JOIN customer_contacts cc ON c.id = cc.customer_id ";
	
	/** 顧客の新規登録（primary_contact_id はNULL、タイムスタンプは現在時刻） */
	private static final String SQL_INSERT_CUSTOMER = 
			"INSERT INTO customers "
	      + " (id, company_code, company_name, mail, phone, postal_code, address1, address2, "
	      + "  building, primary_contact_id, created_at, updated_at) "
	      + " VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
	
	/** 顧客の基本情報更新（updated_at は現在時刻に更新） */
	private static final String SQL_UPDATE_CUSTOMER = 
			"UPDATE customers "
		  + "   SET company_code = ?, company_name = ?, mail = ?, phone = ?, "
		  + "       postal_code = ?, address1 = ?, address2 = ?, building = ?, "
		  + "       updated_at = CURRENT_TIMESTAMP "
		  + " WHERE id = ?";
	
	/** 顧客の論理削除（deleted_at のみ更新） */
	private static final String SQL_DELETE_CUSTOMER_LOGICAL =
	        "UPDATE customers SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?";
	
	/** customers + assignments（指定YYYY-MM）結合取得 */
    private static final String SQL_SELECT_WITH_ASSIGNMENT =
	        "SELECT "
	        // customers (13 cols)
	      + " c.id, c.company_code, c.company_name, c.mail, c.phone, c.postal_code, c.address1, c.address2, "
	      + " c.building, c.primary_contact_id, c.created_at, c.updated_at, c.deleted_at, "
	        // assignments (15 cols)
	      + " a.id, a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month, "
	      + " a.base_pay_customer, a.base_pay_secretary, a.increase_base_pay_customer, a.increase_base_pay_secretary, "
	      + " a.customer_based_incentive_for_customer, a.customer_based_incentive_for_secretary, a.status, "
	      + " a.created_at, a.updated_at, a.deleted_at, "
	        // task_rank (1 col)
	      + " tr.rank_name, "
	        // secretaries (4 cols)
	      + " s.id, s.name, s.secretary_rank_id, s.is_pm_secretary, "
	        // secretary_rank (1 col)
	      + " sr.rank_name "
	      + " FROM customers c "
	      + " LEFT JOIN assignments a "
	      + "   ON a.customer_id = c.id "
	      + "  AND a.target_year_month = ? "
	      + "  AND a.deleted_at IS NULL "
	      + " LEFT JOIN task_rank tr ON tr.id = a.task_rank_id "
	      + " LEFT JOIN secretaries s ON s.id = a.secretary_id AND s.deleted_at IS NULL "
	      + " LEFT JOIN secretary_rank sr ON sr.id = s.secretary_rank_id "
	      + " WHERE c.deleted_at IS NULL "
	      + " ORDER BY c.company_name, a.created_at NULLS LAST";
    
    /** company_code 重複チェック用 */
    private static final String SQL_COUNT_BY_COMPANY_CODE =
            "SELECT COUNT(*) FROM customers WHERE company_code = ? AND deleted_at IS NULL";

    /** 重複（自レコード除外）チェック用（更新時に使用） */
    private static final String SQL_COUNT_BY_COMPANY_CODE_EXCEPT_ID =
            "SELECT COUNT(*) FROM customers WHERE company_code = ? AND id <> ? AND deleted_at IS NULL";

    
	public CustomerDAO(Connection conn) {
		super(conn);
	}
	
	
    // ========================
    // SELECT
    // ========================
	
	/**
     * 削除されていない顧客を全件取得する。<br>
     * LEFT JOINで担当者も取得し、顧客ごとにリスト化する。
     *
     * @return 顧客DTOのリスト（0件時は空リスト）
     * @throws DAOException DBアクセスに失敗した場合
     */
	public List<CustomerDTO> selectAll() {
		
		Map<UUID, CustomerDTO> map = new LinkedHashMap<>();
		
		final String sql = SQL_SELECT_BASIC
                + " WHERE c.deleted_at IS NULL "
                + "   AND cc.deleted_at IS NULL "
                + " ORDER BY c.company_code, cc.name";
		
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UUID cid = rs.getObject(1, UUID.class);

                // 既存 or 新規 CustomerDTO を取得
                CustomerDTO dto = map.get(cid);
                if (dto == null) {
                    dto = resultSetToCustomerDTO(rs);  				// 顧客基本情報だけ詰める
                    dto.setCustomerContacts(new ArrayList<>()); 	// 念のため初期化
                    map.put(cid, dto);
                }

                // 担当者が付いていれば追加（14列目: cc.id）
                UUID contactId = rs.getObject(14, UUID.class);
                if (contactId != null) {
                    CustomerContactDTO contact = resultSetToCustomerContactDTO(rs, 14);
                    dto.getCustomerContacts().add(contact);
                }
            }
        } catch(SQLException e) {
        	e.printStackTrace();
			String errorMsg = "E:B12 Secretaryテーブルに不正なSELECT処理が行われました。";
			throw new DAOException(errorMsg,e );
		}
		
		return new ArrayList<>(map.values());
	}
	
	
	/**
     * 指定IDの顧客（削除されていない）を1件取得します。<br>
     * 担当者は取得しません（必要なら別メソッドで）。
     *
     * @param id 顧客ID (UUID)
     * @return 該当があれば CustomerDTO、なければ空のDTO
     * @throws SQLException データベースアクセス時にエラーが発生した場合
     */
	public CustomerDTO selectByUUId(UUID id) {
        CustomerDTO dto = new CustomerDTO();

        final String sql = SQL_SELECT_BASIC
                         + " WHERE c.deleted_at IS NULL "
                         + "   AND c.id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    dto = resultSetToCustomerDTO(rs);
                }
            }
            return dto;
        } catch (SQLException e) {
            String errorMsg = "E:C12 Customers 単一取得中にエラーが発生しました。";
            throw new DAOException(errorMsg, e);
        }
    }
	
	
    /**
     * 指定の年月（{@code yyyy-MM}）に該当する assignments を
     * 顧客に結合して取得する。
     *
     * @param yearMonth 例: "2025-08"
     * @return 顧客ごとに assignments を内包したDTOリスト
     * @throws DAOException DBアクセスに失敗した場合
     */
    public List<CustomerDTO> selectAllWithAssignmentsByMonth(String yearMonth) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_WITH_ASSIGNMENT)) {
            ps.setString(1, yearMonth);

            try (ResultSet rs = ps.executeQuery()) {
                List<CustomerDTO> customerDTOs = new ArrayList<>();
                Map<UUID, CustomerDTO> customerMap = new LinkedHashMap<>();

                while (rs.next()) {
                    int i = 1;

                    // customers (13)
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
                        // 既読分スキップ（company_code..deleted_at の12列）
                        i += 12;
                    }

                    // assignments (15)
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

                        // task_rank (1)
                        ad.setTaskRankName(rs.getString(i++));

                        // secretaries (4)
                        ad.setSecretaryId(rs.getObject(i++, UUID.class));
                        ad.setSecretaryName(rs.getString(i++));
                        ad.setSecretaryRankId(rs.getObject(i++, UUID.class));
                        ad.setIsPmSecretary(rs.getBoolean(i++));

                        // secretary_rank (1)
                        ad.setSecretaryRankName(rs.getString(i++));

                        customerMap.get(cId).getAssignmentDTOs().add(ad);
                    } else {
                        // a.id = null → 残り (1 + 4 + 1) を読み飛ばし
                        i += 1 + 4 + 1;
                    }
                }

                customerDTOs.addAll(customerMap.values());
                return customerDTOs;
            }
        } catch (SQLException e) {
            String errorMsg = "E:C13 customers+assignments 取得中にエラーが発生しました。";
            throw new DAOException(errorMsg, e);
        }
    }
	
	
    
    // =======================
    // 登録
    // =======================
    
    /**
     * 顧客の新規登録を行う。
     *
     * @param dto 登録する顧客DTO
     * @return 影響行数（通常は1）
     * @throws DAOException INSERTに失敗した場合
     */
	public int insert(CustomerDTO dto) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_CUSTOMER)) {
            ps.setString(1, dto.getCompanyCode());
            ps.setString(2, dto.getCompanyName());
            ps.setString(3, dto.getMail());
            ps.setString(4, dto.getPhone());
            ps.setString(5, dto.getPostalCode());
            ps.setString(6, dto.getAddress1());
            ps.setString(7, dto.getAddress2());
            ps.setString(8, dto.getBuilding());
            return ps.executeUpdate();
        } catch (SQLException e) {
            String errorMsg = "E:C14 Customers INSERT 中にエラーが発生しました。";
            throw new DAOException(errorMsg, e);
        }
    }
	
	
    // =======================
    // 更新
    // =======================
    
    /**
     * 顧客の基本情報を更新する。
     *
     * @param dto 更新対象（{@code id} 必須）
     * @return 影響行数（通常は1）
     * @throws DAOException UPDATEに失敗した場合
     */
	public int update(CustomerDTO dto) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_CUSTOMER)) {
            ps.setString(1, dto.getCompanyCode());
            ps.setString(2, dto.getCompanyName());
            ps.setString(3, dto.getMail());
            ps.setString(4, dto.getPhone());
            ps.setString(5, dto.getPostalCode());
            ps.setString(6, dto.getAddress1());
            ps.setString(7, dto.getAddress2());
            ps.setString(8, dto.getBuilding());
            ps.setObject(9, dto.getId());
            return ps.executeUpdate();
        } catch (SQLException e) {
            String errorMsg = "E:C15 Customers UPDATE 中にエラーが発生しました。";
            throw new DAOException(errorMsg, e);
        }
	}
	
	
    // =======================
    // 削除（論理削除）
    // =======================
	
    /**
     * 顧客の論理削除を行う（{@code deleted_at} を現在時刻に更新）。
     *
     * @param id 顧客ID（UUID）
     * @throws DAOException UPDATEに失敗した場合
     */
	public void delete(UUID id) {
		
		try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_CUSTOMER_LOGICAL)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            String errorMsg = "E:C16 Customers 論理DELETE 中にエラーが発生しました。";
            throw new DAOException(errorMsg, e);
        }
	}
	
	
    // =======================
    // 重複チェック
    // =======================
	
    /**
     * company_code の重複をチェックします。
     * @param companyCode 会社コード
     * @return 既に存在すれば {@code true}
     * @throws DAOException DBアクセスに失敗した場合
     */
    public boolean companyCodeExists(String companyCode) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_BY_COMPANY_CODE)) {
            ps.setString(1, companyCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) >= 1;
            }
        } catch (SQLException e) {
            throw new DAOException("E:C42 customers.company_code 重複チェックに失敗しました。", e);
        }
    }

    /**
     * company_code の重複をチェックします（自IDは除外）。
     * 更新時に使用してください。
     * @param companyCode 会社コード
     * @param excludeId 除外するID（自分自身）
     * @return 既に存在すれば {@code true}
     * @throws DAOException DBアクセスに失敗した場合
     */
    public boolean companyCodeExistsExceptId(String companyCode, UUID excludeId) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_BY_COMPANY_CODE_EXCEPT_ID)) {
            ps.setString(1, companyCode);
            ps.setObject(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) >= 1;
            }
        } catch (SQLException e) {
            throw new DAOException("E:C43 customers.company_code（除外付き）重複チェックに失敗しました。", e);
        }
    }
	
	
	// ========================
    // ResultSet -> DTO 変換
    // ========================
    
    /**
     * 先頭列（c.id）から13列分を {@link CustomerDTO} に詰め替える。
     *
     * @param rs クエリ結果
     * @return CustomerDTO
     * @throws DAOException 変換失敗時
     */
	private CustomerDTO resultSetToCustomerDTO(ResultSet rs) {
		try {
			CustomerDTO dto = new CustomerDTO();
		    dto.setId(rs.getObject(1, java.util.UUID.class));
		    dto.setCompanyCode(rs.getString(2));
		    dto.setCompanyName(rs.getString(3));
		    dto.setMail(rs.getString(4));
		    dto.setPhone(rs.getString(5));
		    dto.setPostalCode(rs.getString(6));
		    dto.setAddress1(rs.getString(7));
		    dto.setAddress2(rs.getString(8));
		    dto.setBuilding(rs.getString(9));
		    dto.setPrimaryContactId(rs.getObject(10, java.util.UUID.class));
		    dto.setCreatedAt(rs.getTimestamp(11));
		    dto.setUpdatedAt(rs.getTimestamp(12));
		    dto.setDeletedAt(rs.getTimestamp(13));
		    return dto;
		} catch(SQLException e) {
			String errorMsg = "E:C17 ResultSet→CustomerDTO 変換中にエラーが発生しました。";
			throw new DAOException(errorMsg, e);
		}
	}
	
	
    /**
     * 指定開始位置（例：14列目＝cc.id）から10列分を
     * {@link CustomerContactDTO} に詰め替える。
     *
     * @param rs  クエリ結果
     * @param num 開始カラム番号（1始まり）
     * @return CustomerContactDTO
     * @throws DAOException 変換失敗時
     */
	private CustomerContactDTO resultSetToCustomerContactDTO(ResultSet rs, int num) {
		try {
			CustomerContactDTO dto = new CustomerContactDTO();
		    dto.setId(rs.getObject(num++, java.util.UUID.class));
		    dto.setMail(rs.getString(num++));
		    dto.setName(rs.getString(num++));
		    dto.setNameRuby(rs.getString(num++));
		    dto.setPhone(rs.getString(num++));
		    dto.setDepartment(rs.getString(num++));
		    dto.setCreatedAt(rs.getTimestamp(num++));
		    dto.setUpdatedAt(rs.getTimestamp(num++));
		    dto.setDeletedAt(rs.getTimestamp(num++));
		    dto.setLastLoginAt(rs.getTimestamp(num++));
		    return dto;
		} catch(SQLException e) {
			String errorMsg = "E:C18 ResultSet→CustomerContactDTO 変換中にエラーが発生しました。";
			throw new DAOException(errorMsg, e);
		}
	}
}