package ai.devchat.devchat.handler

import ai.devchat.cli.DevChatWrapper
import ai.devchat.common.DevChatPathUtil
import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject

class ListConversationsRequestHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    override fun executeAction() {
        Log.info("Handling list conversations request")
        val callbackFunc = metadata!!.getString("callback")
        val topicHash = metadata!!.getString("topicHash")
        try {
            val devchatWrapper = DevChatWrapper(DevChatPathUtil.devchatBinPath)
            /* conversations' format:
            [
              {
                "user": "Daniel Hu <tao.hu@merico.dev>",
                "date": 1698828867,
                "context": [
                  {
                    "content": "{\"command\":\"ls -l\",\"content\":\"total 8\\n-rw-r--r--@ 1 danielhu  staff  7 Nov  1 16:49 a.py\\n\"}",
                    "role": "system"
                  }
                ],
                "request": "hello again",
                "responses": [
                  "Hello! How can I assist you today?"
                ],
                "request_tokens": 135,
                "response_tokens": 19,
                "hash": "ccbbd3f8c892277d3ea566545bb64b68ba3e34257f9b324551a52449f8f19e17",
                "parent": "596cf7c60a936e33409c71b67ba7f9903886bbeb7c7d2aacf6d1556b0831f04b"
              },
              {
                "user": "Daniel Hu <tao.hu@merico.dev>",
                "date": 1698828624,
                "context": [
                  {
                    "content": "{\"languageId\":\"python\",\"path\":\"a.py\",\"startLine\":0,\"content\":\"adkfjj\\n\"}",
                    "role": "system"
                  }
                ],
                "request": "hello",
                "responses": [
                  "Hi there! How can I assist you with Python today?"
                ],
                "request_tokens": 46,
                "response_tokens": 22,
                "hash": "596cf7c60a936e33409c71b67ba7f9903886bbeb7c7d2aacf6d1556b0831f04b",
                "parent": null
              }
            ]
            */
            val conversations = devchatWrapper.listConversationsInOneTopic(topicHash)
            // remove request_tokens and response_tokens in the conversations object
            for (i in conversations.indices) {
                val conversation = conversations.getJSONObject(i)
                conversation.remove("request_tokens")
                conversation.remove("response_tokens")
            }
            devChatActionHandler.sendResponse(
                DevChatActions.LIST_CONVERSATIONS_RESPONSE,
                callbackFunc
            ) { metadata: JSONObject, payload: JSONObject ->
                metadata["status"] = "success"
                metadata["error"] = ""
                payload["conversations"] = conversations
            }
        } catch (e: Exception) {
            Log.error("Exception occrred while executing DevChat command. Exception message: " + e.message)
            devChatActionHandler.sendResponse(
                DevChatActions.LIST_CONVERSATIONS_RESPONSE,
                callbackFunc
            ) { metadata: JSONObject, payload: JSONObject? ->
                metadata["status"] = "error"
                metadata["error"] = e.message
            }
        }
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
