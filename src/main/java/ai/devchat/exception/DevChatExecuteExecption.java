package ai.devchat.exception;

public class DevChatExecuteExecption extends RuntimeException {
    public DevChatExecuteExecption(String message) {
        super(message);
    }

    public DevChatExecuteExecption(String message, Throwable cause) {
        super(message, cause);
    }
}
