package com.knowme.app.ui.theme

import androidx.compose.ui.graphics.Color

// 风格：柔和清新 —— 浅薄荷微染背景 + 白卡片 + 低饱和马卡龙绿，轻松亲和。
// （变量名沿用，Theme.kt 无需改动；语义见注释）

// ── 浅色 ──
val PaperBg = Color(0xFFEDF5F0)            // 背景：薄荷微染白（明显带绿，区别于纯白）
val PaperSurface = Color(0xFFFFFFFF)       // 卡片：纯白，在薄荷底上更跳、更清新
val PaperSurfaceVariant = Color(0xFFDDECE4) // 次级面 / chip：浅薄荷
val OnSurfaceVariantWarm = Color(0xFF44524B)
val InkTeal = Color(0xFF2E9E74)            // 主色：低饱和马卡龙绿（友好、明亮）
val InkTealOn = Color(0xFFFFFFFF)
val TealContainer = Color(0xFFCBEBD8)      // hero / primaryContainer：奶绿
val OnTealContainer = Color(0xFF0E432E)
val InkText = Color(0xFF1F2B25)            // 正文：深绿灰（非纯黑，更柔）
val WarmOutline = Color(0xFFA1B3A9)        // 描边：绿灰
val WarmError = Color(0xFFC24B3C)
val OnWarmError = Color(0xFFFFFFFF)
val WarmErrorContainer = Color(0xFFFAE0DB)
val OnWarmErrorContainer = Color(0xFF5A1812)

// ── 深色 ──
val PaperBgDark = Color(0xFF121815)        // 深绿黑
val PaperSurfaceDark = Color(0xFF1A211D)
val PaperSurfaceVariantDark = Color(0xFF283029)
val OnSurfaceVariantWarmDark = Color(0xFFC2D0C8)
val InkTealDark = Color(0xFF84D6AE)        // 深色下更亮的薄荷绿
val OnInkTealDark = Color(0xFF06311F)
val TealContainerDark = Color(0xFF1E4A38)
val OnTealContainerDark = Color(0xFFC7EED9)
val InkTextDark = Color(0xFFE6EFE9)        // 浅绿白正文
val WarmOutlineDark = Color(0xFF8FA399)
val WarmErrorDark = Color(0xFFE6A097)
val OnWarmErrorDark = Color(0xFF44150F)
val WarmErrorContainerDark = Color(0xFF5C2A24)
val OnWarmErrorContainerDark = Color(0xFFF6DDD7)

// ── 三档优先级语义色 ──
val PriorityHigh = Color(0xFFCF5043)   // 🔴 需要你处理
val PriorityMid = Color(0xFFD49A3C)    // 🟡 知道就行
val PriorityLow = Color(0xFF97A89E)    // ⚪️ 噪音（绿灰，融入清新底）
