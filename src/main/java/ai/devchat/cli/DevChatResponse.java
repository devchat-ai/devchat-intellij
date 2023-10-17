package ai.devchat.cli;

/*
 * User: Daniel Hu <tao.hu@merico.dev>
 * Date: Mon Oct 16 22:40:06 2023 +0800
 *
 * Hello! How can I assist you today?
 *
 * prompt 6e2a0d9b5c15eb33008250fee40383e77e8f80c75d9644b15bda60be256c8010
 */
public class DevChatResponse {
    private String user;
    private String date;
    private String message;
    private String promptHash;

    public void populateFromLine(String line) {
        if (line.startsWith("User: ") && this.user == null) {
            user = line.substring("User: ".length());
        } else if (line.startsWith("Date: ") && this.date == null) {
            date = line.substring("Date: ".length());
            // 71 is the length of string
            // "prompt 6e2a0d9b5c15eb33008250fee40383e77e8f80c75d9644b15bda60be256c8010"
        } else if (line.startsWith("prompt ") && line.length() == 71) {
            promptHash = line.substring("prompt ".length());
            message += "\n";
        } else if (!line.isEmpty()) {
            if (message == null) {
                message = line;
            } else {
                message += "\n" + line;
            }
        }
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPromptHash() {
        return promptHash;
    }

    public void setPromptHash(String promptHash) {
        this.promptHash = promptHash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("User: ").append(user).append("\n");
        sb.append("Date: ").append(date).append("\n\n");
        sb.append(message).append("\n");
        sb.append("prompt ").append(promptHash).append("\n");
        return sb.toString();
    }
}
