package dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 秘書の通算サマリ値を保持するDTO
 */
public class SecretaryTotalsDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 請求合計金額 */
    private BigDecimal totalSecretaryAmount;
    /** 件数 */
    private Integer totalTasksCount;
    /** 総時間(分想定) */
    private Integer totalWorkTime;

    public BigDecimal getTotalSecretaryAmount() { return totalSecretaryAmount; }
    public void setTotalSecretaryAmount(BigDecimal v) { this.totalSecretaryAmount = v; }
    public Integer getTotalTasksCount() { return totalTasksCount; }
    public void setTotalTasksCount(Integer v) { this.totalTasksCount = v; }
    public Integer getTotalWorkTime() { return totalWorkTime; }
    public void setTotalWorkTime(Integer v) { this.totalWorkTime = v; }
}
