package service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import dao.AssignmentDAO;
import dao.CustomerDAO;
import dao.CustomerMonthlyInvoiceDAO;
import dao.TransactionManager;
import domain.Assignment;
import domain.Customer;
import dto.AssignmentDTO;
import dto.CustomerDTO;
import dto.CustomerMonthlyInvoiceDTO;

/**
 * 【admin】顧客サービス（一覧／詳細／登録／編集／削除）
 *
 * <p>このクラスは管理者向けの顧客機能を提供します。入力検証は {@link BaseService#validation}、
 * DB アクセスは {@link TransactionManager} を用いた try-with-resources で行います。
 * 例外発生時は {@code errorMsg} を設定し、共通エラーページへ遷移します。</p>
 *
 * <ul>
 *   <li>FrontController でのルーティング（/admin 配下）に対応</li>
 *   <li>ビュー名・属性名・パラメータ名は定数に集約し、JSP 名やキー名のハードコードを回避</li>
 *   <li>JSP 側参照名は既存のまま（キー文字列は不変）</li>
 * </ul>
 */
public class CustomerService extends BaseService {

    // =========================
    // ① 定数・共通化（パラメータ名／パス／フォーマッタ／コンバータ）
    // =========================
	
	// ===== ビュー名 =====
    private static final String VIEW_HOME      		= "customer/admin/home";
    private static final String VIEW_REGISTER			= "customer/admin/register";    
    private static final String VIEW_REGISTER_CHECK	= "customer/admin/register_check";    
    private static final String VIEW_REGISTER_DONE 	= "customer/admin/register_done"; 
    private static final String VIEW_EDIT         		= "customer/admin/edit";         
    private static final String VIEW_EDIT_CHECK		= "customer/admin/edit_check"; 
    private static final String VIEW_EDIT_DONE    	= "customer/admin/edit_done";   
    private static final String VIEW_DETAIL          = "customer/admin/detail";

    // ----- Attribute keys -----
    private static final String A_CUSTOMERS          = "customers";
    private static final String A_CUSTOMER           = "customer";
    private static final String A_MESSAGE            = "message";
    private static final String A_ERROR_MSG          = "errorMsg";

    // 明細/集計画面で使用している既存の属性キー（JSP互換のため変更禁止）
    private static final String A_ASSIGNMENTS_THIS   = "assignmentsThisMonth";
    private static final String A_CONT_MONTHS        = "contMonths";
    private static final String A_INVOICE_TOTAL_AMT  = "invoiceTotalAmount";
    private static final String A_INVOICE_TOTAL_CNT  = "invoiceTotalCount";
    private static final String A_INVOICE_TOTAL_WORK = "invoiceTotalWork";
    private static final String A_INVOICES_LAST12    = "invoicesLast12";
    private static final String A_ASSIGNMENTS_HIST   = "assignmentsHistory";
    private static final String A_TARGET_YM          = "targetYM";

    // ----- Request parameter keys -----
    private static final String P_ID           = "id";
    private static final String P_COMPANY_CODE = "companyCode";
    private static final String P_COMPANY_NAME = "companyName";
    private static final String P_MAIL         = "mail";
    private static final String P_PHONE        = "phone";
    private static final String P_POSTAL_CODE  = "postalCode";
    private static final String P_ADDRESS1     = "address1";
    private static final String P_ADDRESS2     = "address2";
    private static final String P_BUILDING     = "building";


    // ----- Formatter / Zone -----
    private static final ZoneId     Z_JST      = ZoneId.of("Asia/Tokyo");
    private static final DateTimeFormatter F_YM = DateTimeFormatter.ofPattern("yyyy-MM");

    // ----- Converter（DTO → Domain 変換器） -----
    private final Converter conv = new Converter();

    // =========================
    // ② フィールド、コンストラクタ
    // =========================

    /**
     * コンストラクタ。
     * @param req   現在の {@link HttpServletRequest}
     * @param useDB DBを使用するかどうか（フラグ自体は保持のみ）
     */
    public CustomerService(HttpServletRequest req, boolean useDB) {
        super(req, useDB);
    }

    // =========================
    // ③ メソッド（コントローラ呼び出しメソッド：すべて admin 用）
    // =========================

    // =========================
    // 「【admin】機能：顧客一覧」
    // =========================

