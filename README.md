# Plandev Mission Model Tutorial

NASA AMMOS [Aerie](https://github.com/NASA-AMMOS/aerie) 向け衛星運用計画ミッションモデルのチュートリアルリポジトリです。
小型 LEO 衛星を模したモデルで、**観測 → 姿勢変更 → ダウンリンク → 充電管理 → セーフモード**という
衛星運用の一連の流れを Aerie 上でプランニング・シミュレーションできます。

地上局可視・食などの軌道イベントは**ミッションモデル内では計算せず**、外部の軌道力学系（FDS）が
解析した成果物ファイルを取り込みます。インタフェースは
[ICD: FDS 軌道イベントファイル](docs/ICD_FDS_ORBIT_EVENTS.md) で定義しています。

---

## モデルの全体像

```
┌─────────┐ 軌道イベントファイル ┌────────────────────────────────┐
│   FDS    │ (ICD-FDS-001 準拠) │  Aerie ミッションモデル                       │
│ 軌道決定  │ ────────────────▶│                                          │
│ パス解析  │                    │  デーモン（自律動作）                        │
│ 食解析    │                    │   ・FDS イベント再生: 日照/食・地上局可視      │
└─────────┘                    │   ・充電制御: 満充電クランプ                  │
                                │   ・電力FDIR: SoC低下→自動セーフモード        │
                                │                                          │
 プラン（アクティビティ）          │  Slew → Observation → Slew → Downlink …   │
                                │       │ リソースを消費/生成                  │
                                │  電力（バッテリー/負荷/発電）・データ（SSR）     │
                                │  モード・指向・可視・日照 … 計15リソース        │
                                └────────────────────────────────┘
```

運用検討のポイントになる要素をすべてモデル化しています。

| 検討項目 | モデル上の表現 |
|---|---|
| 食（日陰）での電力収支 | FDS の UMBRA_ENTRY/EXIT イベントで太陽電池出力が 0 ⇔ 定格に切り替わる |
| 地上局パスの待ち | FDS の AOS/LOS イベントが可視リソースを駆動し、`Downlink` が `waitUntil` で待機 |
| 姿勢の競合 | 観測・通信・充電で必要な指向モードが異なり `Slew` が必要 |
| SSR の溜まり過ぎ | 観測で増加・ダウンリンクで減少、使用率を制約でチェック |
| バッテリー保護 | SoC 低下で FDIR が自動セーフモード化、観測コマンドを拒否 |

## FDS インタフェース（軌道イベントの取り込み）

- 可視・食はモデル内で計算しない。**FDS 配付の軌道イベントファイル（CSV）が唯一の情報源**。
- ファイルはシミュレーション設定 `orbitEventsFilePath` で指定し、シミュレーション開始時に
  読込・ICD 整合性検証（時系列順・AOS/LOS 対応・非重複など）を行い、違反時は fail-fast する。
- 取り込んだイベントは離散リソース（下表）としてタイムラインに再生され、
  アクティビティの `waitUntil` と Aerie の制約（Constraint）の両方から参照できる。

詳細・制約の記述例: [docs/ICD_FDS_ORBIT_EVENTS.md](docs/ICD_FDS_ORBIT_EVENTS.md)。
サンプル成果物: [fds/orbit_events_sample.csv](fds/orbit_events_sample.csv)（26時間分）。

## リソース構成

| リソース名 | 型 | 単位 | 説明 |
|---|---|---|---|
| `/power/battery_energy_wh` | real | Wh | バッテリー残量（満充電でクランプ） |
| `/power/battery_soc_percent` | real | % | 充電率（派生リソース） |
| `/power/battery_rate_w` | real | W | 充放電レート（正 = 充電） |
| `/power/load_w` | real | W | 瞬時消費電力（全負荷合計） |
| `/power/solar_input_w` | real | W | 瞬時発電量（食中は 0） |
| `/power/net_w` | real | W | 電力収支（発電 − 負荷） |
| `/data/ssr_volume_gb` | real | GB | SSR 蓄積データ量 |
| `/data/ssr_usage_percent` | real | % | SSR 使用率（派生リソース） |
| `/data/recording_rate_gb_per_sec` | real | GB/s | 正味記録レート |
| `/satellite/mode` | discrete | — | 動作モード（SAFE/NOMINAL/SCIENCE/DOWNLINK/CHARGING） |
| `/satellite/mode_conflicted` | discrete | — | 同時刻のモード書き込み競合フラグ |
| `/satellite/pointing` | discrete | — | 指向モード（太陽/直下/ターゲット/地上局） |
| `/orbit/in_sunlight` | discrete | — | 日照中フラグ（FDS イベント由来） |
| `/orbit/orbit_number` | discrete | — | 周回番号（FDS イベント由来） |
| `/ground/station_visible` | discrete | — | 地上局可視フラグ（FDS イベント由来） |
| `/ground/visible_station_id` | discrete | — | 可視中の地上局識別子（FDS イベント由来） |

## アクティビティ構成

| アクティビティ | 役割 | 主なパラメータ |
|---|---|---|
| `Slew` | 指向モードの変更（姿勢変更） | `targetPointing`, `slewDuration`, `wheelPowerW` |
| `Observation` | 科学観測（電力↑・SSR↑）。セーフモード中は拒否 | `duration`, `instrumentPowerW`, `dataRateGbPerSec`, `targetId` |
| `Downlink` | 可視ウィンドウを待って送信（電力↑・SSR↓）。SSR が空なら早期終了 | `maxTransmitDuration`, `transmitterPowerW`, `downlinkRateGbPerSec`, `waitForStationVisibility` |
| `SolarCharging` | 太陽指向で充電強化 | `duration`, `additionalPowerW` |
| `SafeModeEntry` | 地上コマンドによる計画的セーフホールド | `holdDuration`, `loadShedW` |
| `TypicalOrbitOps` | 1運用サイクルの複合アクティビティ（スルー→観測→スルー→ダウンリンク→復帰） | `targetId`, `observationDuration`, `downlinkMaxDuration` |

## デーモンタスク（プランと独立に動く自律機能）

| デーモン | 説明 |
|---|---|
| FDS 軌道イベント再生 | 取り込んだ AOS/LOS・UMBRA_ENTRY/EXIT をタイムラインに再生し、可視・日照リソースと太陽電池出力を駆動 |
| 充電制御 | バッテリー満充電到達時に余剰発電をトリムし、容量超過を防止 |
| 電力 FDIR | SoC が 30% を切ると自動でセーフモード（太陽指向 + 負荷シェッド）。60% で復帰 |

## ディレクトリ構成

```
docs/
└── ICD_FDS_ORBIT_EVENTS.md    # FDS→運用計画系インタフェース定義（ICD-FDS-001）
fds/
└── orbit_events_sample.csv    # FDS 成果物のサンプル（チュートリアル用）
src/main/java/gov/nasa/jpl/aerie/tutorial/
├── package-info.java          # @MissionModel アノテーション（Aerie への登録）
├── Mission.java               # トップレベルモデル（FDS 読込 + サブシステム集約 + FDIR）
├── Configuration.java         # シミュレーション設定（FDS ファイルパス/電力/データ/FDIR）
├── fds/
│   ├── OrbitEventType.java    # イベント種別（AOS/LOS/UMBRA_ENTRY/UMBRA_EXIT）
│   ├── OrbitEvent.java        # イベントレコード
│   └── OrbitEventsLoader.java # CSV ローダー + ICD 整合性検証
├── models/
│   ├── SatelliteMode.java     # 動作モード列挙型
│   ├── PointingMode.java      # 指向モード列挙型
│   ├── OrbitModel.java        # FDS イベントのタイムライン再生・リソース公開
│   ├── PowerModel.java        # 電力サブシステム（充電制御デーモン含む）
│   ├── DataModel.java         # データサブシステム（SSR）
│   └── ModeModel.java         # モード・指向管理
└── activities/
    ├── Slew.java              # 姿勢変更
    ├── Observation.java       # 科学観測
    ├── Downlink.java          # 地上局ダウンリンク（可視待ち）
    ├── SolarCharging.java     # 充電強化
    ├── SafeModeEntry.java     # 計画的セーフホールド
    └── TypicalOrbitOps.java   # 1運用サイクル複合アクティビティ
```

---

## セットアップ

### 前提条件

- Java 17+
- Gradle 8+
- GitHub アカウント（Aerie の GitHub Packages を取得するため）

### 1. GitHub Packages の認証設定

```bash
cp gradle.properties.template gradle.properties
# gradle.properties を編集して GitHub ユーザー名と PAT を設定
```

GitHub PAT の作成: <https://github.com/settings/tokens>（必要スコープ: `read:packages`）

### 2. ビルドと Aerie へのアップロード

```bash
gradle build   # または ./gradlew build
# 生成された build/libs/*.jar を Aerie UI（Models ページ）または API でアップロード
```

---

## 運用シナリオ例（1日プランの組み方）

1. **FDS 成果物の準備**: FDS から配付された軌道イベントファイル
   （まずは同梱の `fds/orbit_events_sample.csv`）を Aerie のシミュレーション設定
   `orbitEventsFilePath` に指定する。プラン開始時刻はファイルのカバレッジ内
   （サンプルは 2026-01-01T00:00:00Z 起点）にすること。
2. **プラン作成**: アップロードしたモデルから 24 時間のプランを作る。
3. **観測サイクルの配置**: `TypicalOrbitOps` を地上局パス（サンプルでは 01:40Z から
   6 時間ごと）の少し前に置く。観測でデータを溜め、直後のパスで掃き出す流れが
   タイムラインに展開される。
4. **シミュレーション実行**: 以下を確認する。
   - `/power/battery_soc_percent` — 食のたびに下がり日照で回復するノコギリ波形
   - `/data/ssr_volume_gb` — 観測で増えダウンリンクで減る
   - `/ground/visible_station_id` — パスごとに GS-1/GS-2 が切り替わる
5. **負荷をかけてみる**: 観測時間を伸ばす・`SolarCharging` を削る等で SoC を 30% 以下に
   落とすと、FDIR が自動でセーフモードに入り、以降の `Observation` が拒否される
   のが観察できる。
6. **制約の追加（推奨）**: Aerie の Constraints DSL で
   - `Downlink` 実行中は `/ground/station_visible == true`（ICD 8章に記述例）
   - `SolarCharging` 実行中は `/orbit/in_sunlight == true`
   - `/data/ssr_usage_percent < 100`（SSR 溢れ禁止）
   - `/power/battery_soc_percent > 30`（DOD 制限）
   をチェックすると、プランの成立性を機械的に検証できる。
7. **軌道決定の更新**: FDS がイベントファイルを再配付したら設定を差し替えて
   再シミュレーションする（ICD 6章）。パス時刻のずれによる制約違反が検出される。

---

## Aerie モデリングの学習ポイント

### 外部データの取り込み（FDS 成果物）

軌道イベントは `Path` 型のシミュレーション設定で受け取り、ミッションモデルの
初期化時に読込・検証し、デーモンタスクでタイムラインに再生します。
「外部系の解析結果を取り込んでリソース化し、制約から参照する」パターンの実例です。

### 線形リソースの加算合成

電力・データは `Accumulator`（線形リソース）で表現し、エフェクトは**加算**で合成されます。
アクティビティは開始時に `addLoad(+25)`、終了時に `addLoad(-25)` と逆符号で打ち消します。

```
バッテリーレート = (発電 − 負荷) / 3600   [Wh/s]
```

### 離散リソースの上書き合成と競合検出

モードは `Register`（最後の書き込み優先）。同時刻に複数アクティビティが書き込むと
`/satellite/mode_conflicted` が true になり、プラン上のモード競合を検出できます。

### 条件待ち（waitUntil）

`Downlink` は `waitUntil(stationVisible.is(true))` で可視ウィンドウまで待機します。
FDIR デーモンも `socPercent.isBetween(...)` の条件でしきい値交差を監視します。

### アクティビティ分解（decomposition）

`TypicalOrbitOps` は生成された `ActivityActions.call(...)` で子アクティビティを
逐次実行します。タイムラインには親子付きで展開されます。

### モデルの簡略化（実ミッションとの差分）

- バッテリーの放電下限（0 Wh）はクランプしない — FDIR と制約で守る前提
- `SolarCharging` は食の最中でも効果が出る（日照制約はプランナー責務、制約でチェック）
- FDS ファイルは半影（penumbra）を区別しない（必要なら ICD を改訂してイベント種別を追加）

---

## 今後の拡張ポイント

- **熱モデル**: 機器温度を追加リソースとして追跡し、観測時間に熱制約を加える
- **局別レート**: `visible_station_id` に応じてダウンリンクレートを切り替え
- **スケジューリングゴール**: Aerie Scheduler で「SSR 使用率が 50% を超えたらダウンリンクを自動配置」
- **コマンド展開**: アクティビティからコマンドシーケンス（SeqJSON）への展開

## 参考

- [NASA AMMOS Aerie](https://github.com/NASA-AMMOS/aerie)
- [Aerie ドキュメント](https://nasa-ammos.github.io/aerie-docs/)
- [Mission Modeling ガイド](https://nasa-ammos.github.io/aerie-docs/mission-modeling/introduction/)
- [banananation サンプルモデル](https://github.com/NASA-AMMOS/aerie/tree/develop/examples/banananation)
