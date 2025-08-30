package service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import dao.SecretaryDAO;
import dao.TransactionManager;
import domain.Secretary;
import domain.SecretaryRank;
import dto.SecretaryDTO;
import dto.SecretaryRankDTO;


/**
 * 秘書（Secretary）に関するサービス。
 * <p>各メソッドは必要に応じ {@link TransactionManager} でトランザクション境界を張ります。</p>
 */
public class SecretaryService extends BaseService{
	
	// ==============================
    // ビュー名（散在回避のため定数化）
    // ==============================
	private static final String VIEW_HOME            = "secretary/admin/home";
    private static final String VIEW_REGISTER        = "secretary/admin/register";
    private static final String VIEW_REGISTER_CHECK  = "secretary/admin/register_check";
    private static final String VIEW_REGISTER_DONE   = "secretary/admin/register_done";
    private static final String VIEW_EDIT            = "secretary/admin/edit";
    private static final String VIEW_EDIT_CHECK      = "secretary/admin/edit_check";
    private static final String VIEW_EDIT_DONE       = "secretary/admin/edit_done";

    // ==============================
    // リクエスト・パラメータ名
    // ==============================
    private static final String P_ID              = "id";
    private static final String P_SECRETARY_CODE  = "secretaryCode";
    private static final String P_NAME            = "name";
    private static final String P_NAME_RUBY       = "nameRuby";
    private static final String P_MAIL            = "mail";
    private static final String P_PASSWORD        = "password";
    private static final String P_PHONE           = "phone";
    private static final String P_POSTAL_CODE     = "postalCode";
    private static final String P_ADDRESS1        = "address1";
    private static final String P_ADDRESS2        = "address2";
    private static final String P_BUILDING        = "building";
    private static final String P_PM_SECRETARY    = "pmSecretary";
    private static final String P_SECRETARY_RANK  = "secretaryRankId";

    // ==============================
    // 属性名
    // ==============================
    private static final String A_SECRETARIES = "secretaries";
    private static final String A_RANKS       = "ranks";
    private static final String A_SECRETARY   = "secretary";
    private static final String A_MESSAGE     = "message";
    private static final String A_ERROR_MSG   = "errorMsg";

    /** 使い回し用コンバータ（毎回 new しない） */
    private final Converter conv = new Converter();
	
