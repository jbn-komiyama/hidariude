package dao;

/**
 * トランザクション処理中に発生する例外を表す実行時例外
 */
public class TransactionException extends RuntimeException{
	/**
	 * コンストラクタ
	 *
	 * @param cause 原因となった例外
	 */
	public TransactionException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * コンストラクタ
	 *
	 * @param message エラーメッセージ
	 */
	public TransactionException(String message) {
		super(message);
	}

	/**
	 * コンストラクタ
	 *
	 * @param message エラーメッセージ
	 * @param cause 原因となった例外
	 */
	public TransactionException(String message, Throwable cause) {
		super(message, cause);
	}
}
