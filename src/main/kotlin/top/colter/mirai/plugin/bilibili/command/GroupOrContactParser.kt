package top.colter.mirai.plugin.bilibili.command

import top.colter.mirai.plugin.bilibili.BiliData
import top.colter.mirai.plugin.bilibili.onebot.OBContact
import top.colter.mirai.plugin.bilibili.utils.findContact
import top.colter.mirai.plugin.bilibili.utils.findContactAll

/**
 * Parse a string argument into a GroupOrContact.
 * If it matches a BiliData group name, returns that group.
 * Otherwise tries to find a contact by actual ID (group or friend),
 * then falls back to delegate string lookup.
 */
fun parseGroupOrContact(raw: String, defaultContact: OBContact): GroupOrContact {
    val group = BiliData.group[raw]
    return if (group != null) {
        GroupOrContact(group = group)
    } else {
        // First try as actual ID (matches both groups and friends)
        val contact = findContactAll(raw) ?: findContact(raw) ?: defaultContact
        GroupOrContact(contact = contact)
    }
}
