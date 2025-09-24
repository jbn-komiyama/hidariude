package service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import dao.CustomerMonthlyInvoiceDAO;
import dao.InvoiceDAO;
import dao.SecretaryDAO;
import dao.TransactionManager;
import domain.Invoice;
import domain.LoginUser;
import domain.Task;
import dto.InvoiceDTO;
import dto.SecretaryDTO;
import dto.TaskDTO;

/**
 * 請求・支払サマリー／Excel発行に関するアプリケーションサービス。
 * 役割別（admin / customer / secretary）の画面向けデータ組み立てと、
 * 請求書Excelの生成・ダウンロードを担います。
 */
public class InvoiceService extends BaseService {

    // =========================================================
    // ① 定数・共通化（パラメータ名／パス／フォーマッタ）
    // =========================================================

    /** Excelテンプレ格納パス（WEB-INF配下） */
    private static final String TEMPLATE_PATH = "/WEB-INF/templates/invoice.xlsx";

    /** 年月フォーマッタ（yyyy-MM） */
    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    /** 日付フォーマッタ（yyyy-MM-dd） */
    private static final DateTimeFormatter YMD_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ----- Request params -----
    /** 画面入力パラメータ名（新系：yearMonth） */
    private static final String P_YM = "yearMonth";
    /** 画面入力パラメータ名（既存：targetYM） */
    private static final String P_YM_LEGACY = "targetYM";

    // ----- Request attributes（JSP 参照キー） -----
    /** 明細テーブル：タスク */
    private static final String A_TASKS = "tasks";
    /** 集計テーブル：請求行 */
    private static final String A_INVOICES = "invoices";
    /** 合計金額（請求合計・支払合計） */
    private static final String A_GRAND_TOTAL = "grandTotalFee";
    /** 表示中の年月（新旧どちらのキーもセットする） */
    private static final String A_YM = "yearMonth";
    private static final String A_YM_LEGACY = "targetYM";

    // ----- View names -----
    private static final String VIEW_SUMMARY_SECRETARY = "invoice/secretary/summary";
    private static final String VIEW_SUMMARY_CUSTOMER  = "invoice/customer/summary";
    private static final String VIEW_SUMMARY_ADMIN_COSTS = "invoice/admin/costs";
    private static final String VIEW_SUMMARY_ADMIN_SALES = "invoice/admin/sales";

    // Excel テンプレの既定明細行数（1始まり座標前提の相対制御に利用）
    private static final int TAXABLE_DEFAULT_ROWS = 5;
    private static final int NONTAX_DEFAULT_ROWS = 5;

    // =========================================================
    // ② フィールド・コンストラクタ
    // =========================================================
    private final Converter conv = new Converter();

    public InvoiceService(HttpServletRequest req, boolean useDB) {
        super(req, useDB);
    }

    // =========================================================
    // ③ メソッド（アクター別：admin → customer → secretary）
    // =========================================================

