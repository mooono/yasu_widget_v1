---
name: timetable-updater
description: Skill for updating static timetable JSON data files (train and bus) used by the yasu-widget app. This skill should be used when the user wants to regenerate train_timetable.json from JR HTML source files, update bus_timetable.json from manual timetable input, or when a timetable revision (ダイヤ改正) requires data refresh.
---

# Timetable Updater

Update the static timetable JSON data files for the yasu-widget app.

## When to Use

- ダイヤ改正（timetable revision）で時刻表データを更新する必要があるとき
- 電車の時刻表JSON（`train_timetable.json`）を再生成するとき
- バスの時刻表JSON（`bus_timetable.json`）を更新するとき
- 新しい駅やバス停を追加・削除するとき

## Output Files

| ファイル | パス | 生成方法 |
|---|---|---|
| 電車時刻表 | `app/src/main/assets/train_timetable.json` | JR HTML → スクリプト変換 |
| バス時刻表 | `app/src/main/assets/bus_timetable.json` | 手動入力 → スクリプト変換 |

## 電車時刻表の更新手順

### Step 1: JR HTML ファイルの取得

JRおでかけネットの路線時刻表ページから4ファイルをダウンロードする。

対象路線: **琵琶湖線・ＪＲ京都線・ＪＲ神戸線・東海道・山陽本線(米原～大阪～姫路)**

ブラウザでアクセスし HTMLとして保存、または `curl` で取得する。
URLは改正ごとに変わるため、JRおでかけネット（`https://www.jr-odekake.net/`）で都度確認する。

保存先（4ファイル）:

```
/tmp/jr_down_wd.html   ← 下り・平日
/tmp/jr_down.html      ← 下り・土休日
/tmp/jr_up_wd.html     ← 上り・平日
/tmp/jr_up.html        ← 上り・土休日
```

### Step 2: 抽出スクリプトの実行

```bash
python3 .claude/skills/timetable-updater/scripts/extract_train.py
```

出力: `/tmp/train_timetable_with_type.json`

スクリプトは以下を行う:
- 4つのHTMLから15駅分の時刻を抽出
- 列車種別（普通/快速/新快速）を取得
- 特急・寝台特急を除外
- 行先を短縮ラベルにマッピング
- `24:xx` 表記を除外
- 時刻順ソート・重複除去

### Step 3: 結果の確認とデプロイ

出力されたサマリーで各駅の本数を確認し、異常値がないか確認する。

```bash
cp /tmp/train_timetable_with_type.json app/src/main/assets/train_timetable.json
```

### Step 4: テスト実行

```bash
./gradlew test
```

## バス時刻表の更新手順

### Step 1: スクレイピング実行

NAVITIMEのページからPlaywrightで時刻表を取得する。
チェックボックス操作で村田製作所行の便のみをフィルタリングし、経由別に取得する。

```bash
# 野洲駅発（南口のりば2 + 北口のりば1）→ 村田製作所方面
python3 .claude/skills/timetable-updater/scripts/scrape_bus_yasu.py
# 出力: /tmp/bus_yasu_scraped.json

# 村田製作所発 → 野洲駅方面
python3 .claude/skills/timetable-updater/scripts/scrape_bus_murata.py
# 出力: /tmp/bus_murata_scraped.json
```

対象URL:
- **野洲駅のりば2（南口）**: `busstop=00480294&course-sequence=0007900384-1`
- **野洲駅北口のりば1**: `busstop=00480358&course-sequence=0007900402-1`
- **村田製作所**: `busstop=00480508&course-sequence=0007900395-1`

野洲駅のりば2には花緑総合・アウトレット線等の他路線が含まれるため、
チェックボックスで村田製作所行のみを自動検出してフィルタリングする。

### Step 2: JSON生成

スクレイピング結果からbus_timetable.jsonを生成する。

```bash
python3 .claude/skills/timetable-updater/scripts/generate_bus.py
```

出力: `/tmp/bus_timetable_v2.json`

### Step 3: デプロイとテスト

```bash
cp /tmp/bus_timetable_v2.json app/src/main/assets/bus_timetable.json
./gradlew test
```

## JSON フォーマット仕様

詳細は `references/data_format.md` を参照。

### 電車（概要）

```json
{
  "stations": {
    "<StationId>": {
      "name": "<駅名>",
      "lines": {
        "Tokaido": {
          "name": "琵琶湖線",
          "up": { "weekday": [...], "holiday": [...] },
          "down": { "weekday": [...], "holiday": [...] }
        }
      }
    }
  }
}
```

各エントリ: `{ "time": "HH:mm", "destination": "...", "type": "普通|快速|新快速" }`

### バス（概要）

```json
{
  "route_name": "...",
  "to_yasu": { "weekday": [...], "holiday": [...] },
  "to_murata": { "weekday": [...], "holiday": [...] },
  "notes": [...]
}
```

各エントリ: `{ "time": "HH:mm", "destination": "...", "via": "..." }`

## 対象15駅

以下のIDと駅名の対応は `domain/constants/LocationConstants.kt` と一致させること。

| ID | 駅名 |
|---|---|
| Nagaokakyo | 長岡京 |
| Mukomachi | 向日町 |
| Katsuragawa | 桂川 |
| NishiOji | 西大路 |
| Kyoto | 京都 |
| Yamashina | 山科 |
| Otsu | 大津 |
| Zeze | 膳所 |
| Ishiyama | 石山 |
| Seta | 瀬田 |
| MinamiKusatsu | 南草津 |
| Kusatsu | 草津 |
| Ritto | 栗東 |
| Moriyama | 守山 |
| Yasu | 野洲 |

## 注意事項

- 抽出スクリプトはJRおでかけネットのHTML構造に依存する。サイトリニューアル時はスクリプトの修正が必要
- 草津駅はHTMLに「発」行がなく「着」行のみの場合がある。スクリプトは着時刻にフォールバックする
- 行先マッピング（`map_destination`）は路線変更時に見直す
- `00:xx` の深夜便は翌日扱いだが、v1では `LocalTime` として扱う（日跨ぎ未対応）
