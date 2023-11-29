package ai.devchat.idea.storage

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@Service
@State(name = "ai.devchat.devchat.DeletedTopicsState", storages = [Storage("deletedTopics.xml")])
class DeletedTopicsState : PersistentStateComponent<DeletedTopicsState?> {
    var deletedTopicHashes: List<String> = ArrayList()
    override fun getState(): DeletedTopicsState? {
        return this
    }

    override fun loadState(state: DeletedTopicsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: DeletedTopicsState
            get() = ApplicationManager.getApplication().getService(DeletedTopicsState::class.java)
    }
}
