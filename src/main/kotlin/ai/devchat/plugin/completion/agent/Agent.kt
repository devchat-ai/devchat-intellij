package ai.devchat.plugin.completion.agent

import ai.devchat.storage.CONFIG
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


val CLOSING_BRACES = setOf("}", "]", ")")
const val MAX_CONTINUOUS_INDENT_COUNT = 4

class Agent(val scope: CoroutineScope) {
  private val logger = Logger.getInstance(Agent::class.java)
  private val gson = Gson()
  private val httpClient = OkHttpClient()
  private var prevCompletion: String? = null
  private var currentRequest: RequestInfo? = null
  private val nvapiEndpoint = "https://api.nvcf.nvidia.com/v2/nvcf/pexec/functions/6acada03-fe2f-4e4d-9e0a-e711b9fd1b59"
  private val defaultCompletionModel = "ollama/starcoder2:15b"

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
    data class Choice(
      val index: Int,
      val delta: String?,
      val text: String?,
      @SerializedName("finish_reason") val finishReason: String?
    )
  }

  data class CompletionRequest(
    val filepath: String,
    val language: String,
    val text: String,
    val position: Int,
    val manually: Boolean?,
  )

  data class CompletionResponse(
    val id: String,
    val model: String?,
    val choices: List<Choice>,
    val promptBuildingElapse: Long,
    val llmRequestElapse: Long
  ) {
    data class Choice(val index: Int, val text: String, val replaceRange: Range, ) {
      data class Range(val start: Int, val end: Int)
    }
  }

  data class LogEventRequest(
    val type: EventType,
    @SerializedName("completion_id") val completionId: String,
    @SerializedName("lines") val lines: Int,
    @SerializedName("length") val length: Int,
    @SerializedName("ide") val ide: String,
    @SerializedName("language") val language: String,
    @SerializedName("prompt_time") val promptBuildingElapse: Long,
    @SerializedName("llm_time") val llmRequestElapse: Long,
    @SerializedName("model") val model: String? = null,
    @SerializedName("cache_hit") val cacheHit: Boolean = false
  ) {
    enum class EventType {
      @SerializedName("view") VIEW,
      @SerializedName("select") SELECT,
    }
  }

  data class CodeCompletionChunk(val id: String, var text: String)

  data class RequestInfo(
    val filepath: String,
    val language: String,
    val upperPart: String,
    val lowerPart: String,
    val offset: Int,
    val currentLinePrefix: String,
    val currentLineSuffix: String,
    val lineBefore: String?,
    val lineAfter: String?,
    val currentIndent: Int
  ) {
    companion object {
      fun fromCompletionRequest(completionRequest: CompletionRequest): RequestInfo {
        val upperPart = completionRequest.text.substring(0, completionRequest.position)
        val lowerPart = completionRequest.text.substring(completionRequest.position)
        val currentLinePrefix = upperPart.substringAfterLast(LINE_SEPARATOR, upperPart)
        val currentLineSuffix = lowerPart.lineSequence().firstOrNull()?.second ?: ""
        val currentIndent = currentLinePrefix.takeWhile { it.isWhitespace() }.length
        val lineBefore = upperPart.lineSequenceReversed().firstOrNull { (_, l) ->
          l.endsWith(LINE_SEPARATOR) && l.trim().isNotEmpty()
        }?.second
        val lineAfter = lowerPart.lineSequence().withIndex().firstOrNull { (i, v) ->
          i > 0 && v.second.trim().isNotEmpty()
        }?.value?.second
        return RequestInfo(
          filepath = completionRequest.filepath,
          language = completionRequest.language,
          upperPart = upperPart,
          lowerPart = lowerPart,
          offset = completionRequest.position,
          currentLinePrefix = currentLinePrefix,
          currentLineSuffix = currentLineSuffix,
          lineBefore = lineBefore,
          lineAfter = lineAfter,
          currentIndent = currentIndent
        )
      }
    }
  }

  fun request(prompt: String): Flow<CodeCompletionChunk> {
    val nvapiKey = CONFIG["complete_key"] as? String
    return if (!nvapiKey.isNullOrEmpty()) requestNVAPI(prompt) else requestDevChatAPI(prompt)
  }

  private fun requestNVAPI(prompt: String): Flow<CodeCompletionChunk> = flow {
    val nvapiKey = CONFIG["complete_key"] as? String
    if (nvapiKey.isNullOrEmpty()) throw IllegalArgumentException("Require api key")
    val endingChunk = "data:[DONE]"
    val requestBody = gson.toJson(Payload(prompt)).toRequestBody("application/json; charset=utf-8".toMediaType())
    val requestBuilder = Request.Builder().url(nvapiEndpoint).post(requestBody)
    requestBuilder.addHeader("Authorization", "Bearer $nvapiKey")
    requestBuilder.addHeader("Accept", "text/event-stream")
    requestBuilder.addHeader("Content-Type", "application/json")
    httpClient.newCall(requestBuilder.build()).execute().use { response ->
      if (!response.isSuccessful) throw IllegalArgumentException("Unexpected code $response")
      response.body?.charStream()?.buffered()?.use {reader ->
        reader.lineSequence().asFlow()
          .filter {it.isNotEmpty()}
          .takeWhile { it.startsWith("data:") && it != endingChunk}
          .map { gson.fromJson(it.drop(5).trim(), CompletionResponseChunk::class.java) }
          .takeWhile {it != null}
          .collect { emit(CodeCompletionChunk(it.id, it.choices[0].delta!!)) }
      }
    }
  }

  private fun requestDevChatAPI(prompt: String): Flow<CodeCompletionChunk> = flow {
    val devChatEndpoint = CONFIG["providers.devchat.api_base"] as? String
    val devChatAPIKey = CONFIG["providers.devchat.api_key"] as? String
    val endpoint = "$devChatEndpoint/completions"
    val endingChunk = "[DONE]"
    val payload = mapOf(
      "model" to ((CONFIG["complete_model"] as? String) ?: defaultCompletionModel),
      "prompt" to prompt,
      "stream" to true,
      "stop" to listOf("<|endoftext|>", "<|EOT|>", "<file_sep>", "```", "/", "\n\n"),
      "temperature" to 0.2
    )
    val requestBody = gson.toJson(payload).toRequestBody("application/json; charset=utf-8".toMediaType())
    val requestBuilder = Request.Builder().url(endpoint).post(requestBody)
    requestBuilder.addHeader("Authorization", "Bearer $devChatAPIKey")
    requestBuilder.addHeader("Accept", "text/event-stream")
    requestBuilder.addHeader("Content-Type", "application/json")
    httpClient.newCall(requestBuilder.build()).execute().use { response ->
      if (!response.isSuccessful) throw IllegalArgumentException("Unexpected code $response")
      response.body?.charStream()?.buffered()?.use {reader ->
        reader.lineSequence().asFlow()
          .filter {it.isNotEmpty()}
          .takeWhile { it.startsWith("data:") }
          .map { it.drop(5).trim() }
          .takeWhile { it.uppercase() != endingChunk }
          .map { gson.fromJson(it, CompletionResponseChunk::class.java) }
          .takeWhile {it != null}
          .collect { emit(CodeCompletionChunk(it.id, it.choices[0].text!!)) }
      }
    }
  }

  private fun toLines(chunks: Flow<CodeCompletionChunk>): Flow<CodeCompletionChunk> = flow {
    var ongoingLine = ""
    var latestId = ""
    chunks.catch { logger.warn(it) }.collect { chunk ->
      var remaining = chunk.text
      while (remaining.contains(LINE_SEPARATOR)) {
        val parts = remaining.split(LINE_SEPARATOR, limit = 2)
        emit(CodeCompletionChunk(chunk.id, ongoingLine + parts[0] + LINE_SEPARATOR))
        ongoingLine = ""
        remaining = parts[1]
      }
      ongoingLine += remaining
      latestId = chunk.id
    }
    if (ongoingLine.isNotEmpty()) emit(CodeCompletionChunk(latestId, ongoingLine))
  }

  private fun stopAtFirstBrace(chunks: Flow<CodeCompletionChunk>): Flow<CodeCompletionChunk> = flow {
    var stopChunk: CodeCompletionChunk? = null
    var onlyWhitespaceSoFar = true
    chunks.takeWhile { chunk ->
      val trimmedText = chunk.text.trim()
      if (onlyWhitespaceSoFar && trimmedText in CLOSING_BRACES) {
        stopChunk = chunk
        return@takeWhile false
      }
      onlyWhitespaceSoFar = onlyWhitespaceSoFar && trimmedText.isEmpty()
      true
    }.collect(::emit)
    stopChunk?.let {emit(it)}
}

  private fun stopAtDuplicateLine(chunks: Flow<CodeCompletionChunk>): Flow<CodeCompletionChunk> = flow {
    val requestInfo = currentRequest!!
    var linePrev = requestInfo.lineBefore
    chunks.withIndex().takeWhile { (idx, chunk) ->
      val line = if (idx == 0) requestInfo.currentLinePrefix + chunk.text else chunk.text
      if (line == linePrev || line == requestInfo.lineAfter) return@takeWhile false
      linePrev = line
      true
    }.collect{ (_, chunk) -> emit(chunk) }
  }

  private fun stopAtBlockEnds(chunks: Flow<CodeCompletionChunk>): Flow<CodeCompletionChunk> = flow {
    val requestIndent = currentRequest!!.currentIndent
    var indentPrev = requestIndent
    var continuousIndentCount = 1
    var stopChunk: CodeCompletionChunk? = null
    chunks.withIndex().takeWhile { (idx, chunk) ->
      if (idx == 0 || chunk.text.trim().isEmpty()) return@takeWhile true
      if (continuousIndentCount >= MAX_CONTINUOUS_INDENT_COUNT) return@takeWhile false
      val indent = chunk.text.takeWhile { it.isWhitespace() }.length
      if (indent < requestIndent) return@takeWhile false
      if (indent < indentPrev && indent == requestIndent) {
        stopChunk = chunk
        return@takeWhile false
      }
      continuousIndentCount = if (indentPrev == indent) continuousIndentCount + 1 else 1
      indentPrev = indent
      true
    }.collect { (_, chunk) ->
        emit(chunk)
    }
    stopChunk?.let { emit(it) }
  }

  private suspend fun aggregate(chunks: Flow<CodeCompletionChunk>): CodeCompletionChunk {
    val partialChunks = mutableListOf<CodeCompletionChunk>()
    try {
      chunks.collect(partialChunks::add)
    } catch(e: Exception) {
      logger.warn(e)
    }
    val completion = partialChunks
      .asReversed()
      .dropWhile { it.text.trim().isEmpty() }
      .asReversed()
      .fold(CodeCompletionChunk("", "")) { acc, chunk ->
        CodeCompletionChunk(chunk.id, acc.text + chunk.text)
      }
    completion.text = completion.text.removeSuffix(LINE_SEPARATOR.toString())
    return completion
  }

  suspend fun provideCompletions(
    completionRequest: CompletionRequest
  ): CompletionResponse? = suspendCancellableCoroutine { continuation ->
    currentRequest = RequestInfo.fromCompletionRequest(completionRequest)
    val model = CONFIG["complete_model"] as? String
    var startTime = System.currentTimeMillis()
    val prompt = ContextBuilder(
      completionRequest.filepath,
      completionRequest.text,
      completionRequest.position
    ).createPrompt(model)
    val promptBuildingElapse = System.currentTimeMillis() - startTime

    scope.launch {
      startTime = System.currentTimeMillis()
      val chunks = request(prompt)
        .let(::toLines)
        .let(::stopAtFirstBrace)
        .let(::stopAtDuplicateLine)
        .let(::stopAtBlockEnds)
      val completion = aggregate(chunks)
      val llmRequestElapse = System.currentTimeMillis() - startTime
      val offset = completionRequest.position
      val replaceRange = CompletionResponse.Choice.Range(start = offset, end = offset)
      val text = if (completion.text != prevCompletion) completion.text else ""
      val choice = CompletionResponse.Choice(index = 0, text = text, replaceRange = replaceRange)
      val response = CompletionResponse(completion.id, model, listOf(choice), promptBuildingElapse, llmRequestElapse)
      continuation.resumeWith(Result.success(response))
      prevCompletion = completion.text
    }

    continuation.invokeOnCancellation {
      logger.warn("Agent request cancelled")
    }
  }

  suspend fun postEvent(logEventRequest: LogEventRequest): Unit = suspendCancellableCoroutine {
    val devChatEndpoint = CONFIG["providers.devchat.api_base"] as? String
    val devChatAPIKey = CONFIG["providers.devchat.api_key"] as? String
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
        logger.info("Log event response: $response")
      }
      logger.info("Code completion log event: $logEventRequest")
    } catch (e: Exception) {
      logger.warn(e)
    }
  }
}