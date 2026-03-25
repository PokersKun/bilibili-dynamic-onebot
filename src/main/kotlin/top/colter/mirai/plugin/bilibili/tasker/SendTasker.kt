package top.colter.mirai.plugin.bilibili.tasker

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import top.colter.mirai.plugin.bilibili.*
import top.colter.mirai.plugin.bilibili.data.*
import top.colter.mirai.plugin.bilibili.onebot.*
import top.colter.mirai.plugin.bilibili.utils.*
import kotlin.io.path.notExists

object SendTasker : BiliTasker() {

    override var interval: Int = 1
    override val unitTime: Long = 500

    private val templateConfig by BiliConfig::templateConfig
    private val atAllPlus = BiliConfig.pushConfig.atAllPlus

    private val dynamic by BiliData::dynamic
    private val filter by BiliData::filter
    private val atAll by BiliData::atAll
    private val group by BiliData::group
    private val bangumi by BiliData::bangumi

    private val messageChannel by BiliBiliDynamic::messageChannel
    private val missChannel by BiliBiliDynamic::missChannel

    private val messageInterval = BiliConfig.pushConfig.messageInterval
    private val pushInterval = BiliConfig.pushConfig.pushInterval

    private val forwardRegex = """\{>>}(.*?)\{<<}""".toRegex()
    private val tagRegex = """\{([a-z]+)}""".toRegex()

    override suspend fun main() {
        var isMiss = false
        var m = missChannel.tryReceive().getOrNull()
        if (m == null) messageChannel.tryReceive().getOrNull()?.let {
            m = it
            missChannel.trySend(it)
        } else isMiss = true
        if (m == null) return
        val biliMessage = m!!

        withTimeout(300005) {
            val contactList = if (biliMessage.contact == null) when (biliMessage) {
                is DynamicMessage -> getDynamicContactList(biliMessage.mid, biliMessage.content, biliMessage.type)
                is LiveMessage,
                is LiveCloseMessage -> getLiveContactList(biliMessage.mid)
            } else listOf(biliMessage.contact!!)

            if (!contactList.isNullOrEmpty()) {
                val templateMap: MutableMap<String, MutableSet<String>> = mutableMapOf()

                val pushTemplates = when (biliMessage) {
                    is DynamicMessage -> templateConfig.dynamicPush
                    is LiveMessage -> templateConfig.livePush
                    is LiveCloseMessage -> templateConfig.liveClose
                }

                val push = when (biliMessage) {
                    is DynamicMessage -> BiliData.dynamicPushTemplate
                    is LiveMessage -> BiliData.livePushTemplate
                    is LiveCloseMessage -> BiliData.liveCloseTemplate
                }

                val defaultTemplate = when (biliMessage) {
                    is DynamicMessage -> templateConfig.defaultDynamicPush
                    is LiveMessage -> templateConfig.defaultLivePush
                    is LiveCloseMessage -> templateConfig.defaultLiveClose
                }

                if (push.isEmpty()) {
                    if (templateMap[defaultTemplate] == null) templateMap[defaultTemplate] = mutableSetOf()
                    contactList.forEach { templateMap[defaultTemplate]!!.add(it) }
                }

                push.forEach { (t, u) ->
                    contactList.forEach {
                        if (u.contains(it)) {
                            if (templateMap[t] == null) templateMap[t] = mutableSetOf()
                            templateMap[t]!!.add(it)
                        } else {
                            if (templateMap[defaultTemplate] == null) templateMap[defaultTemplate] = mutableSetOf()
                            templateMap[defaultTemplate]!!.add(it)
                        }
                    }
                }

                val templateMsgMap: MutableMap<String, List<OBMessage>> = mutableMapOf()
                templateMap.forEach {
                    templateMsgMap[it.key] = when (biliMessage) {
                        is DynamicMessage -> biliMessage.buildMessage(pushTemplates[it.key]!!)
                        is LiveMessage -> biliMessage.buildMessage(pushTemplates[it.key]!!)
                        is LiveCloseMessage -> biliMessage.buildMessage(pushTemplates[it.key]!!)
                    }
                }
                val contactAtAll: MutableMap<OBContact, Boolean> = mutableMapOf()
                val contactMessage: MutableMap<OBContact, List<OBMessage>> = mutableMapOf()

                for (temp in templateMap) {
                    templateMsgMap[temp.key]?.let { msg ->
                        temp.value.forEach {
                            try {
                                it.toLong()
                                findContact(it)?.let { contact ->
                                    contactMessage[contact] = msg
                                    if (!contactAtAll.containsKey(contact) || contactAtAll[contact] != true)
                                        contactAtAll[contact] = checkAtAll(contact, biliMessage)
                                }
                            } catch (e: NumberFormatException) {
                                group[it]?.contacts?.forEach { gc ->
                                    findContact(gc)?.let { contact ->
                                        if (!contactMessage.contains(contact)) contactMessage[contact] = msg
                                        if (!contactAtAll.containsKey(contact) || contactAtAll[contact] != true)
                                            contactAtAll[contact] = checkAtAll(contact, biliMessage)
                                    }
                                }
                            }
                        }
                    }
                }
                for ((c, msg) in contactMessage) {
                    if (c is OBGroup && c.isBotMuted) continue

                    val finalMsg = if (contactAtAll[c] == true) {
                        if (atAllPlus == "SINGLE_MESSAGE" || msg.last().textContent.contains("[转发消息]")) {
                            msg + listOf(listOf(MessageSegment.atAll()))
                        } else {
                            val last = msg.last() + listOf(MessageSegment.text("\n"), MessageSegment.atAll())
                            msg.dropLast(1) + listOf(last)
                        }
                    } else msg

                    c.sendMessages(finalMsg)
                }
            }
        }
        if (!isMiss) missChannel.tryReceive()
    }

