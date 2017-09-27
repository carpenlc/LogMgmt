package mil.nga.logmgmt;

public interface ManageLogsI {

	/**
	 * Usage String printed when incorrect arguments are supplied.
	 */
	public static final String USAGE_STRING = new String(
			"Usage: java mil.nga.log.CleanupFiles "
	        + "-directory=<directory-to-monitor> "
			+ "-age=<time-in-days> "
	        + "[ -pattern=<file pattern> ] "
			+ "[ -test ] "
			+ "[-h] [-help]");
	
	/**
	 * Help string printed when -h or -help appear on the command line.
	 */
	public static final String HELP_STRING = new String(
			"This application will walk through a given directory deleting" 
			+ "files older than a specified number of days.\n\n"
			+ "-directory=<directory-to-monitor>  Required.  Specifies the "
			+ "directory to be monitored.\n"
			+ "-age=<time-in-days>                Required.  Delete all files "
			+ "older than the number of days specified by this flag.\n"
			+ "[ -pattern=<file pattern> ]   Restrict deletes to files matching "
			+ "this input pattern.  \n"
	        + "[ -test ]                          If supplied, the "
			+ "application will only print out what files would be deleted "
	        + "but does not actually delete them. \n"
			+ "[-h] [-help]    Prints this help message.\n\n");
}
