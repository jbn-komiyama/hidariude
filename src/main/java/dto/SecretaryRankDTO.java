package dto;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * 秘書ランク（secretary_rank）テーブルのDTO
 */
public class SecretaryRankDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private UUID id;
	private String rankName;
	private String description;
	private BigDecimal increaseBasePayCustomer;
	private BigDecimal increaseBasePaySecretary;
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
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
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