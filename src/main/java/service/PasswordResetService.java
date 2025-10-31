package service;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import config.MailConfig;
import dao.CustomerContactDAO;
import dao.PasswordResetTokenDAO;
import dao.SecretaryDAO;
import dao.SystemAdminDAO;
import dao.TransactionManager;
import dto.CustomerContactDTO;
import dto.PasswordResetTokenDTO;
import dto.SecretaryDTO;
import dto.SystemAdminDTO;
import util.PasswordUtil;

/**
 * パスワードリセット機能のサービスクラス。
 *
 * 全ロール（admin / secretary / customer）向けのパスワードリセット機能を提供します。
 * メールアドレス入力、トークン生成・メール送信、新パスワード設定の各処理を実行します。
 *
 * 画面フロー:
 * - リセット申請画面表示 → {@link #showResetRequestForm(String)}
 * - リセット申請処理 → {@link #processResetRequest(String)}
 * - パスワード再設定画面表示 → {@link #showResetForm(String)}
 * - パスワード再設定処理 → {@link #processPasswordReset(String)}
 */
public class PasswordResetService extends BaseService {

    /**
     * ① 定数
     */

    /**
     * Request parameter keys
     */
    private static final String P_EMAIL            = "email";
    private static final String P_TOKEN            = "token";
    private static final String P_NEW_PASSWORD     = "newPassword";
    private static final String P_CONFIRM_PASSWORD = "confirmPassword";

    /**
     * Attribute keys
     */
    private static final String A_ERROR = "errorMsg";
    private static final String A_EMAIL = "email";

    /**
     * トークン有効期限（24時間 = 86400000ミリ秒）
     */
    private static final long TOKEN_EXPIRY_HOURS = 24;
    private static final long TOKEN_EXPIRY_MILLIS = TOKEN_EXPIRY_HOURS * 60 * 60 * 1000;

    /**
     * ② コンストラクタ
     */

    /**
     * コンストラクタ。
     * 
     * @param req   HTTPリクエスト
     * @param useDB データベース接続の要否
     */
    public PasswordResetService(HttpServletRequest req, boolean useDB) {
        super(req, useDB);
    }

    /**
     * ③ メソッド
     */

    /**
     * パスワードリセット申請画面を表示します。
     * 
     * @param userType ユーザータイプ（'admin', 'secretary', 'customer'）
     * @return ビューパス
     */
    public String showResetRequestForm(String userType) {
        return getViewPath(userType, "password_reset_request");
    }

    /**
     * パスワードリセット申請処理を実行します。
     * メールアドレスの存在確認、トークン生成、メール送信を行います。
     * セキュリティのため、メールアドレスが存在しない場合も同じメッセージを表示します。
     *
     * @param userType ユーザータイプ（'admin', 'secretary', 'customer'）
     * @return ビューパス（成功時：完了画面、失敗時：入力画面）
     */
    public String processResetRequest(String userType) {
        String email = req.getParameter(P_EMAIL);

        // バリデーション
        if (!validation.validatePasswordResetRequest(email)) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            req.setAttribute(A_EMAIL, email);
            return getViewPath(userType, "password_reset_request");
        }

