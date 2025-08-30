package domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

public class TaskRank implements Serializable{
	private static final long serialVersionUID = 1L;
	
    private UUID id;
    private String rankName;
    private BigDecimal basePayCustomer;
    private BigDecimal basePaySecretary;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;
    
    
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public String getRankName() {
		return rankName;
	}
	public void setRankName(String rankName) {
		this.rankName = rankName;
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
