package service;

/**
 * サービス層での例外を表すクラス。
 */
public class ServiceException extends RuntimeException{

	/**
	 * コンストラクタ。
	 *
	 * @param cause 原因となった例外
	 */
	public ServiceException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * コンストラクタ。
	 *
	 * @param message エラーメッセージ
	 */
	public ServiceException(String message) {
		super(message);
	}

	/**
	 * コンストラクタ。
	 *
	 * @param message エラーメッセージ
	 * @param cause 原因となった例外
	 */
	public ServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
