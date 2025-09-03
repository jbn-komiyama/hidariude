// TaskDAO.java
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dto.AssignmentDTO;
import dto.CustomerMonthlyInvoiceDTO;
import dto.SecretaryDTO;
import dto.SecretaryMonthlySummaryDTO;
import dto.TaskDTO;

public class TaskDAO extends BaseDAO {

    // ========================
    // SQL 定義
    // ========================

    /**
     * 秘書ID・顧客ID・年月(YYYY-MM) でタスク取得
     * 取得カラム: tasks.* + assignments.* + task_rank.rank_name
     * 絞り込み: a.secretary_id, a.customer_id, a.target_year_month, 各 deleted_at IS NULL
     * 並び順: t.start_time
     */
    private static final String SQL_SELECT_BY_SEC_CUST_MONTH =
        "SELECT " +
        // ---- tasks.* (14) ----
        "  t.id                              AS t_id, " +
        "  t.assignment_id                   AS t_assignment_id, " +
        "  t.work_date                       AS t_work_date, " +
        "  t.start_time                      AS t_start_time, " +
        "  t.end_time                        AS t_end_time, " +
        "  t.work_minute                     AS t_work_minute, " +
        "  t.work_content                    AS t_work_content, " +
        "  t.approved_at                     AS t_approved_at, " +
        "  t.approved_by                     AS t_approved_by, " +
        "  t.customer_monthly_invoice_id     AS t_customer_monthly_invoice_id, " +
        "  t.secretary_monthly_summary_id    AS t_secretary_monthly_summary_id, " +
        "  t.created_at                      AS t_created_at, " +
        "  t.updated_at                      AS t_updated_at, " +
        "  t.deleted_at                      AS t_deleted_at, " +

        // ---- assignments.* (16) ----
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
//        "  a.created_by                      AS a_created_by, " +
        "  a.created_at                      AS a_created_at, " +
        "  a.updated_at                      AS a_updated_at, " +
        "  a.deleted_at                      AS a_deleted_at, " +

        // ---- task_rank.rank_name (1) ----
        "  tr.rank_name                      AS tr_rank_name " +

        "FROM tasks t " +
        "JOIN assignments a ON a.id = t.assignment_id AND a.deleted_at IS NULL " +
        "LEFT JOIN task_rank tr ON tr.id = a.task_rank_id AND tr.deleted_at IS NULL " +
        "WHERE a.secretary_id = ? " +
        "  AND a.customer_id  = ? " +
        "  AND a.target_year_month = ? " +
        "  AND t.deleted_at IS NULL " +
        "ORDER BY t.start_time";
    
 // ---- 追加: 単一取得用SQL ----
    private static final String SQL_SELECT_BY_ID =
        "SELECT " +
        // tasks.*
        "  t.id                              AS t_id, " +
        "  t.assignment_id                   AS t_assignment_id, " +
        "  t.work_date                       AS t_work_date, " +
        "  t.start_time                      AS t_start_time, " +
        "  t.end_time                        AS t_end_time, " +
        "  t.work_minute                     AS t_work_minute, " +
        "  t.work_content                    AS t_work_content, " +
        "  t.approved_at                     AS t_approved_at, " +
        "  t.approved_by                     AS t_approved_by, " +
        "  t.customer_monthly_invoice_id     AS t_customer_monthly_invoice_id, " +
        "  t.secretary_monthly_summary_id    AS t_secretary_monthly_summary_id, " +
        "  t.created_at                      AS t_created_at, " +
        "  t.updated_at                      AS t_updated_at, " +
        "  t.deleted_at                      AS t_deleted_at, " +

        // assignments.*
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

        // task_rank
        "  tr.rank_name                      AS tr_rank_name " +

        "FROM tasks t " +
        "JOIN assignments a ON a.id = t.assignment_id AND a.deleted_at IS NULL " +
        "LEFT JOIN task_rank tr ON tr.id = a.task_rank_id AND tr.deleted_at IS NULL " +
        "WHERE t.id = ? AND t.deleted_at IS NULL";
    
