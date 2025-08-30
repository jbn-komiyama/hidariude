package domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class CustomerMonthlyInvoice  implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private Customer customer;
    private String targetYearMonth;
    private BigDecimal totalAmount;
    private Integer totalTasksCount;
    private Integer totalWorkTime;
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
	public Customer getCustomer() {
		return customer;
	}
	public void setCustomer(Customer customer) {
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