        try (TransactionManager tm = new TransactionManager()) {
            /** 期限切れトークンを削除（クリーンアップ） */
            PasswordResetTokenDAO tokenDAO = new PasswordResetTokenDAO(tm.getConnection());
            int deletedCount = tokenDAO.deleteExpiredTokens();
            if (deletedCount > 0) {
                System.out.println("期限切れトークンを削除しました: " + deletedCount + "件");
            }
            
            UUID userId = findUserIdByEmailAndType(tm, email, userType);
            
            /** ユーザーが存在する場合のみトークン生成・メール送信 */
            if (userId != null) {
                /** 既に有効なトークンが存在するかチェック */
                PasswordResetTokenDTO existingToken = tokenDAO.selectValidByUser(userType, userId);
                
                if (existingToken.getId() != null) {
                    /** 既に有効なトークンが存在する場合はメール送信をスキップ */
                    System.out.println("有効なトークンが既に存在します。メール送信をスキップします。");
                    System.out.println("  ユーザーID: " + userId);
                    System.out.println("  ユーザータイプ: " + userType);

                    /** セキュリティのため、ユーザーには成功メッセージを表示 */
                } else {
                    /** 新規トークン生成 */
                    String token = UUID.randomUUID().toString();
                    Timestamp expiresAt = new Timestamp(
                        System.currentTimeMillis() + TOKEN_EXPIRY_MILLIS
                    );
                    
                    /** トークンをデータベースに保存 */
                    PasswordResetTokenDTO tokenDTO = new PasswordResetTokenDTO();
                    tokenDTO.setUserType(userType);
                    tokenDTO.setUserId(userId);
                    tokenDTO.setToken(token);
                    tokenDTO.setExpiresAt(expiresAt);
                    
                    tokenDAO.insert(tokenDTO);
                    
                    /** メール送信 */
                    sendPasswordResetEmail(email, token, userType);
                }
                
                tm.commit();
            }
            
            /** セキュリティのため、存在しない場合も同じメッセージを表示 */
            return getViewPath(userType, "password_reset_sent");
            
        } catch (Exception e) {
            System.err.println("パスワードリセット申請処理でエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            
            validation.addErrorMsg("処理中にエラーが発生しました。しばらくしてから再度お試しください。");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            req.setAttribute(A_EMAIL, email);
            return getViewPath(userType, "password_reset_request");
        }
    }

