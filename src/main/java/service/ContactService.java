package service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dao.CustomerContactDAO;
import dao.CustomerDAO;
import dao.TransactionManager;
import domain.Customer;
import domain.CustomerContact;
import domain.LoginUser;
import dto.CustomerContactDTO;
import dto.CustomerDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import util.ConvertUtil;
import util.PasswordUtil;

/**
 * 顧客担当者（CustomerContact）のユースケースサービス。
 * <p>一覧／登録（画面→確認→確定）／編集（画面→確認→確定）／削除、および顧客マイページの編集フローを提供。</p>
 * <p>ビュー名・属性名・パラメータ名は定数に集約し、ハードコードを排除。DBは {@link TransactionManager} を使用。</p>
 */
public class ContactService extends BaseService {

    // =========================
    // ① 定数（ビュー/属性/パラメータ）
    // =========================

    // ---- View (admin/customer) ----
    private static final String VIEW_HOME              = "contact/admin/home";
    private static final String VIEW_REGISTER          = "contact/admin/register";
    private static final String VIEW_REGISTER_CK       = "contact/admin/register_check";
    private static final String VIEW_REGISTER_DN       = "contact/admin/register_done";
    private static final String VIEW_EDIT              = "contact/admin/edit";
    private static final String VIEW_EDIT_CK           = "contact/admin/edit_check";
    private static final String VIEW_EDIT_DN           = "contact/admin/edit_done";

    // ---- View (customer mypage) ----
    private static final String VIEW_MYPAGE            = "mypage/customer/home";
    private static final String VIEW_MYPAGE_EDIT       = "mypage/customer/edit";
    private static final String VIEW_MYPAGE_EDIT_CHECK = "mypage/customer/edit_check";

    // ---- Request Attributes ----
    private static final String A_CUSTOMER  = "customer";
    private static final String A_CONTACTS  = "contacts";
    private static final String A_CONTACT   = "contact";
    private static final String A_ERROR_MSG = "errorMsg";
    private static final String A_MESSAGE   = "message";

    // ---- Request Parameters ----
    private static final String P_CUSTOMER_ID = "customerId";
    private static final String P_ID          = "id";
    private static final String P_NAME        = "name";
    private static final String P_NAME_RUBY   = "nameRuby";
    private static final String P_DEPT        = "department";
    private static final String P_MAIL        = "mail";
    private static final String P_PASSWORD    = "password";
    private static final String P_PHONE       = "phone";
    private static final String P_IS_PRIMARY  = "isPrimary";

    // 会社 + マイページ用
    private static final String P_COMPANY_NAME   = "companyName";
    private static final String P_POSTAL_CODE    = "postalCode";
    private static final String P_ADDRESS1       = "address1";
    private static final String P_ADDRESS2       = "address2";
    private static final String P_BUILDING       = "building";
    private static final String P_COMPANY_MAIL   = "companyMail";
    private static final String P_COMPANY_PHONE  = "companyPhone";

    // マイページ（担当者）
    private static final String P_CONTACT_NAME       = "contactName";
    private static final String P_CONTACT_NAME_RUBY  = "contactNameRuby";
    private static final String P_CONTACT_DEPT       = "contactDepartment";
    private static final String P_CONTACT_MAIL       = "contactMail";
    private static final String P_CONTACT_PHONE      = "contactPhone";

    // =========================
    // ② フィールド／コンストラクタ
    // =========================

    /** DTO↔Domain の相互変換 */
    private final ConvertUtil conv = new ConvertUtil();

    /**
     * コンストラクタ。
     * @param req   現在の {@link HttpServletRequest}
     * @param useDB DB使用フラグ（BaseService 踏襲）
     */
    public ContactService(HttpServletRequest req, boolean useDB) {
        super(req, useDB);
    }

    // =========================
    // ③ コントローラ呼び出しメソッド
    // ====== 【admin】担当者（顧客別） ======
    // =========================

