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
import jakarta.servlet.http.HttpSession;

import dao.AssignmentDAO;
import dao.DAOException;
import dao.ProfileDAO;
import dao.SecretaryDAO;
import dao.SecretaryMonthlySummaryDAO;
import dao.TransactionManager;
import domain.LoginUser;
import domain.Profile;
import domain.Secretary;
import domain.SecretaryMonthlySummary;
import domain.SecretaryRank;
import domain.SecretaryTotals;
import dto.AssignmentDTO;
import dto.ProfileDTO;
import dto.SecretaryDTO;
import dto.SecretaryMonthlySummaryDTO;
import dto.SecretaryRankDTO;
import util.PasswordUtil;
import dto.SecretaryTotalsDTO;

/**
 * 秘書（Secretary）に関するサービス。
 * <p>
 * 役割別（admin / secretary）に公開メソッドを配置。<br>
 * Controller からの入口であり、必要に応じ {@link TransactionManager} によりトランザクション境界を張る。
 * </p>
 *
 * <h2>クラス構成</h2>
 * <ol>
 *   <li>定数・共通化（パラメータ名／パス／フォーマッタ／コンバータ）</li>
 *   <li>フィールド、コンストラクタ</li>
 *   <li>コントローラ呼び出しメソッド（admin → secretary の順）</li>
 *   <li>プライベート・ヘルパー</li>
 * </ol>
 */
public class SecretaryService extends BaseService {

    // ==============================
    // ビュー名（散在回避のため定数化）
    // ==============================
    private static final String VIEW_HOME               = "secretary/admin/home";
    private static final String VIEW_REGISTER           = "secretary/admin/register";
    private static final String VIEW_REGISTER_CHECK     = "secretary/admin/register_check";
    private static final String VIEW_REGISTER_DONE      = "secretary/admin/register_done";
    private static final String VIEW_EDIT               = "secretary/admin/edit";
    private static final String VIEW_EDIT_CHECK         = "secretary/admin/edit_check";
    private static final String VIEW_EDIT_DONE          = "secretary/admin/edit_done";
    private static final String VIEW_MYPAGE             = "mypage/secretary/home";
    private static final String VIEW_MYPAGE_EDIT        = "mypage/secretary/edit";
    private static final String VIEW_MYPAGE_EDIT_CHECK  = "mypage/secretary/edit_check";

    // ==============================
    // リクエスト・パラメータ名
    // ==============================
    private static final String P_ID             = "id";
    private static final String P_SECRETARY_CODE = "secretaryCode";
    private static final String P_NAME           = "name";
    private static final String P_NAME_RUBY      = "nameRuby";
    private static final String P_MAIL           = "mail";
    private static final String P_PASSWORD       = "password";
    private static final String P_PHONE          = "phone";
    private static final String P_POSTAL_CODE    = "postalCode";
    private static final String P_ADDRESS1       = "address1";
    private static final String P_ADDRESS2       = "address2";
    private static final String P_BUILDING       = "building";
    private static final String P_PM_SECRETARY   = "pmSecretary";
    private static final String P_SECRETARY_RANK = "secretaryRankId";
    // 口座系
    private static final String P_BANK_NAME      = "bankName";
    private static final String P_BANK_BRANCH    = "bankBranch";
    private static final String P_BANK_TYPE      = "bankType";
    private static final String P_BANK_ACCOUNT   = "bankAccount";
    private static final String P_BANK_OWNER     = "bankOwner";

    // ==============================
    // 属性名（JSPへ渡すキー）
    // ==============================
    private static final String A_SECRETARIES = "secretaries";
    private static final String A_RANKS       = "ranks";
    private static final String A_SECRETARY   = "secretary";
    private static final String A_MESSAGE     = "message";
    private static final String A_ERROR_MSG   = "errorMsg"; // 既存JSP互換名

    /** 使い回し用コンバータ（毎回 new しない） */
    private final Converter conv = new Converter();

    /**
     * コンストラクタ。
     * @param req   HTTPリクエスト
     * @param useDB DB接続を要する処理か（BaseServiceの診断用途）
     */
    public SecretaryService(HttpServletRequest req, boolean useDB) {
        super(req, useDB);
    }

