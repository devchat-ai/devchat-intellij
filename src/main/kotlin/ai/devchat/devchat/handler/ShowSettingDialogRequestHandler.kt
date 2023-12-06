package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil


class ShowSettingDialogRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.SHOW_SETTING_DIALOG_REQUEST
    override fun action() {
        val project = handler?.project
        val dataContext = DataContext { dataId -> project.takeIf { CommonDataKeys.PROJECT.name == dataId }}
        val settingsAction = ActionManager.getInstance().getAction("ShowSettings")
        val event = AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, dataContext)
        ApplicationManager.getApplication().invokeLater { settingsAction.actionPerformed(event) }
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "DevChat")
    }
}
