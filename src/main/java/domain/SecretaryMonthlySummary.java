package domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class SecretaryMonthlySummary implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private UUID id;
    private Secretary secretary;
    private String targetYearMonth;
    private BigDecimal totalSecretaryAmount;
    private Integer totalTasksCount;
    private Integer totalWorkTime;
    private Date finalizedAt;
    private String status;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;
    
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public Secretary getSecretary() {
		return secretary;
	}
	public void setSecretary(Secretary secretary) {
		this.secretary = secretary;
	}
	public String getTargetYearMonth() {
		return targetYearMonth;
	}
	public void setTargetYearMonth(String targetYearMonth) {
		this.targetYearMonth = targetYearMonth;
	}
	public BigDecimal getTotalSecretaryAmount() {
		return totalSecretaryAmount;
	}
	public void setTotalSecretaryAmount(BigDecimal totalSecretaryAmount) {
		this.totalSecretaryAmount = totalSecretaryAmount;
	}
	public Integer getTotalTasksCount() {
		return totalTasksCount;
	}
	public void setTotalTasksCount(Integer totalTasksCount) {
		this.totalTasksCount = totalTasksCount;
	}
	public Integer getTotalWorkTime() {
		return totalWorkTime;
	}
	public void setTotalWorkTime(Integer totalWorkTime) {
		this.totalWorkTime = totalWorkTime;
	}
	public Date getFinalizedAt() {
		return finalizedAt;
	}
	public void setFinalizedAt(Date finalizedAt) {
		this.finalizedAt = finalizedAt;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
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
    
}