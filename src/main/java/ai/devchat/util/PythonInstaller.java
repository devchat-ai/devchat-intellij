package ai.devchat.util;

import java.io.*;
import java.util.Arrays;

import ai.devchat.exception.DevChatSetupException;

public class PythonInstaller {
    private final static int MAX_RETRIES = 3;
    private final static String[] SOURCES = {"https://pypi.org/simple", "https://pypi.tuna.tsinghua.edu.cn/simple"};
    private String pythonPath;
    private int sourceIndex;

    public PythonInstaller(String pythonPath) {
        this.pythonPath = pythonPath;
        this.sourceIndex = 0;
    }

    public void install(String packageName, String packageVersion) throws DevChatSetupException {
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                ProcessBuilder pb = new ProcessBuilder(Arrays.asList(pythonPath, "-m",
                        "pip", "install", "--index-url", SOURCES[sourceIndex], packageName + "==" + packageVersion));
                String command = String.join(" ", pb.command());
                Log.info("Installing " + packageName + " " + packageVersion + " by: " + command);

                pb.redirectErrorStream(true);
                Process process = pb.start();

                // read output
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.info(line);
                }

                process.waitFor();
                if (process.exitValue() == 0) {
                    break;
                } else {
                    // switch source and retry
                    sourceIndex = (sourceIndex + 1) % SOURCES.length;
                    retries++;
                    Log.info("Installation failed, retrying with " + SOURCES[sourceIndex]);
                }
            } catch (IOException | InterruptedException e) {
                throw new DevChatSetupException("Failed to install " + packageName + " " + packageVersion, e);
            }
        }
        if (retries == MAX_RETRIES) {
            Log.error("Python package installation failed. Max retries exceeded.");
        } else {
            Log.info("Python package installation succeeded.");
        }
    }
}
