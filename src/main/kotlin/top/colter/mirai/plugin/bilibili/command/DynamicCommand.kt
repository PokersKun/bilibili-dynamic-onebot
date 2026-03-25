package top.colter.mirai.plugin.bilibili.command

import top.colter.mirai.plugin.bilibili.*
import top.colter.mirai.plugin.bilibili.api.getDynamicDetail
import top.colter.mirai.plugin.bilibili.api.getLive
import top.colter.mirai.plugin.bilibili.api.getUserNewDynamic
import top.colter.mirai.plugin.bilibili.api.searchUserVideo
import top.colter.mirai.plugin.bilibili.data.DynamicDetail
import top.colter.mirai.plugin.bilibili.data.LiveDetail
import top.colter.mirai.plugin.bilibili.onebot.*
import top.colter.mirai.plugin.bilibili.service.*
import top.colter.mirai.plugin.bilibili.utils.*
import top.colter.mirai.plugin.bilibili.utils.logger as utilsLogger

object DynamicCommand {
    private val logger get() = utilsLogger

    private val showLoadingMessage get() = BiliConfig.enableConfig.showLoadingMessage

    fun register() {
        CommandDispatcher.register("help", "h", "帮助", "menu", description = "查看帮助信息") { ctx -> help(ctx) }
        CommandDispatcher.register("reload", "重载", description = "重载配置文件") { ctx -> reload(ctx) }
        CommandDispatcher.register("color", "颜色", description = "设置主题色 <用户> <颜色>") { ctx -> color(ctx) }
        CommandDispatcher.register("add", "follow", "添加", "订阅", description = "订阅UP主 <UID> [目标]") { ctx -> add(ctx) }
        CommandDispatcher.register("del", "unfollow", "删除", description = "取消订阅 <用户> [目标]") { ctx -> del(ctx) }
        CommandDispatcher.register("delall", "删除全部订阅", description = "删除全部订阅 [目标]") { ctx -> delAll(ctx) }
        CommandDispatcher.register("list", "列表", description = "查看订阅列表 [目标]") { ctx -> list(ctx) }
        CommandDispatcher.register("listall", "la", "全部订阅列表", description = "查看全部订阅") { ctx -> listAll(ctx) }
        CommandDispatcher.register("listuser", "lu", "用户列表", description = "查看已订阅用户 [用户]") { ctx -> listUser(ctx) }
        CommandDispatcher.register("filtermode", "fm", "过滤模式", description = "设置过滤模式 <t|r> <w|b> [uid] [目标]") { ctx -> filterMode(ctx) }
        CommandDispatcher.register("filtertype", "ft", "类型过滤", description = "添加类型过滤 <类型> [uid] [目标]") { ctx -> filterType(ctx) }
        CommandDispatcher.register("filterreg", "fr", "正则过滤", description = "添加正则过滤 <正则> [uid] [目标]") { ctx -> filterReg(ctx) }
        CommandDispatcher.register("filterlist", "fl", "过滤列表", description = "查看过滤列表 [uid] [目标]") { ctx -> filterList(ctx) }
        CommandDispatcher.register("filterdel", "fd", "过滤删除", description = "删除过滤项 <序号> [uid] [目标]") { ctx -> filterDel(ctx) }
        CommandDispatcher.register("templatelist", "tl", "模板列表", description = "查看推送模板 [类型:d|l|le]") { ctx -> templateList(ctx) }
        CommandDispatcher.register("template", "t", "模板", description = "设置推送模板 <类型:d|l|le> <模板名> [目标]") { ctx -> template(ctx) }
        CommandDispatcher.register("login", "登录", description = "扫码登录B站") { ctx -> login(ctx) }
        CommandDispatcher.register("atall", "aa", "at全体", description = "添加@全体 [类型] [用户] [目标]") { ctx -> atall(ctx) }
        CommandDispatcher.register("delatall", "daa", "取消at全体", description = "取消@全体 [类型] [用户] [目标]") { ctx -> delAtall(ctx) }
        CommandDispatcher.register("listatall", "laa", "at全体列表", description = "查看@全体列表 [用户] [目标]") { ctx -> listAtall(ctx) }
        CommandDispatcher.register("config", "配置", description = "交互式配置 [用户] [目标]") { ctx -> config(ctx) }
        CommandDispatcher.register("search", "s", "搜索", description = "获取动态详情 <动态ID>") { ctx -> search(ctx) }
        CommandDispatcher.register("live", "直播", description = "查看当前直播状态") { ctx -> live(ctx) }
        CommandDispatcher.register("new", "最新动态", description = "获取最新动态 <用户> [数量]") { ctx -> new(ctx) }
        CommandDispatcher.register("video", "最新视频", description = "获取最新视频 <用户>") { ctx -> newVideo(ctx) }
        CommandDispatcher.register("create", "创建分组", description = "创建分组 <分组名>") { ctx -> createGroup(ctx) }
        CommandDispatcher.register("listgroup", "lg", "分组列表", description = "查看分组列表 [分组名]") { ctx -> listGroup(ctx) }
        CommandDispatcher.register("delgroup", "dg", "删除分组", description = "删除分组 <分组名>") { ctx -> delGroup(ctx) }
        CommandDispatcher.register("addgroupadmin", "aga", "添加分组管理员", description = "添加分组管理员 <分组名> <联系人>") { ctx -> setGroupAdmin(ctx) }
        CommandDispatcher.register("bangroupadmin", "bga", "删除分组管理员", description = "删除分组管理员 <分组名> <联系人>") { ctx -> banGroupAdmin(ctx) }
        CommandDispatcher.register("push", "添加分组", description = "推送到分组 <分组名> <联系人>") { ctx -> pushGroup(ctx) }
        CommandDispatcher.register("ban", description = "从分组移除 <分组名> <联系人>") { ctx -> delGroupContact(ctx) }
        CommandDispatcher.register("clear", description = "清理无效订阅") { ctx -> clear(ctx) }
    }

