package com.knowme.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// 比系统默认更大的圆角，卡片更柔、更"设计感"
val KnowmeShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),   // Card 默认用 medium，更圆更亲和
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
