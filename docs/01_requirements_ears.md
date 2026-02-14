# 主要要求（EARS / INCOSE準拠）

本章の各要求は、以下の要件を満たすように記述する。
- **一義性**: 誰が読んでも同じ解釈
- **検証可能性**: 受け入れ試験で確認できる
- **必要十分性**: v1スコープに対して過不足がない
- **原則**: “shall” を用いる（推奨）

---

## 5. 機能要求（System Functional Requirements）

### 5.1 表示本数・曜日判定
- **SYS-REQ-001（Ubiquitous）**  
  システムは、電車の発車時刻を **上り次2本** と **下り次2本**（合計4本）表示 **しなければならない**。  
  検証: Test

- **SYS-REQ-002（Ubiquitous）**  
  システムは、バスの発車時刻を、表示方向に対して **次2本** 表示 **しなければならない**。  
  検証: Test

- **SYS-REQ-003（Ubiquitous）**  
  システムは、端末ローカル曜日に基づき、土日を `holiday`、平日を `weekday` として時刻表を選択 **しなければならない**。  
  検証: Test

- **SYS-REQ-004（Ubiquitous）**  
  システムは、祝日判定を行わず、祝日を `weekday` として扱わ **なければならない**。  
  検証: Inspection + Test

### 5.2 位置に基づく表示モード切替
（固定座標）
- `YASU_STATION`（野洲駅）
- `MURATA_YASU`（村田製作所 野洲事業所）

（定数）
- `MURATA_RADIUS = 2000m`
- `YASU_RADIUS = 1000m`

- **SYS-REQ-010（State-driven / While）**  
  **現在地が** `MURATA_YASU` から `MURATA_RADIUS` 以内 **である間**、システムはWidgetに **バスのみ** を表示 **しなければならない**。  
  検証: Test

- **SYS-REQ-011（State-driven / While）**  
  **現在地が** `YASU_STATION` から `YASU_RADIUS` 以内 **である間** かつ **SYS-REQ-010の条件に該当しない間**、システムはWidgetに **電車とバスの両方** を表示 **しなければならない**。  
  検証: Test

- **SYS-REQ-012（Ubiquitous）**  
  **SYS-REQ-010およびSYS-REQ-011のいずれにも該当しない場合**、システムはWidgetに **電車のみ** を表示 **しなければならない**。  
  検証: Test

### 5.3 バス方向の自動決定
- **SYS-REQ-020（Ubiquitous）**  
  `distance(current, MURATA_YASU) < distance(current, YASU_STATION)` の場合、システムはバス表示方向を **野洲駅方面（Murata→Yasu）** と **しなければならない**。  
  検証: Test

- **SYS-REQ-021（Ubiquitous）**  
  SYS-REQ-020の条件に該当しない場合、システムはバス表示方向を **村田方面（Yasu→Murata）** と **しなければならない**。  
  検証: Test

### 5.4 電車の駅選択（自動/固定）

- **SYS-REQ-030（Ubiquitous）**  
  システムは、電車表示対象駅を、以下の優先順位で決定 **しなければならない**。  
  1) 固定駅（設定画面）  
  2) 自動最寄り駅（現在地から直線距離最短）  
  検証: Test

### 5.5 更新・状態表示
- **SYS-REQ-040（Ubiquitous）**  
  システムは、Widgetに **最終更新時刻** を表示 **しなければならない**。  
  検証: Inspection + Test

- **SYS-REQ-041（Ubiquitous）**  
  システムは、更新を **可能な限り1分おき** に試行 **しなければならない**。  
  検証: Analysis + Demonstration

- **SYS-REQ-042（Event-driven / When）**  
  **位置取得に失敗したとき**、システムは直近の成功結果（キャッシュ）を表示し、状態メッセージとして **「位置取得不可」** を表示 **しなければならない**。  
  検証: Test

- **SYS-REQ-043（Event-driven / When）**  
  **必要な時刻表データが存在しないとき**、システムは状態メッセージとして **「データ未登録」** を表示 **しなければならない**。  
  検証: Test

- **SYS-REQ-044（Event-driven / When）**  
  **ユーザーがWidgetの手動更新操作を行ったとき**、システムは即時に更新処理を実行 **しなければならない**。  
  検証: Demonstration + Test

---

## 6. UI要求（Widget操作を含む）
- **UI-REQ-001（Ubiquitous）**  
  Widgetは、表示モードに応じて以下の情報を表示 **しなければならない**。  
  - TRAIN_ONLY: 電車（上り2/下り2）＋最終更新時刻  
  - TRAIN_AND_BUS: 電車（上り2/下り2）＋バス（次2）＋最終更新時刻  
  - BUS_ONLY: バス（次2）＋最終更新時刻  
  検証: Inspection + Demonstration

- **UI-REQ-003（Ubiquitous）**  
  Widgetは、手動更新のための操作要素（ボタン等）を提供 **しなければならない**。  
  検証: Demonstration

---

## 7. データ要求
- **DATA-REQ-001（Ubiquitous）**  
  システムは、電車時刻表を `train_timetable.json` から読み込み **しなければならない**。  
  検証: Test

- **DATA-REQ-002（Ubiquitous）**  
  システムは、バス時刻表を `bus_timetable.json` から読み込み **しなければならない**。  
  検証: Test

- **DATA-REQ-003（Ubiquitous）**  
  システムは、現在時刻以降の便を抽出し、要求本数（電車4本、バス2本）に満たない場合は、取得できた範囲で表示 **しなければならない**。  
  検証: Test

---

## 8. 非機能要求（v1）
- **NFR-001（Ubiquitous）**  
  システムは、更新処理が失敗してもWidgetがクラッシュしないように、例外を捕捉し安全に処理 **しなければならない**。  
  検証: Test

- **NFR-002（Ubiquitous）**  
  システムは、位置情報およびキャッシュ等のデータを適切に保持し、アプリ再起動後もWidget表示を継続 **しなければならない**。  
  検証: Demonstration + Test
