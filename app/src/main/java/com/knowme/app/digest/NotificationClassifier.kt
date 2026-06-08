package com.knowme.app.digest

import com.knowme.app.data.db.NotificationEntity
import com.knowme.app.data.db.Priority

/**
 * 本地规则预分类：只对"高置信度的明显噪音"判 LOW，其余交给 AI。
 * 目的：① 省 token（噪音不送 AI）② 更准（确定性规则胜过模型猜测）。
 * 保守起见，绝不在本地把通知判成 HIGH/MID——以免误把重要消息藏掉。
 */
object NotificationClassifier {

    // 命中即判为噪音（促销 / 营销 / 泛社交打扰）
    private val noiseKeywords = listOf(
        "砍一刀", "降价", "优惠券", "红包", "领取", "促销", "秒杀", "限时", "拼团", "清仓",
        "满减", "会员日", "上新", "种草", "新人专享", "幸运", "抽奖", "签到", "金币", "积分",
        "解锁", "升级到", "免费试用", "开通会员", "为你推荐", "猜你喜欢", "附近的人",
        "赞了你", "评论了你", "关注了你", "直播开始", "回放", "立即购买", "下单", "好物",
        "特价", "福利", "补贴", "返现", "助力", "瓜分", "红包雨", "免单",
    )

    /** 返回本地判定的优先级；拿不准返回 null（交给 AI）。 */
    fun preClassify(n: NotificationEntity): Priority? {
        val text = "${n.title} ${n.text}"
        if (noiseKeywords.any { text.contains(it) }) return Priority.LOW
        return null
    }
}
