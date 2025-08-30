package dto;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

public class TaskRankDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	
	// customers
    private UUID id;  
    private String rankName;
    private BigDecimal basePayCustomer;
    private BigDecimal basePaySecretary;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;
    
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

