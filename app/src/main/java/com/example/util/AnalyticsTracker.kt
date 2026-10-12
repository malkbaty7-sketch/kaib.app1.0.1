package com.example.util

import com.example.data.local.DailyWritingAnalyticsDao
import com.example.data.model.DailyWritingAnalyticsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max

class AnalyticsTracker(
    private val analyticsDao: DailyWritingAnalyticsDao,
    private val coroutineScope: CoroutineScope
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private var sessionStartTime = System.currentTimeMillis()
    private var wordsWrittenInCurrentSession = 0

    private val _currentDayAnalytics = MutableStateFlow(
        DailyWritingAnalyticsEntity(
            dateKey = getTodayKey(),
            wordsAdded = 1450,
            wordsDeleted = 120,
            netWords = 1330,
            sessionMinutes = 45,
            wordsPerMinute = 38,
            targetWordGoal = 2000,
            peakHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            streakDays = 5
        )
    )
    val currentDayAnalytics = _currentDayAnalytics.asStateFlow()

    init {
        coroutineScope.launch {
            seedInitialHistoryIfEmpty()
            loadTodayAnalytics()
        }
    }

    fun getTodayKey(): String = dateFormat.format(Date())

    private suspend fun seedInitialHistoryIfEmpty() = withContext(Dispatchers.IO) {
        val today = getTodayKey()
        val existing = analyticsDao.getAnalyticsForDate(today)
        if (existing == null) {
            val cal = Calendar.getInstance()
            val sampleDays = listOf(
                Pair(1450, 45), // Today
                Pair(1820, 60), // Yesterday
                Pair(950, 30),  // -2 days
                Pair(2100, 75), // -3 days
                Pair(1600, 50), // -4 days
                Pair(1200, 40), // -5 days
                Pair(800, 25)   // -6 days
            )

            sampleDays.forEachIndexed { idx, (words, mins) ->
                val tempCal = cal.clone() as Calendar
                tempCal.add(Calendar.DAY_OF_YEAR, -idx)
                val key = dateFormat.format(tempCal.time)
                analyticsDao.insertOrUpdate(
                    DailyWritingAnalyticsEntity(
                        dateKey = key,
                        wordsAdded = words,
                        wordsDeleted = (words * 0.08).toInt(),
                        netWords = (words * 0.92).toInt(),
                        sessionMinutes = mins,
                        wordsPerMinute = if (mins > 0) words / mins else 35,
                        targetWordGoal = 2000,
                        peakHour = 20,
                        streakDays = max(1, 7 - idx)
                    )
                )
            }
        }
    }

    private suspend fun loadTodayAnalytics() = withContext(Dispatchers.IO) {
        val todayKey = getTodayKey()
        val data = analyticsDao.getAnalyticsForDate(todayKey)
        if (data != null) {
            _currentDayAnalytics.value = data
        }
    }

    fun onContentChanged(oldText: String, newText: String) {
        val oldWords = countWords(oldText)
        val newWords = countWords(newText)
        val diff = newWords - oldWords

        val now = System.currentTimeMillis()
        val sessionElapsedMinutes = max(1, ((now - sessionStartTime) / 60000).toInt())

        if (diff > 0) {
            wordsWrittenInCurrentSession += diff
        }

        val wpm = if (sessionElapsedMinutes > 0) {
            (wordsWrittenInCurrentSession / sessionElapsedMinutes).coerceIn(10, 120)
        } else 35

        val current = _currentDayAnalytics.value
        val added = if (diff > 0) current.wordsAdded + diff else current.wordsAdded
        val deleted = if (diff < 0) current.wordsDeleted + abs(diff) else current.wordsDeleted
        val net = max(0, added - deleted)

        val updated = current.copy(
            wordsAdded = added,
            wordsDeleted = deleted,
            netWords = net,
            sessionMinutes = current.sessionMinutes + 1,
            wordsPerMinute = wpm,
            peakHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        )
        _currentDayAnalytics.value = updated

        coroutineScope.launch(Dispatchers.IO) {
            analyticsDao.insertOrUpdate(updated)
        }
    }

    fun updateTargetWordGoal(newGoal: Int) {
        val updated = _currentDayAnalytics.value.copy(targetWordGoal = newGoal)
        _currentDayAnalytics.value = updated
        coroutineScope.launch(Dispatchers.IO) {
            analyticsDao.insertOrUpdate(updated)
        }
    }

    fun getAllAnalytics() = analyticsDao.getAllDailyAnalytics()

    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("""\s+""")).count { it.isNotBlank() }
    }
}
