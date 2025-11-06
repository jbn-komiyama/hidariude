package service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import dao.SystemAdminDAO;
import dao.TransactionManager;
import domain.LoginUser;
import dto.SystemAdminDTO;
import util.PasswordUtil;

/**
 * システム管理者（system_admins）に関する画面遷移・ユースケースを担うサービス（admin用）。
 *
 * 入力値の検証は {@link BaseService#validation} を利用し、DBアクセスは {@link TransactionManager} 経由で行う。
 * 例外発生時はエラーメッセージを {@code errorMsg} に格納し、共通エラーページへ遷移する。
 */
public class SystemAdminService extends BaseService {

    /**
     * ① 定数・共通化（View / Attr / Param）
     */

    /**
     * View names
     */
    private static final String VIEW_HOME           = "systemadmin/admin/home";
    private static final String VIEW_REGISTER       = "systemadmin/admin/register";
    private static final String VIEW_REGISTER_CHECK = "systemadmin/admin/register_check";
    private static final String VIEW_REGISTER_DONE  = "systemadmin/admin/register_done";
    private static final String VIEW_EDIT           = "systemadmin/admin/edit";
    private static final String VIEW_EDIT_CHECK     = "systemadmin/admin/edit_check";
    private static final String VIEW_EDIT_DONE      = "systemadmin/admin/edit_done";

    /**
     * Attribute keys (JSPへ渡すキー名)
     */
    private static final String A_ADMINS  = "admins";
    private static final String A_ADMIN   = "admin";
    private static final String A_ERROR   = "errorMsg";
    private static final String A_MESSAGE = "message";

    /**
     * Request parameter keys（JSP→Controller/Service）
     */
    private static final String P_ID        = "id";
    private static final String P_MAIL      = "mail";
    private static final String P_PASSWORD  = "password";
    private static final String P_NAME      = "name";
    private static final String P_NAME_RUBY = "nameRuby";

    /**
     * ② フィールド／コンストラクタ
     */

    /**
     * コンストラクタ。
     * @param req   {@link HttpServletRequest}
     * @param useDB DB接続の要否（このサービスでは全メソッドで TM を都度生成）
     */
    public SystemAdminService(HttpServletRequest req, boolean useDB) {
        super(req, useDB);
    }

    /**
     * ③ メソッド（admin用：FrontController から呼び出される想定順）
     */

    /**
     * 一覧
     */

    /**
     * 「管理者一覧」表示。
     * - 取得データは {@code admins} として JSP へ渡す。
     * - 自分自身のアカウントは一覧から除外する。
     * - 例外時は {@code errorMsg} を設定し共通エラーへ。
     *
     * @return 一覧ビュー（/WEB-INF/jsp/systemadmin/admin/home.jsp）
     */
    public String systemAdminList() {
        try (TransactionManager tm = new TransactionManager()) {
            SystemAdminDAO dao = new SystemAdminDAO(tm.getConnection());
            UUID currentAdminId = currentSystemAdminId();
            List<SystemAdminDTO> list;
            if (currentAdminId != null) {
                /** 自分自身を除外して取得 */
                list = dao.selectAllExceptId(currentAdminId);
            } else {
                /** セッション情報がない場合は全件取得（通常は発生しない） */
                list = dao.selectAll();
            }
            /** JSP 側で安全に扱えるよう ArrayList で渡す */
            req.setAttribute(A_ADMINS, new ArrayList<>(list));
            return VIEW_HOME;
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /**
     * 新規登録（入力→確定）
     */

    /**
     * 「管理者 新規登録」入力画面表示。
     * 入力フォームを表示するのみ（DBアクセスなし）。
     *
     * @return 登録ビュー（/WEB-INF/jsp/systemadmin/admin/register.jsp）
     */
    public String systemAdminRegister() {
        return VIEW_REGISTER;
    }

    /**
     * 「管理者 新規登録」確認画面。
     * - 受取param: {@code name, nameRuby, mail, password}
     * - バリデーション: 必須（氏名/メール/パスワード）, メール形式, パスワード強度
     * - 成功: 入力値を設定し確認画面へ
     * - 失敗: エラー文言を {@code errorMsg}、入力値をそのまま属性に戻し登録画面へ
     *
     * @return 確認ビュー or 入力ビュー
     */
    public String systemAdminRegisterCheck() {
        final String mail = req.getParameter(P_MAIL);
        final String password = req.getParameter(P_PASSWORD);
        final String name = req.getParameter(P_NAME);
        final String nameRuby = req.getParameter(P_NAME_RUBY);

        // ---- 入力検証（ラベルはユーザ向け日本語で記載）----
        validation.isNull("氏名", name);
        validation.isNull("メールアドレス", mail);
        validation.isNull("パスワード", password);
        validation.isEmail(mail);
        validation.isStrongPassword(password);

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            pushBack(name, nameRuby, mail, password);
            return VIEW_REGISTER;
        }

        /** 確認画面へ入力値を引き継ぐ */
        pushBack(name, nameRuby, mail, password);
        return VIEW_REGISTER_CHECK;
    }

    /**
     * 「管理者 新規登録」確定。
     * - 受取param: {@code name, nameRuby, mail, password}
     * - バリデーション: 必須（氏名/メール/パスワード）, メール形式, メール重複チェック
     * - 成功: 登録し {@code message} 設定、登録完了ビューへ
     * - 失敗: エラー文言を {@code errorMsg}、入力値をそのまま属性に戻し登録画面へ
     *
     * @return 完了ビュー or 入力ビュー
     */
    public String systemAdminRegisterDone() {
        final String mail = req.getParameter(P_MAIL);
        final String password = req.getParameter(P_PASSWORD);
        final String name = req.getParameter(P_NAME);
        final String nameRuby = req.getParameter(P_NAME_RUBY);

        // ---- 入力検証（ラベルはユーザ向け日本語で記載）----
        validation.isNull("氏名", name);
        validation.isNull("メールアドレス", mail);
        validation.isNull("パスワード", password);
        validation.isEmail(mail);
        validation.isStrongPassword(password);

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            pushBack(name, nameRuby, mail, password);
            return VIEW_REGISTER;
        }

        try (TransactionManager tm = new TransactionManager()) {
            SystemAdminDAO dao = new SystemAdminDAO(tm.getConnection());

            // メール重複チェック
            if (dao.mailExists(mail)) {
                validation.addErrorMsg("そのメールアドレスは既に登録済みです。");
                req.setAttribute(A_ERROR, validation.getErrorMsg());
                pushBack(name, nameRuby, mail, password);
                return VIEW_REGISTER;
            }

            /** 登録（パスワードをハッシュ化） */
            SystemAdminDTO dto = new SystemAdminDTO();
            dto.setMail(mail);
            dto.setPassword(PasswordUtil.hashPassword(password));
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
     * 編集（入力→確認→確定）
     */

    /**
     * 「管理者 編集」入力画面表示。
     * - 受取param: {@code id}
     * - 該当データを {@code admin} に詰めて編集ビューへ
     * - 不正IDや例外時は共通エラーへ
     *
     * @return 編集ビュー（/WEB-INF/jsp/systemadmin/admin/edit.jsp）
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
     * 「管理者 編集」確認。
     * - 受取param: {@code id, name, nameRuby, mail, password}
     * - バリデーション: 必須（ID/氏名/メール）, メール形式, メール重複（自ID除外）
     * - 成功: 入力値を戻し用属性に格納し、確認ビューへ
     * - 失敗: エラー文言・入力値を保持し編集ビューへ
     *
     * @return 確認ビュー or 編集ビュー
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

            /** ID は hidden で戻すため、個別に詰める */
            req.setAttribute(P_ID, idStr);
            pushBack(name, nameRuby, mail, password);
            return VIEW_EDIT;
        }

        req.setAttribute(P_ID, idStr);
        pushBack(name, nameRuby, mail, password);
        return VIEW_EDIT_CHECK;
    }

