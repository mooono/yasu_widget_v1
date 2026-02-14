# アーキテクチャ設計（v1）

本書は、要求仕様に基づく実装アーキテクチャを定義する。
要求の正は以下とする。
- docs/01_requirements_ears.md
- docs/06_verification.md

## 1. 設計方針

- 要求IDを満たす最小構成で実装する（スコープ逸脱しない）
- ドメインロジックは Android 依存を持たない純粋 Kotlin とする
- Widget 描画は単一の UI 状態モデルから行う
- 更新は自己再スケジュール型（約1分ごと試行）とする
- 失敗時はクラッシュさせず、キャッシュ表示へフォールバックする

---

## 2. 全体構成（レイヤ分離）

### 2.1 Presentation（Widget/UI）
責務:
- Widget 操作イベント受信（手動更新、駅切替）
- `WidgetUiState` を描画
- エラー詳細を隠蔽し、ユーザー向け文言を表示

主要コンポーネント:
- `TransitWidgetReceiver`（AppWidget/Glance エントリ）
- `WidgetRenderer`（`WidgetUiState` -> RemoteViews/Glance UI）
- `WidgetActionHandler`（`RefreshNow`、`SwitchStationNext/Prev`）

### 2.2 Application（UseCase/Orchestration）
責務:
- 更新フローのユースケース実行
- 各サービス呼び出し順の制御
- 例外捕捉とフォールバック

主要ユースケース:
- `RefreshWidgetUseCase`
- `ResolveDisplayModeUseCase`
- `ResolveBusDirectionUseCase`
- `ResolveTargetStationUseCase`
- `BuildWidgetUiStateUseCase`
- `SwitchStationOverrideUseCase`

### 2.3 Domain（純粋ロジック）
責務:
- モード判定、方向判定、次便抽出、曜日判定
- Android 非依存の判定ロジック

主要サービス:
- `DisplayModeResolver`
- `BusDirectionResolver`
- `TrainStationResolver`
- `NextDeparturesSelector`
- `ServiceDayResolver`

### 2.4 Infrastructure（I/O, Android 依存）
責務:
- 位置取得、JSON読込/パース、永続化、スケジューリング、時刻取得

主要アダプタ:
- `LocationRepository`（FusedLocationProviderClient）
- `TimetableRepository`（`train_timetable.json` / `bus_timetable.json`）
- `WidgetStateStore`（DataStore Preferences）
- `UpdateScheduler`（AlarmManager）
- `TimeProvider`（Clock ラッパ）

---

## 3. データモデル

### 3.1 Domain モデル
- `GeoPoint(lat, lon)`
- `DisplayMode` = `TRAIN_ONLY` | `TRAIN_AND_BUS` | `BUS_ONLY`
- `BusDirection` = `TO_YASU` | `TO_MURATA`
- `ServiceDay` = `WEEKDAY` | `HOLIDAY`
- `Departure(time: LocalTime, destination: String?)`
- `TrainDepartures(up: List<Departure>, down: List<Departure>)`
- `BusDepartures(list: List<Departure>)`

### 3.2 UI 状態モデル（単一ソース）
- `WidgetUiState`
  - `mode: DisplayMode`
  - `headerTitle: String`
  - `train: TrainSection?`
  - `bus: BusSection?`
  - `lastUpdatedAtText: String`
  - `statusMessage: String?`（例: 「位置取得不可」「データ未登録」）

補足:
- 描画は `WidgetUiState` のみ入力とし、描画側で業務判定しない。

### 3.3 永続化モデル（最小）
DataStore キー:
- `last_updated_at_epoch_millis`
- `last_rendered_ui_state_json`
- `pinned_station_id`
- `override_station_id`
- `override_expires_at_epoch_millis`

---

## 4. 主要判定ロジック

### 4.1 表示モード判定（SYS-REQ-010/011/012）
入力: 現在地、固定座標、半径定数

優先順位:
1. 村田2km以内 -> `BUS_ONLY`
2. 野洲1km以内（かつ1に該当しない）-> `TRAIN_AND_BUS`
3. その他 -> `TRAIN_ONLY`

### 4.2 バス方向判定（SYS-REQ-020/021）
- `distance(current, MURATA) < distance(current, YASU)` -> `TO_YASU`
- それ以外 -> `TO_MURATA`

