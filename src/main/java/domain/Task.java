package domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private BigDecimal hourFeeCustomer;
    private BigDecimal feeCustomer;
    private static final BigDecimal SIXTY = BigDecimal.valueOf(60);
    
    private int unapproved;
    private int approved;
    private int remanded;
    private int total;
    
    private static BigDecimal calcFee(BigDecimal hourly, Integer minutes) {
        if (hourly == null || minutes == null) return null;
        // 端数は要件に合わせて（ここでは 0 桁 HALF_UP）
        return hourly.multiply(BigDecimal.valueOf(minutes))
                     .divide(SIXTY, 0, RoundingMode.HALF_UP);
    }
    private void recalcFees() {
        this.fee         = calcFee(this.hourFee, this.workMinute);
        this.feeCustomer = calcFee(this.hourFeeCustomer, this.workMinute);
    }
    
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
        recalcFees();
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
        recalcFees();
    }
	public BigDecimal getHourFeeCustomer() {
		return hourFeeCustomer;
	}
	public void setHourFeeCustomer(BigDecimal hourFeeCustomer) {
        this.hourFeeCustomer = hourFeeCustomer;
        recalcFees();
    }
	public BigDecimal getFeeCustomer() {
		return feeCustomer;
	}
	public void setFeeCustomer(BigDecimal feeCustomer) {
		this.feeCustomer = feeCustomer;
	}
    
	private Date remandedAt;
	private UUID remandedById;
	private String remandComment;

	public Date getRemandedAt() { return remandedAt; }
	public void setRemandedAt(Date remandedAt) { this.remandedAt = remandedAt; }
	public UUID getRemandedById() { return remandedById; }
	public void setRemandedById(UUID remandedById) { this.remandedById = remandedById; }
	public String getRemandComment() { return remandComment; }
	public void setRemandComment(String remandComment) { this.remandComment = remandComment; }
	
	public int getUnapproved() {
		return unapproved;
	}
	public void setUnapproved(int unapproved) {
		this.unapproved = unapproved;
	}
	public int getApproved() {
		return approved;
	}
	public void setApproved(int approved) {
		this.approved = approved;
	}
	public int getRemanded() {
		return remanded;
	}
	public void setRemanded(int remanded) {
		this.remanded = remanded;
	}
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	
	private BigDecimal sumAmountApproved; // 承認済み合計
	private BigDecimal sumAmountAll;      // 全件合計(必要なら)

	public BigDecimal getSumAmountApproved() { return sumAmountApproved; }
	public void setSumAmountApproved(BigDecimal v) { this.sumAmountApproved = v; }
	public BigDecimal getSumAmountAll() { return sumAmountAll; }
	public void setSumAmountAll(BigDecimal v) { this.sumAmountAll = v; }
	private boolean hasRemander;
	public boolean isHasRemander() { return hasRemander; }
	public void setHasRemander(boolean b) { this.hasRemander = b; }

}
