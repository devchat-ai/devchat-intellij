package ai.devchat.cli;

import ai.devchat.util.Log;
import ai.devchat.exception.DevChatSetupException;
import ai.devchat.util.PythonInstaller;

/**
 * DevChat represents for the DevChat Python CLI
 */
public class DevChat {

    private String workPath;

    // Path for the installation of mamba
    private String mambaInstallationPath;

    private Mamba mamba;

    private String pythonBinPath;

    private String devchatCliVersion;

    public DevChat(String workPath, String devchatCliVersion) {
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
}
