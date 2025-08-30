package service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import dao.CustomerDAO;
import dao.TransactionManager;
import domain.Customer;
import dto.CustomerDTO;

/**
 * 顧客サービス（一覧／登録／更新／削除）を扱うクラス。
 * <p>
 * 画面遷移名（ビュー名）、リクエスト属性名、リクエストパラメータ名を定数として集約し、
 * サービス内でのハードコードの散在を防ぎます。DBアクセスは {@link TransactionManager}
 * を try-with-resources で利用し、例外時はエラーページへ遷移します。
 * </p>
 * <p>
 * 入力チェックは {@link BaseService#validation} を用い、エラー時はエラーメッセージを
 * リクエスト属性 {@code errorMsg} に詰めて元画面へ戻します。
 * </p>
 */
public class CustomerService extends BaseService{
	
	// ===== ビュー名 =====
    private static final String VIEW_HOME      		= "customer/admin/home";
    private static final String VIEW_REGISTER			= "customer/admin/register";    
    private static final String VIEW_REGISTER_CHECK	= "customer/admin/register_check";    
    private static final String VIEW_REGISTER_DONE 	= "customer/admin/register_done"; 
    private static final String VIEW_EDIT         		= "customer/admin/edit";         
    private static final String VIEW_EDIT_CHECK		= "customer/admin/edit_check"; 
    private static final String VIEW_EDIT_DONE    	= "customer/admin/edit_done";   

    // ===== 属性名 =====
    private static final String A_CUSTOMERS = "customers";
    private static final String A_CUSTOMER  = "customer";
    private static final String A_MESSAGE   = "message";
    private static final String A_ERROR_MSG = "errorMsg";

    // ===== パラメータ名 =====
    private static final String P_ID           = "id";
    private static final String P_COMPANY_CODE = "companyCode";
    private static final String P_COMPANY_NAME = "companyName";
    private static final String P_MAIL         = "mail";
    private static final String P_PHONE        = "phone";
    private static final String P_POSTAL_CODE  = "postalCode";
    private static final String P_ADDRESS1     = "address1";
    private static final String P_ADDRESS2     = "address2";
    private static final String P_BUILDING     = "building";

    /** DTO→ドメイン変換器（都度 new せず再利用） */
    private final Converter conv = new Converter();

    
    /**
     * コンストラクタ。
     *
     * @param req   現在の {@link HttpServletRequest}
     * @param useDB DBを使用するかどうか（本実装ではフラグ自体は保持のみ）
     */
    public CustomerService(HttpServletRequest req, boolean useDB) {
        super(req, useDB);
    }

    
    // =======================
    // 一覧
    // =======================
    
