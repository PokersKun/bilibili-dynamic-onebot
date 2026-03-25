package top.colter.mirai.plugin.bilibili.data

import kotlinx.serialization.Serializable
import org.jetbrains.skia.Color
import top.colter.mirai.plugin.bilibili.draw.makeRGB
import top.colter.mirai.plugin.bilibili.onebot.ConfigManager
import java.io.File


object BiliImageTheme {
    private lateinit var configFile: File
    private var data: ImageThemeData = ImageThemeData()

    val customOverload: Boolean get() = data.customOverload
    val customTheme: Theme get() = data.customTheme
    val theme: Map<String, Theme> get() = THEMES

    fun init(configDir: File) {
        configFile = configDir.resolve("ImageTheme.yml")
    }

    fun reload() {
        data = ConfigManager.loadYaml(configFile, ImageThemeData())
    }
}

@Serializable
data class ImageThemeData(
    val customOverload: Boolean = false,
    val customTheme: Theme = THEMES["v3"]!!,
)

val THEMES: Map<String, Theme> = mapOf(
    "v3" to Theme(
        "#B4FFFFFF", "#FFFFFF", "#A0FFFFFF", "#FFFFFF", "#FB7299",
        "#313131", "#9C9C9C", "#666666", "#222222", "#178BCF", "#9C9C9C",
        Theme.Shadow("#46000000", 6f, 6f, 25f, 0f),
        Theme.Shadow("#1E000000", 5f, 5f, 15f, 0f),
        Theme.BadgeColor("#00CBFF", "#B4FFFFFF"),
        Theme.BadgeColor("#FFFFFF", "#48C7F0"),
        Theme.BadgeColor("#FFFFFF", "#FB7299"),
        Theme.BadgeColor("#FFFFFF", "#48C7F0"),
    ),
    "v3RainbowOutline" to Theme(
        "#B4FFFFFF",
        "#ff0000;#ff00ff;#0000ff;#00ffff;#00ff00;#ffff00;#ff0000",
        "#A0FFFFFF", "#FFFFFF", "#FB7299",
        "#313131", "#9C9C9C", "#666666", "#222222", "#178BCF", "#9C9C9C",
        Theme.Shadow("#46000000", 6f, 6f, 25f, 0f),
        Theme.Shadow("#1E000000", 5f, 5f, 15f, 0f),
        Theme.BadgeColor("#00CBFF", "#B4FFFFFF"),
        Theme.BadgeColor("#FFFFFF", "#48C7F0"),
        Theme.BadgeColor("#FFFFFF", "#FB7299"),
        Theme.BadgeColor("#FFFFFF", "#48C7F0"),
    ),
    "v2" to Theme(
        "#C8FFFFFF", "#FFFFFF", "#A0FFFFFF", "#FFFFFF", "#FB7299",
        "#313131", "#9C9C9C", "#666666", "#222222", "#178BCF", "#9C9C9C",
        Theme.Shadow("#00000000", 0f, 0f, 0f, 0f),
        Theme.Shadow("#00000000", 0f, 0f, 0f, 0f),
        Theme.BadgeColor("#00CBFF", "#C8FFFFFF"),
        Theme.BadgeColor("#FFFFFF", "#48C7F0"),
        Theme.BadgeColor("#FFFFFF", "#FB7299"),
        Theme.BadgeColor("#FFFFFF", "#48C7F0"),
    )
)

@Serializable
data class Theme(
    val cardBgColorHex: String,
    val cardOutlineColorHex: String,
    val faceOutlineColorHex: String,
    val drawOutlineColorHex: String,
    val nameColorHex: String,
    val titleColorHex: String,
    val subTitleColorHex: String,
    val descColorHex: String,
    val contentColorHex: String,
    val linkColorHex: String,
    val footerColorHex: String,
    val cardShadow: Shadow,
    val smallCardShadow: Shadow,
    val mainLeftBadge: BadgeColor,
    val mainRightBadge: BadgeColor,
    val subLeftBadge: BadgeColor,
    val subRightBadge: BadgeColor,
) {
    @Serializable
    data class Shadow(
        val shadowColorHex: String,
        val offsetX: Float,
        val offsetY: Float,
        val blur: Float,
        val spread: Float = 0f,
    ) {
        val shadowColor: Int get() = Color.makeRGB(shadowColorHex)
    }

    @Serializable
    data class BadgeColor(
        val fontColorHex: String,
        val bgColorHex: String,
    ) {
        val fontColor: Int get() = Color.makeRGB(fontColorHex)
        val bgColor: Int get() = Color.makeRGB(bgColorHex)
    }

    val cardBgColor: Int get() = Color.makeRGB(cardBgColorHex)
    val cardOutlineColors: IntArray get() = cardOutlineColorHex.split(";").map { Color.makeRGB(it) }.toIntArray()
    val faceOutlineColor: Int get() = Color.makeRGB(faceOutlineColorHex)
    val drawOutlineColor: Int get() = Color.makeRGB(drawOutlineColorHex)
    val nameColor: Int get() = Color.makeRGB(nameColorHex)
    val titleColor: Int get() = Color.makeRGB(titleColorHex)
    val subTitleColor: Int get() = Color.makeRGB(subTitleColorHex)
    val descColor: Int get() = Color.makeRGB(descColorHex)
    val contentColor: Int get() = Color.makeRGB(contentColorHex)
    val linkColor: Int get() = Color.makeRGB(linkColorHex)
    val footerColor: Int get() = Color.makeRGB(footerColorHex)
}
