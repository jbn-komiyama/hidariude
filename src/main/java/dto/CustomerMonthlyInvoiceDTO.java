package dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

public class CustomerMonthlyInvoiceDTO  implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private UUID id;
    private CustomerDTO customer;
    private String targetYearMonth;
    private BigDecimal totalAmount;
    private Integer totalTasksCount;
    private Integer totalWorkTime;
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
	public CustomerDTO getCustomer() {
		return customer;
	}
	public void setCustomer(CustomerDTO customer) {
		this.customer = customer;
	}
	public String getTargetYearMonth() {
		return targetYearMonth;
	}
	public void setTargetYearMonth(String targetYearMonth) {
		this.targetYearMonth = targetYearMonth;
	}
	public BigDecimal getTotalAmount() {
		return totalAmount;
	}
	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
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