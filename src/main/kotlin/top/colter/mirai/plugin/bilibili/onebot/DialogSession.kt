package top.colter.mirai.plugin.bilibili.onebot

import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentHashMap

/**
 * A dialog session for interactive conversations.
 * Routes incoming messages from a specific user+group to a waiting coroutine.
 */
class DialogSession {
    private val channel = Channel<String>(Channel.BUFFERED)

    suspend fun send(message: String) {
        channel.send(message)
    }

    suspend fun receive(): String {
        return channel.receive()
    }

    fun close() {
        channel.close()
    }
}

/**
 * Manages active dialog sessions.
 * Key: "userId:groupId" (groupId=0 for private messages)
 */
object DialogSessionManager {
    private val sessions = ConcurrentHashMap<String, DialogSession>()

    private fun key(userId: Long, groupId: Long) = "$userId:$groupId"

    fun getOrCreate(userId: Long, groupId: Long): DialogSession {
        return sessions.getOrPut(key(userId, groupId)) { DialogSession() }
    }

    fun get(userId: Long, groupId: Long): DialogSession? {
        return sessions[key(userId, groupId)]
    }

    fun remove(userId: Long, groupId: Long) {
        sessions.remove(key(userId, groupId))?.close()
    }

    /**
     * Try to route a message to an active dialog session.
     * Returns true if the message was consumed by a session.
     */
    suspend fun tryRoute(event: OneBotEvent): Boolean {
        val groupId = if (event.groupId > 0) event.groupId else 0L
        val session = sessions[key(event.userId, groupId)] ?: return false
        val text = event.extractText()
        session.send(text)
        return true
    }
}
