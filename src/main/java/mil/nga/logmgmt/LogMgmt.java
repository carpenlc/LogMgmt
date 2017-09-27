package mil.nga.logmgmt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import mil.nga.util.Options;
import mil.nga.util.Options.Multiplicity;
import mil.nga.util.Options.Separator;
import mil.nga.logmgmt.exceptions.InputException;
import mil.nga.logmgmt.exceptions.OutputException;
import mil.nga.util.OptionSet;
import mil.nga.util.Options.Multiplicity;
import mil.nga.util.Options.Separator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for Java-based application used to manage disk space on a given
 * server.  The only thing this class does is check to make sure all required 
 * data has been provided as input.  If not, an exception is thrown that will 
 * notify the caller of any issues encountered.
 * 
 * @author L. Craig Carpenter
 */
public class LogMgmt {

    /**
     * Set up the Log4j system for use throughout the class
     */        
    private static final Logger LOGGER = 
            LoggerFactory.getLogger(LogMgmt.class);
    
    /**
     * Constructor used to load the target properties file and invoke the 
     * processing specified by the input properties file.
     * 
     * @param propFile Full path to the property file defining the target log 
     * file parameters. 
     * @param serverGroup Server group used for organizing the output.
     * @param customPrefix Custom string to prepend to output filenames.
     * @param baseOverride Override the base starting directory obtained from
     * the target properties file.
     * @throws InputException Thrown if there are any issues detected with the
     * required program input data.
     * @throws OutputException Thrown if there are problems outputting the data.
     */
    public LogMgmt (
            String propFile, 
            String serverGroup, 
            String customPrefix,
            String baseOverride) 
                    throws InputException, OutputException {
        
        String     method = "Constructor() - ";
        Properties props  = getProperties(propFile);
        
        if (props.isEmpty()) {
            String msg = method 
                    + "Error reading the input properties file [ "
                    + propFile 
                    + " ].  Properties object is empty.";
            LOGGER.error(msg);
            throw new InputException(msg);
        }
        
        InputFile input = new InputFile(props);
        if ((baseOverride != null) && (!baseOverride.isEmpty())) {
            LOGGER.warn(method 
                    + "Client modified search path base from [ "
                    + input.getInputPath()
                    + " ] to [ "
                    + baseOverride
                    + " ].");
            input.setInputPath(baseOverride);
        }
        
        List<Path> candidates = input.getCandidates();
        if ((candidates == null) || (candidates.size() == 0)) {
            LOGGER.info(method
                    + "There are no candidate input files to process.  "
                    + "Exiting.");
        }
        else {
        
            OutputFile output = new OutputFile(
                    props,
                    serverGroup,
                    customPrefix);
            
            output.process(candidates);
            
        }
    }
    
    /**
     * Actually read the properties file from disk.  This is the lame
     * pre-NIO version loading the properties information.
     * 
     * @param filename The full path to the target properties file.
     * @return Populated properties object.  
     */
    private Properties getProperties(String filename) 
            throws InputException {
        
        String      method   = "getProperties() - ";
        Properties  props    = new Properties();
        InputStream is       = null;
        
        try {
            is = new FileInputStream(new File(filename));
            props.load(is);
        }
        catch (FileNotFoundException fnfe) {
            String msg = method  
                    + "Identified properties file [ "
                    + filename 
                    + " ] does not exist!";
            LOGGER.error(msg);
            throw new InputException(msg);
        }
        catch (IOException ioe) {
            String msg = method  
                    + "Unexpected IOException raised while attempting to "
                    + "load the target properties file [ "
                    + filename
                    + " ].  Exception message ["
                    + ioe.getMessage()
                    + " ].";
            LOGGER.error(msg);
            throw new InputException(msg);
        }
        finally {
            if (is != null) {
                try { is.close(); } catch (Exception e) {} 
            }
        }
        
        return props;
    }
    
    /**
     * Simple static method to print the help and usage String information.
     */
    private static void printHelp() {
        System.out.println("");
        System.out.println(LogMgmtI.HELP_STRING);
        System.out.println("");
        System.out.println(LogMgmtI.USAGE_STRING);
    }
    
    /**
     * Driver method used to extract the command line parameters and 
     * initiate processing.
     * @param args Input command line arguments
     * @throws InputException Thrown if the input data does not make 
     * sense.
     * @throws OutputException Thrown if errors are encountered processing
     * the output.
     */
    public static void main (String[] args) 
            throws InputException, OutputException {
        
        String method         = "main() - ";
        String baseOverride   = null;
        String propertiesFile = null;
        String serverGroup    = null;
        String customPrefix   = null;
        
        // Set up the command line options
        Options opt = new Options(args, 0);
        opt.getSet().addOption(
                "propertiesFile", 
                Separator.EQUALS, 
                Multiplicity.ONCE);
        
        opt.getSet().addOption(
                "serverGroup", 
                Separator.EQUALS, 
                Multiplicity.ZERO_OR_ONE);
        
        opt.getSet().addOption(
                "customPrefix", 
                Separator.EQUALS, 
                Multiplicity.ZERO_OR_ONE);
        
        opt.getSet().addOption(
                "baseOverride", 
                Separator.EQUALS, 
                Multiplicity.ZERO_OR_ONE);
        
        opt.getSet().addOption("h", Multiplicity.ZERO_OR_MORE);
        opt.getSet().addOption("help", Multiplicity.ZERO_OR_MORE);
        
        // Make sure the options make sense
        if (!opt.check(true, false)) {
            System.out.println(LogMgmtI.USAGE_STRING);
            System.exit(1);
        }
        
        // See if the user wanted the help message displayed.
        if (opt.getSet().isSet("h") || opt.getSet().isSet("help")) {
            LogMgmt.printHelp();
            System.exit(0);
        }
    
        // Get the name of the properties file
        if (opt.getSet().isSet("propertiesFile")) {
            propertiesFile = 
                    opt.getSet().getOption("propertiesFile").getResultValue(0);
            if ((propertiesFile == null) || (propertiesFile.isEmpty())) {
                LOGGER.error(method 
                        + "ERROR: -propertiesFile was blank or not supplied!");
                LogMgmt.printHelp();
                System.exit(1);
            }
        }
        else {
            LOGGER.error(method 
                    + "ERROR: -propertiesFile option must be supplied!");
            LogMgmt.printHelp();
            System.exit(1);
        }
        
        // Get the optional serverGroup parameter.
        if (opt.getSet().isSet("serverGroup")) {
            serverGroup = 
                    opt.getSet().getOption("serverGroup").getResultValue(0);
        }
        if ((serverGroup == null) || (serverGroup.isEmpty())) {
            serverGroup = LogMgmtI.DEFAULT_SERVER_GROUP;
        }
        
        // Get the optional serverGroup parameter.
        if (opt.getSet().isSet("customPrefix")) {
            customPrefix = 
                    opt.getSet().getOption("customPrefix").getResultValue(0);
        }
        
        // Get the optional baseOverride parameter.
        if (opt.getSet().isSet("baseOverride")) {
            baseOverride = 
                    opt.getSet().getOption("baseOverride").getResultValue(0);
        }
        
        LOGGER.info(method 
                + "Invoking LogMgmt with properties file [ "
                + propertiesFile 
                + " ] and server group [ "
                + serverGroup 
                + " ].");
        
        new LogMgmt(propertiesFile, serverGroup, customPrefix, baseOverride);
    }
    
}
