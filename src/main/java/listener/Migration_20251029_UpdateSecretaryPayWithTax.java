package listener;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 秘書向け金額を税込み（+10%）に更新するマイグレーション
 * 
 * 更新対象:
 *   ・secretary_rank.increase_base_pay_secretary を1.1倍に更新
 *   ・task_rank.base_pay_secretary を1.1倍に更新
 * 
 * 実行日: 2025-10-29
 */
public class Migration_20251029_UpdateSecretaryPayWithTax implements Migration {
    
    @Override
    public void up(Connection conn) throws SQLException {
        System.out.println("  [Migration] Updating secretary pay amounts to include 10% tax...");
        
        // 1. 秘書ランクマスタの increase_base_pay_secretary を1.1倍に更新
        String sql1 = "UPDATE secretary_rank " +
                      "SET increase_base_pay_secretary = ROUND(increase_base_pay_secretary * 1.1, 2), " +
                      "    updated_at = CURRENT_TIMESTAMP " +
                      "WHERE deleted_at IS NULL";
        
        try (Statement stmt = conn.createStatement()) {
            int count1 = stmt.executeUpdate(sql1);
            System.out.println("    - Updated " + count1 + " secretary rank records (increase_base_pay_secretary * 1.1)");
        }
        
        // 2. 業務ランクマスタの base_pay_secretary を1.1倍に更新
        String sql2 = "UPDATE task_rank " +
                      "SET base_pay_secretary = ROUND(base_pay_secretary * 1.1, 2), " +
                      "    updated_at = CURRENT_TIMESTAMP " +
                      "WHERE deleted_at IS NULL";
        
        try (Statement stmt = conn.createStatement()) {
            int count2 = stmt.executeUpdate(sql2);
            System.out.println("    - Updated " + count2 + " task rank records (base_pay_secretary * 1.1)");
        }
        
        System.out.println("  [Migration] Secretary pay amounts updated successfully.");
    }
    
    @Override
    public String getDescription() {
        return "秘書向け金額を税込み（+10%）に更新";
    }
}

