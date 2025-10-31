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
 * Customers（顧客）テーブルを中心としたデータアクセスを提供する DAO。
 *
 * 主な責務：
 * - 顧客の取得／登録／更新／論理削除
 * - 顧客に紐づく担当者（customer_contacts）の読取
 * - 顧客とアサイン（assignments）の月次結合取得
 *
 * 設計メモ：
 * - トランザクション境界は呼び出し側（サービス）で管理し、本DAOは渡された {@link Connection} にのみ依存します。
 * - 本クラス内で発生した {@link SQLException} は、全て実行時例外 {@link DAOException} に包んで再スローします。
 * - ResultSet→DTO の詰替えはプライベートヘルパーで一元化し、カラム順に依存する箇所は丁寧にコメントします。
 *
 * @author Komiyama
 * @since 2025/08/30
 */
public class CustomerDAO extends BaseDAO {

	/** =========================================================
	 * ① フィールド（SQL）
	 * ========================================================= */

	/** 顧客基本 + 担当者（LEFT JOIN）取得のベースSQL（customers 13列 + customer_contacts 10列） */
	private static final String SQL_SELECT_BASIC = "SELECT "
			/** customers (13 cols) */
			+ " c.id, c.company_code, c.company_name, c.mail, c.phone, "
			+ " c.postal_code, c.address1, c.address2, c.building, "
			+ " c.primary_contact_id, c.created_at, c.updated_at, c.deleted_at, "
			/** customer_contacts (10 cols) */
			+ " cc.id, cc.mail, cc.name, cc.name_ruby, cc.phone, cc.department, "
			+ " cc.created_at, cc.updated_at, cc.deleted_at, cc.last_login_at "
			+ " FROM customers c LEFT JOIN customer_contacts cc ON c.id = cc.customer_id ";

	private static final String SQL_SELECT_WITH_CONTACTS = "SELECT "
			/** customers (13) */
			+ "  c.id, c.company_code, c.company_name, c.mail, c.phone, "
			+ "  c.postal_code, c.address1, c.address2, c.building, "
			+ "  c.primary_contact_id, c.created_at, c.updated_at, c.deleted_at, "
			/** customer_contacts (10) */
			+ "  cc.id, cc.mail, cc.name, cc.name_ruby, cc.phone, cc.department, "
			+ "  cc.created_at, cc.updated_at, cc.deleted_at, cc.last_login_at "
			+ "FROM customers c "
			+ "LEFT JOIN customer_contacts cc "
			+ "  ON cc.customer_id = c.id "
			+ " AND cc.deleted_at IS NULL "
			+ "WHERE c.deleted_at IS NULL "
			+ "  AND c.id = ? "
			+ "ORDER BY "
			+ "  CASE WHEN cc.id IS NOT NULL AND c.primary_contact_id = cc.id THEN 0 ELSE 1 END, "
			+ "  cc.created_at NULLS LAST";

	/** 顧客の新規登録（primary_contact_id はNULL、タイムスタンプは現在時刻） */
	private static final String SQL_INSERT_CUSTOMER = "INSERT INTO customers "
			+ " (id, company_code, company_name, mail, phone, postal_code, address1, address2, "
			+ "  building, primary_contact_id, created_at, updated_at) "
			+ " VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

	/** 顧客の基本情報更新（updated_at は現在時刻に更新） */
	private static final String SQL_UPDATE_CUSTOMER = "UPDATE customers "
			+ "   SET company_code = ?, company_name = ?, mail = ?, phone = ?, "
			+ "       postal_code = ?, address1 = ?, address2 = ?, building = ?, "
			+ "       updated_at = CURRENT_TIMESTAMP "
			+ " WHERE id = ?";

	/** 顧客の論理削除（deleted_at のみ更新） */
	private static final String SQL_DELETE_CUSTOMER_LOGICAL = "UPDATE customers SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?";

	/** customers + assignments（指定YYYY-MM）結合取得 */
	private static final String SQL_SELECT_WITH_ASSIGNMENT = "SELECT "
			/** customers (13 cols) */
			+ " c.id, c.company_code, c.company_name, c.mail, c.phone, c.postal_code, c.address1, c.address2, "
			+ " c.building, c.primary_contact_id, c.created_at, c.updated_at, c.deleted_at, "
			/** assignments (15 cols) */
			+ " a.id, a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month, "
			+ " a.base_pay_customer, a.base_pay_secretary, a.increase_base_pay_customer, a.increase_base_pay_secretary, "
			+ " a.customer_based_incentive_for_customer, a.customer_based_incentive_for_secretary, a.status, "
			+ " a.created_at, a.updated_at, a.deleted_at, "
			/** task_rank (1 col) */
			+ " tr.rank_name, "
			/** secretaries (4 cols) */
			+ " s.id, s.name, s.secretary_rank_id, s.is_pm_secretary, "
			/** secretary_rank (1 col) */
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
	
