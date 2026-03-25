package top.colter.mirai.plugin.bilibili.onebot

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import top.colter.mirai.plugin.bilibili.OneBotConfig

/**
 * Command context passed to command handlers.
 */
class CommandContext(
    val event: OneBotEvent,
    val contact: OBContact,       // the group or private chat where the message was sent
    val userId: Long,             // sender's QQ ID
    val args: List<String>,       // remaining arguments after subcommand
) {
    private val logger = LoggerFactory.getLogger("Cmd")

    /** Delegate string for subscription operations (negative for groups, positive for users) */
    val subjectDelegate: String get() = contact.delegate

    suspend fun sendMessage(text: String): Long = contact.sendMessage(text)
    suspend fun sendMessage(segments: List<MessageSegment>): Long = contact.sendMessage(segments)

    suspend fun deleteMessage(messageId: Long) {
        try {
            BotInstance.client.deleteMsg(messageId)
        } catch (e: Exception) {
            logger.error("撤回消息失败: ${e.message}")
        }
    }

    /**
     * Wait for next message from the same user in the same context.
     * Uses DialogSession under the hood.
     */
    suspend fun nextMessage(timeout: Long = 120000): String {
        val session = DialogSessionManager.getOrCreate(userId, if (event.groupId > 0) event.groupId else 0)
        return try {
            withTimeout(timeout) {
                session.receive()
            }
        } finally {
            DialogSessionManager.remove(userId, if (event.groupId > 0) event.groupId else 0)
        }
    }
}

/**
 * Dispatches /bili commands from OneBot message events.
 */
object CommandDispatcher {
    private val logger = LoggerFactory.getLogger("Cmd")

    // Registry: subcommand aliases -> handler
    private val handlers = mutableMapOf<String, suspend (CommandContext) -> Unit>()
    // Registered commands: (aliases, description)
    private val commandEntries = mutableListOf<CommandEntry>()

    data class CommandEntry(val aliases: List<String>, val description: String)

    fun register(vararg aliases: String, description: String = "", handler: suspend (CommandContext) -> Unit) {
        aliases.forEach { handlers[it.lowercase()] = handler }
        if (description.isNotEmpty()) {
            commandEntries.add(CommandEntry(aliases.toList(), description))
        }
    }

    fun buildHelpText(): String {
        val sb = StringBuilder("=== BiliBili Dynamic 命令列表 ===\n\n")
        commandEntries.forEach { entry ->
            // Only show ASCII aliases (skip Chinese aliases)
            val asciiAliases = entry.aliases.filter { it.all { c -> c.code < 128 } }
            val primary = asciiAliases.firstOrNull() ?: entry.aliases.first()
            val shortcuts = asciiAliases.drop(1)
            val cmdStr = if (shortcuts.isNotEmpty()) {
                "/bili $primary (${shortcuts.joinToString(", ")})"
            } else {
                "/bili $primary"
            }
            sb.appendLine("$cmdStr  ${entry.description}")
        }
        return sb.toString().trimEnd()
    }

    /**
     * Try to dispatch a message event as a command.
     * Returns true if it was a command and was handled.
     */
    suspend fun dispatch(event: OneBotEvent): Boolean {
        val text = event.extractText()
        if (!text.startsWith("/bili")) return false

        // Only admins can use commands
        if (!OneBotConfig.isAdmin(event.userId)) return false

        val remaining = text.removePrefix("/bili").trim()
        if (remaining.isEmpty()) {
            // No subcommand, send help
            val contact: OBContact = if (event.groupId > 0) {
                BotInstance.getGroup(event.groupId) ?: return false
            } else {
                BotInstance.findContactAll(event.userId) ?: return false
            }
            contact.sendMessage(buildHelpText())
            return true
        }

        val parts = remaining.split("\\s+".toRegex())
        val subcommand = parts.first().lowercase()
        val args = if (parts.size > 1) parts.drop(1) else emptyList()

        val handler = handlers[subcommand]
        if (handler == null) {
            logger.debug("未知子命令: $subcommand")
            return false
        }

        val contact: OBContact = if (event.groupId > 0) {
            BotInstance.getGroup(event.groupId) ?: run {
                logger.error("未找到群 ${event.groupId}")
                return false
            }
        } else {
            BotInstance.findContactAll(event.userId) ?: run {
                logger.error("未找到用户 ${event.userId}")
                return false
            }
        }

        val context = CommandContext(event, contact, event.userId, args)
        val source = if (event.groupId > 0) "群${event.groupId}" else "私聊"
        val argsStr = if (args.isEmpty()) "" else " ${args.joinToString(" ")}"
        logger.info("[${event.userId}@$source] /bili $subcommand$argsStr")

        try {
            handler(context)
        } catch (e: TimeoutCancellationException) {
            // Dialog timeout, ignore
        } catch (e: Exception) {
            logger.error("命令执行失败 [$subcommand]: ${e.message}", e)
            try {
                context.sendMessage("命令执行失败: ${e.message}")
            } catch (_: Exception) {}
        }
        return true
    }
}
