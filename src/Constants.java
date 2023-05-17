import java.io.IOException;
import java.util.Properties;


public abstract class Constants {

	static Utils utils = new Utils();

	public static final String PROPERTIES_PATH = "Github_SSH.properties";

	public static final Properties properties = utils.readPropertiesFile(PROPERTIES_PATH);

	public static String LOCAL_DIRECTORY_PATH = properties.getProperty("localDirPath");

	public static String INDEX_FILE_PATH = LOCAL_DIRECTORY_PATH + "\\.git\\index.lock";

	public static String SAMPLE_FILE_PATH = LOCAL_DIRECTORY_PATH + "\\SSH.txt";

	public static String TEMP_DIR = "tempVersionControl";

	public static String GIT_REMOTE_ALIAS = "origin";

	public static String REFS_HEADS = "refs/heads/";
	
	public static String REFS_REMOTES = "refs/remotes/"+GIT_REMOTE_ALIAS+"/";

	// messages

	public static String FILE_WRITE_MESSAGE = "File write success";

	public static String PULLED_AND_MERGED = "pulled & merged";

	public static String CLONE = "repository cloned";

	public static String CLONEING = "cloning from repository";

	public static String NO_CHANGE = "No change";

	public static String COMMIT_SUCCESS = "commit success";

	public static String PUSH_SUCCESS = "push success";

	public static String RETRYING = "=== Retrying Git Push ===";

	public static String DELETING_DIR = "Deleting directory";

	public static String NOT_DIRCTORY = " is not directory. localDirPath must be a directory";

	public static String COULD_NOT_CONNECT = "Could not connect using given parameters ";

	public static String BRACHES_FETCH_SUCCESS = "Branches Fetched successfully";
	
	public static String SELECT_BRANCH = "Select branch from branch list: ";
	
	public static String COMMIT_MSG = "Enter commit message: ";
	
	public static String BRANCH_NOT_FOUND = "Could not find branch on remote";

	public static String MENTION_AUTHTYPE = "Please mention authType.";

	public static String INVALID_AUTHTYPE = "Authentication type not supported";

	public static String TERMINATED = "Operation Terminated";

	public static String ENTER_PASSPHRASE = "Enter pass phrase (If any): ";
	
	public static String PARENT_DIR_PATH = "Parent directory path: ";
	
	public static String COMMIT_DIRECTORY = "Git_Versioning/";
	
	public static String SAMPLE_FILE_LOCATION = "Sample file created at location: ";
	
	public static String COMMIT_NAME = "Commit Name: ";
	
	public static String CONFLICT_COMMIT_MSG = "Conflict resolved by overriding local changes with server files using --theirs strategy";
	
	public static final String RES_MSG_GIT_ACCOUNT_CONFG_FAILURE = "Fail to store Git account configurations";
	
	public static String CONFLICT_RESOLVED_MSG = "Conflict resolved using --theirs strategy";
	
	public static String RETRYING_GIT_PULL = "=== Retrying Git Pull ===";
	
	public static String GIT_COMMAND = "Enter git command (if any):";
	
	public static String PROXY_PASSWORD = "Please enter your proxy password: ";
	
	public static String CLEARING = "Clearing... ";
	
	public static String HTTPS = "https";
	
	public static String HTTP = "http";
	
	public static String AUTHENTICATING = "Authenticating.....";
	// Date and Timestamp

	public static String DATE_TIMESTAMP = "dd/MM/yyyy HH:mm:ss";

}
