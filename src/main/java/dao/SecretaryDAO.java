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

import dto.SecretaryDTO;
import dto.SecretaryRankDTO;

/**
 * 秘書（secretaries）および秘書ランク（secretary_rank）に関するデータアクセスを担うDAO。
 *
 * <p>責務：</p>
 * <ul>
 *   <li>秘書情報の取得（一覧／単一／メール）</li>
 *   <li>秘書ランクの取得</li>
 *   <li>秘書の登録・更新（口座情報含む）・論理削除</li>
 *   <li>コード／メールの重複チェック</li>
 * </ul>
 *
 * <p>設計メモ：</p>
 * <ul>
 *   <li>本DAOは渡された {@link Connection} に依存（トランザクション境界は呼び出し側が管理）</li>
 *   <li>DB例外は {@link DAOException} にラップして送出</li>
 *   <li>ResultSet→DTO 変換は専用のプライベートメソッドで一元化</li>
 * </ul>
 */
public class SecretaryDAO extends BaseDAO {

	// ========================
	// ① フィールド（SQL 定義）
	// ========================

	/** 秘書の基本情報＋ランク情報を取得する共通SELECT（口座情報なし） */
	private static final String SQL_SELECT_BASIC = "SELECT "
			+ " s.id, s.secretary_code, s.mail, s.password, "
			+ " sr.id, sr.rank_name, sr.description, sr.increase_base_pay_customer, sr.increase_base_pay_secretary, "
			+ " sr.created_at, sr.updated_at, sr.deleted_at, "
			+ " s.is_pm_secretary, s.name, s.name_ruby, s.phone, s.postal_code, "
			+ " s.address1, s.address2, s.building, "
			+ " s.created_at, s.updated_at, s.deleted_at, s.last_login_at "
			+ " FROM secretaries s "
			+ " INNER JOIN secretary_rank sr ON s.secretary_rank_id = sr.id ";

	/** 秘書の基本情報＋ランク情報＋口座情報を取得（口座列追加版） */
	private static final String SQL_SELECT_BASIC_WITH_BANK = "SELECT "
			+ " s.id, s.secretary_code, s.mail, s.password, "
			+ " sr.id, sr.rank_name, sr.description, sr.increase_base_pay_customer, sr.increase_base_pay_secretary, "
			+ " sr.created_at, sr.updated_at, sr.deleted_at, "
			+ " s.is_pm_secretary, s.name, s.name_ruby, s.phone, s.postal_code, "
			+ " s.address1, s.address2, s.building, "
			+ " s.created_at, s.updated_at, s.deleted_at, s.last_login_at,"
			+ " s.bank_name, s.bank_branch, s.bank_type, s.bank_account, s.bank_owner "
			+ " FROM secretaries s "
			+ " INNER JOIN secretary_rank sr ON s.secretary_rank_id = sr.id ";
	
	/** 最近登録された秘書10件（プロフィール有無付き）を返します。 */
	private static final String SQL_SELECT_RECENT10_WITH_PROFILE =
	    "SELECT s.id, s.secretary_code, s.name, s.mail, s.phone, " +
	    "       sr.rank_name, " +
	    "       CASE WHEN p.id IS NULL THEN FALSE ELSE TRUE END AS has_profile, " +
	    "       s.created_at " +
	    "  FROM secretaries s " +
	    "  LEFT JOIN secretary_rank sr ON sr.id = s.secretary_rank_id " +
	    // ※テーブル名が projects などではなく「profile」想定。異なる場合はここを調整してください。
	    "  LEFT JOIN profiles p ON p.secretary_id = s.id AND p.deleted_at IS NULL " +
	    " WHERE s.deleted_at IS NULL " +
	    " ORDER BY s.created_at DESC " +
	    " LIMIT 10";

	/** 口座列を含む単一取得専用のSELECT（上と同じ構成） */
	private static final String SQL_SELECT_BASIC_INCLUDE_ACCOUNT = SQL_SELECT_BASIC_WITH_BANK;

	/** 新規INSERT（idはDB側で生成、タイムスタンプはCURRENT_TIMESTAMP） */
	private static final String SQL_INSERT = "INSERT INTO secretaries ("
			+ " id, secretary_code, mail, password, secretary_rank_id, "
			+ " is_pm_secretary, name, name_ruby, phone, postal_code, "
			+ " address1, address2, building, created_at, updated_at"
			+ " ) VALUES ("
			+ " gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

