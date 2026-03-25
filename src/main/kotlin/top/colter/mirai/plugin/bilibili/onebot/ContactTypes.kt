package top.colter.mirai.plugin.bilibili.onebot

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory

// ==================== Contact Abstractions ====================

interface OBContact {
    val id: Long
    val contactName: String
    suspend fun sendMessage(message: List<MessageSegment>): Long
    suspend fun sendMessage(text: String): Long = sendMessage(listOf(MessageSegment.text(text)))
}

class OBGroup(
    override val id: Long,
    override val contactName: String,
    var botPermissionLevel: Int = 0, // 0=member, 1=admin, 2=owner
    var isBotMuted: Boolean = false
) : OBContact {
    override suspend fun sendMessage(message: List<MessageSegment>): Long {
        return BotInstance.client.sendGroupMsg(id, message)
    }
}

class OBFriend(
    override val id: Long,
    override val contactName: String
) : OBContact {
    override suspend fun sendMessage(message: List<MessageSegment>): Long {
        return BotInstance.client.sendPrivateMsg(id, message)
    }
}

// Extension: delegate string (negative for groups, positive for users)
val OBContact.delegate: String
    get() = (if (this is OBGroup) id * -1 else id).toString()

// ==================== Bot Instance Singleton ====================

object BotInstance {
    private val logger = LoggerFactory.getLogger("Bot")

    lateinit var client: OneBotClient
        private set
    var botId: Long = 0
        private set
    var botNickname: String = ""
        private set

    private val groupCache = mutableMapOf<Long, OBGroup>()
    private val friendCache = mutableMapOf<Long, OBFriend>()
    private val cacheMutex = Mutex()

    fun init(client: OneBotClient) {
        this.client = client
    }

    suspend fun fetchLoginInfo() {
        val info = client.getLoginInfo()
        botId = info.userId
        botNickname = info.nickname
        logger.info("Bot QQ: $botId ($botNickname)")
    }

    suspend fun refreshContacts() {
        cacheMutex.withLock {
            try {
                val groups = client.getGroupList()
                groupCache.clear()
                groups.forEach { g ->
                    groupCache[g.groupId] = OBGroup(g.groupId, g.groupName)
                }
                // Fetch bot's permission in each group
                for ((gid, group) in groupCache) {
                    try {
                        val memberInfo = client.getGroupMemberInfo(gid, botId)
                        group.botPermissionLevel = memberInfo.permissionLevel
                        group.isBotMuted = memberInfo.isMuted
                    } catch (_: Exception) {}
                }
                logger.info("已缓存 ${groupCache.size} 个群")
            } catch (e: Exception) {
                logger.error("获取群列表失败: ${e.message}")
            }

            try {
                val friends = client.getFriendList()
                friendCache.clear()
                friends.forEach { f ->
                    friendCache[f.userId] = OBFriend(f.userId, f.remark.ifEmpty { f.nickname })
                }
                logger.info("已缓存 ${friendCache.size} 个好友")
            } catch (e: Exception) {
                logger.error("获取好友列表失败: ${e.message}")
            }
        }
    }

    /**
     * Find contact by delegate string (negative = group, positive = friend/user)
     */
    suspend fun findContact(del: String): OBContact? {
        if (del.isBlank()) return null
        val delegate = try { del.toLong() } catch (_: NumberFormatException) { return null }
        return findContact(delegate)
    }

    suspend fun findContact(delegate: Long): OBContact? {
        return cacheMutex.withLock {
            if (delegate < 0) {
                groupCache[delegate * -1]
            } else {
                friendCache[delegate] ?: groupCache.values.find { false } // friends only for positive
            }
        }
    }

    /**
     * Find contact by actual ID (not delegate)
     */
    suspend fun findContactAll(id: Long): OBContact? {
        return cacheMutex.withLock {
            friendCache[id] ?: groupCache[id]
        }
    }

    suspend fun findContactAll(id: String): OBContact? {
        return try { findContactAll(id.toLong()) } catch (_: NumberFormatException) { null }
    }

    suspend fun getGroup(groupId: Long): OBGroup? {
        return cacheMutex.withLock { groupCache[groupId] }
    }

    suspend fun refreshGroupMemberInfo(groupId: Long) {
        try {
            val memberInfo = client.getGroupMemberInfo(groupId, botId, noCache = true)
            cacheMutex.withLock {
                groupCache[groupId]?.apply {
                    botPermissionLevel = memberInfo.permissionLevel
                    isBotMuted = memberInfo.isMuted
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun removeGroup(groupId: Long) {
        cacheMutex.withLock {
            groupCache.remove(groupId)
        }
    }
}
