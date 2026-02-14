package com.example.yasuwidget.infrastructure.timetable

import android.content.Context
import com.example.yasuwidget.domain.model.BusTimetable
import com.example.yasuwidget.domain.model.TrainTimetable
import java.io.File

/**
 * 時刻表データの読み込み（DATA-REQ-001/002）
 *
 * - 電車: 内部ストレージ優先 → assets フォールバック（ユーザー投入）
 * - バス: assets から読み込み（アプリ同梱）
 */
class TimetableRepository(private val context: Context) {

    companion object {
        const val TRAIN_TIMETABLE_FILENAME = "train_timetable.json"
        const val BUS_TIMETABLE_FILENAME = "bus_timetable.json"
    }

    /**
     * 電車時刻表を読み込む
     * 内部ストレージに存在すればそちらを優先し、なければassetsから読み込む
     *
     * @return TrainTimetable または null（データ不在時）
     * @throws TimetableParseException パース失敗時
     */
    fun loadTrainTimetable(): TrainTimetable? {
        val json = readFromInternalStorage(TRAIN_TIMETABLE_FILENAME)
            ?: readFromAssets(TRAIN_TIMETABLE_FILENAME)
            ?: return null

        return TimetableParser.parseTrainTimetable(json)
    }

    /**
     * バス時刻表を読み込む（アプリ同梱assets）
     *
     * @return BusTimetable または null（データ不在時）
     * @throws TimetableParseException パース失敗時
     */
    fun loadBusTimetable(): BusTimetable? {
        val json = readFromAssets(BUS_TIMETABLE_FILENAME) ?: return null
        return TimetableParser.parseBusTimetable(json)
    }

    private fun readFromInternalStorage(filename: String): String? {
        val file = File(context.filesDir, filename)
        return if (file.exists()) {
            file.readText(Charsets.UTF_8)
        } else {
            null
        }
    }

    private fun readFromAssets(filename: String): String? {
        return try {
            context.assets.open(filename).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }
}
