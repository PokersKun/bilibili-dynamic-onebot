package top.colter.mirai.plugin.bilibili

import kotlinx.serialization.Serializable
import org.jetbrains.skia.paragraph.Alignment
import top.colter.mirai.plugin.bilibili.onebot.ConfigManager
import top.colter.mirai.plugin.bilibili.service.TriggerMode
import top.colter.mirai.plugin.bilibili.utils.CacheType
import java.io.File

object BiliConfig {
    private lateinit var configFile: File
    private var data: BiliConfigData = BiliConfigData()

    val enableConfig: EnableConfig get() = data.enableConfig
    val accountConfig: BiliAccountConfig get() = data.accountConfig
    val checkConfig: CheckConfig get() = data.checkConfig
    val pushConfig: PushConfig get() = data.pushConfig
    val imageConfig: ImageConfig get() = data.imageConfig
    val templateConfig: TemplateConfig get() = data.templateConfig
    val cacheConfig: CacheConfig get() = data.cacheConfig
    val proxyConfig: ProxyConfig get() = data.proxyConfig
    val translateConfig: TranslateConfig get() = data.translateConfig
    val linkResolveConfig: LinkResolveConfig get() = data.linkResolveConfig

    fun init(configDir: File) {
        configFile = configDir.resolve("BiliConfig.yml")
    }

    fun reload() {
        data = ConfigManager.loadYaml(configFile, BiliConfigData())
    }

    fun save() {
        ConfigManager.saveYaml(configFile, data)
    }
}

@Serializable
data class BiliConfigData(
    val enableConfig: EnableConfig = EnableConfig(),
    val accountConfig: BiliAccountConfig = BiliAccountConfig(),
    val checkConfig: CheckConfig = CheckConfig(),
    val pushConfig: PushConfig = PushConfig(),
    val imageConfig: ImageConfig = ImageConfig(),
    val templateConfig: TemplateConfig = TemplateConfig(),
    val cacheConfig: CacheConfig = CacheConfig(),
    val proxyConfig: ProxyConfig = ProxyConfig(),
    val translateConfig: TranslateConfig = TranslateConfig(),
    val linkResolveConfig: LinkResolveConfig = LinkResolveConfig(),
)

@Serializable
data class EnableConfig(
    val drawEnable: Boolean = true,
    val notifyEnable: Boolean = true,
    val liveCloseNotifyEnable: Boolean = true,
    val lowSpeedEnable: Boolean = true,
    var translateEnable: Boolean = false,
    val proxyEnable: Boolean = false,
    val cacheClearEnable: Boolean = true,
    val showLoadingMessage: Boolean = true,
)

@Serializable
data class TranslateConfig(
    val cutLine: String = "\n\n〓〓〓 翻译 〓〓〓\n",
    var baidu: BaiduTranslateConfig = BaiduTranslateConfig()
) {
    @Serializable
    data class BaiduTranslateConfig(
        var APP_ID: String = "",
        var SECURITY_KEY: String = "",
    )
}

@Serializable
data class ImageConfig(
    val quality: String = "1000w",
    val theme: String = "v3",
    var font: String = "",
    var defaultColor: String = "#d3edfa",
    var cardOrnament: String = "FanCard",
    val colorGenerator: ColorGenerator = ColorGenerator(),
    val badgeEnable: BadgeEnable = BadgeEnable(),
) {
    @Serializable
    data class ColorGenerator(
        val hueStep: Int = 30,
        val lockSB: Boolean = true,
        val saturation: Float = 0.25f,
        val brightness: Float = 1f,
    )

    @Serializable
    data class BadgeEnable(
        var left: Boolean = true,
        var right: Boolean = false,
    ) {
        val enable: Boolean get() = left || right
    }
}

@Serializable
data class ProxyConfig(
    val proxy: List<String> = listOf(),
)

@Serializable
data class BiliAccountConfig(
    var cookie: String = "",
    var autoFollow: Boolean = true,
    var followGroup: String = "Bot关注"
)

@Serializable
data class CheckConfig(
    var interval: Int = 15,
    var liveInterval: Int = 15,
    var lowSpeed: String = "0-0x2",
    val checkReportInterval: Int = 10,
    val timeout: Int = 10
)

@Serializable
data class PushConfig(
    val messageInterval: Long = 100,
    val pushInterval: Long = 500,
    val atAllPlus: String = "PLUS_END",
    val toShortLink: Boolean = false,
)

