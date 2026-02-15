package com.example.yasuwidget.domain.service

import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * 発車までの残り分数を計算する
 * 純粋関数としてユニットテスト可能
 */
object MinutesUntilCalculator {

    /**
     * 現在時刻から発車時刻までの残り分数を計算する
     * 発車時刻が現在時刻以降であることを前提とする
     *
     * @param currentTime 現在時刻
     * @param departureTime 発車時刻
     * @return 残り分数（切り捨て）
     */
    fun calculate(currentTime: LocalTime, departureTime: LocalTime): Long {
        return ChronoUnit.MINUTES.between(currentTime, departureTime)
    }

    /**
     * 残り分数を表示用テキストに変換する
     *
     * @param minutesUntil 残り分数
     * @return 表示テキスト（例: "3分後"）
     */
    fun formatText(minutesUntil: Long): String {
        return "${minutesUntil}分後"
    }
}
