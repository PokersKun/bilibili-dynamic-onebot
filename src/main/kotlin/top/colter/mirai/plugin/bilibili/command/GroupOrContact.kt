package top.colter.mirai.plugin.bilibili.command

import top.colter.mirai.plugin.bilibili.Group
import top.colter.mirai.plugin.bilibili.onebot.OBContact
import top.colter.mirai.plugin.bilibili.onebot.delegate

data class GroupOrContact(
    val contact: OBContact? = null,
    val group: Group? = null,
)

val GroupOrContact.isGroup: Boolean
    get() = group != null

val GroupOrContact.subject: String
    get() = group?.name ?: contact!!.delegate

val GroupOrContact.name: String
    get() = group?.name ?: contact!!.contactName