@Serializable
data class TemplateConfig(
    var defaultDynamicPush: String = "OneMsg",
    var defaultLivePush: String = "OneMsg",
    var defaultLiveClose: String = "SimpleMsg",
    val dynamicPush: MutableMap<String, String> = mutableMapOf(
        "DrawOnly" to "{draw}",
        "TextOnly" to "{name}@{type}\n{link}\n{content}\n{images}",
        "OneMsg" to "{draw}\n{name}@{type}\n{link}",
        "TwoMsg" to "{draw}\r{name}@{uid}@{type}\n{time}\n{link}",
        "ForwardMsg" to "{draw}{>>}作者：{name}\nUID：{uid}\n时间：{time}\n类型：{type}\n链接：{links}\r{content}\r{images}{<<}",
    ),
    val livePush: MutableMap<String, String> = mutableMapOf(
        "DrawOnly" to "{draw}",
        "TextOnly" to "{name}@直播\n{link}\n标题: {title}",
        "OneMsg" to "{draw}\n{name}@直播\n{link}",
        "TwoMsg" to "{draw}\r{name}@{uid}@直播\n{title}\n{time}\n{link}",
    ),
    val liveClose: MutableMap<String, String> = mutableMapOf(
        "SimpleMsg" to "{name} 直播结束啦!\n直播时长: {duration}",
        "ComplexMsg" to "{name} 直播结束啦!\n标题: {title}\n直播时长: {duration}"
    ),
    val forwardCard: ForwardDisplay = ForwardDisplay(),
    var footer: FooterConfig = FooterConfig(),
)

@Serializable
data class FooterConfig(
    var dynamicFooter: String = "",
    var liveFooter: String = "",
    var footerAlign: Alignment = Alignment.LEFT
)

@Serializable
data class ForwardDisplay(
    val title: String = "{name} {type} 详情",
    val preview: String = "时间: {time}\n{content}",
    val summary: String = "ID: {did}",
    val brief: String = "[{name} {type}]"
)

@Serializable
data class CacheConfig(
    val downloadOriginal: Boolean = true,
    val expires: Map<CacheType, Int> = mapOf(
        CacheType.DRAW to 7,
        CacheType.IMAGES to 7,
        CacheType.EMOJI to 7,
        CacheType.USER to 7,
        CacheType.OTHER to 7,
    )
)

@Serializable
data class LinkResolveConfig(
    val triggerMode: TriggerMode = TriggerMode.At,
    val returnLink: Boolean = false,
    val regex: List<String> = listOf(
        """(www\.bilibili\.com/video/((BV[0-9A-z]{10})|(av\d{1,20})))|^(BV[0-9A-z]{10})|^(av\d{1,20})""",
        """(www\.bilibili\.com/read/cv\d{1,10})|^(cv\d{1,10})|(www\.bilibili\.com/read/mobile/\d{1,10})""",
        """((www|m)\.bilibili\.com/bangumi/(play|media)/(ss|ep|md)\d+)|^((ss|ep|md)\d+)""",
        """([tm]\.bilibili\.com/(dynamic/)?\d+)|(www\.bilibili\.com/opus/\d+)""",
        """live\.bilibili\.com/(h5/)?\d+""",
        """space\.bilibili\.com/\d+""",
        """(b23\.tv|bili2233\.cn)\\?/[0-9A-z]+""",
    )
) {
    val reg: List<Regex> get() = regex.map { it.toRegex() }
}

@Serializable
data class OneBotConfig(
    val wsUrl: String = "ws://127.0.0.1:3001",
    val adminIds: List<Long> = listOf(123456789L),
    val token: String = "",
    val reconnectInterval: Long = 5000,
    val reconnectMaxRetries: Int = -1,
) {
    companion object {
        private lateinit var configFile: File
        private var data: OneBotConfig = OneBotConfig()

        val wsUrl: String get() = data.wsUrl
        val adminIds: List<Long> get() = data.adminIds
        val token: String get() = data.token
        val reconnectInterval: Long get() = data.reconnectInterval
        val reconnectMaxRetries: Int get() = data.reconnectMaxRetries

        fun isAdmin(userId: Long): Boolean = adminIds.contains(userId)

        fun init(configDir: File) {
            configFile = configDir.resolve("OneBotConfig.yml")
        }

        fun reload() {
            data = ConfigManager.loadYaml(configFile, OneBotConfig())
        }
    }
}
