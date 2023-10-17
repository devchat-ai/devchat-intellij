package ai.devchat.cli;

import ai.devchat.common.Log;
import ai.devchat.exception.DevChatSetupException;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;

/**
 * DevChat represents for the DevChat Python CLI
 */
public class DevChatInstallationManager {

    private String workPath;

    // Path for the installation of mamba
    private String mambaInstallationPath;

    private Mamba mamba;

    private String pythonBinPath;

    private String devchatBinPath;

    private String devchatCliVersion;

    public DevChatInstallationManager(String workPath, String devchatCliVersion) {
        // Initialize paths
        this.workPath = workPath;
        this.devchatCliVersion = devchatCliVersion;
        this.mambaInstallationPath = this.workPath + "/mamba";

        this.mamba = new Mamba(mambaInstallationPath);
    }

    // https://mamba.readthedocs.io/en/latest/micromamba-installation.html
    private void installMamba() throws DevChatSetupException {
        Log.info("Mamba is installing.");
        try {
            mamba.install();
        } catch (DevChatSetupException e) {
            throw new DevChatSetupException("Error occurred during Mamba installation: " + e.getMessage(), e);
        }
    }

    // Method to create python environment
    private void createPythonEnvironment() throws DevChatSetupException {
        Log.info("Python environment is creating.");
        try {
            mamba.create();
            pythonBinPath = mamba.getPythonBinPath();
            Log.info("Python is in: " + pythonBinPath);
        } catch (DevChatSetupException e) {
            throw new DevChatSetupException("Error occured during Python environment creating.");
        }
    }

    // Method to install devchat package
    private void installDevchatPackage() throws DevChatSetupException {
        PythonInstaller pi = new PythonInstaller(this.pythonBinPath);
        try {
            pi.install("devchat", devchatCliVersion);
        } catch (DevChatSetupException e) {
            Log.error("Failed to install devchat cli.");
            throw new DevChatSetupException("Failed to install devchat cli.", e);
        }
    }

    // Provide a method to execute all steps of the installation
    public void setup() throws DevChatSetupException {
        Log.info("Start configuring the DevChat CLI environment.");
        try {
            this.installMamba();
            this.createPythonEnvironment();
            this.installDevchatPackage();
        } catch (DevChatSetupException e) {
            throw new DevChatSetupException("Failed to setup DevChat environment.", e);
        }
    }

    public String getDevchatBinPath() {
        devchatBinPath = mambaInstallationPath + "/envs/devchat/bin/devchat";
        return devchatBinPath;
    }
}

class Mamba {
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String OS_ARCH = System.getProperty("os.arch");
    private String installationPath;

    private String pythonVersion = "3.11.4";

    public Mamba(String installationPath) {
        this.installationPath = installationPath;
    }

    public void install() throws DevChatSetupException {
        URL binFileURL = this.getMambaBinFileURL();

        File dstFile = new File(installationPath, binFileURL.getPath().substring(binFileURL.getPath().lastIndexOf("/") + 1));

        if (!dstFile.exists() || !dstFile.canExecute()) {
            if (dstFile.exists() && !dstFile.setExecutable(true)) {
                throw new DevChatSetupException("Unable to set executable permissions on: " + dstFile);
            } else {
                Log.info("Installing Mamba to: " + dstFile.getPath());
                this.copyFileAndSetExecutable(binFileURL, dstFile);
            }
        } else {
            Log.info("Mamba already installed at: " + dstFile.getPath());
        }
    }

    public void create() throws DevChatSetupException {
        String[] command = {installationPath + "/micromamba", "create", "-n", "devchat", "-c", "conda-forge", "-r", installationPath, "python=" + this.pythonVersion, "--yes"};
        Log.info("Preparing to create python environment by: " + command);

        try {
            ProcessBuilder processbuilder = new ProcessBuilder(command);
            Process process = processbuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                Log.info("[Mamba installation] " + line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new DevChatSetupException("Command execution failed with exit code: " + exitCode);
            }

        } catch (IOException e) {
            throw new DevChatSetupException("Command execution failed with exception: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DevChatSetupException("Command execution was interrupted: " + e.getMessage(), e);
        }
    }

    public String getPythonBinPath() {
        if (OS_NAME.contains("win")) {
            return installationPath + "/envs/devchat/python.exe";
        } else {
            return installationPath + "/envs/devchat/bin/python";
        }
    }

    private URL getMambaBinFileURL() throws DevChatSetupException {
        String filePathEnd;
        if (OS_NAME.contains("win")) {
            if (OS_ARCH.contains("64")) {
                filePathEnd = "/tool/mamba/micromamba-win-64/bin/micromamba.exe";
            } else {
                throw new DevChatSetupException("Unsupported architecture: " + OS_ARCH);
            }
        } else if (OS_NAME.contains("darwin") || OS_NAME.contains("mac")) {
            if (OS_ARCH.contains("arm")) {
                filePathEnd = "/tool/mamba/micromamba-osx-arm64/bin/micromamba";
            } else if (OS_ARCH.contains("64")) {
                filePathEnd = "/tool/mamba/micromamba-osx-64/bin/micromamba";
            } else {
                throw new DevChatSetupException("Unsupported architecture: " + OS_ARCH);
            }
        } else if (OS_NAME.contains("linux")) {
            if (OS_ARCH.contains("x64")) {
                filePathEnd = "/tool/mamba/micromamba-linux-64/bin/micromamba";
            } else if (OS_ARCH.contains("ppc64le")) {
                filePathEnd = "/tool/mamba/micromamba-linux-ppc64le/bin/micromamba";
            } else if (OS_ARCH.contains("aarch64")) {
                filePathEnd = "/tool/mamba/micromamba-linux-aarch64/bin/micromamba";
            } else {
                throw new DevChatSetupException("Unsupported architecture: " + OS_ARCH);
            }
        } else {
            throw new DevChatSetupException("Unsupported operating system: " + OS_NAME);
        }

        return getClass().getResource(filePathEnd);
    }

    private void copyFileAndSetExecutable(URL fileURL, File dstFile) throws DevChatSetupException {
        File dstDir = dstFile.getParentFile();
        if (!dstDir.exists()) {
            if (!dstDir.mkdirs()) {
                throw new DevChatSetupException("Unable to create directory: " + dstDir);
            }
        }

        try (InputStream in = fileURL.openStream()) {
            try (OutputStream out = Files.newOutputStream(dstFile.toPath())) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }

            if (!dstFile.setExecutable(true)) {
                throw new DevChatSetupException("Unable to set executable permissions on: " + dstFile);
            }
        } catch (IOException e) {
            throw new DevChatSetupException("Error installing Mamba: " + e.getMessage(), e);
        }
    }
}
