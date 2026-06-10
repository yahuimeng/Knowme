package com.knowme.app.ui.theme

import androidx.compose.ui.graphics.Color

// 风格：干净克制 —— 中性浅底 + 白卡片，绿色仅作点缀（不再满屏着色，避免视觉吵闹）。
// （变量名沿用，Theme.kt 无需改动）

// ── 浅色 ──
val PaperBg = Color(0xFFF5F6F5)            // 背景：近中性浅（几乎无色，干净）
val PaperSurface = Color(0xFFFFFFFF)       // 卡片：纯白
val PaperSurfaceVariant = Color(0xFFEBEEEC) // 次级面 / chip：浅中性灰
val OnSurfaceVariantWarm = Color(0xFF50534F)
val InkTeal = Color(0xFF2E7D6B)            // 主色：沉静青绿（仅按钮/选中/链接点缀）
val InkTealOn = Color(0xFFFFFFFF)
val TealContainer = Color(0xFFD9EBE5)      // hero / primaryContainer：柔青绿
val OnTealContainer = Color(0xFF0A3B32)
val InkText = Color(0xFF1C1F1E)            // 正文：中性近黑
val WarmOutline = Color(0xFFBABEBB)        // 描边：中性灰
val WarmError = Color(0xFFBA1A1A)
val OnWarmError = Color(0xFFFFFFFF)
val WarmErrorContainer = Color(0xFFFFDAD6)
val OnWarmErrorContainer = Color(0xFF410002)

// ── 深色 ──
val PaperBgDark = Color(0xFF121413)
val PaperSurfaceDark = Color(0xFF1C1F1E)
val PaperSurfaceVariantDark = Color(0xFF2A2E2C)
val OnSurfaceVariantWarmDark = Color(0xFFC4C8C4)
val InkTealDark = Color(0xFF7CD0BD)
val OnInkTealDark = Color(0xFF06372E)
val TealContainerDark = Color(0xFF1E4A41)
val OnTealContainerDark = Color(0xFFC6EBE0)
val InkTextDark = Color(0xFFE3E3E0)
val WarmOutlineDark = Color(0xFF8B8F8C)
val WarmErrorDark = Color(0xFFFFB4AB)
val OnWarmErrorDark = Color(0xFF690005)
val WarmErrorContainerDark = Color(0xFF93000A)
val OnWarmErrorContainerDark = Color(0xFFFFDAD6)

// ── 三档优先级语义色（克制）──
val PriorityHigh = Color(0xFFC1453A)   // 🔴 需要你处理
val PriorityMid = Color(0xFFC2862A)    // 🟡 知道就行
val PriorityLow = Color(0xFF9AA09C)    // ⚪️ 噪音
