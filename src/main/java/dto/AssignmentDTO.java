package dto;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

public class AssignmentDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	
	// customers
    private UUID customerId;                 // c.id
    private String customerCompanyCode;      // c.company_code
    private String customerCompanyName;      // c.company_name

    // assignments
    private UUID assignmentId;                                   // a.id
    private UUID assignmentCustomerId;                           // a.customer_id
    private UUID assignmentSecretaryId;                          // a.secretary_id
    private UUID taskRankId;                                     // a.task_rank_id
    private String targetYearMonth;                              // a.target_year_month (YYYY-MM)
    private BigDecimal basePayCustomer;                          // a.base_pay_customer
    private BigDecimal basePaySecretary;                         // a.base_pay_secretary
    private BigDecimal increaseBasePayCustomer;                  // a.increase_base_pay_customer
    private BigDecimal increaseBasePaySecretary;                 // a.increase_base_pay_secretary
    private BigDecimal customerBasedIncentiveForCustomer;        // a.customer_based_incentive_for_customer
    private BigDecimal customerBasedIncentiveForSecretary;       // a.customer_based_incentive_for_secretary
    private String assignmentStatus;                             // a.status
    private UUID assignmentCreatedBy;                            // a.created_by
    private Timestamp assignmentCreatedAt;                   // a.created_at
    private Timestamp assignmentUpdatedAt;                   // a.updated_at
    private Timestamp assignmentDeletedAt;                   // a.deleted_at (nullable)

    // task_rank
    private String taskRankName;            // tr.rank_name

    // secretaries
    private UUID secretaryId;               // s.id
    private UUID secretaryRankId;           // s.secretary_rank_id
    private String secretaryName;           // s.name
    private Boolean isPmSecretary;   

  
	// secretary_rank
    private String secretaryRankName;       // sr.rank_name
    

	public UUID getCustomerId() {
		return customerId;
	}

	public void setCustomerId(UUID customerId) {
		this.customerId = customerId;
	}

	public String getCustomerCompanyCode() {
		return customerCompanyCode;
	}

	public void setCustomerCompanyCode(String customerCompanyCode) {
		this.customerCompanyCode = customerCompanyCode;
	}

	public String getCustomerCompanyName() {
		return customerCompanyName;
	}

	public void setCustomerCompanyName(String customerCompanyName) {
		this.customerCompanyName = customerCompanyName;
	}

	public UUID getAssignmentId() {
		return assignmentId;
	}

	public void setAssignmentId(UUID assignmentId) {
		this.assignmentId = assignmentId;
	}

	public UUID getAssignmentCustomerId() {
		return assignmentCustomerId;
	}

	public void setAssignmentCustomerId(UUID assignmentCustomerId) {
		this.assignmentCustomerId = assignmentCustomerId;
	}

	public UUID getAssignmentSecretaryId() {
		return assignmentSecretaryId;
	}

	public void setAssignmentSecretaryId(UUID assignmentSecretaryId) {
		this.assignmentSecretaryId = assignmentSecretaryId;
	}

	public UUID getTaskRankId() {
		return taskRankId;
	}

	public void setTaskRankId(UUID taskRankId) {
		this.taskRankId = taskRankId;
	}

	public String getTargetYearMonth() {
		return targetYearMonth;
	}

	public void setTargetYearMonth(String targetYearMonth) {
		this.targetYearMonth = targetYearMonth;
	}

	public BigDecimal getBasePayCustomer() {
		return basePayCustomer;
	}

	public void setBasePayCustomer(BigDecimal basePayCustomer) {
		this.basePayCustomer = basePayCustomer;
	}

	public BigDecimal getBasePaySecretary() {
		return basePaySecretary;
	}

	public void setBasePaySecretary(BigDecimal basePaySecretary) {
		this.basePaySecretary = basePaySecretary;
	}

	public BigDecimal getIncreaseBasePayCustomer() {
		return increaseBasePayCustomer;
	}

	public void setIncreaseBasePayCustomer(BigDecimal increaseBasePayCustomer) {
		this.increaseBasePayCustomer = increaseBasePayCustomer;
	}

	public BigDecimal getIncreaseBasePaySecretary() {
		return increaseBasePaySecretary;
	}

	public void setIncreaseBasePaySecretary(BigDecimal increaseBasePaySecretary) {
		this.increaseBasePaySecretary = increaseBasePaySecretary;
	}

	public BigDecimal getCustomerBasedIncentiveForCustomer() {
		return customerBasedIncentiveForCustomer;
	}

	public void setCustomerBasedIncentiveForCustomer(BigDecimal customerBasedIncentiveForCustomer) {
		this.customerBasedIncentiveForCustomer = customerBasedIncentiveForCustomer;
	}

	public BigDecimal getCustomerBasedIncentiveForSecretary() {
		return customerBasedIncentiveForSecretary;
	}

	public void setCustomerBasedIncentiveForSecretary(BigDecimal customerBasedIncentiveForSecretary) {
		this.customerBasedIncentiveForSecretary = customerBasedIncentiveForSecretary;
	}

	public String getAssignmentStatus() {
		return assignmentStatus;
	}

	public void setAssignmentStatus(String assignmentStatus) {
		this.assignmentStatus = assignmentStatus;
	}

	public UUID getAssignmentCreatedBy() {
		return assignmentCreatedBy;
	}

	public void setAssignmentCreatedBy(UUID assignmentCreatedBy) {
		this.assignmentCreatedBy = assignmentCreatedBy;
	}

	public Timestamp getAssignmentCreatedAt() {
		return assignmentCreatedAt;
	}

	public void setAssignmentCreatedAt(Timestamp assignmentCreatedAt) {
		this.assignmentCreatedAt = assignmentCreatedAt;
	}

	public Timestamp getAssignmentUpdatedAt() {
		return assignmentUpdatedAt;
	}

	public void setAssignmentUpdatedAt(Timestamp assignmentUpdatedAt) {
		this.assignmentUpdatedAt = assignmentUpdatedAt;
	}

	public Timestamp getAssignmentDeletedAt() {
		return assignmentDeletedAt;
	}

	public void setAssignmentDeletedAt(Timestamp assignmentDeletedAt) {
		this.assignmentDeletedAt = assignmentDeletedAt;
	}

	public String getTaskRankName() {
		return taskRankName;
	}

	public void setTaskRankName(String taskRankName) {
		this.taskRankName = taskRankName;
	}

	public UUID getSecretaryId() {
		return secretaryId;
	}

	public void setSecretaryId(UUID secretaryId) {
		this.secretaryId = secretaryId;
	}

	public UUID getSecretaryRankId() {
		return secretaryRankId;
	}

	public void setSecretaryRankId(UUID secretaryRankId) {
		this.secretaryRankId = secretaryRankId;
	}

	public String getSecretaryName() {
		return secretaryName;
	}

	public void setSecretaryName(String secretaryName) {
		this.secretaryName = secretaryName;
	}

	public String getSecretaryRankName() {
		return secretaryRankName;
	}

	public void setSecretaryRankName(String secretaryRankName) {
		this.secretaryRankName = secretaryRankName;
	}
	  public Boolean getIsPmSecretary() {
			return isPmSecretary;
		}

		public void setIsPmSecretary(Boolean isPmSecretary) {
			this.isPmSecretary = isPmSecretary;
		}

	
}

