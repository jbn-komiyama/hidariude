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

	/** ========================
	 * SQL 定義
	 * ======================== */

	/** 指定月の customers + assignments + task_rank + secretaries + secretary_rank をまとめて取得 */
	private static final String SQL_SELECT_BY_MONTH = "WITH eff AS ( "
			+ "  SELECT "
			+ "    CASE "
			+ "      WHEN ? > to_char(date_trunc('month', current_date) + interval '1 month', 'YYYY-MM') "
			+ "      THEN to_char(date_trunc('month', current_date) + interval '1 month', 'YYYY-MM') "
			+ "      ELSE ? "
			+ "    END AS ym "
			+ "), "
			+ "a_target AS ( "
			+ "  SELECT a.* "
			+ "  FROM assignments a, eff "
			+ "  WHERE a.deleted_at IS NULL "
			+ "    AND a.target_year_month = eff.ym "
			+ "), "
			+ "rank_months AS ( /* 顧客×秘書×ランク×年月をユニーク化（<= eff.ym）*/ "
			+ "  SELECT DISTINCT a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month "
			+ "  FROM assignments a, eff "
			+ "  WHERE a.deleted_at IS NULL "
			+ "    AND a.target_year_month <= eff.ym "
			+ "), "
			+ "streak AS (  /* 顧客×秘書×ランクの“連続月”島 */ "
			+ "  SELECT "
			+ "    rm.customer_id, "
			+ "    rm.secretary_id, "
			+ "    rm.task_rank_id, "
			+ "    rm.target_year_month, "
			+ "    ( "
			+ "      (extract(YEAR  FROM to_date(rm.target_year_month || '-01','YYYY-MM-DD'))::int * 12) + "
			+ "       extract(MONTH FROM to_date(rm.target_year_month || '-01','YYYY-MM-DD'))::int "
			+ "    ) - row_number() OVER ( "
			+ "          PARTITION BY rm.customer_id, rm.secretary_id, rm.task_rank_id "
			+ "          ORDER BY rm.target_year_month "
			+ "        ) AS grp "
			+ "  FROM rank_months rm "
			+ "), "
			+ "streak_count AS ( /* 末尾=eff.ym の島の長さ=当月の継続月数 */ "
			+ "  SELECT "
			+ "    s.customer_id, "
			+ "    s.secretary_id, "
			+ "    s.task_rank_id, "
			+ "    count(*) AS cont_months "
			+ "  FROM streak s "
			+ "  JOIN a_target t "
			+ "    ON t.customer_id = s.customer_id "
			+ "   AND t.secretary_id = s.secretary_id "
			+ "   AND t.task_rank_id = s.task_rank_id "
			+ "  GROUP BY s.customer_id, s.secretary_id, s.task_rank_id, s.grp "
			+ "  HAVING max(s.target_year_month) = (SELECT ym FROM eff) "
			+ ") "
			+ "SELECT "
			+ "  /* customers (13) */ "
			+ "  c.id, "
			+ "  c.company_code, "
			+ "  c.company_name, "
			+ "  c.mail, "
			+ "  c.phone, "
			+ "  c.postal_code, "
			+ "  c.address1, "
			+ "  c.address2, "
			+ "  c.building, "
			+ "  c.primary_contact_id, "
			+ "  c.created_at, "
			+ "  c.updated_at, "
			+ "  c.deleted_at, "
			+ "  /* assignments (15)（当月分のみ） */ "
			+ "  a.id, "
			+ "  a.customer_id, "
			+ "  a.secretary_id, "
			+ "  a.task_rank_id, "
			+ "  a.target_year_month, "
			+ "  a.base_pay_customer, "
			+ "  a.base_pay_secretary, "
			+ "  a.increase_base_pay_customer, "
			+ "  a.increase_base_pay_secretary, "
			+ "  a.customer_based_incentive_for_customer, "
			+ "  a.customer_based_incentive_for_secretary, "
			+ "  a.status, "
			+ "  a.created_at, "
			+ "  a.updated_at, "
			+ "  a.deleted_at, "
			+ "  /* task_rank */ "
			+ "  tr.rank_name, "
			+ "  tr.rank_no, "
			+ "  /* secretaries */ "
			+ "  s.id AS s_id, "
			+ "  s.secretary_rank_id, "
			+ "  s.name AS s_name, "
			+ "  /* secretary_rank */ "
			+ "  sr.rank_name AS sr_rank_name, "
			+ "  /* 継続月数（当月にアサインがある行のみ） */ "
			+ "  sc.cont_months "
			+ "FROM customers c "
			+ "LEFT JOIN a_target a "
			+ "  ON a.customer_id = c.id "
			+ "LEFT JOIN streak_count sc "
			+ "  ON sc.customer_id = a.customer_id "
			+ " AND sc.secretary_id = a.secretary_id "
			+ " AND sc.task_rank_id = a.task_rank_id "
			+ "LEFT JOIN task_rank tr "
			+ "  ON tr.id = a.task_rank_id "
			+ "LEFT JOIN secretaries s "
			+ "  ON s.id = a.secretary_id "
			+ " AND s.deleted_at IS NULL "
			+ "LEFT JOIN secretary_rank sr "
			+ "  ON sr.id = s.secretary_rank_id "
			+ "WHERE c.deleted_at IS NULL "
			+ "ORDER BY "
			+ "  c.company_name, "
			+ "  s.name NULLS LAST, "
			+ "  tr.rank_no NULLS LAST, "
			+ "  a.created_at NULLS LAST";

	/** 指定月の一覧（フィルタ付き） */
	private static final String SQL_SELECT_BY_MONTH_FILTERED = "WITH eff AS ( "
			+ "  SELECT "
			+ "    CASE "
			+ "      WHEN ? > to_char(date_trunc('month', current_date) + interval '1 month', 'YYYY-MM') "
			+ "      THEN to_char(date_trunc('month', current_date) + interval '1 month', 'YYYY-MM') "
			+ "      ELSE ? "
			+ "    END AS ym "
			+ "), "
			+ "a_target AS ( "
			+ "  SELECT a.* "
			+ "  FROM assignments a, eff "
			+ "  WHERE a.deleted_at IS NULL "
			+ "    AND a.target_year_month = eff.ym "
			+ "), "
			+ "rank_months AS ( "
			+ "  SELECT DISTINCT a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month "
			+ "  FROM assignments a, eff "
			+ "  WHERE a.deleted_at IS NULL "
			+ "    AND a.target_year_month <= eff.ym "
			+ "), "
			+ "streak AS ( "
			+ "  SELECT "
			+ "    rm.customer_id, "
			+ "    rm.secretary_id, "
			+ "    rm.task_rank_id, "
			+ "    rm.target_year_month, "
			+ "    ( "
			+ "      (extract(YEAR  FROM to_date(rm.target_year_month || '-01','YYYY-MM-DD'))::int * 12) + "
			+ "       extract(MONTH FROM to_date(rm.target_year_month || '-01','YYYY-MM-DD'))::int "
			+ "    ) - row_number() OVER ( "
			+ "          PARTITION BY rm.customer_id, rm.secretary_id, rm.task_rank_id "
			+ "          ORDER BY rm.target_year_month "
			+ "        ) AS grp "
			+ "  FROM rank_months rm "
			+ "), "
			+ "streak_count AS ( "
			+ "  SELECT "
			+ "    s.customer_id, "
			+ "    s.secretary_id, "
			+ "    s.task_rank_id, "
			+ "    count(*) AS cont_months "
			+ "  FROM streak s "
			+ "  JOIN a_target t "
			+ "    ON t.customer_id = s.customer_id "
			+ "   AND t.secretary_id = s.secretary_id "
			+ "   AND t.task_rank_id = s.task_rank_id "
			+ "  GROUP BY s.customer_id, s.secretary_id, s.task_rank_id, s.grp "
			+ "  HAVING max(s.target_year_month) = (SELECT ym FROM eff) "
			+ ") "
			+ "SELECT "
			+ "  c.id, "
			+ "  c.company_code, "
			+ "  c.company_name, "
			+ "  c.mail, "
			+ "  c.phone, "
			+ "  c.postal_code, "
			+ "  c.address1, "
			+ "  c.address2, "
			+ "  c.building, "
			+ "  c.primary_contact_id, "
			+ "  c.created_at, "
			+ "  c.updated_at, "
			+ "  c.deleted_at, "
			+ "  a.id, "
			+ "  a.customer_id, "
			+ "  a.secretary_id, "
			+ "  a.task_rank_id, "
			+ "  a.target_year_month, "
			+ "  a.base_pay_customer, "
			+ "  a.base_pay_secretary, "
			+ "  a.increase_base_pay_customer, "
			+ "  a.increase_base_pay_secretary, "
			+ "  a.customer_based_incentive_for_customer, "
			+ "  a.customer_based_incentive_for_secretary, "
			+ "  a.status, "
			+ "  a.created_at, "
			+ "  a.updated_at, "
			+ "  a.deleted_at, "
			+ "  tr.rank_name, "
			+ "  tr.rank_no, "
			+ "  s.id AS s_id, "
			+ "  s.secretary_rank_id, "
			+ "  s.name AS s_name, "
			+ "  sr.rank_name AS sr_rank_name, "
			+ "  sc.cont_months "
			+ "FROM customers c "
			+ "LEFT JOIN a_target a "
			+ "  ON a.customer_id = c.id "
			+ "LEFT JOIN streak_count sc "
			+ "  ON sc.customer_id = a.customer_id "
			+ " AND sc.secretary_id = a.secretary_id "
			+ " AND sc.task_rank_id = a.task_rank_id "
			+ "LEFT JOIN task_rank tr "
			+ "  ON tr.id = a.task_rank_id "
			+ "LEFT JOIN secretaries s "
			+ "  ON s.id = a.secretary_id "
			+ " AND s.deleted_at IS NULL "
			+ "LEFT JOIN secretary_rank sr "
			+ "  ON sr.id = s.secretary_rank_id "
			+ "WHERE c.deleted_at IS NULL "
			+ "  AND ( ? IS NULL OR c.company_name ILIKE '%' || ? || '%' ) "
			+ "  AND ( ?::uuid IS NULL OR a.secretary_id = ?::uuid ) "
			+ "  AND ( ?::int  IS NULL OR COALESCE(sc.cont_months,0) >= ?::int ) "
			+ "ORDER BY "
			+ "  c.company_name, "
			+ "  s.name NULLS LAST, "
			+ "  tr.rank_no NULLS LAST, "
			+ "  a.created_at NULLS LAST";

	/** 指定月の一覧（継続月数の高い順） */
	private static final String SQL_SELECT_BY_MONTH_ORDERBY_CONT_DESC = "WITH eff AS ( "
			+ "  SELECT "
			+ "    CASE "
			+ "      WHEN ? > to_char(date_trunc('month', current_date) + interval '1 month', 'YYYY-MM') "
			+ "      THEN to_char(date_trunc('month', current_date) + interval '1 month', 'YYYY-MM') "
			+ "      ELSE ? "
			+ "    END AS ym "
			+ "), "
			+ "a_target AS ( "
			+ "  SELECT a.* "
			+ "  FROM assignments a, eff "
			+ "  WHERE a.deleted_at IS NULL "
			+ "    AND a.target_year_month = eff.ym "
			+ "), "
			+ "rank_months AS ( "
			+ "  SELECT DISTINCT a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month "
			+ "  FROM assignments a, eff "
			+ "  WHERE a.deleted_at IS NULL "
			+ "    AND a.target_year_month <= eff.ym "
			+ "), "
			+ "streak AS ( "
			+ "  SELECT "
			+ "    rm.customer_id, "
			+ "    rm.secretary_id, "
			+ "    rm.task_rank_id, "
			+ "    rm.target_year_month, "
			+ "    ( "
			+ "      (extract(YEAR  FROM to_date(rm.target_year_month || '-01','YYYY-MM-DD'))::int * 12) + "
			+ "       extract(MONTH FROM to_date(rm.target_year_month || '-01','YYYY-MM-DD'))::int "
			+ "    ) - row_number() OVER ( "
			+ "          PARTITION BY rm.customer_id, rm.secretary_id, rm.task_rank_id "
			+ "          ORDER BY rm.target_year_month "
			+ "        ) AS grp "
			+ "  FROM rank_months rm "
			+ "), "
			+ "streak_count AS ( "
			+ "  SELECT "
			+ "    s.customer_id, "
			+ "    s.secretary_id, "
			+ "    s.task_rank_id, "
			+ "    count(*) AS cont_months "
			+ "  FROM streak s "
			+ "  JOIN a_target t "
			+ "    ON t.customer_id = s.customer_id "
			+ "   AND t.secretary_id = s.secretary_id "
			+ "   AND t.task_rank_id = s.task_rank_id "
			+ "  GROUP BY s.customer_id, s.secretary_id, s.task_rank_id, s.grp "
			+ "  HAVING max(s.target_year_month) = (SELECT ym FROM eff) "
			+ ") "
			+ "SELECT "
			+ "  c.id, "
			+ "  c.company_code, "
			+ "  c.company_name, "
			+ "  c.mail, "
			+ "  c.phone, "
			+ "  c.postal_code, "
			+ "  c.address1, "
			+ "  c.address2, "
			+ "  c.building, "
			+ "  c.primary_contact_id, "
			+ "  c.created_at, "
			+ "  c.updated_at, "
			+ "  c.deleted_at, "
			+ "  a.id, "
			+ "  a.customer_id, "
			+ "  a.secretary_id, "
			+ "  a.task_rank_id, "
			+ "  a.target_year_month, "
			+ "  a.base_pay_customer, "
			+ "  a.base_pay_secretary, "
			+ "  a.increase_base_pay_customer, "
			+ "  a.increase_base_pay_secretary, "
			+ "  a.customer_based_incentive_for_customer, "
			+ "  a.customer_based_incentive_for_secretary, "
			+ "  a.status, "
			+ "  a.created_at, "
			+ "  a.updated_at, "
			+ "  a.deleted_at, "
			+ "  tr.rank_name, "
			+ "  tr.rank_no, "
			+ "  s.id AS s_id, "
			+ "  s.secretary_rank_id, "
			+ "  s.name AS s_name, "
			+ "  sr.rank_name AS sr_rank_name, "
			+ "  sc.cont_months "
			+ "FROM customers c "
			+ "LEFT JOIN a_target a "
			+ "  ON a.customer_id = c.id "
			+ "LEFT JOIN streak_count sc "
			+ "  ON sc.customer_id = a.customer_id "
			+ " AND sc.secretary_id = a.secretary_id "
			+ " AND sc.task_rank_id = a.task_rank_id "
			+ "LEFT JOIN task_rank tr "
			+ "  ON tr.id = a.task_rank_id "
			+ "LEFT JOIN secretaries s "
			+ "  ON s.id = a.secretary_id "
			+ " AND s.deleted_at IS NULL "
			+ "LEFT JOIN secretary_rank sr "
			+ "  ON sr.id = s.secretary_rank_id "
			+ "WHERE c.deleted_at IS NULL "
			+ "ORDER BY "
			+ "  COALESCE(sc.cont_months, 0) DESC NULLS LAST, "
			+ "  c.company_name, "
			+ "  s.name NULLS LAST, "
			+ "  tr.rank_no NULLS LAST, "
			+ "  a.created_at NULLS LAST";

	/** 指定秘書・指定月の assignments を顧客単位で取得（customers最小限 + task_rank名付き） */
	private static final String SQL_SELECT_BY_SECRETARY_AND_MONTH = "SELECT "
			+ "  c.id AS c_id, "
			+ "  c.company_name, "
			+ "  a.id, a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month, "
			+ "  a.base_pay_customer, a.base_pay_secretary, "
			+ "  a.increase_base_pay_customer, a.increase_base_pay_secretary, "
			+ "  a.customer_based_incentive_for_customer, a.customer_based_incentive_for_secretary, "
			+ "  a.status, a.created_at, a.updated_at, a.deleted_at, "
			+ "  tr.rank_name, tr.rank_no "
			+ "FROM assignments a "
			+ "JOIN customers c ON c.id = a.customer_id AND c.deleted_at IS NULL "
			+ "LEFT JOIN task_rank tr ON tr.id = a.task_rank_id "
			+ "WHERE a.secretary_id = ? "
			+ "  AND a.target_year_month = ? "
			+ "  AND a.deleted_at IS NULL "
			+ "ORDER BY c.company_name, tr.rank_no NULLS LAST, a.created_at";

	/** 指定秘書・指定顧客・指定月の assignments を取得（顧客名・タスクランク名付き） */
	private static final String SQL_SELECT_BY_SECRETARY_CUSTOMER_AND_MONTH = "SELECT "
			+ "  a.id, a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month, "
			+ "  a.base_pay_customer, a.base_pay_secretary, "
			+ "  a.increase_base_pay_customer, a.increase_base_pay_secretary, "
			+ "  a.customer_based_incentive_for_customer, a.customer_based_incentive_for_secretary, "
			+ "  a.status, a.created_at, a.updated_at, a.deleted_at, "
			+ "  c.id AS c_id, c.company_name, "
			+ "  tr.rank_name, tr.rank_no "
			+ "FROM assignments a "
			+ "JOIN customers c ON c.id = a.customer_id AND c.deleted_at IS NULL "
			+ "LEFT JOIN task_rank tr ON tr.id = a.task_rank_id "
			+ "WHERE a.secretary_id = ? "
			+ "  AND a.customer_id  = ? "
			+ "  AND a.target_year_month = ? "
			+ "  AND a.deleted_at IS NULL "
			+ "ORDER BY tr.rank_no NULLS LAST, a.created_at";

	/** 指定顧客の既存 assignments を取得 */
	private static final String SQL_SELECT_BY_CUSTOMER_FROM_YM = "SELECT "
			+ " a.id, a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month, "
			+ " a.base_pay_customer, a.base_pay_secretary, "
			+ " a.increase_base_pay_customer, a.increase_base_pay_secretary, "
			+ " a.customer_based_incentive_for_customer, a.customer_based_incentive_for_secretary, "
			+ " a.status, a.created_at, a.updated_at, a.deleted_at, "
			+ " tr.rank_name, "
			+ " s.id, s.secretary_rank_id, s.name "
			+ " FROM assignments a "
			+ " LEFT JOIN task_rank tr ON tr.id = a.task_rank_id "
			+ " LEFT JOIN secretaries s ON s.id = a.secretary_id AND s.deleted_at IS NULL "
			+ " WHERE a.deleted_at IS NULL "
			+ "   AND a.customer_id = ? "
			+ "   AND a.target_year_month >= ? "
			+ " ORDER BY a.target_year_month ASC, tr.rank_name ASC, s.name ASC";

	/** assignments の INSERT（id を RETURNING） */
	private static final String SQL_INSERT = "INSERT INTO assignments ("
			+ " customer_id, secretary_id, task_rank_id, target_year_month,"
			+ " base_pay_customer, base_pay_secretary,"
			+ " increase_base_pay_customer, increase_base_pay_secretary,"
			+ " customer_based_incentive_for_customer, customer_based_incentive_for_secretary,"
			+ " status, created_at, updated_at"
			+ ") VALUES (?,?,?,?,?,?,?,?,?,?,?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
			+ " RETURNING id";

	/** 同一（年月, 顧客, 秘書, タスクランク）の重複存在チェック */
	private static final String SQL_EXISTS_DUPLICATE = "SELECT 1 FROM assignments"
			+ " WHERE target_year_month = ? AND customer_id = ? AND secretary_id = ? AND task_rank_id = ?"
			+ "   AND deleted_at IS NULL"
			+ " LIMIT 1";

	/** assignments の UPDATE（id指定、論理削除済みは対象外） */
	private static final String SQL_UPDATE = "UPDATE assignments "
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

	/** assignments の 論理DELETE（id指定） */
	private static final String SQL_DELETE_LOGICAL = "UPDATE assignments "
			+ "   SET deleted_at = CURRENT_TIMESTAMP "
			+ " WHERE id = ? "
			+ "   AND deleted_at IS NULL";

	private static final String SQL_SELECT_BY_SEC_AND_MONTH = "SELECT " +
			"  a.id                              AS a_id, " +
			"  a.customer_id                     AS a_customer_id, " +
			"  a.secretary_id                    AS a_secretary_id, " +
			"  a.task_rank_id                    AS a_task_rank_id, " +
			"  a.target_year_month               AS a_target_year_month, " +
			"  a.base_pay_customer               AS a_base_pay_customer, " +
			"  a.base_pay_secretary              AS a_base_pay_secretary, " +
			"  a.increase_base_pay_customer      AS a_increase_base_pay_customer, " +
			"  a.increase_base_pay_secretary     AS a_increase_base_pay_secretary, " +
			"  a.customer_based_incentive_for_customer  AS a_cust_incentive_for_customer, " +
			"  a.customer_based_incentive_for_secretary AS a_cust_incentive_for_secretary, " +
			"  a.status                          AS a_status, " +
			"  a.created_at                      AS a_created_at, " +
			"  a.updated_at                      AS a_updated_at, " +
			"  a.deleted_at                      AS a_deleted_at, " +
			"  tr.rank_name                      AS tr_rank_name, " +
			"  c.company_name                    AS c_company_name " +
			"FROM assignments a " +
			"LEFT JOIN task_rank tr ON tr.id = a.task_rank_id AND tr.deleted_at IS NULL " +
			"INNER JOIN customers c ON c.id = a.customer_id AND c.deleted_at IS NULL " +
			"WHERE a.deleted_at IS NULL " +
			"  AND a.secretary_id = ? " +
			"  AND a.target_year_month = ? " +
			"ORDER BY c.company_name, tr.rank_name NULLS LAST, a.created_at";

	/** 指定月(yyyy-MM)のアサイン（当月分のみ）＋継続月数を算出。
	 * フィルタ：顧客名キーワード・秘書ID・継続月数≧N
	 * ソート：sortByMonthsDesc=true のとき継続月数DESC、それ以外は会社名→秘書名→rank_no→作成日時
	 * 指定月(yyyy-MM)のアサイン（当月分のみ）＋継続月数。フィルタ＆任意で継続月数DESC */
	private static final String SQL_SELECT_ASSIGNMENTS_FOR_MONTH_WITH_CONT = "WITH eff AS ( "
			+ "  SELECT "
			+ "    CASE "
			+ "      WHEN ? > to_char(date_trunc('month', current_date) + interval '1 month', 'YYYY-MM') "
			+ "      THEN to_char(date_trunc('month', current_date) + interval '1 month', 'YYYY-MM') "
			+ "      ELSE ? "
			+ "    END AS ym "
			+ "), "
			+ "a_target AS ( "
			+ "  SELECT a.* "
			+ "  FROM assignments a, eff "
			+ "  WHERE a.deleted_at IS NULL "
			+ "    AND a.target_year_month = eff.ym "
			+ "), "
			+ "rank_months AS ( "
			+ "  SELECT DISTINCT a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month "
			+ "  FROM assignments a, eff "
			+ "  WHERE a.deleted_at IS NULL "
			+ "    AND a.target_year_month <= eff.ym "
			+ "), "
			+ "streak AS ( "
			+ "  SELECT "
			+ "    rm.customer_id, "
			+ "    rm.secretary_id, "
			+ "    rm.task_rank_id, "
			+ "    rm.target_year_month, "
			+ "    ( "
			+ "      (extract(YEAR  FROM to_date(rm.target_year_month || '-01','YYYY-MM-DD'))::int * 12) + "
			+ "       extract(MONTH FROM to_date(rm.target_year_month || '-01','YYYY-MM-DD'))::int "
			+ "    ) - row_number() OVER ( "
			+ "          PARTITION BY rm.customer_id, rm.secretary_id, rm.task_rank_id "
			+ "          ORDER BY rm.target_year_month "
			+ "        ) AS grp "
			+ "  FROM rank_months rm "
			+ "), "
			+ "streak_count AS ( "
			+ "  SELECT "
			+ "    s.customer_id, "
			+ "    s.secretary_id, "
			+ "    s.task_rank_id, "
			+ "    count(*) AS cont_months "
			+ "  FROM streak s "
			+ "  JOIN a_target t "
			+ "    ON t.customer_id = s.customer_id "
			+ "   AND t.secretary_id = s.secretary_id "
			+ "   AND t.task_rank_id = s.task_rank_id "
			+ "  GROUP BY s.customer_id, s.secretary_id, s.task_rank_id, s.grp "
			+ "  HAVING max(s.target_year_month) = (SELECT ym FROM eff) "
			+ ") "
			+ "SELECT "
			+ "  /* assignments（当月のみ） */ "
			+ "  a.id                AS a_id, "
			+ "  a.customer_id       AS a_customer_id, "
			+ "  a.secretary_id      AS a_secretary_id, "
			+ "  a.task_rank_id      AS a_task_rank_id, "
			+ "  a.target_year_month AS a_target_year_month, "
			+ "  a.base_pay_customer, "
			+ "  a.base_pay_secretary, "
			+ "  a.increase_base_pay_customer, "
			+ "  a.increase_base_pay_secretary, "
			+ "  a.customer_based_incentive_for_customer, "
			+ "  a.customer_based_incentive_for_secretary, "
			+ "  a.status            AS a_status, "
			+ "  a.created_at        AS a_created_at, "
			+ "  a.updated_at        AS a_updated_at, "
			+ "  a.deleted_at        AS a_deleted_at, "
			+ "  /* customers */ "
			+ "  c.company_name      AS c_company_name, "
			+ "  /* task_rank */ "
			+ "  tr.rank_name        AS tr_rank_name, "
			+ "  tr.rank_no          AS tr_rank_no, "
			+ "  /* secretaries */ "
			+ "  s.id                AS s_id, "
			+ "  s.name              AS s_name, "
			+ "  s.secretary_rank_id AS s_secretary_rank_id, "
			+ "  sr.rank_name        AS sr_rank_name, "
			+ "  /* 継続月数（ランクごと） */ "
			+ "  sc.cont_months      AS cont_months "
			+ "FROM a_target a "
			+ "JOIN customers c         ON c.id = a.customer_id AND c.deleted_at IS NULL "
			+ "LEFT JOIN task_rank tr   ON tr.id = a.task_rank_id "
			+ "LEFT JOIN secretaries s  ON s.id = a.secretary_id AND s.deleted_at IS NULL "
			+ "LEFT JOIN secretary_rank sr ON sr.id = s.secretary_rank_id "
			+ "LEFT JOIN streak_count sc "
			+ "       ON sc.customer_id = a.customer_id "
			+ "      AND sc.secretary_id = a.secretary_id "
			+ "      AND sc.task_rank_id = a.task_rank_id "
			+ "WHERE "
			+ "  ( ? IS NULL OR c.company_name ILIKE '%' || ? || '%' )       /* 顧客名 */ "
			+ "  AND ( ?::uuid IS NULL OR a.secretary_id = ?::uuid )         /* 秘書 */ "
			+ "  AND ( ?::int  IS NULL OR COALESCE(sc.cont_months,0) >= ?::int )  /* 継続月数≧N */ "
			+ "ORDER BY "
			+ "  CASE WHEN ? THEN COALESCE(sc.cont_months,0) END DESC NULLS LAST, "
			+ "  c.company_name, "
			+ "  s.name NULLS LAST, "
			+ "  tr.rank_no NULLS LAST, "
			+ "  a.created_at NULLS LAST";

	/** 先月→対象月で "未登録のものだけ" 候補に出す */
	private static final String SQL_SELECT_CARRYOVER_CANDIDATES = "SELECT " +
			"  a.id AS a_id, " +
			"  a.customer_id, c.company_name AS c_company_name, " +
			"  a.secretary_id, s.name AS s_name, sr.rank_name AS sr_rank_name, " +
			"  a.task_rank_id, tr.rank_name AS tr_rank_name, " +
			"  a.base_pay_customer, a.base_pay_secretary, " +
			"  a.increase_base_pay_customer, a.increase_base_pay_secretary, " +
			"  a.customer_based_incentive_for_customer, a.customer_based_incentive_for_secretary, " +
			"  a.status " +
			"FROM assignments a " +
			"JOIN customers c  ON c.id = a.customer_id AND c.deleted_at IS NULL " +
			"JOIN secretaries s ON s.id = a.secretary_id AND s.deleted_at IS NULL " +
			"LEFT JOIN secretary_rank sr ON sr.id = s.secretary_rank_id " +
			"LEFT JOIN task_rank tr ON tr.id = a.task_rank_id " +
			"WHERE a.deleted_at IS NULL " +
			"  AND a.target_year_month = ? " + /** fromYM */
			"  AND NOT EXISTS ( " +
			"    SELECT 1 FROM assignments b " +
			"     WHERE b.deleted_at IS NULL " +
			"       AND b.target_year_month = ? " + /** toYM */
			"       AND b.customer_id  = a.customer_id " +
			"       AND b.secretary_id = a.secretary_id " +
			"       AND b.task_rank_id = a.task_rank_id " +
			"  ) " +
			"ORDER BY c.company_name, s.name, tr.rank_name NULLS LAST, a.created_at";

	/** 継続月数カウント用：秘書×顧客の存在月（fromYM まで）を降順で返す */
	private static final String SQL_SELECT_MONTHS_FOR_PAIR_UPTO = "SELECT target_year_month " +
			"  FROM assignments " +
			" WHERE deleted_at IS NULL " +
			"   AND secretary_id = ? " +
			"   AND customer_id  = ? " +
			"   AND target_year_month <= ? " +
			" GROUP BY target_year_month " +
			" ORDER BY target_year_month DESC";

	/** 変更画面用：1件取得（名称付き） */
	private static final String SQL_SELECT_ONE_WITH_NAMES = "SELECT a.id, a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month, "
			+
			"       a.base_pay_customer, a.base_pay_secretary, " +
			"       a.increase_base_pay_customer, a.increase_base_pay_secretary, " +
			"       a.customer_based_incentive_for_customer, a.customer_based_incentive_for_secretary, " +
			"       c.company_name, s.name AS secretary_name, tr.rank_name " +
			"  FROM assignments a " +
			"  JOIN customers  c ON c.id = a.customer_id AND c.deleted_at IS NULL " +
			"  LEFT JOIN secretaries s ON s.id = a.secretary_id AND s.deleted_at IS NULL " +
			"  LEFT JOIN task_rank  tr ON tr.id = a.task_rank_id " +
			" WHERE a.id = ? AND a.deleted_at IS NULL";

	/** 変更適用：顧客×秘書×年月 で継続単価を一括更新（他の列は触らない） */
	private static final String SQL_UPDATE_INCENTIVES_BY_PAIR_MONTH = "UPDATE assignments a "
			+ "   SET customer_based_incentive_for_customer = ?, "
			+ "       customer_based_incentive_for_secretary = ?, "
			+ "       updated_at = CURRENT_TIMESTAMP "
			+ "  FROM task_rank tr "
			+ " WHERE a.task_rank_id = tr.id "
			+ "   AND tr.rank_no <> 0 "
			+ "   AND a.deleted_at IS NULL "
			+ "   AND a.customer_id = ? "
			+ "   AND a.secretary_id = ? "
			+ "   AND a.target_year_month = ?";

	/** 削除可否判定：当該アサインに紐づくタスク件数（0 なら削除OK）
	 * 注意: tasks テーブル名・カラム名はプロジェクトに合わせて変更してください */
	private static final String SQL_COUNT_TASKS_BY_ASSIGNMENT = "SELECT COUNT(1) FROM tasks t WHERE t.deleted_at IS NULL AND t.assignment_id = ?";

	/** 単体取得（主キー） */
	private static final String SQL_SELECT_ONE_MINIMAL = "SELECT id, customer_id, secretary_id, task_rank_id, target_year_month, "
			+
			"       customer_based_incentive_for_customer, customer_based_incentive_for_secretary " +
			"  FROM assignments WHERE id = ? AND deleted_at IS NULL";

	/** 今月まで（<= uptoYM）のアサインを取得。月の新しい順 → 会社名 → rank_no → 作成日時 */
	private static final String SQL_SELECT_BY_SECRETARY_UPTO_MONTH_ORDER_BY_YM_DESC = "WITH eff AS ( " +
			"  SELECT ? AS upto_ym " +
			"), a_upto AS ( " +
			"  SELECT a.* FROM assignments a, eff " +
			"   WHERE a.deleted_at IS NULL AND a.target_year_month <= eff.upto_ym " +
			") " +
			"SELECT " +
			"  a.id, a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month, " +
			"  a.base_pay_customer, a.base_pay_secretary, " +
			"  a.increase_base_pay_customer, a.increase_base_pay_secretary, " +
			"  a.customer_based_incentive_for_customer, a.customer_based_incentive_for_secretary, " +
			"  a.status, a.created_at, a.updated_at, a.deleted_at, " +
			"  c.company_name, tr.rank_name, tr.rank_no " +
			"FROM a_upto a " +
			"JOIN customers c ON c.id = a.customer_id AND c.deleted_at IS NULL " +
			"LEFT JOIN task_rank tr ON tr.id = a.task_rank_id " +
			"WHERE a.secretary_id = ? " +
			"ORDER BY a.target_year_month DESC, c.company_name, tr.rank_no NULLS LAST, a.created_at";

	private static final String SQL_SELECT_SECRETARIES_BY_CUSTOMER_AND_MONTH = "SELECT DISTINCT s.id, s.name, s.postal_code, s.address1, s.address2, s.building "
			+ "  FROM assignments a "
			+ "  JOIN secretaries s ON s.id = a.secretary_id AND s.deleted_at IS NULL "
			+ " WHERE a.deleted_at IS NULL "
			+ "   AND a.customer_id = ? "
			+ "   AND a.target_year_month = ? "
			+ " ORDER BY s.name";

	private static final String SQL_SELECT_SECRETARIES_BY_CUSTOMER = "SELECT DISTINCT s.id, s.name, s.postal_code, s.address1, s.address2, s.building "
			+
			"  FROM assignments a " +
			"  JOIN secretaries s ON s.id = a.secretary_id AND s.deleted_at IS NULL " +
			" WHERE a.deleted_at IS NULL " +
			"   AND a.customer_id = ? " +
			" ORDER BY s.name";

	private static final String SQL_SELECT_SECRETARY_BY_ASSIGNMENT = "SELECT s.id, s.name, s.postal_code, s.address1, s.address2, s.building "
			+
			"  FROM assignments a " +
			"  JOIN secretaries s ON s.id = a.secretary_id AND s.deleted_at IS NULL " +
			" WHERE a.deleted_at IS NULL " +
			"   AND a.id = ?";

	private static final String SQL_SELECT_THIS_MONTH_BY_CUSTOMER_WITH_CONT_RANK = "WITH eff AS ( " +
			"  SELECT CASE " +
			"    WHEN ? > to_char(date_trunc('month', current_date) + interval '1 month','YYYY-MM') " +
			"    THEN to_char(date_trunc('month', current_date) + interval '1 month','YYYY-MM') " +
			"    ELSE ? END AS ym " +
			"), " +
			"a_target AS ( " +
			"  SELECT a.* FROM assignments a, eff " +
			"   WHERE a.deleted_at IS NULL AND a.target_year_month = eff.ym AND a.customer_id = ? " +
			"), " +
			"streak AS ( " +
			"  SELECT a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month, " +
			"         ( (extract(YEAR FROM to_date(a.target_year_month||'-01','YYYY-MM-DD'))::int * 12) + " +
			"           extract(MONTH FROM to_date(a.target_year_month||'-01','YYYY-MM-DD'))::int " +
			"         ) - row_number() OVER ( " +
			"               PARTITION BY a.customer_id, a.secretary_id, a.task_rank_id " +
			"               ORDER BY a.target_year_month " +
			"         ) AS grp " +
			"    FROM assignments a, eff " +
			"   WHERE a.deleted_at IS NULL AND a.customer_id = ? AND a.target_year_month <= eff.ym " +
			"), " +
			"streak_count AS ( " +
			"  SELECT s.customer_id, s.secretary_id, s.task_rank_id, count(*) AS cont_months " +
			"    FROM streak s " +
			"    JOIN a_target t ON t.customer_id = s.customer_id " +
			"                   AND t.secretary_id = s.secretary_id " +
			"                   AND t.task_rank_id = s.task_rank_id " +
			"   GROUP BY s.customer_id, s.secretary_id, s.task_rank_id, s.grp " +
			"  HAVING max(s.target_year_month) = (SELECT ym FROM eff) " +
			") " +
			"SELECT " +
			"  a.id, a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month, " +
			"  a.base_pay_customer, a.base_pay_secretary, a.increase_base_pay_customer, a.increase_base_pay_secretary, "
			+
			"  a.customer_based_incentive_for_customer, a.customer_based_incentive_for_secretary, a.status, " +
			"  a.created_at, a.updated_at, a.deleted_at, " +
			"  tr.rank_name, tr.rank_no, " +
			"  s.id AS s_id, s.name AS s_name, s.secretary_rank_id, " +
			"  sr.rank_name AS sr_rank_name, " +
			"  sc.cont_months " +
			"FROM a_target a " +
			"LEFT JOIN task_rank tr      ON tr.id = a.task_rank_id " +
			"LEFT JOIN secretaries s     ON s.id = a.secretary_id AND s.deleted_at IS NULL " +
			"LEFT JOIN secretary_rank sr ON sr.id = s.secretary_rank_id " +
			"LEFT JOIN streak_count sc   ON sc.customer_id = a.customer_id " +
			"                            AND sc.secretary_id = a.secretary_id " +
			"                            AND sc.task_rank_id = a.task_rank_id " +
			"ORDER BY s.name NULLS LAST, tr.rank_no NULLS LAST, a.created_at";

	/** 顧客×今月まで（<=YM）の assignments 履歴（最新月→） */
	private static final String SQL_SELECT_BY_CUSTOMER_UPTO_YM_DESC = "WITH eff AS ( " +
			"  SELECT CASE " +
			"    WHEN ? > to_char(date_trunc('month', current_date) + interval '1 month','YYYY-MM') " +
			"    THEN to_char(date_trunc('month', current_date) + interval '1 month','YYYY-MM') " +
			"    ELSE ? END AS ym " +
			") " +
			"SELECT a.id, a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month, " +
			"       a.base_pay_customer, a.base_pay_secretary, a.increase_base_pay_customer, a.increase_base_pay_secretary, "
			+
			"       a.customer_based_incentive_for_customer, a.customer_based_incentive_for_secretary, a.status, " +
			"       a.created_at, a.updated_at, a.deleted_at, " +
			"       tr.rank_name, tr.rank_no, " +
			"       s.id AS s_id, s.name AS s_name, s.secretary_rank_id, sr.rank_name AS sr_rank_name " +
			"  FROM assignments a " +
			"  LEFT JOIN task_rank tr      ON tr.id = a.task_rank_id " +
			"  LEFT JOIN secretaries s     ON s.id = a.secretary_id AND s.deleted_at IS NULL " +
			"  LEFT JOIN secretary_rank sr ON sr.id = s.secretary_rank_id " +
			" WHERE a.deleted_at IS NULL AND a.customer_id = ? " +
			"   AND a.target_year_month <= (SELECT ym FROM eff) " +
			" ORDER BY a.target_year_month DESC, tr.rank_no NULLS LAST, s.name NULLS LAST, a.created_at";