    // =========================
    // 【admin】 売上サマリー（顧客×秘書×ランク）
    // =========================
    /**
     * 「請求サマリー（管理）」表示。
     * - 入力: param 'yearMonth'（無ければ 'targetYM'→無ければ当月JST）
     * - 当月の顧客向け月次集計を UPSERT（DRAFT）
     * - 当月/前月のKPIと、顧客名グルーピング明細を生成
     * - JSP: /WEB-INF/jsp/invoice/admin/sales.jsp
     */
    public String adminInvoiceSummary() {
        String ym = pickYearMonthParam();
        // 前月算出
        LocalDate cur = LocalDate.parse(ym + "-01", YMD_FMT);
        String prevYm = cur.minusMonths(1).format(YM_FMT);

        try (TransactionManager tm = new TransactionManager()) {
            InvoiceDAO dao = new InvoiceDAO(tm.getConnection());

            // 当月の顧客向け月次サマリーをUPSERT（DRAFT集計）
            new CustomerMonthlyInvoiceDAO(tm.getConnection()).upsertByMonthFromTasks(ym);

            // 明細（顧客×秘書×ランク）
            List<InvoiceDTO> rows = dao.selectAdminLines(ym);

            // 顧客名でグルーピング
            Map<String, List<InvoiceDTO>> grouped = rows.stream()
                    .collect(Collectors.groupingBy(
                            InvoiceDTO::getCustomerCompanyName,
                            TreeMap::new,
                            Collectors.toList()));

            // KPI
            BigDecimal totalAmount = rows.stream().map(InvoiceDTO::getFee)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            int totalMinutes = rows.stream().mapToInt(InvoiceDTO::getTotalMinute).sum();
            int totalTasks = rows.size();

            // 前月
            BigDecimal prevTotalAmount = dao.selectAdminLines(prevYm).stream()
                    .map(InvoiceDTO::getFee).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal diffFromPrev = totalAmount.subtract(prevTotalAmount);

            // JSP属性
            req.setAttribute("yearMonth", ym);           // 既存の admin/sales.jsp は yearMonth を参照
            req.setAttribute("targetYM", ym);            // 念のため互換
            req.setAttribute("prevYearMonth", prevYm);

            Map<String, Object> adminTotals = new HashMap<>();
            adminTotals.put("totalAmount", totalAmount);
            adminTotals.put("totalTasksCount", totalTasks);
            adminTotals.put("totalWorkMinutes", totalMinutes);
            req.setAttribute("adminTotals", adminTotals);

            req.setAttribute("adminGrouped", grouped);
            req.setAttribute("diffFromPrev", diffFromPrev);

            tm.commit();
            return VIEW_SUMMARY_ADMIN_SALES;
        } catch (Exception e) {
            throw new ServiceException("E:INV-ADM-SUM 管理者サマリー作成に失敗しました。", e);
        }
    }

    // =========================
    // 【admin】 秘書への支払サマリー
    // =========================
    /**
     * 「秘書支払いサマリー（管理）」表示。
     * - 入力: param 'targetYM'（無ければ 'yearMonth'→無ければ当月JST）
     * - 当月の秘書×顧客×ランク明細（costLines）と総額（grandTotalCost）を算出
     * - 今月＋過去3ヶ月タイルの合計（costNow/Prev1/2/3）も算出
     * - JSP: /WEB-INF/jsp/invoice/admin/costs.jsp
     */
    public String secretaryInvoiceSummary() {
        // ★ cost_summary.jsp は param/attr ともに targetYM を期待：まずは既存優先で読む
        String targetYM = pickYearMonthParamPreferLegacy();

        // タイル（今月＋過去3ヶ月）
        ZoneId JST = ZoneId.of("Asia/Tokyo");
        YearMonth ym0 = YearMonth.now(JST);      // 今月
        YearMonth ym1 = ym0.minusMonths(1);
        YearMonth ym2 = ym0.minusMonths(2);
        YearMonth ym3 = ym0.minusMonths(3);

        req.setAttribute("m0", String.valueOf(ym0.getMonthValue()));
        req.setAttribute("m1", String.valueOf(ym1.getMonthValue()));
        req.setAttribute("m2", String.valueOf(ym2.getMonthValue()));
        req.setAttribute("m3", String.valueOf(ym3.getMonthValue()));

        req.setAttribute("ymNow",   ym0.toString());
        req.setAttribute("ymPrev1", ym1.toString());
        req.setAttribute("ymPrev2", ym2.toString());
        req.setAttribute("ymPrev3", ym3.toString());

        try (TransactionManager tm = new TransactionManager()) {
            InvoiceDAO dao = new InvoiceDAO(tm.getConnection());

            // ① 当月明細（秘書×顧客×ランク）
            List<InvoiceDTO> lines = dao.selectAdminLines(targetYM);

            BigDecimal grandTotal = lines.stream()
                    .map(InvoiceDTO::getFee)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // cost_summary.jsp が参照する属性名を厳守
            req.setAttribute("costLines", lines);
            req.setAttribute("grandTotalCost", grandTotal);

            // 表示中YM（互換のため両方）
            req.setAttribute(A_YM_LEGACY, targetYM); // targetYM
            req.setAttribute(A_YM, targetYM);        // yearMonth も一応

            // ② タイル用（4ヶ月分の総額）
            req.setAttribute("costNow",   sumFee(dao.selectAdminLines(ym0.toString())));
            req.setAttribute("costPrev1", sumFee(dao.selectAdminLines(ym1.toString())));
            req.setAttribute("costPrev2", sumFee(dao.selectAdminLines(ym2.toString())));
            req.setAttribute("costPrev3", sumFee(dao.selectAdminLines(ym3.toString())));

            tm.commit();
            return VIEW_SUMMARY_ADMIN_COSTS;
        } catch (Exception e) {
            throw new ServiceException("E:INV-S01 秘書支払サマリーの取得に失敗しました。", e);
        }
    }