    // ==================== Helper ====================

    private fun resolveTarget(ctx: CommandContext, argIndex: Int = -1): GroupOrContact {
        val raw = if (argIndex >= 0 && argIndex < ctx.args.size) ctx.args[argIndex] else null
        return if (raw != null) {
            parseGroupOrContact(raw, ctx.contact)
        } else {
            GroupOrContact(contact = ctx.contact)
        }
    }

    // ==================== Commands ====================

    private suspend fun help(ctx: CommandContext) {
        val imgBytes = loadResourceBytes("image/HELP.png")
        ctx.sendMessage(listOf(MessageSegment.image(imageCodeRaw(imgBytes))))
    }

    private suspend fun reload(ctx: CommandContext) {
        BiliConfig.reload()
        ctx.sendMessage("配置重载成功")
    }

    private suspend fun color(ctx: CommandContext) {
        val user = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili color <用户> <颜色>").let {}
        val color = ctx.args.getOrNull(1) ?: return ctx.sendMessage("用法: /bili color <用户> <颜色>").let {}
        matchUser(user) {
            DynamicService.setColor(it, color)
        }?.let {
            ctx.sendMessage(it)
            actionNotify(ctx.contact.id, ctx.contact.contactName, user, "修改主题色", it)
        }
    }

    private suspend fun add(ctx: CommandContext) {
        val id = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili add <ID> [目标]").let {}
        val target = resolveTarget(ctx, 1)

        if (pgcRegex.matches(id)) {
            ctx.sendMessage(PgcService.followPgc(id, target.subject))
        } else try {
            DynamicService.addSubscribe(id.toLong(), target.subject, ctx.contact.delegate == target.subject).let {
                ctx.sendMessage(it)
                actionNotify(ctx.contact.id, ctx.contact.contactName, target.name, "订阅", it)
            }
        } catch (e: NumberFormatException) {
            ctx.sendMessage("ID错误 [$id]")
        } catch (e: Exception) {
            ctx.sendMessage("订阅失败 ${e.message}")
        }
    }

    private suspend fun del(ctx: CommandContext) {
        val id = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili del <ID> [目标]").let {}
        val target = resolveTarget(ctx, 1)

        if (pgcRegex.matches(id)) {
            ctx.sendMessage(PgcService.delPgc(id, target.subject))
        } else matchUser(id) {
            DynamicService.removeSubscribe(it, target.subject, ctx.contact.delegate == target.subject)
        }?.let {
            ctx.sendMessage(it)
            actionNotify(ctx.contact.id, ctx.contact.contactName, target.name, "取消订阅", it)
        }
    }

