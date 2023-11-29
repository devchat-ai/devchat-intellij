package ai.devchat.cli

import java.util.function.Consumer

class DevChatResponseConsumer(private val responseCallback: Consumer<DevChatResponse>) : Consumer<String> {
    private val response: DevChatResponse

    init {
        response = DevChatResponse()
    }

    override fun accept(line: String) {
        response.populateFromLine(line)
        if (response.message != null) {
            responseCallback.accept(response)
        }
    }
}