    // =========================
    // 【customer】 請求サマリー（秘書×ランク）
    // =========================
    /**
     * 「請求サマリー（顧客）」表示。
     * - 入力: param 'yearMonth'（無ければ 'targetYM'→無ければ当月JST）
     * - 当月のタスク明細と秘書×ランク集計を表示
     * - 今月＋過去3ヶ月の未承認件数/合計金額も算出
     * - JSP: /WEB-INF/jsp/invoice/customer/summary.jsp
     */
    public String customerInvoiceSummary() {
        String targetYM = pickYearMonthParam();

        // ログイン中の顧客ID
        HttpSession session = req.getSession(false);
        if (session == null) throw new ServiceException("E:INV-C00 セッションが無効です。");
        LoginUser lu = (LoginUser) session.getAttribute("loginUser");

        UUID customerId = null;
        if (lu != null) {
            if (lu.getCustomer() != null) customerId = lu.getCustomer().getId();
            if (customerId == null && lu.getCustomerContact() != null) {
                customerId = lu.getCustomerContact().getCustomerId();
            }
        }
        if (customerId == null) throw new ServiceException("E:INV-C01 顧客IDが取得できません。");

        // タイル（月表示用）
        ZoneId JST = ZoneId.of("Asia/Tokyo");
        YearMonth ym0 = YearMonth.now(JST);
        YearMonth ym1 = ym0.minusMonths(1);
        YearMonth ym2 = ym0.minusMonths(2);
        YearMonth ym3 = ym0.minusMonths(3);

        req.setAttribute("m0", String.valueOf(ym0.getMonthValue()));
        req.setAttribute("m1", String.valueOf(ym1.getMonthValue()));
        req.setAttribute("m2", String.valueOf(ym2.getMonthValue()));
        req.setAttribute("m3", String.valueOf(ym3.getMonthValue()));

        req.setAttribute("ymNow",   ym0.toString());
        req.setAttribute("ymPrev1", ym1.toString());
        req.setAttribute("ymPrev2", ym2.toString());
        req.setAttribute("ymPrev3", ym3.toString());

        try (TransactionManager tm = new TransactionManager()) {
            InvoiceDAO dao = new InvoiceDAO(tm.getConnection());

            List<TaskDTO> taskDtos = dao.selectTasksByMonthAndCustomer(customerId, targetYM);
            List<InvoiceDTO> invDtos = dao.selectTotalMinutesBySecretaryAndCustomer(customerId, targetYM);

            List<Task> tasks = conv.toTaskDomainList(taskDtos);
            List<Invoice> invoices = conv.toInvoiceDomainList(invDtos);

            BigDecimal grandTotal = invoices.stream()
                    .map(Invoice::getFee).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            req.setAttribute(A_TASKS, tasks);
            req.setAttribute(A_INVOICES, invoices);
            req.setAttribute(A_GRAND_TOTAL, grandTotal);

            // 表示中YM（互換のため両方）
            req.setAttribute(A_YM, targetYM);
            req.setAttribute(A_YM_LEGACY, targetYM);

            // ダッシュボード（月次ステータス）
            req.setAttribute("statNow",   buildMonthlyStat(dao, customerId, ym0.toString()));
            req.setAttribute("statPrev1", buildMonthlyStat(dao, customerId, ym1.toString()));
            req.setAttribute("statPrev2", buildMonthlyStat(dao, customerId, ym2.toString()));
            req.setAttribute("statPrev3", buildMonthlyStat(dao, customerId, ym3.toString()));

            tm.commit();
            return VIEW_SUMMARY_CUSTOMER;
        } catch (Exception e) {
            throw new ServiceException("E:INV-C02 顧客向け請求サマリーの取得に失敗しました。", e);
        }
    }

