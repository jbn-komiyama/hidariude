package dao;

/**
 * DAO層で発生する例外を表す実行時例外
 * SQLException などのチェック例外をラップして送出します
 */
public class DAOException extends RuntimeException{
	/**
	 * コンストラクタ
	 *
	 * @param cause 原因となった例外
	 */
	public DAOException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * コンストラクタ
	 *
	 * @param message エラーメッセージ
	 */
	public DAOException(String message) {
		super(message);
	}

	/**
	 * コンストラクタ
	 *
	 * @param message エラーメッセージ
	 * @param cause 原因となった例外
	 */
	public DAOException(String message, Throwable cause) {
		super(message, cause);
	}
}
