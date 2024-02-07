package ai.devchat.idea.storage

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

enum class ToolWindowState {
    SHOWN,
    HIDDEN,
}

@Service
@State(name = "ai.devchat.DevChatState", storages = [Storage("DevChatState.xml")])
class DevChatState : PersistentStateComponent<DevChatState?> {
    var deletedTopicHashes: List<String> = ArrayList()
    var lastToolWindowState: String = ToolWindowState.SHOWN.name
    override fun getState(): DevChatState {
        return this
    }

    override fun loadState(state: DevChatState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: DevChatState
            get() = ApplicationManager.getApplication().getService(DevChatState::class.java)
    }
}
