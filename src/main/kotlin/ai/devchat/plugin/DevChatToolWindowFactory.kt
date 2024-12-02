package ai.devchat.plugin

import ai.devchat.common.Log
import ai.devchat.core.DevChatClient
import ai.devchat.core.DevChatWrapper
import ai.devchat.storage.ActiveConversation
import ai.devchat.storage.RecentFilesTracker
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.content.Content
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefApp
import kotlinx.coroutines.*
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import ai.devchat.common.*
import ai.devchat.storage.CONFIG
import ai.devchat.storage.DevChatState
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import java.io.BufferedReader
import java.io.File
import java.nio.file.Paths
import ai.devchat.common.Constants.ASSISTANT_NAME_EN
import kotlin.system.measureTimeMillis
import ai.devchat.installer.PythonEnvManager
import com.intellij.openapi.application.ApplicationManager


@Service(Service.Level.PROJECT)
class DevChatService(project: Project) {
    var activeConversation: ActiveConversation? = null
    var browser: Browser? = null
    var localServicePort: Int? = null
    var ideServicePort: Int? = null
    var client: DevChatClient? = null
    var wrapper: DevChatWrapper? = null
    var pythonReady: Boolean = false
    var uiLoaded: Boolean = false
}

class DevChatToolWindowFactory : ToolWindowFactory, DumbAware, Disposable {
    private val minimalPythonVersion: String = "3.8"
    private val defaultPythonVersion: String = "3.11.4"
    private val devChatVersion = PluginManagerCore.getPlugin(
        PluginId.getId(DevChatBundle.message("plugin.id"))
    )?.version

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        Log.info("-----------> DevChatToolWindowFactory.createToolWindowContent started")

        try {
            project.service<RecentFilesTracker>()
            Log.info("-----------> RecentFilesTracker service initialized")

            val devChatService = project.getService(DevChatService::class.java)
            Log.info("-----------> DevChatService obtained")

            val panel = JPanel(BorderLayout())
            val content = toolWindow.contentManager.factory.createContent(panel, "", false)
            Disposer.register(content, this)
            Log.info("-----------> Content created and disposers registered")

            val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                Log.error("-----------> Initialization failed: ${throwable.message}")
                Notifier.error("$ASSISTANT_NAME_EN initialization has failed. Please check the logs for more details.")
            }

