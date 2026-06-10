# Plandev Mission Model Tutorial

NASA AMMOS [Aerie](https://github.com/NASA-AMMOS/aerie) 向け衛星運用計画ミッションモデルのチュートリアルリポジトリです。
小型 LEO 衛星を模したモデルで、**観測 → 姿勢変更 → ダウンリンク → 充電管理 → セーフモード**という
衛星運用の一連の流れを Aerie 上でプランニング・シミュレーションできます。

---

## モデルの全体像

```
                       ┌──────────────────────────────┐
 デーモン（自律動作）   │  OrbitModel: 日照⇔食サイクル / 地上局可視ウィンドウ  │
                       │  PowerModel: 満充電クランプ（充電制御）            │
                       │  Mission:    電力 FDIR（SoC低下→自動セーフモード）  │
                       └──────────────┬───────────────┘
                                      │ リソースを駆動
 プラン（アクティビティ） Slew → Observation → Slew → Downlink → Slew …
                                      │ リソースを消費/生成
                       ┌──────────────┴───────────────┐
                       │  電力（バッテリー/負荷/発電）・データ（SSR）・モード  │
                       └──────────────────────────────┘
```

運用検討のポイントになる要素をすべてモデル化しています。

| 検討項目 | モデル上の表現 |
|---|---|
| 食（日陰）での電力収支 | 軌道周期ごとに太陽電池出力が 0 になる |
| 地上局パスの待ち | `Downlink` が可視ウィンドウまで `waitUntil` で待機 |
| 姿勢の競合 | 観測・通信・充電で必要な指向モードが異なり `Slew` が必要 |
| SSR の溜まり過ぎ | 観測で増加・ダウンリンクで減少、使用率を制約でチェック |
| バッテリー保護 | SoC 低下で FDIR が自動セーフモード化、観測コマンドを拒否 |

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
| `/orbit/in_sunlight` | discrete | — | 日照中フラグ |
| `/orbit/orbit_number` | discrete | — | 周回番号 |
| `/ground/station_visible` | discrete | — | 地上局可視フラグ |

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
| 日照⇔食サイクル | 軌道周期（既定 95 分、うち食 35 分）で太陽電池出力を ON/OFF し、周回番号を更新 |
| 地上局可視ウィンドウ | 既定で 6 時間ごとに 8 分間のパスを生成（最初のパスは開始 100 分後） |
| 充電制御 | バッテリー満充電到達時に余剰発電をトリムし、容量超過を防止 |
| 電力 FDIR | SoC が 30% を切ると自動でセーフモード（太陽指向 + 負荷シェッド）。60% で復帰 |

## ディレクトリ構成

```
src/main/java/gov/nasa/jpl/aerie/tutorial/
├── package-info.java          # @MissionModel アノテーション（Aerie への登録）
├── Mission.java               # トップレベルモデル（サブシステム集約 + FDIR デーモン）
├── Configuration.java         # シミュレーション設定（軌道/地上局/電力/データ/FDIR）
├── models/
│   ├── SatelliteMode.java     # 動作モード列挙型
│   ├── PointingMode.java      # 指向モード列挙型
│   ├── OrbitModel.java        # 軌道イベント（日照/食・地上局可視）
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

1. **プラン作成**: アップロードしたモデルから 24 時間のプランを作る。
2. **観測サイクルの配置**: `TypicalOrbitOps` を地上局パス（開始 100 分後から 6 時間ごと）の
   少し前に置く。観測でデータを溜め、直後のパスで掃き出す流れがタイムラインに展開される。
3. **シミュレーション実行**: 以下を確認する。
   - `/power/battery_soc_percent` — 食のたびに下がり日照で回復するノコギリ波形
   - `/data/ssr_volume_gb` — 観測で増えダウンリンクで減る
   - `/satellite/pointing` — スルーのたびに切り替わる
4. **負荷をかけてみる**: 観測時間を伸ばす・`SolarCharging` を削る等で SoC を 30% 以下に
   落とすと、FDIR が自動でセーフモードに入り、以降の `Observation` が拒否される
   （タイムライン上で `/satellite/mode` が SAFE のまま観測が空振りする）のが観察できる。
5. **制約の追加（推奨）**: Aerie の Constraints DSL で
   - `/data/ssr_usage_percent < 100`（SSR 溢れ禁止）
   - `/power/battery_soc_percent > 30`（DOD 制限）
   - `Downlink` 実行中は `/ground/station_visible == true`
   をチェックすると、プランの成立性を機械的に検証できる。

---

## Aerie モデリングの学習ポイント

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

- 軌道・可視ウィンドウは固定周期の簡易モデル（実際は SPICE 等のパス予報で置換）
- バッテリーの放電下限（0 Wh）はクランプしない — FDIR と制約で守る前提
- `SolarCharging` は食の最中でも効果が出る（日照制約はプランナー責務）

---

## 今後の拡張ポイント

- **熱モデル**: 機器温度を追加リソースとして追跡し、観測時間に熱制約を加える
- **複数地上局**: 局ごとの可視ウィンドウとレートの違いをモデル化
- **スケジューリングゴール**: Aerie Scheduler で「SSR 使用率が 50% を超えたらダウンリンクを自動配置」
- **コマンド展開**: アクティビティからコマンドシーケンス（SeqJSON）への展開

## 参考

- [NASA AMMOS Aerie](https://github.com/NASA-AMMOS/aerie)
- [Aerie ドキュメント](https://nasa-ammos.github.io/aerie-docs/)
- [Mission Modeling ガイド](https://nasa-ammos.github.io/aerie-docs/mission-modeling/introduction/)
- [banananation サンプルモデル](https://github.com/NASA-AMMOS/aerie/tree/develop/examples/banananation)