    /**
     * パスワード再設定画面を表示します。
     * トークンの検証を行い、有効な場合のみ画面を表示します。
     *
     * @param userType ユーザータイプ（'admin', 'secretary', 'customer'）
     * @return ビューパス（成功時：再設定画面、失敗時：エラー画面）
     */
    public String showResetForm(String userType) {
        String token = req.getParameter(P_TOKEN);

        /** トークンの必須チェック */
        if (token == null || token.isEmpty()) {
            validation.addErrorMsg("無効なリンクです。");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return getViewPath(userType, "password_reset_request");
        }

        try (TransactionManager tm = new TransactionManager()) {
            PasswordResetTokenDAO tokenDAO = new PasswordResetTokenDAO(tm.getConnection());
            PasswordResetTokenDTO tokenDTO = tokenDAO.selectByToken(token);

            // トークンの検証（ユーザータイプも確認）
            String errorMsg = validateToken(tokenDTO, userType);
            if (errorMsg != null) {
                validation.addErrorMsg(errorMsg);
                req.setAttribute(A_ERROR, validation.getErrorMsg());
                return getViewPath(userType, "password_reset_request");
            }

            /** トークンを画面に渡す（hiddenフィールドで次の画面へ） */
            req.setAttribute(P_TOKEN, token);
            return getViewPath(userType, "password_reset_form");
            
        } catch (Exception e) {
            System.err.println("パスワードリセット画面表示でエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            
            validation.addErrorMsg("処理中にエラーが発生しました。");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            return getViewPath(userType, "password_reset_request");
        }
    }

    /**
     * パスワード再設定処理を実行します。
     * トークンの検証、パスワードのバリデーション、パスワード更新を行います。
     *
     * @param userType ユーザータイプ（'admin', 'secretary', 'customer'）
     * @return ビューパス（成功時：完了画面、失敗時：入力画面）
     */
    public String processPasswordReset(String userType) {
        String token = req.getParameter(P_TOKEN);
        String newPassword = req.getParameter(P_NEW_PASSWORD);
        String confirmPassword = req.getParameter(P_CONFIRM_PASSWORD);

        // バリデーション
        if (!validation.validatePasswordReset(token, newPassword, confirmPassword)) {
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            req.setAttribute(P_TOKEN, token);
            return getViewPath(userType, "password_reset_form");
        }

        try (TransactionManager tm = new TransactionManager()) {
            PasswordResetTokenDAO tokenDAO = new PasswordResetTokenDAO(tm.getConnection());
            PasswordResetTokenDTO tokenDTO = tokenDAO.selectByToken(token);

            // トークンの検証（ユーザータイプも確認）
            String errorMsg = validateToken(tokenDTO, userType);
            if (errorMsg != null) {
                validation.addErrorMsg(errorMsg);
                req.setAttribute(A_ERROR, validation.getErrorMsg());
                return getViewPath(userType, "password_reset_request");
            }

            /** パスワードをハッシュ化 */
            String hashedPassword = PasswordUtil.hashPassword(newPassword);

            /** パスワード更新（ロール別のDAOを使用） */
            int updated = updatePasswordByUserType(tm, tokenDTO.getUserId(), hashedPassword, userType);

            if (updated == 0) {
                throw new RuntimeException("パスワードの更新に失敗しました。");
            }

            /** トークンを使用済みにマーク */
            tokenDAO.markAsUsed(token);

            /** そのユーザーの過去のトークンもすべて削除 */
            int deletedCount = tokenDAO.deleteByUser(userType, tokenDTO.getUserId());
            if (deletedCount > 0) {
                System.out.println("ユーザーの過去のトークンを削除しました: " + deletedCount + "件");
            }

            tm.commit();

            return getViewPath(userType, "password_reset_done");
            
        } catch (Exception e) {
            System.err.println("パスワード再設定処理でエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            
            validation.addErrorMsg("処理中にエラーが発生しました。しばらくしてから再度お試しください。");
            req.setAttribute(A_ERROR, validation.getErrorMsg());
            req.setAttribute(P_TOKEN, token);
            return getViewPath(userType, "password_reset_form");
        }
    }

    /**
     * ④ ヘルパーメソッド
     */

    /**
     * ユーザータイプに応じたビューパスを生成します。
     * 
     * @param userType ユーザータイプ（'admin', 'secretary', 'customer'）
     * @param page     ページ名（例: 'password_reset_request'）
     * @return ビューパス
     */
    private String getViewPath(String userType, String page) {
        return "common/" + userType + "/" + page;
    }

    /**
     * メールアドレスとユーザータイプからユーザーIDを検索します。
     * 
     * @param tm       トランザクションマネージャー
     * @param email    メールアドレス
     * @param userType ユーザータイプ（'admin', 'secretary', 'customer'）
     * @return ユーザーID（存在しない場合はnull）
     */
    private UUID findUserIdByEmailAndType(TransactionManager tm, String email, String userType) {
        return switch (userType) {
            case "admin" -> {
                SystemAdminDAO dao = new SystemAdminDAO(tm.getConnection());
                SystemAdminDTO dto = dao.selectByMail(email);
                yield (dto != null && dto.getId() != null) ? dto.getId() : null;
            }
            case "secretary" -> {
                SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
                SecretaryDTO dto = dao.selectByMail(email);
                yield (dto != null && dto.getId() != null) ? dto.getId() : null;
            }
            case "customer" -> {
                CustomerContactDAO dao = new CustomerContactDAO(tm.getConnection());
                CustomerContactDTO dto = dao.selectByMail(email);
                yield (dto != null && dto.getId() != null) ? dto.getId() : null;
            }
            default -> null;
        };
    }

    /**
     * ユーザータイプに応じてパスワードを更新します。
     * 
     * @param tm             トランザクションマネージャー
     * @param userId         ユーザーID
     * @param hashedPassword ハッシュ化されたパスワード
     * @param userType       ユーザータイプ（'admin', 'secretary', 'customer'）
     * @return 更新行数
     */
    private int updatePasswordByUserType(TransactionManager tm, UUID userId, String hashedPassword, String userType) {
        return switch (userType) {
            case "admin" -> {
                SystemAdminDAO dao = new SystemAdminDAO(tm.getConnection());
                yield dao.updatePassword(userId, hashedPassword);
            }
            case "secretary" -> {
                SecretaryDAO dao = new SecretaryDAO(tm.getConnection());
                yield dao.updatePassword(userId, hashedPassword);
            }
            case "customer" -> {
                CustomerContactDAO dao = new CustomerContactDAO(tm.getConnection());
                yield dao.updatePassword(userId, hashedPassword);
            }
            default -> 0;
        };
    }

    /**
     * ⑤ トークン検証メソッド
     */

    /**
     * トークンの検証。
     * 
     * @param tokenDTO トークンDTO
     * @param userType 期待するユーザータイプ
     * @return エラーメッセージ（null: OK、エラーメッセージ: NG）
     */
    private String validateToken(PasswordResetTokenDTO tokenDTO, String userType) {
        // トークンが存在しない
        if (tokenDTO.getId() == null) {
            return "無効なリンクです。";
        }

        // ユーザータイプが一致しない
        if (!userType.equals(tokenDTO.getUserType())) {
            return "無効なリンクです。";
        }

        // トークンが既に使用済み
        if (!tokenDTO.isUnused()) {
            return "このリンクは既に使用されています。";
        }

        // トークンが期限切れ
        if (!tokenDTO.isValid()) {
            return "このリンクは有効期限が切れています。最初からやり直してください。";
        }

        return null; // OK
    }

    /**
     * ⑥ メール送信メソッド
     */

    /**
     * パスワードリセット用のメールを送信します。
     * 
     * @param toEmail  送信先メールアドレス
     * @param token    パスワードリセット用トークン
     * @param userType ユーザータイプ（'admin', 'secretary', 'customer'）
     * @throws ServiceException メール送信に失敗した場合
     */
    private void sendPasswordResetEmail(String toEmail, String token, String userType) {
        try {
            /** 送信元情報 */
            Email from = new Email(MailConfig.getFromEmail(), MailConfig.getFromName());
            
            /** 送信先情報 */
            Email to = new Email(toEmail);
            
            /** 件名（ロールに応じて変更） */
            String roleLabel = getRoleLabel(userType);
            String subject = "【Our Desk】パスワードリセットのご案内（" + roleLabel + "）";
            
            /** パスワードリセット用URL */
            String resetUrl = MailConfig.getPasswordResetUrl(token, userType);
            
            /** メール本文（テキスト形式） */
            String textContent = buildPasswordResetEmailBody(resetUrl, roleLabel);
            Content content = new Content("text/plain", textContent);
            
            /** メールオブジェクトを作成 */
            Mail mail = new Mail(from, subject, to, content);
            
            /** SendGrid APIでメール送信 */
            SendGrid sg = new SendGrid(MailConfig.getSendGridApiKey());
            Request request = new Request();
            
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            /** ログ出力 */
            System.out.println("パスワードリセットメール送信:");
            System.out.println("  宛先: " + toEmail);
            System.out.println("  ステータスコード: " + response.getStatusCode());
            
            /** SendGridは2xx系のステータスコードで成功を示す */
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                System.out.println("  結果: 送信成功");
            } else {
                System.err.println("  結果: 送信失敗");
                System.err.println("  レスポンス: " + response.getBody());
                throw new ServiceException(
                    "メール送信に失敗しました。ステータスコード: " + response.getStatusCode()
                );
            }
            
        } catch (IOException e) {
            System.err.println("パスワードリセットメール送信エラー:");
            System.err.println("  宛先: " + toEmail);
            System.err.println("  エラー内容: " + e.getMessage());
            e.printStackTrace();
            throw new ServiceException("メール送信中にエラーが発生しました。", e);
        }
    }
    
    /**
     * ユーザータイプから表示用ラベルを取得します。
     * 
     * @param userType ユーザータイプ（'admin', 'secretary', 'customer'）
     * @return 表示用ラベル
     */
    private String getRoleLabel(String userType) {
        return switch (userType) {
            case "admin" -> "管理者";
            case "secretary" -> "秘書";
            case "customer" -> "顧客";
            default -> "ユーザー";
        };
    }
    
    /**
     * パスワードリセットメールの本文を生成します。
     * 
     * @param resetUrl  パスワードリセット用URL
     * @param roleLabel ロール名（日本語）
     * @return メール本文
     */
    private String buildPasswordResetEmailBody(String resetUrl, String roleLabel) {
        StringBuilder body = new StringBuilder();
        
        body.append("Our Deskをご利用いただきありがとうございます。\n\n");
        body.append("【").append(roleLabel).append("】パスワードリセットのリクエストを受け付けました。\n\n");
        body.append("以下のURLにアクセスして、新しいパスワードを設定してください。\n");
        body.append("このリンクは24時間有効です。\n\n");
        body.append(resetUrl).append("\n\n");
        body.append("────────────────────────────\n\n");
        body.append("※このメールに心当たりがない場合は、破棄してください。\n");
        body.append("※このメールは送信専用です。返信はできません。\n\n");
        body.append("────────────────────────────\n");
        body.append("Our Desk\n");
        body.append(MailConfig.getFromEmail()).append("\n");
        
        return body.toString();
    }
}
