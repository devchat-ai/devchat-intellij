package ai.devchat.common

import ai.devchat.core.DevChatClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException

class HttpClient {
    val client = OkHttpClient()
    val json = Json { ignoreUnknownKeys = true }

    inline fun <reified T> get(
        urlAddress: String,
        queryParams: Map<String, Any?> = emptyMap()
    ): T? {
        Log.info("GET request to [$urlAddress] with request parameters: $queryParams")
        val urlBuilder = urlAddress.toHttpUrlOrNull()?.newBuilder() ?: return null
        queryParams.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v.toString()) }
        val url = urlBuilder.build()
        val request = Request.Builder().url(url).get().build()
        var retries = DevChatClient.MAX_RETRIES
        while (retries > 0) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException(
                        "Unsuccessful response: ${response.code} ${response.message}"
                    )
                    val result = response.body?.string()?.let {
                        json.decodeFromString<T>(it)
                    }
                    return result
                }
            } catch (e: IOException) {
                Log.warn("$e, retrying...")
                retries--
                Thread.sleep(DevChatClient.RETRY_INTERVAL)
            } catch (e: Exception) {
                Log.warn(e.toString())
                return null
            }
        }
        return null
    }

    inline fun <reified T, reified R> post(urlAddress: String, body: T? = null): R? {
        Log.info("POST request to [$urlAddress] with request body: $body")
        val url = urlAddress.toHttpUrlOrNull() ?: return null
        val requestBody = json.encodeToString(serializer(), body).toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        var retries = DevChatClient.MAX_RETRIES
        while (retries > 0) {
            try {
                val response: Response = client.newCall(request).execute()
                if (!response.isSuccessful) throw IOException(
                    "Unsuccessful response: ${response.code} ${response.message}"
                )
                val result = response.body?.let {
                    json.decodeFromString<R>(it.string())
                }
                return result
            } catch (e: IOException) {
                Log.warn("$e, retrying...")
                retries--
                Thread.sleep(DevChatClient.RETRY_INTERVAL)
            } catch (e: Exception) {
                Log.warn(e.toString())
                return null
            }
        }
        return null
    }

    inline fun <reified T, reified R> streamPost(urlAddress: String, body: T? = null): Flow<R> = callbackFlow<R> {
        Log.info("POST request to [$urlAddress] with request body: $body")
        val url = urlAddress.toHttpUrlOrNull() ?: return@callbackFlow
        val requestJson = json.encodeToString(serializer(), body)
        val requestBody = requestJson.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        val call = client.newCall(request)

        val response = call.execute()
        if (!response.isSuccessful) {
            throw IOException("Unexpected code $response")
        }
        response.body?.byteStream()?.use {inputStream ->
            val buffer = ByteArray(8192) // 8KB buffer
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val chunk = buffer.copyOf(bytesRead).toString(Charsets.UTF_8)
                send(json.decodeFromString(chunk))
            }
        }
        close()
        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)


    fun getMedia(
        urlAddress: String,
        queryParams: Map<String, Any?> = emptyMap(),
        outFile: File
    ) {
        Log.info("GET request to [$urlAddress] with request parameters: $queryParams")
        val urlBuilder = urlAddress.toHttpUrlOrNull()?.newBuilder() ?: return
        queryParams.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v.toString()) }
        val url = urlBuilder.build()
        val request = Request.Builder().url(url).get().build()
        var retries = DevChatClient.MAX_RETRIES
        while (retries > 0) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException(
                        "Unsuccessful response: ${response.code} ${response.message}"
                    )
                    outFile.outputStream().use {
                        response.body?.byteStream()?.copyTo(it)
                        Log.info("Media saved to ${outFile.absolutePath}")
                    }
                }
                return
            } catch (e: IOException) {
                Log.warn("$e, retrying...")
                retries--
                Thread.sleep(DevChatClient.RETRY_INTERVAL)
            } catch (e: Exception) {
                Log.warn(e.toString())
            }
        }
    }
}

