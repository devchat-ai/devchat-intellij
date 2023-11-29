package ai.devchat.cli

/*
 * User: Daniel Hu <tao.hu@merico.dev>
 * Date: Mon Oct 16 22:40:06 2023 +0800
 *
 * Hello! How can I assist you today?
 *
 * prompt 6e2a0d9b5c15eb33008250fee40383e77e8f80c75d9644b15bda60be256c8010
 */
class DevChatResponse {
    var user: String? = null
    var date: String? = null
    var message: String? = null
    var promptHash: String? = null
    fun populateFromLine(line: String) {
        if (line.startsWith("User: ") && user == null) {
            user = line.substring("User: ".length)
        } else if (line.startsWith("Date: ") && date == null) {
            date = line.substring("Date: ".length)
            // 71 is the length of string
            // "prompt 6e2a0d9b5c15eb33008250fee40383e77e8f80c75d9644b15bda60be256c8010"
        } else if (line.startsWith("prompt ") && line.length == 71) {
            promptHash = line.substring("prompt ".length)
            message += "\n"
        } else if (!line.isEmpty()) {
            if (message == null) {
                message = line
            } else {
                message += """
                    
                    $line
                    """.trimIndent()
            }
        }
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
