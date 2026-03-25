package top.colter.mirai.plugin.bilibili.onebot

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.slf4j.LoggerFactory
import java.io.File

object ConfigManager {
    private val logger = LoggerFactory.getLogger("Conf")

    @PublishedApi
    internal val yaml = Yaml(configuration = YamlConfiguration(
        strictMode = false,
        encodeDefaults = true
    ))

    inline fun <reified T> loadYaml(file: File, default: T): T =
        loadYamlInternal(file, default, serializer())

    inline fun <reified T> saveYaml(file: File, data: T) =
        saveYamlInternal(file, data, serializer())

    @PublishedApi
    internal fun <T> loadYamlInternal(file: File, default: T, serializer: KSerializer<T>): T {
        return try {
            if (file.exists()) {
                val text = file.readText()
                if (text.isBlank()) {
                    saveYamlInternal(file, default, serializer)
                    default
                } else {
                    yaml.decodeFromString(serializer, text)
                }
            } else {
                file.parentFile?.mkdirs()
                saveYamlInternal(file, default, serializer)
                default
            }
        } catch (e: Exception) {
            logger.error("加载配置文件 ${file.name} 失败: ${e.message}")
            logger.info("使用默认配置")
            try {
                // Backup broken file
                if (file.exists()) {
                    file.copyTo(File(file.path + ".bak"), overwrite = true)
                }
                saveYamlInternal(file, default, serializer)
            } catch (_: Exception) {}
            default
        }
    }

    @PublishedApi
    internal fun <T> saveYamlInternal(file: File, data: T, serializer: KSerializer<T>) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(yaml.encodeToString(serializer, data))
        } catch (e: Exception) {
            logger.error("保存配置文件 ${file.name} 失败: ${e.message}")
        }
    }
}