	/** 最近登録された顧客10件を返します。 */
	private static final String SQL_SELECT_RECENT10_CUSTOMERS =
	    "SELECT id, company_code, company_name, mail, phone, created_at " +
	    "  FROM customers " +
	    " WHERE deleted_at IS NULL " +
	    " ORDER BY created_at DESC " +
	    " LIMIT 10";

	/** company_code 重複チェック用 */
	private static final String SQL_COUNT_BY_COMPANY_CODE = "SELECT COUNT(*) FROM customers WHERE company_code = ? AND deleted_at IS NULL";

	/** 重複（自レコード除外）チェック用（更新時に使用） */
	private static final String SQL_COUNT_BY_COMPANY_CODE_EXCEPT_ID = "SELECT COUNT(*) FROM customers WHERE company_code = ? AND id <> ? AND deleted_at IS NULL";

	/** =========================================================
	 * ② フィールド／コンストラクタ
	 * ========================================================= */

	/**
	 * コンストラクタ。
	 *
	 * @param conn 呼び出し側トランザクションから受け取るコネクション
	 */
	/** 直近2か月（YYYY-MM を2本）で、顧客のアサイン一覧を取得（status列なし） */
    private static final String SQL_SELECT_TWO_MONTH_ASSIGNMENT =
            "SELECT " +
            "  a.id, a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month, " +
            "  a.base_pay_customer, a.base_pay_secretary, a.increase_base_pay_customer, a.increase_base_pay_secretary, " +
            "  a.customer_based_incentive_for_customer, a.customer_based_incentive_for_secretary, " + /** ここまで assignments の金額系 */
            "  a.created_at, a.updated_at, a.deleted_at, " +                                           /** status を除去 */
            "  tr.rank_name, " +
            "  s.id, s.name, s.secretary_rank_id, s.is_pm_secretary, " +
            "  sr.rank_name AS secretary_rank_name " +
            "FROM assignments a " +
            "JOIN task_rank   tr ON tr.id = a.task_rank_id " +
            "JOIN secretaries s  ON s.id = a.secretary_id AND s.deleted_at IS NULL " +
            "LEFT JOIN secretary_rank sr ON sr.id = s.secretary_rank_id " +
            "WHERE a.deleted_at IS NULL " +
            "  AND a.customer_id = ? " +
            "  AND a.target_year_month IN (?, ?) " +
            "ORDER BY a.target_year_month DESC, s.name ASC";

 
    
	public CustomerDAO(Connection conn) {
		super(conn);
	}

	/** =========================================================
	 * ③ メソッド
	 *   3-1. SELECT
	 *   3-2. INSERT
	 *   3-3. UPDATE
	 *   3-4. DELETE（論理）
	 *   3-5. 重複チェック
	 *   3-6. ResultSet→DTO 変換（private）
	 * =========================================================
	 *
	 * =========================
	 * 3-1. SELECT
	 * ========================= */