    /**
     * 顧客一覧を取得して一覧画面へ遷移します。
     * <ul>
     *   <li>DAOで全件取得（論理削除を除外）</li>
     *   <li>DTOをドメインへ変換</li>
     *   <li>リクエスト属性 {@code customers} へ格納</li>
     * </ul>
     *
     * @return 一覧画面のビュー名（{@value #VIEW_HOME}）。例外時はエラーページ。
     */
    public String customerList() {
        try (TransactionManager tm = new TransactionManager()) {
            CustomerDAO dao = new CustomerDAO(tm.getConnection());
            List<CustomerDTO> dtos = dao.selectAll();

            // DTO -> Domain 変換
            List<Customer> customers = new ArrayList<>(dtos.size()); //初期容量最適化
            for (CustomerDTO dto : dtos) {
                customers.add(conv.toDomain(dto)); // Converter の再利用
            }

            req.setAttribute("customers", customers);
            return VIEW_HOME; // ★ CHANGED
        } catch (RuntimeException e) {
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }
    
    
    // =======================
    // 新規登録（画面／確定）
    // =======================
    
    /**
     * 新規登録画面へ遷移します。
     *
     * @return 新規登録画面のビュー名（{@value #VIEW_REGISTER}）
     */
    public String customerRegister() {
        return VIEW_REGISTER;
    }
    
    
    /**
     * ★ ADDED: 新規登録の確認処理。
     * <p>
     * 入力値の必須・形式チェックと、company_code の重複チェックを行い、
     * 問題なければ確認画面へ遷移します。エラー時は入力値をそのまま属性へ戻して
     * 登録画面に戻ります。
     * </p>
     *
     * @return 確認画面のビュー名（{@value #VIEW_REGISTER_CHECK}）。エラー時は {@value #VIEW_REGISTER}。
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
        // 任意チェック（入力がある場合のみ）
        if (notBlank(mail))       validation.isEmail(mail);
        if (notBlank(phone))      validation.isPhoneNumber(phone);
        if (notBlank(postalCode)) validation.isPostalCode(postalCode);
        
        // DB重複チェック
        if (!validation.hasErrorMsg() && notBlank(companyCode)) {
            try (TransactionManager tm = new TransactionManager()) {
                CustomerDAO dao = new CustomerDAO(tm.getConnection());
                if (dao.companyCodeExists(companyCode)) {
                    validation.addErrorMsg("その会社コードは既に使われています。");
                }
            } catch (RuntimeException ignore) { /* 例外時は従来どおりエラーページに遷移するためここでは握りつぶしません */ }
        }

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            pushFormBackToRequest(); // 入力値を戻す
            return VIEW_REGISTER;
        }

        // 確認画面は hidden で値を引き継ぐため、属性にも積んでおく
        pushFormBackToRequest();
        return VIEW_REGISTER_CHECK;
    }
    
    
    /**
     * 新規登録の確定処理。
     * <p>
     * 最終チェック（必須・形式・company_code 重複）を行い、問題なければ INSERT を実行します。
     * 完了後は完了画面へ遷移します。
     * </p>
     *
     * @return 完了画面のビュー名（{@value #VIEW_REGISTER_DONE}）。エラー時は登録画面。
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

            // ★ CHANGED: 重複再チェック
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
    
    
    
    // =====================================================
    // 編集（画面 → 確認 → 確定）
    // =====================================================

    /**
     * 編集画面表示（一覧の「編集」ボタンから遷移）。
     * <p>
     * 指定された ID の顧客を取得し、編集画面へ遷移します。
     * </p>
     *
     * @return 編集画面のビュー名（{@value #VIEW_EDIT}）。ID不正や例外時はエラーページ。
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
     * ★ ADDED: 編集の確認処理。
     * <p>
     * 入力値の必須・形式チェックに加えて、company_code の重複（自ID除外）を確認します。
     * 問題なければ確認画面へ遷移、エラー時は編集画面へ戻します。
     * </p>
     *
     * @return 確認画面のビュー名（{@value #VIEW_EDIT_CHECK}）。エラー時は {@value #VIEW_EDIT}。
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

        // DB重複チェック（自分は除外）
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
     * 編集の確定処理。
     * <p>
     * 最終チェック（必須・形式・company_code 重複（自ID除外））を行い、問題なければ UPDATE を実行します。
     * 完了後は編集完了画面へ遷移します。
     * </p>
     *
     * @return 完了画面のビュー名（{@value #VIEW_EDIT_DONE}）。エラー時は編集画面またはエラーページ。
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

            // ★ CHANGED: 重複再チェック（自分は除外）
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


    
    // =====================================================
    // 削除
    // =====================================================

    /**
     * 顧客の論理削除（deleted_at を現在時刻に更新）を行い、一覧へ戻ります。
     *
     * @return 一覧URL（リダイレクト先想定）。例外時はエラーページ。
     */
    public String customerDelete() {
        final String idStr = req.getParameter(P_ID);
        try (TransactionManager tm = new TransactionManager()) {
            UUID id = UUID.fromString(idStr);
            CustomerDAO dao = new CustomerDAO(tm.getConnection());
            dao.delete(id);
            tm.commit();
            return req.getContextPath() + "/admin/customer"; // 一覧へ
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    
    
    // =====================================================
    // Helper
    // =====================================================

    /** リクエストから読み取った入力値を、そのままリクエスト属性に積み直す。 */
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

    /** 文字列が null/空白でないかの簡易チェック */
    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
