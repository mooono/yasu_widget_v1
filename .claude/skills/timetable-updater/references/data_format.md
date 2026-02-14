# 時刻表JSON フォーマット仕様

本文書は `docs/03_data_spec.md` に基づく、JSONファイルの詳細フォーマット。

## train_timetable.json

### 構造

```
stations
  └─ <StationId>           (例: "Yasu", "Kyoto")
       ├─ name: string     (例: "野洲", "京都")
       └─ lines
            └─ Tokaido
                 ├─ name: string   (例: "琵琶湖線")
                 ├─ up              ← 京都・大阪方面
                 │   ├─ weekday: Departure[]
                 │   └─ holiday: Departure[]
                 └─ down            ← 米原方面
                     ├─ weekday: Departure[]
                     └─ holiday: Departure[]
```

### Departure オブジェクト

| フィールド | 型 | 必須 | 説明 |
|---|---|---|---|
| `time` | string | ○ | `HH:mm` 24時間表記 |
| `destination` | string | × | 行先の表示ラベル |
| `type` | string | × | 列車種別: `"普通"`, `"快速"`, `"新快速"` |

### StationId 一覧

LocationConstants.kt の `TOKAIDO_STATIONS` と一致すること。

```
Nagaokakyo, Mukomachi, Katsuragawa, NishiOji, Kyoto,
Yamashina, Otsu, Zeze, Ishiyama, Seta,
MinamiKusatsu, Kusatsu, Ritto, Moriyama, Yasu
```

### 行先マッピングルール

#### 上り（京都・大阪方面）
- 網干 / 播州赤穂 / 上郡 → `"姫路方面"`
- 新三田 / 関西空港 / 宝塚 / 倉吉 / 鳥取 / 香住 / 豊岡 / 白浜 / 浜坂 / 城崎温泉 → `"大阪方面"`
- その他 → 行先そのまま（京都, 高槻, 大阪 等）
- 行先なし → `"京都方面"`

#### 下り（米原方面）
- 四条畷 / 松井山手 / 長尾 / 京田辺 / 同志社前 / 木津 / 高山 → `"米原方面"`
- その他 → 行先そのまま（米原, 近江塩津, 長浜 等）
- 行先なし → `"米原方面"`

### 除外対象
- 列車種別が `特急` または `寝台特急` の列車
- 時刻が `24:xx` 表記の列車

---

## bus_timetable.json

### 構造

```
├─ route_name: string        (例: "野洲駅 ⇄ 村田製作所（野洲）")
├─ to_yasu                   ← 村田 → 野洲駅方面
│   ├─ weekday: Departure[]
│   └─ holiday: Departure[]
├─ to_murata                 ← 野洲駅 → 村田方面
│   ├─ weekday: Departure[]
│   └─ holiday: Departure[]
└─ notes: string[]           (メタデータ、パーサーでは消費しない)
```

### Departure オブジェクト

| フィールド | 型 | 必須 | 説明 |
|---|---|---|---|
| `time` | string | ○ | `HH:mm` 24時間表記 |
| `destination` | string | × | 行先 + 乗り場区別 |

### 乗り場区別（to_murata 方向のみ）

- `"村田製作所"` → 野洲駅のりば2（南口）から発車
- `"村田製作所(北口発)"` → 野洲駅北口のりば1から発車

to_yasu 方向は全便 `"野洲駅"` とする（着地の南口/北口は区別不要）。

### notes フィールド

人間向けメタデータ。アプリのパーサー(`TimetableParser`)では読み込まない。
ダイヤの出典や注意事項を記録する。

---

## バリデーションルール

パーサー (`TimetableParser.kt`) が適用するルール:

1. 電車: `stations` キーが必須、空は不可
2. 電車: 各駅に `name`, `lines` が必須
3. 電車: 各路線に `name`, `up`, `down` が必須
4. 電車/バス: 各方向に `weekday`, `holiday` 配列が必須
5. 各エントリに `time` が必須（`HH:mm` 形式）
6. バス: `route_name`, `to_yasu`, `to_murata` が必須
7. `destination` は省略可（空文字にフォールバック）
8. `type` は省略可（空文字にフォールバック）
