package ai.devchat.core.handlers

import ai.devchat.common.ProjectUtils
import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.options.ShowSettingsUtil


class ShowSettingDialogRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.SHOW_SETTING_DIALOG_REQUEST
    override fun action() {
        ShowSettingsUtil.getInstance().showSettingsDialog(ProjectUtils.project, "DevChat")
    }
}
