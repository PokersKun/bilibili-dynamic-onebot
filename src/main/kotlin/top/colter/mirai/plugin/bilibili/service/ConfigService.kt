package top.colter.mirai.plugin.bilibili.service

import top.colter.mirai.plugin.bilibili.*
import top.colter.mirai.plugin.bilibili.command.GroupOrContact
import top.colter.mirai.plugin.bilibili.onebot.*
import top.colter.mirai.plugin.bilibili.service.FilterService.addFilter
import top.colter.mirai.plugin.bilibili.service.FilterService.delFilter
import top.colter.mirai.plugin.bilibili.service.FilterService.listFilter
import top.colter.mirai.plugin.bilibili.service.TemplateService.setTemplate

object ConfigService {
    suspend fun config(ctx: CommandContext, uid: Long = 0L, contact: OBContact) {
        if (uid != 0L && !((dynamic.containsKey(uid) && dynamic[uid]!!.contacts.contains(contact.delegate)))) {
            ctx.sendMessage("没有订阅这个人哦 [$uid]")
            return
        }
        val user = if (uid != 0L) dynamic[uid] else null
        val configMap = mutableMapOf<String, String>()

        val configMsgId = ctx.sendMessage(buildString {
            appendLine("配置: ")
            append("用户: ")
            appendLine(if (uid == 0L) "全局" else user?.name)
            append("目标: ")
            appendLine(if (ctx.contact.id == contact.id) "当前环境" else contact.contactName)
            appendLine()
            appendLine("当前可配置项:")
            var i = 1
            if (contact is OBGroup) {
                configMap[i.toString()] = "ATALL"
                val aa = atAll[contact.delegate]?.get(uid)?.isNotEmpty()
                appendLine("  $i: At全体 [${aa ?: false}]")
                appendLine("      $i.1: 当前At全体项")
                appendLine("      $i.2: 添加At全体")
                appendLine("      $i.3: 删除At全体")
                i++
            }
            if (uid != 0L) {
                configMap[i.toString()] = "COLOR"
                appendLine("  ${i++}: 主题色 [${user?.color ?: BiliConfig.imageConfig.defaultColor}]")
            }
            if (uid == 0L) {
                val cdl = BiliData.dynamicPushTemplate.filter { it.value.contains(contact.delegate) }.map { it.key }
                val currDynamic = if (cdl.isNotEmpty()) cdl.first() else BiliConfig.templateConfig.defaultDynamicPush
                val cll = BiliData.livePushTemplate.filter { it.value.contains(contact.delegate) }.map { it.key }
                val currLive = if (cll.isNotEmpty()) cll.first() else BiliConfig.templateConfig.defaultLivePush
                val clel = BiliData.liveCloseTemplate.filter { it.value.contains(contact.delegate) }.map { it.key }
                val currLiveClose = if (clel.isNotEmpty()) clel.first() else BiliConfig.templateConfig.defaultLiveClose
                configMap[i.toString()] = "PUSH"
                appendLine("  $i: 推送模板")
                appendLine("      $i.1: 动态推送模板 [$currDynamic]")
                appendLine("      $i.2: 直播推送模板 [$currLive]")
                appendLine("      $i.3: 直播结束模板 [$currLiveClose]")
                i++
            }

            val filterData = BiliData.filter[contact.delegate]?.get(uid)
            val mode = if (filterData == null) "无过滤器" else
                "类型: ${filterData.typeSelect.mode.value} | 正则: ${filterData.regularSelect.mode.value}"

            configMap[i.toString()] = "FILTER"
            appendLine("  $i: 过滤器")
            appendLine("      $i.1: 过滤器列表")
            appendLine("      $i.2: 添加类型过滤器")
            appendLine("      $i.3: 添加正则过滤器")
            appendLine("      $i.4: 切换过滤模式 [$mode]")
            appendLine("      $i.5: 删除过滤器")
            appendLine()
            append("[中括号]内为当前值\n请输入编号, 2分钟未回复自动退出\n或回复 退出 来主动退出")
        })

        var regMsgId: Long? = null

        while (true) {
            val input = waitForInput(ctx) ?: return
            regMsgId?.let { ctx.deleteMessage(it) }

            if (input == "退出") {
                ctx.sendMessage("已退出")
                return
            }

            // Find matching config key
            val matchedKey = configMap.keys.find { input.startsWith(it) }
            if (matchedKey == null) {
                ctx.sendMessage("没有这个选项哦, 请重新输入")
                val input2 = waitForInput(ctx) ?: return
                if (input2 == "退出") { ctx.sendMessage("已退出"); return }
                val matchedKey2 = configMap.keys.find { input2.startsWith(it) }
                if (matchedKey2 == null) {
                    ctx.sendMessage("超出重试次数, 退出")
                    return
                }
                handleConfigOption(ctx, configMap[matchedKey2]!!, input2, uid, contact)
            } else {
                handleConfigOption(ctx, configMap[matchedKey]!!, input, uid, contact)
            }

            regMsgId = ctx.sendMessage(
                listOf(
                    MessageSegment.reply(configMsgId),
                    MessageSegment.text("输入编号以继续\n不回复或回复 退出 来退出")
                )
            )
        }
    }

