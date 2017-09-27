package mil.nga.logmgmt;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static java.nio.file.StandardCopyOption.*;

import mil.nga.logmgmt.exceptions.OutputException;
import mil.nga.util.FileUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OutputFile {

    /**
     * Set up the LogBack system for use throughout the class
     */        
    private static final Logger LOGGER = 
            LoggerFactory.getLogger(OutputFile.class);
    
    private String  application  = null;
    private String  customPrefix = null;
    private int     outputDelay  = -1;
    private String  outputPath   = null;
    private String  serverGroup  = null;
    private boolean compress     = false;
    
    /**
     * Default host name if the host name cannot be determined.
     */
    public static final String DEFAULT_HOST = "unknown";
    
    /**
     * Default host name if the host name cannot be determined.
     */
    public static final String DEFAULT_APPLICATION = "default";
    
    /**
     * Length of the temporary filename that will be used as an intermediate
     * file when attempting to perform "move with compression" on Windows 
     * servers.
     */
    public static final int TEMP_FILENAME_LENGTH = 8;
    
    /**
     * When constructing the destination path we always need to utilize the 
     * forward slash character for both Linux and Windows.  
     */
    public static final String PATH_SEPARATOR = "/";
    
    /**
     * Collects the required parameters and invokes processing.
     * 
     * @param props
     * @param serverGroup
     * @param customPrefix A string that will be added to the output file name.
     * @throws OutputException
     */
    public OutputFile (
            Properties props,
            String     serverGroup,
            String     customPrefix) throws OutputException {
        
        setApplication(props.getProperty(LogMgmtI.APPLICATION_NAME, null));
        setOutputBasePath(props.getProperty(LogMgmtI.OUTPUT_BASE_PATH, null));
        setOutputDelay(props.getProperty(LogMgmtI.OUTPUT_DELAY, null));
        setCustomPrefix(customPrefix);
        setServerGroup(serverGroup);
        setCompression(props.getProperty(LogMgmtI.OUTPUT_COMPRESS, null));
        
    }
    
    /**
     * Using NIO2 get the file attributes and return the last modified date.
     * 
     * @param file An on-disk file.
     * @return The last modified date.
     * @throws IOException Thrown if there are problems getting the file 
     * attribute information.
     */
    public long getFileDate(Path file) throws IOException {
        BasicFileAttributes attr = Files.getFileAttributeView(
                file, BasicFileAttributeView.class).readAttributes();
        return attr.lastModifiedTime().toMillis();
    }
    
    /**
     * This class received a list of "candidate" files for archive.  Any logic 
     * required to make a decision on whether the file should be archived is 
     * to be added here.
     * 
     * @param file Candidate file.
     * @return True if we should proceed with archiving, false otherwise.
     * @throws IOException Thrown if there are problems getting the file 
     * attribute information.
     */
    private boolean archive (Path file) throws IOException {
        
        String  method   = "archive() - ";
        boolean archive  = false;
        long    fileDate = getFileDate(file);
        long    now      = Calendar.getInstance().getTimeInMillis();
        long    daysOld  = (now - fileDate) / (24 * 60 * 60 * 1000);
        
        if (daysOld >= getOutputDelay()) {
            archive = true;
        }
        else {
            LOGGER.debug(method 
                    + "Input file [ "
                    + file.getFileName().toString() 
                    + " ] is not old enough to archive.  File must be more "
                    + "than [ "
                    + Integer.toString(getOutputDelay()) 
                    + " ] days old.");
        }
        return archive;
    }
    
    /**
     * Extract the file extension from an input file name.
     * 
     * @param fileName The file name.
     * @return The file extension.
     */
    private String getFileExtension(String fileName) {
        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
            return stripVersion(fileName.substring(fileName.lastIndexOf(".")+1));
        }
        else {
            return "";
        }
    }
    
    
    /**
     * Remove the extension from a filename.
     * 
     * @param value The input filename.
     * @return The filename without the extension.
     */
    static String stripVersion (String value) {
        if (value == null) return null;
        int pos = value.lastIndexOf("-");
        if (pos == -1) return value;
        return value.substring(0, pos);
    }
    
    /**
     * Remove the extension from a filename.
     * 
     * @param value The input filename.
     * @return The filename without the extension.
     */
    static String stripExtension (String value) {
        if (value == null) return null;
        int pos = value.lastIndexOf(".");
        if (pos == -1) return value;
        return value.substring(0, pos);
    }
    
    /**
     * Get the month associated with the input date.
     * 
     * @param date The input date extracted from a target file.
     * @return The month associated with the input Date.
     */
    private String getMonth(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM");
        return sdf.format(date);
    }
    
    /**
     * Get the year associated with the input date.
     * 
     * @param date The input date extracted from a target file.
     * @return The year associated with the input date.
     */
    private String getYear(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        return sdf.format(date);
    }
    
    /**
     * Get the year/month/day associated with the input date.
     * 
     * @param date The input date extracted from a target file.
     * @return The year/month/day associated with the input Date.
     */
    private String getYearMonthDay(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return sdf.format(date);
    }
    
    /**
     * If the path doesn't exist, create it.
     * 
     * @param path A path string. 
     */
    private boolean checkPath (String path) {
        
        String  method = "checkPath() - ";
        boolean pathOK = false;
        
        try {
            
            Path dir = Paths.get(path);
            if (!isWindows()) {
                Set<PosixFilePermission> perms =
                        PosixFilePermissions.fromString("rwxr-xr-x");
                FileAttribute<Set<PosixFilePermission>> attr =
                        PosixFilePermissions.asFileAttribute(perms);
                Files.createDirectories(dir, attr);
            }
            else {
                Files.createDirectories(dir);
            }
            pathOK = true;
            
        }
        catch (IOException ioe) {
            LOGGER.error(method
                    + "Unable to determine the state of the requested output "
                    + "path [ "
                    + path 
                    + " ].  An unexpected IOException was encountered.  "
                    + "Message [ "
                    + ioe.getMessage()
                    + " ].");
        }
        return pathOK;
    }
    
    /**
     * Get the host name of the machine on which this code is running.
     *  
     * @return The host name (with domain information stripped off) or 
     * the DEFAULT_HOST string if we're unable to determine the host 
     * name.
     */
    private String getHostName() {
        
        String host   = null;
        String method = "getHostName() - ";
        
        try {
            host = InetAddress.getLocalHost().getCanonicalHostName();
            if ((host == null) || (host.isEmpty())) {
                host = DEFAULT_HOST;
            }
            else {
                if (host.indexOf(".") > 0) {
                    host = host.substring(0, host.indexOf("."));
                }
            }
        }
        catch (UnknownHostException uhe) {
            LOGGER.warn(method 
                    + "Unexpected UnknownHostException encountered.  "
                    + "Error ["
                    + uhe.getMessage()
                    + "].");
            host = DEFAULT_HOST;
        }
        return host;
    }
    
    /**
     * Accessor method for the optional input string identifying the 
     * application that generated the log file.
     * 
     * @return The application that generated the log file.
     */
    public String getApplication() {
        return application;
    }
    
    /**
     * Accessor method for the boolean indicating whether or not compression 
     * should be applied to the output file(s).
     * @return True or false.
     */
    public boolean getCompression() {
        return compress;
    }
    
    /**
     * Mutator method for the custom prefix.
     * @return Custom prefix string.
     */
    public String getCustomPrefix() {
        return customPrefix;
    }
    
    /**
     * Determine if the target destination file exists.
     * 
     * @param destPath The directory path in which the file will be stored.
     * @param filename The anticipated name of the file.
     * @param extension The file name extension.
     * @return True if the target file exists, false otherwise.
     */
    private boolean exists(String destPath, String filename, String extension) {
        StringBuilder sb = new StringBuilder();
        sb.append(destPath);
        if (!destPath.endsWith(File.separator)) {
            sb.append(File.separator);
        }
        sb.append(filename);
        sb.append(extension);
        Path destination = Paths.get(sb.toString());
        return Files.exists(destination);
    }
    
    /**
     * Method implemented to insert a version number in the output file
     * name if the calculated filename already exists.  This logic was 
     * implemented because we found that there were audit logs that were 
     * creating several each day with a version number that was interpreted 
     * as being part of the extension.
     * 
     * @param destPath The path in which the file will be stored.
     * @param partialFilename The calculated filename.
     * @param extension The output file extension.
     * @return A filename guaranteed not to exist.
     */
    private String checkVersioning(
            String destPath, 
            String partialFilename, 
            String extension) {
        
        String method    = "checkVersioning() - ";
        String candidate = new String(partialFilename);
        int    version   = 0;
        
        while (exists(destPath, candidate, extension)) {
            version++;
            candidate = partialFilename + "-" + Integer.toString(version);
            LOGGER.debug(method 
                    + "File exists.  Trying [ "
                    + candidate + extension 
                    + " ].");
        }
        return (candidate + extension);
    }
    
    /**
     * The destination file fill consist of the following components: 
     * 
     * <li>host name</li>
     * <li>custom prefix</li>
     * <li>actual filename</li>
     * <li>date archived</li>
     * <li>extension</li>
     * 
     * @param destPath the destination path.
     * @param file The file to be archived.
     * @return The calculated file name.
     */
    public String getDestinationFile(String destPath, Path file) throws IOException {
        
        String filename  = file.getFileName().toString();
        String extension = getFileExtension(filename);
        filename         = stripExtension(filename);
        StringBuilder sb = new StringBuilder();
        
        // Add the host name
        sb.append(getHostName());
        sb.append("_");
        
        // Add the custom prefix if provided
        if (!(getCustomPrefix() == null) && (!getCustomPrefix().isEmpty())) {
            sb.append(getCustomPrefix());
            sb.append("_");
        }
        
        sb.append(filename);
        sb.append("_");
        
        // Append the year and month
        Date date = new Date(getFileDate(file));
        sb.append(getYearMonthDay(date));

        if (getCompression()) {
            extension = ".zip";
        }
        else {
            extension = "." + extension;
        }
        return checkVersioning(destPath, sb.toString(), extension);
    }

    
    /**
     * The destination path will be made of the following components:
     * 
     * <li>base path (value of output.path property)</li>
     * <li>server group</li>
     * <li>application</li>
     * <li>year</li>
     * <li>month</li>
     * 
     * Updated for cross-platform usage.  When working on Windows we found 
     * that when using UNC path names for remote files you must use "/" for
     * the path separator or all sorts of problems ensue.  As such this 
     * method was updated to remove the use of File.separator and just use
     * a forward slash for both Windows/Linux platorms.
     * 
     * @param file The target file that we are going to archive.
     * @return The target destination path.
     * @throws IOException  Thrown if there are problems getting the file 
     * attribute information.
     * @throws OutputException Thrown if we are unable to create the output
     * path directory.
     */
    public String getDestinationPath(Path file) 
            throws IOException, OutputException {
        
        String        method = "getDestinationPath() - ";
        StringBuilder sb     = new StringBuilder();
        
        // Append the output path
        sb.append(getOutputBasePath());
        if (!sb.toString().endsWith(PATH_SEPARATOR)) {
            sb.append(PATH_SEPARATOR);
        }
        sb.append(getServerGroup());
        sb.append(PATH_SEPARATOR);
        sb.append(getApplication());
        sb.append(PATH_SEPARATOR);
        
        // Append the year and month
        Date date = new Date(getFileDate(file));
        sb.append(getYear(date));
        sb.append(PATH_SEPARATOR);
        sb.append(getMonth(date));
        sb.append(PATH_SEPARATOR);
        
        // Now we have the path...make sure it exists.
        if (!checkPath(sb.toString())) {
            throw new OutputException(method 
                    + "Unable to create the full path for the destination. "
                    + "Target destination was [ "
                    + sb.toString()
                    + " ].  See previous messages for more information.");
        }
        return sb.toString();
    }
    
    /**
     * Generate a valid file path containing a random filename on the local 
     * file system.  This is used on Windows platforms where the zip filesystem
     * has a bug writing to remote filesystems.
     *  
     * @return A file path consisting of the Java temp directory with a random 
     * 8-character filename appended to it.
     */
    public String getIntermediatePath() {
        StringBuilder sb = new StringBuilder();
        sb.append(System.getProperty("java.io.tmpdir"));
        if (!sb.toString().endsWith(File.separator)) {
            sb.append(File.separator);
        }
        sb.append(FileUtils.generateUniqueToken(TEMP_FILENAME_LENGTH));
        sb.append(".zip");
        return sb.toString();
    }
    
    /**
     * Accessor method for the required string identifying the output
     * base path.
     * @return The output base path.
     */
    public String getOutputBasePath() {
        return outputPath;
    }
    
    /**
     * Accessor method for the number of days to wait before archiving 
     * a matching file.
     * @return int number of days.
     */
    public int getOutputDelay() {
        return outputDelay;
    }
    
    /**
     * Accessor method for the required string identifying the server group.
     * @return The server group name.
     */
    public String getServerGroup() {
        return serverGroup;
    }
    
    /** 
     * Check to see if the server is windows by determine the type of filesystem.
     * @return True if the file system is not posix-compliant.
     */
    private boolean isWindows() {
        return !FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }
    
    /**
     * Move the target file to a destination that (hopefully) resides on disk.
     * @param file The source file to be moved.
     * @throws OutputException Propogated from internal calls.
     * @throws IOException Thrown if there is an issue accessing the file.
     */
    public void move(Path file) throws IOException, OutputException {
        
        String dest        = getDestinationPath(file);
        String destFile    = getDestinationFile(dest, file);
        String method      = "move() - ";
        Path   destination = Paths.get(dest + destFile);
        
        LOGGER.info(method 
                + "Moving [ "
                + file.toAbsolutePath() 
                + " ] to [ "
                + destination.toAbsolutePath()
                + " ].");
        
        Files.move(file, destination, REPLACE_EXISTING);
        
    }
    
    /**
     * Move the target file to a destination that (hopefully) resides on disk.
     * 
     * @param file The source file to be moved.
     * @throws OutputException Propogated from internal methods.
     * @throws IOException Propagated from the the internal methods that access
     * file attributes.
     */
    public void moveWithCompression(Path file) throws IOException, OutputException {
        
        String     dest        = getDestinationPath(file);
        String     destFile    = getDestinationFile(dest, file);
        String     destination = dest + destFile;
        String     method      = "moveWithCompression() - ";
        FileSystem zipFS       = null; 
        
        LOGGER.info(method 
                + "Moving [ "
                + file.toAbsolutePath() 
                + " ] to [ "
                + destination
                + " ].");
        
        try {
            
            // Create the output Zip filesystem
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");
            
            // Ensure the destination is a valid URI
            File temp = new File(destination);
            URI zipURI = URI.create(String.format("jar:%s", temp.toURI()));
            LOGGER.debug("Attempting to create ZIP URI [ "
                    + zipURI.toString()
                    + "].");
            
            zipFS = FileSystems.newFileSystem(zipURI, env);
            
            // The destination file is relative to the Zip FileSystem
            Path pathInZipFile = zipFS.getPath(file.getFileName().toString());
            LOGGER.debug(method + "Initiating move into ZIP filesystem...");
            
            Files.move(file, pathInZipFile, StandardCopyOption.REPLACE_EXISTING);
            
        }
        catch (IOException ioe) {
            LOGGER.error(method
                    + "An unexpected IOException was encountered while moving "
                    + "file [ "
                    + file.toAbsolutePath().toString()
                    + " ] into zip file [ "
                    + destination
                    + " ].  Error message [ "
                    + ioe.getMessage()
                    + " ].");
        }
        finally {
            if (zipFS != null) { 
                try { zipFS.close(); } catch (Exception e) {}
            }
        }
    }
    
    /**
     * Move the target file to a destination that (hopefully) resides on disk.
     * The Windows ZIP filesystem implementation has a bug.  When creating the ZIP 
     * filesystem on a remote disk, the application throws ReadOnlyFilesystem 
     * exception.  In order to work around this limitation we create the ZIP 
     * archive in a temporary (local) directory, then move the created ZIP file to 
     * the final resting place.
     * 
     * @param file The source file to be moved.
     * @throws OutputException Propogated from internal methods.
     * @throws IOException Propagated from the the internal methods that access
     * file attributes.
     */
    public void moveWithCompressionWin(Path file) throws IOException, OutputException {
        
        String     dest         = getDestinationPath(file);
        String     intermediate = getIntermediatePath();
        String     destFile     = getDestinationFile(dest, file);
        String     destination  = dest + destFile;
        String     method       = "moveWithCompressionWin() - ";
        FileSystem zipFS        = null; 
        
        LOGGER.info(method 
                + "Intermediate move [ "
                + file.toAbsolutePath() 
                + " ] to [ "
                + intermediate
                + " ].");
        
        try {
            
            // Create the output Zip filesystem
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");
           
            // Ensure the destination is a valid URI
            File temp = new File(intermediate);
            URI zipURI = URI.create(String.format("jar:%s", temp.toURI()));
            
            LOGGER.debug("Attempting to create ZIP URI [ "
                    + zipURI.toString()
                    + "].");
            
            zipFS = FileSystems.newFileSystem(zipURI, env);
            
            // The destination file is relative to the Zip FileSystem
            Path pathInZipFile = zipFS.getPath(file.getFileName().toString());
           
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(method + "Moving source file into ZIP archive...");
            }
            
            Files.move(file, pathInZipFile, StandardCopyOption.REPLACE_EXISTING);
            
            if (zipFS != null) { 
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(method 
                            + "File [ "
                            + file.getFileName().toString()
                            + " ] successfully added to intermediate archive [ "
                            + temp.toURI().toString()
                            + " ].  Closing the ZIP filesystem.");
                }
                try { zipFS.close(); } catch (Exception e) {}
            }
            
            File tempDest = new File(destination);
            LOGGER.info(method 
                    + "Moving intermediate archive [ "
                    + temp.toURI().toString() 
                    + " ] to [ "
                    + tempDest.toURI().toString()
                    + " ].");
            
            Files.move(
                    Paths.get(temp.toURI()), 
                    Paths.get(tempDest.toURI()), 
                    StandardCopyOption.REPLACE_EXISTING);
            
        }
        catch (IOException ioe) {
            LOGGER.error(method
                    + "An unexpected IOException was encountered while moving "
                    + "file [ "
                    + file.toAbsolutePath().toString()
                    + " ] into zip file [ "
                    + destination
                    + " ].  Error message [ "
                    + ioe.getMessage()
                    + " ].");
        }
        finally {
            if (zipFS != null) { 
                try { zipFS.close(); } catch (Exception e) {}
            }
        }
    }
    
    
    /**
     * Process the input list of candidate files.
     * 
     * @param candidates List of candidate files for archive.
     */
    public void process(List<Path> candidates) {
        String method = "process() - ";
        if ((candidates != null) && (candidates.size() > 0)) {
            for (Path path : candidates) {
                try {
                    if (archive(path)) {
                        if (getCompression()) {
                            if (isWindows()) {
                                moveWithCompressionWin(path);
                            }
                            else {
                                moveWithCompression(path);
                            }
                        }
                        else {
                            move(path);
                        }
                    }
                }
                catch (OutputException oe) {
                    LOGGER.error(method 
                            + "An unexpected IOException was encountered " 
                            + "while attempting to create the output "
                            + "directory associated with file [ "
                            + path.toAbsolutePath()
                            + " ].  This file will be skipped.  Error "
                            + "message [ "
                            + oe.getMessage()
                            + " ].");
                }
                catch (IOException ioe) {
                    LOGGER.error(method 
                            + "An unexpected IOException was encountered " 
                            + "while retrieving file attributes associated "
                            + "with file [ "
                            + path.toAbsolutePath()
                            + " ].  This file will be skipped.  Error "
                            + "message [ "
                            + ioe.getMessage()
                            + " ].");
                }
            }
        }
    }
    
    /**
     * Mutator method for the optional input string identifying the 
     * application that generated the log file.
     * 
     * @param value The application that generated the log file.
     */
    public void setApplication(String value) {
        String method = "setApplication() - ";
        if ((value != null) && (!value.isEmpty())) {
            application = value.trim().toLowerCase();
        }
        else {
            application = DEFAULT_APPLICATION;
            LOGGER.warn(method
                    + "Input application name is null or empty.  Using [ "
                    + DEFAULT_APPLICATION
                    + " ].");
        }
    }
    
    /**
     * Mutator method for the optional input string identifying whether or not
     * compression should be applied to the output file.
     * 
     * @param value True or false.  The default setting is false.
     */
    public void setCompression(String value) {
        if ((value != null) && (!value.isEmpty())) {
            compress = Boolean.parseBoolean(value);
        }
    }
    
    /**
     * Mutator method for the custom prefix.
     * @param value Custom prefix string.
     */
    public void setCustomPrefix(String value) {
        customPrefix = value;
    }
      
    /**
     * Mutator method for the optional input string identifying the 
     * application that generated the log file.
     * 
     * @param value The application that generated the log file.
     */
    public void setOutputDelay(String value) {
        if ((value == null) || (value.isEmpty())) {
            outputDelay = LogMgmtI.DEFAULT_OUTPUT_DELAY;
        }
        else {
            outputDelay = Integer.parseInt(value);
        }
    }
    
    /**
     * Mutator method for the required output path parameter.  This 
     * parameter will be the starting point for path to the output 
     * log file.
     * 
     * @param value The top-level path where log files are stored.
     * @throws OutputException Thrown if the required output path is not
     * supplied.
     */
    public void setOutputBasePath(String value) throws OutputException {
        
        String method = "setOutputPath() - ";
        
        if ((value != null) && (!value.isEmpty())) {
            outputPath = value.trim().toLowerCase();
        }
        else {
            throw new OutputException(method
                    + "The required property [ "
                    + LogMgmtI.OUTPUT_BASE_PATH 
                    + " ] was not supplied.");
        }
    }
    
    /**
     * Mutator method for the 
     * @param value
     */
    public void setServerGroup(String value) {
        if ((value != null) && (!value.isEmpty())) {
            serverGroup = value.trim().toLowerCase();
        }
        else {
            serverGroup = LogMgmtI.DEFAULT_SERVER_GROUP;
        }
    }
    
    /**
     * Dump the input parameters.
     */
    public String toString() {
        
        String        newLine = System.getProperty("line.separator");
        StringBuilder sb      = new StringBuilder();
        sb.append("Output path  : ");
        sb.append(getOutputBasePath());
        sb.append(newLine);
        sb.append("Application  : ");
        sb.append(getApplication());
        sb.append(newLine);
        sb.append("Server Group : ");
        sb.append(getServerGroup());
        sb.append(newLine);
        sb.append("Compression  : ");
        sb.append(Boolean.toString(getCompression()));
        sb.append(newLine);
        sb.append("Output Delay : ");
        sb.append(Integer.toString(getOutputDelay()));
        sb.append(newLine);    
        
        return sb.toString();
    }
    
}
