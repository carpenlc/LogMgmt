package mil.nga.logmgmt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.nga.logmgmt.exceptions.InputException;

/**
 * Based upon the input data provided through a Java properties object, this 
 * class will identify candidate input files for archival.
 * 
 * @author L. Craig Carpenter
 */
public class InputFile {
    
    /**
     * Set up the Log4j system for use throughout the class
     */        
    private static final Logger LOGGER = 
            LoggerFactory.getLogger(InputFile.class);

    /**
     * Path in which to look for candidate files for archival.
     */
    private String path    = null;
    
    /**
     * Glob pattern used to identify candidate files.
     */
    private String pattern = null;
    
    /**
     * Constructor allowing clients to supply the starting path and pattern
     * via String arguments vice a Properties object.
     * 
     * @param inputPath The starting location.
     * @param inputPattern The pattern to search for.
     * @throws InputException Thrown if any of the required input data is not
     * supplied by the caller.
     */
    public InputFile(String inputPath, String inputPattern) throws InputException {
        setInputPath(inputPath);
        setInputPattern(inputPattern);
    }
    
    /**
     * Constructor requiring the caller to supply a populated 
     * properties object containing the following required properties:
     * <li><code>input.path</code></li>
     * <li><code>input.pattern</code></li>
     * 
     * @see mil.nga.logmgmt.LogMgmtI
     * @param properties A properties object containing the required input 
     * properties.
     * @throws InputException Thrown if any of the required input data is not
     * supplied by the caller.
     */
    public InputFile (Properties properties) throws InputException {
        String method = "Constructor() - ";
        if (properties != null) {
            setInputPath(properties.getProperty(LogMgmtI.INPUT_PATH, null));
            setInputPattern(properties.getProperty(LogMgmtI.INPUT_PATTERN, null));
        }
        else {
            String msg = method + "The input properties object is null!";
            LOGGER.error(msg);
            throw new InputException(msg);
        }
    }
    
    /**
     * Determine if the input path exists.
     * 
     * @param value The target directory path.
     * @return True if the directory exists, false otherwise.
     */
    private boolean exists(String value) {
        Path path = Paths.get(value);
        return Files.exists(path);
    }
    
    /**
     * Accessor method for the parent directory path.
     * @return The parent directory path.
     */
    public String getInputPath() {
        return path;
    }
    
    /**
     * Accessor method for the file pattern that we wish to search for.
     * @return The target input file REGEX.
     */
    public String getInputPattern() {
        return pattern;
    }
    
    /**
     * Mutator method for the file pattern that we wish to search for.
     * @param value The target input path
     * @throws InputException Thrown if the input parameter is null or 
     * empty.
     */
    public void setInputPath(String value) throws InputException {
        String method = "setInputPath() - ";
        if ((value == null) || (value.isEmpty())) {
            String msg = method
                    + "The required property [ "
                    + LogMgmtI.INPUT_PATH
                    + " ] identifying the path in which the input file "
                    + "resides was not supplied in the input Properties "
                    + "object.";
            LOGGER.error(msg);
            throw new InputException(msg);
        }
        
        // Make sure the identified path exists.
        if (exists(value)) {
            path = value;
        }
        else {
            String msg = method 
                    + "The input path [ "
                    + value 
                    + " ] identified in property [ "
                    + LogMgmtI.INPUT_PATH
                    + " ] does not exist.";
            LOGGER.error(msg);
            throw new InputException(msg);
        }
    }
    
    /**
     * Mutator method for the file pattern that we wish to search for.
     * @param value The target input file REGEX.
     * @throws InputException Thrown if the input parameter is null or 
     * empty.
     */
    public void setInputPattern(String value) throws InputException {
        String method = "setInputPattern() - ";
        if ((value == null) || (value.isEmpty())) {
            String msg = method 
                    + "The required property [ "
                    + LogMgmtI.INPUT_PATTERN
                    + " ] identifying the REGEX for the target input file was "
                    + "not supplied in the Properties object.";
            LOGGER.error(msg);
            throw new InputException(msg);
        }
        pattern = value;
    }
    
    /**
     * Based on the starting path and target file REGEX supplied during 
     * construction, this class will generate a list of candidate files for
     * archiving.
     * 
     * @return A list of candidate files for archival.
     */
    public List<Path> getCandidates() {
        
        String     method  = "getCandidates() - ";
        String     path    = getInputPath();
        String     pattern = getInputPattern();
        List<Path> results = null;
        
        LOGGER.info(method 
                + "Searching [ "
                + path 
                + " ] for files matching glob [ "
                + pattern
                + " ].");
        
        try {
            results = FileFinder.find(path, pattern);
            if ((results == null) || (results.size() == 0)) {
                LOGGER.warn(method
                        + "Unable to find a file in path [ "
                        + path
                        + " ] matching glob [ "
                        + pattern
                        + " ].");
            }
        }
        catch (IOException ioe) {
            LOGGER.error(method 
                    + "Unexpected IOException encountered while searching "
                    + "for candidate input files.  Error encountered [ "
                    + ioe.getMessage()
                    + " ].");
        }
        return results;
    }
    
}
