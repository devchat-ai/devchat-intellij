package ai.devchat.cli

import ai.devchat.common.Log
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

/**
 * DevChat represents for the DevChat Python CLI
 */



class PythonEnvManager(private val workDir: String) {
    private val mambaWorkDir = "$workDir/mamba"
    private val mambaBinPath = "$mambaWorkDir/micromamba"

    init {
        try {
            installMamba()
            installLocalPackages()
        } catch (e: Exception) {
            throw RuntimeException("Failed to setup Python env manager.", e)
        }
    }

    private fun installLocalPackages() {
        IOUtils.copyResourceDirToPath(
            "/tools/site-packages",
            Paths.get(workDir, "site-packages").toString()
        )
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
                "/tools/micromamba-$platform/bin/micromamba"
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

    fun createEnv(name: String, version: String = "3.11.4"): PythonEnv {
        Log.info("Python environment is creating.")
        val errPrefix = "Error occurred during Python environment creation:"
        val pyenv = PythonEnv("$mambaWorkDir/envs/$name")
        val pythonBinPath = pyenv.pythonPath
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
        try {
            ProcessBuilder(*command).start().also { process ->
                process.inputStream.bufferedReader().forEachLine { Log.info("[Mamba installation] $it") }
                val exitCode = process.waitFor()
                if (exitCode != 0) throw RuntimeException(
                    "Command execution failed with exit code: ${exitCode}"
                )
            }
        } catch (e: IOException) {
            throw RuntimeException("$errPrefix Command execution failed with exception: " + e.message, e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("$errPrefix Command execution was interrupted: " + e.message, e)
        }

        Log.info("Python is installed in: $pythonBinPath")
        return pyenv

    }

    private val platform: String
        get() {
            val unsupportedArchMessage = { "Unsupported architecture: $OS_ARCH" }
            return when {
                OS_NAME.contains("win") ->
                    if (OS_ARCH.contains("64")) "win-64"
                    else throw RuntimeException(unsupportedArchMessage())

                OS_NAME.contains("darwin") || OS_NAME.contains("mac") -> when {
                    OS_ARCH.contains("arm") -> "osx-arm64"
                    OS_ARCH.contains("64") -> "osx-64"
                    else -> throw RuntimeException(unsupportedArchMessage())
                }

                OS_NAME.contains("linux") -> when {
                    OS_ARCH.contains("x64") -> "linux-64"
                    OS_ARCH.contains("ppc64le") -> "linux-ppc64le"
                    OS_ARCH.contains("aarch64") -> "linux-aarch64"
                    else -> throw RuntimeException(unsupportedArchMessage())
                }

                else -> throw RuntimeException("Unsupported operating system: $OS_NAME")
            }
        }

    companion object {
        private val OS_NAME: String = System.getProperty("os.name").lowercase(Locale.getDefault())
        private val OS_ARCH: String = System.getProperty("os.arch")
    }
}


class PythonEnv(private val workDir: String) {
    val pythonPath = "$workDir/bin/python${if (OS_NAME.contains("win")) ".exe" else ""}"
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
                    pythonPath, "-m", "pip", "install", "--index-url",
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
        private val OS_NAME: String = System.getProperty("os.name").lowercase(Locale.getDefault())
        private const val MAX_RETRIES = 3
        private val SOURCES = arrayOf("https://pypi.org/simple", "https://pypi.tuna.tsinghua.edu.cn/simple")
    }
}


object IOUtils {
    fun copyResourceDirToPath(resourceDir: String, outputPath: String) {
        val uri = javaClass.getResource(resourceDir)!!.toURI()
        val path = if (uri.scheme == "jar") {
            val fileSystem = FileSystems.newFileSystem(uri, emptyMap<String, Any>())
            fileSystem.getPath("/$resourceDir")
        } else {
            Paths.get(uri)
        }

        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val relativeDir = dir.toString().substring(path.toString().length)
                val targetPath = Paths.get(outputPath, relativeDir)
                return if (!Files.exists(targetPath)) {
                    Files.createDirectory(targetPath)
                    FileVisitResult.CONTINUE
                } else {
                    if (relativeDir == "") FileVisitResult.CONTINUE else FileVisitResult.SKIP_SUBTREE
                }
            }

            @Throws(IOException::class)
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val relativePath = file.toString().substring(path.toString().length)
                val targetFilePath = Paths.get(outputPath, relativePath)
                if (!Files.exists(targetFilePath)) {
                    Files.copy(file, targetFilePath)
                }
                return FileVisitResult.CONTINUE
            }
        })
    }
}
