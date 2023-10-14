package ai.devchat.cli;

import ai.devchat.exception.DevChatSetupException;
import ai.devchat.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;

public class Mamba {
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String OS_ARCH = System.getProperty("os.arch");
    private String installationPath;

    public Mamba(String installationPath) {
        this.installationPath = installationPath;
    }

    public void install() throws DevChatSetupException {
        URL binFileURL = this.getMambaBinFileURL();

        File dstFile = new File(installationPath,
                binFileURL.getPath().substring(binFileURL.getPath().lastIndexOf("/") + 1));

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
