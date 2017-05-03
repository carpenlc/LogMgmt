package mil.nga.logmgmt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.nga.logmgmt.exceptions.InputException;
import mil.nga.logmgmt.exceptions.OutputException;
import mil.nga.util.Options;
import mil.nga.util.Options.Multiplicity;
import mil.nga.util.Options.Separator;

/**
 * Very specific tool for use in managing the JBoss/Wildfly server log
 * files on servers that do not archive the log files.  Translation: 
 * just delete the log files older than a certain number of days.
 * 
 * Admittedly, it is probably much easier to write a script to do this 
 * but I had most of the logic already developed for the LogMgmt tool so 
 * this was just cut-and-paste.
 * 
 * @author L. Craig Carpenter
 */
public class CleanupFiles extends InputFile {

	/**
	 * The number of milliseconds in a day.
	 */
	public static final long MILLISECONDS_PER_DAY = 1000 * 60 * 60 * 24;
	
	/**
	 * The age (in days) of files that will need to be deleted.
	 */
	private int age = -1;
		
    /**
     * Set up the LogBack system for use throughout the class
     */        
    private static final Logger LOGGER = LoggerFactory.getLogger(
    		CleanupFiles.class);
	
	/**
	 * Default constructor that kicks off processing.
	 * 
	 * @param directory The directory to monitor.
	 * @param age Delete all files older than this number of days.
	 * @param test If set to true, files aren't actually deleted.
	 * 
	 * @throws InputException Thrown if there is an issue with the data 
	 * provided by the user.
	 */
	public CleanupFiles(
			String directory, 
			int age, 
			boolean test) 
					throws InputException {
		
		super(directory, "*");
		
		String     method     = "Constructor() - ";
		List<Path> candidates = super.getCandidates();
		setAge(age);
		
		for (Path current : candidates) {
			try {
				if (isReady(current)) {
					if (test) {
						LOGGER.info(method 
								+ "Test mode [ "
								+ test 
								+ " ] file [ "
								+ current.toString()
								+ " ] to be deleted.");
					}
					else {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug(method 
									+ "Deleting file [ "
									+ current.toString()
									+ " ].");
						}
						Files.delete(current);
					}
				}
			}
			catch (IOException ioe) {
				LOGGER.error(method 
						+ "Unexpected IOException processing file [ "
						+ current.toAbsolutePath()
						+ " ].  Error message [ "
						+ ioe.getMessage()
						+ " ].  Could not obtain file attributes, or "
						+ "could not delete the file.");
			}
		}
	}
	
	/**
	 * Determine whether or not the input file should be deleted.
	 * This method will return true if the file is older than the 
	 * user-specified age parameter and is not a directory.
	 * 
	 * @param path The file to test.
	 * @return True if the input file should be deleted, false 
	 * otherwise.
	 * @throws IOException Thrown if there are problems accessing the
	 * file attributes of the input path object.
	 */
	public boolean isReady(Path path) throws IOException {
		
		String  method = "isReady() - ";
		boolean delete = false;
		
		if (!Files.isDirectory(path)) {
			BasicFileAttributes attr = Files.readAttributes(
					path, 
					BasicFileAttributes.class);
			FileTime time = attr.creationTime();
			if (time != null) {
				long fileTime = time.toMillis();
				long currentTime = System.currentTimeMillis();
				long delta = MILLISECONDS_PER_DAY * getAge();
				if ((currentTime - fileTime) > delta) {
					delete = true;
				}
				else {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug(method 
								+ "File [ "
								+ path.toAbsolutePath()
								+ " ] is newer than [ "
								+ getAge() 
								+ " ] days.");	
					}
				}
			}
			else {
				LOGGER.warn(method 
						+ "Unable to obtain file creation date for file [ "
						+ path.toAbsolutePath()
						+ " ].");
			}
		}
		else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(method 
					+ "File [ "
					+ path.getFileName()
					+ " ] is a directory.");
			}
		}
		return delete;
	}
	
	/**
	 * Setter method for the age param.
	 * @param age Age in days of files to delete.
	 */
	public void setAge(int age) {
		this.age = age;
	}
	
	/**
	 * Getter method for the age param.
	 * @return The age param.
	 */
	public int getAge() {
		return this.age;
	}	
	
	/**
	 * Simple static method to print the help and usage String information.
	 */
	private static void printHelp() {
		System.out.println("");
		System.out.println(ManageLogsI.HELP_STRING);
		System.out.println("");
		System.out.println(ManageLogsI.USAGE_STRING);
	}
	
	/**
	 * Driver method used to extract the command line parameters and 
	 * initiate processing.
	 * 
	 * @param args Input command line arguments
	 * @throws InputException Thrown if the input data does not make 
	 * sense.
	 * @throws OutputException Thrown if errors are encountered processing
	 * the output.
	 */
	public static void main(String[] args) throws InputException {
		
		String  method    = "main() - ";
		String  directory = null;
		int     age       = -1;
		boolean testMode  = false;
		
		// Set up the command line options
		Options opt = new Options(args, 0);
		opt.getSet().addOption(
				"directory", 
				Separator.EQUALS, 
				Multiplicity.ONCE);
		opt.getSet().addOption(
				"age", 
				Separator.EQUALS, 
				Multiplicity.ONCE);
		
		opt.getSet().addOption("test", Multiplicity.ZERO_OR_MORE);
		opt.getSet().addOption("h", Multiplicity.ZERO_OR_MORE);
		opt.getSet().addOption("help", Multiplicity.ZERO_OR_MORE);
		
		// Make sure the options make sense
		if (!opt.check(true, false)) {
			System.out.println(ManageLogsI.USAGE_STRING);
			System.exit(1);
		}
		
		// See if the user wanted the help message displayed.
		if (opt.getSet().isSet("h") || opt.getSet().isSet("help")) {
			CleanupFiles.printHelp();
			System.exit(0);
		}
		
		if (opt.getSet().isSet("test")) { 
			testMode = true;
		}
		
		// Get the directory path
		if (opt.getSet().isSet("directory")) {
			directory = opt.getSet().getOption("directory").getResultValue(0);
			if ((directory == null) || (directory.isEmpty())) {
				LOGGER.error(method 
						+ "ERROR: -directory was blank or not supplied!");
				CleanupFiles.printHelp();
				System.exit(1);
			}
		}
		else {
			LOGGER.error(method 
					+ "ERROR: -directory option must be supplied!");
			CleanupFiles.printHelp();
			System.exit(1);
		}
			
		// Get the directory path
		if (opt.getSet().isSet("age")) {
			String data = opt.getSet().getOption("age").getResultValue(0);
			if ((data == null) || (data.isEmpty())) {
				LOGGER.error(method 
						+ "ERROR: -age was blank or not supplied!");
				CleanupFiles.printHelp();
				System.exit(1);
			}
			age = Integer.parseInt(data.trim());
			if (age <= 0) { 
				LOGGER.error(method 
						+ "ERROR: age must be greater than 0, value supplied [ "
						+ age
						+ " ].");
				CleanupFiles.printHelp();
				System.exit(1);
			}
		}
		else {
			LOGGER.error(method 
					+ "ERROR: -age option must be supplied!");
			CleanupFiles.printHelp();
			System.exit(1);
		}
		
		LOGGER.info(method 
				+ "Command line arguments supplied: directory [ "
				+ directory 
				+ " ], age [ "
				+ age
				+ " ], test [ " 
				+ testMode 
				+ " ].");
		
		new CleanupFiles(directory, age, testMode);
	}
}
