import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.Proxy.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class GitPushMainProgram {

	public static void main(String[] args) {
		try {
		push();
		}catch(Exception e) {
			e.printStackTrace();
		}
		

	}

	public static boolean push() {
		String username = "ApurvaHub123";
		String password = "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";
		String Giturl = "https://github.com/ApurvaHub123/proxy_repo2.git";
		String localdirpath = "D:\\GITHUB_PRO_MAX";
		String proxyhost = "squidproxy";
		int proxyPort = 8001;
		String proxyusername = "proxyuser";
		String proxyPass = "admin";
		
		int pushRetryCount = 1;
		int maxRetry = 3;
		
		if (Constants.properties.get("properties") != null) {
			String sysArr = (String) Constants.properties.get("properties");
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
		
		
		try {
			
			Authenticator.setDefault(new ProxyAuthenticator(proxyusername, proxyPass));
			ProxySelector.setDefault(new ProxySelector() {
				final ProxySelector delegate = ProxySelector.getDefault();

				@Override
				public List<Proxy> select(URI uri) {

					// Filter the URIs to be proxied
					if (uri.toString().contains("github")
							&& uri.toString().contains(Constants.HTTPS)) {
						System.out.println(Constants.AUTHENTICATING + uri);
						return Arrays.asList(new Proxy(Type.HTTP,
								InetSocketAddress.createUnresolved(proxyhost, proxyPort)));
					}
					if (uri.toString().contains("github")
							&& uri.toString().contains(Constants.HTTP)) {
						System.out.println(Constants.AUTHENTICATING + uri);
						return Arrays.asList(new Proxy(Type.HTTP,
								InetSocketAddress.createUnresolved(proxyhost, proxyPort)));
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
			
		TrustManager[] trustManagers = null;
		SSLContext sc = SSLContext.getInstance("TLSv1.1,TLSv1.2");
//		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		SSLSocketFactory sslSocketFactory = sc.getSocketFactory();
		sc.init(null, trustManagers, new java.security.SecureRandom());
			SSLSocket sslsocket = (SSLSocket) sslSocketFactory.createSocket(proxyhost, proxyPort);
		
		// SSLContextSSLSocketFactory
		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyhost, proxyPort));
//		SSLSocketFactory ssf = sslContext.getSocketFactory();
		URL url = new URL(Giturl);
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
		HttpsURLConnection httpUrlConn = (HttpsURLConnection) url.openConnection();
//		HttpURLConnection httpUrlConn = (HttpURLConnection) url.openConnection();
		httpUrlConn.setSSLSocketFactory(sslSocketFactory);
		httpUrlConn.setDoOutput(true);
		httpUrlConn.setDoInput(true);
		httpUrlConn.setUseCaches(false);

		String encoded = Base64.getEncoder().encodeToString((proxyusername + ":" + proxyPass).getBytes());
		httpUrlConn.setRequestProperty("Proxy-Authorization", "Basic " + encoded);
//		httpUrlConn.setRequestMethod("HEAD");
		httpUrlConn.setRequestMethod("GET");    
		httpUrlConn.setConnectTimeout(100000);
		httpUrlConn.connect();
		System.out.println(httpUrlConn.usingProxy());
		if (httpUrlConn.getResponseCode() == 200) {
			try {
				sslsocket.startHandshake(); // we're testing a handshake failure
				System.out.println("Secured connection performed successfully");
			} catch (IOException expected) {
				expected.printStackTrace();
			} finally {
				sslsocket.close();
			}
		}
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		boolean result = false;
		File file = new File(Constants.LOCAL_DIRECTORY_PATH);
		try {
			Files.deleteIfExists(Paths.get(Constants.INDEX_FILE_PATH));

			FileRepositoryBuilder builder = new FileRepositoryBuilder();
			Repository repository = builder.readEnvironment().findGitDir(new File(localdirpath)).build();
			Git git2 = new Git(repository);
			
			git2 = Git.open(file);
			Iterable<PushResult> pushResult = null;
			System.out.println(git2.push().setReceivePack(RemoteConfig.DEFAULT_RECEIVE_PACK));
				pushResult = git2.push().setForce(true).setReceivePack(RemoteConfig.DEFAULT_RECEIVE_PACK).setRemote(Constants.GIT_REMOTE_ALIAS).setCredentialsProvider(
						new CustomSSLCredentialProvider(username, password)).call();

			for (PushResult pr : pushResult) {
				System.out.println("PushResult: " + pr.toString());
			}
			result = true;
		} catch (GitAPIException e) {
			e.printStackTrace();
			if (pushRetryCount <= maxRetry) {
				System.out.println(Constants.RETRYING);
				pushRetryCount++;
				return push();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			
		}
		return result;
	}

	
}