	/**
	 * 削除されていない顧客を全件取得します（担当者は LEFT JOIN で同時取得）。
	 * 顧客ごとに担当者リストを構築し、{@link CustomerDTO#getCustomerContacts()} に格納します。
	 *
	 * 挙動:
	 * - customers.deleted_at IS NULL / customer_contacts.deleted_at IS NULL を条件に適用
	 * - 会社コード、担当者名でソート
	 *
	 * @return 顧客DTOのリスト（0件時は空リスト）
	 * @throws DAOException DBアクセスエラーが発生した場合
	 */
	public List<CustomerDTO> selectAll() {
		Map<UUID, CustomerDTO> map = new LinkedHashMap<>();

		/** cc.deleted_at IS NULL の条件を JOIN の ON 句に移動することで、
		 * 担当者がいない（または全て削除された）顧客も一覧に表示されるようにする */
		final String sql = SQL_SELECT_BASIC
				+ "   AND cc.deleted_at IS NULL "
				+ " WHERE c.deleted_at IS NULL "
				+ " ORDER BY c.company_code, cc.name";

		try (PreparedStatement ps = conn.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				UUID cid = rs.getObject(1, UUID.class);

				/** 既存 or 新規 CustomerDTO を取得 */
				CustomerDTO dto = map.get(cid);
				if (dto == null) {
					dto = resultSetToCustomerDTO(rs); /** 先頭13列（顧客）を詰める */
					dto.setCustomerContacts(new ArrayList<>()); /** 空で初期化 */
					map.put(cid, dto);
				}

				/** 14列目: cc.id が非NULLなら担当者1件を追加 */
				UUID contactId = rs.getObject(14, UUID.class);
				if (contactId != null) {
					CustomerContactDTO contact = resultSetToCustomerContactDTO(rs, 14);
					dto.getCustomerContacts().add(contact);
				}
			}
		} catch (SQLException e) {
			/** テーブル名の表記も顧客系に合わせる */
			String errorMsg = "E:C12 CustomersテーブルのSELECT処理中にエラーが発生しました。";
			throw new DAOException(errorMsg, e);
		}
		return new ArrayList<>(map.values());
	}

	/**
	 * 指定IDの顧客（削除されていない）を1件取得します。
	 * 担当者は取得しません（必要に応じて {@link #selectWithContactsByUuid(UUID)} を使用）。
	 *
	 * @param id 顧客ID（UUID）
	 * @return 該当があれば CustomerDTO、該当なしは {@code null}
	 * @throws DAOException DBアクセス時にエラーが発生した場合
	 */
	public CustomerDTO selectByUUId(UUID id) {
		CustomerDTO dto;

		final String sql = SQL_SELECT_BASIC
				+ " WHERE c.deleted_at IS NULL "
				+ "   AND c.id = ?";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setObject(1, id);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					dto = resultSetToCustomerDTO(rs);
					return dto;
				}
				return null;
			}
		} catch (SQLException e) {
			String errorMsg = "E:C12 Customers 単一取得中にエラーが発生しました。";
			throw new DAOException(errorMsg, e);
		}
	}

	/**
     * 指定IDの顧客を1件取得し、同一顧客の担当者一覧（未削除）も同時に取得します。
     * 主担当は {@code primary_contact_id} と一致するレコードに {@link CustomerContactDTO#setPrimary(boolean)} を立てます。
     *
     * @param customerId 顧客ID（UUID）
     * @return 顧客DTO（担当者リスト込み）。該当なしは {@code null}
     * @throws DAOException DBアクセスに失敗した場合
     */
    public CustomerDTO selectWithContactsByUuid(UUID customerId) {
          

        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_WITH_CONTACTS)) {
            ps.setObject(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                CustomerDTO customer = null;
                List<CustomerContactDTO> contacts = new ArrayList<>();

                while (rs.next()) {
                    if (customer == null) {
                        /** 先頭13列（customers）を詰めるヘルパーを再利用 */
                        customer = resultSetToCustomerDTO(rs);
                        customer.setCustomerContacts(contacts); /** 空でもセット */
                    }
                    /** 14列目（cc.id）があれば担当者1件を追加 */
                    UUID ccId = rs.getObject(14, UUID.class);
                    if (ccId != null) {
                        CustomerContactDTO cc = resultSetToCustomerContactDTO(rs, 14);
                        UUID primaryId = customer.getPrimaryContactId();
                        cc.setPrimary(primaryId != null && primaryId.equals(cc.getId())); /** 主担当フラグ */
                        contacts.add(cc);
                    }
                }
                return customer; /** 見つからなければ null のまま */
            }
        } catch (SQLException e) {
            throw new DAOException("E:C12 Customers（含:担当者一覧）単一取得中にエラーが発生しました。", e);
        }
    }

	/**
	 * 指定の年月（{@code yyyy-MM}）に該当する assignments を顧客に結合して取得します。
	 *
	 * 挙動：
	 * - 削除済み顧客は除外
	 * - 削除済みアサインは除外（{@code a.deleted_at IS NULL}）
	 * - 対象年月は {@code assignments.target_year_month = ?} で一致
	 *
	 * @param yearMonth 例: "2025-08"
	 * @return 顧客ごとに assignments を内包したDTOリスト（顧客に紐づく {@link CustomerDTO#getAssignmentDTOs()} を構成）
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

					/** customers (13) */
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
						/** 既に customers は詰め済み：company_code..deleted_at の12列を読み飛ばす */
						i += 12;
					}

					/** assignments (15) */
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

						/** task_rank (1) */
						ad.setTaskRankName(rs.getString(i++));

						/** secretaries (4) */
						ad.setSecretaryId(rs.getObject(i++, UUID.class));
						ad.setSecretaryName(rs.getString(i++));
						ad.setSecretaryRankId(rs.getObject(i++, UUID.class));
						ad.setIsPmSecretary(rs.getBoolean(i++));

						/** secretary_rank (1) */
						ad.setSecretaryRankName(rs.getString(i++));

						customerMap.get(cId).getAssignmentDTOs().add(ad);
					} else {
						/** a.id = null → task_rank(1) + secretaries(4) + secretary_rank(1) を読み飛ばし */
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

	/**
	 * 最近登録された顧客を10件取得します。
	 * 取得カラムは、id / company_code / company_name / mail / phone / created_at。
	 *
	 * @return 表示用マップのリスト（LinkedHashMap）。キーは
	 *         - {@code id}（UUID）
	 *         - {@code companyCode}（String）
	 *         - {@code companyName}（String）
	 *         - {@code mail}（String）
	 *         - {@code phone}（String）
	 *         - {@code createdAt}（java.sql.Timestamp）
	 * @throws DAOException 取得に失敗した場合
	 */
	public List<Map<String, Object>> selectRecent10() {
	    List<Map<String, Object>> list = new ArrayList<>();
	    try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_RECENT10_CUSTOMERS)) {
	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                Map<String, Object> m = new LinkedHashMap<>();
	                m.put("id",          rs.getObject("id", UUID.class));
	                m.put("companyCode", rs.getString("company_code"));
	                m.put("companyName", rs.getString("company_name"));
	                m.put("mail",        rs.getString("mail"));
	                m.put("phone",       rs.getString("phone"));
	                m.put("createdAt",   rs.getTimestamp("created_at"));
	                list.add(m);
	            }
	        }
	    } catch (SQLException e) {
	        throw new DAOException("E:CUS-R10 最近登録された顧客の取得に失敗しました。", e);
	    }
	    return list;
	}

	/** =========================
	 * 3-2. INSERT
	 * ========================= */

	/**
	 * 顧客を新規登録します。
	 *
	 * 仕様：
	 * - {@code id} は DB 側で {@code gen_random_uuid()} を採番
	 * - {@code primary_contact_id} は NULL 初期化
	 * - {@code created_at}/{@code updated_at} は {@code CURRENT_TIMESTAMP}
	 *
	 * @param dto 登録する顧客DTO（company_code, company_name, mail, phone, postal_code, address1, address2, building を使用）
	 * @return 影響行数（通常は1）
	 * @throws DAOException INSERT に失敗した場合
	 */
    
    public List<AssignmentDTO> selectAssignmentsForCustomerInMonths(UUID customerId, String ym1, String ym2) {
        List<AssignmentDTO> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_TWO_MONTH_ASSIGNMENT)) {
            ps.setObject(1, customerId);
            ps.setString(2, ym1);
            ps.setString(3, ym2);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int i = 1;
                    AssignmentDTO ad = new AssignmentDTO();
                    /** assignments */
                    ad.setAssignmentId(rs.getObject(i++, UUID.class));               /** a.id */
                    ad.setAssignmentCustomerId(rs.getObject(i++, UUID.class));        /** a.customer_id */
                    ad.setAssignmentSecretaryId(rs.getObject(i++, UUID.class));       /** a.secretary_id */
                    ad.setTaskRankId(rs.getObject(i++, UUID.class));                  /** a.task_rank_id */
                    ad.setTargetYearMonth(rs.getString(i++));                         /** a.target_year_month */
                    ad.setBasePayCustomer(rs.getBigDecimal(i++));                     /** a.base_pay_customer */
                    ad.setBasePaySecretary(rs.getBigDecimal(i++));                    /** a.base_pay_secretary */
                    ad.setIncreaseBasePayCustomer(rs.getBigDecimal(i++));             /** a.increase_base_pay_customer */
                    ad.setIncreaseBasePaySecretary(rs.getBigDecimal(i++));            /** a.increase_base_pay_secretary */
                    ad.setCustomerBasedIncentiveForCustomer(rs.getBigDecimal(i++));   /** a.customer_based_incentive_for_customer */
                    ad.setCustomerBasedIncentiveForSecretary(rs.getBigDecimal(i++));  /** a.customer_based_incentive_for_secretary */
                    /** （status 削除済みのため読み取りなし） */
                    ad.setAssignmentCreatedAt(rs.getTimestamp(i++));                  /** a.created_at */
                    ad.setAssignmentUpdatedAt(rs.getTimestamp(i++));                  /** a.updated_at */
                    ad.setAssignmentDeletedAt(rs.getTimestamp(i++));                  /** a.deleted_at */

                    /** task_rank */
                    ad.setTaskRankName(rs.getString(i++));                            /** tr.rank_name */

                    /** secretaries */
                    ad.setSecretaryId(rs.getObject(i++, UUID.class));                 /** s.id */
                    ad.setSecretaryName(rs.getString(i++));                           /** s.name */
                    ad.setSecretaryRankId(rs.getObject(i++, UUID.class));             /** s.secretary_rank_id */
                    ad.setIsPmSecretary(rs.getBoolean(i++));                          /** s.is_pm_secretary */

                    /** secretary_rank name */
                    ad.setSecretaryRankName(rs.getString(i++));                       /** sr.rank_name */

                    list.add(ad);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("E:C44 直近2か月のアサイン取得に失敗しました。", e);
        }

        return list;
    }
    
  

    
    /** =======================
     * 登録
     * ======================= */
    
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

	/** =========================
	 * 3-3. UPDATE
	 * ========================= */

	/**
	 * 顧客の基本情報を更新します（{@code updated_at} は DB 側で現在時刻に更新）。
	 *
	 * @param dto 更新対象（{@code id} 必須）。その他 company_code / company_name / mail / phone / postal_code / address1 / address2 / building を利用
	 * @return 影響行数（通常は1）
	 * @throws DAOException UPDATE に失敗した場合
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

	/** =========================
	 * 3-4. DELETE（論理削除）
	 * ========================= */

	/**
	 * 顧客の論理削除を行います（{@code deleted_at} を現在時刻で更新）。
	 *
	 * @param id 顧客ID（UUID）
	 * @throws DAOException UPDATE に失敗した場合
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

	/** =========================
	 * 3-5. 重複チェック
	 * ========================= */

	/**
	 * company_code の重複をチェックします。
	 *
	 * @param companyCode 会社コード
	 * @return 既に存在すれば {@code true}（deleted_at IS NULL のレコードが対象）
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
	 * company_code の重複をチェックします（自IDを除外）。更新時に使用してください。
	 *
	 * @param companyCode 会社コード
	 * @param excludeId   除外するID（自身のID）
	 * @return 既に存在すれば {@code true}（deleted_at IS NULL のレコードが対象）
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

	/** =========================
	 * 3-6. ResultSet -> DTO 変換（private）
	 * ========================= */

	/**
	 * 先頭列（c.id）から13列分を {@link CustomerDTO} に詰め替えます。
	 *
	 * 列対応：
	 * 1. c.id
	 * 2. c.company_code
	 * 3. c.company_name
	 * 4. c.mail
	 * 5. c.phone
	 * 6. c.postal_code
	 * 7. c.address1
	 * 8. c.address2
	 * 9. c.building
	 * 10. c.primary_contact_id
	 * 11. c.created_at
	 * 12. c.updated_at
	 * 13. c.deleted_at
	 *
	 * @param rs クエリ結果のカーソル
	 * @return CustomerDTO（13列ぶんを設定）
	 * @throws DAOException 変換処理中にエラーが発生した場合
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
		} catch (SQLException e) {
			String errorMsg = "E:C17 ResultSet→CustomerDTO 変換中にエラーが発生しました。";
			throw new DAOException(errorMsg, e);
		}
	}

	/**
	 * 指定開始位置（例：14列目＝cc.id）から10列分を {@link CustomerContactDTO} に詰め替えます。
	 *
	 * 列対応（開始カラム= num）：
	 * 1. cc.id
	 * 2. cc.mail
	 * 3. cc.name
	 * 4. cc.name_ruby
	 * 5. cc.phone
	 * 6. cc.department
	 * 7. cc.created_at
	 * 8. cc.updated_at
	 * 9. cc.deleted_at
	 * 10. cc.last_login_at
	 *
	 * @param rs  クエリ結果のカーソル
	 * @param num 開始カラム番号（1始まり）
	 * @return CustomerContactDTO（10列ぶんを設定）
	 * @throws DAOException 変換処理中にエラーが発生した場合
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
		} catch (SQLException e) {
			String errorMsg = "E:C18 ResultSet→CustomerContactDTO 変換中にエラーが発生しました。";
			throw new DAOException(errorMsg, e);
		}
	}
}
