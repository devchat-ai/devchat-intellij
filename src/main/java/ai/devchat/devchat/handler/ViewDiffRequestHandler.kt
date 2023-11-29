package ai.devchat.devchat.handler

import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import com.alibaba.fastjson.JSONObject
import com.intellij.psi.PsiFileFactory

class ViewDiffRequestHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    override fun executeAction() {
        Log.info("Handling view diff request")
        val callbackFunc = metadata!!.getString("callback")
        val diffContent = payload!!.getString("content")
        val psiFileFactory = PsiFileFactory.getInstance(
            devChatActionHandler.project
        )
        //        PsiFile psiFile = psiFileFactory.createFileFromText("yourFileName.java", , diffContent);
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
