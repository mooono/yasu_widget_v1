# インタフェース仕様（v1）

## 入力インタフェース
- 位置情報（FusedLocationProviderClient等）
- 時刻表JSON（電車: ユーザー投入、バス: 同梱）
- ユーザー設定（固定駅）

## 出力インタフェース
- Android AppWidget / Glance によるWidget描画

## データ保存
- DataStore（Preferences）推奨
  - lastUpdatedAt
  - lastRenderedModel（最低限）
  - pinnedStationId（任意）
