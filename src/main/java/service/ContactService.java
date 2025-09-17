package service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import dao.CustomerContactDAO;
import dao.CustomerDAO;
import dao.TransactionManager;
import domain.Customer;
import domain.CustomerContact;
import domain.LoginUser;
import dto.CustomerContactDTO;
import dto.CustomerDTO;

/**
 * 顧客担当者（CustomerContact）のユースケースサービス。
 * <p>
 * 一覧／登録（画面→確認→確定）／編集（画面→確認→確定）／削除の各フローを提供します。
 * 画面遷移名（ビュー名）、リクエスト属性名、リクエストパラメータ名は定数に集約し、
 * サービス内のハードコード散在を防いでいます。DBアクセスは {@link TransactionManager}
 * を try-with-resources で扱い、例外時はエラーページへ遷移します。
 * </p>
 * <p>
 * 入力チェックは {@link BaseService#validation} を使用し、エラー時は
 * エラーメッセージを {@code errorMsg} として詰め、{@link #pushFormBackToRequest()} で
 * 入力値をリクエスト属性へ積み直して元画面に戻します。
 * 顧客ヘッダ等の表示用に {@link #ensureCustomerOnRequest()} で顧客情報を request へ積みます。
 * </p>
 */
public class ContactService extends BaseService {

    // ===== View =====
    private static final String VIEW_HOME         = "contact/admin/home";
    private static final String VIEW_REGISTER     = "contact/admin/register";
    private static final String VIEW_REGISTER_CK  = "contact/admin/register_check";
    private static final String VIEW_REGISTER_DN  = "contact/admin/register_done";
    private static final String VIEW_EDIT         = "contact/admin/edit";
    private static final String VIEW_EDIT_CK      = "contact/admin/edit_check";
    private static final String VIEW_EDIT_DN      = "contact/admin/edit_done";
    private static final String VIEW_MYPAGE           = "mypage/customer/home"; 
    private static final String VIEW_MYPAGE_EDIT      = "mypage/customer/edit"; 
    private static final String VIEW_MYPAGE_EDIT_CHECK= "mypage/customer/edit_check"; 

    // ===== Attr =====
    private static final String A_CUSTOMER  = "customer";
    private static final String A_CONTACTS  = "contacts";
    private static final String A_CONTACT   = "contact";
    private static final String A_ERROR_MSG = "errorMsg";
    private static final String A_MESSAGE   = "message";

    // ===== Param =====
    private static final String P_CUSTOMER_ID = "customerId";          // 顧客ID
    private static final String P_ID          = "id";          // 担当者ID
    private static final String P_NAME        = "name";
    private static final String P_NAME_RUBY   = "nameRuby";
    private static final String P_DEPT        = "department";
    private static final String P_MAIL        = "mail";
    private static final String P_PASSWORD    = "password";    // register 時 NOT NULL
    private static final String P_PHONE       = "phone";
    private static final String P_IS_PRIMARY  = "isPrimary";
    private static final String P_COMPANY_NAME = "companyName";
    private static final String P_POSTAL_CODE  = "postalCode";
    private static final String P_ADDRESS1     = "address1";
    private static final String P_ADDRESS2     = "address2";
    private static final String P_BUILDING     = "building";
    private static final String P_COMPANY_MAIL  = "companyMail";
    private static final String P_COMPANY_PHONE = "companyPhone";
    private static final String P_CONTACT_NAME      = "contactName";
    private static final String P_CONTACT_NAME_RUBY = "contactNameRuby";
    private static final String P_CONTACT_DEPT      = "contactDepartment";
    private static final String P_CONTACT_MAIL      = "contactMail";
    private static final String P_CONTACT_PHONE     = "contactPhone";

    /** DTO↔Domain の変換器（再利用） */
    private final Converter conv = new Converter();

    /**
     * コンストラクタ。
     *
     * @param req   現在の {@link HttpServletRequest}
     * @param useDB DBを使用するかどうか（保持のみ）
     */
    public ContactService(HttpServletRequest req, boolean useDB) { super(req, useDB); }

    // =======================
    // 一覧
    // =======================

