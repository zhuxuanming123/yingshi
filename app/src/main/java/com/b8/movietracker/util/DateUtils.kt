package com.b8.movietracker.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 日期工具类。
 *
 * 巧用「yyyy-MM-dd 格式的字典序 == 时间序」这一特性：
 *   "2026-10-25" > "2026-10-23" 字符串比较结果为 true，等价于 25 号在 23 号之后。
 * 因此判断"是否未来日期"无需解析成 Date 对象，直接字符串比较即可（简洁、无时区坑）。
 */
object DateUtils {

    private val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** 今天的日期字符串（yyyy-MM-dd） */
    fun today(): String = format.format(Date())

    /** N 天后的日期字符串 */
    fun daysFromNow(days: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return format.format(calendar.time)
    }

    /**
     * 判断上映日期是否「即将到来」：日期非空且大于今天。
     * @param releaseDate 上映日期（yyyy-MM-dd），TMDB 返回格式
     */
    fun isUpcoming(releaseDate: String?): Boolean =
        releaseDate != null && releaseDate > today()

    /**
     * 判断上映日期是否在 [今天, N 天后] 区间内（用于"临近上映"提醒）。
     */
    fun isReleasingSoon(releaseDate: String?, withinDays: Int = 7): Boolean {
        if (releaseDate == null) return false
        val today = today()
        val limit = daysFromNow(withinDays)
        return releaseDate >= today && releaseDate <= limit
    }
}
