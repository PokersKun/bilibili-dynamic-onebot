package top.colter.mirai.plugin.bilibili.service

import top.colter.mirai.plugin.bilibili.BiliConfig
import top.colter.mirai.plugin.bilibili.BiliData
import top.colter.mirai.plugin.bilibili.api.getDynamicDetail
import top.colter.mirai.plugin.bilibili.api.getLive
import top.colter.mirai.plugin.bilibili.data.DynamicMessage
import top.colter.mirai.plugin.bilibili.data.LIVE_LINK
import top.colter.mirai.plugin.bilibili.data.LiveCloseMessage
import top.colter.mirai.plugin.bilibili.data.LiveMessage
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import top.colter.mirai.plugin.bilibili.onebot.*
import top.colter.mirai.plugin.bilibili.tasker.DynamicMessageTasker.buildMessage
import top.colter.mirai.plugin.bilibili.tasker.LiveMessageTasker.buildMessage
import top.colter.mirai.plugin.bilibili.tasker.SendTasker.buildMessage
import top.colter.mirai.plugin.bilibili.utils.biliClient

object TemplateService {
    suspend fun listTemplate(type: String, subject: OBContact) {
        val template = when (type) {
            "d" -> BiliConfig.templateConfig.dynamicPush
            "l" -> BiliConfig.templateConfig.livePush
            "le" -> BiliConfig.templateConfig.liveClose
            else -> {
                subject.sendMessage("类型错误 d:动态 l:直播 le:直播结束")
                return
            }
        }

        // https://t.bilibili.com/385190177693666264
        val dynamic = when (type) {
            "d" -> biliClient.getDynamicDetail("385190177693666264")?.buildMessage()!!
            "l" -> biliClient.getLive(1, 1)?.rooms?.first()?.buildMessage()!!
            "le" -> LiveCloseMessage(
                0, 0, "Test", "2022年1月1日 00:00:00", 1640966400, "2022年1月1日 01:02:03",
                "1小时 2分钟 3秒", "测试测试测试TEST", "游戏", LIVE_LINK("0")
            )
            else -> return
        }

        // Pre-build messages for each template (suspend calls)
        var pt = 0
        data class TemplateEntry(val name: String, val msgs: List<OBMessage>, val timeOffset: Int)
        val entries = mutableListOf<TemplateEntry>()
        for (t in template) {
            val innerMsgs: List<OBMessage> = when (dynamic) {
                is DynamicMessage -> dynamic.buildMessage(t.value)
                is LiveMessage -> dynamic.buildMessage(t.value)
                is LiveCloseMessage -> dynamic.buildMessage(t.value)
                else -> emptyList()
            }
            entries.add(TemplateEntry(t.key, innerMsgs, pt))
            pt += 86400
        }

        // Build forward message nodes, expanding nested _forward markers
        val nodes = buildForwardNodes {
            node(BotInstance.botId, dynamic.name, if (type == "d") "动态推送模板" else "直播推送模板", dynamic.timestamp)
            node(BotInstance.botId, dynamic.name, "下面每个转发消息都代表一个模板推送效果", dynamic.timestamp)
            for (entry in entries) {
                node(BotInstance.botId, dynamic.name, entry.name, dynamic.timestamp + entry.timeOffset)
                entry.msgs.forEach { segments ->
                    if (segments.size == 1 && segments[0].type == "_forward") {
                        // Expand nested forward: extract inner nodes and add them directly
                        val innerNodes = segments[0].data["nodes"]?.let {
                            Json.decodeFromJsonElement(ListSerializer(MessageSegment.serializer()), it)
                        } ?: emptyList()
                        innerNodes.forEach { n -> +n }
                    } else {
                        node(BotInstance.botId, dynamic.name, segments, dynamic.timestamp + entry.timeOffset)
                    }
                }
            }
        }

        // Send forward message
        if (subject is OBGroup) {
            BotInstance.client.sendGroupForwardMsg(subject.id, nodes)
        } else {
            BotInstance.client.sendPrivateForwardMsg(subject.id, nodes)
        }
    }

    fun setTemplate(type: String, template: String, subject: String): String {
        val pushTemplates = when (type) {
            "d" -> BiliConfig.templateConfig.dynamicPush
            "l" -> BiliConfig.templateConfig.livePush
            "le" -> BiliConfig.templateConfig.liveClose
            else -> return "类型错误 d:动态 l:直播 le:直播结束"
        }
        val push = when (type) {
            "d" -> BiliData.dynamicPushTemplate
            "l" -> BiliData.livePushTemplate
            "le" -> BiliData.liveCloseTemplate
            else -> return "类型错误 d:动态 l:直播 le:直播结束"
        }
        return if (pushTemplates.containsKey(template)) {
            push.forEach { (_, u) -> u.remove(subject) }
            if (!push.containsKey(template)) push[template] = mutableSetOf()
            push[template]!!.add(subject)
            "配置完成"
        } else "没有这个模板哦 $template"
    }
}
