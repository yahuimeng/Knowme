package com.knowme.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── 暖纸感 · 浅色 ──
val PaperBg = Color(0xFFFAF6EF)            // 米白背景（暖，不冷灰）
val PaperSurface = Color(0xFFFFFDF8)       // 卡片：近白暖
val PaperSurfaceVariant = Color(0xFFEDE7DA) // 次级面 / chip：米色
val OnSurfaceVariantWarm = Color(0xFF5C5648)
val InkTeal = Color(0xFF15605B)            // 主色：墨绿
val InkTealOn = Color(0xFFFBFDFB)
val TealContainer = Color(0xFFD3E7E2)      // hero / primaryContainer：柔绿
val OnTealContainer = Color(0xFF08332F)
val InkText = Color(0xFF2A2620)            // 正文：暖近黑
val WarmOutline = Color(0xFF9C9486)        // 描边：暖灰
val WarmError = Color(0xFFB23A2E)
val OnWarmError = Color(0xFFFFFFFF)
val WarmErrorContainer = Color(0xFFF6DDD7)
val OnWarmErrorContainer = Color(0xFF5C1812)

// ── 暖纸感 · 深色 ──
val PaperBgDark = Color(0xFF17140F)        // 暖近黑（非蓝灰）
val PaperSurfaceDark = Color(0xFF221E18)
val PaperSurfaceVariantDark = Color(0xFF2D2920)
val OnSurfaceVariantWarmDark = Color(0xFFCFC8B9)
val InkTealDark = Color(0xFF77BBB3)        // 深色下的主色更亮
val OnInkTealDark = Color(0xFF06302C)
val TealContainerDark = Color(0xFF1E4A45)
val OnTealContainerDark = Color(0xFFCFE9E3)
val InkTextDark = Color(0xFFECE5D9)        // 暖白正文
val WarmOutlineDark = Color(0xFF8E8675)
val WarmErrorDark = Color(0xFFE6A097)
val OnWarmErrorDark = Color(0xFF44150F)
val WarmErrorContainerDark = Color(0xFF5C2A24)
val OnWarmErrorContainerDark = Color(0xFFF6DDD7)

// ── 三档优先级语义色（克制，适配暖底）──
val PriorityHigh = Color(0xFFC1453A)   // 🔴 需要你处理
val PriorityMid = Color(0xFFC2862A)    // 🟡 知道就行
val PriorityLow = Color(0xFF9A9182)    // ⚪️ 噪音
