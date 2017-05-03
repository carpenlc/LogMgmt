package mil.nga.logmgmt.exceptions;

public class InputException extends Exception {

	/**
	 * Eclipse-generated serial version UID.
	 */
	private static final long serialVersionUID = 7467028186375854971L;

	/** 
	 * Default constructor requiring a message String.
	 * @param msg Information identifying why the exception was raised.
	 */
	public InputException(String msg) {
		super(msg);
	}
}
