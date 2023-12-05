package ai.devchat.idea.storage

import com.alibaba.fastjson.JSONObject


object ActiveConversation {
    private var messages: MutableList<JSONObject>? = null
    var topic: String? = null

    fun reset(topic: String? = null, messages: List<JSONObject>? = null) {
        this.topic = topic
        this.messages = messages?.toMutableList()
    }

    fun addMessage(message: JSONObject) {
        messages?.add(message)
    }

    fun findMessage(hash: String): JSONObject? {
        return messages?.find{it.getString("hash") == hash}
    }

    fun deleteMessage(hash: String) {
        val idx = messages?.indexOfFirst {it.getString("hash") == hash} ?: -1
        if (idx >= 0) {
            messages?.slice(0..<idx)
        }
    }

    val lastMessage get() = messages?.lastOrNull()

    fun getMessages(page: Int = 1, pageSize: Int = 20): List<JSONObject>? {
        if (this.messages == null) {
            return null
        }
        return this.messages!!.asReversed().slice(
            pageSize * (page - 1)..<pageSize * page
        ).reversed()
    }
}

