package com.example.yasuwidget.infrastructure.time

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * 現在時刻の取得を抽象化するインタフェース
 * テスト時に差し替え可能にする（テスト可能性の確保）
 */
interface TimeProvider {
    fun now(): LocalDateTime
    fun currentDate(): LocalDate
    fun currentTime(): LocalTime
    fun currentEpochMillis(): Long
}

/**
 * 実際のシステム時刻を返す実装
 */
class SystemTimeProvider(
    private val zoneId: ZoneId = ZoneId.of("Asia/Tokyo")
) : TimeProvider {
    override fun now(): LocalDateTime = LocalDateTime.now(zoneId)
    override fun currentDate(): LocalDate = LocalDate.now(zoneId)
    override fun currentTime(): LocalTime = LocalTime.now(zoneId)
    override fun currentEpochMillis(): Long = System.currentTimeMillis()
}