    /** tasks INSERT（RETURNING id, created_at, updated_at） */
    private static final String SQL_INSERT =
        "INSERT INTO tasks (" +
        "  assignment_id, work_date, start_time, end_time, work_minute, work_content," +
        "  approved_at, approved_by, customer_monthly_invoice_id, secretary_monthly_summary_id" +
        ") VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id, created_at, updated_at";

    /** tasks UPDATE（論理未削除のみ） */
    private static final String SQL_UPDATE =
        "UPDATE tasks SET " +
        "  assignment_id = ?, " +
        "  work_date = ?, " +
        "  start_time = ?, " +
        "  end_time = ?, " +
        "  work_minute = ?, " +
        "  work_content = ?, " +
        "  approved_at = ?, " +
        "  approved_by = ?, " +
        "  customer_monthly_invoice_id = ?, " +
        "  secretary_monthly_summary_id = ?, " +
        "  updated_at = CURRENT_TIMESTAMP " +
        "WHERE id = ? AND deleted_at IS NULL";

    /** tasks 論理DELETE（deleted_at を現在時刻に） */
    private static final String SQL_DELETE_LOGICAL =
        "UPDATE tasks SET deleted_at = CURRENT_TIMESTAMP " +
        "WHERE id = ? AND deleted_at IS NULL";

    public TaskDAO(Connection conn) {
        super(conn);
    }

    // ========================
    // SELECT
    // ========================

