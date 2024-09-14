package ai.devchat.core

import ai.devchat.core.handlers.*
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class ActionHandlerFactory {
    private val actionHandlerMap: Map<String, KClass<out ActionHandler>> = mapOf(
        DevChatActions.SEND_MESSAGE_REQUEST to SendMessageRequestHandler::class,
        DevChatActions.REGENERATION_REQUEST to SendMessageRequestHandler::class,
        DevChatActions.LIST_COMMANDS_REQUEST to ListCommandsRequestHandler::class,
        DevChatActions.LOAD_CONVERSATIONS_REQUEST to LoadConversationRequestHandler::class,
        DevChatActions.LOAD_HISTORY_MESSAGES_REQUEST to LoadHistoryMessagesRequestHandler::class,
        DevChatActions.OPEN_LINK_REQUEST to OpenLinkRequestHandler::class,
        DevChatActions.LIST_TOPICS_REQUEST to ListTopicsRequestHandler::class,
        DevChatActions.INSERT_CODE_REQUEST to InsertCodeRequestHandler::class,
        DevChatActions.NEW_SRC_FILE_REQUEST to NewSrcFile::class,
        DevChatActions.REPLACE_FILE_CONTENT_REQUEST to ReplaceFileContentHandler::class,
        DevChatActions.VIEW_DIFF_REQUEST to ViewDiffRequestHandler::class,
        DevChatActions.GET_IDE_SERVICE_PORT_REQUEST to GetIDEServicePortRequestHandler::class,
        DevChatActions.GET_SETTING_REQUEST to GetSettingRequestHandler::class,
        DevChatActions.UPDATE_SETTING_REQUEST to UpdateSettingRequestHandler::class,
        DevChatActions.GET_SERVER_SETTINGS_REQUEST to GetServerSettingsRequestHandler::class,
        DevChatActions.UPDATE_SERVER_SETTINGS_REQUEST to UpdateServerSettingsRequestHandler::class,
        DevChatActions.INPUT_REQUEST to InputRequestHandler::class,
        DevChatActions.DELETE_LAST_CONVERSATION_REQUEST to DeleteLastConversationRequestHandler::class,
        DevChatActions.DELETE_TOPIC_REQUEST to DeleteTopicRequestHandler::class,
        DevChatActions.STOP_GENERATION_REQUEST to StopGenerationRequestHandler::class,
    )

    fun createActionHandler(project: Project, action: String, metadata: JSONObject, payload: JSONObject): ActionHandler? {
        val handlerClass = actionHandlerMap[action] ?: throw RuntimeException("Action handler not found: $action")
        return try {
            handlerClass.primaryConstructor!!.call(project, action, metadata, payload)
        } catch (e: Exception) {
            // Catch any exception since the handling is the same
            throw RuntimeException("Failed to instantiate action handler for: $action", e)
        }
    }
}
