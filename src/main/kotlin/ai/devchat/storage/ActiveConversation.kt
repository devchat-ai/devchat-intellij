package ai.devchat.storage

import ai.devchat.core.ShortLog


object ActiveConversation {
    private var messages: MutableList<ShortLog>? = null
    var topic: String? = null

    fun reset(topic: String? = null, messages: List<ShortLog>? = null) {
        this.topic = topic
        this.messages = messages?.toMutableList()
    }

    fun addMessage(message: ShortLog) {
        messages?.add(message)
    }

    fun findMessage(hash: String): ShortLog? {
        return messages?.find{it.hash == hash}
    }

    fun deleteMessage(hash: String) {
        val idx = messages?.indexOfFirst {it.hash == hash} ?: -1
        if (idx >= 0) {
            messages?.slice(0..<idx)
        }
    }

    val lastMessage get() = messages?.lastOrNull()

    fun getMessages(page: Int = 1, pageSize: Int = 20): List<ShortLog>? {
        if (this.messages == null) {
            return null
        }
        val offset = pageSize * (page - 1)
        if (offset >= this.messages!!.size) {
            return null
        }
        var endIndex = offset + pageSize
        if (endIndex > this.messages!!.size) {
            endIndex = this.messages!!.size
        }

        return this.messages!!.asReversed().slice(
            offset..<endIndex
        ).reversed()
    }
}

