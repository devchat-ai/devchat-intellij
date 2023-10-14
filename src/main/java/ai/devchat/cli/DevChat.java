package ai.devchat.cli;

import ai.devchat.util.Log;
import ai.devchat.exception.DevChatSetupException;

/**
 * DevChat represents for the DevChat Python CLI
 */
public class DevChat {

    private String workPath;

    // Path for the installation of mamba
    private String mambaInstallationPath;

    // Path for the python environment
    private String pythonEnvironmentPath;

    // Path for pip
    private String pipPath;

    // Path for the devchat package
    private String devchatPackagePath;

    public DevChat(String workPath) {
        // Initialize paths
        this.workPath = workPath;
        this.mambaInstallationPath = this.workPath+"/mamba";
        this.pythonEnvironmentPath = "";
        this.pipPath = "";
        this.devchatPackagePath = "";
    }

    // https://mamba.readthedocs.io/en/latest/micromamba-installation.html
    private void installMamba() throws DevChatSetupException {
        Log.info("Mamba is installing.");
        try {
            Mamba mamba = new Mamba(this.mambaInstallationPath);
            mamba.install();
        } catch (DevChatSetupException e) {
            throw new DevChatSetupException("Error occurred during mamba installation: " + e.getMessage(), e);
        }
    }

    // Method to create python environment
    private void createPythonEnvironment() throws DevChatSetupException {
        // TO DO: Implement the code to create python environment
    }

    // Method to install pip
    private void installPip() throws DevChatSetupException {
        // TO DO: Implement the code to install pip
    }

    // Method to install devchat package
    private void installDevchatPackage() throws DevChatSetupException {
        // TO DO: Implement the code to install devchat package
    }

    // Provide a method to execute all steps of the installation
    public void setup() throws DevChatSetupException {
        Log.info("Start configuring the DevChat CLI environment.");
        try {
            this.installMamba();
        } catch (DevChatSetupException e) {
            throw new DevChatSetupException("Failed to install Mamba: " + e.getMessage(), e);
        }

        try {
            this.createPythonEnvironment();
        } catch (DevChatSetupException e) {
            throw new DevChatSetupException("Failed to create Python environment: " + e.getMessage(), e);
        }

        try {
            this.installPip();
        } catch (DevChatSetupException e) {
            throw new DevChatSetupException("Failed to install Pip: " + e.getMessage(), e);
        }

        try {
            this.installDevchatPackage();
        } catch (DevChatSetupException e) {
            throw new DevChatSetupException("Failed to install DevChat package: " + e.getMessage(), e);
        }
    }
}
