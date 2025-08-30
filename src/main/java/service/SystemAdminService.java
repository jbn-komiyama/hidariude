package service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dao.SystemAdminDAO;
import dao.TransactionManager;
import dto.SystemAdminDTO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * システム管理者（system_admins）の画面遷移・ユースケースを担うサービス。
 * <p>バリデーションは {@link BaseService#validation} を利用し、DBアクセスは {@link TransactionManager} 経由で行う。</p>
 */
public class SystemAdminService extends BaseService {

    // ====== View names ======
    private static final String VIEW_HOME           = "system_admin/admin/home";
    private static final String VIEW_REGISTER       = "system_admin/admin/register";
    private static final String VIEW_REGISTER_CHECK = "system_admin/admin/register_check";
    private static final String VIEW_REGISTER_DONE  = "system_admin/admin/register_done";
    private static final String VIEW_EDIT           = "system_admin/admin/edit";
    private static final String VIEW_EDIT_CHECK     = "system_admin/admin/edit_check";
    private static final String VIEW_EDIT_DONE      = "system_admin/admin/edit_done";

    // ====== Attr keys ======
    private static final String A_ADMINS   = "admins";
    private static final String A_ADMIN    = "admin";
    private static final String A_ERROR    = "errorMsg";
    private static final String A_MESSAGE  = "message";

    // ====== Param keys ======
    private static final String P_ID       = "id";
    private static final String P_MAIL     = "mail";
    private static final String P_PASSWORD = "password";
    private static final String P_NAME     = "name";
    private static final String P_NAME_RUBY= "nameRuby";

    public SystemAdminService(HttpServletRequest req, boolean useDB) {
        super(req, useDB);
    }

    /**
     * 一覧表示。
     * @return ビュー名
     */
    public String systemAdminList() {
        try (TransactionManager tm = new TransactionManager()) {
            SystemAdminDAO dao = new SystemAdminDAO(tm.getConnection());
            List<SystemAdminDTO> list = dao.selectAll();
            req.setAttribute(A_ADMINS, new ArrayList<>(list));
            return VIEW_HOME;
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /**
     * 新規登録 画面表示。
     */
    public String systemAdminRegister() {
        return VIEW_REGISTER;
    }

    /**
     * 新規登録 確認。
     */
    public String systemAdminRegisterCheck() {
        final String mail = req.getParameter(P_MAIL);
        final String password = req.getParameter(P_PASSWORD);
        final String name = req.getParameter(P_NAME);
        final String nameRuby = req.getParameter(P_NAME_RUBY);

        validation.isNull("氏名", name);
        validation.isNull("メールアドレス", mail);
        validation.isNull("パスワード", password);
        validation.isEmail(mail);

        if (!validation.hasErrorMsg()) {
            try (TransactionManager tm = new TransactionManager()) {
                SystemAdminDAO dao = new SystemAdminDAO(tm.getConnection());
                if (dao.mailExists(mail)) {
                    validation.addErrorMsg("そのメールアドレスは既に登録済みです。");
                }
            }
        }

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            pushBack(name, nameRuby, mail, password);
            return VIEW_REGISTER;
        }

        pushBack(name, nameRuby, mail, password);
        return VIEW_REGISTER_CHECK;
    }

    /**
     * 新規登録 確定。
     */
    public String systemAdminRegisterDone() {
        final String mail = req.getParameter(P_MAIL);
        final String password = req.getParameter(P_PASSWORD);
        final String name = req.getParameter(P_NAME);
        final String nameRuby = req.getParameter(P_NAME_RUBY);

        validation.isNull("氏名", name);
        validation.isNull("メールアドレス", mail);
        validation.isNull("パスワード", password);
        validation.isEmail(mail);

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            pushBack(name, nameRuby, mail, password);
            return VIEW_REGISTER;
        }

        try (TransactionManager tm = new TransactionManager()) {
            SystemAdminDAO dao = new SystemAdminDAO(tm.getConnection());

            if (dao.mailExists(mail)) {
                validation.addErrorMsg("そのメールアドレスは既に登録済みです。");
                req.setAttribute(A_ERROR, validation.getErrorMsg());
                pushBack(name, nameRuby, mail, password);
                return VIEW_REGISTER;
            }

            SystemAdminDTO dto = new SystemAdminDTO();
            dto.setMail(mail);
            dto.setPassword(password);
            dto.setName(name);
            dto.setNameRuby(nameRuby);
            int num = dao.insert(dto);
            tm.commit();
            req.setAttribute(A_MESSAGE, "登録が完了しました（件数:" + num + "）");
            return VIEW_REGISTER_DONE;
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /**
     * 編集 画面表示。
     */
    public String systemAdminEdit() {
        final String idStr = req.getParameter(P_ID);
        try (TransactionManager tm = new TransactionManager()) {
            UUID id = UUID.fromString(idStr);
            SystemAdminDAO dao = new SystemAdminDAO(tm.getConnection());
            SystemAdminDTO dto = dao.selectById(id);
            req.setAttribute(A_ADMIN, dto);
            return VIEW_EDIT;
        } catch (IllegalArgumentException ex) {
            validation.addErrorMsg("不正なIDが指定されました。");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /**
     * 編集 確認。
     */
    public String systemAdminEditCheck() {
        final String idStr = req.getParameter(P_ID);
        final String mail = req.getParameter(P_MAIL);
        final String password = req.getParameter(P_PASSWORD);
        final String name = req.getParameter(P_NAME);
        final String nameRuby = req.getParameter(P_NAME_RUBY);

        validation.isNull("ID", idStr);
        validation.isNull("氏名", name);
        validation.isNull("メールアドレス", mail);
        validation.isEmail(mail);

        if (!validation.hasErrorMsg()) {
            try (TransactionManager tm = new TransactionManager()) {
                UUID id = UUID.fromString(idStr);
                SystemAdminDAO dao = new SystemAdminDAO(tm.getConnection());
                if (dao.mailExistsExceptId(mail, id)) {
                    validation.addErrorMsg("そのメールアドレスは既に登録済みです。");
                }
            } catch (IllegalArgumentException ignore) {
                validation.addErrorMsg("不正なIDが指定されました。");
            }
        }

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            req.setAttribute(P_ID, idStr);
            pushBack(name, nameRuby, mail, password);
            return VIEW_EDIT;
        }

        req.setAttribute(P_ID, idStr);
        pushBack(name, nameRuby, mail, password);
        return VIEW_EDIT_CHECK;
    }

    /**
     * 編集 確定。
     */
    public String systemAdminEditDone() {
        final String idStr = req.getParameter(P_ID);
        final String mail = req.getParameter(P_MAIL);
        final String password = req.getParameter(P_PASSWORD);
        final String name = req.getParameter(P_NAME);
        final String nameRuby = req.getParameter(P_NAME_RUBY);

        validation.isNull("ID", idStr);
        validation.isNull("氏名", name);
        validation.isNull("メールアドレス", mail);
        validation.isEmail(mail);

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            req.setAttribute(P_ID, idStr);
            pushBack(name, nameRuby, mail, password);
            return VIEW_EDIT;
        }

        try (TransactionManager tm = new TransactionManager()) {
            UUID id = UUID.fromString(idStr);
            SystemAdminDAO dao = new SystemAdminDAO(tm.getConnection());

            if (dao.mailExistsExceptId(mail, id)) {
                validation.addErrorMsg("そのメールアドレスは既に登録済みです。");
                req.setAttribute(A_ERROR, validation.getErrorMsg());
                req.setAttribute(P_ID, idStr);
                pushBack(name, nameRuby, mail, password);
                return VIEW_EDIT;
            }

            SystemAdminDTO dto = dao.selectById(id);
            if (dto.getId() == null) {
                validation.addErrorMsg("対象データが存在しません。");
                req.setAttribute(A_ERROR, validation.getErrorMsg());
                return req.getContextPath() + req.getServletPath() + "/error";
            }

            dto.setMail(mail);
            dto.setName(name);
            dto.setNameRuby(nameRuby);
            if (password != null && !password.isBlank()) {
                dto.setPassword(password);
            } else {
                dto.setPassword(dto.getPassword());
            }

            int num = dao.update(dto);
            tm.commit();
            req.setAttribute(A_MESSAGE, "更新が完了しました（件数:" + num + "）");
            return VIEW_EDIT_DONE;
        } catch (IllegalArgumentException ex) {
            validation.addErrorMsg("不正なIDが指定されました。");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /**
     * 論理削除。
     */
    public String systemAdminDelete() {
        final String idStr = req.getParameter(P_ID);
        try (TransactionManager tm = new TransactionManager()) {
            UUID id = UUID.fromString(idStr);
            SystemAdminDAO dao = new SystemAdminDAO(tm.getConnection());
            dao.delete(id);
            tm.commit();
            return req.getContextPath() + "/admin/system_admin";
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    private void pushBack(String name, String nameRuby, String mail, String password) {
        req.setAttribute(P_NAME, name);
        req.setAttribute(P_NAME_RUBY, nameRuby);
        req.setAttribute(P_MAIL, mail);
        req.setAttribute(P_PASSWORD, password);
    }
}