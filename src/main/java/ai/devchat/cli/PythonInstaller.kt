package ai.devchat.cli

import ai.devchat.common.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

class PythonInstaller(private val pythonPath: String) {
    private var sourceIndex = 0
    fun install(packageName: String, packageVersion: String) {
        var retries = 0
        while (retries < MAX_RETRIES) {
            try {
                val pb = ProcessBuilder(
                    Arrays.asList(
                        pythonPath, "-m",
                        "pip", "install", "--index-url", SOURCES[sourceIndex], "$packageName==$packageVersion"
                    )
                )
                val command = java.lang.String.join(" ", pb.command())
                Log.info("Installing $packageName $packageVersion by: $command")
                pb.redirectErrorStream(true)
                val process = pb.start()

                // read output
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                reader.forEachLine { line -> Log.info(line) }
                process.waitFor()
                if (process.exitValue() == 0) {
                    break
                } else {
                    // switch source and retry
                    sourceIndex = (sourceIndex + 1) % SOURCES.size
                    retries++
                    Log.info("Installation failed, retrying with " + SOURCES[sourceIndex])
                }
            } catch (e: IOException) {
                throw RuntimeException("Failed to install $packageName $packageVersion", e)
            } catch (e: InterruptedException) {
                throw RuntimeException("Failed to install $packageName $packageVersion", e)
            }
        }
        if (retries == MAX_RETRIES) {
            Log.error("Python package installation failed. Max retries exceeded.")
        } else {
            Log.info("Python package installation succeeded.")
        }
    }

    companion object {
        private const val MAX_RETRIES = 3
        private val SOURCES = arrayOf("https://pypi.org/simple", "https://pypi.tuna.tsinghua.edu.cn/simple")
    }
}
