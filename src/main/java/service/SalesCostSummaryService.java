// service/SalesCostSummeryService.java（要件版）
package service;

import java.time.LocalDate;
import java.util.*;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;

import jakarta.servlet.http.HttpServletRequest;

import dao.TransactionManager;
import dao.CustomerMonthlyInvoiceDAO;
import dao.SecretaryMonthlySummaryDAO;
import dto.PivotRowDTO;

public class SalesCostSummaryService extends BaseService {

    private static final String VIEW_SALES = "summary/admin/sales";
    private static final String VIEW_COSTS = "summary/admin/costs";

    // Attr keys
    private static final String A_MONTHS   = "months";      // List<String> YYYY-MM（4月→翌3月）
    private static final String A_FY       = "fy";          // 表示年度
    private static final String A_ROWS     = "rows";        // List<PivotRowDTO>
    private static final String A_COLTOTAL = "colTotals";   // Map<ym, BigDecimal>
    private static final String A_GRAND    = "grandTotal";  // BigDecimal

    // Param
    private static final String P_FY = "fy";

    public SalesCostSummaryService(HttpServletRequest req, boolean useDB) { super(req, useDB); }

    /** 顧客×月の売上サマリー */
    public String salesSummary() {
        try (TransactionManager tm = new TransactionManager()) {
            int fy = resolveFiscalYear(param(P_FY));
            List<String> months = buildFiscalMonths(fy);

            String fromYm = months.get(0);
            String toYm   = months.get(months.size()-1);

            CustomerMonthlyInvoiceDAO dao = new CustomerMonthlyInvoiceDAO(tm.getConnection());
            List<PivotRowDTO> rows = dao.selectSalesByCustomerMonth(fromYm, toYm, months);

            Map<String, BigDecimal> colTotals = calcColTotals(months, rows);
            BigDecimal grand = calcGrand(colTotals);

            req.setAttribute(A_FY, fy);
            req.setAttribute(A_MONTHS, months);
            req.setAttribute(A_ROWS, rows);
            req.setAttribute(A_COLTOTAL, colTotals);
            req.setAttribute(A_GRAND, grand);
            return VIEW_SALES;
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute("errorMsg", validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /** 秘書×月の支出サマリー */
    public String costSummary() {
        try (TransactionManager tm = new TransactionManager()) {
            int fy = resolveFiscalYear(param(P_FY));
            List<String> months = buildFiscalMonths(fy);

            String fromYm = months.get(0);
            String toYm   = months.get(months.size()-1);

            SecretaryMonthlySummaryDAO dao = new SecretaryMonthlySummaryDAO(tm.getConnection());
            List<PivotRowDTO> rows = dao.selectCostsBySecretaryMonth(fromYm, toYm, months);

            Map<String, BigDecimal> colTotals = calcColTotals(months, rows);
            BigDecimal grand = calcGrand(colTotals);

            req.setAttribute(A_FY, fy);
            req.setAttribute(A_MONTHS, months);
            req.setAttribute(A_ROWS, rows);
            req.setAttribute(A_COLTOTAL, colTotals);
            req.setAttribute(A_GRAND, grand);
            return VIEW_COSTS;
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute("errorMsg", validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // ===== helper =====
    private String param(String n){ return req.getParameter(n); }

    /** 4月開始の会計年度（fy）を決める。param優先、なければ今日基準。 */
    private int resolveFiscalYear(String paramFy){
        if (paramFy != null && !paramFy.isBlank()) {
            try { return Integer.parseInt(paramFy); } catch (NumberFormatException ignore) {}
        }
        LocalDate today = LocalDate.now();
        return (today.getMonthValue() >= 4) ? today.getYear() : today.getYear()-1;
    }

    /** 指定FYの12か月（4月→翌3月） */
    private List<String> buildFiscalMonths(int fy){
        List<String> list = new ArrayList<>(12);
        for (int i=0;i<12;i++){
            int month = 4 + i; // 4..15
            int year  = fy + (month-1)/12;
            int calM  = ((month-1)%12)+1;
            list.add(String.format("%04d-%02d", year, calM));
        }
        return list;
    }

    private Map<String, BigDecimal> calcColTotals(List<String> months, List<PivotRowDTO> rows){
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (String m : months) totals.put(m, BigDecimal.ZERO);
        for (PivotRowDTO r : rows) {
            for (String m : months) {
                java.math.BigDecimal v = r.getAmountByYm().getOrDefault(m, BigDecimal.ZERO);
                totals.put(m, totals.get(m).add(v));
            }
        }
        return totals;
    }

    private BigDecimal calcGrand(Map<String, BigDecimal> colTotals){
        BigDecimal g = BigDecimal.ZERO;
        for (BigDecimal v : colTotals.values()) g = g.add(v);
        return g;
    }
}