### 4.3 電車駅選択（SYS-REQ-030/031/032/033）
優先順位:
1. 一時選択（期限内）
2. 固定駅
3. 自動最寄り駅

駅切替イベント時:
- `override_station_id` 更新
- `override_expires_at = now + 30min`

### 4.4 曜日判定（SYS-REQ-003/004）
- 土日 -> `HOLIDAY`
- 平日・祝日 -> `WEEKDAY`

### 4.5 次便抽出（SYS-REQ-001/002/003）
- 現在時刻以降の便を抽出
- 電車は上り2 + 下り2、バスは方向別2
- 不足時は取得できた本数のみ表示

---

## 5. 更新フロー設計

## 5.1 自動更新（SYS-REQ-041）
1. アラーム受信
2. `RefreshWidgetUseCase` 実行
3. 成功/失敗にかかわらず次回を約1分後に再スケジュール

## 5.2 手動更新（SYS-REQ-044）
1. Widget の更新操作イベント受信
2. 即時 `RefreshWidgetUseCase` 実行
3. 描画更新 + 次回スケジュール再設定

## 5.3 `RefreshWidgetUseCase` 詳細
1. `now = TimeProvider.now()`
2. 位置取得（失敗ならキャッシュフォールバック）
3. JSON読込・バリデーション（失敗なら「データ未登録」）
4. ドメイン判定（モード/方向/駅/曜日）
5. 次便抽出
6. `WidgetUiState` 構築
7. 永続化（`lastUpdatedAt`, `lastRenderedUiState`）
8. 描画

---

## 6. エラーハンドリング

- すべての更新エントリで `try-catch` により例外捕捉（NFR-001）
- 位置取得失敗:
  - 直近キャッシュ描画
  - `statusMessage = "位置取得不可"`（SYS-REQ-042）
- データ欠損/JSON不正:
  - `statusMessage = "データ未登録"`（SYS-REQ-043）
- いずれもクラッシュさせない

---

## 7. JSON バリデーション方針

- 必須キー欠落はエラーとして扱う
- `time` は `HH:mm` 形式のみ許可
- 不正データは黙殺せず、読込失敗として扱う
- パース処理は Domain から分離（Infrastructure 層）

---

## 8. テスト戦略（受け入れ基準対応）

### 8.1 Domain ユニットテスト
- `DisplayModeResolverTest` -> AC-001/002/003
- `BusDirectionResolverTest` -> AC-004
- `ServiceDayResolverTest` -> AC-005
- `TrainStationResolverTest` -> AC-008
- `NextDeparturesSelectorTest` -> AC-003

### 8.2 Application テスト
- `RefreshWidgetUseCaseTest`
  - 位置失敗時フォールバック + 文言（AC-006）
  - データ欠損時文言（AC-007）
  - 最終更新時刻セット（AC-009）

### 8.3 Widget/統合テスト
- 手動更新イベントから即時更新（AC-010）
- 表示モードごとの表示項目確認（UI-REQ-001）

---

## 9. 推奨パッケージ構成

- `app.widget`（Receiver, Renderer, Action）
- `app.usecase`（Refresh, SwitchStation など）
- `domain.model`（純粋モデル）
- `domain.service`（Resolver/Selector）
- `infra.location`
- `infra.timetable`
- `infra.store`
- `infra.scheduler`
- `infra.time`

---

## 10. 要求トレーサビリティ（抜粋）

- SYS-REQ-010/011/012 -> `DisplayModeResolver`
- SYS-REQ-020/021 -> `BusDirectionResolver`
- SYS-REQ-030〜033 -> `TrainStationResolver` + `SwitchStationOverrideUseCase`
- SYS-REQ-040/041/044 -> `RefreshWidgetUseCase` + `UpdateScheduler`
- SYS-REQ-042/043 -> `RefreshWidgetUseCase`（フォールバック/エラー文言）
- DATA-REQ-001/002/003 -> `TimetableRepository` + `NextDeparturesSelector`
- NFR-001/002 -> 例外捕捉・DataStore 永続化

---

## 11. v1で実装しない事項

- リアルタイム遅延取得
- 祝日カレンダー判定
- Webスクレイピング
- 全国対応拡張

以上。