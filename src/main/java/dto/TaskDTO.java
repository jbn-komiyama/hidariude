package dto;
import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;

public class TaskDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private UUID id;
    private AssignmentDTO assignment;
    private Date workDate;
    private Timestamp startTime;
    private Timestamp endTime;
    private Integer workMinute;
    private String workContent;
    private Timestamp approvedAt;
    private SecretaryDTO approvedBy;
    private CustomerMonthlyInvoiceDTO customerMonthlyInvoice;
    private SecretaryMonthlySummaryDTO secretaryMonthlySummary;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;
    
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public AssignmentDTO getAssignment() {
		return assignment;
	}
	public void setAssignment(AssignmentDTO assignment) {
		this.assignment = assignment;
	}
	public Date getWorkDate() {
		return workDate;
	}
	public void setWorkDate(Date workDate) {
		this.workDate = workDate;
	}
	public Timestamp getStartTime() {
		return startTime;
	}
	public void setStartTime(Timestamp startTime) {
		this.startTime = startTime;
	}
	public Timestamp getEndTime() {
		return endTime;
	}
	public void setEndTime(Timestamp endTime) {
		this.endTime = endTime;
	}
	public Integer getWorkMinute() {
		return workMinute;
	}
	public void setWorkMinute(Integer workMinute) {
		this.workMinute = workMinute;
	}
	public String getWorkContent() {
		return workContent;
	}
	public void setWorkContent(String workContent) {
		this.workContent = workContent;
	}
	public Timestamp getApprovedAt() {
		return approvedAt;
	}
	public void setApprovedAt(Timestamp approvedAt) {
		this.approvedAt = approvedAt;
	}
	public SecretaryDTO getApprovedBy() {
		return approvedBy;
	}
	public void setApprovedBy(SecretaryDTO approvedBy) {
		this.approvedBy = approvedBy;
	}
	public CustomerMonthlyInvoiceDTO getCustomerMonthlyInvoice() {
		return customerMonthlyInvoice;
	}
	public void setCustomerMonthlyInvoice(CustomerMonthlyInvoiceDTO customerMonthlyInvoice) {
		this.customerMonthlyInvoice = customerMonthlyInvoice;
	}
	public SecretaryMonthlySummaryDTO getSecretaryMonthlySummary() {
		return secretaryMonthlySummary;
	}
	public void setSecretaryMonthlySummary(SecretaryMonthlySummaryDTO secretaryMonthlySummary) {
		this.secretaryMonthlySummary = secretaryMonthlySummary;
	}
	public Timestamp getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}
	public Timestamp getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(Timestamp updatedAt) {
		this.updatedAt = updatedAt;
	}
	public Timestamp getDeletedAt() {
		return deletedAt;
	}
	public void setDeletedAt(Timestamp deletedAt) {
		this.deletedAt = deletedAt;
	}
    
    
}

