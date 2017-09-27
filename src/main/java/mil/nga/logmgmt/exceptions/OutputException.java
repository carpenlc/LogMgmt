package mil.nga.logmgmt.exceptions;

public class OutputException extends Exception {

    /**
     * Eclipse-generated serialVersionUID
     */
    private static final long serialVersionUID = -3060252947754967277L;

    /** 
     * Default constructor requiring a message String.
     * @param msg Information identifying why the exception was raised.
     */
    public OutputException(String msg) {
        super(msg);
    }
}
