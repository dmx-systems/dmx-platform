package systems.dmx.accesscontrol.identityproviderredirect;

public class SingleUseAuthenticationFailedException extends Exception {
    public SingleUseAuthenticationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