    /**
     * 秘書ID・顧客ID・年月で tasks を取得します。
     * @param secretaryId 秘書ID
     * @param customerId 顧客ID
     * @param yearMonth "YYYY-MM"
     * @return List<TaskDTO>
     * @throws DAOException 取得時エラー
     */
    public List<TaskDTO> selectBySecretaryAndCustomerAndMonth(UUID secretaryId, UUID customerId, String yearMonth) {
        List<TaskDTO> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_SEC_CUST_MONTH)) {
            int p = 1;
            ps.setObject(p++, secretaryId);
            ps.setObject(p++, customerId);
            ps.setString(p++, yearMonth);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TaskDTO t = new TaskDTO();

                    // ---- tasks.* ----
                    t.setId(rs.getObject("t_id", UUID.class));

                    AssignmentDTO ad = new AssignmentDTO();
                    ad.setAssignmentId(rs.getObject("a_id", UUID.class)); // set now; also set below
                    t.setAssignment(ad);

                    t.setWorkDate(rs.getDate("t_work_date"));
                    t.setStartTime(rs.getTimestamp("t_start_time"));
                    t.setEndTime(rs.getTimestamp("t_end_time"));
                    t.setWorkMinute(rs.getObject("t_work_minute", Integer.class));
                    t.setWorkContent(rs.getString("t_work_content"));
                    t.setApprovedAt(rs.getTimestamp("t_approved_at"));

                    UUID approvedById = rs.getObject("t_approved_by", UUID.class);
                    if (approvedById != null) {
                        SecretaryDTO s = new SecretaryDTO();
                        s.setId(approvedById);
                        t.setApprovedBy(s);
                    }

                    UUID cmiId = rs.getObject("t_customer_monthly_invoice_id", UUID.class);
                    if (cmiId != null) {
                        CustomerMonthlyInvoiceDTO cmi = new CustomerMonthlyInvoiceDTO();
                        cmi.setId(cmiId);
                        t.setCustomerMonthlyInvoice(cmi);
                    }

                    UUID smsId = rs.getObject("t_secretary_monthly_summary_id", UUID.class);
                    if (smsId != null) {
                        SecretaryMonthlySummaryDTO sms = new SecretaryMonthlySummaryDTO();
                        sms.setId(smsId);
                        t.setSecretaryMonthlySummary(sms);
                    }

                    t.setCreatedAt(rs.getTimestamp("t_created_at"));
                    t.setUpdatedAt(rs.getTimestamp("t_updated_at"));
                    t.setDeletedAt(rs.getTimestamp("t_deleted_at"));

                    // ---- assignments.* を TaskDTO#assignment に詰める ----
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
//                    ad.setAssignmentCreatedBy(rs.getObject("a_created_by", UUID.class));
                    ad.setAssignmentCreatedAt(rs.getTimestamp("a_created_at"));
                    ad.setAssignmentUpdatedAt(rs.getTimestamp("a_updated_at"));
                    ad.setAssignmentDeletedAt(rs.getTimestamp("a_deleted_at"));

                    // ---- task_rank.rank_name ----
                    ad.setTaskRankName(rs.getString("tr_rank_name"));

                    list.add(t);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("E:TS11 tasks 取得に失敗しました。", e);
        }

        return list;
    }
    
 // ---- 追加: 単一取得メソッド ----
    public TaskDTO selectById(UUID taskId) {
        if (taskId == null) {
            throw new DAOException("E:TS10 id が未設定です。");
        }

        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_ID)) {
            ps.setObject(1, taskId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                TaskDTO t = new TaskDTO();

                // tasks.*
                t.setId(rs.getObject("t_id", UUID.class));

                AssignmentDTO ad = new AssignmentDTO();
                ad.setAssignmentId(rs.getObject("a_id", UUID.class));
                t.setAssignment(ad);

                t.setWorkDate(rs.getDate("t_work_date"));
                t.setStartTime(rs.getTimestamp("t_start_time"));
                t.setEndTime(rs.getTimestamp("t_end_time"));
                t.setWorkMinute(rs.getObject("t_work_minute", Integer.class));
                t.setWorkContent(rs.getString("t_work_content"));
                t.setApprovedAt(rs.getTimestamp("t_approved_at"));

                UUID approvedById = rs.getObject("t_approved_by", UUID.class);
                if (approvedById != null) {
                    SecretaryDTO s = new SecretaryDTO();
                    s.setId(approvedById);
                    t.setApprovedBy(s);
                }

                UUID cmiId = rs.getObject("t_customer_monthly_invoice_id", UUID.class);
                if (cmiId != null) {
                    CustomerMonthlyInvoiceDTO cmi = new CustomerMonthlyInvoiceDTO();
                    cmi.setId(cmiId);
                    t.setCustomerMonthlyInvoice(cmi);
                }

                UUID smsId = rs.getObject("t_secretary_monthly_summary_id", UUID.class);
                if (smsId != null) {
                    SecretaryMonthlySummaryDTO sms = new SecretaryMonthlySummaryDTO();
                    sms.setId(smsId);
                    t.setSecretaryMonthlySummary(sms);
                }

                t.setCreatedAt(rs.getTimestamp("t_created_at"));
                t.setUpdatedAt(rs.getTimestamp("t_updated_at"));
                t.setDeletedAt(rs.getTimestamp("t_deleted_at"));

                // assignments.* -> t.assignment
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

                // task_rank
                ad.setTaskRankName(rs.getString("tr_rank_name"));

                return t;
            }
        } catch (SQLException e) {
            throw new DAOException("E:TS12 tasks 単一取得に失敗しました。", e);
        }
    }
    
    /**
     * タスクを新規登録します。
     * 必須: assignment.id, workDate, startTime, endTime, workMinute, workContent
     * 任意: approvedAt/By, customerMonthlyInvoiceId, secretaryMonthlySummaryId
     *
     * @param dto TaskDTO
     * @return 採番された tasks.id
     */
    public UUID insert(TaskDTO dto) {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            int i = 1;

            // assignment_id
            UUID assignmentId = (dto.getAssignment() != null) ? dto.getAssignment().getAssignmentId() : null;
            if (assignmentId == null) {
                throw new DAOException("E:TS21 assignmentId が未設定です。");
            }
            ps.setObject(i++, assignmentId);

            // work_date / start_time / end_time / work_minute / work_content
            if (dto.getWorkDate() == null) throw new DAOException("E:TS22 workDate が未設定です。");
            ps.setDate(i++, new java.sql.Date(dto.getWorkDate().getTime()));

            ps.setTimestamp(i++, dto.getStartTime());
            ps.setTimestamp(i++, dto.getEndTime());

            if (dto.getWorkMinute() == null) throw new DAOException("E:TS23 workMinute が未設定です。");
            ps.setInt(i++, dto.getWorkMinute());

            ps.setString(i++, dto.getWorkContent());

            // approved_at / approved_by / customer_monthly_invoice_id / secretary_monthly_summary_id
            if (dto.getApprovedAt() != null) {
                ps.setTimestamp(i++, dto.getApprovedAt());
            } else {
                ps.setNull(i++, Types.TIMESTAMP);
            }

            if (dto.getApprovedBy() != null && dto.getApprovedBy().getId() != null) {
                ps.setObject(i++, dto.getApprovedBy().getId());
            } else {
                ps.setNull(i++, Types.OTHER);
            }

            if (dto.getCustomerMonthlyInvoice() != null && dto.getCustomerMonthlyInvoice().getId() != null) {
                ps.setObject(i++, dto.getCustomerMonthlyInvoice().getId());
            } else {
                ps.setNull(i++, Types.OTHER);
            }

            if (dto.getSecretaryMonthlySummary() != null && dto.getSecretaryMonthlySummary().getId() != null) {
                ps.setObject(i++, dto.getSecretaryMonthlySummary().getId());
            } else {
                ps.setNull(i++, Types.OTHER);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject(1, UUID.class); // id
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DAOException("E:TS24 tasks INSERT に失敗しました。", e);
        }
    }
    
    /**
     * タスクを更新します（論理未削除のみ対象）。
     * 必須: id, assignment.id, workDate, startTime, endTime, workMinute, workContent
     * 任意: approvedAt/By, customerMonthlyInvoiceId, secretaryMonthlySummaryId
     *
     * @param dto TaskDTO
     * @return 影響行数（0 の場合は未更新）
     */
    public int update(TaskDTO dto) {
        if (dto.getId() == null) {
            throw new DAOException("E:TS31 id が未設定です。");
        }
        UUID assignmentId = (dto.getAssignment() != null) ? dto.getAssignment().getAssignmentId() : null;
        if (assignmentId == null) {
            throw new DAOException("E:TS32 assignmentId が未設定です。");
        }
        if (dto.getWorkDate() == null) {
            throw new DAOException("E:TS33 workDate が未設定です。");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new DAOException("E:TS34 startTime/endTime が未設定です。");
        }
        if (dto.getWorkMinute() == null) {
            throw new DAOException("E:TS35 workMinute が未設定です。");
        }
        if (dto.getWorkContent() == null) {
            throw new DAOException("E:TS36 workContent が未設定です。");
        }

        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            int i = 1;

            // 必須
            ps.setObject(i++, assignmentId);
            ps.setDate(i++, new java.sql.Date(dto.getWorkDate().getTime()));
            ps.setTimestamp(i++, dto.getStartTime());
            ps.setTimestamp(i++, dto.getEndTime());
            ps.setInt(i++, dto.getWorkMinute());
            ps.setString(i++, dto.getWorkContent());

            // 任意（NULL可）
            if (dto.getApprovedAt() != null) {
                ps.setTimestamp(i++, dto.getApprovedAt());
            } else {
                ps.setNull(i++, Types.TIMESTAMP);
            }

            if (dto.getApprovedBy() != null && dto.getApprovedBy().getId() != null) {
                ps.setObject(i++, dto.getApprovedBy().getId());
            } else {
                ps.setNull(i++, Types.OTHER);
            }

            if (dto.getCustomerMonthlyInvoice() != null && dto.getCustomerMonthlyInvoice().getId() != null) {
                ps.setObject(i++, dto.getCustomerMonthlyInvoice().getId());
            } else {
                ps.setNull(i++, Types.OTHER);
            }

            if (dto.getSecretaryMonthlySummary() != null && dto.getSecretaryMonthlySummary().getId() != null) {
                ps.setObject(i++, dto.getSecretaryMonthlySummary().getId());
            } else {
                ps.setNull(i++, Types.OTHER);
            }

            // WHERE id = ? AND deleted_at IS NULL
            ps.setObject(i++, dto.getId());

            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:TS37 tasks UPDATE に失敗しました。", e);
        }
    }

    /**
     * タスクを論理削除します（deleted_at を現在時刻に更新）。
     * @param id tasks.id
     * @return 影響行数（0 の場合は未削除）
     */
    public int delete(UUID id) {
        if (id == null) {
            throw new DAOException("E:TS41 id が未設定です。");
        }
        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_LOGICAL)) {
            ps.setObject(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("E:TS42 tasks 論理DELETE に失敗しました。", e);
        }
    }
    
}