    private suspend fun delAll(ctx: CommandContext) {
        val target = resolveTarget(ctx, 0)

        val msg = DynamicService.removeAllSubscribe(target.subject).let { "删除订阅成功! 共删除 $it 个订阅" }
        ctx.sendMessage(msg)
        actionNotify(ctx.contact.id, ctx.contact.contactName, target.name, "取消全部订阅", msg)
    }

    private suspend fun list(ctx: CommandContext) {
        val target = resolveTarget(ctx, 0)

        ctx.sendMessage(DynamicService.list(target.subject))
    }

    private suspend fun listAll(ctx: CommandContext) {
        ctx.sendMessage(DynamicService.listAll())
    }

    private suspend fun listUser(ctx: CommandContext) {
        val user = ctx.args.getOrNull(0) ?: ""
        if (user.isEmpty()) {
            ctx.sendMessage(DynamicService.listUser())
        } else {
            matchUser(user) {
                DynamicService.listUser(it)
            }?.let { ctx.sendMessage(it) }
        }
    }

    private suspend fun filterMode(ctx: CommandContext) {
        // /bili fm <t|r> <w|b> [uid] [target]
        val type = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili fm <t|r> <w|b> [uid] [目标]").let {}
        val mode = ctx.args.getOrNull(1) ?: return ctx.sendMessage("用法: /bili fm <t|r> <w|b> [uid] [目标]").let {}
        val uid = ctx.args.getOrNull(2)?.toLongOrNull() ?: 0L
        val target = resolveTarget(ctx, 3)

        ctx.sendMessage(
            FilterService.addFilter(
                if (type == "t") FilterType.TYPE else FilterType.REGULAR,
                if (mode == "w") FilterMode.WHITE_LIST else FilterMode.BLACK_LIST,
                null, uid, target.subject
            )
        )
    }

    private suspend fun filterType(ctx: CommandContext) {
        val type = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili ft <类型> [uid] [目标]").let {}
        val uid = ctx.args.getOrNull(1)?.toLongOrNull() ?: 0L
        val target = resolveTarget(ctx, 2)

        ctx.sendMessage(FilterService.addFilter(FilterType.TYPE, null, type, uid, target.subject))
    }

    private suspend fun filterReg(ctx: CommandContext) {
        val reg = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili fr <正则> [uid] [目标]").let {}
        val uid = ctx.args.getOrNull(1)?.toLongOrNull() ?: 0L
        val target = resolveTarget(ctx, 2)

        ctx.sendMessage(FilterService.addFilter(FilterType.REGULAR, null, reg, uid, target.subject))
    }

    private suspend fun filterList(ctx: CommandContext) {
        val uid = ctx.args.getOrNull(0)?.toLongOrNull() ?: 0L
        val target = resolveTarget(ctx, 1)

        ctx.sendMessage(FilterService.listFilter(uid, target.subject))
    }

    private suspend fun filterDel(ctx: CommandContext) {
        val index = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili fd <索引> [uid] [目标]").let {}
        val uid = ctx.args.getOrNull(1)?.toLongOrNull() ?: 0L
        val target = resolveTarget(ctx, 2)

        ctx.sendMessage(FilterService.delFilter(index, uid, target.subject))
    }

    private suspend fun templateList(ctx: CommandContext) {
        val type = ctx.args.getOrNull(0) ?: "d"
        val ms = if (showLoadingMessage) ctx.sendMessage("加载中...") else null
        TemplateService.listTemplate(type, ctx.contact)
        ms?.let { ctx.deleteMessage(it) }
    }

    private suspend fun template(ctx: CommandContext) {
        val type = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili t <类型> <模板> [目标]").let {}
        val templateName = ctx.args.getOrNull(1) ?: return ctx.sendMessage("用法: /bili t <类型> <模板> [目标]").let {}
        val target = resolveTarget(ctx, 2)

        ctx.sendMessage(TemplateService.setTemplate(type, templateName, target.subject))
    }

