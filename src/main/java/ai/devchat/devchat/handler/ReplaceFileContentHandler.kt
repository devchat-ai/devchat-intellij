package ai.devchat.devchat.handler

import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileEditor.FileEditorManager

class ReplaceFileContentHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    override fun executeAction() {
        Log.info("Handling replace file content request")
        val project = devChatActionHandler.project
        val newFileContent = payload!!.getString("content")
        val callbackFunc = metadata!!.getString("callback")
        ApplicationManager.getApplication().invokeLater {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            val document = editor!!.document
            CommandProcessor.getInstance().executeCommand(
                project,
                { ApplicationManager.getApplication().runWriteAction { document.setText(newFileContent) } },
                "ReplaceFileContentHandler",
                null
            )
            devChatActionHandler.sendResponse(
                DevChatActions.REPLACE_FILE_CONTENT_RESPONSE, callbackFunc
            ) { metadata: JSONObject, payload: JSONObject? -> metadata["status"] = "success" }
        }
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