	/** 管理画面での基本更新（口座情報は含まない） */
	private static final String SQL_UPDATE_BASIC = "UPDATE secretaries "
			+ "   SET secretary_code = ?, mail = ?, secretary_rank_id = ?, is_pm_secretary = ?, "
			+ "       name = ?, name_ruby = ?, phone = ?, "
			+ "       postal_code = ?, address1 = ?, address2 = ?, building = ?, "
			+ "       updated_at = CURRENT_TIMESTAMP "
			+ " WHERE id = ?";

	/** 秘書本人用の更新（口座情報込み） */
	private static final String SQL_UPDATE_BASIC_SECRETARY = "UPDATE secretaries "
			+ "   SET secretary_code = ?, mail = ?, is_pm_secretary = ?, "
			+ "       name = ?, name_ruby = ?, phone = ?, "
			+ "       postal_code = ?, address1 = ?, address2 = ?, building = ?, "
			+ "       bank_name = ?, bank_branch = ?, bank_type = ?, bank_account = ?, bank_owner = ?, "
			+ "       updated_at = CURRENT_TIMESTAMP "
			+ " WHERE id = ?";

	/** 論理削除（deleted_at を現在時刻に） */
	private static final String SQL_DELETE_LOGICAL = "UPDATE secretaries SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?";

	/** 重複チェック（コード／メール） */
	private static final String SQL_COUNT_BY_CODE = "SELECT COUNT(*) FROM secretaries WHERE secretary_code = ?";
	private static final String SQL_COUNT_BY_MAIL = "SELECT COUNT(*) FROM secretaries WHERE mail = ?";

	/** ランク全件取得 */
	private static final String SQL_SELECT_RANK_ALL = "SELECT id, rank_name, description, "
			+ "       increase_base_pay_customer, increase_base_pay_secretary, "
			+ "       created_at, updated_at, deleted_at "
			+ "  FROM secretary_rank";

	/** 自ID除外つき重複チェック（更新時用） */
	private static final String SQL_COUNT_BY_CODE_EXCEPT_ID = "SELECT COUNT(*) FROM secretaries WHERE secretary_code = ? AND id <> ?";
	private static final String SQL_COUNT_BY_MAIL_EXCEPT_ID = "SELECT COUNT(*) FROM secretaries WHERE mail = ? AND id <> ?";

	/** 最終ログイン時刻の更新 */
	private static final String SQL_UPDATE_LAST_LOGIN_AT = "UPDATE secretaries SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?";

	// ========================
	// ② フィールド／コンストラクタ
	// ========================

	/**
	 * コンストラクタ。
	 *
	 * @param conn 呼び出し側が管理するDBコネクション（トランザクション境界も呼び出し側）
	 */
	public SecretaryDAO(Connection conn) {
		super(conn);
	}

	// ========================
	// ③ メソッド群
	// ========================

	// ------------------------
	// SELECT
	// ------------------------

