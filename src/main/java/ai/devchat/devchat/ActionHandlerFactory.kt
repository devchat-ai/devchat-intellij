package ai.devchat.devchat

import ai.devchat.devchat.handler.*
import java.lang.reflect.InvocationTargetException

class ActionHandlerFactory {
    private val actionHandlerMap: Map<String, Class<out ActionHandler>> =
        object : HashMap<String?, Class<out ActionHandler?>?>() {
            init {
                put(DevChatActions.SEND_MESSAGE_REQUEST, SendMessageRequestHandler::class.java)
                put(DevChatActions.SET_OR_UPDATE_KEY_REQUEST, SetOrUpdateKeyRequestHandler::class.java)
                put(DevChatActions.LIST_COMMANDS_REQUEST, ListCommandsRequestHandler::class.java)
                put(DevChatActions.LIST_CONVERSATIONS_REQUEST, ListConversationsRequestHandler::class.java)
                put(DevChatActions.LIST_TOPICS_REQUEST, ListTopicsRequestHandler::class.java)
                put(DevChatActions.INSERT_CODE_REQUEST, InsertCodeRequestHandler::class.java)
                put(DevChatActions.REPLACE_FILE_CONTENT_REQUEST, ReplaceFileContentHandler::class.java)
                put(DevChatActions.VIEW_DIFF_REQUEST, ViewDiffRequestHandler::class.java)
                put(DevChatActions.LIST_CONTEXTS_REQUEST, ListContextsRequestHandler::class.java)
                put(DevChatActions.LIST_MODELS_REQUEST, ListModelsRequestHandler::class.java)
                put(DevChatActions.ADD_CONTEXT_REQUEST, AddContextRequestHandler::class.java)
                put(DevChatActions.GET_KEY_REQUEST, GetKeyRequestHandler::class.java)
                put(DevChatActions.COMMIT_CODE_REQUEST, CommitCodeRequestHandler::class.java)
                put(DevChatActions.GET_SETTING_REQUEST, GetSettingRequestHandler::class.java)
                put(DevChatActions.UPDATE_SETTING_REQUEST, UpdateSettingRequestHandler::class.java)
                put(DevChatActions.SHOW_SETTING_DIALOG_REQUEST, ShowSettingDialogRequestHandler::class.java)
                put(DevChatActions.DELETE_LAST_CONVERSATION_REQUEST, DeleteLastConversationRequestHandler::class.java)
            }
        }

    fun createActionHandler(action: String): ActionHandler {
        val handlerClass = actionHandlerMap[action]
        return if (handlerClass != null) {
            try {
                val constructor = handlerClass
                    .getConstructor(DevChatActionHandler::class.java)
                constructor.newInstance(DevChatActionHandler.Companion.getInstance())
            } catch (e: NoSuchMethodException) {
                throw RuntimeException("Failed to instantiate action handler for: $action", e)
            } catch (e: InstantiationException) {
                throw RuntimeException("Failed to instantiate action handler for: $action", e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException("Failed to instantiate action handler for: $action", e)
            } catch (e: InvocationTargetException) {
                throw RuntimeException("Failed to instantiate action handler for: $action", e)
            }
        } else {
            throw RuntimeException("Action handler not found: $action")
        }
    }
}
