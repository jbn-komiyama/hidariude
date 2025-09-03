package domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class Task implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private UUID id;
    private Assignment assignment;
    private Date workDate;
    private Date startTime;
    private Date endTime;
    private Integer workMinute;
    private String workContent;
    private Date approvedAt;
    private Secretary approvedBy;
    private CustomerMonthlyInvoice customerMonthlyInvoice;
    private SecretaryMonthlySummary secretaryMonthlySummary;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;
    private BigDecimal hourFee;
    private BigDecimal fee;
    
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public Assignment getAssignment() {
		return assignment;
	}
	public void setAssignment(Assignment assignment) {
		this.assignment = assignment;
	}
	public Date getWorkDate() {
		return workDate;
	}
	public void setWorkDate(Date workDate) {
		this.workDate = workDate;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public Date getEndTime() {
		return endTime;
	}
	public void setEndTime(Date endTime) {
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
	public Date getApprovedAt() {
		return approvedAt;
	}
	public void setApprovedAt(Date approvedAt) {
		this.approvedAt = approvedAt;
	}
	public Secretary getApprovedBy() {
		return approvedBy;
	}
	public void setApprovedBy(Secretary approvedBy) {
		this.approvedBy = approvedBy;
	}
	public CustomerMonthlyInvoice getCustomerMonthlyInvoice() {
		return customerMonthlyInvoice;
	}
	public void setCustomerMonthlyInvoice(CustomerMonthlyInvoice customerMonthlyInvoice) {
		this.customerMonthlyInvoice = customerMonthlyInvoice;
	}
	public SecretaryMonthlySummary getSecretaryMonthlySummary() {
		return secretaryMonthlySummary;
	}
	public void setSecretaryMonthlySummary(SecretaryMonthlySummary secretaryMonthlySummary) {
		this.secretaryMonthlySummary = secretaryMonthlySummary;
	}
	public Date getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	public Date getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	public Date getDeletedAt() {
		return deletedAt;
	}
	public void setDeletedAt(Date deletedAt) {
		this.deletedAt = deletedAt;
	}
	public BigDecimal getFee() {
		return fee;
	}
	public void setFee(BigDecimal fee) {
		this.fee = fee;
	}
	public BigDecimal getHourFee() {
		return hourFee;
	}
	public void setHourFee(BigDecimal hourFee) {
		this.hourFee = hourFee;
		BigDecimal fee = hourFee.multiply(BigDecimal.valueOf((double)workMinute/ 60));
		this.fee = fee;
	}
    
    
}
