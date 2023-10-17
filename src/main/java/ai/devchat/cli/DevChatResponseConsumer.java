package ai.devchat.cli;

import java.util.function.Consumer;

public class DevChatResponseConsumer implements Consumer<String> {

    private final Consumer<DevChatResponse> responseCallback;
    private final DevChatResponse response;

    public DevChatResponseConsumer(Consumer<DevChatResponse> responseCallback) {
        this.responseCallback = responseCallback;
        this.response = new DevChatResponse();
    }

    @Override
    public void accept(String line) {
        response.populateFromLine(line);

        if (response.getMessage() != null) {
            responseCallback.accept(response);
        }
    }
}
