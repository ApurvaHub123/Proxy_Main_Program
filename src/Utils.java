import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidMergeHeadsException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.ServiceUnavailableException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class Utils {
	private static final Logger LOGGER = LogManager.getLogger(Git_Versioning.class);
	GitVO gitVo = new GitVO();
	int pushRetryCount = 1;
	int maxRetry = 3;

	public Properties readPropertiesFile(String file) {

		FileInputStream fis = null;
		Properties prop = null;
		try {
			fis = new FileInputStream(file);
			prop = new Properties();
			prop.load(fis);
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try {
				fis.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return prop;
	}

	public void fetchFromRemote(File localFile, GitVO gitVo) {
		Git tempgit = null;
		Repository repo = null;
		File file = null;
		Scanner sc = new Scanner(System.in);
		String branchName = "";
		try {

			file = Files.createTempDirectory("tempversioncontrol").toFile();

			tempgit = Git.init().setDirectory(file).call();

			tempgit.remoteAdd().setName(Constants.GIT_REMOTE_ALIAS).setUri(new URIish(gitVo.getRemoteUrl())).call();

			List<Ref> branches;

			if (tempgit != null) {
				repo = tempgit.getRepository();
				if (repo != null) {
					StoredConfig config = repo.getConfig();
					config.setString("remote", Constants.GIT_REMOTE_ALIAS, "url", gitVo.getRemoteUrl());
					config.setBoolean("https", gitVo.getRemoteUrl(), "sslVerify", false);
					config.save();
				}
			}

			switch (gitVo.getAuthType()) {

			case "SSH":

				tempgit.fetch().setTransportConfigCallback(new SshTransportConfigCallback(gitVo.getPassPhrase(),
						gitVo.getSshPrivateKey(), gitVo.getSshPublicKey())).call();
				break;

			case "UNP":

				tempgit.fetch().setCredentialsProvider(
						new CustomSSLCredentialProvider(gitVo.getUsername(), gitVo.getPassword())).call();
				break;
			}

			branches = tempgit.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
			List<String> branchList = new ArrayList<>();
			for (Ref branch : branches) {
				branchList.add(branch.getName().substring(branch.getName().lastIndexOf('/') + 1));
			}
			System.out.println("Total branches found: " + branchList.size());
			System.out.println("Branch List:");
			branchList.stream().forEach(System.out::println);
//			System.out.println(Constants.BRACHES_FETCH_SUCCESS);
			LOGGER.log(Level.INFO, Constants.BRACHES_FETCH_SUCCESS);

			System.out.println(Constants.SELECT_BRANCH);
			branchName = sc.next();

			if (!branchList.contains(branchName)) {
				System.out.println(Constants.BRANCH_NOT_FOUND);
				LOGGER.log(Level.ERROR, Constants.BRANCH_NOT_FOUND);
				System.out.println(Constants.TERMINATED);
				LOGGER.log(Level.ERROR, Constants.TERMINATED);
				clearProperties(Constants.properties);
				System.exit(-1);
			} else {
				gitVo.setBranch(branchName);
			}

		} catch (TransportException e) {
			System.out.println(Constants.COULD_NOT_CONNECT);
			LOGGER.log(Level.ERROR, Constants.COULD_NOT_CONNECT);
			LOGGER.log(Level.ERROR, e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage());
			e.printStackTrace();
		} finally {
			if (tempgit != null) {
				tempgit.close();
			}

			if (repo != null) {
				repo.close();
			}
			if (file != null) {
				try {
					FileUtils.deleteDirectory(file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void cloneRepository(GitVO gitVo, File file) {

		System.out.println(Constants.CLONEING);
		LOGGER.log(Level.INFO, Constants.CLONEING);

		Git gitTemp = null;

		try {
			switch (gitVo.getAuthType()) {
			case "UNP":

				gitTemp = Git.cloneRepository().setBranch(gitVo.getBranch()).setURI(gitVo.getRemoteUrl())
						.setDirectory(file)
						.setCredentialsProvider(
								new CustomSSLCredentialProvider(gitVo.getUsername(), gitVo.getPassword()))
						.setProgressMonitor(new SimpleProgressMonitor()).call();

				break;

			case "SSH":

				gitTemp = Git.cloneRepository().setBranch(gitVo.getBranch())
						.setTransportConfigCallback(new SshTransportConfigCallback(gitVo.getPassPhrase(),
								gitVo.getSshPrivateKey(), gitVo.getSshPublicKey()))
						.setURI(gitVo.getRemoteUrl()).setDirectory(file).call();

				break;
			}

			if (gitTemp != null) {
				System.out.println(Constants.CLONE);
				LOGGER.log(Level.INFO, Constants.CLONE);
			}
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getStackTrace());
			e.printStackTrace();
		}
//		finally {
//			if (file != null) {
//				try {
//					FileUtils.deleteDirectory(file);
//					System.out.println(Constants.DELETING_DIR);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}
	}

	public GitVO collectInfo(Properties properties) {
		// TODO Auto-generated method stub

		GitVO gitVo = new GitVO();
		String enteredPassPhraseStr = null;

		if (properties.getProperty("authType") == null || "".equalsIgnoreCase(properties.getProperty("authType"))
				|| "null".equalsIgnoreCase(properties.getProperty("authType"))) {
			System.out.println(Constants.MENTION_AUTHTYPE);
			LOGGER.log(Level.INFO, Constants.MENTION_AUTHTYPE);
			System.out.println(Constants.TERMINATED);
			LOGGER.log(Level.INFO, Constants.TERMINATED);
			System.exit(-1);
		} else {
			gitVo.setAuthType(properties.getProperty("authType"));
		}

		gitVo.setProvider(properties.getProperty("provider"));
		gitVo.setRemoteUrl(properties.getProperty("remoteUrl").trim());
		gitVo.setLocalDirPath(properties.getProperty("localDirPath"));

		if ("true".equalsIgnoreCase(properties.getProperty("useProxy"))) {
			gitVo.setProxyPort(Integer.parseInt(properties.getProperty("proxyPort")));
			gitVo.setProxyHost(properties.getProperty("proxyHost"));
			gitVo.setProxyUsername(properties.getProperty("proxyUsername"));
		}

		switch (gitVo.getAuthType()) {
		case "SSH":
			gitVo.setSshPrivateKey(properties.getProperty("sshPrivateKey"));
			try {
				Console console = System.console();
				char[] enteredPassPhrase = null;
				enteredPassPhrase = console.readPassword("Please enter your passphrase: ");
				enteredPassPhraseStr = String.valueOf(enteredPassPhrase);
			} catch (NullPointerException np) {
				Scanner sc = new Scanner(System.in);
				System.out.println("Please enter your passphrase: ");
				enteredPassPhraseStr = sc.nextLine();
			}
			gitVo.setPassPhrase(enteredPassPhraseStr);
//			System.out.println(gitVo.getPassPhrase());

			String privateLine;
			String privateKey = "";

			try {
				FileReader privateReader = new FileReader(gitVo.getSshPrivateKey());
				BufferedReader br = new BufferedReader(privateReader);
//			System.out.println("privateKey: ");
				while ((privateLine = br.readLine()) != null) {
					if (privateLine.isEmpty()) {
						continue;
					}
//				System.out.println(privateLine);
					privateKey += privateLine;
				}
				gitVo.setSshPublicKey(privateKey);
//			System.out.println(gitVo.getPrivateKey());

			} catch (Exception e) {
				LOGGER.log(Level.ERROR, e.getStackTrace());
//				e.printStackTrace();
			}
			break;

		case "UNP":
			gitVo.setUsername(properties.getProperty("username"));
			try {
				Console console = System.console();
				char[] enteredPassPhrase = null;
				enteredPassPhrase = console.readPassword("Please enter your password: ");
				enteredPassPhraseStr = String.valueOf(enteredPassPhrase);
			} catch (NullPointerException np) {
				Scanner sc = new Scanner(System.in);
				System.out.println("Please enter your password: ");
				enteredPassPhraseStr = sc.nextLine();
			}
			gitVo.setPassword(enteredPassPhraseStr);
			break;
		default:

			System.out.println(Constants.INVALID_AUTHTYPE);
			LOGGER.log(Level.INFO, Constants.INVALID_AUTHTYPE);
			break;
		}

		if (enteredPassPhraseStr.length() == 0) {
			System.out.println("Password not entered");
		}
		return gitVo;

	}

	public String getParentString(File localFile) {

		Git git = null;
		Repository repo;
		try {
			git = Git.open(localFile);
			if (git != null) {
				repo = git.getRepository();
				System.out.println(Constants.PARENT_DIR_PATH + repo.getDirectory().getParent());
				return repo.getDirectory().getParent();
			}
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getStackTrace());
			e.printStackTrace();
		}
		return null;
	}

	public String createSampleFile(String parentDir, GitVO gitVo) {

		if (parentDir == null) {
			return null;
		}

		String diff = "";

		File f = new File(Constants.SAMPLE_FILE_PATH);
		try {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern(Constants.DATE_TIMESTAMP);
			LocalDateTime now = LocalDateTime.now();
			if (f.exists()) {
//				diff = readFile(f);
				f.delete();
			} else {
				if (f.isFile())
					f.createNewFile();
				else if (f.isDirectory())
					f.mkdir();
			}
			try (FileWriter fw = new FileWriter(f)) {
				fw.write("Uploaded file to " + gitVo.getProvider() + " during trial....." + "\n");
				fw.write("\n");
				fw.write("Commit Date: " + dtf.format(now));
				fw.flush();
			}
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getStackTrace());
			e.printStackTrace();
		}
		System.out.println(Constants.FILE_WRITE_MESSAGE);
		LOGGER.log(Level.INFO, Constants.FILE_WRITE_MESSAGE);
		System.out.println(Constants.SAMPLE_FILE_LOCATION + f.getAbsolutePath());
		LOGGER.log(Level.INFO, Constants.SAMPLE_FILE_LOCATION + f.getAbsolutePath());
//		
//		System.out.println("Comparing file content.....");
//		String current = readFile(f);
//		System.out.println(current);

//		StringUtils.difference(diff, current);

		return f.getName();

	}

	private String readFile(File f) {
		String diff = "";
		String diffLine = "";

		try (FileReader FileReader = new FileReader(f); BufferedReader br = new BufferedReader(FileReader);) {

			while ((diffLine = br.readLine()) != null) {
				if (diffLine.isEmpty()) {
					continue;
				}
//				System.out.println(diffLine);
				diff += diffLine;
			}
			System.out.println(diff);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return diff;
	}

	public void commit(String sampleFile, GitVO gitVo) {

		Git git = null;
		String commitName = "";
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(Constants.DATE_TIMESTAMP);
		LocalDateTime now = LocalDateTime.now();
		Scanner sc = new Scanner(System.in);
		String commitMsg = "";
		File file = new File(Constants.LOCAL_DIRECTORY_PATH);
		try {
//			System.out.println(Paths.get(Constants.INDEX_FILE_PATH));
			Files.deleteIfExists(Paths.get(Constants.INDEX_FILE_PATH));
			git = Git.open(file);
			System.out.println(git.status().call().getModified());
			LOGGER.log(Level.INFO, git.status().call().getModified());
			git.add().addFilepattern(".").call();
			git.tag();
			sampleFile = Constants.COMMIT_DIRECTORY + sampleFile;
			StatusCommand statusCmd = git.status().addPath(sampleFile);
			Status gitStatus = statusCmd.call();
			if (gitStatus.isClean()) {
				System.out.println(Constants.NO_CHANGE);
				LOGGER.log(Level.INFO, Constants.NO_CHANGE);
			}
			CommitCommand commitCmd = git.commit();
			commitCmd.setAuthor(System.getenv("USERNAME"), "");
			System.out.println(Constants.COMMIT_MSG);
			commitMsg = sc.nextLine();
			if (commitMsg != null && !"".equalsIgnoreCase(commitMsg)) {
				gitVo.setCommitMessage(commitMsg + "using " + gitVo.getAuthType());
				commitCmd.setMessage(gitVo.getCommitMessage());
			} else if (commitMsg.isEmpty()) {
				commitCmd.setMessage("Commit on " + dtf.format(now) + " " + "using " + gitVo.getAuthType());
			}
			System.out.println("Commit message: " + commitCmd.getMessage());
			LOGGER.log(Level.INFO, "Commit message: " + commitCmd.getMessage());
			commitName = commitCmd.call().getName();

		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getStackTrace());
			e.printStackTrace();
		}
		finally {
			if (git != null) {
				git.close();
			}
		}

		System.out.println(Constants.COMMIT_NAME + commitName);
		LOGGER.log(Level.INFO, Constants.COMMIT_NAME + commitName);
		System.out.println(Constants.COMMIT_SUCCESS);
		LOGGER.log(Level.INFO, Constants.COMMIT_SUCCESS);

	}

	public Git pull(GitVO gitVo) {

		Git git = null;
		File file = new File(Constants.LOCAL_DIRECTORY_PATH);
		Repository repo = null;
		int pushRetryCount = 1;
		int pullRetryCount = 1;
		int maxRetry = 3;

		try {
//			git = Git.init().setDirectory(localFile).call();

			fetch(gitVo);
			Files.deleteIfExists(Paths.get(Constants.INDEX_FILE_PATH));

			git = Git.open(file);
			repo = git.getRepository();
			String branch = "master".equalsIgnoreCase(repo.getBranch()) ? "main" : repo.getBranch();

			MergeResult mergeResult = git.merge().setStrategy(MergeStrategy.RECURSIVE)
					.include(repo.findRef(Constants.REFS_REMOTES + branch)).setCommit(true).call();

			System.out.println("MERGE_RESULT = {" + mergeResult + "}");
			LOGGER.log(Level.INFO, "MERGE_RESULT = {" + mergeResult + "}");

			if (mergeResult.getMergeStatus() == MergeResult.MergeStatus.CONFLICTING
					|| mergeResult.getMergeStatus() == MergeResult.MergeStatus.FAILED) {

				mergeResult.getMergeStatus();
				git.checkout().setAllPaths(true).setStage(CheckoutCommand.Stage.THEIRS).setCreateBranch(false)
						.setForced(true).call();
				git.add().addFilepattern(".").call();
				git.commit().setMessage(Constants.CONFLICT_COMMIT_MSG).setAll(true).call();
				return git;
			}
			return git;
		} catch (NoHeadException | UnmergedPathsException | NoFilepatternException | IOException
				| ServiceUnavailableException | AbortedByHookException | RefNotFoundException
				| RefAlreadyExistsException | InvalidRefNameException | InvalidMergeHeadsException
				| ConcurrentRefUpdateException | NoMessageException ex) {
			LOGGER.log(Level.ERROR, ex.getStackTrace());
			throw new RuntimeException(ex);
		} catch (CheckoutConflictException ex) {
			LOGGER.log(Level.ERROR, ex.getMessage());
			try {
				assert git != null;
				git.checkout().setAllPaths(true).setStage(CheckoutCommand.Stage.THEIRS).setCreateBranch(false)
						.setForced(true).call();
				git.add().addFilepattern(".").call();
				git.commit().setMessage(Constants.CONFLICT_RESOLVED_MSG).setAll(true).call();
				return git;
			} catch (Exception ce) {
				LOGGER.log(Level.ERROR, ce.getStackTrace());
				ex.printStackTrace();
			}
		} catch (GitAPIException ex) {
			if (pullRetryCount <= maxRetry) {
				LOGGER.log(Level.ERROR, ex.getStackTrace());
				System.out.println(Constants.RETRYING_GIT_PULL);
				LOGGER.log(Level.INFO, Constants.RETRYING_GIT_PULL);
				pullRetryCount++;
				return pull(gitVo);
			} else {
				pullRetryCount = 1;
				LOGGER.log(Level.ERROR, ex.getStackTrace());
				ex.printStackTrace();
			}
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getStackTrace());
			e.printStackTrace();
		} finally {
//			if (git != null) {
//				git.close();
//			}
			if (repo != null) {
				repo.close();
			}

		}
		return git;

	}

	private void fetch(GitVO gitVo) throws Exception {
		// TODO Auto-generated method stub
		Git git = null;
		File file = null;
		try {

			file = new File(Constants.LOCAL_DIRECTORY_PATH);

			git = Git.init().setDirectory(file).call();

			/**
			 * Sometimes when git operation fails index.lock doesn't get delete. so deleting
			 * this before accessing the git repo.
			 */
			Files.deleteIfExists(Paths.get(Constants.INDEX_FILE_PATH));

			git.remoteAdd().setName(Constants.GIT_REMOTE_ALIAS).setUri(new URIish(gitVo.getRemoteUrl())).call();

			switch (gitVo.getAuthType()) {

			case "SSH":
				git.fetch().setTransportConfigCallback(new SshTransportConfigCallback(gitVo.getPassPhrase(),
						gitVo.getSshPrivateKey(), gitVo.getSshPublicKey())).call();
				break;
			case "UNP":
				git.fetch().setCredentialsProvider(
						new CustomSSLCredentialProvider(gitVo.getUsername(), gitVo.getPassword())).call();
				break;
			}

		} catch (IOException | GitAPIException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			throw new Exception(e.getMessage());
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		} finally {
			if (git != null) {
				git.close();
			}
		}
	}

	public boolean push(GitVO gitVo) {
		System.out.println("pushing ....");
		Git git2 = null;
		boolean result = false;
		File file = new File(Constants.LOCAL_DIRECTORY_PATH);
		try {
			Files.deleteIfExists(Paths.get(Constants.INDEX_FILE_PATH));

			git2 = Git.open(file);
			Iterable<PushResult> pushResult = null;

			switch (gitVo.getAuthType()) {

			case "UNP":
				pushResult = git2.push().setRemote(Constants.GIT_REMOTE_ALIAS).add(gitVo.getBranch())
						.setCredentialsProvider(
								new CustomSSLCredentialProvider(gitVo.getUsername(), gitVo.getPassword()))
						.call();
				break;

			case "SSH":
				pushResult = git2.push().setRemote(Constants.GIT_REMOTE_ALIAS)
						.setTransportConfigCallback(new SshTransportConfigCallback(gitVo.getPassPhrase(),
								gitVo.getSshPrivateKey(), gitVo.getSshPublicKey()))
						.call();

				break;

			}

			for (PushResult pr : pushResult) {
				LOGGER.log(Level.INFO, "PushResult: " + pr.toString());
				System.out.println("PushResult: " + pr.toString());
			}
			result = true;
		} catch (GitAPIException e) {
			LOGGER.log(Level.ERROR, e.getStackTrace());
			e.printStackTrace();
			if (pushRetryCount <= maxRetry) {
				System.out.println(Constants.RETRYING);
				pushRetryCount++;
				return push(gitVo);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
//			if (git2 != null) {
//				git2.close();
//			}
		}
		return result;
	}

	public void setConfiguration(GitVO gitVo) throws Exception {

		Git git = null;
		Repository repo = null;
		try {
			File file = new File(Constants.LOCAL_DIRECTORY_PATH);
			Files.deleteIfExists(Paths.get(Constants.INDEX_FILE_PATH));
			git = Git.open(file);
			if (git != null) {
				repo = git.getRepository();
				if (repo != null) {
					StoredConfig config = repo.getConfig();
					config.setString("remote", Constants.GIT_REMOTE_ALIAS, "url", gitVo.getRemoteUrl());
					config.setBoolean("http", gitVo.getRemoteUrl(), "sslVerify", false);
					config.save();
					System.out.println("gitVo.getBranch: " + gitVo.getBranch());
					checkoutBranchIfChanged(git, repo.getBranch(), gitVo.getBranch());
				}
			}

		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getStackTrace());
			e.printStackTrace();
		}

	}

	private void checkoutBranchIfChanged(Git git, String currentBranch, String newBranch) throws Exception {

		Repository repo = null;
		String newBranchName = "main".equalsIgnoreCase(newBranch) ? "master" : newBranch;
		try {
			LOGGER.info("Old branch: {}, New Branch: {}", currentBranch, newBranchName);
			if (!currentBranch.equals(newBranchName)) {
				LOGGER.info("Git Branch changed. Checking out new branch");
				List<Ref> branches = git.branchList().setListMode(ListMode.ALL).call();
				if (branches.stream().anyMatch(br -> br.getName().equals(Constants.REFS_HEADS + newBranchName))) {
					git.checkout().setForceRefUpdate(true).setForced(true).setName(newBranchName).call();
					repo = git.getRepository();

					/**
					 * This will merge the old branch to new branch and override the files with old
					 * branch if any conflict occurs
					 */
					MergeResult mergeResult = git.merge().setStrategy(MergeStrategy.RECURSIVE)
							.include(repo.findRef(Constants.REFS_HEADS + currentBranch)).setCommit(true).call();

					if (mergeResult.getMergeStatus().isSuccessful()) {
						LOGGER.info("Branch : {} checkout & merged", newBranchName);
					} else if (mergeResult.getMergeStatus() == MergeStatus.CONFLICTING
							|| mergeResult.getMergeStatus() == MergeStatus.FAILED) {
						/**
						 * This will merge the old branch to new branch and override the files with old
						 * branch if conflict occurs
						 */
						LOGGER.info("Merged {} while merging branch - checkout using --theirs stratergy",
								mergeResult.getMergeStatus());
						git.checkout().setAllPaths(true).setStage(Stage.THEIRS).setCreateBranch(false).setForced(true)
								.call();
						git.add().addFilepattern(".").call();
						git.commit().setMessage(
								"Conflict resolved by overriding new branch changes with old branch files using --theirs stratergy")
								.setAll(true).call();
					} else {
						throw new Exception(Constants.RES_MSG_GIT_ACCOUNT_CONFG_FAILURE + ":: Error : "
								+ mergeResult.getMergeStatus());
					}
				} else {
					git.checkout().setCreateBranch(true).setForceRefUpdate(true).setForced(true).setName(newBranchName)
							.call();
					LOGGER.info("Branch : {} created", newBranchName);
				}
			}
		} catch (RefAlreadyExistsException e) {
			LOGGER.error(e.getMessage(), e);
			try {
				git.checkout().setForceRefUpdate(true).setForced(true).setName(newBranchName).call();
				git.reset().setRef(Constants.REFS_HEADS + currentBranch).setMode(ResetType.HARD).call();
			} catch (CheckoutConflictException e1) {
				throw new Exception(Constants.RES_MSG_GIT_ACCOUNT_CONFG_FAILURE + ":: Error : " + e1.getMessage(), e1);
			} catch (GitAPIException e2) {
				throw new Exception(Constants.RES_MSG_GIT_ACCOUNT_CONFG_FAILURE + ":: Error : " + e2.getMessage(), e2);
			}
		} catch (GitAPIException | IOException e) {
			throw new Exception(Constants.RES_MSG_GIT_ACCOUNT_CONFG_FAILURE + ":: Error : " + e.getMessage(), e);
		} finally {
			if (repo != null) {
				repo.close();
			}
		}

	}

//	public void testProxyConnection(File localFile, GitVO gitVo) throws IOException, IllegalStateException, GitAPIException {
//		
//		String enteredPassPhraseStr = "";
//		HttpURLConnection httpURLConnection = null;
//		
//		try {
//			Files.deleteIfExists(Paths.get(Constants.INDEX_FILE_PATH));
//		try {
//			Console console = System.console();
//			char[] enteredPassPhrase = null;
//			enteredPassPhrase = console.readPassword("Please enter your proxy password: ");
//			enteredPassPhraseStr = String.valueOf(enteredPassPhrase);
//		}catch (NullPointerException np) {
//			Scanner sc = new Scanner(System.in);
//			System.out.println("Please enter your proxy password: ");
//			enteredPassPhraseStr = sc.nextLine();
//		}
//		
//		System.setProperty("https.proxyHost", gitVo.getProxyHost());
//        System.setProperty("https.proxyPort", String.valueOf(gitVo.getProxyPort()));
////        System.setProperty("https.proxyUser", gitVo.getProxyUsername());
////        System.setProperty("https.proxyPassword", enteredPassPhraseStr);
//        
//        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(gitVo.getProxyHost(), gitVo.getProxyPort()));
//        try {
//        	URL url  = new URL(gitVo.getRemoteUrl().toString());
//			httpURLConnection = (HttpURLConnection) url.openConnection(proxy);
//			httpURLConnection.connect();
//			InputStream in = url.openStream();
//			System.out.println("is connection proxy: "+httpURLConnection.usingProxy());
//			System.out.println("Is Using proxy: "+httpURLConnection.getResponseMessage());
//			LOGGER.log(Level.INFO,"Is Using proxy: "+httpURLConnection.usingProxy()                    );
//		} catch (IOException e) {
//			System.out.println("Connection not proxy");
//			e.printStackTrace();
//		}
//        
//        Authenticator.setDefault(new ProxyAuthenticator(gitVo.getProxyUsername(), enteredPassPhraseStr));
//        
//		}catch(Exception e) {
//			e.printStackTrace();
//		}
//        
//	}

	public void testConnection(File localFile, GitVO gitVo) {

		HttpsURLConnection connection = null;
		Git tempgit = null;

		try {
			tempgit = Git.init().setDirectory(localFile).call();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Authenticator.setDefault(new ProxyAuthenticator(gitVo.getProxyUsername(), gitVo.getProxyPassword()));
		ProxySelector.setDefault(new ProxySelector() {
			final ProxySelector delegate = ProxySelector.getDefault();

			@Override
			public List<Proxy> select(URI uri) {

				// Filter the URIs to be proxied
				if (uri.toString().contains(gitVo.getProvider().toLowerCase())
						&& uri.toString().contains(Constants.HTTPS)) {
					System.out.println(Constants.AUTHENTICATING + uri);
					return Arrays.asList(new Proxy(Type.HTTP,
							InetSocketAddress.createUnresolved(gitVo.getProxyHost(), gitVo.getProxyPort())));
				}
				if (uri.toString().contains(gitVo.getProvider().toLowerCase())
						&& uri.toString().contains(Constants.HTTP)) {
					System.out.println(Constants.AUTHENTICATING + uri);
					return Arrays.asList(new Proxy(Type.HTTP,
							InetSocketAddress.createUnresolved(gitVo.getProxyHost(), gitVo.getProxyPort())));
				}
				// revert to the default behaviour
				return delegate == null ? Arrays.asList(Proxy.NO_PROXY) : delegate.select(uri);
			}

			@Override
			public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
				if (uri == null || sa == null || ioe == null) {
					throw new IllegalArgumentException("Arguments can't be null.");
				}
			}
		});

		try {
			URL url = new URL(gitVo.getRemoteUrl());
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(gitVo.getProxyHost(), gitVo.getProxyPort());
			InputStream in = sslsocket.getInputStream();
			OutputStream out = sslsocket.getOutputStream();
			try {
				SSLContext sc = SSLContext.getInstance("TLSv1.3");
				// Create empty HostnameVerifier
				HostnameVerifier hv = new HostnameVerifier() {
					public boolean verify(String urlHostName, SSLSession session) {
						return true;
					}
				};
				// Create a trust manager that does not validate certificate chains
				TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
					}

					public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
					}
				} };

				sc.init(null, trustAllCerts, new java.security.SecureRandom());
				SSLSocketFactory sslSocketFactory = sc.getSocketFactory();

				HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
				HttpsURLConnection.setDefaultHostnameVerifier(hv);

//				connection = (HttpsURLConnection) new URL(url.toString()).openConnection();

			} catch (Exception e) {
				e.printStackTrace();
			}

			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(gitVo.getProxyHost(), gitVo.getProxyPort()));
//			URL url = new URL("https://www.youtube.com/");
			CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
			connection = (HttpsURLConnection) url.openConnection(proxy);
			String encoded = Base64.getEncoder()
					.encodeToString((gitVo.getProxyUsername() + ":" + gitVo.getProxyPassword()).getBytes());
			connection.setRequestProperty("Proxy-Authorization", "Basic " + encoded);
			connection.setRequestProperty("Accept", "text/html");
//			connection.setRequestMethod("HEAD");
//			connection.setDoInput(true);
//			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
//			HttpURLConnection.setFollowRedirects(false);
			connection.setFollowRedirects(Boolean.FALSE);
			connection.setConnectTimeout(100000);
			connection.setSSLSocketFactory(sslsocketfactory);
//			connection.setAllowUserInteraction(true);
//			connection.setReadTimeout(1000);
			connection.connect();
			System.out.println(connection.getPermission());
			System.out.println("Is proxy: " + connection.usingProxy());
			LOGGER.log(Level.INFO, "Is Using proxy: " + connection.usingProxy());
			System.out.println(connection.getResponseCode() + "-" + connection.getResponseMessage());
			if (connection.getResponseCode() == 200) {
				try {
					sslsocket.startHandshake(); // we're testing a handshake failure
					System.out.println("Secured connection performed successfully");
				} catch (IOException expected) {
					expected.printStackTrace();
				} finally {
					sslsocket.close();
				}
			}

		} catch (SocketTimeoutException e) {
			LOGGER.log(Level.ERROR, e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Could not test proxy connection");
			LOGGER.log(Level.ERROR, e.getMessage());
			e.printStackTrace();
		}

		List l = null;
		try {
			l = ProxySelector.getDefault().select(new URI(gitVo.getRemoteUrl()));
		} catch (URISyntaxException e) {
			LOGGER.log(Level.ERROR, e.getMessage());
			e.printStackTrace();
		}

		if (l != null) {
			for (Iterator iter = l.iterator(); iter.hasNext();) {
				java.net.Proxy proxy = (java.net.Proxy) iter.next();
				System.out.println("proxy type : " + proxy.type());
				LOGGER.log(Level.INFO, "proxy type : " + proxy.type());
				InetSocketAddress addr = (InetSocketAddress) proxy.address();
				if (addr == null) {
					System.out.println("No Proxy");
					LOGGER.log(Level.INFO, "No Proxy");
				} else {
					System.out.println("proxy hostname : " + addr.getHostString());
					LOGGER.log(Level.INFO, "proxy hostname : " + addr.getHostString());
					System.out.println("proxy port : " + addr.getPort());
					LOGGER.log(Level.INFO, "proxy port : " + addr.getPort());
				}
			}
		}

	}

	public void loadProperties(GitVO gitVo, Properties properties) {
		// TODO Auto-generated method stub
		Properties prop = new Properties();
		String enteredPassPhraseStr = "";

		Authenticator.setDefault(new Authenticator() {
			@Override
			public PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(gitVo.getProxyUsername(), gitVo.getProxyPassword().toCharArray());
			}
		});

		if (properties.get("properties") != null) {
			String sysArr = (String) properties.get("properties");
			if (sysArr != null && !sysArr.isEmpty()) {
				System.out.println("Loading... ");
				String[] list = sysArr.split(";");
				for (String string : list) {
					String[] split = string.split("=");
					if (split.length == 2) {
//						System.out.println("Key "+ split[0]);
//						System.out.println("Value "+ split[1]);
						System.setProperty(split[0], split[1]);
					} else if (split.length == 1) {
//						System.out.println("Key "+ split[0]);
						System.setProperty(split[0], "");
					} else {
						System.err.println(string);
					}
				}
			}
		}
		System.out.println(System.getProperties());

		try {
			Scanner sc = new Scanner(System.in);

			System.out.println(Constants.GIT_COMMAND);
			String p = sc.nextLine();
			while (p != null) {
				if ("".equalsIgnoreCase(p)) {
					break;
				}
				Process process = Runtime.getRuntime().exec(p);
				process.waitFor();
				if (process.exitValue() == 0) {
					System.out.println(Constants.GIT_COMMAND);
					p = sc.nextLine();
					if ("".equalsIgnoreCase(p)) {
						break;
					}
				} else {
					System.out.println("break with exit value: " + process.exitValue());
					break;
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}

		if ("true".equalsIgnoreCase(Constants.properties.getProperty("useProxy"))) {

			try {
				Console console = System.console();
				char[] enteredPassPhrase = null;
				enteredPassPhrase = console.readPassword(Constants.PROXY_PASSWORD);
				enteredPassPhraseStr = String.valueOf(enteredPassPhrase);
			} catch (NullPointerException np) {
				Scanner sc = new Scanner(System.in);
				System.out.println(Constants.PROXY_PASSWORD);
				enteredPassPhraseStr = sc.nextLine();
			}

			gitVo.setProxyPassword(enteredPassPhraseStr);
			System.setProperty("-Dhttps.proxyPassword", gitVo.getProxyPassword());
		}
	}

	public void clearProperties(Properties properties) {
		// TODO Auto-generated method stub
		String sysArr = (String) properties.get("properties");
		if (sysArr != null && !sysArr.isEmpty()) {
			System.out.println(Constants.CLEARING);
			String[] list = sysArr.split(";");
			for (String string : list) {
				String[] split = string.split("=");
				if (split.length >= 1) {
					System.clearProperty(split[0]);
				}
			}
		}
	}

	public void testConnection2(GitVO gitVo) {
		try {
			TrustManager[] trustManagers = null;
			SSLContext sslContext = SSLContext.getInstance("TLSv1.0");

			sslContext.init(null, trustManagers, new java.security.SecureRandom());

			// SSLContextSSLSocketFactory
			SSLSocketFactory ssf = sslContext.getSocketFactory();
			URL url = new URL(gitVo.getRemoteUrl());
			HttpsURLConnection httpUrlConn = (HttpsURLConnection) url.openConnection();
			httpUrlConn.setSSLSocketFactory(ssf);
			httpUrlConn.setDoOutput(true);
			httpUrlConn.setDoInput(true);
			httpUrlConn.setUseCaches(false);

			String encoded = Base64.getEncoder()
					.encodeToString((gitVo.getProxyUsername() + ":" + gitVo.getProxyPassword()).getBytes());
			httpUrlConn.setRequestProperty("Proxy-Authorization", "Basic " + encoded);
			httpUrlConn.setRequestMethod("HEAD");
			httpUrlConn.setRequestMethod("POST");
			httpUrlConn.setConnectTimeout(100000);
			httpUrlConn.connect();
			System.out.println(httpUrlConn.usingProxy());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void setGitConfiguration(GitVO gitVo, File localFile) {
		// TODO Auto-generated method stub
		Git git = null;
		Repository repo = null;
		try {
			File file = new File(Constants.LOCAL_DIRECTORY_PATH);
			if (file.exists()) {
				/**
				 * Sometimes when git operation fails index.lock doesn't get delete. so deleting
				 * this before accessing the git repo.
				 */
				Files.deleteIfExists(Paths.get(Constants.INDEX_FILE_PATH));
				git = Git.open(file);
			}
			if (git != null) {
				repo = git.getRepository();
				if (repo != null) {
					StoredConfig config = repo.getConfig();
					config.setString("remote", Constants.GIT_REMOTE_ALIAS, "url", gitVo.getRemoteUrl());
					config.setBoolean("http", null, "sslVerify", false);
					config.save();
					checkoutBranchIfChanged(git, repo.getBranch(), gitVo.getBranch());
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (repo != null) {
				repo.close();
			}
			if (git != null) {
				git.close();
			}
		}

	}

	public void getStatus() {
		// TODO Auto-generated method stub
		Git git = null;
		File file = new File(Constants.LOCAL_DIRECTORY_PATH);
		try {
			Files.deleteIfExists(Paths.get(Constants.INDEX_FILE_PATH));
			git = Git.open(file);

			Status status = git.status().call();
			Set<String> added = status.getAdded();
			for (String add : added) {
				System.out.println("Added: " + add);
				LOGGER.log(Level.INFO, "Added: " + add);
			}
			Set<String> uncommittedChanges = status.getUncommittedChanges();
			for (String uncommitted : uncommittedChanges) {
				System.out.println("Uncommitted: " + uncommitted);
				LOGGER.log(Level.INFO, "Uncommitted: " + uncommitted);
			}

			Set<String> untracked = status.getUntracked();
			for (String untrack : untracked) {
				System.out.println("Untracked: " + untrack);
				LOGGER.log(Level.INFO, "Untracked: " + untrack);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void testConnectionWithCert(GitVO gitVo, File localFile) {

		// https://stackoverflow.com/questions/120797/how-do-i-set-the-proxy-to-be-used-by-the-jvm#32897878

//		HttpsURLConnection httpsURLConnection;
		HttpURLConnection httpsURLConnection;
		try {
			Authenticator.setDefault(new ProxyAuthenticator(gitVo.getProxyUsername(), gitVo.getProxyPassword()));
			ProxySelector.setDefault(new ProxySelector() {
				final ProxySelector delegate = ProxySelector.getDefault();

				@Override
				public List<Proxy> select(URI uri) {

					// Filter the URIs to be proxied
					if (uri.toString().contains(gitVo.getProvider().toLowerCase())
							&& uri.toString().contains(Constants.HTTPS)) {
						System.out.println(Constants.AUTHENTICATING + uri);
						return Arrays.asList(new Proxy(Type.HTTP,
								InetSocketAddress.createUnresolved(gitVo.getProxyHost(), gitVo.getProxyPort())));
					}
					if (uri.toString().contains(gitVo.getProvider().toLowerCase())
							&& uri.toString().contains(Constants.HTTP)) {
						System.out.println(Constants.AUTHENTICATING + uri);
						return Arrays.asList(new Proxy(Type.HTTP,
								InetSocketAddress.createUnresolved(gitVo.getProxyHost(), gitVo.getProxyPort())));
					}
					// revert to the default behaviour
					return delegate == null ? Arrays.asList(Proxy.NO_PROXY) : delegate.select(uri);
				}

				@Override
				public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
					if (uri == null || sa == null || ioe == null) {
						throw new IllegalArgumentException("Arguments can't be null.");
					}
				}
			});
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(gitVo.getProxyHost(), gitVo.getProxyPort());
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(gitVo.getProxyHost(), gitVo.getProxyPort()));
			httpsURLConnection = (HttpURLConnection) new URL(gitVo.getRemoteUrl()).openConnection(proxy);
			CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
			String encoded = Base64.getEncoder()
					.encodeToString((gitVo.getProxyUsername() + ":" + gitVo.getProxyPassword()).getBytes());
			httpsURLConnection.setRequestProperty("Proxy-Authorization", "Basic " + encoded);
			httpsURLConnection.setRequestProperty("Accept", "text/html");
//			connection.setRequestMethod("HEAD");
//			connection.setDoInput(true);
//			connection.setDoOutput(true);
			httpsURLConnection.setRequestMethod("POST");
//			HttpURLConnection.setFollowRedirects(false);
			httpsURLConnection.setFollowRedirects(Boolean.FALSE);
			httpsURLConnection.setConnectTimeout(100000);
//			httpsURLConnection.setSSLSocketFactory(sslsocketfactory);
//			connection.setAllowUserInteraction(true);
//			connection.setReadTimeout(1000);
			httpsURLConnection.connect();
			System.out.println(httpsURLConnection.getPermission());
			System.out.println("Is proxy: " + httpsURLConnection.usingProxy());
			LOGGER.log(Level.INFO, "Is Using proxy: " + httpsURLConnection.usingProxy());
			System.out.println(httpsURLConnection.getResponseCode() + "-" + httpsURLConnection.getResponseMessage());
			
			// more options with HttpsURLConnection and SSL
//	        java.security.cert.Certificate[] serverCertificates = httpsURLConnection.getServerCertificates();
//			
//	        X509Certificate[] x509Certificates = (X509Certificate[]) serverCertificates;
//	        List<String> subjects = Arrays.stream(x509Certificates)
//	                .map(X509Certificate::getSubjectDN)
//	                .map(Principal::getName)
//	                .collect( java.util.stream.Collectors.toList());
////	        assertThat(subjects.contains("CN=github.com, O=\"GitHub, Inc.\""));
//	        for(String sub : subjects) {
//	        	if (sub.contains("CN=github.com, O=\"GitHub, Inc.\""))
//	        	System.out.println("certificate found");
//	        }
//	        
//	     // same as preceding
////	        assertThat(serverCertificates).extracting("SubjectDN.name").contains("O=GitHub, Inc., CN=github.com");
//
//	        Set<String> issuers = Arrays.stream(x509Certificates)
//	                .map(X509Certificate::getIssuerDN)
//	                .map(Principal::getName)
//	                .collect(java.util.stream.Collectors.toSet());
////	        assertThat(issuers.contains("O=DigiCert Inc, CN=DigiCert TLS Hybrid ECC SHA384 2020 CA1"));
//	        for(String issue : issuers) {
//	        	if (issue.contains("O=DigiCert Inc, CN=DigiCert TLS Hybrid ECC SHA384 2020 CA1"))
//	        	System.out.println("issuers found");
//	        }
	        
	        
	        List l = null;
			try {
				l = ProxySelector.getDefault().select(new URI(gitVo.getRemoteUrl()));
			} catch (URISyntaxException e) {
				LOGGER.log(Level.ERROR, e.getMessage());
				e.printStackTrace();
			}

			if (l != null) {
				for (Iterator iter = l.iterator(); iter.hasNext();) {
					java.net.Proxy proxy1 = (java.net.Proxy) iter.next();
					System.out.println("proxy type : " + proxy1.type());
					LOGGER.log(Level.INFO, "proxy type : " + proxy1.type());
					InetSocketAddress addr = (InetSocketAddress) proxy1.address();
					if (addr == null) {
						System.out.println("No Proxy");
						LOGGER.log(Level.INFO, "No Proxy");
					} else {
						System.out.println("proxy hostname : " + addr.getHostString());
						LOGGER.log(Level.INFO, "proxy hostname : " + addr.getHostString());
						System.out.println("proxy port : " + addr.getPort());
						LOGGER.log(Level.INFO, "proxy port : " + addr.getPort());
					}
				}
			}
			try {
			if(httpsURLConnection.getResponseCode() == 200 && "true".equalsIgnoreCase(Constants.properties.getProperty("startHandshake"))) {
				sslsocket.startHandshake();
			}
			}catch(Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
