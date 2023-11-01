package ai.devchat.cli;

import ai.devchat.common.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DevChatWrapper {
    private String apiBase;
    private String apiKey;
    private String command;
    private final String DEFAULT_LOG_MAX_COUNT = "100";

    public DevChatWrapper(String command) {
        this.command = command;
    }

    public DevChatWrapper(String apiBase, String apiKey, String command) {
        this.apiBase = apiBase;
        this.apiKey = apiKey;
        this.command = command;
    }

    private String execCommand(List<String> commands) {
        ProcessBuilder pb = new ProcessBuilder(commands);
        if (apiBase != null) {
            pb.environment().put("OPENAI_API_BASE", apiBase);
            Log.info("api_base: " + apiBase);
        }
        if (apiKey != null) {
            pb.environment().put("OPENAI_API_KEY", apiKey);
            Log.info("api_key: " + apiKey.substring(0, 5) + "...");
        }

        try {
            Log.info("Executing command: " + String.join(" ", pb.command()));
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String error = readOutput(process.getErrorStream());
                throw new RuntimeException(
                        "Failed to execute command: " + commands + " Exit Code: " + exitCode + " Error: " + error);
            }
            return readOutput(process.getInputStream());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute command: " + commands, e);
        }
    }

    private void execCommand(List<String> commands, Consumer<String> callback) {
        ProcessBuilder pb = new ProcessBuilder(commands);
        if (apiBase != null) {
            pb.environment().put("OPENAI_API_BASE", apiBase);
            Log.info("api_base: " + apiBase);
        }
        if (apiKey != null) {
            pb.environment().put("OPENAI_API_KEY", apiKey);
            Log.info("api_key: " + apiKey.substring(0, 5) + "...");
        }

        try {
            Log.info("Executing command: " + String.join(" ", pb.command()));
            Process process = pb.start();
            readOutputByLine(process.getInputStream(), callback);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String error = readOutput(process.getErrorStream());
                throw new RuntimeException(
                        "Failed to execute command: " + commands + " Exit Code: " + exitCode + " Error: " + error);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute command: " + commands, e);
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

    public void runPromptCommand(Map<String, String> flags, String message, Consumer<String> callback) {
        try {
            List<String> commands = prepareCommand("prompt", flags, message);
            execCommand(commands, callback);
        } catch (Exception e) {
            throw new RuntimeException("Fail to run [prompt] command", e);
        }
    }

    public String runLogCommand(Map<String, String> flags) {
        try {
            List<String> commands = prepareCommand(flags, "log");
            return execCommand(commands);
        } catch (Exception e) {
            throw new RuntimeException("Failed to run [log] command", e);
        }
    }

    public JSONArray getCommandList() {
        String result = runRunCommand("--list", null);
        return JSON.parseArray(result);
    }

    public String[] getCommandNamesList() {
        JSONArray commandList = getCommandList();
        String[] names = new String[commandList.size()];
        for (int i = 0; i < commandList.size(); i++) {
            names[i] = commandList.getJSONObject(i).getString("name");
        }
        return names;
    }

    public JSONArray listConversationsInOneTopic(String topicHash) {
        String result = runLogCommand(Map.of("topic", topicHash, "max-count", DEFAULT_LOG_MAX_COUNT));
        return JSON.parseArray(result);
    }

    public String runRunCommand(String subCommand, Map<String, String> flags) {
        try {
            List<String> commands = prepareCommand(flags, "run", subCommand);
            return execCommand(commands);
        } catch (Exception e) {
            Log.error("Failed to run [run] command: " + e.getMessage());
            throw new RuntimeException("Failed to run [run] command", e);
        }
    }

    public String runTopicCommand(Map<String, String> flags) {
        try {
            List<String> commands = prepareCommand(flags, "topic");
            return execCommand(commands);
        } catch (Exception e) {
            throw new RuntimeException("Failed to run [topic] command", e);
        }
    }

    private List<String> prepareCommand(Map<String, String> flags, String... subCommands) {
        List<String> commands = new ArrayList<>();
        commands.add(command);
        Collections.addAll(commands, subCommands);
        if (flags == null) {
            return commands;
        }
        flags.forEach((flag, value) -> {
            commands.add("--" + flag);
            commands.add(value);
        });
        return commands;
    }

    private List<String> prepareCommand(String subCommand, Map<String, String> flags, String message) {
        List<String> commands = prepareCommand(flags, subCommand);
        // Add the message to the command list
        if (message != null && !message.isEmpty()) {
            commands.add("--");
            commands.add(message);
        }
        return commands;
    }
}
