package mil.nga.logmgmt;

/**
 * Simple interface containing the names of the properties used by the 
 * log management application.
 * 
 * @author L. Craig Carpenter
 */
public interface LogMgmtI {
    
    /**
     * OPTIONAL: The application name that generated the log file, or a 
     * category associated with the log file.  This parameter will be 
     * utilized in output path calculation.
     */
    public static final String APPLICATION_NAME  = "application.name";

    /**
     * True/false parameter indicating whether the input file should be
     * deleted after being copied to the output path.
     */
    public static final String INPUT_FILE_DELETE = "input.file.delete";
    
    /**
     * The path in which to look for the input files.
     */
    public static final String INPUT_PATH        = "input.path";
    
    /**
     * The REGEX pattern used to identify the target input file.
     */
    public static final String INPUT_PATTERN     = "input.pattern";
    
    /**
     * True/false parameter indicating whether the output file is to be 
     * compressed.  The default is False.
     */
    public static final String OUTPUT_COMPRESS   = "output.compress";
    
    /**
     * The number of days to delay before removing the input file from 
     * the target directory.
     */
    public static final String OUTPUT_DELAY      = "output.delay";
    
    /**
     * Target output location where the input files should be copied/moved.
     */
    public static final String OUTPUT_BASE_PATH  = "output.path";
    
    /** 
     * String used if the server group is not defined.
     */
    public static final String DEFAULT_SERVER_GROUP = "gateway";
    
    /**
     * If not supplied, set the default output delay parameter to 5 days.
     */
    public static final int DEFAULT_OUTPUT_DELAY = 5;
    
    /**
     * Usage String printed when incorrect arguments are supplied.
     */
    public static final String USAGE_STRING = new String(
            "Usage: java mil.nga.log.LogMgmt "
            + "-properties=<path-to-properties-file> "
            + "[ -serverGroup=<server-group> ] " 
            + "[ -customPrefix=<prefix> ] "
            + "[ -baseOverride=<base directory> ] "
            + "[-h] [-help]");
    
    /**
     * Help string printed when -h or -help appear on the command line.
     */
    public static final String HELP_STRING = new String(
            "This application is used for helping to manage disk space by "
            + "archiving old log files in accordance with parameters supplied " 
            + "in an external properties file.  Options supported are as "
            + "follows:\n\n"
            + "-propertiesFile=<path-to-properties-file>  Required.  This "
            + "identifies the external properties file defining what log to "
            + "archive and what to do with it.\n"
            + "[ -serverGroup=<group identifier> ]   Optional but "
            + "recommended.  This property is used in organizing the output "
            + "log files. \n"
            + "[ -customPrefix=<prefix> ] Optional string added to the name "
            + "of the archived file.  Useful when archiving multiple logs "
            + "associated with the same application. \n"
            + "[ -baseOverride=<base directory> ] Override the base directory "
            + "defined in the target properties file. \n"
            + "[-h] [-help]    Prints this help message.\n\n");
}
