package top.colter.mirai.plugin.bilibili.onebot

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// ==================== Message Segments ====================

@Serializable
data class MessageSegment(
    val type: String,
    val data: JsonObject = JsonObject(emptyMap())
) {
    companion object {
        fun text(text: String) = MessageSegment("text", buildJsonObject { put("text", text) })
        fun image(file: String) = MessageSegment("image", buildJsonObject { put("file", file) })
        fun at(qq: String) = MessageSegment("at", buildJsonObject { put("qq", qq) })
        fun atAll() = MessageSegment("at", buildJsonObject { put("qq", "all") })
        fun reply(id: Long) = MessageSegment("reply", buildJsonObject { put("id", id.toString()) })
        fun node(
            userId: Long,
            nickname: String,
            content: List<MessageSegment>
        ) = MessageSegment("node", buildJsonObject {
            put("user_id", userId.toString())
            put("nickname", nickname)
            put("content", Json.encodeToJsonElement(content))
        })
    }
}

// ==================== API Request/Response ====================

@Serializable
data class ApiRequest(
    val action: String,
    val params: JsonObject = JsonObject(emptyMap()),
    val echo: String? = null
)

@Serializable
data class ApiResponse(
    val status: String = "",
    val retcode: Int = 0,
    val data: JsonElement? = null,
    val message: String = "",
    val wording: String = "",
    val echo: String? = null
)

// ==================== Events ====================

@Serializable
data class OneBotEvent(
    @SerialName("post_type") val postType: String = "",
    @SerialName("message_type") val messageType: String = "",
    @SerialName("notice_type") val noticeType: String = "",
    @SerialName("sub_type") val subType: String = "",
    @SerialName("self_id") val selfId: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("message_id") val messageId: Long = 0,
    val message: JsonElement? = null,
    @SerialName("raw_message") val rawMessage: String = "",
    val sender: JsonObject? = null,
    val time: Long = 0,
    @SerialName("operator_id") val operatorId: Long = 0,
)

// ==================== Contact Info ====================

@Serializable
data class LoginInfo(
    @SerialName("user_id") val userId: Long,
    val nickname: String
)

@Serializable
data class GroupInfo(
    @SerialName("group_id") val groupId: Long,
    @SerialName("group_name") val groupName: String,
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("max_member_count") val maxMemberCount: Int = 0
)

@Serializable
data class FriendInfo(
    @SerialName("user_id") val userId: Long,
    val nickname: String,
    val remark: String = ""
)

@Serializable
data class MemberInfo(
    @SerialName("group_id") val groupId: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    val nickname: String = "",
    val card: String = "",
    val role: String = "", // "owner", "admin", "member"
    @SerialName("shut_up_timestamp") val shutUpTimestamp: Long = 0
) {
    val permissionLevel: Int
        get() = when (role) {
            "owner" -> 2
            "admin" -> 1
            else -> 0
        }

    val isMuted: Boolean
        get() = shutUpTimestamp > System.currentTimeMillis() / 1000
}

// ==================== Send Message Response ====================

@Serializable
data class SendMsgResponse(
    @SerialName("message_id") val messageId: Long = 0
)

// ==================== Exceptions ====================

class OneBotApiException(
    val action: String,
    val retcode: Int,
    override val message: String
) : RuntimeException("OneBot API [$action] failed: retcode=$retcode, message=$message")
