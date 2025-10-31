package service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dao.CustomerMonthlyInvoiceDAO;
import dao.SecretaryMonthlySummaryDAO;
import dao.TransactionManager;
import dto.PivotRowDTO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 売上（顧客×月）・コスト（秘書×月）の会計年度（4月開始）サマリーを提供するサービス。
 *
 * コントローラ（FrontController）からの入口は以下の2メソッドのみ:
 * - {@link #salesSummary()}  … /admin/summary/sales 用（顧客×月売上）
 * - {@link #costSummary()}   … /admin/summary/costs 用（秘書×月コスト）
 *
 * 画面へは以下の属性を渡します（いずれも変更なし）:
 * - {@code "months"}（A_MONTHS）: List&lt;String&gt; … 表示対象の年月（YYYY-MM）12か月（4月→翌3月）
 * - {@code "fy"}（A_FY）: int … 会計年度（4月開始）
 * - {@code "rows"}（A_ROWS）: List&lt;PivotRowDTO&gt; … ピボット行（顧客別or秘書別）
 * - {@code "colTotals"}（A_COLTOTAL）: Map&lt;ym, BigDecimal&gt; … 列合計（各月の合計）
 * - {@code "grandTotal"}（A_GRAND）: BigDecimal … 総合計
 *
 * エラー時は {@code "errorMsg"} にメッセージを詰めてエラーページへ遷移します（キー名は既存仕様を踏襲）。
 */
public class SalesCostSummaryService extends BaseService {

    /**
     * ① 定数・共通化（パス／パラメータ名／属性名）
     */

    /** ビュー: 売上（顧客×月） */
    private static final String VIEW_SALES = "summary/admin/sales";
    /** ビュー: コスト（秘書×月） */
    private static final String VIEW_COSTS = "summary/admin/costs";

    /**
     * Request Attribute keys（JSP で参照）
     * List&lt;String&gt; YYYY-MM（4月→翌3月）: months
     * 表示年度: fy
     * List&lt;PivotRowDTO&gt;: rows
     * Map&lt;ym, BigDecimal&gt;: colTotals
     * BigDecimal: grandTotal
     */
    private static final String A_MONTHS   = "months";
    private static final String A_FY       = "fy";
    private static final String A_ROWS     = "rows";
    private static final String A_COLTOTAL = "colTotals";
    private static final String A_GRAND    = "grandTotal";

    /**
     * Request Parameter names（受取）
     * 例: 2025（4月開始のFY）
     */
    private static final String P_FY = "fy";

    /**
     * ② フィールド・コンストラクタ
     */

    /**
     * コンストラクタ。
     * @param req   リクエスト
     * @param useDB DB接続フラグ（BaseService踏襲・現状未使用）
     */
    public SalesCostSummaryService(HttpServletRequest req, boolean useDB) {
        super(req, useDB);
    }

    /**
     * ③ コントローラ呼び出しメソッド（admin 用）
     */

    /**
     * 「【admin】 機能：売上サマリー（顧客×月）」
     */
    /**
     * 「顧客×月の売上サマリー」を表示する。
     * - request param {@code 'fy'}：会計年度（4月開始）。未指定なら今日からFYを推定。
     * - 対象FYの12か月（4月→翌3月）を生成し、範囲に対して集計クエリを実行。
     * - 列合計と総合計を計算して JSP に渡す。
     *
     * @return ビュー名 {@value VIEW_SALES}（エラー時はエラーページ）
     */
    public String salesSummary() {
        try (TransactionManager tm = new TransactionManager()) {
            final int fy = resolveFiscalYear(req.getParameter(P_FY));
            final List<String> months = buildFiscalMonths(fy);

            /** 期間（最初と最後のYM） */
            final String fromYm = months.get(0);
            final String toYm   = months.get(months.size() - 1);

            /** DAO 呼び出し：顧客×月の売上ピボット */
            CustomerMonthlyInvoiceDAO dao = new CustomerMonthlyInvoiceDAO(tm.getConnection());
            List<PivotRowDTO> rows = dao.selectSalesByCustomerMonth(fromYm, toYm, months);

            /** 列合計・総合計 */
            Map<String, BigDecimal> colTotals = calcColTotals(months, rows);
            BigDecimal grand = calcGrand(colTotals);

            /** JSP へ */
            req.setAttribute(A_FY, fy);
            req.setAttribute(A_MONTHS, months);
            req.setAttribute(A_ROWS, rows);
            req.setAttribute(A_COLTOTAL, colTotals);
            req.setAttribute(A_GRAND, grand);

            return VIEW_SALES;

        } catch (RuntimeException e) {
            /** 例外時は既存のキー "errorMsg" でエラーメッセージを渡す（他画面と統一） */
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute("errorMsg", validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /**
     * 「【admin】 機能：コストサマリー（秘書×月）」
     */
    /**
     * 「秘書×月のコストサマリー」を表示する。
     * - request param {@code 'fy'}：会計年度（4月開始）。未指定なら今日からFYを推定。
     * - 対象FYの12か月（4月→翌3月）を生成し、範囲に対して集計クエリを実行。
     * - 列合計と総合計を計算して JSP に渡す。
     *
     * @return ビュー名 {@value VIEW_COSTS}（エラー時はエラーページ）
     */
    public String costSummary() {
        try (TransactionManager tm = new TransactionManager()) {
            final int fy = resolveFiscalYear(req.getParameter(P_FY));
            final List<String> months = buildFiscalMonths(fy);

            final String fromYm = months.get(0);
            final String toYm   = months.get(months.size() - 1);

            // DAO 呼び出し：秘書×月のコストピボット
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

    /**
     * ④ ヘルパーメソッド（private）
     */

    /**
     * 4月開始の会計年度（FY）を決定する。パラメータ優先、無い/不正なら今日で推定。
     * - 4〜12月 … その年がFY
     * - 1〜3月  … 前年がFY
     *
     * @param paramFy リクエストパラメータ {@code "fy"}（null/空/不正は無視）
     * @return 決定した FY（int）
     */
    private int resolveFiscalYear(String paramFy) {
        if (paramFy != null && !paramFy.isBlank()) {
            try {
                return Integer.parseInt(paramFy);
            } catch (NumberFormatException ignore) {
                // フォールバックで今日から推定
            }
        }
        LocalDate today = LocalDate.now();
        return (today.getMonthValue() >= 4) ? today.getYear() : today.getYear() - 1;
    }

    /**
     * 指定 FY の 12か月（4月→翌3月）を "YYYY-MM" で返す。
     * 例：fy=2025 → 2025-04, …, 2026-03
     *
     * @param fy 会計年度（4月開始）
     * @return 12要素の年月リスト（昇順）
     */
    private List<String> buildFiscalMonths(int fy) {
        List<String> list = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            int month = 4 + i;              // 4..15
            int year  = fy + (month - 1) / 12;
            int calM  = ((month - 1) % 12) + 1;
            list.add(String.format("%04d-%02d", year, calM));
        }
        return list;
    }

    /**
     * 列（各月）ごとの合計を計算する。
     * @param months 表示する年月（YYYY-MM）
     * @param rows   ピボット行（顧客別または秘書別）
     * @return ym→合計金額 の LinkedHashMap（順序保持）
     */
    private Map<String, BigDecimal> calcColTotals(List<String> months, List<PivotRowDTO> rows) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (String m : months) {
            totals.put(m, BigDecimal.ZERO);
        }
        for (PivotRowDTO r : rows) {
            for (String m : months) {
                BigDecimal v = r.getAmountByYm().getOrDefault(m, BigDecimal.ZERO);
                totals.put(m, totals.get(m).add(v));
            }
        }
        return totals;
    }

    /**
     * 総合計（全列合計の合計）を返す。
     * @param colTotals 月別合計
     * @return 総合計
     */
    private BigDecimal calcGrand(Map<String, BigDecimal> colTotals) {
        BigDecimal g = BigDecimal.ZERO;
        for (BigDecimal v : colTotals.values()) {
            g = g.add(v);
        }
        return g;
    }
}
