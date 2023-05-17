import java.net.URL;

import org.eclipse.jgit.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;


public class GitVO {

	String username;
	String password;
	String passPhrase;
	String remoteUrl;
	String branch;
	String localDirPath;
	String authType;
	String commitMessage;
	String repository;
	String sshPrivateKey;
	String sshPublicKey;
	String provider;
	@Nullable
	String proxyHost;
	@Nullable
	int proxyPort;
	@Nullable
	String proxyUsername;
	@Nullable
	String proxyPassword;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassPhrase() {
		return passPhrase;
	}

	public void setPassPhrase(String passPhrase) {
		this.passPhrase = passPhrase;
	}

	public String getRemoteUrl() {
		return remoteUrl;
	}

	public void setRemoteUrl(String remoteUrl) {
		this.remoteUrl = remoteUrl;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public String getLocalDirPath() {
		return localDirPath;
	}

	public void setLocalDirPath(String localDirPath) {
		this.localDirPath = localDirPath;
	}

	public String getAuthType() {
		return authType;
	}

	public void setAuthType(String authType) {
		this.authType = authType;
	}

	public String getCommitMessage() {
		return commitMessage;
	}

	public void setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
	}

	public String getRepository() {
		return repository;
	}

	public void setRepository(String repository) {
		this.repository = repository;
	}

	public String getSshPrivateKey() {
		return sshPrivateKey;
	}

	public void setSshPrivateKey(String sshPrivateKey) {
		this.sshPrivateKey = sshPrivateKey;
	}

	public String getSshPublicKey() {
		return sshPublicKey;
	}

	public void setSshPublicKey(String sshPublicKey) {
		this.sshPublicKey = sshPublicKey;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}
	
	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public String getProxyUsername() {
		return proxyUsername;
	}

	public void setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}
	
	public String getProxyPassword() {
		return proxyPassword;
	}

	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	public GitVO() {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	public String toString() {
		return "GitVO [username=" + username + ", password=" + password + ", passPhrase=" + passPhrase + ", remoteUrl="
				+ remoteUrl + ", branch=" + branch + ", localDirPath=" + localDirPath + ", authType=" + authType
				+ ", commitMessage=" + commitMessage + ", repository=" + repository + ", sshPrivateKey=" + sshPrivateKey
				+ ", sshPublicKey=" + sshPublicKey + ", provider=" + provider + ", proxyHost=" + proxyHost
				+ ", proxyPort=" + proxyPort + ", proxyUsername=" + proxyUsername + ", proxyPassword=" + proxyPassword
				+ "]";
	}

}
