package com.knowme.app.ui.theme

import androidx.compose.ui.graphics.Color

// 风格参考 Zepp/Amazfit：浅灰画布 + 纯白卡片 + 黑字为主、颜色极少。
// 灰底让白卡浮起来 = 清爽舒服。（变量名沿用，Theme.kt 无需改动）

// ── 浅色 ──
val PaperBg = Color(0xFFECEEF0)            // 画布：浅冷灰（关键：不是白）
val PaperSurface = Color(0xFFFFFFFF)       // 卡片：纯白（浮在灰底上）
val PaperSurfaceVariant = Color(0xFFE3E6EA) // 次级填充 / chip：浅灰
val OnSurfaceVariantWarm = Color(0xFF585C61) // 次要文字：中灰（对比足够）
val InkTeal = Color(0xFF12A07A)            // 强调色：清新绿（按钮/开关/选中统一用它）
val InkTealOn = Color(0xFFFFFFFF)
val TealContainer = Color(0xFFD3E7E2)
val OnTealContainer = Color(0xFF08332F)
val InkText = Color(0xFF1B1C1E)            // 主文字：近黑（标题加粗有力）
val WarmOutline = Color(0xFFC6CACE)        // 描边：浅灰
val WarmError = Color(0xFFBA1A1A)
val OnWarmError = Color(0xFFFFFFFF)
val WarmErrorContainer = Color(0xFFFFDAD6)
val OnWarmErrorContainer = Color(0xFF410002)

// ── 深色 ──
val PaperBgDark = Color(0xFF131517)        // 画布：深灰黑
val PaperSurfaceDark = Color(0xFF1E2123)   // 卡片：比画布略亮（浮起）
val PaperSurfaceVariantDark = Color(0xFF282C30)
val OnSurfaceVariantWarmDark = Color(0xFFB2B7BB)
val InkTealDark = Color(0xFF7FD0C2)
val OnInkTealDark = Color(0xFF06322B)
val TealContainerDark = Color(0xFF1E4A44)
val OnTealContainerDark = Color(0xFFC7EBE3)
val InkTextDark = Color(0xFFE4E5E7)
val WarmOutlineDark = Color(0xFF44484C)
val WarmErrorDark = Color(0xFFFFB4AB)
val OnWarmErrorDark = Color(0xFF690005)
val WarmErrorContainerDark = Color(0xFF93000A)
val OnWarmErrorContainerDark = Color(0xFFFFDAD6)

// ── 三档优先级语义色（克制小点缀）──
val PriorityHigh = Color(0xFFC1453A)   // 🔴 需要你处理
val PriorityMid = Color(0xFFC2862A)    // 🟡 知道就行
val PriorityLow = Color(0xFF9AA09C)    // ⚪️ 噪音
