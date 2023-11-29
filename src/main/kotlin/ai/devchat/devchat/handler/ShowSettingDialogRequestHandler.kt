package ai.devchat.devchat.handler

import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil

class ShowSettingDialogRequestHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    override fun executeAction() {
        Log.info("Handling show setting dialog request.")
        val dataContext = DataContext { dataId ->
            if (CommonDataKeys.PROJECT.name == dataId) {
                devChatActionHandler.project
            } else null
        }
        val actionManager = ActionManager.getInstance()
        val settingsAction = actionManager.getAction("ShowSettings")
        val event = AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, dataContext)
        ApplicationManager.getApplication().invokeLater { settingsAction.actionPerformed(event) }
        val project = devChatActionHandler.project
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "DevChat")
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
