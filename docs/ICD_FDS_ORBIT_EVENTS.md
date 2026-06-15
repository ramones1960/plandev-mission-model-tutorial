# ICD: FDS 軌道イベントファイル

| 項目 | 内容 |
|---|---|
| 文書番号 | ICD-FDS-001 |
| 版 | 1.0 |
| インタフェース | 軌道力学系（FDS） → 運用計画系（Aerie ミッションモデル） |
| 対象成果物 | 軌道イベントファイル（地上局可視 AOS/LOS・食 本影出入り） |

---

## 1. 目的・適用範囲

本 ICD は、軌道力学系（FDS: Flight Dynamics System）が軌道決定・予報に基づいて解析した
**軌道イベント**を、運用計画系（Aerie 上の本ミッションモデル）へ受け渡すための
ファイルインタフェースを定める。

地上局可視および食（日陰）は**ミッションモデル内では計算しない**。
本ファイルが運用計画における可視・食情報の唯一の情報源（Single Source of Truth）である。

本 ICD の適用範囲はファイルフォーマット・整合性規則・受け渡し手順・
ミッションモデル側での公開リソースまでとし、FDS 内部の軌道解析手法は対象外とする。

## 2. インタフェース概要

```
┌─────────────┐   軌道イベントファイル    ┌──────────────────────────┐
│  FDS         │  (orbit_events_*.csv)   │  Aerie ミッションモデル              │
│  ・軌道決定   │ ───────────────────▶ │  ・シミュレーション開始時に読込・検証      │
│  ・パス解析   │                         │  ・タイムラインに再生し離散リソース化     │
│  ・食解析     │                         │     /ground/station_visible ほか     │
└─────────────┘                         │  ・アクティビティの waitUntil と       │
                                         │    制約(Constraint)の両方から参照     │
                                         └──────────────────────────┘
```

- **作成者**: FDS（パス予報・食予報の確定後に出力）
- **利用者**: Aerie ミッションモデル（シミュレーション設定 `orbitEventsFilePath` で指定）
- **配付契機**: プランニングサイクルごと（軌道決定更新時は再配付し、プランを再シミュレーションする）

## 3. ファイル仕様

### 3.1 形式

| 項目 | 規定 |
|---|---|
| 形式 | CSV（カンマ区切り、引用符なし） |
| 文字コード | UTF-8（BOM なし） |
| 改行コード | LF |
| ファイル名規約 | `orbit_events_<YYYYMMDD>_<version>.csv`（例: `orbit_events_20260101_v01.csv`） |
| コメント行 | `#` で始まる行は読み飛ばされる。先頭にメタ情報（ICD 版数・カバレッジ期間）を記載すること |
| ヘッダ行 | `event_time_utc,event_type,station_id`（1行、必須） |

### 3.2 カラム定義

| # | カラム | 型 | 必須 | 内容 |
|---|---|---|---|---|
| 1 | `event_time_utc` | ISO 8601 UTC（`YYYY-MM-DDThh:mm:ssZ`、秒以下任意） | ○ | イベント発生時刻 |
| 2 | `event_type` | 列挙（3.3 参照） | ○ | イベント種別 |
| 3 | `station_id` | 文字列（英数字とハイフン） | AOS/LOS のみ○ | 地上局識別子。食イベントでは空欄 |

### 3.3 イベント種別

| event_type | 意味 | ミッションモデルへの影響 |
|---|---|---|
| `AOS` | Acquisition of Signal: 地上局可視ウィンドウ開始 | `/ground/station_visible` → true、`/ground/visible_station_id` → station_id |
| `LOS` | Loss of Signal: 地上局可視ウィンドウ終了 | `/ground/station_visible` → false、`/ground/visible_station_id` → 空文字列 |
| `UMBRA_ENTRY` | 本影突入（食開始） | `/orbit/in_sunlight` → false、太陽電池発電 0 W |
| `UMBRA_EXIT` | 本影脱出（食終了） | `/orbit/in_sunlight` → true、発電回復、`/orbit/orbit_number` +1 |

可視ウィンドウは仰角マスク・局運用制約を加味した**運用可能ウィンドウ**として FDS が出力する
（幾何学的可視からのマージン取りは FDS 側の責務）。

## 4. 時刻系・カバレッジ

1. すべての時刻は UTC とする。
2. ファイルは**対象プラン期間全体 + 前後マージン（推奨: 前後各1周回以上）**をカバーすること。
3. プラン開始時刻ちょうど、またはそれ以前のイベントは「プラン開始時点の初期状態」の導出に
   使用される（例: 開始前に `AOS` のみがあり対応する `LOS` が開始後にある場合、開始時点で可視と扱う）。
