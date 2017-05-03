package mil.nga.logmgmt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.nga.logmgmt.exceptions.InputException;

/**
 * Extension of class InputFile adding some logic to handle the cleanup
 * of archived CRL files from the Axway Valicert application.
 * 
 * @author carpenlc
 */
public class VESArchives extends InputFile {

	/**
	 * Set up the Log4j system for use throughout the class
	 */		
	private static final Logger LOGGER = LoggerFactory.getLogger(
			VESArchives.class);
	
	/**
	 * Default search pattern for the VES archive directories.
	 */
	private static final String DIR_SEARCH_PATTERN = new String("archive");
	
	/**
	 * Default search pattern for the VES archived CRL files.
	 */
	private static final String FILE_SEARCH_PATTERN = new String("*.crl");
	
	/**
	 * Default constructor requiring clients to supply the starting location
	 * for the search.
	 * 
	 * @param inputPath Starting location for the directory search.
	 * @throws InputException Thrown if the input data does not make 
	 * sense.
	 */
	public VESArchives(String inputPath) throws InputException {
		super(inputPath, DIR_SEARCH_PATTERN);
	}
	
	/**
	 * Get the year/month/day associated with the input date.
	 * 
	 * @param date The input date extracted from a target file.
	 * @return The year/month/day associated with the input Date.
	 */
	private String getYearMonthDay(Path file) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String           stringDate = "unavailable";
		String           method     = "getYearMonthDay() - ";
		
		try {
			stringDate = sdf.format(
					Files.getLastModifiedTime(file).toMillis());
		}
		catch (IOException ioe) {
			LOGGER.warn(method 
					+ "Unexpected IOException obtaining last modified "
					+ "time for file [ "
					+ file.toString()
					+ " ]");
		}
		return stringDate;
	}
	
	/**
	 * This method will delete all of the files in the input list.
	 * @param files List of files to delete.
	 * @param test If set to true nothing will be deleted.
	 * @return The amount of space recovered from the process.
	 */
	public long delete(List<Path> files, boolean test) {
		
		String method    = "delete() - ";
		long   sizeAccum = 0;
		
		if ((files != null) && (!files.isEmpty())) { 
			for (Path file : files) {
				
				try {
					sizeAccum += Files.size(file);
					if (test) {
						LOGGER.info("*** TEST MODE ***: Process would "
								+ "remove [ " 
								+ file.toString()
								+ " ] with date of [ " 
								+ getYearMonthDay(file)
								+ " ].");
					}
					else {
						LOGGER.info(method 
								+ "Removing [ " 
								+ file.toString()
								+ " ] with date of [ " 
								+ getYearMonthDay(file)
								+ " ].");
						Files.delete(file);
					}
				}
				catch (IOException ioe) {
					LOGGER.warn(method 
							+ "Unexpected exception removing file [ "
							+ file.toString()
							+ " ].  Error [ "
							+ ioe.getMessage() 
							+ " ].");
				}
			}
		}
		else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(method 
						+ "Input list is null or empty.");
			}
		}
		LOGGER.info(method 
				+ "Recovered [ "
				+ sizeAccum 
				+ " ] bytes for the filesystem.");
		return sizeAccum;
		
	}
	
	/**
	 * Get the list of archived CRLs residing in the target directory.  The 
	 * output list will be sorted oldest to newest.
	 * 
	 * @param directory Starting location for the search.
	 * @throws InputException Thrown if there are problems accessing the input
	 * path.
	 */
	public List<Path> getAllCRLs(Path directory) throws InputException {
		
		String     method = "getCRLs() - ";
		List<Path> crls   = null;
		
		if ((directory == null) || (!Files.exists(directory))) {
			LOGGER.warn(method
					+ "Input directory path is null or does not exist.");
		}
		else {
			if (Files.isDirectory(directory)) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(method 
							+ "Processing archive directory [ " 
							+ directory.toString() 
							+ " ].");
				}
				super.setInputPath(directory.toString());
				super.setInputPattern(FILE_SEARCH_PATTERN);
				crls = getCandidates();
			}
			else {
				LOGGER.warn(method
						+ "Input path is not a directory.  Path supplied [ "
						+ directory.toString() 
						+ " ].");
			}
		}
		
		if (crls != null) {
			
			// Sort oldest to newest by lastModifiedDate
			Collections.sort(crls, new Comparator<Path>() {
			    
				@Override
				public int compare(Path o1, Path o2) {
			    	
			    	String method = "compare() - ";
			    	int    value  = 0;
			    	
			        try {
			            value =  Files.getLastModifiedTime(o1).compareTo(Files.getLastModifiedTime(o2));
			        } 
			        catch (IOException e) {
			            LOGGER.warn(method 
			            		+ "IOException encountered doing comparison.  Error [ "
			            		+ e.getMessage() 
			            		+ " ].");
			        }
			        return value;
			    }
			});
		}
		return crls;
	}
	
}
