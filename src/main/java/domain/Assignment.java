package domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Assignment implements Serializable{
	private static final long serialVersionUID = 1L;
	
	// --- assignments ---
    private UUID id;
    private UUID customerId;
    private UUID secretaryId;
    private UUID taskRankId;
    /** YYYY-MM */
    private String targetYearMonth;
    private BigDecimal basePayCustomer;
    private BigDecimal basePaySecretary;
    private BigDecimal increaseBasePayCustomer;
    private BigDecimal increaseBasePaySecretary;
    private BigDecimal customerBasedIncentiveForCustomer;
    private BigDecimal customerBasedIncentiveForSecretary;
    private String status;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private String taskRankName;

    // --- relations (domain objects) ---
    private Customer customer;
    private Secretary secretary;
    
    
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public UUID getCustomerId() {
		return customerId;
	}
	public void setCustomerId(UUID customerId) {
		this.customerId = customerId;
	}
	public UUID getSecretaryId() {
		return secretaryId;
	}
	public void setSecretaryId(UUID secretaryId) {
		this.secretaryId = secretaryId;
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
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public UUID getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(UUID createdBy) {
		this.createdBy = createdBy;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
	public LocalDateTime getDeletedAt() {
		return deletedAt;
	}
	public void setDeletedAt(LocalDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}
	public String getTaskRankName() {
		return taskRankName;
	}
	public void setTaskRankName(String taskRankName) {
		this.taskRankName = taskRankName;
	}
	public Customer getCustomer() {
		return customer;
	}
	public void setCustomer(Customer customer) {
		this.customer = customer;
	}
	public Secretary getSecretary() {
		return secretary;
	}
	public void setSecretary(Secretary secretary) {
		this.secretary = secretary;
	}
    
}