    private suspend fun handleConfigOption(
        ctx: CommandContext, configType: String, input: String,
        uid: Long, contact: OBContact
    ) {
        when (configType) {
            "ATALL" -> handleAtAll(ctx, input, uid, contact)
            "COLOR" -> handleColor(ctx, uid)
            "PUSH" -> handlePush(ctx, input, contact)
            "FILTER" -> handleFilter(ctx, input, uid, contact)
        }
    }

    private suspend fun handleAtAll(ctx: CommandContext, input: String, uid: Long, contact: OBContact) {
        val b = input.split(".").last()
        when (b) {
            "1" -> ctx.sendMessage(AtAllService.listAtAll(uid, contact.delegate))
            "2" -> {
                ctx.sendMessage(buildString {
                    appendLine("请选择要At全体的内容: ")
                    appendLine("  全部")
                    appendLine("  ├─ 全部动态")
                    appendLine("  │   ├─ 视频")
                    appendLine("  │   ├─ 音乐")
                    appendLine("  │   └─ 专栏")
                    appendLine("  └─ 直播")
                })
                val typeInput = waitForInputWithRetry(ctx, AtAllType.values().map { it.value }.toSet()) ?: return
                ctx.sendMessage(AtAllService.addAtAll(typeInput, uid, GroupOrContact(contact)))
            }
            "3" -> {
                val list = atAll[contact.delegate]?.get(uid)
                if (list == null || list.isEmpty()) {
                    ctx.sendMessage("没有At全体哦")
                    return
                }
                ctx.sendMessage("At全体项:\n" + AtAllService.listAtAll(uid, contact.delegate) + "\n请回复要删除的项")
                val type = waitForInput(ctx) ?: return
                ctx.sendMessage(AtAllService.delAtAll(type, uid, contact.delegate))
            }
        }
    }

    private suspend fun handleColor(ctx: CommandContext, uid: Long) {
        ctx.sendMessage("请输入16进制颜色，例如: #d3edfa")
        var count = 0
        while (count < 2) {
            val colorInput = waitForInput(ctx) ?: return
            if (colorInput == "退出") { ctx.sendMessage("已退出"); return }
            if (colorInput.firstOrNull() != '#' || colorInput.length != 7) {
                ctx.sendMessage("格式错误，请输入16进制颜色，例如: #d3edfa")
                count++
            } else {
                ctx.sendMessage(DynamicService.setColor(uid, colorInput))
                return
            }
        }
    }