    // =========================
    // 【secretary】 自身の請求サマリー
    // =========================
    /**
     * 「請求サマリー（秘書）」表示（自身の稼働ベース）。
     * - 入力: param 'yearMonth'（無ければ 'targetYM'→無ければ当月JST）
     * - タスク明細と会社別集計を取得し、月次サマリーを DRAFT で UPSERT
     * - JSP: /WEB-INF/jsp/invoice/secretary/summary.jsp
     */
    public String invoiceSummery() {
        String targetYM = pickYearMonthParam();

        // ログイン中の秘書ID
        HttpSession session = req.getSession(false);
        if (session == null) throw new ServiceException("E:INV-SVC00 セッションが無効です。");
        LoginUser loginUser = (LoginUser) session.getAttribute("loginUser");
        UUID secretaryId = loginUser.getSecretary().getId();

        try (TransactionManager tm = new TransactionManager()) {
            InvoiceDAO dao = new InvoiceDAO(tm.getConnection());

            List<TaskDTO> taskDtos    = dao.selectTasksByMonthAndSecretary(secretaryId, targetYM);
            List<InvoiceDTO> invDtos  = dao.selectTotalMinutesByCompanyAndSecretary(secretaryId, targetYM);

            List<Task> tasks    = conv.toTaskDomainList(taskDtos);
            List<Invoice> invoices = conv.toInvoiceDomainList(invDtos);

            BigDecimal grandTotal = invoices.stream()
                    .map(Invoice::getFee).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int totalMinutes = tasks.stream()
                    .map(Task::getWorkMinute).filter(Objects::nonNull)
                    .mapToInt(Integer::intValue).sum();

            int totalTasks = (tasks != null) ? tasks.size() : 0;

            // 月次サマリーUPSERT（DRAFT）
            dao.upsertSecretaryMonthlySummary(
                    secretaryId, targetYM, grandTotal, totalTasks, totalMinutes,
                    null, "DRAFT");

            // JSP属性
            req.setAttribute(A_TASKS, tasks);
            req.setAttribute(A_INVOICES, invoices);
            req.setAttribute(A_GRAND_TOTAL, grandTotal);
            // 表示中YM（互換のため両方）
            req.setAttribute(A_YM, targetYM);
            req.setAttribute(A_YM_LEGACY, targetYM);

            tm.commit();
            return VIEW_SUMMARY_SECRETARY;
        } catch (Exception e) {
            throw new ServiceException("E:INV-SVC01 請求サマリーの取得に失敗しました。", e);
        }
    }

