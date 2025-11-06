package listener;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 秘書向け金額を税込み前（元の金額）に戻すマイグレーション
 * 
 * 更新対象:
 *   - secretary_rank.increase_base_pay_secretary を1.1で割って元の金額に戻す
 *   - task_rank.base_pay_secretary を1.1で割って元の金額に戻す
 * 
 * 実行日: 2025-10-30
 */
public class Migration_20251030_RevertSecretaryPayWithTax implements Migration {
    
    @Override
    public void up(Connection conn) throws SQLException {
        System.out.println("  [Migration] Reverting secretary pay amounts to original (without tax)...");
        
        /** 1. 秘書ランクマスタの increase_base_pay_secretary を1.1で割って元に戻す */
        String sql1 = "UPDATE secretary_rank " +
                      "SET increase_base_pay_secretary = ROUND(increase_base_pay_secretary / 1.1, 2), " +
                      "    updated_at = CURRENT_TIMESTAMP " +
                      "WHERE deleted_at IS NULL";
        
        try (Statement stmt = conn.createStatement()) {
            int count1 = stmt.executeUpdate(sql1);
            System.out.println("    - Updated " + count1 + " secretary rank records (increase_base_pay_secretary / 1.1)");
        }
        
        /** 2. 業務ランクマスタの base_pay_secretary を1.1で割って元に戻す */
        String sql2 = "UPDATE task_rank " +
                      "SET base_pay_secretary = ROUND(base_pay_secretary / 1.1, 2), " +
                      "    updated_at = CURRENT_TIMESTAMP " +
                      "WHERE deleted_at IS NULL";
        
        try (Statement stmt = conn.createStatement()) {
            int count2 = stmt.executeUpdate(sql2);
            System.out.println("    - Updated " + count2 + " task rank records (base_pay_secretary / 1.1)");
        }
        
        System.out.println("  [Migration] Secretary pay amounts reverted successfully.");
    }
    
    @Override
    public String getDescription() {
        return "秘書向け金額を税込み前（元の金額）に戻す";
    }
}

