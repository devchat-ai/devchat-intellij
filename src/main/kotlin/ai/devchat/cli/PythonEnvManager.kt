package ai.devchat.cli

import ai.devchat.common.Log
import ai.devchat.common.OSInfo
import java.io.File
import java.io.IOException
import java.nio.file.Paths

/**
 * DevChat represents for the DevChat Python CLI
 */

class PythonEnvManager(private val workDir: String) {
    private val mambaWorkDir = Paths.get(workDir, "mamba").toString()
    private val mambaBinPath = Paths.get(mambaWorkDir, "micromamba").toString()

    init {
        try {
            installMamba()
        } catch (e: Exception) {
            throw RuntimeException("Failed to setup Python env manager.", e)
        }
    }

    private fun installMamba() {
        // https://mamba.readthedocs.io/en/latest/micromamba-installation.html
        Log.info("Mamba is installing.")
        val errPrefix = "Error occurred during Mamba installation:"
        val dstFile = File(mambaBinPath)
        if (!dstFile.exists()) {
            Log.info("Installing Mamba to: " + dstFile.path)
            val dstDir = dstFile.parentFile
            dstDir.exists() || dstDir.mkdirs() || throw RuntimeException("Unable to create directory: $dstDir")
            javaClass.getResource(
                "/tools/micromamba-${OSInfo.platform}/bin/micromamba${if (OSInfo.isWindows) ".exe" else ""}"
            )!!.openStream().buffered().use { input ->
                dstFile.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }
        }
        if (!dstFile.canExecute() && !dstFile.setExecutable(true)) throw RuntimeException(
            "$errPrefix Unable to set executable permissions on: $dstFile"
        )
        Log.info("Mamba already installed at: " + dstFile.path)
    }

    fun createEnv(name: String, version: String = "3.11.4", retries: Int = 5): PythonEnv {
        Log.info("Python environment is creating.")
        val errPrefix = "Error occurred during Python environment creation:"
        val pyenv = PythonEnv(Paths.get(mambaWorkDir, "envs", name).toString())
        val pythonBinPath = pyenv.pythonCommand
        if (File(pythonBinPath).exists()) {
            Log.info("Python environment already exists.")
            return pyenv
        }
        val command = arrayOf(
            mambaBinPath,
            "create",
            "-n", name,
            "-c", "conda-forge",
            "-r", mambaWorkDir,
            "python=$version",
            "--yes"
        )
        Log.info("Preparing to create python environment by: $command")

        var remainingRetries: Int = retries
        while (remainingRetries-- > 0) {
            try {
                ProcessBuilder(*command).start().also { process ->
                    process.inputStream.bufferedReader().forEachLine { Log.info("[Mamba installation] $it") }
                    val exitCode = process.waitFor()
                    if (exitCode == 0) {
                        Log.info("Python is installed in: $pythonBinPath")
                        return pyenv
                    }
                    Log.warn(
                        "$errPrefix Command execution failed with exit code $exitCode, $remainingRetries retries left"
                    )
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Log.warn(
                    "$errPrefix Exception occurred while executing the command: ${e.message}, $remainingRetries retries left"
                )
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.warn(
                    "$errPrefix Command execution was interrupted: ${e.message}, $remainingRetries retries left"
                )
            }
        }
        throw RuntimeException("$errPrefix Maximum retries exceed")
    }
}


class PythonEnv(private val workDir: String) {
    val pythonCommand = if (OSInfo.isWindows) {
        Paths.get(workDir, "python.exe").toString()
    } else {
        Paths.get(workDir, "bin", "python").toString()
    }
    private var sourceIndex = 0
    fun installPackage(packageName: String, packageVersion: String) {
        pipInstall("$packageName==$packageVersion")
    }

    fun installRequirements(requirementFile: String) {
        pipInstall("-r", requirementFile)
    }

    private fun pipInstall(vararg things: String) {
        var exitCode = 0
        repeat(MAX_RETRIES) {
            try {
                ProcessBuilder(
                    pythonCommand, "-m", "pip", "install", "--index-url",
                    SOURCES[sourceIndex], *things
                ).apply {
                    val cmd = this.command().joinToString(" ")
                    Log.info("Installing by: $cmd")
                    redirectErrorStream(true)
                    start().apply {
                        inputStream.bufferedReader().forEachLine { Log.info(it) }
                        exitCode = waitFor()
                        if (exitCode == 0) return@repeat
                    }
                }
                // switch source and retry
                sourceIndex = (sourceIndex + 1) % SOURCES.size
                Log.info("Installation failed, retrying with ${SOURCES[sourceIndex]}")
            } catch (e: Exception) {
                when(e){
                    is IOException, is InterruptedException -> throw RuntimeException(
                        "Failed to install $things", e
                    )
                    else -> throw e
                }
            }
        }
        if (exitCode != 0) Log.error("Python package installation failed. Max retries exceeded.")
        else Log.info("Python package installation succeeded.")
    }


    companion object {
        private const val MAX_RETRIES = 3
        private val SOURCES = arrayOf("https://pypi.org/simple", "https://pypi.tuna.tsinghua.edu.cn/simple")
    }
}