    // =========================
    // 【secretary】 Excel発行（ダウンロード）
    // =========================
    /**
     * 「請求書発行」Excelダウンロード。
     * - 入力: param 'yearMonth'（無ければ 'targetYM'→無ければ当月JST）
     * - 未承認タスクがあれば 409 を返す
     * - 見出しセルを検出し、テンプレの上詰め構造に自動追従して明細を差し込み
     * - PDF出力なし（xlsx のみ）
     */
    public void issueInvoiceExcel(HttpServletResponse resp) {
        String targetYM = pickYearMonthParam();

        // ログイン中の秘書
        HttpSession session = req.getSession(false);
        if (session == null) throw new ServiceException("E:INV-ISSUE00 セッションが無効です。");
        LoginUser loginUser = (LoginUser) session.getAttribute("loginUser");
        if (loginUser == null || loginUser.getSecretary() == null)
            throw new ServiceException("E:INV-ISSUE01 ログイン情報が見つかりません。");
        UUID secretaryId = loginUser.getSecretary().getId();

        // 請求書ヘッダ（住所・口座）
        String secretaryName = "";
        String secretaryAddress1 = "";
        String secretaryAddress2 = "";
        String secretaryBuilding = "";
        String secretaryPostal = "";
        String secretaryTel = "";
        String bankName = "";
        String bankBranch = "";
        String bankType = "";
        String bankAccount = "";
        String bankOwner = "";

        List<TaskDTO> taskDtos;
        List<InvoiceDTO> invoiceDtos;

        // 取得
        try (TransactionManager tm = new TransactionManager()) {
            SecretaryDAO sdao = new SecretaryDAO(tm.getConnection());
            SecretaryDTO sec = sdao.selectByUUIdIncludeAccount(secretaryId);
            if (sec != null) {
                secretaryName     = nvl(sec.getName());
                secretaryPostal   = nvl(sec.getPostalCode());
                secretaryAddress1 = nvl(safe(sec.getAddress1()));
                secretaryAddress2 = nvl(safe(sec.getAddress2()));
                secretaryBuilding = nvl(safe(sec.getBuilding()));
                secretaryTel      = nvl(safe(sec.getPhone()));
                bankName          = nvl(safe(sec.getBankName()));
                bankBranch        = nvl(safe(sec.getBankBranch()));
                bankType          = nvl(safe(sec.getBankType()));
                bankAccount       = nvl(safe(sec.getBankAccount()));
                bankOwner         = nvl(safe(sec.getBankOwner()));
            }

            InvoiceDAO dao = new InvoiceDAO(tm.getConnection());
            taskDtos    = dao.selectTasksByMonthAndSecretary(secretaryId, targetYM);
            invoiceDtos = dao.selectTotalMinutesByCompanyAndSecretary(secretaryId, targetYM);
            tm.commit();
        } catch (Exception e) {
            throw new ServiceException("E:INV-ISSUE10 データ取得に失敗しました。", e);
        }

        if (taskDtos == null || taskDtos.isEmpty()) {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "対象データがありません。");
            return;
        }
        long unapproved = taskDtos.stream().filter(t -> t.getApprovedAt() == null).count();
        if (unapproved > 0) {
            sendError(resp, HttpServletResponse.SC_CONFLICT, "未承認タスクが含まれています。承認後に発行してください。");
            return;
        }

        // 並び：会社→ランク
        invoiceDtos.sort(Comparator
                .comparing(InvoiceDTO::getCustomerCompanyName, Comparator.nullsLast(String::compareTo))
                .thenComparing(InvoiceDTO::getTaskRankName, Comparator.nullsLast(String::compareTo)));