    /**
     * 「管理者 編集」確定。
     * - 受取param: {@code id, name, nameRuby, mail, password(空なら据え置き)}
     * - バリデーション: 必須（ID/氏名/メール）, メール形式, メール重複（自ID除外）
     * - 成功: 更新し {@code message} を設定、編集完了ビューへ
     * - 失敗: エラー文言・入力値を保持し編集ビューへ
     *
     * @return 完了ビュー or 編集ビュー
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

            /** メール重複チェック（自ID除外） */
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

            /** 値を上書き（パスワードは空なら現状維持） */
            dto.setMail(mail);
            dto.setName(name);
            dto.setNameRuby(nameRuby);
            if (password != null && !password.isBlank()) {
                /** パスワード変更時は強度チェック */
                if (!validation.isStrongPassword(password)) {
                    req.setAttribute(A_ERROR, validation.getErrorMsg());
                    req.setAttribute(P_ID, idStr);
                    pushBack(name, nameRuby, mail, password);
                    return VIEW_EDIT;
                }
                dto.setPassword(PasswordUtil.hashPassword(password));
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
     * 論理削除
     */

    /**
     * 「管理者 論理削除」実行。
     * - 受取param: {@code id}
     * - 成功: 一覧へリダイレクト（{@code /admin/system_admin}）
     * - 失敗: 共通エラーページへ
     *
     * @return リダイレクト先 or エラーページ
     */
    public String systemAdminDelete() {
        final String idStr = req.getParameter(P_ID);
        try (TransactionManager tm = new TransactionManager()) {
            UUID id = UUID.fromString(idStr);
            SystemAdminDAO dao = new SystemAdminDAO(tm.getConnection());
            dao.delete(id);
            tm.commit();
            /** 一覧へ戻る（FrontController が redirect を解釈） */
            return req.getContextPath() + "/admin/system_admin";
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /**
     * ④ ヘルパー
     */

    /**
     * 入力値をそのまま JSP に戻すための属性詰め直し。
     * 属性キーは JSP の参照名と一致（パラメータ名と同名）。
     *
     * @param name      氏名
     * @param nameRuby  氏名（ふりがな）
     * @param mail      メールアドレス
     * @param password  パスワード（確認画面では表示しない想定）
     */
    private void pushBack(String name, String nameRuby, String mail, String password) {
        req.setAttribute(P_NAME, name);
        req.setAttribute(P_NAME_RUBY, nameRuby);
        req.setAttribute(P_MAIL, mail);
        req.setAttribute(P_PASSWORD, password);
    }

    /**
     * セッションからログイン中のシステム管理者IDを取得。
     * @return UUID（未ログイン時は null）
     */
    private UUID currentSystemAdminId() {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object user = session.getAttribute("loginUser");
        if (user instanceof LoginUser loginUser && loginUser.getSystemAdmin() != null) {
            return loginUser.getSystemAdmin().getId();
        }
        return null;
    }
}