    private suspend fun login(ctx: CommandContext) {
        LoginService.login(ctx.contact)
    }

    private suspend fun atall(ctx: CommandContext) {
        val type = ctx.args.getOrNull(0) ?: "a"
        val user = ctx.args.getOrNull(1) ?: "0"
        val target = resolveTarget(ctx, 2)

        matchUser(user) {
            AtAllService.addAtAll(type, it, target)
        }?.let { ctx.sendMessage(it) }
    }

    private suspend fun delAtall(ctx: CommandContext) {
        val type = ctx.args.getOrNull(0) ?: "a"
        val user = ctx.args.getOrNull(1) ?: "0"
        val target = resolveTarget(ctx, 2)

        matchUser(user) {
            AtAllService.delAtAll(type, it, target.subject)
        }?.let { ctx.sendMessage(it) }
    }

    private suspend fun listAtall(ctx: CommandContext) {
        val user = ctx.args.getOrNull(0) ?: "0"
        val target = resolveTarget(ctx, 1)

        matchUser(user) {
            AtAllService.listAtAll(it, target.subject)
        }?.let { ctx.sendMessage(it) }
    }

    private suspend fun config(ctx: CommandContext) {
        val user = ctx.args.getOrNull(0) ?: "0"
        val target = resolveTarget(ctx, 1)

        if (user == "0") {
            ConfigService.config(ctx, 0, target.contact ?: ctx.contact)
        } else {
            matchUser(user) {
                ConfigService.config(ctx, it, target.contact ?: ctx.contact)
                null
            }?.let { ctx.sendMessage(it) }
        }
    }

    private suspend fun search(ctx: CommandContext) {
        val did = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili search <动态ID>").let {}
        val msg = if (showLoadingMessage) ctx.sendMessage("加载中...") else null
        try {
            val detail = biliClient.getDynamicDetail(did)
            if (detail != null) {
                BiliBiliDynamic.dynamicChannel.send(DynamicDetail(detail, ctx.contact.delegate))
            } else {
                ctx.sendMessage("未找到动态")
            }
        } catch (e: Exception) {
            ctx.sendMessage("获取动态失败 ${e.message}")
            logger.error("获取动态失败", e)
        }
        msg?.let { ctx.deleteMessage(it) }
    }

    private suspend fun live(ctx: CommandContext) {
        val detail = biliClient.getLive(1, 1)
        if (detail != null) {
            ctx.sendMessage("加载中...")
            BiliBiliDynamic.liveChannel.send(LiveDetail(detail.rooms.first(), ctx.contact.delegate))
        } else {
            ctx.sendMessage("当前没有人在直播")
        }
    }

    private suspend fun new(ctx: CommandContext) {
        val user = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili new <用户> [数量]").let {}
        val count = ctx.args.getOrNull(1)?.toIntOrNull() ?: 1
        val msg = if (showLoadingMessage) ctx.sendMessage("加载中...") else null
        matchUser(user) {
            try {
                val list = biliClient.getUserNewDynamic(it)?.items?.subList(0, count)
                list?.forEach { di ->
                    BiliBiliDynamic.dynamicChannel.send(DynamicDetail(di, ctx.contact.delegate))
                }
                if (list.isNullOrEmpty()) "未找到动态" else null
            } catch (e: Exception) {
                logger.error("获取动态失败", e)
                "获取动态失败 ${e.message}"
            }
        }?.let { ctx.sendMessage(it) }
        msg?.let { ctx.deleteMessage(it) }
    }

    private suspend fun newVideo(ctx: CommandContext) {
        val user = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili video <用户>").let {}
        val msg = if (showLoadingMessage) ctx.sendMessage("加载中...") else null
        matchUser(user) {
            try {
                biliClient.searchUserVideo(it)?.list?.vlist?.run {
                    if (isNotEmpty()) {
                        val video = first()
                        val type = matchingRegular(video.bvid)
                        val img = type?.drawGeneral() ?: return@run "解析失败"
                        val imgMsg = uploadImage(img, CacheType.DRAW_SEARCH) ?: return@run "图片上传失败"
                        val segments = mutableListOf<MessageSegment>()
                        segments.addAll(parseMessageContent(imgMsg))
                        if (BiliConfig.linkResolveConfig.returnLink) {
                            segments.add(MessageSegment.text(type.getLink()))
                        }
                        ctx.sendMessage(segments)
                        null
                    } else {
                        "未找到视频"
                    }
                }
            } catch (e: Exception) {
                logger.error("获取视频失败", e)
                "获取视频失败 ${e.message}"
            }
        }?.let { ctx.sendMessage(it) }
        msg?.let { ctx.deleteMessage(it) }
    }

