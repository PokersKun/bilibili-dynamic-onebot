package top.colter.mirai.plugin.bilibili

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import top.colter.mirai.plugin.bilibili.onebot.BotInstance
import top.colter.mirai.plugin.bilibili.onebot.OneBotClient
import java.io.File

fun main(args: Array<String>) = runBlocking {
    val dataDir = File(args.getOrElse(0) { "./data" }).canonicalFile
    val configDir = File(args.getOrElse(1) { "./config" }).canonicalFile
    val logger = BiliBiliDynamic.logger

    logger.info("=== BiliBili Dynamic (OneBot 11) ===")
    logger.info("数据目录: ${dataDir.absolutePath}")
    logger.info("配置目录: ${configDir.absolutePath}")

    // 1. Initialize paths and load configs
    BiliBiliDynamic.init(dataDir, configDir)
    OneBotConfig.init(configDir)
    BiliBiliDynamic.onEnable()

    // 2. Create and connect OneBot client
    val wsUrl = OneBotConfig.wsUrl
    val token = OneBotConfig.token
    logger.info("OneBot WebSocket 地址: $wsUrl")
    if (token.isNotEmpty()) logger.info("使用 Token 认证")

    val client = OneBotClient(wsUrl, BiliBiliDynamic, token)
    BotInstance.init(client)

    // Launch WebSocket connection in background
    val connectJob = BiliBiliDynamic.launch {
        client.connect()
    }

    // Wait for connection
    var retries = 0
    while (!client.connected && retries < 30) {
        kotlinx.coroutines.delay(1000)
        retries++
    }
    if (!client.connected) {
        logger.error("无法连接到 OneBot WebSocket: $wsUrl")
        logger.error("请确保 OneBot 实现已启动并监听在 $wsUrl")
        return@runBlocking
    }

    // 3. Fetch bot info and cache contacts
    try {
        BotInstance.fetchLoginInfo()
        BotInstance.refreshContacts()
    } catch (e: Exception) {
        logger.error("获取Bot信息失败: ${e.message}")
        return@runBlocking
    }

    // 4. Initialize BiliBili data (cookie, tagid, fonts)
    BiliBiliDynamic.launch {
        try {
            initData()
        } catch (e: Exception) {
            logger.error("初始化数据失败: ${e.message}", e)
        }
    }

    // 5. Start taskers
    BiliBiliDynamic.onStartup()

    // 6. Register shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("正在关闭...")
        BiliBiliDynamic.shutdown()
        client.close()
        logger.info("已关闭")
    })

    logger.info("=== 启动完成 ===")

    // Block main thread
    try {
        awaitCancellation()
    } catch (_: Exception) {
        // shutdown
    }
}