    private suspend fun OBContact.sendMessages(messages: List<OBMessage>) = try {
        messages.forEach {
            sendMessage(it)
            delay(messageInterval)
        }
        delay(pushInterval)
    } catch (e: Throwable) {
        logger.error("发送消息失败！", e)
        delay(pushInterval)
    }

    fun checkAtAll(contact: OBContact?, biliMessage: BiliMessage): Boolean {
        if (contact == null) return false
        if (contact !is OBGroup || contact.botPermissionLevel <= 0) return false
        var isAtAll = false
        val aa = atAll[contact.delegate]?.get(biliMessage.mid) ?: atAll[contact.delegate]?.get(0L)
        if (!aa.isNullOrEmpty()) {
            if (aa.contains(AtAllType.ALL)) isAtAll = true
            else when (biliMessage) {
                is DynamicMessage ->
                    if (aa.contains(AtAllType.DYNAMIC) || aa.contains(biliMessage.type.toAtAllType()))
                        isAtAll = true
                is LiveMessage -> if (aa.contains(AtAllType.LIVE)) isAtAll = true
                is LiveCloseMessage -> isAtAll = false
            }
        }
        return isAtAll
    }

    fun DynamicType.toAtAllType() =
        when (this) {
            DynamicType.DYNAMIC_TYPE_AV -> AtAllType.VIDEO
            DynamicType.DYNAMIC_TYPE_MUSIC -> AtAllType.MUSIC
            DynamicType.DYNAMIC_TYPE_ARTICLE -> AtAllType.ARTICLE
            else -> AtAllType.DYNAMIC
        }

    fun DynamicType.toFilterType() =
        when (this) {
            DynamicType.DYNAMIC_TYPE_WORD,
            DynamicType.DYNAMIC_TYPE_DRAW,
            DynamicType.DYNAMIC_TYPE_COMMON_SQUARE,
            DynamicType.DYNAMIC_TYPE_COMMON_VERTICAL,
            DynamicType.DYNAMIC_TYPE_UNKNOWN,
            DynamicType.DYNAMIC_TYPE_NONE -> DynamicFilterType.DYNAMIC

            DynamicType.DYNAMIC_TYPE_FORWARD -> DynamicFilterType.FORWARD
            DynamicType.DYNAMIC_TYPE_AV,
            DynamicType.DYNAMIC_TYPE_UGC_SEASON,
            DynamicType.DYNAMIC_TYPE_PGC,
            DynamicType.DYNAMIC_TYPE_PGC_UNION -> DynamicFilterType.VIDEO

            DynamicType.DYNAMIC_TYPE_MUSIC -> DynamicFilterType.MUSIC
            DynamicType.DYNAMIC_TYPE_ARTICLE -> DynamicFilterType.ARTICLE
            DynamicType.DYNAMIC_TYPE_LIVE,
            DynamicType.DYNAMIC_TYPE_LIVE_RCMD -> DynamicFilterType.LIVE
        }


