package top.colter.mirai.plugin.bilibili

import kotlinx.serialization.Serializable
import top.colter.mirai.plugin.bilibili.onebot.BotInstance
import top.colter.mirai.plugin.bilibili.onebot.ConfigManager
import top.colter.mirai.plugin.bilibili.onebot.OBFriend
import top.colter.mirai.plugin.bilibili.onebot.OBGroup
import java.io.File
import java.time.Instant

object BiliData {
    private lateinit var dataFile: File
    private var data: BiliDataStore = BiliDataStore()

    var dataVersion: Int
        get() = data.dataVersion
        set(value) { data.dataVersion = value }

    val dynamic: MutableMap<Long, SubData> get() = data.dynamic
    val filter: MutableMap<String, MutableMap<Long, DynamicFilter>> get() = data.filter
    val dynamicPushTemplate: MutableMap<String, MutableSet<String>> get() = data.dynamicPushTemplate
    val livePushTemplate: MutableMap<String, MutableSet<String>> get() = data.livePushTemplate
    val liveCloseTemplate: MutableMap<String, MutableSet<String>> get() = data.liveCloseTemplate
    val atAll: MutableMap<String, MutableMap<Long, MutableSet<AtAllType>>> get() = data.atAll
    val group: MutableMap<String, Group> get() = data.group
    val bangumi: MutableMap<Long, Bangumi> get() = data.bangumi

    fun init(dataDir: File) {
        dataFile = dataDir.resolve("BiliData.yml")
    }

    fun reload() {
        data = ConfigManager.loadYaml(dataFile, BiliDataStore())
    }

    fun save() {
        ConfigManager.saveYaml(dataFile, data)
    }
}

@Serializable
data class BiliDataStore(
    var dataVersion: Int = 0,
    val dynamic: MutableMap<Long, SubData> = mutableMapOf(0L to SubData("ALL")),
    val filter: MutableMap<String, MutableMap<Long, DynamicFilter>> = mutableMapOf(),
    val dynamicPushTemplate: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    val livePushTemplate: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    val liveCloseTemplate: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    val atAll: MutableMap<String, MutableMap<Long, MutableSet<AtAllType>>> = mutableMapOf(),
    val group: MutableMap<String, Group> = mutableMapOf(),
    val bangumi: MutableMap<Long, Bangumi> = mutableMapOf(),
)

@Serializable
data class SubData(
    var name: String,
    var color: String? = null,
    var last: Long = Instant.now().epochSecond,
    var lastLive: Long = Instant.now().epochSecond,
    val contacts: MutableSet<String> = mutableSetOf(),
    val banList: MutableMap<String, String> = mutableMapOf(),
)

@Serializable
data class Group(
    val name: String,
    val creator: Long,
    val admin: MutableSet<Long> = mutableSetOf(),
    val contacts: MutableSet<String> = mutableSetOf(),
) {
    override fun toString(): String {
        return buildString {
            appendLine("分组名: $name")
            appendLine("创建者: $creator")
            appendLine()
            appendLine("管理员: ")
            appendLine(admin.joinToString("\n") { it.toString() }.ifEmpty { "暂无管理员" })
            appendLine()
            appendLine("用户: ")
            appendLine(contacts.joinToString("\n").ifEmpty { "暂无用户" })
        }.trimEnd()
    }
}

@Serializable
data class Bangumi(
    val title: String,
    val seasonId: Long,
    val mediaId: Long,
    val type: String,
    var isEnd: Boolean = false,
    var color: String? = null,
    val contacts: MutableSet<String> = mutableSetOf(),
)

@Serializable
enum class FilterType {
    TYPE,
    REGULAR
}

@Serializable
data class DynamicFilter(
    val typeSelect: TypeFilter = TypeFilter(),
    val regularSelect: RegularFilter = RegularFilter(),
)

@Serializable
data class TypeFilter(
    var mode: FilterMode = FilterMode.BLACK_LIST,
    val list: MutableList<DynamicFilterType> = mutableListOf()
)

@Serializable
data class RegularFilter(
    var mode: FilterMode = FilterMode.BLACK_LIST,
    val list: MutableList<String> = mutableListOf()
)

@Serializable
enum class FilterMode(val value: String) {
    WHITE_LIST("白名单"),
    BLACK_LIST("黑名单")
}

@Serializable
enum class DynamicFilterType(val value: String) {
    DYNAMIC("动态"),
    FORWARD("转发动态"),
    VIDEO("视频"),
    MUSIC("音乐"),
    ARTICLE("专栏"),
    LIVE("直播"),
}

enum class AtAllType(val value: String) {
    ALL("全部"),
    DYNAMIC("全部动态"),
    VIDEO("视频"),
    MUSIC("音乐"),
    ARTICLE("专栏"),
    LIVE("直播"),
}
