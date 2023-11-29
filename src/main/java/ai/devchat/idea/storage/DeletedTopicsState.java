package ai.devchat.idea.storage;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(name = "ai.devchat.devchat.DeletedTopicsState", storages = @Storage("deletedTopics.xml"))
public class DeletedTopicsState implements PersistentStateComponent<DeletedTopicsState> {
    public List<String> deletedTopicHashes = new ArrayList<>();

    public static DeletedTopicsState getInstance() {
        return ServiceManager.getService(DeletedTopicsState.class);
    }

    @Nullable
    @Override
    public DeletedTopicsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull DeletedTopicsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
