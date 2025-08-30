package domain;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class SecretaryRank {
    private UUID id;
    private String rankName;
    private String description;
    private BigDecimal increaseBasePayCustomer;
    private BigDecimal increaseBasePaySecretary;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public SecretaryRank() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRankName() { return rankName; }
    public void setRankName(String rankName) { this.rankName = rankName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getIncreaseBasePayCustomer() { return increaseBasePayCustomer; }
    public void setIncreaseBasePayCustomer(BigDecimal increaseBasePayCustomer) { this.increaseBasePayCustomer = increaseBasePayCustomer; }

    public BigDecimal getIncreaseBasePaySecretary() { return increaseBasePaySecretary; }
    public void setIncreaseBasePaySecretary(BigDecimal increaseBasePaySecretary) { this.increaseBasePaySecretary = increaseBasePaySecretary; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}