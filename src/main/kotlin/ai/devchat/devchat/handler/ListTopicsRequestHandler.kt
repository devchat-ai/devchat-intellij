package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.storage.DevChatState
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject


class ListTopicsRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.LIST_TOPICS_RESPONSE
    override fun action() {
        val topics = wrapper.topicList
        val deletedTopicHashes = DevChatState.instance.deletedTopicHashes
        // Filter out deleted topics
        val filteredTopics = JSONArray()
        topics.forEachIndexed {i, _ ->
            val topic = topics.getJSONObject(i)
            val rootPrompt = topic.getJSONObject("root_prompt")
            if (rootPrompt.getString("hash") !in deletedTopicHashes) {
                val req = rootPrompt.getString("request")
                val res = rootPrompt.getJSONArray("responses").getString(0)
                rootPrompt["title"] = "$req-$res"
                filteredTopics.add(topic)
            }
        }
        send(payload= mapOf("topics" to filteredTopics))
    }
}