import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;

public class Git_Versioning {

	private static final Logger LOGGER = LogManager.getLogger(Git_Versioning.class);

	public static void main(String[] args) {
		File file = null;
		Utils utils = new Utils();
		Git git = null;
		try {

			// creating temporary directory
			file = Files.createTempDirectory(Constants.TEMP_DIR).toFile();
			LOGGER.log(Level.INFO, "Temporary directory created : " + file.getAbsolutePath());
			File localFile = new File(Constants.LOCAL_DIRECTORY_PATH);
			if (localFile.exists()) {
				Path x = Paths.get(Constants.INDEX_FILE_PATH);
//				System.out.println(x);
				Files.deleteIfExists(x);
			} else {
				localFile.mkdirs();
			}
			// collect information from properties file
			GitVO gitVo = utils.collectInfo(Constants.properties);
			// test proxy connection
			if ("true".equalsIgnoreCase(Constants.properties.getProperty("useProxy"))) {
//				utils.testProxyConnection(localFile,gitVo);
				utils.loadProperties(gitVo, Constants.properties);
//				utils.testConnection(localFile, gitVo);
				utils.testConnectionWithCert(gitVo, localFile);
//				utils.testConnection2(gitVo);
			}
			// fetching branches from remote
			utils.fetchFromRemote(localFile, gitVo);
			// setting configurations
			utils.setConfiguration(gitVo);
			// cloning repository
			utils.cloneRepository(gitVo, file);
			// getting parent directory
			String parentDir = utils.getParentString(localFile);
			// creating sample file
			String sampleFile = utils.createSampleFile(parentDir, gitVo);
			// status call
			utils.getStatus();
			// commiting changes in sample file
			utils.commit(sampleFile, gitVo);
			// getting pull from branch
			Git git2 = utils.pull(gitVo);
			if (git2!=null) {
				System.out.println(Constants.PULLED_AND_MERGED);
				LOGGER.log(Level.INFO, Constants.PULLED_AND_MERGED);
			}
			// making push operation on branch
			boolean push = utils.push(gitVo);
			if (push) {
				System.out.println(Constants.PUSH_SUCCESS);
				LOGGER.log(Level.INFO, Constants.PUSH_SUCCESS);
			}
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getStackTrace());
			e.printStackTrace();
		} finally {
//			if (file != null) {
//				try {
//					FileUtils.deleteDirectory(file);
//					System.out.println(Constants.DELETING_DIR);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
			utils.clearProperties(Constants.properties);
		}
	}
}