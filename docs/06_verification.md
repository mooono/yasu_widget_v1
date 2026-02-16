# 検証計画（v1）

## 受け入れ基準（Acceptance Criteria）
- AC-001: 村田から2km以内で TRAIN_AND_BUS になる（SYS-REQ-010）
- AC-002: 野洲駅から1km以内（村田2km外）で TRAIN_AND_BUS になる（SYS-REQ-011）
- AC-003: 通常は TRAIN_ONLY で電車が上下3本ずつ＋バスが表示される（SYS-REQ-001/012）
- AC-004: バス方向が村田半径に基づき正しく切り替わる（SYS-REQ-020/021）
- AC-005: 土日はholidayを参照する（SYS-REQ-003）
- AC-006: 位置取得失敗時にキャッシュ+「位置取得不可」を表示する（SYS-REQ-042）
- AC-007: データ未登録時に「データ未登録」を表示する（SYS-REQ-043）
- AC-009: Widgetに最終更新時刻が常に表示される（SYS-REQ-040）
- AC-010: 手動更新操作で即時更新が走る（SYS-REQ-044）

## 検証方法の指針
- Inspection: 画面・ログの目視確認、設定/表示項目の確認
- Demonstration: 実機操作での再現（ボタン操作、切替、手動更新）
- Test: 自動/手動テスト（ユニットテスト、インテグレーションテスト）
- Analysis: OS制約等の分析結果のREADMEへの明記
