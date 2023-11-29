package ai.devchat.cli

import ai.devchat.common.Log
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.nio.file.Files
import java.util.*

/**
 * DevChat represents for the DevChat Python CLI
 */
class DevChatInstallationManager(private val workPath: String, private val devchatCliVersion: String) {
    private val mambaInstallationPath: String
    private val mamba: Mamba
    private var pythonBinPath: String? = null

    init {
        // Initialize paths
        mambaInstallationPath = workPath + "/mamba"
        mamba = Mamba(mambaInstallationPath)
    }

    // https://mamba.readthedocs.io/en/latest/micromamba-installation.html
    @Throws(RuntimeException::class)
    private fun installMamba() {
        Log.info("Mamba is installing.")
        try {
            mamba.install()
        } catch (e: Exception) {
            throw RuntimeException("Error occurred during Mamba installation: " + e.message, e)
        }
    }

    // Method to create python environment
    @Throws(RuntimeException::class)
    private fun createPythonEnvironment() {
        Log.info("Python environment is creating.")
        try {
            mamba.create()
            pythonBinPath = mamba.pythonBinPath
            Log.info("Python is in: $pythonBinPath")
        } catch (e: Exception) {
            throw RuntimeException("Error occured during Python environment creating.")
        }
    }

    // Method to install devchat package
    @Throws(RuntimeException::class)
    private fun installDevchatPackage() {
        val pi = PythonInstaller(pythonBinPath)
        try {
            pi.install("devchat", devchatCliVersion)
        } catch (e: Exception) {
            Log.error("Failed to install devchat cli.")
            throw RuntimeException("Failed to install devchat cli.", e)
        }
    }

    // Provide a method to execute all steps of the installation
    @Throws(RuntimeException::class)
    fun setup() {
        Log.info("Start configuring the DevChat CLI environment.")
        try {
            installMamba()
            createPythonEnvironment()
            installDevchatPackage()
        } catch (e: Exception) {
            throw RuntimeException("Failed to setup DevChat environment.", e)
        }
    }
}

internal class Mamba(private val installationPath: String) {
    private val pythonVersion = "3.11.4"
    @Throws(RuntimeException::class)
    fun install() {
        val binFileURL = mambaBinFileURL
        val dstFile = File(
            installationPath,
            binFileURL.path.substring(binFileURL.path.lastIndexOf("/") + 1)
        )
        if (!dstFile.exists() || !dstFile.canExecute()) {
            if (dstFile.exists() && !dstFile.setExecutable(true)) {
                throw RuntimeException("Unable to set executable permissions on: $dstFile")
            } else {
                Log.info("Installing Mamba to: " + dstFile.path)
                copyFileAndSetExecutable(binFileURL, dstFile)
            }
        } else {
            Log.info("Mamba already installed at: " + dstFile.path)
        }
    }

    fun create() {
        if (File(pythonBinPath).exists()) {
            Log.info("Python environment already exists.")
            return
        }
        val command = arrayOf(
            "$installationPath/micromamba", "create", "-n", "devchat", "-c", "conda-forge", "-r",
            installationPath, "python=" + pythonVersion, "--yes"
        )
        Log.info("Preparing to create python environment by: $command")
        try {
            val processbuilder = ProcessBuilder(*command)
            val process = processbuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String
            while (reader.readLine().also { line = it } != null) {
                Log.info("[Mamba installation] $line")
            }
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw RuntimeException("Command execution failed with exit code: $exitCode")
            }
        } catch (e: IOException) {
            throw RuntimeException("Command execution failed with exception: " + e.message, e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Command execution was interrupted: " + e.message, e)
        }
    }

    val pythonBinPath: String
        get() = if (OS_NAME.contains("win")) {
            "$installationPath/envs/devchat/python.exe"
        } else {
            "$installationPath/envs/devchat/bin/python"
        }

    @get:Throws(RuntimeException::class)
    private val mambaBinFileURL: URL
        private get() {
            val filePathEnd: String
            filePathEnd = if (OS_NAME.contains("win")) {
                if (OS_ARCH.contains("64")) {
                    "/tool/mamba/micromamba-win-64/bin/micromamba.exe"
                } else {
                    throw RuntimeException("Unsupported architecture: " + OS_ARCH)
                }
            } else if (OS_NAME.contains("darwin") || OS_NAME.contains("mac")) {
                if (OS_ARCH.contains("arm")) {
                    "/tool/mamba/micromamba-osx-arm64/bin/micromamba"
                } else if (OS_ARCH.contains("64")) {
                    "/tool/mamba/micromamba-osx-64/bin/micromamba"
                } else {
                    throw RuntimeException("Unsupported architecture: " + OS_ARCH)
                }
            } else if (OS_NAME.contains("linux")) {
                if (OS_ARCH.contains("x64")) {
                    "/tool/mamba/micromamba-linux-64/bin/micromamba"
                } else if (OS_ARCH.contains("ppc64le")) {
                    "/tool/mamba/micromamba-linux-ppc64le/bin/micromamba"
                } else if (OS_ARCH.contains("aarch64")) {
                    "/tool/mamba/micromamba-linux-aarch64/bin/micromamba"
                } else {
                    throw RuntimeException("Unsupported architecture: " + OS_ARCH)
                }
            } else {
                throw RuntimeException("Unsupported operating system: " + OS_NAME)
            }
            return javaClass.getResource(filePathEnd)
        }

    @Throws(RuntimeException::class)
    private fun copyFileAndSetExecutable(fileURL: URL, dstFile: File) {
        val dstDir = dstFile.parentFile
        if (!dstDir.exists()) {
            if (!dstDir.mkdirs()) {
                throw RuntimeException("Unable to create directory: $dstDir")
            }
        }
        try {
            fileURL.openStream().use { `in` ->
                Files.newOutputStream(dstFile.toPath()).use { out ->
                    val buffer = ByteArray(8192)
                    var length: Int
                    while (`in`.read(buffer).also { length = it } > 0) {
                        out.write(buffer, 0, length)
                    }
                }
                if (!dstFile.setExecutable(true)) {
                    throw RuntimeException("Unable to set executable permissions on: $dstFile")
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Error installing Mamba: " + e.message, e)
        }
    }

    companion object {
        private val OS_NAME = System.getProperty("os.name").lowercase(Locale.getDefault())
        private val OS_ARCH = System.getProperty("os.arch")
    }
}
