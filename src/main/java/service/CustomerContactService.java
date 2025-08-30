package service;

import java.util.*;
import jakarta.servlet.http.HttpServletRequest;

import dao.CustomerContactDAO;
import dao.CustomerDAO;
import dao.TransactionManager;
import domain.Customer;
import domain.CustomerContact;
import dto.CustomerContactDTO;
import dto.CustomerDTO;

/**
 * 顧客担当者（CustomerContact）のユースケースサービス（mail UNIQUE / password NOT NULL / is_primary 対応）
 */
public class CustomerContactService extends BaseService {

    // ===== View =====
    private static final String VIEW_HOME         = "customer_contact/admin/home";
    private static final String VIEW_REGISTER     = "customer_contact/admin/register";
    private static final String VIEW_REGISTER_CK  = "customer_contact/admin/register_check";
    private static final String VIEW_REGISTER_DN  = "customer_contact/admin/register_done";
    private static final String VIEW_EDIT         = "customer_contact/admin/edit";
    private static final String VIEW_EDIT_CK      = "customer_contact/admin/edit_check";
    private static final String VIEW_EDIT_DN      = "customer_contact/admin/edit_done";

    // ===== Attr =====
    private static final String A_CUSTOMER  = "customer";
    private static final String A_CONTACTS  = "contacts";
    private static final String A_CONTACT   = "contact";
    private static final String A_ERROR_MSG = "errorMsg";
    private static final String A_MESSAGE   = "message";

    // ===== Param =====
    private static final String P_CUSTOMER_ID = "customerId";
    private static final String P_ID          = "id";
    private static final String P_NAME        = "name";
    private static final String P_NAME_RUBY   = "nameRuby";
    private static final String P_DEPT        = "department";
    private static final String P_MAIL        = "mail";
    private static final String P_PASSWORD    = "password";   // ★ register 必須
    private static final String P_PHONE       = "phone";
    private static final String P_IS_PRIMARY  = "isPrimary";

    private final Converter conv = new Converter();

    public CustomerContactService(HttpServletRequest req, boolean useDB) { super(req, useDB); }

    // =======================
    // 一覧
    // =======================
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
    public String contactRegister() {
        if (!ensureCustomerOnRequest()) return req.getContextPath() + req.getServletPath() + "/error";
        return VIEW_REGISTER;
    }

    public String contactRegisterCheck() {
        final String cidStr = req.getParameter(P_CUSTOMER_ID);
        final String name   = req.getParameter(P_NAME);
        final String mail   = req.getParameter(P_MAIL);
        final String pass   = req.getParameter(P_PASSWORD);
        final String phone  = req.getParameter(P_PHONE);

        validation.isNull("顧客ID", cidStr);
        validation.isNull("氏名", name);
        validation.isNull("メール", mail);      // ★ NOT NULL
        validation.isNull("パスワード", pass);  // ★ NOT NULL
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

    public String contactEditCheck() {
        final String idStr     = req.getParameter(P_ID);
        final String cidStr    = req.getParameter(P_CUSTOMER_ID);
        final String name      = req.getParameter(P_NAME);
        final String mail      = req.getParameter(P_MAIL);
        final String phone     = req.getParameter(P_PHONE);

        validation.isNull("ID", idStr);
        validation.isNull("顧客ID", cidStr);
        validation.isNull("氏名", name);
        validation.isNull("メール", mail);  // ★ NOT NULL
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
            return req.getContextPath() + "/admin/customer/contact?customerId=" + cidStr;

        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =======================
    // helpers
    // =======================
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

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

    /** 会社ヘッダ表示のため顧客情報を request に積む */
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
}