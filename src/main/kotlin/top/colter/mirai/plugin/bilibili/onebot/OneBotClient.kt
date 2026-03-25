package top.colter.mirai.plugin.bilibili.onebot

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class OneBotClient(
    private val wsUrl: String,
    private val scope: CoroutineScope,
    private val token: String = ""
) {
    private val logger = LoggerFactory.getLogger("OBot")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = HttpClient(OkHttp) {
        install(WebSockets)
    }

    private var session: DefaultWebSocketSession? = null
    private val echoCounter = AtomicLong(0)
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<ApiResponse>>()
    private val sendMutex = Mutex()

    // Event handlers
    private val messageHandlers = mutableListOf<suspend (OneBotEvent) -> Unit>()
    private val noticeHandlers = mutableListOf<suspend (OneBotEvent) -> Unit>()

    var connected = false
        private set

    fun onMessage(handler: suspend (OneBotEvent) -> Unit) {
        messageHandlers.add(handler)
    }

    fun onNotice(handler: suspend (OneBotEvent) -> Unit) {
        noticeHandlers.add(handler)
    }

    suspend fun connect() {
        var retryDelay = 1000L
        while (scope.isActive) {
            try {
                logger.info("正在连接 OneBot WebSocket: $wsUrl")
                client.webSocket(wsUrl, request = {
                    if (token.isNotEmpty()) {
                        headers.append("Authorization", "Bearer $token")
                    }
                }) {
                    session = this
                    connected = true
                    retryDelay = 1000L
                    logger.info("OneBot WebSocket 连接成功")

                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                scope.launch {
                                    try {
                                        handleMessage(text)
                                    } catch (e: Exception) {
                                        logger.error("处理消息失败: ${e.message}")
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("WebSocket 连接断开: ${e.message}")
            }
            connected = false
            session = null
            logger.info("将在 ${retryDelay}ms 后重连...")
            delay(retryDelay)
            retryDelay = (retryDelay * 2).coerceAtMost(30000L)
        }
    }

    private suspend fun handleMessage(text: String) {
        val jsonElement = json.parseToJsonElement(text)
        val jsonObj = jsonElement.jsonObject

        // Check if it's an API response (has echo field)
        if (jsonObj.containsKey("echo") && jsonObj["echo"]?.jsonPrimitive?.contentOrNull != null) {
            val echo = jsonObj["echo"]!!.jsonPrimitive.content
            val response = json.decodeFromJsonElement<ApiResponse>(jsonElement)
            pendingRequests.remove(echo)?.complete(response)
            return
        }

        // It's an event
        val postType = jsonObj["post_type"]?.jsonPrimitive?.contentOrNull ?: return
        val event = json.decodeFromJsonElement<OneBotEvent>(jsonElement)

        when (postType) {
            "message", "message_sent" -> messageHandlers.forEach { it(event) }
            "notice" -> noticeHandlers.forEach { it(event) }
            "meta_event" -> {} // heartbeat etc, ignore
        }
    }

    suspend fun callApi(action: String, params: JsonObject = JsonObject(emptyMap())): ApiResponse {
        val echo = "req_${echoCounter.incrementAndGet()}"
        val request = ApiRequest(action, params, echo)
        val deferred = CompletableDeferred<ApiResponse>()
        pendingRequests[echo] = deferred

        try {
            val text = json.encodeToString(request)
            sendMutex.withLock {
                session?.send(text) ?: throw IllegalStateException("WebSocket 未连接")
            }
            return withTimeout(30000) { deferred.await() }
        } catch (e: Exception) {
            pendingRequests.remove(echo)
            throw e
        }
    }

    // ==================== API Helpers ====================

    private fun requireData(resp: ApiResponse, action: String): JsonElement {
        if (resp.retcode != 0) {
            throw OneBotApiException(action, resp.retcode, resp.message.ifEmpty { resp.wording })
        }
        return resp.data ?: throw OneBotApiException(action, resp.retcode, "response data is null")
    }

    private fun optionalMsgId(resp: ApiResponse): Long {
        if (resp.retcode != 0) {
            logger.warn("API 调用失败: retcode=${resp.retcode}, msg=${resp.message.ifEmpty { resp.wording }}")
        }
        return try {
            resp.data?.let { json.decodeFromJsonElement<SendMsgResponse>(it).messageId } ?: -1L
        } catch (_: Exception) { -1L }
    }

    // ==================== API Methods ====================

    suspend fun getLoginInfo(): LoginInfo {
        val resp = callApi("get_login_info")
        return json.decodeFromJsonElement(requireData(resp, "get_login_info"))
    }

    suspend fun getGroupList(): List<GroupInfo> {
        val resp = callApi("get_group_list")
        return json.decodeFromJsonElement(requireData(resp, "get_group_list"))
    }

    suspend fun getFriendList(): List<FriendInfo> {
        val resp = callApi("get_friend_list")
        return json.decodeFromJsonElement(requireData(resp, "get_friend_list"))
    }

    suspend fun getGroupMemberInfo(groupId: Long, userId: Long, noCache: Boolean = false): MemberInfo {
        val resp = callApi("get_group_member_info", buildJsonObject {
            put("group_id", groupId)
            put("user_id", userId)
            put("no_cache", noCache)
        })
        return json.decodeFromJsonElement(requireData(resp, "get_group_member_info"))
    }

    suspend fun sendGroupMsg(groupId: Long, message: List<MessageSegment>): Long {
        val resp = callApi("send_group_msg", buildJsonObject {
            put("group_id", groupId)
            put("message", json.encodeToJsonElement(message))
        })
        return optionalMsgId(resp)
    }

    suspend fun sendPrivateMsg(userId: Long, message: List<MessageSegment>): Long {
        val resp = callApi("send_private_msg", buildJsonObject {
            put("user_id", userId)
            put("message", json.encodeToJsonElement(message))
        })
        return optionalMsgId(resp)
    }

    suspend fun deleteMsg(messageId: Long) {
        callApi("delete_msg", buildJsonObject {
            put("message_id", messageId)
        })
    }

    suspend fun sendGroupForwardMsg(
        groupId: Long,
        nodes: List<MessageSegment>,
        source: String? = null,
        summary: String? = null,
        prompt: String? = null,
        news: List<String>? = null
    ): Long {
        val resp = callApi("send_group_forward_msg", buildJsonObject {
            put("group_id", groupId)
            put("message", json.encodeToJsonElement(nodes))
            source?.let { put("source", it) }
            summary?.let { put("summary", it) }
            prompt?.let { put("prompt", it) }
            news?.let { put("news", buildJsonArray { it.forEach { n -> addJsonObject { put("text", n) } } }) }
        })
        return optionalMsgId(resp)
    }

    suspend fun sendPrivateForwardMsg(
        userId: Long,
        nodes: List<MessageSegment>,
        source: String? = null,
        summary: String? = null,
        prompt: String? = null,
        news: List<String>? = null
    ): Long {
        val resp = callApi("send_private_forward_msg", buildJsonObject {
            put("user_id", userId)
            put("message", json.encodeToJsonElement(nodes))
            source?.let { put("source", it) }
            summary?.let { put("summary", it) }
            prompt?.let { put("prompt", it) }
            news?.let { put("news", buildJsonArray { it.forEach { n -> addJsonObject { put("text", n) } } }) }
        })
        return optionalMsgId(resp)
    }

    fun close() {
        client.close()
    }
}
