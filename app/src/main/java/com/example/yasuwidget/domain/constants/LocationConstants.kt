package com.example.yasuwidget.domain.constants

import com.example.yasuwidget.domain.model.GeoPoint
import com.example.yasuwidget.domain.model.StationInfo

/**
 * 位置情報に関する定数
 * SYS-REQ-010/011: 表示モード判定用半径
 */
object LocationConstants {
    /** 野洲駅 */
    val YASU_STATION = GeoPoint(35.0654, 136.0253)

    /** 村田製作所 野洲事業所 */
    val MURATA_YASU = GeoPoint(35.0480, 136.0330)

    /** 村田判定半径（メートル）SYS-REQ-010 */
    const val MURATA_RADIUS_METERS = 2000.0

    /** 野洲駅判定半径（メートル）SYS-REQ-011 */
    const val YASU_RADIUS_METERS = 1000.0

    /**
     * 東海道線 長岡京〜野洲 の全駅一覧（v1スコープ）
     * 自動最寄り駅判定に使用（SYS-REQ-030）
     */
    val TOKAIDO_STATIONS: List<StationInfo> = listOf(
        StationInfo("Nagaokakyo", "長岡京", GeoPoint(34.9312, 135.6955)),
        StationInfo("Mukomachi", "向日町", GeoPoint(34.9389, 135.7081)),
        StationInfo("Katsuragawa", "桂川", GeoPoint(34.9561, 135.7119)),
        StationInfo("NishiOji", "西大路", GeoPoint(34.9864, 135.7341)),
        StationInfo("Kyoto", "京都", GeoPoint(34.9858, 135.7588)),
        StationInfo("Yamashina", "山科", GeoPoint(34.9926, 135.8155)),
        StationInfo("Otsu", "大津", GeoPoint(35.0065, 135.8600)),
        StationInfo("Zeze", "膳所", GeoPoint(35.0059, 135.8717)),
        StationInfo("Ishiyama", "石山", GeoPoint(34.9642, 135.9020)),
        StationInfo("Seta", "瀬田", GeoPoint(34.9631, 135.9266)),
        StationInfo("MinamiKusatsu", "南草津", GeoPoint(35.0063, 135.9521)),
        StationInfo("Kusatsu", "草津", GeoPoint(35.0186, 135.9609)),
        StationInfo("Ritto", "栗東", GeoPoint(35.0247, 135.9909)),
        StationInfo("Moriyama", "守山", GeoPoint(35.0524, 136.0055)),
        StationInfo("Yasu", "野洲", GeoPoint(35.0654, 136.0253))
    )
}
