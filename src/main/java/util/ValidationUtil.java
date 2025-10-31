package util;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 入力バリデーションのユーティリティ。
 * エラーメッセージは内部に蓄積され、{@link #hasErrorMsg()} と {@link #getErrorMsg()} で参照できます。
 */
public class ValidationUtil {
	private final List<String> errors = new ArrayList<>();
	
	/**
     * 必須チェック。空／null ならエラーメッセージを積み、true を返します。
     * @param label 表示名（例: "会社名"）
     * @param value 入力値
     * @return 必須エラーがあれば true
     */
    public boolean isNull(String label, String value) {
        if (isBlank(value)) {
            errors.add(label + " は必須です。");
            return true;
        }
        return false;
    }
    
    /**
     * UUID 形式かを判定します（メッセージは積みません）。
     * @param s 入力値
     * @return UUID として妥当なら true
     */
    public boolean isUuid(String s) {
        if (isBlank(s)) return false;
        try {
            UUID.fromString(s.trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * yyyy-MM 形式かを判定します（メッセージは積みません）。
     * 月(01..12) まで厳密にチェックします。
     *
     * @param s 入力値
     * @return 妥当なら true
     */
    public boolean isYearMonth(String s) {
        if (isBlank(s)) return false;
        try {
            YearMonth.parse(s.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 金額（0以上の整数）チェック。null/空はエラー扱い。
     * - カンマは許容し除去します（"1,234" → 1234）
     * - 整数でない（小数点含む）、負数、非数値はエラー
     *
     * @param label 表示名（例: "単価（顧客）"）
     * @param value 入力値
     */
    public void mustBeMoneyOrZero(String label, String value) {
        if (isBlank(value)) {
            errors.add(label + " は必須です。");
            return;
        }
        String t = value.trim().replace(",", "");

        /** 数字のみ */
        if (!t.matches("^\\d+$")) {
            errors.add(label + " は 0 以上の整数で入力してください。");
            return;
        }
        try {
            BigDecimal bd = new BigDecimal(t);
            if (bd.signum() < 0) {
                errors.add(label + " は 0 以上の整数で入力してください。");
            }

            /** 小数チェック（整数のみ許容） */
            if (bd.scale() > 0) {
                errors.add(label + " は 小数なしの整数で入力してください。");
            }
        } catch (NumberFormatException e) {
            errors.add(label + " は 数値で入力してください。");
        }
    }
    
    /** 文字列が null または空白のみなら true。 */
    public boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    
    /** 任意のエラーメッセージを追加。 */
    public void addErrorMsg(String msg) {
        if (msg != null && !msg.isBlank()) {
            errors.add(msg);
        }
    }
    
    /** エラーが1件以上あれば true。 */
    public boolean hasErrorMsg() {
        return !errors.isEmpty();
    }
    
    /**
     * 蓄積されたエラーメッセージを HTML 改行で連結して返します。
     * テンプレート側でリスト表示するなら本メソッドではなく {@link #getErrors()} を使ってください。
     */
    public String getErrorMsg() {
        return String.join("<br>", errors);
    }
    
    /** エラーメッセージの生リスト。 */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    /** エラーをクリア。必要に応じて呼び出してください。 */
    public void clear() { 
        errors.clear();
    }


	/**
	 * 文字列長チェック。
	 * 文字列の長さをチェックします。
	 *
	 * @param textName 表示名（例: "文字列"）
	 * @param text 入力値
	 * @param min 最小長
	 * @param max 最大長
	 */
	public void length(String textName, String text, int min, int max) {
		if (text == null || text.length() < min || text.length() > max) {
			this.errors.add(textName + "は、" + min + "文字以上、" + max + "文字以内で入力してください");
		}
	}

	/**
	 * 文字列長チェック。
	 * 文字列の長さをチェックします。
	 *
	 * @param textName 表示名（例: "文字列"）
	 * @param text 入力値
	 * @param max 最大長
	 */
	public void length(String textName, String text, int max) {
		if (text == null || text.length() > max) {
			this.errors.add(textName + "は、" + max + "文字以内で入力してください");
		}
	}

	/**
	 * 数値形式チェック。
	 * 数値の形式をチェックします。
	 *
	 * @param textName 表示名（例: "数値"）
	 * @param text 入力値
	 * @return 数値形式が妥当なら true
	 */
	public boolean isNumber(String textName, String text) {
		boolean flg = false;
		try {
			Long.parseLong(text);
			flg = true;
		} catch (NumberFormatException e) {
			this.errors.add(textName + "は数値を入力してください");
		}
		return flg;
	}

	/**
	 * 整数形式チェック。
	 * 整数の形式をチェックします。
	 *
	 * @param textName 表示名（例: "整数"）
	 * @param text 入力値
	 * @return 整数形式が妥当なら true
	 */
	public boolean isInteger(String textName, String text) {
		boolean flg = false;
		try {
			Integer.parseInt(text);
			flg = true;
		} catch (NumberFormatException e) {
			this.errors.add(textName + "は数値を入力してください");
		}
		return flg;
	}
	
	/**
	 * 郵便番号形式チェック。
	 * 郵便番号の形式をチェックします。
	 *
	 * @param text 入力値
	 * @return 郵便番号形式が妥当なら true
	 */
	public boolean isPostalCode(String text) {
		if (text == null)
			return false;
		if (text.matches("^\\d{7}$")) {
			return true;
		} else {
			this.errors.add("郵便番号の形式で入力してください");
			return false;
		}

	}

	/**
	 * 電話番号形式チェック。
	 * 電話番号の形式をチェックします。
	 *
	 * @param text 入力値
	 * @return 電話番号形式が妥当なら true
	 */
	public boolean isPhoneNumber(String text) {
		if (text == null)
			return false;
		if (text.matches("^\\d{10,11}$"))
			return true;
		if (text.matches("^0\\d{1,4}-\\d{1,4}-\\d{3,4}$")) {
			return true;
		} else {
			this.errors.add("電話番号の形式で入力してください");
			return false;
		}
	}

	/**
	 * メールアドレス形式チェック。
	 * メールアドレスの形式をチェックします。
	 *
	 * @param text 入力値
	 * @return メールアドレス形式が妥当なら true
	 */
	public boolean isEmail(String text) {
		if (text == null)
			return false;
		if (text.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
			return true;
		} else {
			this.errors.add("メールアドレスの形式で入力してください");
			return false;
		}
	}

	/**
	 * 日付チェック。
	 * 日付の形式をチェックします。
	 *
	 * @param textName 表示名（例: "日付"）
	 * @param text 入力値
	 * @return 日付
	 */
	public Date isDate(String textName, String text) {
		Date date = null;
		try {
			SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd");
			date = sdFormat.parse(text);
		} catch (ParseException e) {
			this.errors.add(textName + "は日付を(yyyy/MM/dd)の形式で入力してください");
		}
		return date;
	}

	/**
	 * パスワード強度チェック。
	 * - 8文字以上
	 * - 英大文字 (A-Z) 1文字以上
	 * - 英小文字 (a-z) 1文字以上
	 * - 数字 (0-9) 1文字以上
	 *
	 * @param password パスワード
	 * @return 強度要件を満たす場合true
	 */
	public boolean isStrongPassword(String password) {
		if (isBlank(password)) {
			errors.add("パスワードは必須です。");
			return false;
		}
		
		if (password.length() < 8) {
			errors.add("パスワードは8文字以上で入力してください。");
			return false;
		}
		
		boolean hasUpperCase = password.chars().anyMatch(Character::isUpperCase);
		boolean hasLowerCase = password.chars().anyMatch(Character::isLowerCase);
		boolean hasDigit = password.chars().anyMatch(Character::isDigit);
		
		if (!hasUpperCase || !hasLowerCase || !hasDigit) {
			errors.add("パスワードは8文字以上で、英大文字・英小文字・数字を含む必要があります。");
			return false;
		}
		
		return true;
	}

	/**
	 * パスワード一致チェック。
	 * パスワードと確認用パスワードが一致するかを確認します。
	 *
	 * @param password        パスワード
	 * @param confirmPassword 確認用パスワード
	 * @return 一致する場合true
	 */
	public boolean isPasswordMatch(String password, String confirmPassword) {
		if (password == null || confirmPassword == null) {
			errors.add("パスワードと確認用パスワードを入力してください。");
			return false;
		}
		
		if (!password.equals(confirmPassword)) {
			errors.add("パスワードと確認用パスワードが一致しません。");
			return false;
		}
		
		return true;
	}

	/**
	 * パスワードリセット申請のバリデーション。
	 * メールアドレスの必須チェックと形式チェックを行います。
	 *
	 * @param email メールアドレス
	 * @return バリデーションに成功した場合true
	 */
	public boolean validatePasswordResetRequest(String email) {
		/** 必須チェック */
		if (isNull("メールアドレス", email)) {
			return false;
		}
		
		/** メールアドレス形式チェック */
		if (!isEmail(email)) {
			return false;
		}
		
		return true;
	}

	/**
	 * パスワード再設定のバリデーション。
	 * トークン、新パスワード、確認用パスワードのバリデーションを行います。
	 *
	 * @param token           トークン
	 * @param newPassword     新パスワード
	 * @param confirmPassword 確認用パスワード
	 * @return バリデーションに成功した場合true
	 */
	public boolean validatePasswordReset(String token, String newPassword, String confirmPassword) {
		/** トークン必須チェック */
		if (isNull("トークン", token)) {
			return false;
		}
		
		/** パスワード必須チェック */
		if (isNull("新しいパスワード", newPassword)) {
			return false;
		}
		if (isNull("確認用パスワード", confirmPassword)) {
			return false;
		}
		
		/** パスワード強度チェック */
		if (!isStrongPassword(newPassword)) {
			return false;
		}
		
		/** パスワード一致チェック */
		if (!isPasswordMatch(newPassword, confirmPassword)) {
			return false;
		}
		
		return true;
	}

}
