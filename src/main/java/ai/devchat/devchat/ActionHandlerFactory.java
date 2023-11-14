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