//    	/** 顧客×今月まで（<=YM）の assignments 履歴（最新月→） */
//    	private static final String SQL_SELECT_BY_CUSTOMER_UPTO_YM_DESC =
//    	    "WITH eff AS ( " +
//    	    "  SELECT CASE " +
//    	    "    WHEN ? > to_char(date_trunc('month', current_date) + interval '1 month','YYYY-MM') " +
//    	    "    THEN to_char(date_trunc('month', current_date) + interval '1 month','YYYY-MM') " +
//    	    "    ELSE ? END AS ym " +
//    	    ") " +
//    	    "SELECT a.id, a.customer_id, a.secretary_id, a.task_rank_id, a.target_year_month, " +
//    	    "       a.base_pay_customer, a.base_pay_secretary, a.increase_base_pay_customer, a.increase_base_pay_secretary, " +
//    	    "       a.customer_based_incentive_for_customer, a.customer_based_incentive_for_secretary, a.status, " +
//    	    "       a.created_at, a.updated_at, a.deleted_at, " +
//    	    "       tr.rank_name, tr.rank_no, " +
//    	    "       s.id AS s_id, s.name AS s_name, s.secretary_rank_id, sr.rank_name AS sr_rank_name " +
//    	    "  FROM assignments a " +
//    	    "  LEFT JOIN task_rank tr      ON tr.id = a.task_rank_id " +
//    	    "  LEFT JOIN secretaries s     ON s.id = a.secretary_id AND s.deleted_at IS NULL " +
//    	    "  LEFT JOIN secretary_rank sr ON sr.id = s.secretary_rank_id " +
//    	    " WHERE a.deleted_at IS NULL AND a.customer_id = ? " +
//    	    "   AND a.target_year_month <= (SELECT ym FROM eff) " +
//    	    " ORDER BY a.target_year_month DESC, tr.rank_no NULLS LAST, s.name NULLS LAST, a.created_at";
    
    	
    	/** 秘書プロフィール取得 */
        private static final String SQL_SELECT_PROFILE_BY_SECRETARY_ID =
        	    "SELECT id, secretary_id, " +
        	    " weekday_morning, weekday_daytime, weekday_night, " +
        	    " saturday_morning, saturday_daytime, saturday_night, " +
        	    " sunday_morning, sunday_daytime, sunday_night, " +
        	    " weekday_work_hours, saturday_work_hours, sunday_work_hours, " +
        	    " monthly_work_hours, " +
        	    " remark, qualification, work_history, academic_background, self_introduction, " +
        	    " created_at, updated_at, deleted_at " +
        	    "FROM profiles " +
        	    "WHERE deleted_at IS NULL AND secretary_id = ?";
        
        /** 秘書の基本情報（氏名/フリガナ）のみ取得 */
        private static final String SQL_SELECT_SECRETARY_NAME_BY_ID =
            "SELECT name, name_ruby FROM secretaries WHERE deleted_at IS NULL AND id = ?";
        
        private static final String SQL_SELECT_SECRETARY_ID_BY_NAME =
        	    "SELECT id FROM secretaries " +
        	    " WHERE deleted_at IS NULL AND name = ? " +
        	    " ORDER BY created_at ASC LIMIT 1";   
        
        
	public AssignmentDAO(Connection conn) {
		super(conn);
	}

	/** ========================
	 * SELECT
	 * ======================== */

	/**
	 * 指定年月（yyyy-MM）のアサイン一覧を顧客ごとに取得。
	 * ・Pが先頭（task_rank.rank_no ASC）
	 * ・継続月数はSQL側で算出して ad.setConsecutiveMonths(...) に格納
	 * ・指定年月はサーバ日付基準で「来月」までにクランプ（以降は見せない）
	 */
	public List<CustomerDTO> selectAllByMonth(String yearMonth) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_MONTH)) {
			/** クランプ用に同じ値を2回渡す（SQL側で比較して選択） */
			ps.setString(1, yearMonth);
			ps.setString(2, yearMonth);

			try (ResultSet rs = ps.executeQuery()) {
				Map<UUID, CustomerDTO> customerMap = new LinkedHashMap<>();

				while (rs.next()) {
					int i = 1;

					/** customers */
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
						i += 12; /** 既読分スキップ */
					}

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

						ad.setTaskRankName(rs.getString(i++)); /** tr.rank_name */
						i++; /** tr.rank_no 読み飛ばし（必要ならDTOへ） */

						ad.setSecretaryId(rs.getObject(i++, UUID.class));
						ad.setSecretaryRankId(rs.getObject(i++, UUID.class));
						ad.setSecretaryName(rs.getString(i++));
						ad.setSecretaryRankName(rs.getString(i++));

						/** 継続月数 */
						Integer cont = (Integer) rs.getObject(i++);
						ad.setConsecutiveMonths(cont); /** DTOに追加 */

						c.getAssignmentDTOs().add(ad);
					}
				}
				return new ArrayList<>(customerMap.values());
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS11 指定月の assignments 取得に失敗しました。", e);
		}
	}

	/**
	 * 指定月のアサイン（当月分）を継続月数付きで取得し、必要に応じて絞り込み・並べ替え。
	 * @param yearMonth         "yyyy-MM"。SQL側で「来月」までに自動クランプ
	 * @param filterSecretaryId 秘書で絞り込み（null可）
	 * @param qCustomer         顧客名部分一致（null/空可）
	 * @param minMonths         継続月数の下限（null可）
	 * @param sortByMonthsDesc  trueなら継続月数の降順を最優先
	 * @param outContMonths     a.id -> 継続月数 を詰める（null可）
	 */
	public List<AssignmentDTO> selectAssignmentsForMonthWithCont(
			String yearMonth,
			UUID filterSecretaryId,
			String qCustomer,
			Integer minMonths,
			boolean sortByMonthsDesc,
			Map<UUID, Integer> outContMonths) {

		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ASSIGNMENTS_FOR_MONTH_WITH_CONT)) {
			int p = 1;

			/** 年月（クランプ用に2回） */
			ps.setString(p++, yearMonth);
			ps.setString(p++, yearMonth);

			/** 顧客名（WHEREで2回） */
			if (qCustomer == null || qCustomer.isBlank()) {
				ps.setNull(p++, java.sql.Types.VARCHAR);
				ps.setNull(p++, java.sql.Types.VARCHAR);
			} else {
				ps.setString(p++, qCustomer);
				ps.setString(p++, qCustomer);
			}

			/** 秘書ID（WHEREで2回） */
			if (filterSecretaryId == null) {
				ps.setNull(p++, java.sql.Types.OTHER);
				ps.setNull(p++, java.sql.Types.OTHER);
			} else {
				ps.setObject(p++, filterSecretaryId);
				ps.setObject(p++, filterSecretaryId);
			}

			/** 継続月数min（WHEREで2回） */
			if (minMonths == null) {
				ps.setNull(p++, java.sql.Types.INTEGER);
				ps.setNull(p++, java.sql.Types.INTEGER);
			} else {
				ps.setInt(p++, minMonths);
				ps.setInt(p++, minMonths);
			}

			/** 継続月数の降順ソートフラグ（ORDER BY の CASE WHEN ? THEN ...） */
			ps.setBoolean(p++, sortByMonthsDesc);

			List<AssignmentDTO> list = new ArrayList<>();
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int i = 1;

					AssignmentDTO ad = new AssignmentDTO();
					ad.setAssignmentId(rs.getObject(i++, UUID.class)); /** a_id */
					ad.setAssignmentCustomerId(rs.getObject(i++, UUID.class)); /** a_customer_id */
					ad.setAssignmentSecretaryId(rs.getObject(i++, UUID.class)); /** a_secretary_id */
					ad.setTaskRankId(rs.getObject(i++, UUID.class)); /** a_task_rank_id */
					ad.setTargetYearMonth(rs.getString(i++)); /** a_target_year_month */
					ad.setBasePayCustomer(rs.getBigDecimal(i++));
					ad.setBasePaySecretary(rs.getBigDecimal(i++));
					ad.setIncreaseBasePayCustomer(rs.getBigDecimal(i++));
					ad.setIncreaseBasePaySecretary(rs.getBigDecimal(i++));
					ad.setCustomerBasedIncentiveForCustomer(rs.getBigDecimal(i++));
					ad.setCustomerBasedIncentiveForSecretary(rs.getBigDecimal(i++));
					ad.setAssignmentStatus(rs.getString(i++)); /** a_status */
					ad.setAssignmentCreatedAt(rs.getTimestamp(i++)); /** a_created_at */
					ad.setAssignmentUpdatedAt(rs.getTimestamp(i++)); /** a_updated_at */
					ad.setAssignmentDeletedAt(rs.getTimestamp(i++)); /** a_deleted_at */

					ad.setCustomerCompanyName(rs.getString(i++)); /** c_company_name */

					ad.setTaskRankName(rs.getString(i++));
					/** tr_rank_name */
					rs.getInt(i++); /** tr_rank_no（必要ならDTOに保持） */

					UUID sId = rs.getObject(i++, UUID.class); /** s_id */
					ad.setSecretaryId(sId); /** これが必須（Converter が見るのはこっち） */
					ad.setSecretaryName(rs.getString(i++)); /** s_name */
					ad.setSecretaryRankId(rs.getObject(i++, UUID.class));
					ad.setSecretaryRankName(rs.getString(i++)); /** sr_rank_name */

					Number contNum = (Number) rs.getObject(i++);
					Integer cont = (contNum == null) ? null : contNum.intValue();
					ad.setConsecutiveMonths(cont);
					if (outContMonths != null && ad.getAssignmentId() != null) {
						outContMonths.put(ad.getAssignmentId(), cont == null ? 0 : cont);
					} /** cont_months */
					ad.setConsecutiveMonths(cont); /** DTOに追加済み想定 */

					list.add(ad);

					if (outContMonths != null && ad.getAssignmentId() != null) {
						outContMonths.put(ad.getAssignmentId(), cont == null ? 0 : cont);
					}
				}
			}
			return list;
		} catch (SQLException e) {
			throw new DAOException("E:AS11C 継続月数付き assignments 取得に失敗しました。", e);
		}
	}

	/**
	 * 指定月（yyyy-MM）の一覧をフィルタ付きで取得。
	 * @param yearMonth    対象年月（来月を上限にSQL側でクランプ）
	 * @param keyword      顧客名の部分一致（NULL可）
	 * @param secretaryId  担当秘書で絞り込み（NULL可）
	 * @param minMonths    継続月数≧min で絞り込み（NULL可）
	 */
	public List<CustomerDTO> selectAllByMonthFiltered(
			String yearMonth, String keyword, UUID secretaryId, Integer minMonths) {

		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_MONTH_FILTERED)) {
			int p = 1;
			ps.setString(p++, yearMonth);
			ps.setString(p++, yearMonth);

			/** keyword は2回使う */
			if (keyword == null || keyword.isBlank()) {
				ps.setNull(p++, java.sql.Types.VARCHAR);
				ps.setNull(p++, java.sql.Types.VARCHAR);
			} else {
				ps.setString(p++, keyword);
				ps.setString(p++, keyword);
			}

			/** secretaryId は2回使う */
			if (secretaryId == null) {
				ps.setNull(p++, java.sql.Types.OTHER);
				ps.setNull(p++, java.sql.Types.OTHER);
			} else {
				ps.setObject(p++, secretaryId);
				ps.setObject(p++, secretaryId);
			}

			/** minMonths は2回使う */
			if (minMonths == null) {
				ps.setNull(p++, java.sql.Types.INTEGER);
				ps.setNull(p++, java.sql.Types.INTEGER);
			} else {
				ps.setInt(p++, minMonths);
				ps.setInt(p++, minMonths);
			}

			try (ResultSet rs = ps.executeQuery()) {
				Map<UUID, CustomerDTO> customerMap = new LinkedHashMap<>();

				while (rs.next()) {
					int i = 1;

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
						i += 12;
					}

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

						ad.setTaskRankName(rs.getString(i++)); // tr.rank_name
						i++; // tr.rank_no

						ad.setSecretaryId(rs.getObject(i++, UUID.class));
						ad.setSecretaryRankId(rs.getObject(i++, UUID.class));
						ad.setSecretaryName(rs.getString(i++));
						ad.setSecretaryRankName(rs.getString(i++));

						Number contNum = (Number) rs.getObject(i++);
						Integer cont = (contNum == null) ? null : contNum.intValue();
						ad.setConsecutiveMonths(cont);

						c.getAssignmentDTOs().add(ad);
					}
				}
				return new ArrayList<>(customerMap.values());
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS11F フィルタ付き assignments 取得に失敗しました。", e);
		}
	}

	public List<AssignmentDTO> selectBySecretaryUpToMonthOrderByYmDesc(UUID secretaryId, String uptoYm) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_SECRETARY_UPTO_MONTH_ORDER_BY_YM_DESC)) {
			int p = 1;
			ps.setString(p++, uptoYm);
			ps.setObject(p++, secretaryId);
			try (ResultSet rs = ps.executeQuery()) {
				List<AssignmentDTO> list = new ArrayList<>();
				while (rs.next()) {
					int i = 1;
					AssignmentDTO ad = new AssignmentDTO();
					ad.setAssignmentId(rs.getObject(i++, UUID.class));
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
					ad.setCustomerCompanyName(rs.getString(i++)); /** c.company_name */
					ad.setTaskRankName(rs.getString(i++));
					/** tr.rank_name */
					rs.getInt(i++); /** tr.rank_no（必要ならDTOへ） */
					list.add(ad);
				}
				return list;
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS11U 今月までの assignments 取得に失敗しました。", e);
		}
	}

	/**
	 * 指定月（yyyy-MM）の一覧を継続月数の高い順で取得。
	 * Pが先頭（rank_no ASC）を維持しつつ、主ソートは継続月数DESC。
	 */
	public List<CustomerDTO> selectAllByMonthOrderByConsecutiveDesc(String yearMonth) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_MONTH_ORDERBY_CONT_DESC)) {
			ps.setString(1, yearMonth);
			ps.setString(2, yearMonth);

			try (ResultSet rs = ps.executeQuery()) {
				Map<UUID, CustomerDTO> customerMap = new LinkedHashMap<>();

				while (rs.next()) {
					int i = 1;

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
						i += 12;
					}

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

					ad.setTaskRankName(rs.getString(i++));
					i++; /** tr.rank_no */

						ad.setSecretaryId(rs.getObject(i++, UUID.class));
						ad.setSecretaryRankId(rs.getObject(i++, UUID.class));
						ad.setSecretaryName(rs.getString(i++));
						ad.setSecretaryRankName(rs.getString(i++));

						Number contNum = (Number) rs.getObject(i++);
						Integer cont = (contNum == null) ? null : contNum.intValue();
						ad.setConsecutiveMonths(cont);

						c.getAssignmentDTOs().add(ad);
					}
				}
				return new ArrayList<>(customerMap.values());
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS11S 継続月数ソート assignments 取得に失敗しました。", e);
		}
	}

	/**
	 * 指定した秘書ID・年月（yyyy-MM）のアサイン情報を顧客単位で取得します。
	 * 顧客は {@code id, company_name} のみをセットし、その配下に {@link AssignmentDTO} を格納します。
	 *
	 * @param secretaryId 秘書ID（{@link UUID}）
	 * @param yearMonth   年月（例: "2025-09"）
	 * @return 顧客ごとに assignments を束ねた {@link CustomerDTO} のリスト（会社名昇順）
	 * @throws DAOException 取得時にエラーが発生した場合
	 */
	public List<CustomerDTO> selectBySecretaryAndMonthToCustomer(UUID secretaryId, String yearMonth) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_SECRETARY_AND_MONTH)) {
			int p = 1;
			ps.setObject(p++, secretaryId);
			ps.setString(p++, yearMonth);

			try (ResultSet rs = ps.executeQuery()) {
				Map<UUID, CustomerDTO> customerMap = new LinkedHashMap<>();

				while (rs.next()) {
					int i = 1;

					/** customers (2) */
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

					/** assignments (15) */
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

					/** task_rank (1) */
					ad.setTaskRankName(rs.getString(i++));

					/** 顧客配下に格納 */
					c.getAssignmentDTOs().add(ad);
				}

				return new ArrayList<>(customerMap.values());
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS12 指定秘書・指定月の assignments 取得に失敗しました。", e);
		}
	}

	/** 秘書×年月のアサイン一覧（register.jsp のプルダウン用） */
	public List<AssignmentDTO> selectBySecretaryAndMonthToAssignment(UUID secretaryId, String yearMonth) {
		List<AssignmentDTO> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_SEC_AND_MONTH)) {
			int p = 1;
			ps.setObject(p++, secretaryId);
			ps.setString(p++, yearMonth);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					AssignmentDTO ad = new AssignmentDTO();
					ad.setAssignmentId(rs.getObject("a_id", UUID.class));
					ad.setAssignmentCustomerId(rs.getObject("a_customer_id", UUID.class));
					ad.setAssignmentSecretaryId(rs.getObject("a_secretary_id", UUID.class));
					ad.setTaskRankId(rs.getObject("a_task_rank_id", UUID.class));
					ad.setTargetYearMonth(rs.getString("a_target_year_month"));
					ad.setBasePayCustomer(rs.getBigDecimal("a_base_pay_customer"));
					ad.setBasePaySecretary(rs.getBigDecimal("a_base_pay_secretary"));
					ad.setIncreaseBasePayCustomer(rs.getBigDecimal("a_increase_base_pay_customer"));
					ad.setIncreaseBasePaySecretary(rs.getBigDecimal("a_increase_base_pay_secretary"));
					ad.setCustomerBasedIncentiveForCustomer(rs.getBigDecimal("a_cust_incentive_for_customer"));
					ad.setCustomerBasedIncentiveForSecretary(rs.getBigDecimal("a_cust_incentive_for_secretary"));
					ad.setAssignmentStatus(rs.getString("a_status"));
					ad.setAssignmentCreatedAt(rs.getTimestamp("a_created_at"));
					ad.setAssignmentUpdatedAt(rs.getTimestamp("a_updated_at"));
					ad.setAssignmentDeletedAt(rs.getTimestamp("a_deleted_at"));

					/** 画面表示用 */
					ad.setTaskRankName(rs.getString("tr_rank_name"));
					ad.setCustomerCompanyName(rs.getString("c_company_name"));

					list.add(ad);
				}
			}
		} catch (SQLException e) {
			throw new DAOException("E:ASG11 assignments(秘書×年月) 取得に失敗しました。", e);
		}
		return list;
	}

	/**
	 * 指定した秘書ID・顧客ID・年月（yyyy-MM）のアサイン情報を取得します。
	 * AssignmentDTO に顧客名・タスクランク名を含めて返します。
	 *
	 * @param secretaryId 秘書ID（UUID）
	 * @param customerId  顧客ID（UUID）
	 * @param yearMonth   年月（例: "2025-09"）
	 * @return 該当 assignments のリスト（空リストあり）
	 * @throws DAOException 取得時にエラーが発生した場合
	 */
	public List<AssignmentDTO> selectBySecretaryAndCustomerAndMonth(UUID secretaryId, UUID customerId,
			String yearMonth) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_SECRETARY_CUSTOMER_AND_MONTH)) {
			int p = 1;
			ps.setObject(p++, secretaryId);
			ps.setObject(p++, customerId);
			ps.setString(p++, yearMonth);

			try (ResultSet rs = ps.executeQuery()) {
				List<AssignmentDTO> result = new ArrayList<>();

				while (rs.next()) {
					int i = 1;
					AssignmentDTO ad = new AssignmentDTO();

					/** assignments (15) */
					ad.setAssignmentId(rs.getObject(i++, UUID.class));
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
					/** customers (2) */
					ad.setAssignmentCustomerId(rs.getObject(i++, UUID.class)); /** c.id */
					ad.setCustomerCompanyName(rs.getString(i++)); /** c.company_name */

					/** task_rank (1) */
					ad.setTaskRankName(rs.getString(i++));

					result.add(ad);
				}

				return result;
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS14 指定秘書・顧客・月の assignments 取得に失敗しました。", e);
		}
	}

	/**
	 * 指定顧客の今月以降のアサイン一覧を取得します。
	 * @param customerId 顧客ID
	 * @param fromYm     "yyyy-MM"（例: 2025-09）
	 */
	public List<AssignmentDTO> selectByCustomerFromYearMonth(java.util.UUID customerId, String fromYm) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_CUSTOMER_FROM_YM)) {
			int p = 1;
			ps.setObject(p++, customerId);
			ps.setString(p++, fromYm);
			try (ResultSet rs = ps.executeQuery()) {
				List<AssignmentDTO> list = new ArrayList<>();
				while (rs.next()) {
					int i = 1;
					AssignmentDTO ad = new AssignmentDTO();
					/** assignments (15) */
					ad.setAssignmentId(rs.getObject(i++, java.util.UUID.class));
					ad.setAssignmentCustomerId(rs.getObject(i++, java.util.UUID.class));
					ad.setAssignmentSecretaryId(rs.getObject(i++, java.util.UUID.class));
					ad.setTaskRankId(rs.getObject(i++, java.util.UUID.class));

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
					ad.setTaskRankName(rs.getString(i++));

					/** secretaries (3) */
					ad.setSecretaryId(rs.getObject(i++, java.util.UUID.class));
					ad.setSecretaryRankId(rs.getObject(i++, java.util.UUID.class));
					ad.setSecretaryName(rs.getString(i++));

					list.add(ad);
				}
				return list;
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS90 顧客の今月以降アサイン取得に失敗しました。", e);
		}
	}

	/** ID配列で元アサインを取得（適用時にコピー元として使用） */
	public List<AssignmentDTO> selectByIds(List<UUID> ids) {
		if (ids == null || ids.isEmpty())
			return java.util.Collections.emptyList();
		StringBuilder sql = new StringBuilder(
				"SELECT id, customer_id, secretary_id, task_rank_id, target_year_month, " +
						" base_pay_customer, base_pay_secretary, increase_base_pay_customer, increase_base_pay_secretary, "
						+
						" customer_based_incentive_for_customer, customer_based_incentive_for_secretary, status " +
						"FROM assignments WHERE deleted_at IS NULL AND id IN (");
		for (int i = 0; i < ids.size(); i++) {
			if (i > 0)
				sql.append(',');
			sql.append('?');
		}
		sql.append(')');

		List<AssignmentDTO> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			int p = 1;
			for (UUID id : ids)
				ps.setObject(p++, id);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					AssignmentDTO ad = new AssignmentDTO();
					ad.setAssignmentId(rs.getObject("id", UUID.class));
					ad.setAssignmentCustomerId(rs.getObject("customer_id", UUID.class));
					ad.setAssignmentSecretaryId(rs.getObject("secretary_id", UUID.class));
					ad.setTaskRankId(rs.getObject("task_rank_id", UUID.class));
					ad.setTargetYearMonth(rs.getString("target_year_month"));
					ad.setBasePayCustomer(rs.getBigDecimal("base_pay_customer"));
					ad.setBasePaySecretary(rs.getBigDecimal("base_pay_secretary"));
					ad.setIncreaseBasePayCustomer(rs.getBigDecimal("increase_base_pay_customer"));
					ad.setIncreaseBasePaySecretary(rs.getBigDecimal("increase_base_pay_secretary"));
					ad.setCustomerBasedIncentiveForCustomer(rs.getBigDecimal("customer_based_incentive_for_customer"));
					ad.setCustomerBasedIncentiveForSecretary(
							rs.getBigDecimal("customer_based_incentive_for_secretary"));
					ad.setAssignmentStatus(rs.getString("status"));
					list.add(ad);
				}
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS-CO3 ID指定の取得に失敗しました。", e);
		}
		return list;
	}

	/** 1件取得（編集表示用：名称付き） */
	public AssignmentDTO selectOneWithNames(UUID id) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ONE_WITH_NAMES)) {
			ps.setObject(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next())
					return null;
				AssignmentDTO d = new AssignmentDTO();
				int i = 1;
				d.setAssignmentId(rs.getObject(i++, UUID.class));
				d.setAssignmentCustomerId(rs.getObject(i++, UUID.class));
				d.setAssignmentSecretaryId(rs.getObject(i++, UUID.class));
				d.setTaskRankId(rs.getObject(i++, UUID.class));
				d.setTargetYearMonth(rs.getString(i++));
				d.setBasePayCustomer(rs.getBigDecimal(i++)); /** 追加 */
				d.setBasePaySecretary(rs.getBigDecimal(i++)); /** 追加 */
				d.setIncreaseBasePayCustomer(rs.getBigDecimal(i++)); /** 追加 */
				d.setIncreaseBasePaySecretary(rs.getBigDecimal(i++)); /** 追加 */
				d.setCustomerBasedIncentiveForCustomer(rs.getBigDecimal(i++));
				d.setCustomerBasedIncentiveForSecretary(rs.getBigDecimal(i++));
				d.setCustomerCompanyName(rs.getString(i++));
				d.setSecretaryName(rs.getString(i++));
				d.setTaskRankName(rs.getString(i++));
				return d;
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS60 アサイン1件取得に失敗しました。", e);
		}
	}

	/** 1件取得（最小） */
	public AssignmentDTO selectOne(UUID id) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ONE_MINIMAL)) {
			ps.setObject(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next())
					return null;
				AssignmentDTO d = new AssignmentDTO();
				d.setAssignmentId(rs.getObject("id", UUID.class));
				d.setAssignmentCustomerId(rs.getObject("customer_id", UUID.class));
				d.setAssignmentSecretaryId(rs.getObject("secretary_id", UUID.class));
				d.setTaskRankId(rs.getObject("task_rank_id", UUID.class));
				d.setTargetYearMonth(rs.getString("target_year_month"));
				d.setCustomerBasedIncentiveForCustomer(rs.getBigDecimal("customer_based_incentive_for_customer"));
				d.setCustomerBasedIncentiveForSecretary(rs.getBigDecimal("customer_based_incentive_for_secretary"));
				return d;
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS61 アサイン1件(最小)取得に失敗しました。", e);
		}
	}

	public List<AssignmentDTO> selectThisMonthByCustomerWithContRank(UUID customerId, String yearMonth) {
		List<AssignmentDTO> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_THIS_MONTH_BY_CUSTOMER_WITH_CONT_RANK)) {
			int p = 1;
			ps.setString(p++, yearMonth);
			ps.setString(p++, yearMonth);
			ps.setObject(p++, customerId);
			ps.setObject(p++, customerId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int i = 1;
					AssignmentDTO ad = new AssignmentDTO();
					ad.setAssignmentId(rs.getObject(i++, UUID.class));
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

					ad.setTaskRankName(rs.getString(i++)); /** tr.rank_name */
					rs.getInt(i++); /** tr.rank_no（未使用） */

					rs.getObject(i++, UUID.class); /** s_id（未使用） */
					ad.setSecretaryName(rs.getString(i++));
					ad.setSecretaryRankId(rs.getObject(i++, UUID.class));
					ad.setSecretaryRankName(rs.getString(i++));

					Number cont = (Number) rs.getObject(i++);
					ad.setConsecutiveMonths(cont == null ? null : cont.intValue());

					list.add(ad);
				}
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS-D1 顧客×今月（継続月数/ランク別）取得に失敗しました。", e);
		}
		return list;
	}

    //秘書プロフィール表示
    public dto.ProfileDTO selectProfileBySecretaryId(UUID secretaryId) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_PROFILE_BY_SECRETARY_ID)) {
            ps.setObject(1, secretaryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                dto.ProfileDTO p = new dto.ProfileDTO();
                int i = 1;
                p.setId(rs.getObject(i++, UUID.class));
                p.setSecretaryId(rs.getObject(i++, UUID.class));

                p.setWeekdayMorning(rs.getObject(i++, Integer.class));
                p.setWeekdayDaytime(rs.getObject(i++, Integer.class));
                p.setWeekdayNight(rs.getObject(i++, Integer.class));

                p.setSaturdayMorning(rs.getObject(i++, Integer.class));
                p.setSaturdayDaytime(rs.getObject(i++, Integer.class));
                p.setSaturdayNight(rs.getObject(i++, Integer.class));

                p.setSundayMorning(rs.getObject(i++, Integer.class));
                p.setSundayDaytime(rs.getObject(i++, Integer.class));
                p.setSundayNight(rs.getObject(i++, Integer.class));

                p.setWeekdayWorkHours(rs.getBigDecimal(i++));
                p.setSaturdayWorkHours(rs.getBigDecimal(i++));
                p.setSundayWorkHours(rs.getBigDecimal(i++));

                p.setMonthlyWorkHours(rs.getBigDecimal(i++));

                p.setRemark(rs.getString(i++));
                p.setQualification(rs.getString(i++));
                p.setWorkHistory(rs.getString(i++));
                p.setAcademicBackground(rs.getString(i++));
                p.setSelfIntroduction(rs.getString(i++));

                p.setCreatedAt(rs.getTimestamp(i++));
                p.setUpdatedAt(rs.getTimestamp(i++));
                p.setDeletedAt(rs.getTimestamp(i++));
                return p;
            }
        } catch (SQLException e) {
            throw new DAOException("E:C46 profiles 読み取りに失敗しました。", e);
        }
    }

    /** 氏名/フリガナだけ取得（存在しなければ null を返す） */
    public java.util.Optional<java.util.AbstractMap.SimpleEntry<String,String>> selectSecretaryNameById(UUID secretaryId) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_SECRETARY_NAME_BY_ID)) {
            ps.setObject(1, secretaryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString(1);
                    String ruby = rs.getString(2);
                    return java.util.Optional.of(new java.util.AbstractMap.SimpleEntry<>(name, ruby));
                }
                return java.util.Optional.empty();
            }
        } catch (SQLException e) {
            throw new DAOException("E:C47 secretaries 氏名取得に失敗しました。", e);
        }
    }
    
    /** 氏名（完全一致）→ 秘書ID を返す。該当なしは Optional.empty() */
    public java.util.Optional<java.util.UUID> selectSecretaryIdByName(String name) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_SECRETARY_ID_BY_NAME)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return java.util.Optional.of(rs.getObject(1, java.util.UUID.class));
                }
                return java.util.Optional.empty();
            }
        } catch (SQLException e) {
            throw new DAOException("E:AS-N2 秘書ID(氏名検索) 取得に失敗しました。", e);
        }
    }

	public List<AssignmentDTO> selectByCustomerUpToYearMonthDesc(UUID customerId, String upToYm) {
		List<AssignmentDTO> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_CUSTOMER_UPTO_YM_DESC)) {
			int p = 1;
			ps.setString(p++, upToYm);
			ps.setString(p++, upToYm);
			ps.setObject(p++, customerId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int i = 1;
					AssignmentDTO ad = new AssignmentDTO();
					ad.setAssignmentId(rs.getObject(i++, UUID.class));
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

					ad.setTaskRankName(rs.getString(i++));
					rs.getInt(i++); /** tr.rank_no */
					rs.getObject(i++, UUID.class); /** s_id */
					ad.setSecretaryName(rs.getString(i++));
					ad.setSecretaryRankId(rs.getObject(i++, UUID.class));
					ad.setSecretaryRankName(rs.getString(i++));

					list.add(ad);
				}
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS-D2 顧客×今月までの履歴取得に失敗しました。", e);
		}
		return list;
	}

	/** ========================
	 * INSERT
	 * ======================== */

	/**
	 * アサインを新規登録し、採番された {@code id} を返します。
	 * 一部の金額カラムは NULL を許容します。
	 *
	 * @param dto 登録対象
	 * @return 新規採番された {@link UUID}（失敗時や RETURNING 無返却なら {@code null}）
	 * @throws DAOException 登録時にエラーが発生した場合
	 */
	public UUID insert(AssignmentDTO dto) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
			int i = 1;
			/** 必須 */
			ps.setObject(i++, dto.getAssignmentCustomerId());
			ps.setObject(i++, dto.getAssignmentSecretaryId());
			ps.setObject(i++, dto.getTaskRankId());
			ps.setString(i++, dto.getTargetYearMonth());

			/** 金額（NULL許容） */
			setBigDecimalOrNull(ps, i++, dto.getBasePayCustomer());
			setBigDecimalOrNull(ps, i++, dto.getBasePaySecretary());
			setBigDecimalOrNull(ps, i++, dto.getIncreaseBasePayCustomer());
			setBigDecimalOrNull(ps, i++, dto.getIncreaseBasePaySecretary());
			setBigDecimalOrNull(ps, i++, dto.getCustomerBasedIncentiveForCustomer());
			setBigDecimalOrNull(ps, i++, dto.getCustomerBasedIncentiveForSecretary());

			/** status（NULL許容） */
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

	/** ========================
	 * EXISTS（重複チェック）
	 * ======================== */

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

	/** ========================
	 * UPDATE
	 * ======================== */

	/**
	 * アサインを更新します（論理削除済みは対象外）。
	 * NULL 許容の金額カラム／status は NULL をセット可能です。
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

			ps.setObject(i++, dto.getAssignmentId()); /** WHERE id = ? */

			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:AS41 assignments UPDATE に失敗しました。", e);
		}
	}

	/** 顧客×秘書×年月で継続単価を一括更新 */
	public int updateIncentivesByPairAndMonth(
			UUID customerId, UUID secretaryId, String yearMonth,
			java.math.BigDecimal incentiveForCustomer, java.math.BigDecimal incentiveForSecretary) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_INCENTIVES_BY_PAIR_MONTH)) {
			int p = 1;
			setBigDecimalOrNull(ps, p++, incentiveForCustomer);
			setBigDecimalOrNull(ps, p++, incentiveForSecretary);
			ps.setObject(p++, customerId);
			ps.setObject(p++, secretaryId);
			ps.setString(p++, yearMonth);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("E:AS62 継続単価の一括更新に失敗しました。", e);
		}
	}

	/** 当該 assignment に紐づくタスクが存在するか */
	public boolean hasTasks(UUID assignmentId) {
		try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_TASKS_BY_ASSIGNMENT)) {
			ps.setObject(1, assignmentId);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getInt(1) > 0;
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS63 タスク存在チェックに失敗しました。", e);
		}
	}

	/** ========================
	 * DELETE
	 * ======================== */

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

	public List<AssignmentDTO> selectCarryOverCandidates(String fromYM, String toYM) {
		List<AssignmentDTO> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_CARRYOVER_CANDIDATES)) {
			ps.setString(1, fromYM);
			ps.setString(2, toYM);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					AssignmentDTO ad = new AssignmentDTO();
					ad.setAssignmentId(rs.getObject("a_id", UUID.class));
					ad.setAssignmentCustomerId(rs.getObject("customer_id", UUID.class));
					ad.setCustomerCompanyName(rs.getString("c_company_name"));
					ad.setAssignmentSecretaryId(rs.getObject("secretary_id", UUID.class));
					ad.setSecretaryName(rs.getString("s_name"));
					ad.setSecretaryRankName(rs.getString("sr_rank_name"));
					ad.setTaskRankId(rs.getObject("task_rank_id", UUID.class));
					ad.setTaskRankName(rs.getString("tr_rank_name"));
					ad.setBasePayCustomer(rs.getBigDecimal("base_pay_customer"));
					ad.setBasePaySecretary(rs.getBigDecimal("base_pay_secretary"));
					ad.setIncreaseBasePayCustomer(rs.getBigDecimal("increase_base_pay_customer"));
					ad.setIncreaseBasePaySecretary(rs.getBigDecimal("increase_base_pay_secretary"));
					ad.setCustomerBasedIncentiveForCustomer(rs.getBigDecimal("customer_based_incentive_for_customer"));
					ad.setCustomerBasedIncentiveForSecretary(
							rs.getBigDecimal("customer_based_incentive_for_secretary"));
					ad.setAssignmentStatus(rs.getString("status"));
					list.add(ad);
				}
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS-CO1 引継ぎ候補の取得に失敗しました。", e);
		}
		return list;
	}

	public List<String> selectMonthsForPairUpTo(UUID secretaryId, UUID customerId, String upToYm) {
		List<String> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_MONTHS_FOR_PAIR_UPTO)) {
			ps.setObject(1, secretaryId);
			ps.setObject(2, customerId);
			ps.setString(3, upToYm);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					list.add(rs.getString(1));
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS-CO2 継続月数の取得に失敗しました。", e);
		}
		return list;
	}

	/**
	 * 指定顧客・指定年月（yyyy-MM）にアサインされている秘書（重複除外）を取得。
	 * 戻り値は JSP 互換の List<Map>（name/address）です。
	 */
	public List<Map<String, Object>> selectSecretariesByCustomerAndMonth(
			java.util.UUID customerId, String yearMonth) {
		List<Map<String, Object>> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_SECRETARIES_BY_CUSTOMER_AND_MONTH)) {
			ps.setObject(1, customerId);
			ps.setString(2, yearMonth);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String name = rs.getString("name");
					String postal = rs.getString("postal_code");
					String a1 = rs.getString("address1");
					String a2 = rs.getString("address2");
					String bld = rs.getString("building");

					StringBuilder addr = new StringBuilder();
					if (postal != null && !postal.isBlank())
						addr.append("〒").append(postal.trim()).append(' ');
					if (a1 != null && !a1.isBlank())
						addr.append(a1.trim());
					if (a2 != null && !a2.isBlank())
						addr.append(a2.startsWith(" ") ? a2 : " " + a2.trim());
					if (bld != null && !bld.isBlank())
						addr.append(bld.startsWith(" ") ? bld : " " + bld.trim());

					Map<String, Object> m = new LinkedHashMap<>();
					m.put("name", name);
					m.put("address", addr.toString().trim());
					list.add(m);
				}
			}
		} catch (SQLException e) {

			throw new DAOException("E:AS-SX 顧客×年月の秘書一覧取得に失敗しました。", e);
		}
		return list;
	}

	//アサインIDで
	public List<Map<String, Object>> selectSecretariesByCustomer(java.util.UUID customerId) {
		List<Map<String, Object>> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_SECRETARIES_BY_CUSTOMER)) {
			ps.setObject(1, customerId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String name = rs.getString("name");
					String postal = rs.getString("postal_code");
					String a1 = rs.getString("address1");
					String a2 = rs.getString("address2");
					String bld = rs.getString("building");

					StringBuilder addr = new StringBuilder();
					if (postal != null && !postal.isBlank())
						addr.append("〒").append(postal.trim()).append(' ');
					if (a1 != null && !a1.isBlank())
						addr.append(a1.trim());
					if (a2 != null && !a2.isBlank())
						addr.append(a2.startsWith(" ") ? a2 : " " + a2.trim());
					if (bld != null && !bld.isBlank())
						addr.append(bld.startsWith(" ") ? bld : " " + bld.trim());

					Map<String, Object> m = new LinkedHashMap<>();
					m.put("name", name);
					m.put("address", addr.toString().trim()); /** JSP側で（${s.address}）と括弧付き表示 */
					list.add(m);
				}
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS-SC 顧客IDの秘書一覧（案件ベース）取得に失敗しました。", e);
		}
		return list;
	}

	public List<Map<String, Object>> selectSecretariesByAssignment(UUID assignmentId) {
		List<Map<String, Object>> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_SECRETARY_BY_ASSIGNMENT)) {
			ps.setObject(1, assignmentId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String name = rs.getString("name");
					String postal = rs.getString("postal_code");
					String a1 = rs.getString("address1");
					String a2 = rs.getString("address2");
					String bld = rs.getString("building");

					StringBuilder addr = new StringBuilder();
					if (postal != null && !postal.isBlank())
						addr.append("〒").append(postal.trim()).append(' ');
					if (a1 != null && !a1.isBlank())
						addr.append(a1.trim());
					if (a2 != null && !a2.isBlank())
						addr.append(a2.startsWith(" ") ? a2 : " " + a2.trim());
					if (bld != null && !bld.isBlank())
						addr.append(bld.startsWith(" ") ? bld : " " + bld.trim());

					Map<String, Object> m = new LinkedHashMap<>();
					m.put("name", name);
					m.put("address", addr.toString().trim());
					/** 必要なら m.put("note", ...); を追加 */
					list.add(m);
				}
			}
		} catch (SQLException e) {
			throw new DAOException("E:AS-SA アサインIDの秘書情報取得に失敗しました。", e);
		}
		return list;
	}

	/** ========================
	 * Helper
	 * ======================== */

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