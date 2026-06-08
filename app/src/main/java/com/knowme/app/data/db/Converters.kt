package com.knowme.app.data.db

import androidx.room.TypeConverter

/** Room 不内建枚举转换，这里把 Priority 存成字符串。 */
class Converters {
    @TypeConverter
    fun fromPriority(p: Priority): String = p.name

    @TypeConverter
    fun toPriority(value: String): Priority =
        runCatching { Priority.valueOf(value) }.getOrDefault(Priority.UNKNOWN)
}