    // -------------------------
    // 「【admin】 機能：担当者一覧」
    // -------------------------
    /**
     * 指定顧客の担当者一覧を表示。
     * - customerId: request param 'customerId'（必須, UUID）
     * - 取得: 顧客情報, 担当者一覧
     * - setAttribute: 'customer', 'contacts'
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

    // -------------------------
    // 「【admin】 機能：担当者登録（画面）」
    // -------------------------
    /**
     * 担当者登録画面の表示。
     * - customerId: request param 'customerId'（必須, UUID）
     * - 顧客ヘッダ用に 'customer' を積む
     */
    public String contactRegister() {
        if (!ensureCustomerOnRequest()) return req.getContextPath() + req.getServletPath() + "/error";
        return VIEW_REGISTER;
    }

    // -------------------------
    // 「【admin】 機能：担当者登録（確認）」
    // -------------------------
    /**
     * 担当者登録の確認。
     * - 必須: customerId, name, mail, password
     * - 形式: mail（Email）, phone（任意・形式）
     * - 一意: mail（グローバル）
     * - 失敗時: errorMsg, 入力値を request に積み直し、登録画面へ戻す
     */
    public String contactRegisterCheck() {
        final String cidStr = req.getParameter(P_CUSTOMER_ID);
        final String name   = req.getParameter(P_NAME);
        final String mail   = req.getParameter(P_MAIL);
        final String pass   = req.getParameter(P_PASSWORD);
        final String phone  = req.getParameter(P_PHONE);

        validation.isNull("顧客ID", cidStr);
        validation.isNull("氏名", name);
        validation.isNull("メール", mail);
        validation.isNull("パスワード", pass);
        validation.isEmail(mail);
        if (notBlank(phone)) validation.isPhoneNumber(phone);

        if (!validation.hasErrorMsg()) {
            try (TransactionManager tm = new TransactionManager()) {
                CustomerContactDAO dao = new CustomerContactDAO(tm.getConnection());
                if (dao.mailExists(mail)) validation.addErrorMsg("このメールアドレスは既に登録されています。");
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

    // -------------------------
    // 「【admin】 機能：担当者登録（確定）」
    // -------------------------
    /**
     * 担当者登録の確定。
     * - 重複再検証: mail（グローバル）
     * - 主担当ON時: 同顧客の他担当の主担当をクリア→自分をON
     * - 完了時: 'message' を設定して完了画面へ
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
        validation.isStrongPassword(pass);
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
            dto.setPassword(PasswordUtil.hashPassword(pass));
            dto.setPhone(phone);
            dto.setPrimary("true".equalsIgnoreCase(isPrimary));

            UUID newId = dao.insertReturningId(dto);

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

    // -------------------------
    // 「【admin】 機能：担当者編集（画面）」
    // -------------------------
    /**
     * 担当者編集画面の表示。
     * - id, customerId: request param（必須, UUID）
     * - setAttribute: 'customer', 'contact'
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

    // -------------------------
    // 「【admin】 機能：担当者編集（確認）」
    // -------------------------
    /**
     * 担当者編集の確認。
     * - 必須: id, customerId, name, mail
     * - 一意: mail（自ID除外）
     * - 失敗時: errorMsg, 入力値・IDを request に積み直し、編集画面へ戻す
     */
    public String contactEditCheck() {
        final String idStr  = req.getParameter(P_ID);
        final String cidStr = req.getParameter(P_CUSTOMER_ID);
        final String name   = req.getParameter(P_NAME);
        final String mail   = req.getParameter(P_MAIL);
        final String phone  = req.getParameter(P_PHONE);

        validation.isNull("ID", idStr);
        validation.isNull("顧客ID", cidStr);
        validation.isNull("氏名", name);
        validation.isNull("メール", mail);
        validation.isEmail(mail);
        if (notBlank(phone)) validation.isPhoneNumber(phone);

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

    // -------------------------
    // 「【admin】 機能：担当者編集（確定）」
    // -------------------------
    /**
     * 担当者編集の確定。
     * - 一意再検証: mail（自ID除外）
     * - 主担当ON: 同顧客の他担当をOFF→自分をON／OFF: 自分だけOFF
     * - 完了時: 'message' を設定して完了画面へ
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

            if (isPrimary) {
                dao.clearPrimaryForCustomer(customerId);
                dao.setPrimaryById(id, true);
            } else {
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

    // -------------------------
    // 「【admin】 機能：担当者削除」
    // -------------------------
    /**
     * 担当者の物理削除。
     * - id, customerId: request param（必須, UUID）
     * - 成功時: /admin/contact?customerId=... にリダイレクト
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
            dao.delete(id);
            tm.commit();
            return req.getContextPath() + "/admin/contact?customerId=" + cidStr;

        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // ====== 【customer】マイページ ======
    // =========================

    // -------------------------
    // 「【customer】 機能：マイページ表示」
    // -------------------------
    /**
     * 顧客マイページ表示。
     * - セッション: loginUser.customerContact から顧客IDを取得
     * - setAttribute: 'customer', 'cc'
     */
    public String myPageList() {
        UUID customerId = currentCustomerContactId();
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
            Customer customer = conv.toDomain(customerdto);
            req.setAttribute(A_CUSTOMER, customer);

            HttpSession session = req.getSession(false);
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

    // -------------------------
    // 「【customer】 機能：マイページ編集（画面）」
    // -------------------------
    /**
     * 顧客マイページ編集画面の表示。
     * - セッション: 顧客ID / 担当者ID 必須
     * - setAttribute: 'customer', 'cc'（入力保持も反映）
     */
    public String myPageEdit() {
        UUID customerId = currentCustomerContactId();
        UUID contactId  = currentContactId();
        if (customerId == null || contactId == null) {
            validation.addErrorMsg("ログイン情報が見つかりません。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
        try (TransactionManager tm = new TransactionManager()) {
            CustomerDAO cDao  = new CustomerDAO(tm.getConnection());
            CustomerContactDAO ccDao = new CustomerContactDAO(tm.getConnection());

            CustomerDTO cDto = cDao.selectByUUId(customerId);
            CustomerContactDTO ccDto = ccDao.selectById(contactId);
            if (cDto == null || ccDto == null) {
                validation.addErrorMsg("アカウント情報が取得できませんでした。");
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                return req.getContextPath() + req.getServletPath() + "/error";
            }
            req.setAttribute(A_CUSTOMER, conv.toDomain(cDto));
            req.setAttribute("cc",       conv.toDomain(ccDto));
            pushCustomerAndContactFormBackToRequest();
            return VIEW_MYPAGE_EDIT;
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // -------------------------
    // 「【customer】 機能：マイページ編集（確認）」
    // -------------------------
    /**
     * 顧客マイページ編集の確認。
     * - 担当者: 氏名/メール必須、メール形式＆重複（自ID除外）、電話形式
     * - 会社  : 会社名必須、メール/電話/郵便番号形式
     * - setAttribute: 入力保持
     */
    public String myPageEditCheck() {
        UUID customerId = currentCustomerContactId();
        UUID contactId  = currentContactId();
        if (customerId == null || contactId == null) {
            validation.addErrorMsg("ログイン情報が見つかりません。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        // 入力
        final String cn   = req.getParameter(P_CONTACT_NAME);
        final String cm   = req.getParameter(P_CONTACT_MAIL);
        final String cph  = req.getParameter(P_CONTACT_PHONE);
        final String comp = req.getParameter(P_COMPANY_NAME);
        final String em   = req.getParameter(P_COMPANY_MAIL);
        final String ph   = req.getParameter(P_COMPANY_PHONE);
        final String pc   = req.getParameter(P_POSTAL_CODE);

        // 担当者
        validation.isNull("氏名", cn);
        validation.isNull("メールアドレス（担当者）", cm);
        if (notBlank(cm))  validation.isEmail(cm);
        if (notBlank(cph)) validation.isPhoneNumber(cph);
        // 会社
        validation.isNull("会社名", comp);
        if (notBlank(em))  validation.isEmail(em);
        if (notBlank(ph))  validation.isPhoneNumber(ph);
        if (notBlank(pc))  validation.isPostalCode(pc);

        // メール重複（自ID除外）
        if (!validation.hasErrorMsg()) {
            try (TransactionManager tm = new TransactionManager()) {
                CustomerContactDAO ccDao = new CustomerContactDAO(tm.getConnection());
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

    // -------------------------
    // 「【customer】 機能：マイページ編集（確定）」
    // -------------------------
    /**
     * 顧客マイページ編集の確定。
     * - 会社情報: Customer を UPDATE
     * - 担当者情報: CustomerContact を UPDATE（主担当フラグは不変更）
     * - セッションの loginUser も再詰め替え（表示ブレ防止）
     * - 完了時: /customer/mypage/home へリダイレクト
     */
    public String myPageEditDone() {
        UUID customerId = currentCustomerContactId();
        UUID contactId  = currentContactId();
        if (customerId == null || contactId == null) {
            validation.addErrorMsg("ログイン情報が見つかりません。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
        // ※ バリデーションは myPageEditCheck() と同等に行う想定（省略可）
        try (TransactionManager tm = new TransactionManager()) {
            CustomerDAO cDao  = new CustomerDAO(tm.getConnection());
            CustomerContactDAO ccDao = new CustomerContactDAO(tm.getConnection());

            // 会社 UPDATE
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

            // 担当者 UPDATE（主担当には触れない）
            CustomerContactDTO ccUpdate = new CustomerContactDTO();
            ccUpdate.setId(contactId);
            ccUpdate.setName(req.getParameter(P_CONTACT_NAME));
            ccUpdate.setNameRuby(req.getParameter(P_CONTACT_NAME_RUBY));
            ccUpdate.setDepartment(req.getParameter(P_CONTACT_DEPT));
            ccUpdate.setMail(req.getParameter(P_CONTACT_MAIL));
            ccUpdate.setPhone(req.getParameter(P_CONTACT_PHONE));
            ccDao.update(ccUpdate);

            tm.commit();

            // セッションも更新
            HttpSession session = req.getSession(false);
            if (session != null) {
                Object u = session.getAttribute("loginUser");
                if (u instanceof LoginUser lu) {
                    CustomerDTO cReload = cDao.selectByUUId(customerId);
                    lu.setCustomer(conv.toDomain(cReload));
                    CustomerContactDTO ccReload = ccDao.selectById(contactId);
                    lu.setCustomerContact(conv.toDomain(ccReload));
                    session.setAttribute("loginUser", lu);
                }
            }

            return req.getContextPath() + "/customer/mypage/home";
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================
    // ④ ヘルパー
    // =========================

    /** 空白以外を判定するユーティリティ。 */
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    /**
     * 入力値を request 属性へ積み直し（登録/編集フォームの入力保持）。
     * JSP 側は「属性優先→既存値」で表示する想定。
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

    /** マイページ（会社/担当者）の入力保持をまとめて積む。 */
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
     * 顧客ヘッダ表示のため、request に 'customer' を積む。
     * @return true: 成功／false: 失敗（errorMsg 設定済み）
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

    /** セッションの loginUser.customerContact から「顧客ID」を取得（なければ null）。 */
    private UUID currentCustomerContactId() {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object user = session.getAttribute("loginUser");
        if (user instanceof LoginUser loginUser && loginUser.getCustomerContact() != null) {
            return loginUser.getCustomerContact().getCustomerId();
        }
        return null;
    }

    /** セッションの loginUser.customerContact から「担当者ID」を取得（なければ null）。 */
    private UUID currentContactId() {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object u = session.getAttribute("loginUser");
        if (u instanceof LoginUser lu && lu.getCustomerContact() != null) {
            return lu.getCustomerContact().getId();
        }
        return null;
    }
}
