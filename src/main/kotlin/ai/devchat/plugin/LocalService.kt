package ai.devchat.plugin

import ai.devchat.common.Log
import ai.devchat.common.Notifier
import ai.devchat.common.PathUtils
import ai.devchat.storage.CONFIG
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.OSProcessUtil.killProcessTree
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.io.BaseOutputReader
import java.net.ServerSocket


class LocalService(project: Project) {
    var port: Int? = null
    val workspace: String? = project.basePath
    private var processHandler: OSProcessHandler? = null
    private var isShutdownHookRegistered = false

    fun start(): LocalService {
        ServerSocket(0).use {
             port = it.localPort
        }
        val commandLine = GeneralCommandLine()
            .withExePath(CONFIG["python_for_chat"] as String)
            .withParameters(PathUtils.localServicePath)
            .withWorkDirectory(workspace)
            .withEnvironment("PYTHONPATH", PathUtils.pythonPath)
            .withEnvironment("DC_SVC_PORT", port.toString())
            .withEnvironment("DC_SVC_WORKSPACE", workspace ?: "")
        processHandler = object: OSProcessHandler(commandLine) {
            override fun readerOptions(): BaseOutputReader.Options {
                return BaseOutputReader.Options.forMostlySilentProcess()
            }
        }

        processHandler?.addProcessListener(object : ProcessAdapter() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                Log.info("[LocalService] ${event.text}")
            }

            override fun processTerminated(event: ProcessEvent) {
                Log.info("Local service terminated with exit code: ${event.exitCode}")
            }
        })

        // Register shutdown hook
        if (!isShutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(Thread { stop() })
            isShutdownHookRegistered = true
        }

        processHandler?.startNotify()
        Log.info("Local service started on port: $port")
        Notifier.info("Local service started at $port.")
        return this
    }

    fun stop() {
        processHandler?.let { handler ->
            Log.info("Stopping local service...")
            killProcessTree(handler.process)
            if (handler.waitFor(5000)) {
                Log.info("Local service stopped successfully")
            } else {
                Log.warn("Failed to stop local service, retrying...")
                killProcessTree(handler.process)
                handler.waitFor(3000)
            }
            processHandler = null
        } ?: Log.info("Local service is not running")
    }
}