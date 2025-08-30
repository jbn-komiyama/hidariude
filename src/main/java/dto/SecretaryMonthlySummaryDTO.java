package dto;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

public class SecretaryMonthlySummaryDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private UUID id;
    private SecretaryDTO secretary;
    private String targetYearMonth;
    private BigDecimal totalSecretaryAmount;
    private Integer totalTasksCount;
    private Integer totalWorkTime;
    private Timestamp finalizedAt;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;
    
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public SecretaryDTO getSecretary() {
		return secretary;
	}
	public void setSecretary(SecretaryDTO secretary) {
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
	public Timestamp getFinalizedAt() {
		return finalizedAt;
	}
	public void setFinalizedAt(Timestamp finalizedAt) {
		this.finalizedAt = finalizedAt;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
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

