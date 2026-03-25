package top.colter.mirai.plugin.bilibili.onebot

import kotlinx.serialization.json.*
import java.io.File
import java.nio.file.Path
import java.util.Base64

// ==================== Message Type Aliases ====================

typealias OBMessage = List<MessageSegment>

// ==================== Message Content Property ====================

val OBMessage.textContent: String
    get() = this.filter { it.type == "text" }
        .joinToString("") { it.data["text"]?.jsonPrimitive?.content ?: "" }

// ==================== Message Builder DSL ====================

class MessageBuilder {
    private val segments = mutableListOf<MessageSegment>()

    fun text(text: String) { if (text.isNotEmpty()) segments.add(MessageSegment.text(text)) }
    fun image(file: String) { segments.add(MessageSegment.image(file)) }
    fun imageBase64(data: ByteArray) {
        val b64 = Base64.getEncoder().encodeToString(data)
        segments.add(MessageSegment.image("base64://$b64"))
    }
    fun imagePath(path: Path) {
        val file = path.toFile()
        if (file.exists()) {
            imageBase64(file.readBytes())
        } else {
            segments.add(MessageSegment.image("file://${path.toAbsolutePath()}"))
        }
    }
    fun at(qq: Long) { segments.add(MessageSegment.at(qq.toString())) }
    fun atAll() { segments.add(MessageSegment.atAll()) }
    fun reply(messageId: Long) { segments.add(MessageSegment.reply(messageId)) }

    operator fun String.unaryPlus() { text(this) }
    operator fun MessageSegment.unaryPlus() { segments.add(this) }

    fun build(): OBMessage = segments.toList()
}

fun buildOBMessage(block: MessageBuilder.() -> Unit): OBMessage {
    return MessageBuilder().apply(block).build()
}

// ==================== OB Message Code ====================

// Placeholder format: [OB:image:file:///path/to/img.png] or [OB:image:base64://...]
private val obCodeRegex = """\[OB:image:(.*?)]""".toRegex()

/**
 * Parse text containing [OB:image:...] placeholders into message segments.
 * File paths are automatically converted to base64 for cross-environment compatibility.
 */
fun parseMessageContent(text: String): OBMessage {
    val segments = mutableListOf<MessageSegment>()
    var lastEnd = 0

    for (match in obCodeRegex.findAll(text)) {
        // Add text before this image
        if (match.range.first > lastEnd) {
            val textPart = text.substring(lastEnd, match.range.first)
            if (textPart.isNotEmpty()) {
                segments.add(MessageSegment.text(textPart))
            }
        }
        // Add image segment - convert file:// paths to base64
        val imageRef = match.groupValues[1]
        segments.add(resolveImageSegment(imageRef))
        lastEnd = match.range.last + 1
    }

    // Remaining text after last image
    if (lastEnd < text.length) {
        val remaining = text.substring(lastEnd)
        if (remaining.isNotEmpty()) {
            segments.add(MessageSegment.text(remaining))
        }
    }

    // If no OB codes found, treat entire text as plain text
    if (segments.isEmpty() && text.isNotEmpty()) {
        segments.add(MessageSegment.text(text))
    }

    return segments
}

/**
 * Resolve image reference to MessageSegment.
 * file:// paths are read and converted to base64 for cross-environment compatibility.
 */
private fun resolveImageSegment(imageRef: String): MessageSegment {
    if (imageRef.startsWith("file://")) {
        val filePath = imageRef.removePrefix("file://")
        val file = File(filePath)
        if (file.exists()) {
            val b64 = Base64.getEncoder().encodeToString(file.readBytes())
            return MessageSegment.image("base64://$b64")
        }
    }
    return MessageSegment.image(imageRef)
}

/**
 * Create an OB image placeholder string from a file path.
 */
fun imageCode(path: Path): String = "[OB:image:file://${path.toAbsolutePath()}]"
fun imageCode(file: File): String = imageCode(file.toPath())
fun imageCodeBase64(data: ByteArray): String {
    val b64 = Base64.getEncoder().encodeToString(data)
    return "[OB:image:base64://$b64]"
}

// ==================== Forward Message Builder ====================

class ForwardMessageBuilder {
    private val nodes = mutableListOf<MessageSegment>()

    fun node(userId: Long, nickname: String, content: OBMessage, time: Int? = null) {
        nodes.add(MessageSegment("node", buildJsonObject {
            put("user_id", userId.toString())
            put("nickname", nickname)
            put("content", Json.encodeToJsonElement(content))
            time?.let { put("time", it.toString()) }
        }))
    }

    fun node(userId: Long, nickname: String, text: String, time: Int? = null) {
        node(userId, nickname, listOf(MessageSegment.text(text)), time)
    }

    /** Add a pre-built node segment directly */
    operator fun MessageSegment.unaryPlus() {
        nodes.add(this)
    }

    fun build(): List<MessageSegment> = nodes.toList()
}

fun buildForwardNodes(block: ForwardMessageBuilder.() -> Unit): List<MessageSegment> {
    return ForwardMessageBuilder().apply(block).build()
}

// ==================== Utility Extensions ====================

/**
 * Append AtAll to message list (returns new list).
 * Handles both SINGLE_MESSAGE and PLUS_END modes.
 */
fun OBMessage.plusAtAll(mode: String = "PLUS_END"): OBMessage {
    return if (mode == "SINGLE_MESSAGE" || this.isEmpty()) {
        this + listOf(MessageSegment.atAll())
    } else {
        // Append to last segment
        val last = this.last()
        val newLast = if (last.type == "text") {
            // Add newline then atAll as separate segments
            listOf(MessageSegment.text(last.data["text"]?.jsonPrimitive?.content + "\n"), MessageSegment.atAll())
        } else {
            listOf(last, MessageSegment.text("\n"), MessageSegment.atAll())
        }
        this.dropLast(1) + newLast
    }
}

// Check if event message contains an at for a specific QQ
fun OneBotEvent.hasAtTarget(targetId: Long): Boolean {
    val messageArray = try {
        message?.jsonArray ?: return false
    } catch (_: Exception) { return false }

    return messageArray.any { seg ->
        val obj = seg.jsonObject
        obj["type"]?.jsonPrimitive?.content == "at" &&
            obj["data"]?.jsonObject?.get("qq")?.jsonPrimitive?.content == targetId.toString()
    }
}

// Extract plain text from event message
fun OneBotEvent.extractText(): String {
    val messageArray = try {
        message?.jsonArray ?: return rawMessage
    } catch (_: Exception) { return rawMessage }

    return messageArray.filter { seg ->
        seg.jsonObject["type"]?.jsonPrimitive?.content == "text"
    }.joinToString("") {
        it.jsonObject["data"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
    }.trim()
}
