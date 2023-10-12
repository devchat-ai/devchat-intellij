package ai.devchat.exception;

public class DevChatSetupException extends Exception {

    public DevChatSetupException(String message) {
        super(message);
    }

    public DevChatSetupException(String message, Throwable cause) {
        super(message, cause);
    }
}
