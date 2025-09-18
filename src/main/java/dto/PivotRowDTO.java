// dto/PivotRowDTO.java
package dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** ピボット1行分（顧客/秘書） */
public class PivotRowDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String label; // 会社名 or 秘書名
    private Map<String, BigDecimal> amountByYm = new LinkedHashMap<>(); // ym -> 金額
    private BigDecimal rowTotal = BigDecimal.ZERO;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Map<String, BigDecimal> getAmountByYm() { return amountByYm; }
    public void setAmountByYm(Map<String, BigDecimal> amountByYm) { this.amountByYm = amountByYm; }
    public BigDecimal getRowTotal() { return rowTotal; }
    public void setRowTotal(BigDecimal rowTotal) { this.rowTotal = rowTotal; }
}
