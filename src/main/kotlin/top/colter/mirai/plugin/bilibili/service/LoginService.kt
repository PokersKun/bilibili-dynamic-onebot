package top.colter.mirai.plugin.bilibili.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import top.colter.mirai.plugin.bilibili.BiliBiliDynamic
import top.colter.mirai.plugin.bilibili.BiliConfig
import top.colter.mirai.plugin.bilibili.api.getLoginQrcode
import top.colter.mirai.plugin.bilibili.api.loginInfo
import top.colter.mirai.plugin.bilibili.draw.loginQrCode
import top.colter.mirai.plugin.bilibili.initTagid
import top.colter.mirai.plugin.bilibili.onebot.BotInstance
import top.colter.mirai.plugin.bilibili.onebot.MessageSegment
import top.colter.mirai.plugin.bilibili.onebot.OBContact
import java.net.URI
import java.util.Base64

object LoginService {
    suspend fun login(contact: OBContact) {
        val loginData = client.getLoginQrcode()!!

        val image = loginQrCode(loginData.url)
        val imgBytes = image.encodeToData()!!.bytes
        val b64 = Base64.getEncoder().encodeToString(imgBytes)
        val qrMsgId = contact.sendMessage(listOf(MessageSegment.image("base64://$b64")))
        val loginMsgId = contact.sendMessage("请使用BiliBili手机APP扫码登录 3分钟有效")
        runCatching {
            withTimeout(180000) {
                while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                    delay(3000)
                    val loginInfo = client.loginInfo(loginData.qrcodeKey!!)!!
                    if (loginInfo.code == 0) {
                        val querys = URI(loginInfo.url!!).query.split("&")
                        val cookie = buildString {
                            querys.forEach {
                                if (it.contains("SESSDATA") || it.contains("bili_jct"))
                                    append("${it.replace(",", "%2C").replace("*", "%2A")}; ")
                            }
                        }
                        BiliConfig.accountConfig.cookie = cookie
                        BiliConfig.save()
                        BiliBiliDynamic.cookie.parse(cookie)
                        initTagid()
                        contact.sendMessage("登录成功!")
                        break
                    }
                }
            }
        }.onFailure {
            contact.sendMessage("登录失败 ${it.message}")
        }
        try {
            BotInstance.client.deleteMsg(qrMsgId)
            BotInstance.client.deleteMsg(loginMsgId)
        } catch (_: Throwable) {}
    }
}