4. プラン期間より後のイベントは無視される（記載してよい）。

## 5. 整合性規則

ミッションモデルはファイル読込時（シミュレーション開始時）に以下を検証し、
違反があれば行番号付きのエラーで**シミュレーションを開始しない**（fail-fast）。

### 5.1 共通

- (R1) イベントは時刻昇順に記載すること。
- (R2) `event_type` は 3.3 の4種のいずれかであること。
- (R3) `AOS`/`LOS` には `station_id` を必ず記載すること。

### 5.2 食イベント

- (R4) `UMBRA_ENTRY` と `UMBRA_EXIT` は交互に出現すること
  （ファイル先頭が `UMBRA_EXIT` の場合、ファイル開始時点で食中とみなす）。
- (R5) 食イベントが1件も記載されていない期間は**日照**とみなす。

### 5.3 可視ウィンドウ

- (R6) 可視ウィンドウは `AOS` → 同一 `station_id` の `LOS` の完全な対で記載すること。
- (R7) 可視ウィンドウ同士は重複しないこと（地上系は同時に1局のみ運用する前提。
  複数局が幾何学的に可視の場合も、FDS が運用局を選択して1本のウィンドウ列に編成する）。

## 6. 受け渡し手順（Aerie への取り込み）

1. FDS は 3〜5 章に適合するファイルを配付する。
2. 運用計画担当は Aerie のシミュレーション設定（Simulation Configuration）で
   `orbitEventsFilePath` に当該ファイルのパスを設定する
   （Aerie UI では Path 型パラメータとしてファイルをアップロードして指定する）。
3. シミュレーション実行時にミッションモデルが読込・検証し、
   タイムライン上の離散リソースとして再生する。
4. 軌道決定の更新でファイルが再配付された場合は、設定を差し替えて再シミュレーションする。
   ファイル差し替え前のシミュレーション結果と混在させてはならない。

## 7. ミッションモデル側での公開リソース

| リソース | 型 | 由来イベント | 用途 |
|---|---|---|---|
| `/ground/station_visible` | discrete (boolean) | AOS/LOS | ダウンリンク可否の判定・制約 |
| `/ground/visible_station_id` | discrete (string) | AOS/LOS | 使用局の確認・局別制約 |
| `/orbit/in_sunlight` | discrete (boolean) | UMBRA_ENTRY/EXIT | 電力収支・充電系アクティビティの制約 |
| `/orbit/orbit_number` | discrete (int) | UMBRA_EXIT | 周回基準の運用カウント |

アクティビティからは条件待ちで参照できる（例: `Downlink` は
`waitUntil(stationVisible.is(true))` で可視ウィンドウ開始まで待機する）。

## 8. 制約（Constraint）としての利用例

Aerie の Constraints eDSL（TypeScript）から本リソースを参照し、
プランの成立性を機械的に検証する。代表例:

```ts
// ダウンリンクは地上局可視ウィンドウ内でのみ実行されていること
export default (): Constraint =>
  Windows.During(ActivityType.Downlink).if(
    Discrete.Resource("/ground/station_visible").equal(true)
  );
```

```ts
// 充電強化は日照中にのみ実行されていること
export default (): Constraint =>
  Windows.During(ActivityType.SolarCharging).if(
    Discrete.Resource("/orbit/in_sunlight").equal(true)
  );
```

※ eDSL の正確な構文は使用する Aerie バージョンの
[Constraints ドキュメント](https://nasa-ammos.github.io/aerie-docs/constraints/introduction/)に従うこと。

## 9. サンプル

リポジトリ同梱の [`fds/orbit_events_sample.csv`](../fds/orbit_events_sample.csv) は、
軌道周期 95 分（うち食 35 分）・2局交互・6時間間隔 8 分パスの 26 時間分のサンプルである。
チュートリアル用であり、実運用では FDS 配付物に差し替えること。

```csv
# FDS orbit events product (sample)
# ICD: docs/ICD_FDS_ORBIT_EVENTS.md, version 1.0
event_time_utc,event_type,station_id
2026-01-01T01:00:00Z,UMBRA_ENTRY,
2026-01-01T01:35:00Z,UMBRA_EXIT,
2026-01-01T01:40:00Z,AOS,GS-1
2026-01-01T01:48:00Z,LOS,GS-1
```

## 10. 改訂管理

| 版 | 日付 | 内容 |
|---|---|---|
| 1.0 | 2026-06-11 | 初版制定 |

本 ICD の変更は FDS・運用計画系双方の合意により行い、`event_type` の追加は
後方互換（未知タイプをエラーとする現行実装の改修を伴う）として版数を上げること。
