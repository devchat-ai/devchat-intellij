package ai.devchat.devchat

import ai.devchat.devchat.handler.*
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class ActionHandlerFactory {
    private val actionHandlerMap: Map<String, KClass<out ActionHandler>> =
        object : HashMap<String, KClass<out ActionHandler>>() {
            init {
                put(DevChatActions.SEND_MESSAGE_REQUEST, SendMessageRequestHandler::class)
                put(DevChatActions.SET_OR_UPDATE_KEY_REQUEST, SetOrUpdateKeyRequestHandler::class)
                put(DevChatActions.LIST_COMMANDS_REQUEST, ListCommandsRequestHandler::class)
                put(DevChatActions.LIST_CONVERSATIONS_REQUEST, ListConversationsRequestHandler::class)
                put(DevChatActions.LIST_TOPICS_REQUEST, ListTopicsRequestHandler::class)
                put(DevChatActions.INSERT_CODE_REQUEST, InsertCodeRequestHandler::class)
                put(DevChatActions.REPLACE_FILE_CONTENT_REQUEST, ReplaceFileContentHandler::class)
                put(DevChatActions.VIEW_DIFF_REQUEST, ViewDiffRequestHandler::class)
                put(DevChatActions.LIST_CONTEXTS_REQUEST, ListContextsRequestHandler::class)
                put(DevChatActions.LIST_MODELS_REQUEST, ListModelsRequestHandler::class)
                put(DevChatActions.ADD_CONTEXT_REQUEST, AddContextRequestHandler::class)
                put(DevChatActions.GET_KEY_REQUEST, GetKeyRequestHandler::class)
                put(DevChatActions.COMMIT_CODE_REQUEST, CommitCodeRequestHandler::class)
                put(DevChatActions.GET_SETTING_REQUEST, GetSettingRequestHandler::class)
                put(DevChatActions.UPDATE_SETTING_REQUEST, UpdateSettingRequestHandler::class)
                put(DevChatActions.SHOW_SETTING_DIALOG_REQUEST, ShowSettingDialogRequestHandler::class)
                put(DevChatActions.DELETE_LAST_CONVERSATION_REQUEST, DeleteLastConversationRequestHandler::class)
                put(DevChatActions.DELETE_TOPIC_REQUEST, DeleteTopicRequestHandler::class)
            }
        }

    fun createActionHandler(action: String): ActionHandler {
        val handlerClass = actionHandlerMap[action]
        return if (handlerClass != null) {
            try {
                handlerClass.primaryConstructor!!.call(DevChatActionHandler.instance)
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
