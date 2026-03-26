package top.colter.mirai.plugin.bilibili

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import top.colter.mirai.plugin.bilibili.data.*
import top.colter.mirai.plugin.bilibili.tasker.*
import java.io.File
import java.nio.file.Path

object BiliBiliDynamic : CoroutineScope {
    val logger = LoggerFactory.getLogger("Bili")!!

    private val exceptionHandler = CoroutineExceptionHandler { ctx, throwable ->
        val name = ctx[CoroutineName]?.name ?: "unknown"
        logger.error("$name: ${throwable.message}", throwable)
    }
    private val supervisorJob = SupervisorJob()
    override val coroutineContext = supervisorJob + Dispatchers.Default + CoroutineName("BiliBiliDynamic") + exceptionHandler

    var uid: Long = 0L
    var tagid: Int = 0

    var cookie = BiliCookie()

    val dynamicChannel = Channel<DynamicDetail>(20)
    val liveChannel = Channel<LiveDetail>(20)
    val messageChannel = Channel<BiliMessage>(20)
    val missChannel = Channel<BiliMessage>(10)

    val liveUsers = mutableMapOf<Long, Long>()

    lateinit var dataFolder: File
    lateinit var dataFolderPath: Path
    lateinit var configFolder: File

    fun init(dataDir: File, configDir: File) {
        dataFolder = dataDir
        dataFolderPath = dataDir.toPath()
        configFolder = configDir
        dataDir.mkdirs()
        configDir.mkdirs()
    }

    fun getResourceAsStream(path: String) =
        BiliBiliDynamic::class.java.getResourceAsStream("/$path")
            ?: BiliBiliDynamic::class.java.classLoader.getResourceAsStream(path)

    fun onEnable() {
        logger.info("BiliBili Dynamic loaded (OneBot 11)")

        BiliConfig.init(configFolder)
        BiliData.init(dataFolder)
        BiliImageTheme.init(configFolder)
        BiliImageQuality.init(configFolder)

        BiliData.reload()
        BiliConfig.reload()
        BiliImageTheme.reload()
        BiliImageQuality.reload()

        OneBotConfig.reload()
    }

    fun onStartup() {
        DynamicCheckTasker.start()
        LiveCheckTasker.start()
        DynamicMessageTasker.start()
        LiveMessageTasker.start()
        SendTasker.start()
        ListenerTasker.start()
        if (BiliConfig.enableConfig.liveCloseNotifyEnable) LiveCloseCheckTasker.start()
        if (BiliConfig.enableConfig.cacheClearEnable) CacheClearTasker.start()
    }

    fun onDisable() {
        dynamicChannel.close()
        messageChannel.close()
        BiliTasker.cancelAll()
        BiliData.save()
        BiliConfig.save()
    }

    fun shutdown() {
        onDisable()
        supervisorJob.cancel()
    }
}
