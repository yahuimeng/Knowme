package com.knowme.app.digest

import com.knowme.app.data.db.NotificationEntity
import com.knowme.app.data.db.Priority

/**
 * 本地规则预分类：按"消息来源在生活里的重要性"排。
 *  ① 真人通讯（微信/QQ/短信/钉钉/飞书/WhatsApp 等，或系统标记 category=msg）= 最高，
 *     永不本地折叠为噪音，交给 AI 优先判要紧；
 *  ② 明显营销/促销 = 噪音，本地直接 LOW，省 token；
 *  ③ 其余交给 AI。
 * 仍保守：本地只下判 LOW；要紧/留意由 AI + 兜底（见 DigestGenerator）决定。
 */
object NotificationClassifier {

    /** 真人通讯软件包名（生活里最重要的消息来源）。 */
    private val commPackages = setOf(
        "com.tencent.mm",                     // 微信
        "com.tencent.mobileqq", "com.tencent.tim", // QQ / TIM
        "com.tencent.wework",                 // 企业微信
        "com.alibaba.android.rimet",          // 钉钉
        "com.ss.android.lark",                // 飞书
        "com.whatsapp", "com.whatsapp.w4b",   // WhatsApp
        "org.telegram.messenger",             // Telegram
        "com.facebook.orca",                  // Messenger
        "jp.naver.line.android",              // Line
        "com.google.android.apps.messaging",  // Google Messages
        "com.android.mms", "com.android.messaging", "com.samsung.android.messaging", // 短信
        "com.skype.raider", "com.viber.voip", "com.discord", "org.thoughtcrime.securesms",
    )

    /** 是否真人通讯来源：包名命中，或系统把它标为消息类（category=msg）。 */
    fun isComms(n: NotificationEntity): Boolean =
        n.category == "msg" || n.packageName in commPackages

    /** 群聊里被人 @我/@所有人 —— 生活里最该立刻看的消息，一律最高优先级。 */
    private val mentionMarkers = listOf(
        "有人@我", "@我", "@你", "@所有人", "@全体成员",
        "提到了你", "提及了你", "提到了我", "mentioned you", "@all", "@everyone",
    )

    fun isMention(n: NotificationEntity): Boolean {
        val t = "${n.title} ${n.text}"
        return mentionMarkers.any { t.contains(it, ignoreCase = true) }
    }

    // 命中即判为噪音（促销 / 营销 / 泛社交打扰）
    private val noiseKeywords = listOf(
        "砍一刀", "降价", "优惠券", "领取", "促销", "秒杀", "限时", "拼团", "清仓",
        "满减", "会员日", "上新", "种草", "新人专享", "幸运", "抽奖", "签到", "金币", "积分",
        "解锁", "升级到", "免费试用", "开通会员", "为你推荐", "猜你喜欢", "附近的人",
        "赞了你", "评论了你", "关注了你", "直播开始", "回放", "立即购买", "下单", "好物",
        "特价", "福利", "补贴", "返现", "助力", "瓜分", "红包雨", "免单",
    )

    /** 返回本地判定的优先级；拿不准返回 null（交给 AI）。 */
    fun preClassify(n: NotificationEntity): Priority? {
        // 真人通讯：永不本地折叠，留给 AI 优先判要紧
        if (isComms(n)) return null
        val text = "${n.title} ${n.text}"
        if (noiseKeywords.any { text.contains(it) }) return Priority.LOW
        return null
    }
}
