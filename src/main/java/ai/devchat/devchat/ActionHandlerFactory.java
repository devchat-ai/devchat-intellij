package ai.devchat.devchat;

import ai.devchat.devchat.handler.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ActionHandlerFactory {
    private final Map<String, Class<? extends ActionHandler>> actionHandlerMap = new HashMap<>() {
        {
            put(DevChatActions.SEND_MESSAGE_REQUEST, SendMessageRequestHandler.class);
            put(DevChatActions.SET_OR_UPDATE_KEY_REQUEST, SetOrUpdateKeyRequestHandler.class);
            put(DevChatActions.LIST_COMMANDS_REQUEST, ListCommandsRequestHandler.class);
            put(DevChatActions.LIST_CONVERSATIONS_REQUEST, ListConversationsRequestHandler.class);
            put(DevChatActions.LIST_TOPICS_REQUEST, ListTopicsRequestHandler.class);
            put(DevChatActions.INSERT_CODE_REQUEST, InsertCodeRequestHandler.class);
            put(DevChatActions.REPLACE_FILE_CONTENT_REQUEST, ReplaceFileContentHandler.class);
            put(DevChatActions.VIEW_DIFF_REQUEST, ViewDiffRequestHandler.class);
            put(DevChatActions.LIST_CONTEXTS_REQUEST, ListContextsRequestHandler.class);
            put(DevChatActions.LIST_MODELS_REQUEST, ListModelsRequestHandler.class);
            put(DevChatActions.ADD_CONTEXT_REQUEST, AddContextRequestHandler.class);
            put(DevChatActions.GET_KEY_REQUEST, GetKeyRequestHandler.class);
            put(DevChatActions.COMMIT_CODE_REQUEST, CommitCodeRequestHandler.class);
            put(DevChatActions.GET_SETTING_REQUEST, GetSettingRequestHandler.class);
            put(DevChatActions.UPDATE_SETTING_REQUEST, UpdateSettingRequestHandler.class);
            put(DevChatActions.SHOW_SETTING_DIALOG_REQUEST, ShowSettingDialogRequestHandler.class);
        }
    };

    public ActionHandler createActionHandler(String action) {
        Class<? extends ActionHandler> handlerClass = actionHandlerMap.get(action);
        if (handlerClass != null) {
            try {
                Constructor<? extends ActionHandler> constructor = handlerClass
                        .getConstructor(DevChatActionHandler.class);
                return constructor.newInstance(DevChatActionHandler.getInstance());
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
                     | InvocationTargetException e) {
                throw new RuntimeException("Failed to instantiate action handler for: " + action, e);
            }
        } else {
            throw new RuntimeException("Action handler not found: " + action);
        }
    }
}
