package domain;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 秘書の通算サマリ値を保持するドメインモデル
 */
public class SecretaryTotals implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal totalSecretaryAmount;
    private Integer totalTasksCount;
    private Integer totalWorkTime;

    public BigDecimal getTotalSecretaryAmount() { return totalSecretaryAmount; }
    public void setTotalSecretaryAmount(BigDecimal v) { this.totalSecretaryAmount = v; }
    public Integer getTotalTasksCount() { return totalTasksCount; }
    public void setTotalTasksCount(Integer v) { this.totalTasksCount = v; }
    public Integer getTotalWorkTime() { return totalWorkTime; }
    public void setTotalWorkTime(Integer v) { this.totalWorkTime = v; }
}
