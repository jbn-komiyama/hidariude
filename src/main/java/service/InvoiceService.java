package service;

//===== 追加 import =====
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
 * アサイン（assignment）登録/一覧のアプリケーションサービス。
 * <ul>
 *   <li>一覧（当月）表示</li>
 *   <li>通常登録：入力 → 確認 → 完了</li>
 *   <li>PM登録：入力 → 確認 → 完了</li>
 * </ul>
 * 入力値検証は {@code Validation} を利用し、重複チェックは DAO で実施します。
 */
public class InvoiceService extends BaseService {
	
	// =========================================================
    // 定数・共通化（パラメータ名／パス／フォーマッタ／コンバータ）
    // =========================================================
	
	/** Excelテンプレ格納パス（WEB-INF配下・直接DL不可の場所） */
	private static final String TEMPLATE_PATH = "/WEB-INF/templates/invoice.xlsx"; 
    /** 年月フォーマッタ（yyyy-MM） */
    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM"); // ★ CHANGED: フォーマッタを定数化

    // 表示用に使う属性キー
    private static final String A_TASKS        = "tasks";
    private static final String A_INVOICES     = "invoices";
    private static final String A_GRAND_TOTAL  = "grandTotalFee";
    private static final String A_TARGET_YM    = "yearMonth";
    
    // ----- Request params -----
    private static final String P_TARGET_YM     = "yearMonth";

    // ----- View names -----
    private static final String VIEW_SUMMARY               = "invoice/secretary/summary";
    private static final String VIEW_SUMMARY_CUSTOMER     = "invoice/customer/summary";
	
    /*テンプレ初期座標（1始まり） */
    private static final int TAXABLE_DEFAULT_ROWS    = 5;   // テンプレに元々ある明細行数
    private static final int NONTAX_DEFAULT_ROWS     = 5;   // 非課税の元行数

    // ★ CHANGED: 変換用インスタンスをフィールド化（new の重複排除）
    private final Converter conv = new Converter();
    
	public InvoiceService(HttpServletRequest req, boolean useDB) {
		super(req, useDB);
	}
	
