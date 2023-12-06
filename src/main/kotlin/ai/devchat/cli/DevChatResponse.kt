package ai.devchat.cli

class DevChatResponse {
    var user: String? = null
    var date: String? = null
    var message: String? = null
    var promptHash: String? = null

    fun update(line: String) : DevChatResponse {
        when {
            line.startsWith("User: ") -> user = user ?: line.substring("User: ".length)
            line.startsWith("Date: ") -> date = date ?: line.substring("Date: ".length)
            // 71 is the length of the prompt hash
            line.startsWith("prompt ") && line.length == 71 -> {
                promptHash = line.substring("prompt ".length)
                message = message?.let { "$it\n" } ?: "\n"
            }
            line.isNotEmpty() -> message = message?.let { "$it\n$line" } ?: line
        }
        return this
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("User: ").append(user).append("\n")
        sb.append("Date: ").append(date).append("\n\n")
        sb.append(message).append("\n")
        sb.append("prompt ").append(promptHash).append("\n")
        return sb.toString()
    }
}
