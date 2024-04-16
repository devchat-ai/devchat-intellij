package ai.devchat.plugin.completion.agent

import ai.devchat.storage.CONFIG
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader

class Agent(val endpoint: String? = null, private val apiKey: String? = null) {
  private val logger = Logger.getInstance(Agent::class.java)
  private val gson = Gson()
  private val httpClient = OkHttpClient()

  data class Payload(
    val prompt: String,
    val temperature: Float = 0.2F,
    @SerializedName("top_p") val topP: Float = 0.7F,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val seed: Int = 42,
    val bad: Nothing? = null,
    val stop: List<String> = listOf("<file_sep>", "```", "\n\n"),
    val stream: Boolean = true
  )

  data class CompletionResponseChunk(val id: String, val choices: List<Choice>) {
    data class Choice(val index: Int, val delta: String, @SerializedName("finish_reason") val finishReason: Nothing?)
  }

  data class CompletionRequest(
    val filepath: String,
    val language: String,
    val text: String,
    val position: Int,
    val manually: Boolean?,
  )

  data class CompletionResponse(val id: String, val choices: List<Choice>) {
    data class Choice(val index: Int, val text: String, val replaceRange: Range, ) {
      data class Range(val start: Int, val end: Int)
    }
  }

  data class LogEventRequest(
    val type: EventType,
    @SerializedName("completion_id") val completionId: String,
    @SerializedName("choice_index") val choiceIndex: Int,
    @SerializedName("select_kind") val selectKind: SelectKind? = null,
    @SerializedName("view_id") val viewId: String? = null,
    val elapsed: Int? = null,
  ) {
    enum class EventType {
      @SerializedName("view") VIEW,
      @SerializedName("select") SELECT,
      @SerializedName("dismiss") DISMISS,
    }

    enum class SelectKind {
      @SerializedName("line") LINE,
    }
  }

  suspend fun provideCompletions(
    completionRequest: CompletionRequest
  ): CompletionResponse? = suspendCancellableCoroutine { continuation ->
    if (apiKey.isNullOrEmpty()) throw IllegalArgumentException("Require api key")
    val offset = completionRequest.position
    val requestBuilder = Request.Builder()
      .url(endpoint!!)
      .post(
        gson.toJson(Payload(completionRequest.text)).toRequestBody(
          "application/json; charset=utf-8".toMediaType()
        )
      )
    requestBuilder.addHeader("Authorization", "Bearer $apiKey")
    requestBuilder.addHeader("Accept", "text/event-stream")
    requestBuilder.addHeader("Content-Type", "application/json")
    httpClient.newCall(requestBuilder.build()).execute().use { response ->
      if (!response.isSuccessful) {
        throw IllegalArgumentException("Unexpected code $response")
      }
      response.body?.let {body -> BufferedReader(body.charStream()).use { reader ->
        var chunk: String?
        while (reader.readLine().also { chunk = it } != null) {
          if (!chunk!!.startsWith("data:")) {
            logger.info("Unexpected data: $chunk")
            break
          }

          val jsonData = chunk!!.drop(5).trim()
          if (jsonData == "[DONE]") break
          try {
            val data = gson.fromJson(jsonData, CompletionResponseChunk::class.java)
            val replaceRange = CompletionResponse.Choice.Range(start = offset, end = offset)
            continuation.resumeWith(Result.success(CompletionResponse(
              id = data.id,
              choices = listOf(CompletionResponse.Choice(
                index = 0, text = data.choices[0].delta, replaceRange = replaceRange
              ))
            )))
          } catch (e: Exception) {
            logger.info("Received: $chunk")
            logger.error("JSON Parsing Error: ${e.message}")
          }
        }
      }}
    }
    continuation.invokeOnCancellation {
      logger.debug("Agent request cancelled")
    }
  }

  suspend fun postEvent(logEventRequest: LogEventRequest): Unit = suspendCancellableCoroutine {
    val devChatEndpoint = CONFIG["providers.devchat.api_base"]
    val devChatAPIKey = CONFIG["providers.devchat.api_key"]
    val requestBuilder = Request.Builder()
      .url("$devChatEndpoint/complete_events")
      .post(
        gson.toJson(logEventRequest).toRequestBody(
          "application/json; charset=utf-8".toMediaType()
        )
      )
    requestBuilder.addHeader("Authorization", "Bearer $devChatAPIKey")
    requestBuilder.addHeader("Content-Type", "application/json")
    try {
      httpClient.newCall(requestBuilder.build()).execute().use { response ->
        logger.info(response.toString())
      }
    } catch (e: Exception) {
      logger.warn(e)
    }
  }
}