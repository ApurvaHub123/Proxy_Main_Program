
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SshTransportConfigCallback implements TransportConfigCallback {

	private static final String RSA_KEY_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
	private static final String RSA_KEY_FOOTER = "-----END RSA PRIVATE KEY-----";
	private String password;
	private String privateKey;
	private String publicKey;

	public SshTransportConfigCallback(String password, String privateKey, String publicKey) throws IOException {
		this.password = password;
		if (Objects.nonNull(privateKey)) {
			this.privateKey = formatRSAPrivateKey(privateKey);
		}
		if (Objects.nonNull(publicKey)) {
			this.publicKey = formatRSAPublicKey(publicKey);
		}

	}

	private JschConfigSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
		@Override
		protected void configure(OpenSshConfig.Host hc, Session session) {
			session.setConfig("StrictHostKeyChecking", "no");
			session.setConfig("PubkeyAuthentication", "yes");
			session.setConfig("HostbasedAuthentication", "yes");
		}

		protected JSch createDefaultJSch(FS fs) {
//			JSch defaultJSch = super.createDefaultJSch(fs);
//			defaultJSch.addIdentity("ssh", privateKey == null ? null : privateKey.getBytes(),
//					publicKey == null ? null : publicKey.getBytes(), password == null ? null : password.getBytes());
//			return defaultJSch;
			
			JSch jsch = new JSch();
			try {
				jsch.addIdentity(privateKey, password);
			} catch (JSchException e) {
			}
			if (JSch.getConfig("ssh-rsa") == null) {
				JSch.setConfig("ssh-rsa", JSch.getConfig("signature.rsa"));
			}
			if (JSch.getConfig("ssh-dss") == null) {
				JSch.setConfig("ssh-dss", JSch.getConfig("signature.dss"));
			}
			configureJSch(jsch);
			knownHosts(jsch, fs);
			identities(jsch, fs);
			return jsch;
		}
		
		private void knownHosts(JSch sch, FS fs) {
    		File home = fs.userHome();
    		if (home == null)
    			return;
    		File known_hosts = new File(new File(home, ".ssh"), "known_hosts");
    		try (FileInputStream in = new FileInputStream(known_hosts)) {
				sch.setKnownHosts(in);
    		} catch (Exception ex) {
    		} 
    	}

    	private void identities(JSch sch, FS fs) {
    		File home = fs.userHome();
    		if (home == null)
    			return;
    		File sshdir = new File(home, ".ssh");
    		if (sshdir.isDirectory()) {
    			loadIdentity(sch, new File(sshdir, "identity")); 
    			loadIdentity(sch, new File(sshdir, "id_rsa"));
    			loadIdentity(sch, new File(sshdir, "id_dsa"));
    		}
    	}
    	
    	private void loadIdentity(JSch sch, File priv) {
    		if (priv.isFile()) {
    			try {
    				sch.addIdentity(priv.getAbsolutePath());
    			} catch (JSchException e) {
    				// Instead, pretend the key doesn't exist.
    			}
    		}
    	}
	};

	@Override
	public void configure(Transport transport) {
		SshTransport sshTransport = (SshTransport) transport;
		sshTransport.setSshSessionFactory(sshSessionFactory);
	}

	private String formatRSAPrivateKey(String privateKey) {

		return privateKey;

//		return new String(Base64.getDecoder().decode(privateKey));

//		String[] lines = privateKey.split(System.lineSeparator());
//		if (lines.length > 1) {
//			lines = Arrays.copyOfRange(lines, 1, lines.length - 1);
//			privateKey = String.join(System.lineSeparator(), lines);
//		} else {
//			privateKey = privateKey.replace(RSA_KEY_HEADER, "");
//			privateKey = privateKey.replace(RSA_KEY_FOOTER, "");
//		}
//		privateKey = privateKey.trim().replace(' ', '\n');
//
//		privateKey = RSA_KEY_HEADER + System.lineSeparator() + privateKey + System.lineSeparator() + RSA_KEY_FOOTER;
//
//		return privateKey;
	}

	private String formatRSAPublicKey(String publicKey) {
//		return new String(Base64.getDecoder().decode(publicKey));
		return publicKey;
//		return publicKey.replace(' ', '\n');
	}

}
