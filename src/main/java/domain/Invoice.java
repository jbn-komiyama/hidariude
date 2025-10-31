package domain;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * 請求情報を集約したドメインモデル
 */
public class Invoice implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/** customers */
    private UUID customerId;                 /** c.id */
    private String customerCompanyName;      /** c.company_name */

    /** tasks */
    private int totalMinute;
    
    /** assignments */
    private BigDecimal hourlyPay;                  
    private String targetYM;
    
    /** task_rank */
    private String taskRankName;
    
    private BigDecimal fee;

	private BigDecimal totalFee = BigDecimal.ZERO;
	
	
	public UUID getCustomerId() {
		return customerId;
	}

	public void setCustomerId(UUID customerId) {
		this.customerId = customerId;
	}

	public String getCustomerCompanyName() {
		return customerCompanyName;
	}

	public void setCustomerCompanyName(String customerCompanyName) {
		this.customerCompanyName = customerCompanyName;
	}

	public int getTotalMinute() {
		return totalMinute;
	}

	public void setTotalMinute(int totalMinute) {
		this.totalMinute = totalMinute;
	}

	public BigDecimal getHourlyPay() {
		return hourlyPay;
	}

	public void setHourlyPay(BigDecimal hourlyPay) {
		this.hourlyPay = hourlyPay;
	}

	public String getTargetYM() {
		return targetYM;
	}

	public void setTargetYM(String targetYM) {
		this.targetYM = targetYM;
	}

	public String getTaskRankName() {
		return taskRankName;
	}

	public void setTaskRankName(String taskRankName) {
		this.taskRankName = taskRankName;
	}

	public BigDecimal getFee() {
		return fee;
	}

	public void setFee(BigDecimal fee) {
		this.fee = fee;
		this.totalFee = totalFee.add(fee);
	}

	public BigDecimal getTotalFee() {
		return totalFee;
	}

	public void setTotalFee(BigDecimal totalFee) {
		this.totalFee = totalFee;
	}
	
}