        // Excel 生成
        try (InputStream in = req.getServletContext().getResourceAsStream(TEMPLATE_PATH)) {
            if (in == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Excelテンプレが見つかりません: " + TEMPLATE_PATH);
                return;
            }
            try (XSSFWorkbook wb = new XSSFWorkbook(in)) {
                Sheet sh = wb.getSheetAt(0);

                // ヘッダ
                setCell(sh, 2, 1, "御　請　求　書（" + targetYM + "）");                // A2
                setCell(sh, 5, 8, secretaryName);                                      // H5
                setCell(sh, 6, 8, secretaryPostal.isEmpty() ? "" : "〒" + secretaryPostal);
                setCell(sh, 7, 8, secretaryAddress1 + secretaryAddress2);
                setCell(sh, 8, 8, secretaryBuilding);
                setCell(sh, 9, 8, secretaryTel.isEmpty() ? "" : "TEL：" + secretaryTel);
                LocalDate todayJST = LocalDate.now(ZoneId.of("Asia/Tokyo"));
                setCell(sh, 1, 9, "請求日：" + formatJpYmd(todayJST));                  // I1

                // 口座
                StringBuilder bankLine = new StringBuilder();
                if (!bankName.isBlank())   bankLine.append(bankName);
                if (!bankBranch.isBlank()) bankLine.append(bankLine.length() > 0 ? " " : "").append(bankBranch);
                if (!bankType.isBlank())   bankLine.append(bankLine.length() > 0 ? " " : "").append("(").append(bankType).append(")");
                if (!bankAccount.isBlank())bankLine.append(bankLine.length() > 0 ? " " : "").append(bankAccount);
                setCell(sh, 31, 1, bankLine.toString());
                setCell(sh, 32, 1, bankOwner.isBlank() ? "" : "口座名義：" + bankOwner);

                // 見出し検出
                int taxableHeader = findRowByText(sh, "会社名", 200);
                int rankHeader    = findRowByText(sh, "ランク", 200);
                if (taxableHeader <= 0 || (rankHeader > 0 && rankHeader != taxableHeader)) {
                    taxableHeader = 13; // フォールバック（旧テンプレ）
                }
                final int TAXABLE_DATA_START = taxableHeader + 1;

                int nonTaxHeader = findRowByText(sh, "非課税項目", 200);
                if (nonTaxHeader <= 0) nonTaxHeader = 21;
                final int NONTAX_DATA_START = nonTaxHeader + 1;

                // 行確保
                final int count   = invoiceDtos.size();
                final int addRows = Math.max(0, count - TAXABLE_DEFAULT_ROWS);
                final int SUBTOTAL_ROW_BASE_DYNAMIC = TAXABLE_DATA_START + TAXABLE_DEFAULT_ROWS;
                if (addRows > 0) {
                    sh.shiftRows(SUBTOTAL_ROW_BASE_DYNAMIC, sh.getLastRowNum(), addRows);
                    Row pattern = getOrCreateRow(sh, TAXABLE_DATA_START + TAXABLE_DEFAULT_ROWS - 1);
                    for (int i = 0; i < addRows; i++) {
                        int newRowNum = TAXABLE_DATA_START + TAXABLE_DEFAULT_ROWS + i;
                        copyRowStyle(pattern, getOrCreateRow(sh, newRowNum));
                    }
                }

                // データ書込み
                for (int i = 0; i < count; i++) {
                    InvoiceDTO d = invoiceDtos.get(i);
                    int r = TAXABLE_DATA_START + i;
                    int totalMin = d.getTotalMinute();
                    int hours = totalMin / 60;
                    int mins  = totalMin % 60;

                    setCell(sh, r, 1, nvl(d.getCustomerCompanyName())); // A
                    setCell(sh, r, 5, nvl(d.getTaskRankName()));         // E
                    setCell(sh, r, 6, hours);                            // F
                    setCell(sh, r, 7, mins);                             // G
                    if (d.getHourlyPay() != null) setCell(sh, r, 8, d.getHourlyPay().doubleValue()); // H
                    setCellFormula(sh, r, 10, "ROUND(H" + r + "*(F" + r + "+G" + r + "/60),0)");      // J
                }

                // 小計
                final int subRow    = SUBTOTAL_ROW_BASE_DYNAMIC + addRows;
                final int nonTaxSub = (NONTAX_DATA_START + NONTAX_DEFAULT_ROWS) + addRows;
                setCellFormula(sh, subRow,    10, "SUM(J" + TAXABLE_DATA_START + ":J" + (TAXABLE_DATA_START + count - 1) + ")");
                setCell(sh,        nonTaxSub, 10, 0);

                wb.setForceFormulaRecalculation(true);

                String fileName = "【御請求書】" + safeFileName(secretaryName) + "_" + targetYM + ".xlsx";
                String encoded  = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
                resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                resp.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
                wb.write(resp.getOutputStream());
                resp.flushBuffer();
            }
        } catch (IOException e) {
            throw new ServiceException("E:INV-ISSUE20 Excel生成に失敗しました。", e);
        }
    }

    // =========================================================
    // ④ ヘルパー
    // =========================================================

    /** param 'yearMonth' を優先、無ければ 'targetYM'→当月JST を返す */
    private String pickYearMonthParam() {
        String ym = req.getParameter(P_YM);
        if (ym == null || ym.isBlank()) ym = req.getParameter(P_YM_LEGACY);
        if (ym == null || ym.isBlank()) ym = LocalDate.now(ZoneId.of("Asia/Tokyo")).format(YM_FMT);
        return ym;
    }

    /** param 'targetYM' を優先、無ければ 'yearMonth'→当月JST を返す（costs 用） */
    private String pickYearMonthParamPreferLegacy() {
        String ym = req.getParameter(P_YM_LEGACY);
        if (ym == null || ym.isBlank()) ym = req.getParameter(P_YM);
        if (ym == null || ym.isBlank()) ym = LocalDate.now(ZoneId.of("Asia/Tokyo")).format(YM_FMT);
        return ym;
    }

    private static String nvl(String s) { return s == null ? "" : s; }
    private static String safe(String s) { return s == null ? "" : s.replaceAll("[\\r\\n]", " "); }
    private static String safeFileName(String s) {
        String base = (s == null || s.isBlank()) ? "secretary" : s;
        return base.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
    private void sendError(HttpServletResponse resp, int code, String msg) {
        try { resp.sendError(code, msg); } catch (IOException ignore) {}
    }

    private static Row getOrCreateRow(Sheet sh, int row1) { // 1始まり
        Row r = sh.getRow(row1 - 1);
        return (r != null) ? r : sh.createRow(row1 - 1);
    }
    private static Cell getOrCreateCell(Row row, int col1) {
        Cell c = row.getCell(col1 - 1);
        return (c != null) ? c : row.createCell(col1 - 1);
    }
    private static void setCell(Sheet sh, int row1, int col1, String v) {
        Row r = getOrCreateRow(sh, row1);
        Cell c = getOrCreateCell(r, col1);
        c.setCellValue(v);
    }
    private static void setCell(Sheet sh, int row1, int col1, int v) {
        Row r = getOrCreateRow(sh, row1);
        Cell c = getOrCreateCell(r, col1);
        c.setCellValue((double) v);
    }
    private static void setCell(Sheet sh, int row1, int col1, double v) {
        Row r = getOrCreateRow(sh, row1);
        Cell c = getOrCreateCell(r, col1);
        c.setCellValue(v);
    }
    private static void setCellFormula(Sheet sh, int row1, int col1, String formula) {
        Row r = getOrCreateRow(sh, row1);
        Cell c = getOrCreateCell(r, col1);
        c.setCellFormula(formula);
    }
    private static void copyRowStyle(Row src, Row dst) {
        dst.setHeight(src.getHeight());
        int max = Math.max(src.getLastCellNum(), (short) 0);
        for (int i = 1; i <= max; i++) {
            Cell sc = src.getCell(i - 1);
            Cell dc = dst.getCell(i - 1);
            if (dc == null) dc = dst.createCell(i - 1);
            if (sc != null && sc.getCellStyle() != null) {
                dc.setCellStyle(sc.getCellStyle());
            }
        }
    }
    private static int findRowByText(Sheet sh, String text, int scanMaxRows) {
        final int MAX_COL = 30;
        for (int r = 1; r <= scanMaxRows; r++) {
            Row row = sh.getRow(r - 1);
            if (row == null) continue;
            for (int c = 1; c <= MAX_COL; c++) {
                Cell cell = row.getCell(c - 1);
                if (cell == null) continue;
                if (cell.getCellType() == CellType.STRING) {
                    String v = cell.getStringCellValue();
                    if (v != null && v.trim().equals(text)) return r;
                }
            }
        }
        return -1;
    }
    private static String formatJpYmd(LocalDate d) {
        return String.format("%d年%02d月%02d日", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }

    /** 月次ステータス生成：未承認件数と合計金額（データ無しは total=null） */
    private Map<String, Object> buildMonthlyStat(InvoiceDAO dao, UUID customerId, String yearMonth) {
        Map<String, Object> stat = new HashMap<>();

        int unapproved = 0;
        List<TaskDTO> taskDtos = dao.selectTasksByMonthAndCustomer(customerId, yearMonth);
        if (taskDtos != null) {
            for (TaskDTO t : taskDtos) {
                boolean approved = (t.getApprovedAt() != null);
                if (!approved) unapproved++;
            }
        }
        stat.put("unapproved", unapproved);

        List<InvoiceDTO> invs = dao.selectTotalMinutesBySecretaryAndCustomer(customerId, yearMonth);
        BigDecimal total = null;
        if (invs != null && !invs.isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (InvoiceDTO d : invs) {
                if (d.getFee() != null) sum = sum.add(d.getFee());
            }
            total = sum;
        }
        stat.put("total", total);
        return stat;
    }

    /** fee 合計（null 無視） */
    private BigDecimal sumFee(List<InvoiceDTO> list) {
        return list.stream()
                .map(InvoiceDTO::getFee)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