    private suspend fun createGroup(ctx: CommandContext) {
        val name = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili create <分组名>").let {}
        ctx.sendMessage(GroupService.createGroup(name, ctx.contact.id))
    }

    private suspend fun listGroup(ctx: CommandContext) {
        val name = ctx.args.getOrNull(0)
        ctx.sendMessage(GroupService.listGroup(name, ctx.contact.id))
    }

    private suspend fun delGroup(ctx: CommandContext) {
        val name = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili dg <分组名>").let {}
        ctx.sendMessage(GroupService.delGroup(name, ctx.contact.id))
    }

    private suspend fun setGroupAdmin(ctx: CommandContext) {
        val name = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili aga <分组名> <联系人>").let {}
        val contacts = ctx.args.getOrNull(1) ?: return ctx.sendMessage("用法: /bili aga <分组名> <联系人>").let {}
        ctx.sendMessage(GroupService.setGroupAdmin(name, contacts, ctx.contact.id))
    }

    private suspend fun banGroupAdmin(ctx: CommandContext) {
        val name = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili bga <分组名> <联系人>").let {}
        val contacts = ctx.args.getOrNull(1) ?: return ctx.sendMessage("用法: /bili bga <分组名> <联系人>").let {}
        ctx.sendMessage(GroupService.banGroupAdmin(name, contacts, ctx.contact.id))
    }

    private suspend fun pushGroup(ctx: CommandContext) {
        val name = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili push <分组名> <联系人>").let {}
        val contacts = ctx.args.getOrNull(1) ?: return ctx.sendMessage("用法: /bili push <分组名> <联系人>").let {}
        ctx.sendMessage(GroupService.pushGroupContact(name, contacts, ctx.contact.id))
    }

    private suspend fun delGroupContact(ctx: CommandContext) {
        val name = ctx.args.getOrNull(0) ?: return ctx.sendMessage("用法: /bili ban <分组名> <联系人>").let {}
        val contacts = ctx.args.getOrNull(1) ?: return ctx.sendMessage("用法: /bili ban <分组名> <联系人>").let {}
        ctx.sendMessage(GroupService.delGroupContact(name, contacts, ctx.contact.id))
    }

    private suspend fun clear(ctx: CommandContext) {
        val map = mutableMapOf<String, MutableList<Long>>()
        BiliData.dynamic.forEach { (uid, sub) ->
            sub.contacts.forEach {
                try {
                    it.toLong()
                    if (findContact(it) == null) {
                        map.getOrPut(it) { mutableListOf() }.add(uid)
                    }
                } catch (_: NumberFormatException) {}
            }
        }
        if (map.isEmpty()) {
            ctx.sendMessage("未找到失效的群/好友")
            return
        }
        ctx.sendMessage("发现以下失效的群/好友：\n\n${map.keys.joinToString("\n")}\n\n带负号的为群\n确认删除这些用户的订阅吗\n请回复 '确定' 或 '取消'")
        try {
            val reply = ctx.nextMessage(300000)
            if (reply == "确定") {
                map.forEach { (c, u) ->
                    u.forEach { DynamicService.removeSubscribe(it, c) }
                }
                ctx.sendMessage("删除成功")
            } else {
                ctx.sendMessage("已取消")
            }
        } catch (_: Exception) {
            // timeout
        }
    }

    /**
     * Create a base64 image URI from raw bytes for direct sending.
     */
    private fun imageCodeRaw(bytes: ByteArray): String {
        val b64 = java.util.Base64.getEncoder().encodeToString(bytes)
        return "base64://$b64"
    }
}
