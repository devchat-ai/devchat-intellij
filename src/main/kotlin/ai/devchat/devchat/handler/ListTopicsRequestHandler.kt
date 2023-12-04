package ai.devchat.devchat.handler

import ai.devchat.cli.DevChatWrapper
import ai.devchat.common.DevChatPathUtil.devchatBinPath
import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.storage.DeletedTopicsState
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject

class ListTopicsRequestHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    override fun executeAction() {
        Log.info("Handling list topics request")
        val callbackFunc = metadata!!.getString("callback")
        try {
            val devchatWrapper = DevChatWrapper(devchatBinPath)
            /*
             * topics format:
             * [
             * {
             * "root_prompt": {
             * "user": "Daniel Hu <tao.hu@merico.dev>",
             * "date": 1698828624,
             * "context": [
             * {
             * "content":
             * "{\"languageId\":\"python\",\"path\":\"a.py\",\"startLine\":0,\"content\":\"adkfjj\\n\"}",
             * "role": "system"
             * }
             * ],
             * "request": "hello",
             * "responses": [
             * "Hi there! How can I assist you with Python today?"
             * ],
             * "request_tokens": 46,
             * "response_tokens": 22,
             * "hash": "596cf7c60a936e33409c71b67ba7f9903886bbeb7c7d2aacf6d1556b0831f04b",
             * "parent": null
             * },
             * "latest_time": 1698828867,
             * "title": null,
             * "hidden": false
             * }
             * ]
             */
            val topics = devchatWrapper.topicList

            // Get deleted topics hash list
            val deletedTopicsState = DeletedTopicsState.instance
            val deletedTopicHashes = deletedTopicsState.deletedTopicHashes

            // Filter out deleted topics
            val filteredTopics = JSONArray()
            // remove request_tokens and response_tokens in the topics object, then update
            // title field.
            for (i in topics.indices) {
                val topic = topics.getJSONObject(i)
                val rootPrompt = topic.getJSONObject("root_prompt")
                val topicHash = rootPrompt.getString("hash")
                if (!deletedTopicHashes.contains(topicHash)) {
                    // set title = root_prompt.request + "-" + root_prompt.responses[0]
                    val title = (rootPrompt.getString("request") + "-"
                            + rootPrompt.getJSONArray("responses").getString(0))
                    rootPrompt["title"] = title
                    filteredTopics.add(topic)
                }
            }
            devChatActionHandler.sendResponse(
                DevChatActions.LIST_TOPICS_RESPONSE,
                callbackFunc
            ) { metadata: JSONObject, payload: JSONObject ->
                metadata["status"] = "success"
                metadata["error"] = ""
                payload["topics"] = topics
            }
        } catch (e: Exception) {
            Log.error("Exception occrred while executing DevChat command. Exception message: " + e.message)
            devChatActionHandler.sendResponse(
                DevChatActions.LIST_TOPICS_RESPONSE,
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
