# データ仕様（v1）

本章は、DATA-REQ-001/002を満たすためのJSON構造を定義する。

## train_timetable.json（ユーザー投入）
```json
{
  "stations": {
    "Yasu": {
      "name": "野洲",
      "lines": {
        "Tokaido": {
          "name": "東海道線",
          "up": {
            "weekday": [
              { "time": "07:05", "destination": "米原" }
            ],
            "holiday": []
          },
          "down": {
            "weekday": [
              { "time": "07:12", "destination": "網干" }
            ],
            "holiday": []
          }
        }
      }
    }
  }
}
```

- `time`: `HH:mm`（24時間表記）
- `destination`: 表示用（省略可）
- `type`: 列車種別（省略可）。例: `"新快速"`, `"快速"`, `"普通"`

## bus_timetable.json（同梱）
```json
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
  },
  "notes": [
    "村田製作所休業日は運休の可能性があります（v1は休業日判定しません）"
  ]
}
```