    // =========================================================
    // Admin（管理者）向け機能
    // =========================================================

    /**
     * 「【admin】機能：秘書一覧」
     *
     * <p>秘書の全件を取得して一覧画面へ。</p>
     *
     * <ul>
     *   <li>JSP属性:
     *      <ul>
     *        <li>{@code secretaries}: List&lt;Secretary&gt;</li>
     *      </ul>
     *   </li>
     *   <li>エラー時: {@code errorMsg} を設定し /error へ</li>
     * </ul>
     *
     * @return 遷移先JSPパス
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

    /**
     * 「【admin】機能：秘書新規登録（入力画面）」
     *
     * <p>秘書ランク一覧をロードし、入力画面を表示。</p>
     * <ul>
     *   <li>セッション属性: {@code ranks} に List&lt;SecretaryRank&gt;</li>
     *   <li>入力値の再描画サポート: {@link #pushFormBackToRequest()}</li>
     * </ul>
     */
    public String secretaryRegister() {
        try (TransactionManager tm = new TransactionManager()) {
            SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
            List<SecretaryRankDTO> dtos = dao.selectRankAll();

            List<SecretaryRank> ranks = new ArrayList<>(dtos.size());
            for (SecretaryRankDTO dto : dtos) ranks.add(conv.toDomain(dto));

            HttpSession session = req.getSession(true);
            session.setAttribute(A_RANKS, ranks);
            pushFormBackToRequest();
            return VIEW_REGISTER;
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /**
     * 「【admin】機能：秘書新規登録（確認画面）」
     *
     * <p>入力検証のみ。DBアクセスなし。</p>
     * <ul>
     *   <li>必須: {@code name}, {@code mail}, {@code password}, {@code secretaryRankId}</li>
     *   <li>任意形式チェック: 郵便番号、電話番号、メール形式</li>
     *   <li>エラー時: {@code errorMsg} を設定し入力画面へ戻す</li>
     * </ul>
     */
    public String secretaryRegisterCheck() {
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
     * 「【admin】機能：秘書新規登録（確定）」
     *
     * <p>重複（メール／秘書コード）をチェックし、問題なければ INSERT。</p>
     * <ul>
     *   <li>エラー時: {@code errorMsg} を設定し入力画面へ戻る</li>
     *   <li>成功時: 完了画面へ</li>
     * </ul>
     */
    public String secretaryRegisterDone() {
        // 再検証（確認画面スキップ対策）
        validation.isNull("名前", param(P_NAME));
        validation.isNull("メールアドレス", param(P_MAIL));
        validation.isNull("パスワード", param(P_PASSWORD));
        if (notBlank(param(P_POSTAL_CODE))) validation.isPostalCode(param(P_POSTAL_CODE));
        if (notBlank(param(P_PHONE)))       validation.isPhoneNumber(param(P_PHONE));
        if (notBlank(param(P_MAIL)))        validation.isEmail(param(P_MAIL));
        if (!notBlank(param(P_SECRETARY_RANK))) {
            validation.addErrorMsg("秘書ランクを選択してください。");
        }
        validation.isStrongPassword(param(P_PASSWORD));
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
        dto.setPassword(PasswordUtil.hashPassword(param(P_PASSWORD)));
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

    /**
     * 「【admin】機能：秘書詳細」
     *
     * <p>基本情報、当月アサイン、遡りアサイン、12ヶ月サマリー等を表示。</p>
     * <ul>
     *   <li>必須 param: {@code id}（UUID）</li>
     *   <li>JSP属性:
     *     <ul>
     *       <li>{@code secretary}, {@code profile}, {@code yearMonth}</li>
     *       <li>{@code assignThisMonth}, {@code assignUptoMonth}</li>
     *       <li>{@code totals}, {@code last12}</li>
     *     </ul>
     *   </li>
     * </ul>
     */
    public String secretaryDetail() {
        final String idStr = req.getParameter(P_ID);
        if (!validation.isUuid(idStr)) {
            validation.addErrorMsg("不正なIDが指定されました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        final ZoneId Z = ZoneId.of("Asia/Tokyo");
        final LocalDate today = LocalDate.now(Z);
        final String ym = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        final String ymFrom12 = today.minusMonths(11).format(DateTimeFormatter.ofPattern("yyyy-MM"));

        try (TransactionManager tm = new TransactionManager()) {
            UUID secId = UUID.fromString(idStr);

            // ① 秘書情報
            SecretaryDAO sdao = new SecretaryDAO(tm.getConnection());
            SecretaryDTO sDto = sdao.selectByUUId(secId);
            Secretary secretary = conv.toDomain(sDto);
            req.setAttribute(A_SECRETARY, secretary);
            req.setAttribute("yearMonth", ym);

            // ①-2 プロフィール
            ProfileDAO pdao = new ProfileDAO(tm.getConnection());
            ProfileDTO pDto = pdao.selectBySecretaryId(secId); // null 可
            Profile profile = conv.toDomain(pDto);
            req.setAttribute("profile", profile);

            // ② 今月アサイン（継続月数付き）
            AssignmentDAO adao = new AssignmentDAO(tm.getConnection());
            Map<UUID, Integer> contMap = new HashMap<>();
            List<AssignmentDTO> assignThisMonth =
                    adao.selectAssignmentsForMonthWithCont(ym, secId, null, null, false, contMap);
            System.out.println(assignThisMonth.size());
            req.setAttribute("assignThisMonth", assignThisMonth);

            // ④ 過去アサイン（最大24ヶ月遡り）
            List<AssignmentDTO> uptoList = new ArrayList<>();
            LocalDate cursor = today;
            for (int i = 0; i < 24; i++) {
                String ymLoop = cursor.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                List<AssignmentDTO> rows = adao.selectAssignmentsForMonthWithCont(ymLoop, secId, null, null, false, null);
                if (rows != null && !rows.isEmpty()) {
                    uptoList.addAll(rows); // 最新→過去の降順で追加
                }
                cursor = cursor.minusMonths(1);
                if (cursor.isBefore(today.minusYears(5))) break; // 安全停止
            }
            req.setAttribute("assignUptoMonth", uptoList);

            // ③ + ⑤ 12ヶ月集計
            SecretaryMonthlySummaryDAO smdao = new SecretaryMonthlySummaryDAO(tm.getConnection());

            SecretaryTotalsDTO totalsDto = smdao.selectTotals(secId);
            SecretaryTotals totals = conv.toDomain(totalsDto);
            req.setAttribute("totals", totals);

            List<SecretaryMonthlySummaryDTO> last12Dto = smdao.selectLast12Months(secId, ymFrom12, ym);
            List<SecretaryMonthlySummary> last12 = new ArrayList<>();
            for (SecretaryMonthlySummaryDTO d : last12Dto) last12.add(conv.toDomain(d));
            req.setAttribute("last12", last12);

            return "secretary/admin/detail";
        } catch (RuntimeException e) {
            e.printStackTrace();
            validation.addErrorMsg("データ取得に失敗しました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /**
     * 「【admin】機能：秘書編集（入力画面）」
     *
     * @return 編集画面 JSP
     */
    public String secretaryEdit() {
        final String uuidStr = param(P_ID);
        if (!validation.isUuid(uuidStr)) {
            validation.addErrorMsg("不正なIDが指定されました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        try (TransactionManager tm = new TransactionManager()) {
            UUID id = UUID.fromString(uuidStr);
            SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
            SecretaryDTO dto = dao.selectByUUIdWithBank(id);
            Secretary secretary = conv.toDomain(dto);

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
     * 「【admin】機能：秘書編集（確認画面）」
     *
     * <p>自ID除外の重複（メール／秘書コード）をチェック。</p>
     */
    public String secretaryEditCheck() {
        final String idStr           = param(P_ID);
        final String secretaryCode   = param(P_SECRETARY_CODE);
        final String name            = param(P_NAME);
        final String mail            = param(P_MAIL);
        final String phone           = param(P_PHONE);
        final String postalCode      = param(P_POSTAL_CODE);

        // 必須・形式
        validation.isNull("名前", name);
        validation.isNull("メールアドレス", mail);
        if (notBlank(postalCode)) validation.isPostalCode(postalCode);
        if (notBlank(phone))      validation.isPhoneNumber(phone);
        if (notBlank(mail))       validation.isEmail(mail);

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
            req.setAttribute(P_ID, idStr);
            pushFormBackToRequest();
            return VIEW_EDIT;
        }

        req.setAttribute(P_ID, idStr);
        pushFormBackToRequest();
        return VIEW_EDIT_CHECK;
    }

    /**
     * 「【admin】機能：秘書編集（確定）」
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

            // 念のため再チェック
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

    /**
     * 「【admin】機能：秘書論理削除」
     *
     * <p>param {@code id}（UUID）を検証の上、論理削除。</p>
     * <ul>
     *   <li>成功時: 秘書一覧へリダイレクト</li>
     *   <li>エラー時: {@code errorMsg} を設定し /error へ</li>
     * </ul>
     */
    public String secretaryDelete() {
        final String idStr = req.getParameter(P_ID);
        if (!validation.isUuid(idStr)) {
            validation.addErrorMsg("不正なIDが指定されました。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
        try (TransactionManager tm = new TransactionManager()) {
            UUID id = UUID.fromString(idStr);
            SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
            dao.delete(id);
            tm.commit();
            return req.getContextPath() + "/admin/secretary";
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    // =========================================================
    // Secretary（秘書本人）向け機能（マイページ）
    // =========================================================

    /**
     * 「【secretary】機能：マイページ（表示）」
     *
     * <p>セッションのログイン秘書IDから自身の情報（口座含む）を取得して表示。</p>
     */
    public String myPageList() {
        UUID myId = currentSecretaryId();
        if (myId == null) {
            validation.addErrorMsg("ログイン情報が見つかりません。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
        try (TransactionManager tm = new TransactionManager()) {
            SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
            SecretaryDTO dto = dao.selectByUUIdWithBank(myId);
            if (dto == null) {
                validation.addErrorMsg("アカウント情報が取得できませんでした。");
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                return req.getContextPath() + req.getServletPath() + "/error";
            }
            Secretary secretary = conv.toDomain(dto);
            req.setAttribute(A_SECRETARY, secretary);
            return VIEW_MYPAGE;
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /**
     * 「【secretary】機能：マイページ編集（入力画面）」
     */
    public String myPageEdit() {
        UUID myId = currentSecretaryId();
        if (myId == null) {
            validation.addErrorMsg("ログイン情報が見つかりません。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
        try (TransactionManager tm = new TransactionManager()) {
            SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
            SecretaryDTO dto = dao.selectByUUIdWithBank(myId);
            if (dto == null) {
                validation.addErrorMsg("アカウント情報が取得できませんでした。");
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                return req.getContextPath() + req.getServletPath() + "/error";
            }
            Secretary secretary = conv.toDomain(dto);
            req.setAttribute(A_SECRETARY, secretary);
            pushMyPageFormBackToRequest();
            return VIEW_MYPAGE_EDIT;
        } catch (RuntimeException e) {
            validation.addErrorMsg("データベースに不正な操作が行われました");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

    /**
     * 「【secretary】機能：マイページ編集（確認画面）」
     *
     * <p>本人更新可能項目のバリデーションおよびメール重複（自ID除外）を実施。</p>
     */
    public String myPageEditCheck() {
        UUID myId = currentSecretaryId();
        if (myId == null) {
            validation.addErrorMsg("ログイン情報が見つかりません。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        final String password   = req.getParameter(P_PASSWORD);
        final String name       = req.getParameter(P_NAME);
        final String mail       = req.getParameter(P_MAIL);
        final String phone      = req.getParameter(P_PHONE);
        final String postalCode = req.getParameter(P_POSTAL_CODE);

        // 必須＆形式
        validation.isNull("氏名", name);
        validation.isNull("メールアドレス", mail);
        if (notBlank(mail))       validation.isEmail(mail);
        if (notBlank(postalCode)) validation.isPostalCode(postalCode);
        if (notBlank(phone))      validation.isPhoneNumber(phone);
        if (notBlank(password) && password.length() < 8) {
            validation.addErrorMsg("パスワードは8文字以上で入力してください。");
        }

        // メール重複（自分以外）
        if (!validation.hasErrorMsg()) {
            try (TransactionManager tm = new TransactionManager()) {
                SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
                SecretaryDTO dto = dao.selectByUUId(myId);
                if (dto == null) {
                    validation.addErrorMsg("アカウント情報が取得できませんでした。");
                } else if (notBlank(mail) && dao.mailCheckExceptId(mail, dto.getId())) {
                    validation.addErrorMsg("登録いただいたメールアドレスはすでに使われています。");
                }
            }
        }

        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            pushMyPageFormBackToRequest();
            return VIEW_MYPAGE_EDIT;
        }

        pushMyPageFormBackToRequest();
        return VIEW_MYPAGE_EDIT_CHECK;
    }

    /**
     * 「【secretary】機能：マイページ編集（確定）」
     *
     * <p>本人が編集可能な範囲のみ上書き。非編集の項目は現行値を維持。</p>
     * <p>更新後、セッション内の {@code loginUser} も更新。</p>
     */
    public String myPageEditDone() {
        UUID myId = currentSecretaryId();
        if (myId == null) {
            validation.addErrorMsg("ログイン情報が見つかりません。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            return req.getContextPath() + req.getServletPath() + "/error";
        }

        final String password   = req.getParameter(P_PASSWORD);
        final String name       = req.getParameter(P_NAME);
        final String nameRuby   = req.getParameter(P_NAME_RUBY);
        final String mail       = req.getParameter(P_MAIL);
        final String phone      = req.getParameter(P_PHONE);
        final String postalCode = req.getParameter(P_POSTAL_CODE);
        final String address1   = req.getParameter(P_ADDRESS1);
        final String address2   = req.getParameter(P_ADDRESS2);
        final String building   = req.getParameter(P_BUILDING);
        final String bankName    = req.getParameter(P_BANK_NAME);
        final String bankBranch  = req.getParameter(P_BANK_BRANCH);
        final String bankType    = req.getParameter(P_BANK_TYPE);
        final String bankAccount = req.getParameter(P_BANK_ACCOUNT);
        final String bankOwner   = req.getParameter(P_BANK_OWNER);

        // 入力検証
        validation.isNull("氏名", name);
        validation.isNull("メールアドレス", mail);
        if (notBlank(mail))       validation.isEmail(mail);
        if (notBlank(postalCode)) validation.isPostalCode(postalCode);
        if (notBlank(phone))      validation.isPhoneNumber(phone);
        if (notBlank(password) && password.length() < 8) {
            validation.addErrorMsg("パスワードは8文字以上で入力してください。");
        }
        if (validation.hasErrorMsg()) {
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            pushMyPageFormBackToRequest();
            return VIEW_MYPAGE_EDIT;
        }

        try (TransactionManager tm = new TransactionManager()) {
            SecretaryDAO dao = new SecretaryDAO(tm.getConnection());

            // 現行データ
            SecretaryDTO cur = dao.selectByUUId(myId);
            if (cur == null || cur.getId() == null) { // ← 空DTO対策
            	System.out.println("アカウント情報が取得できませんでした。");
                validation.addErrorMsg("アカウント情報が取得できませんでした。");
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                return req.getContextPath() + req.getServletPath() + "/error";
            }

            // 自ID除外のメール重複
            // メール重複（論理削除含め全件を対象）
            if (notBlank(mail) && dao.mailCheckExceptId(mail, cur.getId())) {
            	System.out.println("登録いただいたメールアドレスはすでに使われています。");
            	validation.addErrorMsg("登録いただいたメールアドレスはすでに使われています。");
                req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                pushMyPageFormBackToRequest();
                return VIEW_MYPAGE_EDIT;
            }

            // DTO 組み立て（非編集項目は現行値）
            SecretaryDTO dto = new SecretaryDTO();
            dto.setId(cur.getId());
            dto.setSecretaryCode(cur.getSecretaryCode());     // 非編集
            dto.setPmSecretary(cur.isPmSecretary());          // 非編集
            dto.setSecretaryRankId(cur.getSecretaryRankId()); // 非編集
            dto.setName(name);
            dto.setNameRuby(nameRuby);
            dto.setMail(mail);
            dto.setPhone(phone);
            dto.setPostalCode(postalCode);
            dto.setAddress1(address1);
            dto.setAddress2(address2);
            dto.setBuilding(building);
            dto.setPassword(notBlank(password) ? password : cur.getPassword());
            dto.setBankName(bankName);
            dto.setBankBranch(bankBranch);
            dto.setBankType(bankType);
            dto.setBankAccount(bankAccount);
            dto.setBankOwner(bankOwner);
            dao.updateWithBank(dto);
            tm.commit();
            try {
                dao.updateWithBank(dto);
            } catch (DAOException ex) {
                // 23505 = unique_violation (PostgreSQL)
                Throwable cause = ex.getCause();
                if (cause instanceof java.sql.SQLException sqlEx && "23505".equals(sqlEx.getSQLState())) {
                    System.out.println("登録いただいたメールアドレスはすでに使われています。");
                    validation.addErrorMsg("登録いただいたメールアドレスはすでに使われています。");
                    req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
                    pushMyPageFormBackToRequest();
                    return VIEW_MYPAGE_EDIT;
                }
                throw ex; 
            }

            tm.commit();

            HttpSession session = req.getSession(false);
            if (session != null) {
                Object u = session.getAttribute("loginUser");
                if (u instanceof LoginUser lu) {
                    SecretaryDTO refreshed = dao.selectByUUId(myId);
                    lu.setSecretary(conv.toDomain(refreshed));
                    session.setAttribute("loginUser", lu);
                }
            }

            // 最新表示のためマイページへ
            return req.getContextPath() + "/secretary/mypage/home";

        } catch (RuntimeException e) {
            // ここに来るのは主に DB/DAO のその他例外
            validation.addErrorMsg("処理中にエラーが発生しました。再度お試しください。");
            req.setAttribute(A_ERROR_MSG, validation.getErrorMsg());
            System.out.println("処理中にエラーが発生しました。再度お試しください。");
            return req.getContextPath() + req.getServletPath() + "/error";
        }
    }

	// =========================================================
    // Private helpers
    // =========================================================

    /** request param のショートカット */
    private String param(String name) { return req.getParameter(name); }

    /** 文字列が null／空白でないか */
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    /**
     * 入力値を request に戻す（新規／編集：admin 用）
     * <p>画面再描画で入力値を保持する。</p>
     */
    private void pushFormBackToRequest() {
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

    /**
     * 入力値を request に戻す（マイページ編集：secretary 用）
     */
    private void pushMyPageFormBackToRequest() {
        req.setAttribute(P_PASSWORD,     req.getParameter(P_PASSWORD));
        req.setAttribute(P_NAME,         req.getParameter(P_NAME));
        req.setAttribute(P_NAME_RUBY,    req.getParameter(P_NAME_RUBY));
        req.setAttribute(P_MAIL,         req.getParameter(P_MAIL));
        req.setAttribute(P_PHONE,        req.getParameter(P_PHONE));
        req.setAttribute(P_POSTAL_CODE,  req.getParameter(P_POSTAL_CODE));
        req.setAttribute(P_ADDRESS1,     req.getParameter(P_ADDRESS1));
        req.setAttribute(P_ADDRESS2,     req.getParameter(P_ADDRESS2));
        req.setAttribute(P_BUILDING,     req.getParameter(P_BUILDING));
        // 口座系
        req.setAttribute(P_BANK_NAME,    req.getParameter(P_BANK_NAME));
        req.setAttribute(P_BANK_BRANCH,  req.getParameter(P_BANK_BRANCH));
        req.setAttribute(P_BANK_TYPE,    req.getParameter(P_BANK_TYPE));
        req.setAttribute(P_BANK_ACCOUNT, req.getParameter(P_BANK_ACCOUNT));
        req.setAttribute(P_BANK_OWNER,   req.getParameter(P_BANK_OWNER));
    }

    /**
     * セッションからログイン中の秘書IDを取得。
     * @return UUID（未ログイン時は null）
     */
    private UUID currentSecretaryId() {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object user = session.getAttribute("loginUser");
        if (user instanceof LoginUser loginUser && loginUser.getSecretary() != null) {
            return loginUser.getSecretary().getId();
        }
        return null;
    }
}
