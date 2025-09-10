package service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import dao.AssignmentDAO;
import dao.CustomerDAO;
import dao.DAOException;
import dao.InvoiceDAO;
import dao.SecretaryDAO;
import dao.TaskRankDAO;
import dao.TransactionManager;
import domain.Assignment;
import domain.AssignmentGroup;
import domain.Customer;
import domain.Invoice;
import domain.LoginUser;
import domain.Secretary;
import domain.Task;
import domain.TaskRank;
import dto.AssignmentDTO;
import dto.CustomerDTO;
import dto.InvoiceDTO;
import dto.SecretaryDTO;
import dto.TaskDTO;
import dto.TaskRankDTO;



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
	
	    // ----- Attribute keys -----
//	    private static final String A_CUSTOMERS    = "customers";
//	    private static final String A_TARGET_YM    = "targetYM";
//	    private static final String A_CUSTOMER     = "customer";
//	    private static final String A_SECRETARIES  = "secretaries";
//	    private static final String A_TASK_RANKS   = "taskRanks";
//	    private static final String A_TASK_RANK    = "taskRank";
//	    private static final String A_ERROR        = "errorMsg";
//	    private static final String A_MESSAGE      = "message";
//	    private static final String A_STATUS       = "status";
//	    private static final String A_FUTURE_ASSIGNMENTS = "futureAssignments";

    // ★ CHANGED: 変換用インスタンスをフィールド化（new の重複排除）
    private final Converter conv = new Converter();
    
	public InvoiceService(HttpServletRequest req, boolean useDB) {
		super(req, useDB);
	}
	
	/** PM（is_pm_secretary = TRUE）向けのアサイン登録画面表示 */
	public String invoiceSummery() {
		// --- targetYM の決定（属性→パラメータ→現在時刻） ---
		
        String targetYM = req.getParameter(A_TARGET_YM);
        System.out.println(targetYM);
        
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


}