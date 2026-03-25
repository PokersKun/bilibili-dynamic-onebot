package top.colter.mirai.plugin.bilibili

import io.ktor.client.call.*
import io.ktor.client.request.*
import top.colter.mirai.plugin.bilibili.BiliConfig.accountConfig
import top.colter.mirai.plugin.bilibili.api.createGroup
import top.colter.mirai.plugin.bilibili.api.followGroup
import top.colter.mirai.plugin.bilibili.api.userInfo
import top.colter.mirai.plugin.bilibili.data.EditThisCookie
import top.colter.mirai.plugin.bilibili.data.toCookie
import top.colter.mirai.plugin.bilibili.utils.FontUtils.loadTypeface
import top.colter.mirai.plugin.bilibili.utils.biliClient
import top.colter.mirai.plugin.bilibili.utils.decode
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlin.io.path.*

suspend fun initData() {
    checkCookie()
    initTagid()
    loadFonts()
}

suspend fun checkCookie() {
    val cookieFile = BiliBiliDynamic.dataFolder.resolve("cookies.json")
    if (cookieFile.exists()) {
        try {
            val cookie = cookieFile.readText().decode<List<EditThisCookie>>().toCookie()
            if (!cookie.isEmpty()) {
                BiliBiliDynamic.cookie = cookie
            } else {
                BiliBiliDynamic.logger.error("cookies.json 中缺少必要的值 [SESSDATA] [bili_jct]")
            }
        } catch (e: Exception) {
            BiliBiliDynamic.logger.error("解析 cookies.json 失败")
        }
    }
    if (BiliBiliDynamic.cookie.isEmpty()) BiliBiliDynamic.cookie.parse(accountConfig.cookie)

    try {
        BiliBiliDynamic.uid = biliClient.userInfo()?.mid!!
        BiliBiliDynamic.logger.info("BiliBili UID: ${BiliBiliDynamic.uid}")
    } catch (e: Exception) {
        BiliBiliDynamic.logger.error(e.message)
        BiliBiliDynamic.logger.error("如未登录，请bot管理员在聊天环境内发送 /bili login 进行登录")
        return
    }
}

suspend fun initTagid() {
    if (accountConfig.autoFollow && accountConfig.followGroup.isNotEmpty()) {
        try {
            biliClient.followGroup()?.forEach {
                if (it.name == accountConfig.followGroup) {
                    BiliBiliDynamic.tagid = it.tagId
                    return
                }
            }
            val res = biliClient.createGroup(accountConfig.followGroup) ?: throw Exception()
            BiliBiliDynamic.tagid = res.tagId
        } catch (e: Exception) {
            BiliBiliDynamic.logger.error("初始化分组失败 ${e.message}")
        }
    }
}

suspend fun loadFonts() {
    val fontFolder = BiliBiliDynamic.dataFolder.resolve("font")
    val fontFolderPath = BiliBiliDynamic.dataFolderPath.resolve("font")
    val defaultFont = fontFolder.resolve("HarmonyOS_Sans_SC_Regular.ttf")

    fontFolderPath.apply {
        if (!exists()) createDirectory()
        if (fontFolder.listFiles()?.none { it.isFile } != false || !defaultFont.exists()) {
            try {
                downloadAndExtractZip(
                    "https://developer.huawei.com/images/download/general/HarmonyOS-Sans.zip",
                    fontFolder
                )
                val src = fontFolder.resolve("HarmonyOS Sans")
                    .resolve("HarmonyOS_Sans_SC")
                    .resolve("HarmonyOS_Sans_SC_Regular.ttf")
                if (src.exists()) {
                    src.copyTo(defaultFont, overwrite = true)
                }
                // Clean up extracted directories
                listOf("HarmonyOS Sans", "__MACOSX").forEach { name ->
                    fontFolder.resolve(name).let { dir ->
                        if (dir.exists()) try {
                            dir.walkBottomUp().forEach { it.delete() }
                        } catch (_: Exception) { }
                    }
                }
            } catch (e: Throwable) {
                BiliBiliDynamic.logger.error("下载字体失败! $e")
            }
        }
        forEachDirectoryEntry {
            if (it.toFile().isFile) loadTypeface(it.toString(), it.name.split(".").first())
        }
    }
}

private suspend fun downloadAndExtractZip(url: String, targetDir: File) {
    targetDir.mkdirs()
    val zipBytes = biliClient.useHttpClient { client ->
        client.get(url).body<ByteArray>()
    }
    ZipInputStream(BufferedInputStream(zipBytes.inputStream())).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            val outFile = targetDir.resolve(entry.name)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { fos ->
                    zis.copyTo(fos)
                }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
}
