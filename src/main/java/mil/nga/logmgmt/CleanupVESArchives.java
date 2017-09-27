package mil.nga.logmgmt;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.nga.logmgmt.exceptions.InputException;
import mil.nga.logmgmt.exceptions.OutputException;
import mil.nga.util.Options;
import mil.nga.util.Options.Multiplicity;
import mil.nga.util.Options.Separator;

/**
 * Very specific tool for use with the Axway Valicert application.  Axway 
 * Valicert downloads CRLs from target distribution points according to a 
 * specific schedule.  As the CRLs are processed the application saves all 
 * of the previously downloaded CRLs in an archive directory.  This tool 
 * was written to clean up these archived CRLs.
 * 
 * (Note: Admittedly, it is probably much easier to write a script to do this 
 * but I had most of the logic already developed so this was just 
 * cut-and-paste.)
 * 
 * @author L. Craig Carpenter
 */
public class CleanupVESArchives {

    /**
     * Set up the Log4j system for use throughout the class
     */        
    private static final Logger LOGGER = 
            LoggerFactory.getLogger(CleanupVESArchives.class);
    
    /**
     * This method will return the number of elements to extract from the
     * input list of CRLs.
     * 
     * @param crls The total list of archived CRLs.
     * @return The number of elements in the input list that should be 
     * extracted for copy.  0 is returned if there are none to be deleted.
     */
    private int getIndex(List<Path> crls) {
        int numElements = 0;
        if ((crls != null) && (!crls.isEmpty())) {
            if (crls.size() > CleanupVESArchivesI.ARCHIVES_TO_SAVE) {
                numElements = crls.size() - 
                        CleanupVESArchivesI.ARCHIVES_TO_SAVE;
            }
        }
        return numElements;
    }
    
    /**
     * Constructor used to load the target properties file and invoke the 
     * processing specified by the input properties file.
     * 
     * @param searchLoc Starting location for the search operations. 
     * @param test If true, files will not be deleted.
     * @throws InputException Thrown if there are any issues detected with the
     * required program input data.
     * @throws OutputException Thrown if there are problems outputting the data.
     */
    public CleanupVESArchives (String searchLoc, boolean test) 
            throws InputException {
        
        String      method    = "constructor() - ";
        VESArchives archives  = new VESArchives(searchLoc);
        long        sizeAccum = 0;
        List<Path> candidates = archives.getCandidates();
        
        if ((candidates == null) || (candidates.isEmpty())) {
            LOGGER.warn(method
                    + "There are no candidate input archive directories to "
                    + "further process.  Exiting.");
        }
        else {
            
            for (Path directory : candidates) {
                
                List<Path> archivedCRLs = archives.getAllCRLs(directory);
                if ((archivedCRLs == null) || (archivedCRLs.isEmpty())) {
                    LOGGER.info(method 
                            + "There are no potential archived CRLs for "
                            + "removal in [ "
                            + directory.toString()
                            + " ].");
                }
                else {
                    int endIndex = getIndex(archivedCRLs);
                    if (endIndex > 0) {
                        List<Path> crlsToDelete = archivedCRLs.subList(0, endIndex);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(method 
                                    + "There were [ "
                                    + archivedCRLs.size()
                                    + " ] archived CRLs in the target "
                                    + "directory, deleting [ " 
                                    + crlsToDelete.size()
                                    + " ].");
                        }
                        sizeAccum += archives.delete(crlsToDelete, test);
                    }
                    else {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(method 
                                    + "There are no CRLs in [ "
                                    + directory.toString() 
                                    + " ] to delete.");
                        }
                    }
                }
            }
            LOGGER.info(method
                    + "Recovered [ "
                    + sizeAccum
                    + " ] bytes.");
        }    
    }

    /**
     * Simple static method to print the help and usage String information.
     */
    private static void printHelp() {
        System.out.println("");
        System.out.println(CleanupVESArchivesI.HELP_STRING);
        System.out.println("");
        System.out.println(CleanupVESArchivesI.USAGE_STRING);
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
        
        String  method         = "main() - ";
        String  searchLoc      = null;
        boolean testingOnly    = false;
        
        // Set up the command line options
        Options opt = new Options(args, 0);
        opt.getSet().addOption(
                "searchLoc", 
                Separator.EQUALS, 
                Multiplicity.ONCE);
        
        opt.getSet().addOption("test", Multiplicity.ZERO_OR_MORE);
        opt.getSet().addOption("h", Multiplicity.ZERO_OR_MORE);
        opt.getSet().addOption("help", Multiplicity.ZERO_OR_MORE);
        
        // Make sure the options make sense
        if (!opt.check(true, false)) {
            System.out.println(CleanupVESArchivesI.USAGE_STRING);
            System.exit(1);
        }
        
        // See if the user wanted the help message displayed.
        if (opt.getSet().isSet("h") || opt.getSet().isSet("help")) {
            CleanupVESArchives.printHelp();
            System.exit(0);
        }
        
        // See if the user wanted the help message displayed.
        if (opt.getSet().isSet("test")) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(method
                        + "Application to be run in test mode.");
            }
            testingOnly = true;
        }
        
        // Get the name of the properties file
        if (opt.getSet().isSet("searchLoc")) {
            searchLoc = opt.getSet().getOption("searchLoc").getResultValue(0);
            if ((searchLoc == null) || (searchLoc.isEmpty())) {
                LOGGER.error(method 
                        + "ERROR: -searchLoc was blank or not supplied!");
                CleanupVESArchives.printHelp();
                System.exit(1);
            }
        }
        else {
            LOGGER.error(method 
                    + "ERROR: -searchLoc option must be supplied!");
            CleanupVESArchives.printHelp();
            System.exit(1);
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Search from top-level directory [ "
                    + searchLoc
                    + " ].");
        }
        
        // All must be good, start execution.
        new CleanupVESArchives(searchLoc, testingOnly);
    }
}