            CoroutineScope(Dispatchers.Default + exceptionHandler).launch {
                try {
                    ApplicationManager.getApplication().invokeLater {
                        Notifier.info("Starting $ASSISTANT_NAME_EN initialization...")
                    }

                    // Step 1: Setup Python and install tools
                    ApplicationManager.getApplication().invokeLater {
                        Notifier.info("Checking and installing Python environment. This may take a while...")
                    }
                    var workflowCopied = setupPythonAndTools(project, devChatService)
                    Log.info("-----------> setup python and install tools")

                    // Step 2: Start local service
                    val localService = startLocalService(project, content, devChatService)
                    if (localService == null) {
                        Log.error("-----------> Failed to start local service")
                        ApplicationManager.getApplication().invokeLater {
                            Notifier.error("$ASSISTANT_NAME_EN initialization has failed. Please check the logs for more details.")
                        }
                        return@launch
                    }
                    Log.info("-----------> start local service")

                    // Step 3: Start IDE server
                    val ideServer = IDEServer(project).start()
                    Disposer.register(content, ideServer)
                    devChatService.ideServicePort = ideServer.port
                    Log.info("-----------> IDE server started on port ${ideServer.port}")

                    // Step 4: Create and setup DevChatWrapper
                    val wrapper = DevChatWrapper(project)
                    Disposer.register(content, wrapper)
                    devChatService.wrapper = wrapper
                    Log.info("-----------> DevChatWrapper created and registered")

                    // Step 5: Create ActiveConversation
                    devChatService.activeConversation = ActiveConversation()
                    Log.info("-----------> ActiveConversation created")

                    // Step 6: Update workflows
                    if (!workflowCopied) {
                        updateWorkflows(devChatService.client!!)
                    }
                    Log.info("-----------> update workflows")

                    // Step 7: Initialize and add browser component
                    ApplicationManager.getApplication().invokeAndWait {
                        try {
                            initializeBrowser(project, devChatService, panel, content)
                            toolWindow.contentManager.addContent(content)
                            Log.info("-----------> Content added to tool window")
                        } catch (e: Exception) {
                            Log.error("Error initializing browser: ${e.message}")
                            Notifier.error("Failed to initialize browser. Please check the logs for more details.")
                        }
                    }
                    Log.info("-----------> initializeBrowser")

                    // Step 8: Update DevChatState
                    DevChatState.instance.lastVersion = devChatVersion
                    Log.info("-----------> DevChatState updated with new version")

                    ApplicationManager.getApplication().invokeLater {
                        Notifier.info("$ASSISTANT_NAME_EN initialization has completed successfully.")
                    }
                } catch (e: Exception) {
                    Log.error("Failed to initialize $ASSISTANT_NAME_EN: $e\n" + e.stackTrace.joinToString("\n"))
                    ApplicationManager.getApplication().invokeLater {
                        Notifier.error("$ASSISTANT_NAME_EN initialization has failed. Please check the logs for more details.")
                    }
                }
            }
        } catch (e: Exception) {
            Log.error("-----------> Exception during DevChatToolWindowFactory initialization: ${e.message}")
            Notifier.error("Failed to initialize $ASSISTANT_NAME_EN. Please check the logs for more details.")
        }
    }

    private suspend fun setupPythonAndTools(project: Project, devChatService: DevChatService): Boolean {
        var workflowCopied = false
            Log.info("Start configuring the $ASSISTANT_NAME_EN CLI environment.")
        val executionTime = measureTimeMillis {
            try {
                Log.info("Creating PythonEnvManager")
                val pythonEnvManager = PythonEnvManager()
                Log.info("Setting up Python")
                setupPython(pythonEnvManager, devChatService)
                Log.info("Installing workflows")
                workflowCopied = installWorkflows()
                Log.info("Installing tools")
                CoroutineScope(Dispatchers.IO).launch {
                    installTools()
                }
            } catch (e: Exception) {
                Log.error("Error in setupPythonAndTools: ${e.message}")
            }
        }
        Log.info("-----------> Time took to setup python and workflows: ${executionTime/1000} s")
        return workflowCopied
    }

    private suspend fun setupPython(envManager: PythonEnvManager, devChatService: DevChatService) {
        val overwrite = devChatVersion != DevChatState.instance.lastVersion
        Log.info("start to copy site-packages files")
        PathUtils.copyResourceDirToPath("/tools/site-packages", PathUtils.sitePackagePath, overwrite)
        Log.info("copy site-packages files finished")
        var t1 = CONFIG["python_for_chat"]
        Log.info("Load config file finished")
        "python_for_chat".let { k ->
            if (OSInfo.isWindows) {
                val installDir = Paths.get(PathUtils.workPath, "python-win").toString()
                Log.info("start to copy python-win files")
                try {
                    PathUtils.copyResourceDirToPath("/tools/python-3.11.6-embed-amd64", installDir, overwrite)
                } catch (e: Exception) {
                    Log.error("Failed to copy python-win files: ${e.message}")
                }
                Log.info("copy python-win files finished")
                val pthFile = File(Paths.get(installDir, "python311._pth").toString())
                val pthContent = pthFile.readText().replace(
                    "%PYTHONPATH%",
                    "${PathUtils.sitePackagePath}${System.lineSeparator()}${PathUtils.workflowPath}"
                )
                pthFile.writeText(pthContent)
                CONFIG[k] = Paths.get(installDir, "python.exe").toString()
            } else if ((CONFIG[k] as? String).isNullOrEmpty()) {
                CONFIG[k] = getSystemPython(minimalPythonVersion) ?: envManager.createEnv(
                    "devchat", defaultPythonVersion
                ).pythonCommand
            }
        }
        devChatService.pythonReady = true
    }

    private suspend fun installWorkflows(): Boolean {
        Log.info("Start checking and copying workflows files")
        val workflowMericoDir = File(PathUtils.workflowMericoPath)
        var updatePublicWorkflows = CONFIG["update_public_workflow"]
        val overwrite = devChatVersion != DevChatState.instance.lastVersion

        var workflowCopied = false;
        if ((overwrite && updatePublicWorkflows == false) || !workflowMericoDir.exists() || !workflowMericoDir.isDirectory || workflowMericoDir.listFiles()?.isEmpty() == true) {
            Log.info("Workflow Merico directory is missing or empty. Creating and populating it.")
            PathUtils.copyResourceDirToPath("/workflows", PathUtils.workflowPath, true)
            workflowCopied = true;
        } else {
            Log.info("Workflow Merico directory exists and is not empty. Skipping copy.")
        }

        Log.info("Finished checking and copying workflows files")
        return workflowCopied;
    }

    private suspend fun installTools() {
        val overwrite = devChatVersion != DevChatState.instance.lastVersion
        Log.info("start to copy tools files")
        PathUtils.copyResourceDirToPath(
            "/tools/code-editor/${PathUtils.codeEditorBinary}",
            Paths.get(PathUtils.toolsPath, PathUtils.codeEditorBinary).toString(),
            overwrite
        )
        PathUtils.copyResourceDirToPath(
            "/tools/sonar-rspec",
            Paths.get(PathUtils.toolsPath, "sonar-rspec").toString(),
            overwrite
        )
        Log.info("copy tools files finished")
    }

    private suspend fun startLocalService(project: Project, content: Content, devChatService: DevChatService): LocalService? {
        return withTimeoutOrNull(60000) {
            val localService = LocalService(project).start()
            localService?.let {
                Disposer.register(content, it)
                devChatService.localServicePort = it.port!!
                devChatService.client = DevChatClient(project, it.port!!)
                Log.info("-----------> DevChat client ready on port ${it.port}")
                it
            }
        }
    }

    private fun initializeBrowser(project: Project, devChatService: DevChatService, panel: JPanel, content: Content) {
        val browser = Browser(project)
        devChatService.browser = browser
        Log.info("-----------> Browser created and set in DevChatService")

        // Register the browser to be disposed with the content
        Disposer.register(content, browser)
        Log.info("-----------> Browser registered for disposal")

        if (!JBCefApp.isSupported()) {
            Log.error("JCEF is not supported.")
            panel.add(JLabel("JCEF is not supported", SwingConstants.CENTER))
        } else {
            panel.add(browser.jbCefBrowser.component, BorderLayout.CENTER)
            Log.info("-----------> JCEF browser component added to panel")
        }
        panel.border = BorderFactory.createMatteBorder(0, 1, 0, 1, JBColor.LIGHT_GRAY)
    }

    private fun updateWorkflows(client: DevChatClient) {
        try {
            client.updateWorkflows()
            client.updateCustomWorkflows()
            Log.info("-----------> Workflows updated successfully")
        } catch (e: Exception) {
            Log.warn("-----------> Failed to update workflows: ${e.message}")
        }
    }

    private fun getSystemPython(minimalVersion: String): String? {
        val (minMajor, minMinor) = minimalVersion.split(".").take(2).map(String::toInt)
        val process = ProcessBuilder(
            if (OSInfo.isWindows) listOf("cmd","/c","python --version")
            else listOf("/bin/bash","-c", "python3 --version")
        ).start()
        val output = process.inputStream.bufferedReader().use(BufferedReader::readLine)
        process.waitFor()

        return output?.let {
            val (major, minor) = it.split(" ")[1].split(".").take(2).map(String::toInt)
            val cmd = "import sys; print(sys.executable)"
            val pb = ProcessBuilder(
                if (OSInfo.isWindows) listOf("cmd","/c","python -c \"$cmd\"")
                else listOf("/bin/bash","-c", "python3 -c \"$cmd\"")
            )
            pb.environment()["PYTHONUTF8"] = "1"
            val proc = pb.start()
            val python = proc.inputStream.bufferedReader().use(BufferedReader::readText).trim()
            val errs = proc.errorStream.bufferedReader().use(BufferedReader::readText)
            val exitCode = proc.waitFor()
            if (exitCode != 0) {
                Log.warn("Failed to get system: $errs")
            }
            when {
                major > minMajor -> python
                major == minMajor && minor >= minMinor -> python
                else -> null
            }
        }
    }

    override fun dispose() {
        Log.info("-----------> DevChatToolWindowFactory disposed")
    }
}