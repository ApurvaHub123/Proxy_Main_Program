

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

public class CustomSSLCredentialProvider extends CredentialsProvider {

	
	private String username;
	private char[] password;

	public CustomSSLCredentialProvider(String username, String password) {
		this.username = username;
		this.password = password.toCharArray();
	}
	@Override
	public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
		for (CredentialItem item : items) {
			if (item instanceof CredentialItem.InformationalMessage) {
				continue;
			}
			if (item instanceof CredentialItem.Username) {
				((CredentialItem.Username) item).setValue(username);
				continue;
			}
			if (item instanceof CredentialItem.Password) {
				((CredentialItem.Password) item).setValue(password);
				continue;
			}
			if (item instanceof CredentialItem.StringType) {
				if (item.getPromptText().equals("Password: ")) {
					((CredentialItem.StringType) item).setValue(new String(password));
					continue;
				}
			}
			if (item instanceof CredentialItem.YesNoType) {
				((CredentialItem.YesNoType) item).setValue(true);
				continue;
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean isInteractive() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supports(CredentialItem... items) {
		// TODO Auto-generated method stub
		for (CredentialItem item : items) {
			if (item instanceof CredentialItem.InformationalMessage) {
				continue;
			}
			if (item instanceof CredentialItem.Username) {
				continue;
			}
			if (item instanceof CredentialItem.Password) {
				continue;
			}
			if (item instanceof CredentialItem.StringType && item.getPromptText().equals("Password: ")) {
				continue;
			}
			if (item instanceof CredentialItem.YesNoType) {
				continue;
			}
			return false;
		}
		return true;
	}

}
