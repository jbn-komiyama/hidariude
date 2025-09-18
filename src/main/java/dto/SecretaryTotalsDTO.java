// dto/SecretaryTotalsDTO.java
package dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class SecretaryTotalsDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal totalSecretaryAmount; // 請求合計金額
    private Integer totalTasksCount;         // 件数
    private Integer totalWorkTime;           // 総時間(分想定)

    public BigDecimal getTotalSecretaryAmount() { return totalSecretaryAmount; }
    public void setTotalSecretaryAmount(BigDecimal v) { this.totalSecretaryAmount = v; }
    public Integer getTotalTasksCount() { return totalTasksCount; }
    public void setTotalTasksCount(Integer v) { this.totalTasksCount = v; }
    public Integer getTotalWorkTime() { return totalWorkTime; }
    public void setTotalWorkTime(Integer v) { this.totalWorkTime = v; }
}
