package ai.devchat.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import ai.devchat.exception.DevChatExecuteExecption;

public class DevChatWrapper {
    private String apiKey;
    private String command = "devchat";

    public DevChatWrapper(String apiKey) {
        this.apiKey = apiKey;
    }

    public DevChatWrapper(String apiKey, String command) {
        this.apiKey = apiKey;
        this.command = command;
    }

    public String execCommand(List<String> commands) {
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.environment().put("OPENAI_API_KEY", apiKey);

        try {
            Process process = pb.start();
            return readOutput(process.getInputStream());
        } catch (IOException e) {
            throw new DevChatExecuteExecption("Failed to execute command: " + commands, e);
        }
    }

    public void execCommand(List<String> commands, Consumer<String> callback) {
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.environment().put("OPENAI_API_KEY", apiKey);

        try {
            Process process = pb.start();
            readOutputByLine(process.getInputStream(), callback);
        } catch (IOException e) {
            throw new DevChatExecuteExecption("Failed to execute command: " + commands, e);
        }
    }

    private String readOutput(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder output = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            output.append(line);
            output.append('\n');
        }

        return output.toString();
    }

    private void readOutputByLine(InputStream inputStream, Consumer<String> callback) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;

        while ((line = reader.readLine()) != null) {
            callback.accept(line);
        }
    }

    public void runPromptCommand(Map<String, String> flags, Consumer<String> callback) {
        try {
            List<String> commands = prepareCommand("prompt", flags);
            execCommand(commands, callback);
        } catch (DevChatExecuteExecption e) {
            throw new DevChatExecuteExecption("Failed to run [prompt] command", e);
        }
    }

    public String runLogCommand(Map<String, String> flags) {
        try {
            List<String> commands = prepareCommand("log", flags);
            return execCommand(commands);
        } catch (Exception e) {
            throw new DevChatExecuteExecption("Failed to run [log] command", e);
        }
    }

    public String runRunCommand(Map<String, String> flags) {
        try {
            List<String> commands = prepareCommand("run", flags);
            return execCommand(commands);
        } catch (Exception e) {
            throw new DevChatExecuteExecption("Failed to run [run] command", e);
        }
    }

    public String runTopicCommand(Map<String, String> flags) {
        try {
            List<String> commands = prepareCommand("topic", flags);
            return execCommand(commands);
        } catch (Exception e) {
            throw new DevChatExecuteExecption("Failed to run [topic] command", e);
        }
    }

    private List<String> prepareCommand(String subCommand, Map<String, String> flags) {
        List<String> commands = new ArrayList<>();
        commands.add(command);
        commands.add(subCommand);
        flags.forEach((flag, value) -> {
            commands.add("--" + flag);
            commands.add(value);
        });
        return commands;
    }
}
