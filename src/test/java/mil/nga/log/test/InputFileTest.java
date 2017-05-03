package mil.nga.log.test;

import java.util.Properties;
import org.junit.Test;

import mil.nga.logmgmt.InputFile;
import mil.nga.logmgmt.LogMgmtI;
import mil.nga.logmgmt.exceptions.InputException;

public class InputFileTest {

	/**
	 * Create a Properties file for testing.
	 * 
	 * @return A Properties file containing the required params for 
	 * constructing the InputFile object.
	 */
	public Properties createProperties() {
		String path    = System.getProperty("java.io.tmpdir");
		String pattern = "file.txt";
		Properties props = new Properties();
		props.setProperty(LogMgmtI.INPUT_PATH, path);
		props.setProperty(LogMgmtI.INPUT_PATTERN, pattern);
		return props;
	}
	
	@Test
	public void testConstruction() throws InputException {
		InputFile input = new InputFile(createProperties());
		System.out.println("Input directory [ " + input.getInputPath() + " ].");
	}
}