    private fun getDynamicContactList(mid: Long, content: String, type: DynamicType): MutableSet<String>? {
        return try {
            if (type == DynamicType.DYNAMIC_TYPE_PGC || type == DynamicType.DYNAMIC_TYPE_PGC_UNION) {
                return bangumi[mid]?.contacts
            }

            val all = dynamic[0] ?: return null
            val list: MutableSet<String> = mutableSetOf()
            list.addAll(all.contacts)
            val subData = dynamic[mid] ?: return list

            list.addAll(subData.contacts)
            list.removeAll(subData.banList.keys)

            list.filter { contact ->
                if (filter.containsKey(contact) && (filter[contact]!!.containsKey(mid) || filter[contact]!!.containsKey(0L))) {
                    val dynamicFilter = filter[contact]!![mid] ?: filter[contact]!![0L]!!
                    val typeSelect = dynamicFilter.typeSelect
                    if (typeSelect.list.isNotEmpty()) {
                        val b = typeSelect.list.contains(type.toFilterType())
                        when (typeSelect.mode) {
                            FilterMode.WHITE_LIST -> if (!b) return@filter false
                            FilterMode.BLACK_LIST -> if (b) return@filter false
                        }
                    }
                    val regularSelect = dynamicFilter.regularSelect
                    if (regularSelect.list.isNotEmpty()) {
                        regularSelect.list.forEach {
                            val b = Regex(it).containsMatchIn(content)
                            when (regularSelect.mode) {
                                FilterMode.WHITE_LIST -> if (!b) return@filter false
                                FilterMode.BLACK_LIST -> if (b) return@filter false
                            }
                        }
                    }
                }
                true
            }.toMutableSet()
        } catch (e: Throwable) {
            logger.warn("getDynamicContactList error: ${e.message}")
            null
        }
    }

    fun getLiveContactList(uid: Long): MutableSet<String>? {
        return try {
            val all = dynamic[0] ?: return null
            val list: MutableSet<String> = mutableSetOf()
            list.addAll(all.contacts)
            val subData = dynamic[uid] ?: return list

            list.addAll(subData.contacts)
            list.removeAll(subData.banList.keys)

            list.filter { contact ->
                if (filter.containsKey(contact) && (filter[contact]!!.containsKey(uid) || filter[contact]!!.containsKey(0L))) {
                    val dynamicFilter = filter[contact]!![uid] ?: filter[contact]!![0L]!!
                    val typeSelect = dynamicFilter.typeSelect
                    if (typeSelect.list.isNotEmpty()) {
                        val b = typeSelect.list.contains(DynamicFilterType.LIVE)
                        when (typeSelect.mode) {
                            FilterMode.WHITE_LIST -> if (!b) return@filter false
                            FilterMode.BLACK_LIST -> if (b) return@filter false
                        }
                    }
                }
                true
            }.toMutableSet()
        } catch (e: Throwable) {
            logger.warn("getLiveContactList error: ${e.message}")
            null
        }
    }

    suspend fun LiveMessage.buildMessage(template: String) =
        buildMsgList(template) { buildLiveMsg(it, this) }

    private suspend fun buildLiveMsg(ms: String, lm: LiveMessage): String {
        var p = 0
        var content = ms

        while (true) {
            val key = tagRegex.find(content, p) ?: break
            val rep = when (key.destructured.component1()) {
                "name" -> lm.name
                "uid" -> lm.mid.toString()
                "rid" -> lm.rid.toString()
                "time" -> lm.time
                "type" -> "直播"
                "title" -> lm.title
                "area" -> lm.area
                "link" -> lm.link
                "cover" -> uploadImage(lm.cover, CacheType.IMAGES) ?: ""
                "draw" -> if (lm.drawPath == null) "[绘制直播图片失败]" else {
                    val path = cachePath.resolve(lm.drawPath)
                    if (path.notExists()) "[未找到绘制的直播图片]"
                    else uploadImage(path) ?: "[上传图片失败]"
                }
                else -> "[不支持的类型: ${key.destructured.component1()}]"
            }
            content = content.replaceRange(key.range, rep)
            p = key.range.first + rep.length
        }
        return content
    }

    suspend fun DynamicMessage.buildMessage(template: String): List<OBMessage> {
        val msgList = mutableListOf<OBMessage>()
        val msgTemplate = template.replace("\n", "\\n").replace("\r", "\\r")
        val forwardCardTemplate = templateConfig.forwardCard
        val res = forwardRegex.findAll(msgTemplate)
        var index = 0

        res.forEach { mr ->
            if (mr.range.first > index) {
                msgList.addAll(buildMsgList(msgTemplate.substring(index, mr.range.first)) {
                    buildMsg(it, this)
                })
            }
            val msg = buildMsgList(mr.destructured.component1()) {
                buildMsg(it, this@buildMessage)
            }
            if (msg.isNotEmpty()) {
                // Build forward message nodes
                val nodes = buildForwardNodes {
                    msg.forEach { segments ->
                        node(
                            BotInstance.botId,
                            this@buildMessage.name,
                            segments,
                            this@buildMessage.timestamp
                        )
                    }
                }
                // Instead of sending forward directly, wrap it as a marker
                // The actual forward sending will happen in the send loop
                msgList.add(listOf(MessageSegment("_forward", kotlinx.serialization.json.buildJsonObject {
                    put("nodes", kotlinx.serialization.json.Json.encodeToJsonElement(
                        kotlinx.serialization.builtins.ListSerializer(MessageSegment.serializer()), nodes))
                    put("brief", kotlinx.serialization.json.JsonPrimitive(buildSimpleMsg(forwardCardTemplate.brief, this@buildMessage)))
                    put("summary", kotlinx.serialization.json.JsonPrimitive(buildSimpleMsg(forwardCardTemplate.summary, this@buildMessage)))
                    put("title", kotlinx.serialization.json.JsonPrimitive(buildSimpleMsg(forwardCardTemplate.title, this@buildMessage)))
                    put("preview", kotlinx.serialization.json.JsonPrimitive(buildSimpleMsg(forwardCardTemplate.preview, this@buildMessage)))
                })))
            }
            index = mr.range.last + 1
        }

        if (index < msgTemplate.length) {
            msgList.addAll(buildMsgList(msgTemplate.substring(index, msgTemplate.length)) {
                buildMsg(it, this)
            })
        }

        return msgList
    }

