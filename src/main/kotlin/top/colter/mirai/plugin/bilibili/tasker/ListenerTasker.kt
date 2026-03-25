package top.colter.mirai.plugin.bilibili.tasker

import top.colter.mirai.plugin.bilibili.BiliConfig
import top.colter.mirai.plugin.bilibili.BiliData
import top.colter.mirai.plugin.bilibili.command.DynamicCommand
import top.colter.mirai.plugin.bilibili.onebot.*
import top.colter.mirai.plugin.bilibili.service.DynamicService.removeAllSubscribe
import top.colter.mirai.plugin.bilibili.service.TriggerMode
import top.colter.mirai.plugin.bilibili.service.matchingRegular
import top.colter.mirai.plugin.bilibili.utils.*

object ListenerTasker : BiliTasker() {
    override var interval: Int = -1

    private val triggerMode get() = BiliConfig.linkResolveConfig.triggerMode
    private val returnLink get() = BiliConfig.linkResolveConfig.returnLink
    private val showLoadingMessage get() = BiliConfig.enableConfig.showLoadingMessage

    override suspend fun main() {
        // Register command handlers
        DynamicCommand.register()

        // Register notice handler: bot leave group
        BotInstance.client.onNotice { event ->
            if (event.noticeType == "group_decrease" && event.subType == "kick_me") {
                val groupId = event.groupId
                val d = (-groupId).toString() // delegate for group is negative
                if (findContact(d) == null) {
                    removeAllSubscribe(d)
                    BiliData.dynamicPushTemplate.forEach { (_, c) -> c.remove(d) }
                    BiliData.livePushTemplate.forEach { (_, c) -> c.remove(d) }
                    logger.warn("Bot退出群 $groupId 已删除此群的所有订阅数据")
                }
                BotInstance.removeGroup(groupId)
            }
        }

        // Register message handler: command dispatch + dialog routing + link resolve
        BotInstance.client.onMessage { event ->
            // First try to route to an active dialog session
            if (DialogSessionManager.tryRoute(event)) return@onMessage

            // Try command dispatch
            if (CommandDispatcher.dispatch(event)) return@onMessage

            // Link resolve (group messages only)
            if (event.messageType == "group") {
                handleLinkResolve(event)
            }
        }
    }

    private suspend fun handleLinkResolve(event: OneBotEvent) {
        var shouldResolve = false
        when (triggerMode) {
            TriggerMode.At -> {
                if (event.hasAtTarget(BotInstance.botId)) {
                    shouldResolve = true
                }
            }
            TriggerMode.Always -> shouldResolve = true
            TriggerMode.Never -> shouldResolve = false
        }
        if (!shouldResolve) return

        val msg = event.extractText()
        val type = matchingRegular(msg) ?: return

        val contact = BotInstance.getGroup(event.groupId) ?: return

        val ms = if (showLoadingMessage) contact.sendMessage("加载中...") else null
        val img = type.drawGeneral()
        if (img == null) {
            ms?.let { BotInstance.client.deleteMsg(it) }
            contact.sendMessage("解析失败")
            return
        }
        val imgMsg = uploadImage(img, CacheType.DRAW_SEARCH)
        if (imgMsg == null) {
            ms?.let { BotInstance.client.deleteMsg(it) }
            contact.sendMessage("图片上传失败")
            return
        }

        val segments = mutableListOf<MessageSegment>()
        segments.addAll(parseMessageContent(imgMsg))
        if (returnLink) segments.add(MessageSegment.text(type.getLink()))
        contact.sendMessage(segments)
        ms?.let { BotInstance.client.deleteMsg(it) }
    }
}