    private suspend fun handlePush(ctx: CommandContext, input: String, contact: OBContact) {
        val b = input.split(".").last()
        val template = when (b) {
            "1" -> BiliConfig.templateConfig.dynamicPush
            "2" -> BiliConfig.templateConfig.livePush
            "3" -> BiliConfig.templateConfig.liveClose
            else -> {
                ctx.sendMessage("没有这个选项哦")
                return
            }
        }
        ctx.sendMessage("请选择一个推送模板, 回复模板名\n生成模板需要一定时间...")
        TemplateService.listTemplate(if (b == "1") "d" else if (b == "2") "l" else "le", contact)
        val templateInput = waitForInputWithRetry(ctx, template.keys) ?: return
        ctx.sendMessage(setTemplate(if (b == "1") "d" else if (b == "2") "l" else "le", templateInput, contact.delegate))
    }

    private suspend fun handleFilter(ctx: CommandContext, input: String, uid: Long, contact: OBContact) {
        val b = input.split(".").last()
        val filterData = BiliData.filter[contact.delegate]?.get(uid)
        when (b) {
            "1" -> ctx.sendMessage(listFilter(uid, contact.delegate))
            "2" -> {
                val mode = filterData?.typeSelect?.mode?.value ?: "黑名单"
                val types = DynamicFilterType.values().joinToString("\n    ") { it.value }
                ctx.sendMessage("当前过滤器类型: $mode\n支持的类型: \n    $types\n请回复要过滤的类型")
                val typeInput = waitForInputWithRetry(ctx, DynamicFilterType.values().map { it.value }.toSet()) ?: return
                ctx.sendMessage(addFilter(FilterType.TYPE, null, typeInput, uid, contact.delegate))
            }
            "3" -> {
                val mode = filterData?.regularSelect?.mode?.value ?: "黑名单"
                ctx.sendMessage("当前过滤器类型: $mode\n请回复过滤文本或正则")
                val reg = waitForInput(ctx) ?: return
                if (reg.isNotEmpty()) {
                    ctx.sendMessage(addFilter(FilterType.REGULAR, null, reg, uid, contact.delegate))
                }
            }
            "4" -> {
                val typeMode = filterData?.typeSelect?.mode?.value ?: "黑名单"
                val regMode = filterData?.regularSelect?.mode?.value ?: "黑名单"
                ctx.sendMessage("类型过滤器: $typeMode\n正则过滤器: $regMode\n请选择要切换的过滤的类型\nt: 类型过滤器\nr: 正则过滤器")
                val selectInput = waitForInputWithRetry(ctx, setOf("t", "r")) ?: return
                val selectType = if (selectInput == "t") FilterType.TYPE else FilterType.REGULAR
                val selectMode = if (selectInput == "t") {
                    filterData?.typeSelect?.mode ?: FilterMode.BLACK_LIST
                } else {
                    filterData?.regularSelect?.mode ?: FilterMode.BLACK_LIST
                }
                val newMode = if (selectMode == FilterMode.BLACK_LIST) FilterMode.WHITE_LIST else FilterMode.BLACK_LIST
                ctx.sendMessage(addFilter(selectType, newMode, null, uid, contact.delegate))
            }
            "5" -> {
                ctx.sendMessage(listFilter(uid, contact.delegate))
                val reg = waitForInput(ctx) ?: return
                ctx.sendMessage(delFilter(reg, uid, contact.delegate))
            }
        }
    }

    // ==================== Input Helpers ====================

    private suspend fun waitForInput(ctx: CommandContext, timeout: Long = 120_000): String? {
        return try {
            ctx.nextMessage(timeout)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Wait for input matching one of the valid options, with retry.
     */
    private suspend fun waitForInputWithRetry(
        ctx: CommandContext,
        validOptions: Set<String>,
        maxRetries: Int = 2,
        timeout: Long = 120_000
    ): String? {
        var retries = 0
        while (retries < maxRetries) {
            val input = waitForInput(ctx, timeout) ?: return null
            if (input == "退出") { ctx.sendMessage("已退出"); return null }
            if (validOptions.contains(input)) return input
            retries++
            if (retries < maxRetries) {
                ctx.sendMessage("没有这个选项哦, 请重新输入")
            } else {
                ctx.sendMessage("超出重试次数, 退出")
                return null
            }
        }
        return null
    }
}