    private inline fun buildMsgList(template: String, build: (ms: String) -> String): List<OBMessage> {
        val msgs = template.split("\\r", "\r")
        val msgList = mutableListOf<OBMessage>()
        msgs.forEach { ms ->
            build(ms).let {
                if (it.isNotBlank()) msgList.add(parseMessageContent(it.replace("\\n", "\n")))
            }
        }
        return msgList.toList()
    }

    private fun buildSimpleMsg(ms: String, dm: DynamicMessage): String {
        return ms.replace("{name}", dm.name)
            .replace("{uid}", dm.mid.toString())
            .replace("{did}", dm.did)
            .replace("{time}", dm.time)
            .replace("{type}", dm.type.text)
            .replace("{content}", dm.content)
            .replace("{link}", dm.links?.get(0)?.value!!)
    }

    public suspend fun buildMsg(ms: String, dm: DynamicMessage): String {
        var p = 0
        var content = ms

        while (true) {
            val key = tagRegex.find(content, p) ?: break
            val rep = when (key.destructured.component1()) {
                "name" -> dm.name
                "uid" -> dm.mid.toString()
                "did" -> dm.did
                "time" -> dm.time
                "type" -> dm.type.text
                "content" -> dm.content
                "link" -> dm.links?.get(0)?.value!!
                "links" -> dm.links?.joinToString("\n") { it.value }!!
                "images" -> buildString {
                    dm.images?.forEach {
                        appendLine(uploadImage(it, CacheType.IMAGES) ?: "")
                    }
                }
                "draw" -> if (dm.drawPath == null) "[绘制动态失败]" else {
                    val path = cachePath.resolve(dm.drawPath)
                    if (path.notExists()) "[未找到绘制的动态]"
                    else uploadImage(path) ?: "[上传图片失败]"
                }
                else -> "[不支持的类型: ${key.destructured.component1()}]"
            }
            content = content.replaceRange(key.range, rep)
            p = key.range.first + rep.length
        }
        return content
    }

    fun LiveCloseMessage.buildMessage(template: String) = listOf(
        listOf(MessageSegment.text(buildCloseMsg(template, this)))
    )

    private fun buildCloseMsg(ms: String, lcm: LiveCloseMessage): String {
        return ms.replace("{name}", lcm.name)
            .replace("{uid}", lcm.mid.toString())
            .replace("{rid}", lcm.rid.toString())
            .replace("{startTime}", lcm.time)
            .replace("{endTime}", lcm.endTime)
            .replace("{duration}", lcm.duration)
            .replace("{title}", lcm.title)
            .replace("{area}", lcm.area)
            .replace("{link}", lcm.link)
    }

    /**
     * Custom send that handles forward message markers.
     */
    private suspend fun OBContact.sendMessage(msg: OBMessage): Long {
        // Check if this is a forward message marker
        if (msg.size == 1 && msg[0].type == "_forward") {
            val data = msg[0].data
            val nodesElement = data["nodes"]!!
            val nodes = kotlinx.serialization.json.Json.decodeFromJsonElement(
                kotlinx.serialization.builtins.ListSerializer(MessageSegment.serializer()), nodesElement)
            val summary = data["summary"]?.jsonPrimitive?.content
            val prompt = data["brief"]?.jsonPrimitive?.content
            val preview = data["preview"]?.jsonPrimitive?.content
            val news = preview?.split("\\n", "\n")

            return if (this is OBGroup) {
                BotInstance.client.sendGroupForwardMsg(id, nodes, summary = summary, prompt = prompt, news = news)
            } else {
                BotInstance.client.sendPrivateForwardMsg(id, nodes, summary = summary, prompt = prompt, news = news)
            }
        }
        return sendMessage(msg)
    }
}

private val kotlinx.serialization.json.JsonElement.jsonPrimitive
    get() = this as? kotlinx.serialization.json.JsonPrimitive
