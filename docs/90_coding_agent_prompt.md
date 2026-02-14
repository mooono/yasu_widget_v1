# コーディングエージェント用プロンプト（EARS/INCOSE要求ID参照）

あなたはAndroidのコーディングエージェントです。以下の要求IDに従ってAndroidアプリ（Widget中心）を実装してください。

## ドキュメント
- `docs/01_requirements_ears.md` の要求ID（SYS-REQ / UI-REQ / DATA-REQ / NFR）を満たすこと。
- 受け入れ基準は `docs/06_verification.md` を参照。

## 実装ゴール
- 仕様は “shall” 要求（EARS）として定義済み。要求IDを満たす実装を行う。
- v1はオンライン取得を行わず、静的JSONで動作する（DATA-REQ-001/002）。

## 必須成果物
1. Android Studioでビルド可能なプロジェクト一式
2. README（ビルド手順、権限、既知制約、Dozeにより毎分更新が保証されない旨=SYS-REQ-041の補足）
3. Widget実装（UI-REQ-001/002/003）
4. 更新スケジューリング（SYS-REQ-041/040/044）
5. キャッシュ・失敗時表示（SYS-REQ-042/043, NFR-001/002）

## 技術推奨（変更可）
- Kotlin
- Widget: Jetpack Glance（可能なら）
- 状態保存: DataStore
- 位置取得: FusedLocationProviderClient
- JSON: kotlinx.serialization など

## 受け入れ（テスト観点）
- `docs/06_verification.md` のAC-001〜010を満たすこと。