	/** PM（is_pm_secretary = TRUE）向けのアサイン登録画面表示 */
	public String invoiceSummery() {
		// --- targetYM の決定（属性→パラメータ→現在時刻） ---
		
        String targetYM = req.getParameter(A_TARGET_YM);
        
        if (targetYM == null || targetYM.isBlank()) {
            targetYM = LocalDate.now(ZoneId.of("Asia/Tokyo"))
                                .format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        // --- secretaryId の取得 ---
        HttpSession session = req.getSession(false);
        if (session == null) {
            throw new ServiceException("E:INV-SVC00 セッションが無効です。");
        }
        LoginUser loginUser = (LoginUser) session.getAttribute("loginUser");
        UUID secretaryId = loginUser.getSecretary().getId();

        TransactionManager tm = new TransactionManager();
        try {
            // --- DAO 呼び出し ---
            InvoiceDAO dao = new InvoiceDAO(tm.getConnection());

            List<TaskDTO> taskDtos = dao.selectTasksByMonthAndSecretary(secretaryId, targetYM);
            List<InvoiceDTO> invoiceDtos = dao.selectTotalMinutesByCompanyAndSecretary(secretaryId, targetYM);

            // --- DTO -> Domain 詰め替え ---
            List<Task> tasks = conv.toTaskDomainList(taskDtos);
            List<Invoice> invoices = conv.toInvoiceDomainList(invoiceDtos);

            // 総合計（全顧客の fee 合計）
            BigDecimal grandTotal = invoices.stream()
                    .map(Invoice::getFee)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // --- リクエストに格納 ---
            req.setAttribute(A_TASKS, tasks);
            req.setAttribute(A_INVOICES, invoices);
            req.setAttribute(A_GRAND_TOTAL, grandTotal);
            req.setAttribute(A_TARGET_YM, targetYM);
            tm.commit();

            return VIEW_SUMMARY;
        } catch (Exception e) {
        	tm.rollback();
            throw new ServiceException("E:INV-SVC01 請求サマリーの取得に失敗しました。", e);
        } finally {
            tm.close();
        }
    }
	
	// =========================
    // 機能：Excel発行（ダウンロード）
    // =========================
    /**
     * 「請求書発行」Excelダウンロード。
     * - yearMonth: request param 'yearMonth'（無ければ当月）
     * - 未承認があれば sendError(409)
     * - 見出しテキストを検出してテンプレ上詰めに自動追従
     * - PDF出力なし
     */
    public void issueInvoiceExcel(HttpServletResponse resp) {
        String targetYM = req.getParameter(P_TARGET_YM);
        if (targetYM == null || targetYM.isBlank()) {
            targetYM = LocalDate.now(ZoneId.of("Asia/Tokyo")).format(YM_FMT);
        }

        // セッション
        HttpSession session = req.getSession(false);
        if (session == null) throw new ServiceException("E:INV-ISSUE00 セッションが無効です。");
        LoginUser loginUser = (LoginUser) session.getAttribute("loginUser");
        if (loginUser == null || loginUser.getSecretary() == null)
            throw new ServiceException("E:INV-ISSUE01 ログイン情報が見つかりません。");

        UUID secretaryId = loginUser.getSecretary().getId();

     // 変更後（追加項目あり）
        String secretaryName = "";
        String secretaryAddress1 = "";
        String secretaryAddress2 = "";
        String secretaryBuilding = "";
        String secretaryPostal = "";
        String secretaryTel = "";
        
     // ★ 追加：銀行口座情報
        String bankName = "";
        String bankBranch = "";
        String bankType = "";
        String bankAccount = "";
        String bankOwner = "";

        List<TaskDTO> taskDtos;
        List<InvoiceDTO> invoiceDtos;

        TransactionManager tm = new TransactionManager();
        try {
            SecretaryDAO sdao = new SecretaryDAO(tm.getConnection());
            SecretaryDTO sec = sdao.selectByUUIdIncludeAccount(secretaryId);
         // 変更後
            if (sec != null) {
                secretaryName     = nvl(sec.getName());
                secretaryPostal   = nvl(sec.getPostalCode());
                secretaryAddress1 = nvl(safe(sec.getAddress1()));
                secretaryAddress2 = nvl(safe(sec.getAddress2()));
                secretaryBuilding = nvl(safe(sec.getBuilding()));
                secretaryTel      = nvl(safe(sec.getPhone()));
                bankName    = nvl(safe(sec.getBankName()));
                bankBranch  = nvl(safe(sec.getBankBranch()));
                bankType    = nvl(safe(sec.getBankType()));
                bankAccount = nvl(safe(sec.getBankAccount()));
                bankOwner   = nvl(safe(sec.getBankOwner()));
            }

            InvoiceDAO dao = new InvoiceDAO(tm.getConnection());
            taskDtos    = dao.selectTasksByMonthAndSecretary(secretaryId, targetYM);
            invoiceDtos = dao.selectTotalMinutesByCompanyAndSecretary(secretaryId, targetYM);
            tm.commit();
        } catch (Exception e) {
            tm.rollback();
            throw new ServiceException("E:INV-ISSUE10 データ取得に失敗しました。", e);
        } finally {
            tm.close();
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

        // 並び順：会社→ランク
        invoiceDtos.sort(Comparator
                .comparing(InvoiceDTO::getCustomerCompanyName, Comparator.nullsLast(String::compareTo))
                .thenComparing(InvoiceDTO::getTaskRankName, Comparator.nullsLast(String::compareTo)));

        // Excel生成
        try (InputStream in = req.getServletContext().getResourceAsStream(TEMPLATE_PATH)) {
            if (in == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Excelテンプレが見つかりません: " + TEMPLATE_PATH);
                return;
            }
            try (XSSFWorkbook wb = new XSSFWorkbook(in)) {
                Sheet sh = wb.getSheetAt(0);

	             // ==== ヘッダ書き込み（セル直指定） ====
	             // A2: 「御　請　求　書（YYYY-MM）」
	             setCell(sh, 2, 1, "御　請　求　書（" + targetYM + "）");
	
	             // H5: {秘書名}
	             setCell(sh, 5, 8, secretaryName);
	
	             // H6: 「〒{郵便番号}」
	             setCell(sh, 6, 8, secretaryPostal.isEmpty() ? "" : "〒" + secretaryPostal);
	
	             // H7: 「{住所1}{住所2}」 ※スペース無しで結合
	             setCell(sh, 7, 8, secretaryAddress1 + secretaryAddress2);
	
	             // H8: 「{建物}」
	             setCell(sh, 8, 8, secretaryBuilding);
	
	             // H9: 「TEL：{電話番号}」 ※全角コロン
	             setCell(sh, 9, 8, secretaryTel.isEmpty() ? "" : "TEL：" + secretaryTel);
	
	             // I1: 「請求日：YYYY年MM月DD日」
	             LocalDate todayJST = LocalDate.now(ZoneId.of("Asia/Tokyo"));
	             setCell(sh, 1, 9, "請求日：" + formatJpYmd(todayJST));
				// ==== A31/A32：口座情報 ====
				// A31: 「{銀行名} {支店名} ({種別}) {口座番号}」
				// 空要素はスキップし、適切にスペース/括弧を付加
				StringBuilder bankLine = new StringBuilder();
				if (!bankName.isBlank())
					bankLine.append(bankName);
				if (!bankBranch.isBlank())
					bankLine.append(bankLine.length() > 0 ? " " : "").append(bankBranch);
				if (!bankType.isBlank())
					bankLine.append(bankLine.length() > 0 ? " " : "").append("(").append(bankType).append(")");
				if (!bankAccount.isBlank())
					bankLine.append(bankLine.length() > 0 ? " " : "").append(bankAccount);
				setCell(sh, 31, 1, bankLine.toString());

				// A32: 「口座名義：{名義人}」※名義が空なら空セル
				setCell(sh, 32, 1, bankOwner.isBlank() ? "" : "口座名義：" + bankOwner);
	

                // 見出し検出（テンプレ上詰め自動追従）
                int taxableHeader = findRowByText(sh, "会社名", 200);
                int rankHeader    = findRowByText(sh, "ランク",  200);
                if (taxableHeader <= 0 || (rankHeader > 0 && rankHeader != taxableHeader)) {
                    taxableHeader = 13; // 旧テンプレフォールバック
                }
                final int TAXABLE_DATA_START = taxableHeader + 1;

                int nonTaxHeader = findRowByText(sh, "非課税項目", 200);
                if (nonTaxHeader <= 0) {
                    nonTaxHeader = 21; // フォールバック
                }
                final int NONTAX_DATA_START = nonTaxHeader + 1;

                // 行確保：下方シフト
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

                // データ書込み（会社×ランク）
                for (int i = 0; i < count; i++) {
                    InvoiceDTO d = invoiceDtos.get(i);
                    int r = TAXABLE_DATA_START + i;
                    int totalMin = d.getTotalMinute();
                    int hours = totalMin / 60;
                    int mins  = totalMin % 60;

                    setCell(sh, r, 1,  nvl(d.getCustomerCompanyName())); // A:会社名
                    setCell(sh, r, 5,  nvl(d.getTaskRankName()));        // E:ランク
                    setCell(sh, r, 6,  hours);                           // F:時間
                    setCell(sh, r, 7,  mins);                            // G:分
                    if (d.getHourlyPay() != null) {
                        setCell(sh, r, 8, d.getHourlyPay().doubleValue()); // H:単価
                    }
                    setCellFormula(sh, r, 10, "ROUND(H"+r+"*(F"+r+"+G"+r+"/60),0)"); // J:合計
                }

                // 相対位置で小計/総計/内税
                final int subRow     = SUBTOTAL_ROW_BASE_DYNAMIC + addRows;                      // 課税小計
                final int nonTaxSub  = (NONTAX_DATA_START + NONTAX_DEFAULT_ROWS) + addRows;      // 非課税小計


                setCellFormula(sh, subRow,    10, "SUM(J" + TAXABLE_DATA_START + ":J" + (TAXABLE_DATA_START + count - 1) + ")");
                setCell(sh,        nonTaxSub, 10, 0);

                wb.setForceFormulaRecalculation(true);

                // ダウンロード：ファイル名「【御請求書】秘書名_YYYY-MM.xlsx」
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
	
	// ===== ヘルパ（InvoiceService 内に追記） =====
	private static String nvl(String s){ return s==null ? "" : s; }
	private static String safe(String s){ return s==null ? "" : s.replaceAll("[\\r\\n]", " "); }
	private static String safeFileName(String s){
	    String base = (s==null || s.isBlank()) ? "secretary" : s;
	    return base.replaceAll("[\\\\/:*?\"<>|]", "_");
	}
	private void sendError(HttpServletResponse resp, int code, String msg){
	    try {
	        resp.sendError(code, msg);
	    } catch (IOException ignore) {}
	}

	//pull時に名前重複してるのでメソッド名に2を入れてる。後で必要なら直す
	public void issueInvoiceExcel2(HttpServletResponse resp) {
	    String targetYM = req.getParameter(P_TARGET_YM);
	    if (targetYM == null || targetYM.isBlank()) {
	        targetYM = LocalDate.now(ZoneId.of("Asia/Tokyo")).format(YM_FMT);
	    }

	    // セッション
	    HttpSession session = req.getSession(false);
	    if (session == null) throw new ServiceException("E:INV-ISSUE00 セッションが無効です。");
	    LoginUser loginUser = (LoginUser) session.getAttribute("loginUser");
	    if (loginUser == null || loginUser.getSecretary() == null)
	        throw new ServiceException("E:INV-ISSUE01 ログイン情報が見つかりません。");

	    UUID secretaryId = loginUser.getSecretary().getId();

	    // 変更後（追加項目あり）
	    String secretaryName = "";
	    String secretaryAddress1 = "";
	    String secretaryAddress2 = "";
	    String secretaryBuilding = "";
	    String secretaryPostal = "";
	    String secretaryTel = "";

	    // ★ 追加：銀行口座情報
	    String bankName = "";
	    String bankBranch = "";
	    String bankType = "";
	    String bankAccount = "";
	    String bankOwner = "";

	    List<TaskDTO> taskDtos;
	    List<InvoiceDTO> invoiceDtos;

	    TransactionManager tm = new TransactionManager();
	    try {
	        SecretaryDAO sdao = new SecretaryDAO(tm.getConnection());
	        SecretaryDTO sec = sdao.selectByUUIdIncludeAccount(secretaryId);
	        if (sec != null) {
	            secretaryName     = nvl(sec.getName());
	            secretaryPostal   = nvl(sec.getPostalCode());
	            secretaryAddress1 = nvl(safe(sec.getAddress1()));
	            secretaryAddress2 = nvl(safe(sec.getAddress2()));
	            secretaryBuilding = nvl(safe(sec.getBuilding()));
	            secretaryTel      = nvl(safe(sec.getPhone()));
	            bankName    = nvl(safe(sec.getBankName()));
	            bankBranch  = nvl(safe(sec.getBankBranch()));
	            bankType    = nvl(safe(sec.getBankType()));
	            bankAccount = nvl(safe(sec.getBankAccount()));
	            bankOwner   = nvl(safe(sec.getBankOwner()));
	        }

	        InvoiceDAO dao = new InvoiceDAO(tm.getConnection());
	        taskDtos    = dao.selectTasksByMonthAndSecretary(secretaryId, targetYM);
	        invoiceDtos = dao.selectTotalMinutesByCompanyAndSecretary(secretaryId, targetYM);
	        tm.commit();
	    } catch (Exception e) {
	        tm.rollback();
	        throw new ServiceException("E:INV-ISSUE10 データ取得に失敗しました。", e);
	    } finally {
	        tm.close();
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

	    // 並び順：会社→ランク
	    invoiceDtos.sort(Comparator
	            .comparing(InvoiceDTO::getCustomerCompanyName, Comparator.nullsLast(String::compareTo))
	            .thenComparing(InvoiceDTO::getTaskRankName, Comparator.nullsLast(String::compareTo)));

	    // Excel生成
	    try (InputStream in = req.getServletContext().getResourceAsStream(TEMPLATE_PATH)) {
	        if (in == null) {
	            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Excelテンプレが見つかりません: " + TEMPLATE_PATH);
	            return;
	        }
	        byte[] excelBytes;

	        try (XSSFWorkbook wb = new XSSFWorkbook(in)) {
	            Sheet sh = wb.getSheetAt(0);

	            // ==== ヘッダ書き込み（セル直指定） ====
	            // A2: 「御　請　求　書（YYYY-MM）」
	            setCell(sh, 2, 1, "御　請　求　書（" + targetYM + "）");

	            // H5: {秘書名}
	            setCell(sh, 5, 8, secretaryName);

	            // H6: 「〒{郵便番号}」
	            setCell(sh, 6, 8, secretaryPostal.isEmpty() ? "" : "〒" + secretaryPostal);

	            // H7: 「{住所1}{住所2}」
	            setCell(sh, 7, 8, secretaryAddress1 + secretaryAddress2);

	            // H8: 「{建物}」
	            setCell(sh, 8, 8, secretaryBuilding);

	            // H9: 「TEL：{電話番号}」
	            setCell(sh, 9, 8, secretaryTel.isEmpty() ? "" : "TEL：" + secretaryTel);

	            // I1: 「請求日：YYYY年MM月DD日」
	            LocalDate todayJST = LocalDate.now(ZoneId.of("Asia/Tokyo"));
	            setCell(sh, 1, 9, "請求日：" + formatJpYmd(todayJST));

	            // ==== A31/A32：口座情報 ====
	            // A31: 「{銀行名} {支店名} ({種別}) {口座番号}」
	            StringBuilder bankLine = new StringBuilder();
	            if (!bankName.isBlank())    bankLine.append(bankName);
	            if (!bankBranch.isBlank())  bankLine.append(bankLine.length() > 0 ? " " : "").append(bankBranch);
	            if (!bankType.isBlank())    bankLine.append(bankLine.length() > 0 ? " " : "").append("(").append(bankType).append(")");
	            if (!bankAccount.isBlank()) bankLine.append(bankLine.length() > 0 ? " " : "").append(bankAccount);
	            setCell(sh, 31, 1, bankLine.toString());

	            // A32: 「口座名義：{名義人}」
	            setCell(sh, 32, 1, bankOwner.isBlank() ? "" : "口座名義：" + bankOwner);

	            // 見出し検出（テンプレ上詰め自動追従）
	            int taxableHeader = findRowByText(sh, "会社名", 200);
	            int rankHeader    = findRowByText(sh, "ランク",  200);
	            if (taxableHeader <= 0 || (rankHeader > 0 && rankHeader != taxableHeader)) {
	                taxableHeader = 13; // 旧テンプレフォールバック
	            }
	            final int TAXABLE_DATA_START = taxableHeader + 1;

	            int nonTaxHeader = findRowByText(sh, "非課税項目", 200);
	            if (nonTaxHeader <= 0) {
	                nonTaxHeader = 21; // フォールバック
	            }
	            final int NONTAX_DATA_START = nonTaxHeader + 1;

	            // 行確保：下方シフト
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

	            // データ書込み（会社×ランク）
	            for (int i = 0; i < count; i++) {
	                InvoiceDTO d = invoiceDtos.get(i);
	                int r = TAXABLE_DATA_START + i;
	                int totalMin = d.getTotalMinute();
	                int hours = totalMin / 60;
	                int mins  = totalMin % 60;

	                setCell(sh, r, 1,  nvl(d.getCustomerCompanyName())); // A:会社名
	                setCell(sh, r, 5,  nvl(d.getTaskRankName()));        // E:ランク
	                setCell(sh, r, 6,  hours);                           // F:時間
	                setCell(sh, r, 7,  mins);                            // G:分
	                if (d.getHourlyPay() != null) {
	                    setCell(sh, r, 8, d.getHourlyPay().doubleValue()); // H:単価
	                }
	                setCellFormula(sh, r, 10, "ROUND(H"+r+"*(F"+r+"+G"+r+"/60),0)"); // J:合計
	            }

	            // 相対位置で小計/総計/内税
	            final int subRow     = SUBTOTAL_ROW_BASE_DYNAMIC + addRows;                 // 課税小計
	            final int nonTaxSub  = (NONTAX_DATA_START + NONTAX_DEFAULT_ROWS) + addRows; // 非課税小計

	            setCellFormula(sh, subRow,    10, "SUM(J" + TAXABLE_DATA_START + ":J" + (TAXABLE_DATA_START + count - 1) + ")");
	            setCell(sh,        nonTaxSub, 10, 0);

	            wb.setForceFormulaRecalculation(true);

	            // まずはメモリへ
	            try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(64 * 1024)) {
	                wb.write(baos);
	                excelBytes = baos.toByteArray();
	            }
	        }

	        // ==== 合計値を算出 ====
	        BigDecimal grandTotalFinal = BigDecimal.ZERO;
	        for (InvoiceDTO inv : invoiceDtos) {
	            if (inv.getFee() != null) grandTotalFinal = grandTotalFinal.add(inv.getFee());
	        }
	        int totalMinutesFinal = 0;
	        for (TaskDTO t : taskDtos) {
	            if (t.getWorkMinute() != null) totalMinutesFinal += t.getWorkMinute();
	        }
	        int totalTasksFinal = (taskDtos != null) ? taskDtos.size() : 0;

	        // ==== UPSERT は新しいトランザクションで ====
	        try (TransactionManager tm2 = new TransactionManager()) {
	            InvoiceDAO dao2 = new InvoiceDAO(tm2.getConnection());
	            dao2.upsertSecretaryMonthlySummary(
	                    secretaryId,
	                    targetYM,
	                    grandTotalFinal,
	                    totalTasksFinal,
	                    totalMinutesFinal,
	                    new java.sql.Timestamp(System.currentTimeMillis()),
	                    "FINALIZED" // 不要なら null
	            );
	            tm2.commit();
	        } catch (Exception e) {
	            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
	                    "E:INV-ISSUE30 月次サマリの保存に失敗しました。");
	            return;
	        }

	        // ==== レスポンスに配信（UPSERT成功後） ====
	        String fileName = "【御請求書】" + safeFileName(secretaryName) + "_" + targetYM + ".xlsx";
	        String encoded  = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
	        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	        resp.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
	        resp.getOutputStream().write(excelBytes);
	        resp.flushBuffer();

	    } catch (IOException e) {
	        throw new ServiceException("E:INV-ISSUE20 Excel生成に失敗しました。", e);
	    }
	}

	
	// ===== ヘルパ（InvoiceService 内に追記） =====
//	private static String nvl(String s){ return s==null ? "" : s; }
//	private static String safe(String s){ return s==null ? "" : s.replaceAll("[\\r\\n]", " "); }
//	private static String safeFileName(String s){
//	    String base = (s==null || s.isBlank()) ? "secretary" : s;
//	    return base.replaceAll("[\\\\/:*?\"<>|]", "_");
//	}
//	private void sendError(HttpServletResponse resp, int code, String msg){
//	    try {
//	        resp.sendError(code, msg);
//	    } catch (IOException ignore) {}
//	}


	private static Row getOrCreateRow(Sheet sh, int row1){  // 1始まり
	    Row r = sh.getRow(row1-1);
	    return (r != null) ? r : sh.createRow(row1-1);
	}
	private static Cell getOrCreateCell(Row row, int col1){
	    Cell c = row.getCell(col1-1);
	    return (c != null) ? c : row.createCell(col1-1);
	}
	private static void setCell(Sheet sh, int row1, int col1, String v){
	    Row r = getOrCreateRow(sh, row1);
	    Cell c = getOrCreateCell(r, col1);
	    c.setCellValue(v);
	}
	private static void setCell(Sheet sh, int row1, int col1, int v){
	    Row r = getOrCreateRow(sh, row1);
	    Cell c = getOrCreateCell(r, col1);
	    c.setCellValue((double) v);
	}
	private static void setCell(Sheet sh, int row1, int col1, double v){
	    Row r = getOrCreateRow(sh, row1);
	    Cell c = getOrCreateCell(r, col1);
	    c.setCellValue(v);
	}
	private static void setCellFormula(Sheet sh, int row1, int col1, String formula){
	    Row r = getOrCreateRow(sh, row1);
	    Cell c = getOrCreateCell(r, col1);
	    c.setCellFormula(formula);
	}
	
	
	/** 書式コピー（幅/高さはテンプレに依存） */
	private static void copyRowStyle(Row src, Row dst){
	    dst.setHeight(src.getHeight());
	    int max = Math.max(src.getLastCellNum(), (short)0);
	    for (int i=1; i<=max; i++){
	        Cell sc = src.getCell(i-1);
	        Cell dc = dst.getCell(i-1);
	        if (dc == null) dc = dst.createCell(i-1);
	        if (sc != null && sc.getCellStyle() != null) {
	            dc.setCellStyle(sc.getCellStyle());
	        }
	        // 合計列(J)などは後で式を入れ直すので値はコピー不要
	    }
	}
	
	/** シート内で完全一致するテキストを探して行番号(1始まり)を返す。見つからなければ -1 */
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

    // InvoiceService の private ヘルパに追加
    private static String formatJpYmd(LocalDate d) {
        return String.format("%d年%02d月%02d日", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }
    
    
    /** 顧客向け：請求サマリー（秘書×ランク集計） */
    /** 顧客向け：請求サマリー（秘書×ランク集計）＋ダッシュボード用ステータスも積む */
    public String customerInvoiceSummary() {
        // 対象年月（明細画面用）
        String targetYM = req.getParameter(A_TARGET_YM);
        if (targetYM == null || targetYM.isBlank()) {
            targetYM = LocalDate.now(ZoneId.of("Asia/Tokyo")).format(YM_FMT);
        }

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

        // ダッシュボード用（今月＋過去3ヶ月）
        ZoneId JST = ZoneId.of("Asia/Tokyo");
        YearMonth ym0 = YearMonth.now(JST);        // 今月
        YearMonth ym1 = ym0.minusMonths(1);        // 先月
        YearMonth ym2 = ym0.minusMonths(2);        // 2か月前
        YearMonth ym3 = ym0.minusMonths(3);        // 3か月前

        // タイル表示ラベル用（数字の月）
        req.setAttribute("m0", String.valueOf(ym0.getMonthValue()));
        req.setAttribute("m1", String.valueOf(ym1.getMonthValue()));
        req.setAttribute("m2", String.valueOf(ym2.getMonthValue()));
        req.setAttribute("m3", String.valueOf(ym3.getMonthValue()));

        // タイルリンク用YM
        req.setAttribute("ymNow",   ym0.toString());  // "yyyy-MM"
        req.setAttribute("ymPrev1", ym1.toString());
        req.setAttribute("ymPrev2", ym2.toString());
        req.setAttribute("ymPrev3", ym3.toString());

        try (TransactionManager tm = new TransactionManager()) {
            InvoiceDAO dao = new InvoiceDAO(tm.getConnection());

            // ====== 明細画面（指定月）既存機能は維持 ======
            List<TaskDTO> taskDtos   = dao.selectTasksByMonthAndCustomer(customerId, targetYM);
            List<InvoiceDTO> invDtos = dao.selectTotalMinutesBySecretaryAndCustomer(customerId, targetYM);

            List<Task> tasks        = conv.toTaskDomainList(taskDtos);
            List<Invoice> invoices  = conv.toInvoiceDomainList(invDtos);

            java.math.BigDecimal grandTotal = invoices.stream()
                    .map(Invoice::getFee)
                    .filter(java.util.Objects::nonNull)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            req.setAttribute("tasks", tasks);
            req.setAttribute("invoices", invoices);
            req.setAttribute("grandTotalFee", grandTotal);
            req.setAttribute(A_TARGET_YM, targetYM);

            // ====== ダッシュボード用ステータス（未承認/合計）を4ヶ月分セット ======
            req.setAttribute("statNow",   buildMonthlyStat(dao, customerId, ym0.toString()));
            req.setAttribute("statPrev1", buildMonthlyStat(dao, customerId, ym1.toString()));
            req.setAttribute("statPrev2", buildMonthlyStat(dao, customerId, ym2.toString()));
            req.setAttribute("statPrev3", buildMonthlyStat(dao, customerId, ym3.toString()));

            tm.commit();
            return VIEW_SUMMARY_CUSTOMER; // 既存の戻り先を維持（ダッシュボードJSPを使う場合はそのパスに変更）
        } catch (Exception e) {
            throw new ServiceException("E:INV-C02 顧客向け請求サマリーの取得に失敗しました。", e);
        }
    }

    /** 月次ステータス生成：未承認件数と合計金額（データ無しは total=null） */
    private Map<String, Object> buildMonthlyStat(InvoiceDAO dao, UUID customerId, String yearMonth) {
        Map<String, Object> stat = new java.util.HashMap<>();

        // 1) 未承認件数（タスクから算出）
        int unapproved = 0;
        List<TaskDTO> taskDtos = dao.selectTasksByMonthAndCustomer(customerId, yearMonth);
        if (taskDtos != null) {
            for (TaskDTO t : taskDtos) {
                // 承認フラグの名前がプロジェクト差異ありのため、代表的なパターンを順に判定
                boolean approved = false;
                try {
                    try { // getApproved(): Boolean
                        java.lang.reflect.Method m = t.getClass().getMethod("getApproved");
                        Object v = m.invoke(t);
                        if (v instanceof Boolean b) approved = b;
                    } catch (NoSuchMethodException ignore) {}

                    if (!approved) try { // isApproved(): boolean
                        java.lang.reflect.Method m = t.getClass().getMethod("isApproved");
                        Object v = m.invoke(t);
                        if (v instanceof Boolean b) approved = b;
                    } catch (NoSuchMethodException ignore) {}

                    if (!approved) try { // getApprovalStatus(): String -> "APPROVED" / "承認済み"
                        java.lang.reflect.Method m = t.getClass().getMethod("getApprovalStatus");
                        Object v = m.invoke(t);
                        if (v != null) {
                            String s = v.toString();
                            approved = "APPROVED".equalsIgnoreCase(s) || "承認済み".equals(s);
                        }
                    } catch (NoSuchMethodException ignore) {}
                } catch (Exception ignore) {}

                if (!approved) unapproved++;
            }
        }
        stat.put("unapproved", unapproved);

        // 2) 合計金額（データ無しなら null）
        List<InvoiceDTO> invs = dao.selectTotalMinutesBySecretaryAndCustomer(customerId, yearMonth);
        java.math.BigDecimal total = null;
        if (invs != null && !invs.isEmpty()) {
            java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
            for (InvoiceDTO d : invs) {
                if (d.getFee() != null) sum = sum.add(d.getFee());
            }
            total = sum;
        }
        stat.put("total", total);

        return stat;
    }

}