    /**
     * 指定顧客の担当者一覧を取得し、一覧ビューへ遷移します。
     * <ul>
     *   <li>顧客IDの妥当性チェック（UUID）</li>
     *   <li>顧客情報の取得と request への積み込み</li>
     *   <li>担当者の取得（顧客IDで絞り込み）と Domain 変換</li>
     *   <li>{@code contacts} 属性へ格納</li>
     * </ul>
     *
     * @return 一覧ビュー名（{@value #VIEW_HOME}）。入力不正・例外時はエラーページ。
     */
    public String contactList() {
        final String cidStr = req.getParameter(P_CUSTOMER_ID);
        if (!validation.isUuid(cidStr)) {
            validation.addErrorMsg("不正な顧客IDが指定されました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
        UUID customerId = UUID.fromString(cidStr);

        try (TransactionManager tm = new TransactionManager()) {
            CustomerDAO cdao = new CustomerDAO(tm.getConnection());
            CustomerDTO cdto = cdao.selectByUUId(customerId);
            Customer customer = conv.toDomain(cdto);

            CustomerContactDAO dao = new CustomerContactDAO(tm.getConnection());
            List<CustomerContactDTO> dtos = dao.selectByCustomerId(customerId);

            List<CustomerContact> contacts = new ArrayList<>(dtos.size());
            for (CustomerContactDTO d : dtos) contacts.add(conv.toDomain(d));

            req.setAttribute(A_CUSTOMER, customer);
            req.setAttribute(A_CONTACTS, contacts);
            return VIEW_HOME;

        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =======================
    // 登録（画面→確認→確定）
    // =======================

    /**
     * 登録画面へ遷移します。
     * <p>顧客ヘッダ表示のため {@link #ensureCustomerOnRequest()} を呼び出します。</p>
     *
     * @return 登録ビュー名（{@value #VIEW_REGISTER}）。顧客ID不正時はエラーページ。
     */
    public String contactRegister() {
        if (!ensureCustomerOnRequest()) return req.getContextPath() + req.getServletPath() + "/error";
        return VIEW_REGISTER;
    }

    /**
     * 登録の確認処理。
     * <p>
     * 必須・形式チェック、メール一意制約（グローバル）を行います。
     * エラー時は {@link #pushFormBackToRequest()} で入力値を戻し、登録画面へ。
     * 問題なければ確認画面へ遷移します。
     * </p>
     *
     * @return 確認ビュー名（{@value #VIEW_REGISTER_CK}）。エラー時は {@value #VIEW_REGISTER}。
     */
    public String contactRegisterCheck() {
        final String cidStr = req.getParameter(P_CUSTOMER_ID);
        final String name   = req.getParameter(P_NAME);
        final String mail   = req.getParameter(P_MAIL);
        final String pass   = req.getParameter(P_PASSWORD);
        final String phone  = req.getParameter(P_PHONE);

        validation.isNull("顧客ID", cidStr);
        validation.isNull("氏名", name);
        validation.isNull("メール", mail);      // NOT NULL
        validation.isNull("パスワード", pass);  // NOT NULL
        validation.isEmail(mail);
        if (notBlank(phone)) validation.isPhoneNumber(phone);

        // mail UNIQUE（グローバル）
        if (!validation.hasErrorMsg()) {
            try (TransactionManager tm = new TransactionManager()) {
                CustomerContactDAO dao = new CustomerContactDAO(tm.getConnection());
                if (dao.mailExists(mail)) {
                    validation.addErrorMsg("このメールアドレスは既に登録されています。");
                }
            }
        }

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            pushFormBackToRequest();
            ensureCustomerOnRequest();
            return VIEW_REGISTER;
        }

        pushFormBackToRequest();
        ensureCustomerOnRequest();
        return VIEW_REGISTER_CK;
    }

    /**
     * 登録の確定処理。
     * <p>
     * 最終チェック（必須・形式・メール一意）を行い、INSERT を実施します。
     * 主担当チェックボックスが ON の場合は、同一顧客の他担当の主担当フラグをクリアし、自分を主担当にします。
     * 完了時はメッセージを設定し、登録完了ビューへ遷移します。
     * </p>
     *
     * @return 完了ビュー名（{@value #VIEW_REGISTER_DN}）。エラー時は登録画面またはエラーページ。
     */
    public String contactRegisterDone() {
        final String cidStr     = req.getParameter(P_CUSTOMER_ID);
        final String name       = req.getParameter(P_NAME);
        final String mail       = req.getParameter(P_MAIL);
        final String pass       = req.getParameter(P_PASSWORD);
        final String phone      = req.getParameter(P_PHONE);
        final String isPrimary  = req.getParameter(P_IS_PRIMARY);

        validation.isNull("顧客ID", cidStr);
        validation.isNull("氏名", name);
        validation.isNull("メール", mail);
        validation.isNull("パスワード", pass);
        validation.isEmail(mail);
        if (notBlank(phone)) validation.isPhoneNumber(phone);

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            pushFormBackToRequest();
            ensureCustomerOnRequest();
            return VIEW_REGISTER;
        }

        try (TransactionManager tm = new TransactionManager()) {
            UUID customerId = UUID.fromString(cidStr);
            CustomerContactDAO dao = new CustomerContactDAO(tm.getConnection());

            // mail 重複再チェック（グローバル）
            if (dao.mailExists(mail)) {
                validation.addErrorMsg("このメールアドレスは既に登録されています。");
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                pushFormBackToRequest();
                ensureCustomerOnRequest();
                return VIEW_REGISTER;
            }

            CustomerDTO c = new CustomerDTO();
            c.setId(customerId);

            CustomerContactDTO dto = new CustomerContactDTO();
            dto.setCustomerDTO(c);
            dto.setName(name);
            dto.setNameRuby(req.getParameter(P_NAME_RUBY));
            dto.setDepartment(req.getParameter(P_DEPT));
            dto.setMail(mail);
            dto.setPassword(pass); // NOT NULL
            dto.setPhone(phone);
            dto.setPrimary("true".equalsIgnoreCase(isPrimary)); // 初期フラグ

            UUID newId = dao.insertReturningId(dto);

            // 主担当にする → 同顧客の他担当をOFF → 自分をON（正規化）
            if (dto.isPrimary()) {
                dao.clearPrimaryForCustomer(customerId);
                dao.setPrimaryById(newId, true);
            }

            tm.commit();
            req.setAttribute(A_MESSAGE, "担当者の登録が完了しました。");
            ensureCustomerOnRequest();
            return VIEW_REGISTER_DN;

        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            ensureCustomerOnRequest();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =======================
    // 編集（画面→確認→確定）
    // =======================

    /**
     * 編集画面表示。
     * <p>
     * 担当者ID／顧客IDの妥当性を確認し、担当者情報を取得して編集ビューへ遷移します。
     * 顧客情報はヘッダ表示のため {@link #ensureCustomerOnRequest()} で request に積みます。
     * </p>
     *
     * @return 編集ビュー名（{@value #VIEW_EDIT}）。ID不正・例外時はエラーページ。
     */
    public String contactEdit() {
        final String idStr  = req.getParameter(P_ID);
        final String cidStr = req.getParameter(P_CUSTOMER_ID);

        if (!validation.isUuid(idStr) || !validation.isUuid(cidStr)) {
            validation.addErrorMsg("不正なIDが指定されました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        try (TransactionManager tm = new TransactionManager()) {
            UUID id = UUID.fromString(idStr);
            CustomerContactDAO dao = new CustomerContactDAO(tm.getConnection());
            CustomerContactDTO dto = dao.selectById(id);
            CustomerContact contact = conv.toDomain(dto);

            ensureCustomerOnRequest();
            req.setAttribute(A_CONTACT, contact);
            return VIEW_EDIT;

        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /**
     * 編集の確認処理。
     * <p>
     * 必須・形式チェックと、メール一意制約（自ID除外）を行います。
     * エラー時は入力値を {@link #pushFormBackToRequest()} で戻し、編集画面へ。
     * 問題なければ確認ビューへ遷移します。
     * </p>
     *
     * @return 確認ビュー名（{@value #VIEW_EDIT_CK}）。エラー時は {@value #VIEW_EDIT}。
     */
    public String contactEditCheck() {
        final String idStr     = req.getParameter(P_ID);
        final String cidStr    = req.getParameter(P_CUSTOMER_ID);
        final String name      = req.getParameter(P_NAME);
        final String mail      = req.getParameter(P_MAIL);
        final String phone     = req.getParameter(P_PHONE);

        validation.isNull("ID", idStr);
        validation.isNull("顧客ID", cidStr);
        validation.isNull("氏名", name);
        validation.isNull("メール", mail);  // NOT NULL
        validation.isEmail(mail);
        if (notBlank(phone)) validation.isPhoneNumber(phone);

        // mail UNIQUE（自ID除外）
        if (!validation.hasErrorMsg() && validation.isUuid(idStr)) {
            try (TransactionManager tm = new TransactionManager()) {
                CustomerContactDAO dao = new CustomerContactDAO(tm.getConnection());
                if (dao.mailExistsExceptId(mail, UUID.fromString(idStr))) {
                    validation.addErrorMsg("このメールアドレスは既に登録されています。");
                }
            }
        }

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            req.setAttribute(P_ID, idStr);
            req.setAttribute(P_CUSTOMER_ID, cidStr);
            pushFormBackToRequest();
            ensureCustomerOnRequest();
            return VIEW_EDIT;
        }

        req.setAttribute(P_ID, idStr);
        req.setAttribute(P_CUSTOMER_ID, cidStr);
        pushFormBackToRequest();
        ensureCustomerOnRequest();
        return VIEW_EDIT_CK;
    }

    /**
     * 編集の確定処理。
     * <p>
     * 最終チェック（必須・形式・メール一意（自ID除外））後、UPDATE を実行します。
     * 主担当指定時は同一顧客で主担当を一意化します。
     * 完了後は完了メッセージを設定し、編集完了ビューへ遷移します。
     * </p>
     *
     * @return 完了ビュー名（{@value #VIEW_EDIT_DN}）。エラー時は編集画面またはエラーページ。
     */
    public String contactEditDone() {
        final String idStr     = req.getParameter(P_ID);
        final String cidStr    = req.getParameter(P_CUSTOMER_ID);
        final String name      = req.getParameter(P_NAME);
        final String mail      = req.getParameter(P_MAIL);
        final String phone     = req.getParameter(P_PHONE);
        final boolean isPrimary= "true".equalsIgnoreCase(req.getParameter(P_IS_PRIMARY));

        validation.isNull("ID", idStr);
        validation.isNull("顧客ID", cidStr);
        validation.isNull("氏名", name);
        validation.isNull("メール", mail);
        validation.isEmail(mail);
        if (notBlank(phone)) validation.isPhoneNumber(phone);

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            req.setAttribute(P_ID, idStr);
            req.setAttribute(P_CUSTOMER_ID, cidStr);
            pushFormBackToRequest();
            ensureCustomerOnRequest();
            return VIEW_EDIT;
        }

        try (TransactionManager tm = new TransactionManager()) {
            UUID id = UUID.fromString(idStr);
            UUID customerId = UUID.fromString(cidStr);
            CustomerContactDAO dao = new CustomerContactDAO(tm.getConnection());

            // mail UNIQUE 再チェック（自ID除外）
            if (dao.mailExistsExceptId(mail, id)) {
                validation.addErrorMsg("このメールアドレスは既に登録されています。");
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                req.setAttribute(P_ID, idStr);
                req.setAttribute(P_CUSTOMER_ID, cidStr);
                pushFormBackToRequest();
                ensureCustomerOnRequest();
                return VIEW_EDIT;
            }

            CustomerContactDTO dto = new CustomerContactDTO();
            dto.setId(id);
            dto.setMail(mail);
            dto.setName(name);
            dto.setNameRuby(req.getParameter(P_NAME_RUBY));
            dto.setPhone(phone);
            dto.setDepartment(req.getParameter(P_DEPT));
            dto.setPrimary(isPrimary);

            int num = dao.update(dto);

            // 主担当の一意化
            if (isPrimary) {
                dao.clearPrimaryForCustomer(customerId);
                dao.setPrimaryById(id, true);
            } else {
                // OFF 指定なら、この担当者だけ OFF に
                dao.setPrimaryById(id, false);
            }

            tm.commit();
            req.setAttribute(A_MESSAGE, "担当者の更新が完了しました（件数:" + num + "）。");
            ensureCustomerOnRequest();
            return VIEW_EDIT_DN;

        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            ensureCustomerOnRequest();
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =======================
    // 削除
    // =======================

    /**
     * 担当者の物理削除を行い、担当者一覧へ戻ります。
     * <p>主担当であっても削除は可能で、特別な解除処理は不要です。</p>
     *
     * @return 一覧URL（リダイレクト想定）。入力不正・例外時はエラーページ。
     */
    public String contactDelete() {
        final String idStr  = req.getParameter(P_ID);
        final String cidStr = req.getParameter(P_CUSTOMER_ID);

        if (!validation.isUuid(idStr) || !validation.isUuid(cidStr)) {
            validation.addErrorMsg("不正なIDが指定されました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        try (TransactionManager tm = new TransactionManager()) {
            UUID id = UUID.fromString(idStr);
            CustomerContactDAO dao = new CustomerContactDAO(tm.getConnection());
            dao.delete(id); // 主担当でも行削除されるため特別な解除は不要
            tm.commit();
            return req.getContextPath() + "/admin/contact?customerId=" + cidStr;

        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }
    
    
    // =========================================================
    // 顧客マイページ（画面）
    // =========================================================

    /** 顧客のマイページ情報を取得して表示。 */
    public String myPageList() {
        UUID customerId = currentCustomerContactId(); // ★「担当者の属する顧客ID」を返す前提
        if (customerId == null) {
            validation.addErrorMsg("ログイン情報が見つかりません。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
        try (TransactionManager tm = new TransactionManager()) {
            CustomerDAO customerdao = new CustomerDAO(tm.getConnection());
            CustomerDTO customerdto = customerdao.selectWithContactsByUuid(customerId);
            if (customerdto == null) {
                validation.addErrorMsg("アカウント情報が取得できませんでした。");
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                return req.getContextPath() + req.getServletPath() + "/error";
            }
            Customer customer = conv.toDomain(customerdto); // DTO→Domain（contactsも詰める実装に）
            req.setAttribute("customer", customer);
            
            var session = req.getSession(false);
            if (session != null) {
                Object u = session.getAttribute("loginUser");
                if (u instanceof LoginUser lu && lu.getCustomerContact() != null) {
                    req.setAttribute("cc", lu.getCustomerContact());
                }
            }
            return VIEW_MYPAGE;
            
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /** 顧客マイページ：編集画面 */
    public String myPageEdit() {
        UUID customerId = currentCustomerContactId();
        UUID contactId  = currentContactId();
        if (customerId == null || contactId == null) {
          validation.addErrorMsg("ログイン情報が見つかりません。");
          req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
          return req.getContextPath() + req.getServletPath() + "/error";
        }
        try (TransactionManager tm = new TransactionManager()) {
            var cDao  = new CustomerDAO(tm.getConnection());
            var ccDao = new CustomerContactDAO(tm.getConnection());

            CustomerDTO  cDto  = cDao.selectByUUId(customerId);
            CustomerContactDTO ccDto = ccDao.selectById(contactId);
            if (cDto == null || ccDto == null) {
                validation.addErrorMsg("アカウント情報が取得できませんでした。");
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                return req.getContextPath() + req.getServletPath() + "/error";
            }
            req.setAttribute("customer",     conv.toDomain(cDto));
            req.setAttribute("cc",           conv.toDomain(ccDto)); // JSPが cc.* を参照
            pushCustomerAndContactFormBackToRequest();              // 入力持ち回し
            return VIEW_MYPAGE_EDIT;
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }


    /** 顧客マイページ：編集確認 */
    public String myPageEditCheck() {
        UUID customerId = currentCustomerContactId();
        UUID contactId  = currentContactId();
        if (customerId == null || contactId == null) {
            validation.addErrorMsg("ログイン情報が見つかりません。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        // 入力取得
        final String cn   = req.getParameter(P_CONTACT_NAME);
        final String cnr  = req.getParameter(P_CONTACT_NAME_RUBY);
        final String dept = req.getParameter(P_CONTACT_DEPT);
        final String cm   = req.getParameter(P_CONTACT_MAIL);
        final String cph  = req.getParameter(P_CONTACT_PHONE);

        final String comp = req.getParameter(P_COMPANY_NAME);
        final String em   = req.getParameter(P_COMPANY_MAIL);
        final String ph   = req.getParameter(P_COMPANY_PHONE);
        final String pc   = req.getParameter(P_POSTAL_CODE);
        final String a1   = req.getParameter(P_ADDRESS1);
        final String a2   = req.getParameter(P_ADDRESS2);
        final String bld  = req.getParameter(P_BUILDING);

        // バリデーション（担当者）
        validation.isNull("氏名", cn);
        validation.isNull("メールアドレス（担当者）", cm);
        if (notBlank(cm))  validation.isEmail(cm);
        if (notBlank(cph)) validation.isPhoneNumber(cph);

        // バリデーション（会社）
        validation.isNull("会社名", comp);
        if (notBlank(em))  validation.isEmail(em);
        if (notBlank(ph))  validation.isPhoneNumber(ph);
        if (notBlank(pc))  validation.isPostalCode(pc);

        // メール重複（担当者：自ID除外）
        if (!validation.hasErrorMsg()) {
            try (TransactionManager tm = new TransactionManager()) {
                var ccDao = new CustomerContactDAO(tm.getConnection());
                if (notBlank(cm) && ccDao.mailExistsExceptId(cm, contactId)) {
                    validation.addErrorMsg("この担当者メールは既に使われています。");
                }
            }
        }

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            pushCustomerAndContactFormBackToRequest();
            return VIEW_MYPAGE_EDIT;
        }

        pushCustomerAndContactFormBackToRequest();
        return VIEW_MYPAGE_EDIT_CHECK;
    }


    /** 顧客マイページ：編集確定 */
    public String myPageEditDone() {
        UUID customerId = currentCustomerContactId();
        UUID contactId  = currentContactId();
        if (customerId == null || contactId == null) {
            validation.addErrorMsg("ログイン情報が見つかりません。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        // 再検証（略：上と同じバリデーション）
        // ...（同じチェックを実施）
        // 失敗時は EDIT に戻す処理も同様

        try (TransactionManager tm = new TransactionManager()) {
            var cDao  = new CustomerDAO(tm.getConnection());
            var ccDao = new CustomerContactDAO(tm.getConnection());

            // --- 会社更新 ---
            CustomerDTO cur = cDao.selectByUUId(customerId);
            CustomerDTO cUpdate = new CustomerDTO();
            cUpdate.setId(cur.getId());
            cUpdate.setCompanyCode(cur.getCompanyCode());
            cUpdate.setPrimaryContactId(cur.getPrimaryContactId());
            cUpdate.setCompanyName(req.getParameter(P_COMPANY_NAME));
            cUpdate.setMail(req.getParameter(P_COMPANY_MAIL));
            cUpdate.setPhone(req.getParameter(P_COMPANY_PHONE));
            cUpdate.setPostalCode(req.getParameter(P_POSTAL_CODE));
            cUpdate.setAddress1(req.getParameter(P_ADDRESS1));
            cUpdate.setAddress2(req.getParameter(P_ADDRESS2));
            cUpdate.setBuilding(req.getParameter(P_BUILDING));
            cDao.update(cUpdate);

            // --- 担当者更新（主担当フラグには触れない） ---
            CustomerContactDTO ccUpdate = new CustomerContactDTO();
            ccUpdate.setId(contactId);
            ccUpdate.setName(req.getParameter(P_CONTACT_NAME));
            ccUpdate.setNameRuby(req.getParameter(P_CONTACT_NAME_RUBY));
            ccUpdate.setDepartment(req.getParameter(P_CONTACT_DEPT));
            ccUpdate.setMail(req.getParameter(P_CONTACT_MAIL));
            ccUpdate.setPhone(req.getParameter(P_CONTACT_PHONE));
            ccDao.update(ccUpdate);

            tm.commit();

            // セッションも最新化（ホームでの表示ブレ防止）
            var session = req.getSession(false);
            if (session != null) {
                Object u = session.getAttribute("loginUser");
                if (u instanceof LoginUser lu) {
                    // customer
                    CustomerDTO cReload = cDao.selectByUUId(customerId);
                    lu.setCustomer(conv.toDomain(cReload));
                    // contact
                    CustomerContactDTO ccReload = ccDao.selectById(contactId);
                    lu.setCustomerContact(conv.toDomain(ccReload));
                    session.setAttribute("loginUser", lu);
                }
            }

            // ★ 完了後はホームへ（リダイレクト）
            return req.getContextPath() + "/customer/mypage/home";
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }


    // =======================
    // helpers
    // =======================

    /**
     * 文字列が null/空白でないかの簡易チェック。
     *
     * @param s 対象文字列
     * @return {@code true}：非nullかつ空白でない
     */
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    /**
     * 入力値をリクエスト属性へ積み直します（エラー時の入力保持に使用）。
     * <p>
     * 画面（JSP）側は {@code ${not empty name ? name : contact.name}} のように
     * 「リクエスト属性優先→既存値」のフォールバックで表示します。
     * </p>
     */
    private void pushFormBackToRequest() {
        req.setAttribute(P_CUSTOMER_ID, req.getParameter(P_CUSTOMER_ID));
        req.setAttribute(P_ID,          req.getParameter(P_ID));
        req.setAttribute(P_NAME,        req.getParameter(P_NAME));
        req.setAttribute(P_NAME_RUBY,   req.getParameter(P_NAME_RUBY));
        req.setAttribute(P_DEPT,        req.getParameter(P_DEPT));
        req.setAttribute(P_MAIL,        req.getParameter(P_MAIL));
        req.setAttribute(P_PASSWORD,    req.getParameter(P_PASSWORD));
        req.setAttribute(P_PHONE,       req.getParameter(P_PHONE));
        req.setAttribute(P_IS_PRIMARY,  req.getParameter(P_IS_PRIMARY));
    }
    
    private void pushCustomerAndContactFormBackToRequest() {
        // Contact
        req.setAttribute(P_CONTACT_NAME,      req.getParameter(P_CONTACT_NAME));
        req.setAttribute(P_CONTACT_NAME_RUBY, req.getParameter(P_CONTACT_NAME_RUBY));
        req.setAttribute(P_CONTACT_DEPT,      req.getParameter(P_CONTACT_DEPT));
        req.setAttribute(P_CONTACT_MAIL,      req.getParameter(P_CONTACT_MAIL));
        req.setAttribute(P_CONTACT_PHONE,     req.getParameter(P_CONTACT_PHONE));
        // Company
        req.setAttribute(P_COMPANY_NAME,  req.getParameter(P_COMPANY_NAME));
        req.setAttribute(P_COMPANY_MAIL,  req.getParameter(P_COMPANY_MAIL));
        req.setAttribute(P_COMPANY_PHONE, req.getParameter(P_COMPANY_PHONE));
        req.setAttribute(P_POSTAL_CODE,   req.getParameter(P_POSTAL_CODE));
        req.setAttribute(P_ADDRESS1,      req.getParameter(P_ADDRESS1));
        req.setAttribute(P_ADDRESS2,      req.getParameter(P_ADDRESS2));
        req.setAttribute(P_BUILDING,      req.getParameter(P_BUILDING));
    }

    /**
     * 顧客ヘッダ表示のため、顧客情報を request に積みます。
     * <p>
     * 顧客IDの妥当性（UUID）を検証し、取得に失敗した場合はエラーメッセージを設定します。
     * </p>
     *
     * @return {@code true}: 取得成功（{@code customer} 属性を設定）／{@code false}: 失敗
     */
    private boolean ensureCustomerOnRequest() {
        final String cidStr = req.getParameter(P_CUSTOMER_ID);
        if (!validation.isUuid(cidStr)) {
            validation.addErrorMsg("不正な顧客IDが指定されました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return false;
        }
        try (TransactionManager tm = new TransactionManager()) {
            CustomerDAO cdao = new CustomerDAO(tm.getConnection());
            CustomerDTO cdto = cdao.selectByUUId(UUID.fromString(cidStr));
            Customer c = conv.toDomain(cdto);
            req.setAttribute(A_CUSTOMER, c);
            return true;
        } catch (RuntimeException e) {
            validation.addErrorMsg("顧客情報の取得に失敗しました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return false;
        }
    }
    
    /** ログイン中の担当者が属する会社IDをセッションから取得 */
    private UUID currentCustomerContactId() { 
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object user = session.getAttribute("loginUser");
        if (user instanceof LoginUser loginUser && loginUser.getCustomerContact() != null) {
            return loginUser.getCustomerContact().getCustomerId();
        }
        return null;
    }
    
    /** セッションからログイン中の担当者IDを取得 */
    private UUID currentContactId() {
        var session = req.getSession(false);
        if (session == null) return null;
        Object u = session.getAttribute("loginUser");
        if (u instanceof LoginUser lu && lu.getCustomerContact() != null) {
            return lu.getCustomerContact().getId();
        }
        return null;
    }
    
}
