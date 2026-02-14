package com.example.yasuwidget.infrastructure.timetable

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

/**
 * 時刻表JSONパーサーのテスト（DATA-REQ-001/002）
 * AC-007相当: データ不正時のエラーハンドリング
 */
class TimetableParserTest {

    // --- 電車時刻表パース ---

    @Test
    fun `正常な電車時刻表JSONをパースできる`() {
        val json = """
        {
          "stations": {
            "Yasu": {
              "name": "野洲",
              "lines": {
                "Tokaido": {
                  "name": "東海道線",
                  "up": {
                    "weekday": [
                      { "time": "07:12", "destination": "京都" },
                      { "time": "07:25", "destination": "京都" }
                    ],
                    "holiday": []
                  },
                  "down": {
                    "weekday": [
                      { "time": "07:05", "destination": "米原" }
                    ],
                    "holiday": [
                      { "time": "08:00", "destination": "米原" }
                    ]
                  }
                }
              }
            }
          }
        }
        """.trimIndent()

        val timetable = TimetableParser.parseTrainTimetable(json)

        assertEquals(1, timetable.stations.size)
        val yasu = timetable.stations["Yasu"]!!
        assertEquals("野洲", yasu.name)
        assertEquals(1, yasu.lines.size)

        val tokaido = yasu.lines["Tokaido"]!!
        assertEquals("東海道線", tokaido.name)
        assertEquals(2, tokaido.up.weekday.size)
        assertEquals(0, tokaido.up.holiday.size)
        assertEquals(1, tokaido.down.weekday.size)
        assertEquals(1, tokaido.down.holiday.size)

        assertEquals(LocalTime.of(7, 12), tokaido.up.weekday[0].time)
        assertEquals("京都", tokaido.up.weekday[0].destination)
        assertEquals(LocalTime.of(7, 5), tokaido.down.weekday[0].time)
    }

    @Test(expected = TimetableParseException::class)
    fun `stationsキーが欠損している場合はエラー`() {
        TimetableParser.parseTrainTimetable("""{ "other": {} }""")
    }

    @Test(expected = TimetableParseException::class)
    fun `空のstationsはエラー`() {
        TimetableParser.parseTrainTimetable("""{ "stations": {} }""")
    }

    @Test(expected = TimetableParseException::class)
    fun `不正な時刻形式はエラー`() {
        val json = """
        {
          "stations": {
            "Yasu": {
              "name": "野洲",
              "lines": {
                "Tokaido": {
                  "name": "東海道線",
                  "up": {
                    "weekday": [{ "time": "25:99", "destination": "京都" }],
                    "holiday": []
                  },
                  "down": {
                    "weekday": [],
                    "holiday": []
                  }
                }
              }
            }
          }
        }
        """.trimIndent()
        TimetableParser.parseTrainTimetable(json)
    }

    @Test(expected = TimetableParseException::class)
    fun `不正なJSON文字列はエラー`() {
        TimetableParser.parseTrainTimetable("not a json")
    }

    // --- バス時刻表パース ---

    @Test
    fun `正常なバス時刻表JSONをパースできる`() {
        val json = """
        {
          "route_name": "野洲駅 ⇄ 村田製作所（野洲）",
          "to_yasu": {
            "weekday": [
              { "time": "07:10", "destination": "野洲駅" }
            ],
            "holiday": []
          },
          "to_murata": {
            "weekday": [
              { "time": "07:30", "destination": "村田（野洲）" }
            ],
            "holiday": []
          }
        }
        """.trimIndent()

        val timetable = TimetableParser.parseBusTimetable(json)

        assertEquals("野洲駅 ⇄ 村田製作所（野洲）", timetable.routeName)
        assertEquals(1, timetable.toYasu.weekday.size)
        assertEquals(LocalTime.of(7, 10), timetable.toYasu.weekday[0].time)
        assertEquals("野洲駅", timetable.toYasu.weekday[0].destination)
        assertEquals(1, timetable.toMurata.weekday.size)
    }

    @Test(expected = TimetableParseException::class)
    fun `route_nameが欠損している場合はエラー`() {
        val json = """
        {
          "to_yasu": { "weekday": [], "holiday": [] },
          "to_murata": { "weekday": [], "holiday": [] }
        }
        """.trimIndent()
        TimetableParser.parseBusTimetable(json)
    }

    @Test(expected = TimetableParseException::class)
    fun `to_yasuが欠損している場合はエラー`() {
        val json = """
        {
          "route_name": "test",
          "to_murata": { "weekday": [], "holiday": [] }
        }
        """.trimIndent()
        TimetableParser.parseBusTimetable(json)
    }

    @Test
    fun `destinationが省略可能なことを確認`() {
        val json = """
        {
          "route_name": "test",
          "to_yasu": {
            "weekday": [{ "time": "07:10" }],
            "holiday": []
          },
          "to_murata": {
            "weekday": [],
            "holiday": []
          }
        }
        """.trimIndent()

        val timetable = TimetableParser.parseBusTimetable(json)
        assertEquals(1, timetable.toYasu.weekday.size)
        assertEquals("", timetable.toYasu.weekday[0].destination)
    }
}
