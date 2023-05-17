import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class ProxyAuthenticator extends Authenticator {

	private  PasswordAuthentication passwordAuthentication;
	
	ProxyAuthenticator(String username, String password) {
        this.passwordAuthentication = new PasswordAuthentication(username,
                password.toCharArray());
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return this.passwordAuthentication;
    }
	
}
