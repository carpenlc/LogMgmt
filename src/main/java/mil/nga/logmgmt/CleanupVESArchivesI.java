package mil.nga.logmgmt;

/**
 * Simple interface containing the names of the properties used by the 
 * VES cleanup application.
 * 
 * @author L. Craig Carpenter
 */
public interface CleanupVESArchivesI {
	
	/**
	 * Constant defining how many of the VES CRL archive files that we
	 * should save.
	 */
	public static final int ARCHIVES_TO_SAVE  = 5;
	
	/**
	 * Usage String printed when incorrect arguments are supplied.
	 */
	public static final String USAGE_STRING = new String(
			"Usage: java mil.nga.log.CleanupVESArchives "
	        + "-searchLoc=<directory-path> "
			+ "[ -test ] "
			+ "[-h] [-help]");
	
	/**
	 * Help string printed when -h or -help appear on the command line.
	 */
	public static final String HELP_STRING = new String(
			"This application is used for helping to manage disk space by "
			+ "deleting CRL files that have been archived by the Axway "
			+ "Valicert application:\n\n"
			+ "-searchLoc=<directory-path>  Required.  This "
			+ "identifies the starting location from which to start the "
			+ "search process.\n"
			+ "[ -test ]       Optional: The default is false.  If true the "
	        + "application will not actually delete anything, it will simply "
			+ "print the files that would have been deleted.\n"
			+ "[-h] [-help]    Prints this help message.\n\n");
	
}