    /**
     * 「顧客一覧」表示。
     * <ul>
     *   <li>DAOから全件（論理削除除外想定）取得。</li>
     *   <li>DTO を Domain に変換し {@code customers} へ格納。</li>
     * </ul>
     * @return 一覧ビュー（{@value #VIEW_HOME}）。例外時はエラーページへ。
     */
    public String customerList() {
        try (TransactionManager tm = new TransactionManager()) {
            CustomerDAO dao = new CustomerDAO(tm.getConnection());
            List<CustomerDTO> dtos = dao.selectAll();

            // DTO -> Domain へ詰替
            List<Customer> customers = new ArrayList<>(dtos.size());
            for (CustomerDTO dto : dtos) {
                customers.add(conv.toDomain(dto));
            }

            req.setAttribute(A_CUSTOMERS, customers);
            return VIEW_HOME;
        } catch (RuntimeException e) {
            validation.addErrorMsg("顧客一覧の取得に失敗しました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // 「【admin】機能：顧客詳細」
    // =========================

    /**
     * 「顧客詳細」表示。
     * <ul>
     *   <li>request param {@code id}: 顧客UUID</li>
     *   <li>顧客＆担当者、当月アサイン、通算請求サマリ、直近12か月請求、アサイン履歴を取得。</li>
     *   <li>取得データを既存の属性キー名で JSP に渡す（キー名は変更しない）。</li>
     * </ul>
     * @return 詳細ビュー（{@value #VIEW_DETAIL}）。不正IDや例外時はエラーページへ。
     */
    public String customerDetail() {
        final String idStr = req.getParameter(P_ID); // 顧客ID(UUID文字列)

        // UUIDバリデーション（簡易：BaseService#validation を活用）
        if (!validation.isUuid(idStr)) {
            req.setAttribute(A_ERROR_MSG, List.of("不正な顧客IDです。"));
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        // 「当月 (yyyy-MM)」を JST で算出
        String ymNow = LocalDate.now(Z_JST).format(F_YM);

        try (TransactionManager tm = new TransactionManager()) {
            UUID customerId = UUID.fromString(idStr);

            // ①② 顧客＋担当者一覧
            CustomerDAO cdao = new CustomerDAO(tm.getConnection());
            CustomerDTO cDto = cdao.selectWithContactsByUuid(customerId);
            if (cDto == null) {
                req.setAttribute(A_ERROR_MSG, List.of("顧客が見つかりません。"));
                return req.getContextPath() + req.getServletPath() + "/error";
            }
            Customer customer = conv.toDomain(cDto);
            req.setAttribute(A_CUSTOMER, customer);

            // ③ 今月のアサイン（継続ランク付き）
            AssignmentDAO adao = new AssignmentDAO(tm.getConnection());
            List<AssignmentDTO> thisMonthDtos = adao.selectThisMonthByCustomerWithContRank(customerId, ymNow);

            List<Assignment> thisMonth = new ArrayList<>();
            Map<UUID, Integer> contMap = new HashMap<>();
            for (AssignmentDTO d : thisMonthDtos) {
                thisMonth.add(conv.toDomain(d));
                if (d.getAssignmentId() != null) {
                    contMap.put(d.getAssignmentId(),
                                d.getConsecutiveMonths() == null ? 0 : d.getConsecutiveMonths());
                }
            }
            req.setAttribute(A_ASSIGNMENTS_THIS, thisMonth);
            req.setAttribute(A_CONT_MONTHS, contMap);

            // ④ 今までの請求合計（Summary）
            CustomerMonthlyInvoiceDAO idao = new CustomerMonthlyInvoiceDAO(tm.getConnection());
            var summary = idao.selectSummaryUpToYm(customerId, ymNow);
            req.setAttribute(A_INVOICE_TOTAL_AMT,  summary.totalAmount);
            req.setAttribute(A_INVOICE_TOTAL_CNT,  summary.count);
            req.setAttribute(A_INVOICE_TOTAL_WORK, summary.totalWorkMinutes);

            // ⑥ 直近1年の請求（DTOをそのまま渡す）
            List<CustomerMonthlyInvoiceDTO> inv12 = idao.selectLast12UpToYm(customerId, ymNow);
            req.setAttribute(A_INVOICES_LAST12, inv12);

            // ⑤ 今月までのアサイン履歴（最新→）
            List<AssignmentDTO> historyDtos = adao.selectByCustomerUpToYearMonthDesc(customerId, ymNow);
            List<Assignment> history = new ArrayList<>();
            for (AssignmentDTO d : historyDtos) history.add(conv.toDomain(d));
            req.setAttribute(A_ASSIGNMENTS_HIST, history);

            req.setAttribute(A_TARGET_YM, ymNow);
            return VIEW_DETAIL;
        } catch (RuntimeException e) {
            validation.addErrorMsg("顧客詳細の取得に失敗しました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // 「【admin】機能：顧客 新規登録（画面→確認→確定）」
    // =========================

    /**
     * 「顧客 新規登録」画面表示。
     * <p>入力フォーム表示のみ。DB アクセスなし。</p>
     * @return 登録ビュー（{@value #VIEW_REGISTER}）
     */
    public String customerRegister() {
        return VIEW_REGISTER;
    }

    /**
     * 「顧客 新規登録」確認。
     * <ul>
     *   <li>request param: {@code companyCode, companyName, mail, phone, postalCode} ほか</li>
     *   <li>必須/形式チェック、{@code companyCode} の重複チェック。</li>
     *   <li>エラー時: {@code errorMsg} と入力値を戻して登録画面へ。</li>
     *   <li>成功時: 入力値を属性へ詰め替え、確認画面へ。</li>
     * </ul>
     * @return 確認ビュー（{@value #VIEW_REGISTER_CHECK}）または登録ビュー。
     */
    public String customerRegisterCheck() {
        final String companyCode = req.getParameter(P_COMPANY_CODE);
        final String companyName = req.getParameter(P_COMPANY_NAME);
        final String mail        = req.getParameter(P_MAIL);
        final String phone       = req.getParameter(P_PHONE);
        final String postalCode  = req.getParameter(P_POSTAL_CODE);

        // 必須
        validation.isNull("会社コード", companyCode);
        validation.isNull("会社名", companyName);
        // 任意の形式チェック
        if (notBlank(mail))       validation.isEmail(mail);
        if (notBlank(phone))      validation.isPhoneNumber(phone);
        if (notBlank(postalCode)) validation.isPostalCode(postalCode);

        // companyCode 重複チェック
        if (!validation.hasErrorMsg() && notBlank(companyCode)) {
            try (TransactionManager tm = new TransactionManager()) {
                CustomerDAO dao = new CustomerDAO(tm.getConnection());
                if (dao.companyCodeExists(companyCode)) {
                    validation.addErrorMsg("その会社コードは既に使われています。");
                }
            } catch (RuntimeException ignore) {
                // ここでは握りつぶし、後段の例外ハンドリングに委譲
            }
        }

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            pushFormBackToRequest(); // 入力値を戻す
            return VIEW_REGISTER;
        }

        pushFormBackToRequest();     // hiddenで引き継ぐ＋画面でも参照可能に
        return VIEW_REGISTER_CHECK;
    }

    /**
     * 「顧客 新規登録」確定。
     * <ul>
     *   <li>最終チェック（必須/形式/重複）を行い INSERT。</li>
     *   <li>成功時: {@code message} を設定し完了画面へ。</li>
     *   <li>失敗時: {@code errorMsg} と入力値を戻して登録画面へ。</li>
     * </ul>
     * @return 完了ビュー（{@value #VIEW_REGISTER_DONE}）または登録ビュー。
     */
    public String customerRegisterDone() {
        final String companyCode = req.getParameter(P_COMPANY_CODE);
        final String companyName = req.getParameter(P_COMPANY_NAME);
        final String mail        = req.getParameter(P_MAIL);
        final String phone       = req.getParameter(P_PHONE);
        final String postalCode  = req.getParameter(P_POSTAL_CODE);
        final String address1    = req.getParameter(P_ADDRESS1);
        final String address2    = req.getParameter(P_ADDRESS2);
        final String building    = req.getParameter(P_BUILDING);

        validation.isNull("会社コード", companyCode);
        validation.isNull("会社名", companyName);
        if (notBlank(mail))       validation.isEmail(mail);
        if (notBlank(phone))      validation.isPhoneNumber(phone);
        if (notBlank(postalCode)) validation.isPostalCode(postalCode);

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            pushFormBackToRequest();
            return VIEW_REGISTER;
        }

        try (TransactionManager tm = new TransactionManager()) {
            CustomerDAO dao = new CustomerDAO(tm.getConnection());

            // companyCode 重複の最終確認
            if (notBlank(companyCode) && dao.companyCodeExists(companyCode)) {
                validation.addErrorMsg("その会社コードは既に使われています。");
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                pushFormBackToRequest();
                return VIEW_REGISTER;
            }

            CustomerDTO dto = new CustomerDTO();
            dto.setCompanyCode(companyCode);
            dto.setCompanyName(companyName);
            dto.setMail(mail);
            dto.setPhone(phone);
            dto.setPostalCode(postalCode);
            dto.setAddress1(address1);
            dto.setAddress2(address2);
            dto.setBuilding(building);

            int num = dao.insert(dto);
            tm.commit();

            req.setAttribute(A_MESSAGE, "登録が完了しました（件数:" + num + "）");
            return VIEW_REGISTER_DONE;
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            pushFormBackToRequest();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // 「【admin】機能：顧客 編集（画面→確認→確定）」
    // =========================

    /**
     * 「顧客 編集」画面表示。
     * <ul>
     *   <li>request param {@code id}: 顧客UUID</li>
     *   <li>該当顧客を取得して {@code customer} に設定。</li>
     * </ul>
     * @return 編集ビュー（{@value #VIEW_EDIT}）。不正IDや例外時はエラーページ。
     */
    public String customerEdit() {
        final String idStr = req.getParameter(P_ID);
        try {
            UUID id = UUID.fromString(idStr);
            try (TransactionManager tm = new TransactionManager()) {
                CustomerDAO dao = new CustomerDAO(tm.getConnection());
                CustomerDTO dto = dao.selectByUUId(id);
                Customer customer = conv.toDomain(dto);
                req.setAttribute(A_CUSTOMER, customer);
                return VIEW_EDIT;
            }
        } catch (Exception ex) {
            validation.addErrorMsg("不正なIDが指定されました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /**
     * 「顧客 編集」確認。
     * <ul>
     *   <li>必須/形式チェック、{@code companyCode} の重複チェック（自ID除外）。</li>
     *   <li>エラー時: {@code errorMsg} と入力値を戻して編集画面へ。</li>
     *   <li>成功時: 入力値を属性に詰め替え、確認画面へ。</li>
     * </ul>
     * @return 確認ビュー（{@value #VIEW_EDIT_CHECK}）または編集ビュー。
     */
    public String customerEditCheck() {
        final String idStr       = req.getParameter(P_ID);
        final String companyCode = req.getParameter(P_COMPANY_CODE);
        final String companyName = req.getParameter(P_COMPANY_NAME);
        final String mail        = req.getParameter(P_MAIL);
        final String phone       = req.getParameter(P_PHONE);
        final String postalCode  = req.getParameter(P_POSTAL_CODE);

        validation.isNull("ID", idStr);
        validation.isNull("会社名", companyName);
        if (notBlank(mail))       validation.isEmail(mail);
        if (notBlank(phone))      validation.isPhoneNumber(phone);
        if (notBlank(postalCode)) validation.isPostalCode(postalCode);

        // companyCode 重複（自分は除外）
        if (!validation.hasErrorMsg() && notBlank(companyCode)) {
            try (TransactionManager tm = new TransactionManager()) {
                CustomerDAO dao = new CustomerDAO(tm.getConnection());
                UUID id = UUID.fromString(idStr);
                if (dao.companyCodeExistsExceptId(companyCode, id)) {
                    validation.addErrorMsg("その会社コードは既に使われています。");
                }
            } catch (RuntimeException ignore) { }
        }

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            req.setAttribute(P_ID, idStr);
            pushFormBackToRequest();
            return VIEW_EDIT;
        }

        req.setAttribute(P_ID, idStr);
        pushFormBackToRequest();
        return VIEW_EDIT_CHECK;
    }

    /**
     * 「顧客 編集」確定。
     * <ul>
     *   <li>最終チェック（必須/形式/重複（自ID除外））を行い UPDATE。</li>
     *   <li>成功時: {@code message} を設定し完了画面へ。</li>
     *   <li>失敗時: {@code errorMsg} と入力値を戻して編集画面へ、またはエラーページへ。</li>
     * </ul>
     * @return 完了ビュー（{@value #VIEW_EDIT_DONE}）または編集ビュー/エラーページ。
     */
    public String customerEditDone() {
        final String idStr       = req.getParameter(P_ID);
        final String companyCode = req.getParameter(P_COMPANY_CODE);
        final String companyName = req.getParameter(P_COMPANY_NAME);
        final String mail        = req.getParameter(P_MAIL);
        final String phone       = req.getParameter(P_PHONE);
        final String postalCode  = req.getParameter(P_POSTAL_CODE);
        final String address1    = req.getParameter(P_ADDRESS1);
        final String address2    = req.getParameter(P_ADDRESS2);
        final String building    = req.getParameter(P_BUILDING);

        validation.isNull("ID", idStr);
        validation.isNull("会社名", companyName);
        if (notBlank(mail))       validation.isEmail(mail);
        if (notBlank(phone))      validation.isPhoneNumber(phone);
        if (notBlank(postalCode)) validation.isPostalCode(postalCode);

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            req.setAttribute(P_ID, idStr);
            pushFormBackToRequest();
            return VIEW_EDIT;
        }

        try (TransactionManager tm = new TransactionManager()) {
            CustomerDAO dao = new CustomerDAO(tm.getConnection());
            UUID id = UUID.fromString(idStr);

            // companyCode 重複（自分除外）の最終確認
            if (notBlank(companyCode) && dao.companyCodeExistsExceptId(companyCode, id)) {
                validation.addErrorMsg("その会社コードは既に使われています。");
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                req.setAttribute(P_ID, idStr);
                pushFormBackToRequest();
                return VIEW_EDIT;
            }

            CustomerDTO dto = new CustomerDTO();
            dto.setId(id);
            dto.setCompanyCode(companyCode);
            dto.setCompanyName(companyName);
            dto.setMail(mail);
            dto.setPhone(phone);
            dto.setPostalCode(postalCode);
            dto.setAddress1(address1);
            dto.setAddress2(address2);
            dto.setBuilding(building);

            int num = dao.update(dto);
            tm.commit();

            req.setAttribute(A_MESSAGE, "更新が完了しました（件数:" + num + "）");
            return VIEW_EDIT_DONE;
        } catch (IllegalArgumentException ex) {
            validation.addErrorMsg("不正なIDが指定されました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // 「【admin】機能：顧客 削除」
    // =========================

    /**
     * 「顧客 論理削除」実行。
     * <ul>
     *   <li>request param {@code id}: 顧客UUID</li>
     *   <li>成功時: 一覧へリダイレクト（{@code /admin/customer}）。</li>
     * </ul>
     * @return リダイレクト先 もしくは エラーページ。
     */
    public String customerDelete() {
        final String idStr = req.getParameter(P_ID);
        try (TransactionManager tm = new TransactionManager()) {
            UUID id = UUID.fromString(idStr);
            CustomerDAO dao = new CustomerDAO(tm.getConnection());
            dao.delete(id);
            tm.commit();
            return req.getContextPath() + "/admin/customer";
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // ④ ヘルパー
    // =========================

    /**
     * 入力値（request param）を同名の属性に詰め替える（JSP の再表示用）。
     * <p>属性キー名はパラメータ名と同一。JSP 互換性を壊さない。</p>
     */
    private void pushFormBackToRequest() {
        req.setAttribute(P_COMPANY_CODE, req.getParameter(P_COMPANY_CODE));
        req.setAttribute(P_COMPANY_NAME, req.getParameter(P_COMPANY_NAME));
        req.setAttribute(P_MAIL,         req.getParameter(P_MAIL));
        req.setAttribute(P_PHONE,        req.getParameter(P_PHONE));
        req.setAttribute(P_POSTAL_CODE,  req.getParameter(P_POSTAL_CODE));
        req.setAttribute(P_ADDRESS1,     req.getParameter(P_ADDRESS1));
        req.setAttribute(P_ADDRESS2,     req.getParameter(P_ADDRESS2));
        req.setAttribute(P_BUILDING,     req.getParameter(P_BUILDING));
    }

    /**
     * 文字列が null・空白でないかの簡易チェック。
     * @param s 文字列
     * @return true=非空白
     */
    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
