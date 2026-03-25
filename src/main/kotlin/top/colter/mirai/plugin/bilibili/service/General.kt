package top.colter.mirai.plugin.bilibili.service

import top.colter.mirai.plugin.bilibili.BiliBiliDynamic
import top.colter.mirai.plugin.bilibili.BiliData
import top.colter.mirai.plugin.bilibili.client.BiliClient

internal val logger by BiliBiliDynamic::logger

val client = BiliClient()

val dynamic by BiliData::dynamic
val filter by BiliData::filter
val group by BiliData::group
val atAll by BiliData::atAll
val bangumi by BiliData::bangumi


fun isFollow(uid: Long, subject: String) =
    uid == 0L || (dynamic.containsKey(uid) && dynamic[uid]!!.contacts.contains(subject))