	/**
	 * 秘書の一覧（削除されていない行）を、ランク情報とともに取得します。
	 * <p>口座情報は含みません。</p>
	 *
	 * @return {@link SecretaryDTO} のリスト（0件なら空）
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public List<SecretaryDTO> selectAll() {
		final String sql = SQL_SELECT_BASIC + " WHERE s.deleted_at IS NULL";
		try (PreparedStatement ps = conn.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			List<SecretaryDTO> dtos = new ArrayList<>();
			while (rs.next()) {
				dtos.add(resultSetToSecretaryDTO(rs));
			}
			return dtos;
		} catch (SQLException e) {
			throw new DAOException("E:S11 secretaries 全件取得に失敗しました。", e);
		}
	}

	/**
	 * PM対応可能（{@code is_pm_secretary = TRUE}）な秘書のみを取得します。
	 * <p>削除済みは除外、口座情報は含みません。</p>
	 *
	 * @return {@link SecretaryDTO} のリスト（0件なら空）
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public List<SecretaryDTO> selectAllPM() {
		final String sql = SQL_SELECT_BASIC
				+ " WHERE s.deleted_at IS NULL AND s.is_pm_secretary = TRUE";
		try (PreparedStatement ps = conn.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			List<SecretaryDTO> dtos = new ArrayList<>();
			while (rs.next()) {
				dtos.add(resultSetToSecretaryDTO(rs));
			}
			return dtos;
		} catch (SQLException e) {
			throw new DAOException("E:S12 PM秘書の取得に失敗しました。", e);
		}
	}

	/**
	 * 主キーで秘書を1件取得します（削除済みは除外／口座情報含む）。
	 *
	 * @param id 秘書ID
	 * @return 該当があれば口座情報込みの {@link SecretaryDTO}、なければ空のDTO
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public SecretaryDTO selectByUUIdIncludeAccount(UUID id) {
		final String sql = SQL_SELECT_BASIC_INCLUDE_ACCOUNT
				+ " WHERE s.deleted_at IS NULL AND s.id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setObject(1, id);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					SecretaryDTO dto = resultSetToSecretaryDTO(rs); // 基本＋ランク
					// 追加の口座列（25..29列目）を詰める
					dto.setBankName(rs.getString(25));
					dto.setBankBranch(rs.getString(26));
					dto.setBankType(rs.getString(27));
					dto.setBankAccount(rs.getString(28));
					dto.setBankOwner(rs.getString(29));
					return dto;
				}
				return new SecretaryDTO();
			}
		} catch (SQLException e) {
			throw new DAOException("E:S13 秘書ID（口座込み）単一取得に失敗しました。", e);
		}
	}

	/**
	 * 主キーで秘書を1件取得します（削除済みは除外／口座情報なし）。
	 *
	 * @param id 秘書ID
	 * @return 該当があれば {@link SecretaryDTO}、なければ空のDTO
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public SecretaryDTO selectByUUId(UUID id) {
		final String sql = SQL_SELECT_BASIC
				+ " WHERE s.deleted_at IS NULL AND s.id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setObject(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return resultSetToSecretaryDTO(rs);
				return new SecretaryDTO();
			}
		} catch (SQLException e) {
			throw new DAOException("E:S13 秘書IDによる単一取得に失敗しました。", e);
		}
	}

	/**
	 * 主キーで秘書を1件取得します（削除済みは除外／口座情報も取得）。
	 *
	 * @param id 秘書ID
	 * @return 該当があれば口座情報込みの {@link SecretaryDTO}、なければ空のDTO
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public SecretaryDTO selectByUUIdWithBank(UUID id) {
		final String sql = SQL_SELECT_BASIC_WITH_BANK
				+ " WHERE s.deleted_at IS NULL AND s.id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setObject(1, id);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					SecretaryDTO dto = resultSetToSecretaryDTO(rs);
					// 25..29列：口座情報
					resultSetToSecretaryDTOWithBank(rs, dto);
					return dto;
				}
				return new SecretaryDTO();
			}
		} catch (SQLException e) {
			throw new DAOException("E:S13 秘書ID（口座込み）単一取得に失敗しました。", e);
		}
	}

	/**
	 * メールアドレスで秘書を1件取得します（削除済みは除外）。
	 *
	 * @param mail メールアドレス（ユニーク想定）
	 * @return 該当があれば {@link SecretaryDTO}、なければ空のDTO
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public SecretaryDTO selectByMail(String mail) {
		final String sql = SQL_SELECT_BASIC
				+ " WHERE s.deleted_at IS NULL AND s.mail = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, mail);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return resultSetToSecretaryDTO(rs);
				return new SecretaryDTO();
			}
		} catch (SQLException e) {
			throw new DAOException("E:S14 メールによる単一取得に失敗しました。", e);
		}
	}

	/**
	 * 秘書ランクを全件取得します。
	 *
	 * @return {@link SecretaryRankDTO} のリスト（0件なら空）
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public List<SecretaryRankDTO> selectRankAll() {
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_RANK_ALL);
				ResultSet rs = ps.executeQuery()) {

			List<SecretaryRankDTO> dtos = new ArrayList<>();
			while (rs.next()) {
				dtos.add(resultSetToSecretaryRankDTO(rs, 1));
			}
			return dtos;
		} catch (SQLException e) {
			throw new DAOException("E:S21 secretary_rank 全件取得に失敗しました。", e);
		}
	}
	
	/**
	 * 最近登録された秘書を10件取得します。<br>
	 * 取得カラムは、id / secretary_code / name / rank_name / mail / phone / has_profile / created_at。<br>
	 * rank_name は {@code secretary_rank} から、プロフィール有無は {@code profile} の存在で判定します。
	 *
	 * @return 表示用マップのリスト（LinkedHashMap）。キーは
	 *         <ul>
	 *           <li>{@code id}（UUID）</li>
	 *           <li>{@code secretaryCode}（String）</li>
	 *           <li>{@code name}（String）</li>
	 *           <li>{@code rankName}（String）</li>
	 *           <li>{@code mail}（String）</li>
	 *           <li>{@code phone}（String）</li>
	 *           <li>{@code hasProfile}（Boolean）</li>
	 *           <li>{@code createdAt}（java.sql.Timestamp）</li>
	 *         </ul>
	 * @throws DAOException 取得に失敗した場合
	 */
	public List<Map<String, Object>> selectRecent10WithProfileFlag() {
	    List<Map<String, Object>> list = new ArrayList<>();
	    try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_RECENT10_WITH_PROFILE)) {
	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                Map<String, Object> m = new LinkedHashMap<>();
	                m.put("id",           rs.getObject("id", UUID.class));
	                m.put("secretaryCode",rs.getString("secretary_code"));
	                m.put("name",         rs.getString("name"));
	                m.put("rankName",     rs.getString("rank_name"));
	                m.put("mail",         rs.getString("mail"));
	                m.put("phone",        rs.getString("phone"));
	                m.put("hasProfile",   rs.getBoolean("has_profile"));
	                m.put("createdAt",    rs.getTimestamp("created_at"));
	                list.add(m);
	            }
	        }
	    } catch (SQLException e) {
	        throw new DAOException("E:SEC-R10 最近登録された秘書の取得に失敗しました。", e);
	    }
	    return list;
	}

	// ------------------------
	// INSERT
	// ------------------------

	/**
	 * 秘書を新規登録します。
	 * <p>{@code id} はDB側で生成、{@code created_at/updated_at} は現在時刻が入ります。</p>
	 *
	 * @param dto 登録対象（code/mail/password/rankId など必須）
	 * @return 影響行数（通常は1）
	 * @throws DAOException INSERTに失敗した場合
	 */
	public int insert(SecretaryDTO dto) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
			ps.setString(1, dto.getSecretaryCode());
			ps.setString(2, dto.getMail());
			ps.setString(3, dto.getPassword());
			ps.setObject(4, dto.getSecretaryRankId()); // UUID
			ps.setBoolean(5, dto.isPmSecretary());
			ps.setString(6, dto.getName());
			ps.setString(7, dto.getNameRuby());
			ps.setString(8, dto.getPhone());
			ps.setString(9, dto.getPostalCode());
			ps.setString(10, dto.getAddress1());
			ps.setString(11, dto.getAddress2());
			ps.setString(12, dto.getBuilding());
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:S31 secretaries INSERT に失敗しました。", e);
		}
	}

	// ------------------------
	// UPDATE
	// ------------------------

	/**
	 * 秘書の基本情報を更新します（口座情報は含みません）。
	 *
	 * @param dto 更新対象（{@code id} 必須、rank更新時は {@code secretaryRankId} 必須）
	 * @return 影響行数（通常は1）
	 * @throws DAOException UPDATEに失敗した場合
	 */
	public int update(SecretaryDTO dto) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_BASIC)) {
			ps.setString(1, dto.getSecretaryCode());
			ps.setString(2, dto.getMail());
			ps.setObject(3, dto.getSecretaryRankId()); // UUID
			ps.setBoolean(4, dto.isPmSecretary());
			ps.setString(5, dto.getName());
			ps.setString(6, dto.getNameRuby());
			ps.setString(7, dto.getPhone());
			ps.setString(8, dto.getPostalCode());
			ps.setString(9, dto.getAddress1());
			ps.setString(10, dto.getAddress2());
			ps.setString(11, dto.getBuilding());
			ps.setObject(12, dto.getId());
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:S32 secretaries UPDATE に失敗しました。", e);
		}
	}

	/**
	 * 秘書の基本情報＋口座情報を更新します（本人向け編集を想定）。
	 *
	 * @param dto 更新対象（{@code id} 必須。口座情報：bankName/branch/type/account/owner）
	 * @return 影響行数（通常は1）
	 * @throws DAOException UPDATEに失敗した場合
	 */
	public int updateWithBank(SecretaryDTO dto) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_BASIC_SECRETARY)) {
			int i = 1;
			ps.setString(i++, dto.getSecretaryCode());
			ps.setString(i++, dto.getMail());
			ps.setBoolean(i++, dto.isPmSecretary());
			ps.setString(i++, dto.getName());
			ps.setString(i++, dto.getNameRuby());
			ps.setString(i++, dto.getPhone());
			ps.setString(i++, dto.getPostalCode());
			ps.setString(i++, dto.getAddress1());
			ps.setString(i++, dto.getAddress2());
			ps.setString(i++, dto.getBuilding());
			// 口座情報
			ps.setString(i++, dto.getBankName());
			ps.setString(i++, dto.getBankBranch());
			ps.setString(i++, dto.getBankType());
			ps.setString(i++, dto.getBankAccount());
			ps.setString(i++, dto.getBankOwner());
			ps.setObject(i++, dto.getId());
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:S32 secretaries UPDATE（口座込み）に失敗しました。", e);
		}
	}

	// ------------------------
	// DELETE（論理）
	// ------------------------

	/**
	 * 秘書を論理削除します（{@code deleted_at = CURRENT_TIMESTAMP}）。
	 *
	 * @param id 秘書ID
	 * @throws DAOException UPDATEに失敗した場合
	 */
	public void delete(UUID id) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_LOGICAL)) {
			ps.setObject(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:S33 secretaries 論理DELETE に失敗しました。", e);
		}
	}

	// ------------------------
	// 重複チェック
	// ------------------------

	/**
	 * メールアドレスの重複をチェックします。
	 *
	 * @param mail メールアドレス
	 * @return 既に存在すれば {@code true}
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public boolean mailCheck(String mail) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_BY_MAIL)) {
			ps.setString(1, mail);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() && rs.getInt(1) >= 1;
			}
		} catch (SQLException e) {
			throw new DAOException("E:S41 secretaries.mail 重複チェックに失敗しました。", e);
		}
	}

	/**
	 * 秘書コードの重複をチェックします。
	 *
	 * @param code 秘書コード
	 * @return 既に存在すれば {@code true}
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public boolean secretaryCodeCheck(String code) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_BY_CODE)) {
			ps.setString(1, code);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() && rs.getInt(1) >= 1;
			}
		} catch (SQLException e) {
			throw new DAOException("E:S42 secretaries.secretary_code 重複チェックに失敗しました。", e);
		}
	}

	/**
	 * 自IDを除外して秘書コードの重複をチェックします（更新時に使用）。
	 * <p>例：同じ {@code secretary_code} を持つ他レコードが存在するか。</p>
	 *
	 * @param code 秘書コード
	 * @param excludeId 除外する自レコードID
	 * @return 自分以外に同コードが存在すれば {@code true}
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public boolean secretaryCodeCheckExceptId(String code, UUID excludeId) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_BY_CODE_EXCEPT_ID)) {
			ps.setString(1, code);
			ps.setObject(2, excludeId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() && rs.getInt(1) >= 1;
			}
		} catch (SQLException e) {
			throw new DAOException("E:S43 secretary_code（除外付き）重複チェックに失敗しました。", e);
		}
	}

	/**
	 * 自IDを除外してメールの重複をチェックします（更新時に使用）。
	 *
	 * @param mail メールアドレス
	 * @param excludeId 除外する自レコードID
	 * @return 自分以外に同メールが存在すれば {@code true}
	 * @throws DAOException DBアクセスに失敗した場合
	 */
	public boolean mailCheckExceptId(String mail, UUID excludeId) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_BY_MAIL_EXCEPT_ID)) {
			ps.setString(1, mail);
			ps.setObject(2, excludeId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() && rs.getInt(1) >= 1;
			}
		} catch (SQLException e) {
			throw new DAOException("E:S44 mail（除外付き）重複チェックに失敗しました。", e);
		}
	}

	// ------------------------
	// ResultSet -> DTO 変換（private）
	// ------------------------

	/**
	 * {@code SQL_SELECT_BASIC} のカラム並びに基づき、{@link SecretaryDTO} を構築します。
	 * <p>カラム構成（1-origin）: 1..4: s.*, 5..12: sr.*, 13..24: s.*（続き）</p>
	 *
	 * @param rs クエリ結果
	 * @return 変換済み {@link SecretaryDTO}
	 * @throws DAOException 変換に失敗した場合
	 */
	private SecretaryDTO resultSetToSecretaryDTO(ResultSet rs) {
		try {
			SecretaryDTO dto = new SecretaryDTO();
			dto.setId(rs.getObject(1, UUID.class));
			dto.setSecretaryCode(rs.getString(2));
			dto.setMail(rs.getString(3));
			dto.setPassword(rs.getString(4));
			dto.setSecretaryRankDTO(resultSetToSecretaryRankDTO(rs, 5)); // 5..12
			dto.setPmSecretary(rs.getBoolean(13));
			dto.setName(rs.getString(14));
			dto.setNameRuby(rs.getString(15));
			dto.setPhone(rs.getString(16));
			dto.setPostalCode(rs.getString(17));
			dto.setAddress1(rs.getString(18));
			dto.setAddress2(rs.getString(19));
			dto.setBuilding(rs.getString(20));
			dto.setCreatedAt(rs.getTimestamp(21));
			dto.setUpdatedAt(rs.getTimestamp(22));
			dto.setDeletedAt(rs.getTimestamp(23));
			dto.setLastLoginAt(rs.getTimestamp(24));
			return dto;
		} catch (SQLException e) {
			throw new DAOException("E:S51 ResultSet→SecretaryDTO 変換中に失敗しました。", e);
		}
	}

	/**
	 * {@code SQL_SELECT_BASIC_WITH_BANK} の後段（25..29列）から口座情報を {@link SecretaryDTO} に流し込みます。
	 *
	 * @param rs   クエリ結果
	 * @param dto  すでに基本情報が詰まった {@link SecretaryDTO}
	 * @throws DAOException 変換に失敗した場合
	 */
	private void resultSetToSecretaryDTOWithBank(ResultSet rs, SecretaryDTO dto) {
		try {
			dto.setBankName(rs.getString(25));
			dto.setBankBranch(rs.getString(26));
			dto.setBankType(rs.getString(27));
			dto.setBankAccount(rs.getString(28));
			dto.setBankOwner(rs.getString(29));
		} catch (SQLException e) {
			throw new DAOException("E:S52 ResultSet→SecretaryDTO(口座) 変換中に失敗しました。", e);
		}
	}

	/**
	 * {@code secretary_rank} テーブルの列群を {@link SecretaryRankDTO} に詰めます。
	 *
	 * @param rs  クエリ結果
	 * @param num 開始カラム（1-origin）
	 * @return 変換済み {@link SecretaryRankDTO}
	 * @throws DAOException 変換に失敗した場合
	 */
	private SecretaryRankDTO resultSetToSecretaryRankDTO(ResultSet rs, int num) {
		try {
			SecretaryRankDTO dto = new SecretaryRankDTO();
			dto.setId(rs.getObject(num++, UUID.class));
			dto.setRankName(rs.getString(num++));
			dto.setDescription(rs.getString(num++));
			dto.setIncreaseBasePayCustomer(rs.getBigDecimal(num++));
			dto.setIncreaseBasePaySecretary(rs.getBigDecimal(num++));
			dto.setCreatedAt(rs.getTimestamp(num++));
			dto.setUpdatedAt(rs.getTimestamp(num++));
			dto.setDeletedAt(rs.getTimestamp(num++));
			return dto;
		} catch (SQLException e) {
			throw new DAOException("E:S52 ResultSet→SecretaryRankDTO 変換中に失敗しました。", e);
		}
	}

	/**
	 * 最終ログイン時刻を現在時刻で更新します。
	 *
	 * @param id 秘書ID
	 * @throws DAOException 更新に失敗した場合
	 */
	public void updateLastLoginAt(UUID id) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_LAST_LOGIN_AT)) {
			ps.setObject(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:S60 secretaries.last_login_at 更新に失敗しました。", e);
		}
	}
}