	public SecretaryService(HttpServletRequest req, boolean useDB) {
		super(req, useDB);
	}
	
	
	// =========================================================
    // 一覧
    // =========================================================
    /**
     * 秘書一覧を取得して表示。
     */
    public String secretaryList() {
        try (TransactionManager tm = new TransactionManager()) {
            SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
            List<SecretaryDTO> dtos = dao.selectAll();

            List<Secretary> secretaries = new ArrayList<>(dtos.size());
            for (SecretaryDTO dto : dtos) {
                secretaries.add(conv.toDomain(dto));
            }

            req.setAttribute(A_SECRETARIES, secretaries);
            return VIEW_HOME;
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }
	
    
    // =========================================================
    // 新規登録（画面）
    // =========================================================
    
    /**
     * 新規登録画面を表示（ランク一覧をロード）。
     */
    public String secretaryRegister() {
        try (TransactionManager tm = new TransactionManager()) {
            SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
            List<SecretaryRankDTO> dtos = dao.selectRankAll();

            List<SecretaryRank> ranks = new ArrayList<>(dtos.size());
            for (SecretaryRankDTO dto : dtos) {
                ranks.add(conv.toDomain(dto));
            }

            // 既存テンプレートに合わせてセッションに格納
            HttpSession session = req.getSession(true);
            session.setAttribute(A_RANKS, ranks);
            return VIEW_REGISTER;

        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }
    
    
    /**
     * 入力内容の確認画面。
     * <p>DBアクセス不要のためトランザクションは張りません。</p>
     */
    public String secretaryRegisterCheck() {
        // 必須 & 形式
        validation.isNull("名前", param(P_NAME));
        validation.isNull("メールアドレス", param(P_MAIL));
        validation.isNull("パスワード", param(P_PASSWORD));
        if (notBlank(param(P_POSTAL_CODE))) validation.isPostalCode(param(P_POSTAL_CODE));
        if (notBlank(param(P_PHONE)))       validation.isPhoneNumber(param(P_PHONE));
        if (notBlank(param(P_MAIL)))        validation.isEmail(param(P_MAIL));
        if (!notBlank(param(P_SECRETARY_RANK))) {
            validation.addErrorMsg("秘書ランクを選択してください。");
        }

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            pushFormBackToRequest();
            return VIEW_REGISTER;
        }

        pushFormBackToRequest();
        return VIEW_REGISTER_CHECK;
    }
	
	
    /**
     * 新規登録の確定。
     */
    public String secretaryRegisterDone() {
        // 入力検証（確認画面をスキップされた場合に備えて再検証）
        validation.isNull("名前", param(P_NAME));
        validation.isNull("メールアドレス", param(P_MAIL));
        validation.isNull("パスワード", param(P_PASSWORD));
        if (notBlank(param(P_POSTAL_CODE))) validation.isPostalCode(param(P_POSTAL_CODE));
        if (notBlank(param(P_PHONE)))       validation.isPhoneNumber(param(P_PHONE));
        if (notBlank(param(P_MAIL)))        validation.isEmail(param(P_MAIL));
        if (!notBlank(param(P_SECRETARY_RANK))) {
            validation.addErrorMsg("秘書ランクを選択してください。");
        }
        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            pushFormBackToRequest();
            return VIEW_REGISTER;
        }

        final boolean pmSecretary = Boolean.parseBoolean(param(P_PM_SECRETARY));
        final UUID secretaryRankId = UUID.fromString(param(P_SECRETARY_RANK));

        SecretaryDTO dto = new SecretaryDTO();
        dto.setSecretaryCode(param(P_SECRETARY_CODE));
        dto.setName(param(P_NAME));
        dto.setNameRuby(param(P_NAME_RUBY));
        dto.setMail(param(P_MAIL));
        dto.setPassword(param(P_PASSWORD));
        dto.setPhone(param(P_PHONE));
        dto.setPostalCode(param(P_POSTAL_CODE));
        dto.setAddress1(param(P_ADDRESS1));
        dto.setAddress2(param(P_ADDRESS2));
        dto.setBuilding(param(P_BUILDING));
        dto.setPmSecretary(pmSecretary);
        dto.setSecretaryRankId(secretaryRankId);

        try (TransactionManager tm = new TransactionManager()) {
            SecretaryDAO dao = new SecretaryDAO(tm.getConnection());

            // 重複チェック
            if (dao.mailCheck(dto.getMail())) {
                validation.addErrorMsg("登録いただいたメールアドレスはすでに使われています。");
            }
            if (notBlank(dto.getSecretaryCode()) && dao.secretaryCodeCheck(dto.getSecretaryCode())) {
                validation.addErrorMsg("登録いただいた秘書コードはすでに使われています。");
            }
            if (validation.hasErrorMsg()) {
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                pushFormBackToRequest();
                return VIEW_REGISTER;
            }

            int num = dao.insert(dto);
            tm.commit();
            req.setAttribute(A_MESSAGE, "登録が完了しました（件数:" + num + "）");
            return VIEW_REGISTER_DONE;
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }
    
    
    // =========================================================
    // 編集（画面）
    // =========================================================
    /**
     * 編集画面を表示。
     */
    public String secretaryEdit() {
        final String uuidStr = param(P_ID);
        // UUID 形式の検証
        if (!validation.isUuid(uuidStr)) {
            validation.addErrorMsg("不正なIDが指定されました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        try (TransactionManager tm = new TransactionManager()) {
            UUID id = UUID.fromString(uuidStr);
            SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
            SecretaryDTO dto = dao.selectByUUId(id);
            Secretary secretary = conv.toDomain(dto);

            // ランク一覧
            List<SecretaryRankDTO> rankDtos = dao.selectRankAll();
            List<SecretaryRank> ranks = new ArrayList<>(rankDtos.size());
            for (SecretaryRankDTO r : rankDtos) ranks.add(conv.toDomain(r));

            req.setAttribute(A_SECRETARY, secretary);
            req.setAttribute(A_RANKS, ranks);
            return VIEW_EDIT;
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }
	
    
    /**
     * 編集の確認画面。
     * <p>ここで自ID除外の重複チェックも行い、弾ければ編集画面に戻します。</p>
     */
    public String secretaryEditCheck() {
        final String idStr           = param(P_ID);
        final String secretaryCode   = param(P_SECRETARY_CODE);
        final String name            = param(P_NAME);
        final String nameRuby        = param(P_NAME_RUBY);
        final String mail            = param(P_MAIL);
        final String phone           = param(P_PHONE);
        final String postalCode      = param(P_POSTAL_CODE);
        final String address1        = param(P_ADDRESS1);
        final String address2        = param(P_ADDRESS2);
        final String building        = param(P_BUILDING);
        final String pmSecretaryStr  = param(P_PM_SECRETARY);
        final String secretaryRankId = param(P_SECRETARY_RANK);

        // 入力検証
        validation.isNull("ID", idStr);
        validation.isNull("名前", name);
        validation.isNull("メールアドレス", mail);
        if (notBlank(postalCode)) validation.isPostalCode(postalCode);
        if (notBlank(phone))      validation.isPhoneNumber(phone);
        if (notBlank(mail))       validation.isEmail(mail);
        if (!notBlank(secretaryRankId)) validation.addErrorMsg("秘書ランクを選択してください。");

        // 重複チェック（自ID除外）
        if (!validation.hasErrorMsg() && validation.isUuid(idStr)) {
            try (TransactionManager tm = new TransactionManager()) {
                SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
                UUID id = UUID.fromString(idStr);

                if (notBlank(mail) && dao.mailCheckExceptId(mail, id)) {
                    validation.addErrorMsg("登録いただいたメールアドレスはすでに使われています。");
                }
                if (notBlank(secretaryCode) && dao.secretaryCodeCheckExceptId(secretaryCode, id)) {
                    validation.addErrorMsg("登録いただいた秘書コードはすでに使われています。");
                }
            }
        }

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            // 編集画面に戻すため、IDと入力値を戻す
            req.setAttribute(P_ID, idStr);
            pushFormBackToRequest();
            return VIEW_EDIT;
        }

        // 確認画面へ（hidden で引き継ぐ）
        req.setAttribute(P_ID, idStr);
        pushFormBackToRequest();
        return VIEW_EDIT_CHECK;
    }

    
    /**
     * 編集確定処理。
     */
    public String secretaryEditDone() {
        final String idStr           = param(P_ID);
        final String secretaryCode   = param(P_SECRETARY_CODE);
        final String name            = param(P_NAME);
        final String nameRuby        = param(P_NAME_RUBY);
        final String mail            = param(P_MAIL);
        final String phone           = param(P_PHONE);
        final String postalCode      = param(P_POSTAL_CODE);
        final String address1        = param(P_ADDRESS1);
        final String address2        = param(P_ADDRESS2);
        final String building        = param(P_BUILDING);
        final String pmSecretaryStr  = param(P_PM_SECRETARY);
        final String secretaryRankId = param(P_SECRETARY_RANK);

        // 入力検証
        validation.isNull("ID", idStr);
        validation.isNull("名前", name);
        validation.isNull("メールアドレス", mail);
        if (notBlank(postalCode)) validation.isPostalCode(postalCode);
        if (notBlank(phone))      validation.isPhoneNumber(phone);
        if (notBlank(mail))       validation.isEmail(mail);
        if (!notBlank(secretaryRankId)) validation.addErrorMsg("秘書ランクを選択してください。");

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            req.setAttribute(P_ID, idStr);
            pushFormBackToRequest();
            return VIEW_EDIT;
        }

        try (TransactionManager tm = new TransactionManager()) {
            SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
            UUID id = UUID.fromString(idStr);

            // 二重防御：自ID除外で再チェック
            if (notBlank(mail) && dao.mailCheckExceptId(mail, id)) {
                validation.addErrorMsg("登録いただいたメールアドレスはすでに使われています。");
            }
            if (notBlank(secretaryCode) && dao.secretaryCodeCheckExceptId(secretaryCode, id)) {
                validation.addErrorMsg("登録いただいた秘書コードはすでに使われています。");
            }
            if (validation.hasErrorMsg()) {
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                req.setAttribute(P_ID, idStr);
                pushFormBackToRequest();
                return VIEW_EDIT;
            }

            SecretaryDTO dto = new SecretaryDTO();
            dto.setId(id);
            dto.setSecretaryCode(secretaryCode);
            dto.setName(name);
            dto.setNameRuby(nameRuby);
            dto.setMail(mail);
            dto.setPhone(phone);
            dto.setPostalCode(postalCode);
            dto.setAddress1(address1);
            dto.setAddress2(address2);
            dto.setBuilding(building);
            dto.setPmSecretary(Boolean.parseBoolean(pmSecretaryStr));
            dto.setSecretaryRankId(UUID.fromString(secretaryRankId));

            int num = dao.update(dto);
            tm.commit();

            req.setAttribute(A_MESSAGE, "更新が完了しました（件数:" + num + "）");
            return VIEW_EDIT_DONE;
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }
	
    
    
    // =========================================================
    // 削除
    // =========================================================
    
    /** 論理削除 */
	public String secretaryDelete(){
		String idStr = req.getParameter("id");
		UUID id = UUID.fromString(idStr);
		try(TransactionManager tm = new TransactionManager()) {
			SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
			dao.delete(id);
			tm.commit();
			return req.getContextPath() + "/admin/secretary";
		} catch(RuntimeException e) {
	    	validation.addErrorMsg("データベースに不正な操作が行われました");
	    	req.setAttribute("errorMsg", validation.getErrorMsg());
	    	return req.getContextPath() + req.getServletPath() + "/error";
	    }
	}
	
	
	// =========================================================
    // Private helpers
    // =========================================================
	
	private String param(String name) { return req.getParameter(name); }
	
	private boolean notBlank(String s) { return s != null && !s.isBlank(); }
	
    /**
     * 入力値を request に戻す（エラー時の再描画用）。
     */
    private void pushFormBackToRequest() { // ★ CHANGED: 共通化
        req.setAttribute(P_SECRETARY_CODE,  req.getParameter(P_SECRETARY_CODE));
        req.setAttribute(P_NAME,            req.getParameter(P_NAME));
        req.setAttribute(P_NAME_RUBY,       req.getParameter(P_NAME_RUBY));
        req.setAttribute(P_MAIL,            req.getParameter(P_MAIL));
        req.setAttribute(P_PASSWORD,        req.getParameter(P_PASSWORD)); // register_check 用
        req.setAttribute(P_PHONE,           req.getParameter(P_PHONE));
        req.setAttribute(P_POSTAL_CODE,     req.getParameter(P_POSTAL_CODE));
        req.setAttribute(P_ADDRESS1,        req.getParameter(P_ADDRESS1));
        req.setAttribute(P_ADDRESS2,        req.getParameter(P_ADDRESS2));
        req.setAttribute(P_BUILDING,        req.getParameter(P_BUILDING));
        req.setAttribute(P_PM_SECRETARY,    req.getParameter(P_PM_SECRETARY));
        req.setAttribute(P_SECRETARY_RANK,  req.getParameter(P_SECRETARY_RANK));
    }
	
}
