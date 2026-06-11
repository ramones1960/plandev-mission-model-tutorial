# Plandev Mission Model Tutorial

NASA AMMOS [PlanDev](https://github.com/NASA-AMMOS/plandev)（旧 [Aerie](https://github.com/NASA-AMMOS/aerie)）向け衛星運用計画ミッションモデルのチュートリアルリポジトリです。
**リソース管理**（電力・データ量・動作モード）に焦点を当てた小型 LEO 衛星を模したモデルを実装しています。

実装は公式の [aerie-modeling-tutorial](https://github.com/NASA-AMMOS/aerie-modeling-tutorial) と同じく
`contrib` の **streamline ライブラリ**（`gov.nasa.jpl.aerie.contrib.streamline`）の流儀に従っています。

---

## リソース構成

| リソース名 | 型 | 単位 | 説明 |
|---|---|---|---|
| `power/battery_energy_wh` | 線形（クランプ付き積分） | Wh | バッテリー残量（0〜総容量に制限） |
| `power/total_draw_w` | 離散（Discrete） | W | 瞬時総消費電力 |
| `power/solar_power_w` | 離散（Discrete） | W | 太陽電池発電量 |
| `data/stored_volume_gb` | 線形（クランプ付き積分） | GB | SSR 蓄積データ量（0〜総容量に制限） |
| `data/recording_rate_gb_per_sec` | 離散（Discrete） | GB/s | 正味記録レート |
| `satellite/mode` | 離散（Discrete） | — | 動作モード（列挙型） |

## アクティビティ構成

| アクティビティ | 主なパラメータ | 影響リソース |
|---|---|---|
| `Observation` | `duration`, `instrumentPowerDrawW`, `dataProductionRateGbPerSec` | 消費電力↑ / データ↑ / モード→SCIENCE |
| `Downlink` | `duration`, `transmitterPowerDrawW`, `downlinkRateGbPerSec` | 消費電力↑ / データ↓ / モード→DOWNLINK |
| `SafeModeEntry` | `safeModeDuration`, `powerShedW` | 消費電力↓ / モード→SAFE |
| `SolarCharging` | `duration`, `additionalPowerW` | 発電量↑（充電加速） / モード→CHARGING |

各アクティビティには `@Validation` によるパラメータ検証（継続時間が正であること、レート類が非負であること）が付いています。

## ディレクトリ構成

```
src/main/java/missionmodel/
├── package-info.java          # @MissionModel アノテーション（PlanDev への登録）
├── Mission.java               # トップレベルモデル（サブシステムを集約）
├── Configuration.java         # シミュレーション設定パラメータ（@Template でデフォルト値を定義）
├── models/
│   ├── SatelliteMode.java     # 動作モード列挙型
│   ├── PowerModel.java        # 電力サブシステム
│   ├── DataModel.java         # データサブシステム
│   └── ModeModel.java         # モード管理
└── activities/
    ├── Observation.java       # 科学観測
    ├── Downlink.java          # 地上局ダウンリンク
    ├── SafeModeEntry.java     # セーフモード移行
    └── SolarCharging.java     # 太陽電池充電強化
```

---

## セットアップ

### 前提条件

- Java 21+（Aerie 2.8.0 以降のミッションモデルは Java 21 が必要）
- GitHub アカウント（Aerie の GitHub Packages を取得するため）

Gradle はリポジトリ同梱の wrapper（`./gradlew`）を使うためインストール不要です。

### 1. GitHub Packages の認証設定

```bash
cp gradle.properties.template gradle.properties
# gradle.properties を編集して GitHub ユーザー名と PAT を設定
```

GitHub PAT の作成: <https://github.com/settings/tokens>
必要なスコープ: `read:packages`

### 2. ビルド

```bash
./gradlew build
```

### 3. PlanDev へのアップロード

```bash
./gradlew jar
# 生成された build/libs/*.jar を PlanDev UI または API でアップロード
```

`jar` タスクは依存ライブラリ（`contrib` など）を同梱した fat jar を生成します
（公式 [aerie-mission-model-template](https://github.com/NASA-AMMOS/aerie-mission-model-template) と同じ方式）。

---

## モデリングの基本概念

### レートを離散リソースで持ち、量を積分で導出する

streamline 流儀では「瞬時レート」を `MutableResource<Discrete<Double>>` として持ち、
「累積量」はその時間積分として導出します。アクティビティはレートに対して
`DiscreteEffects.increase` / `decrease` で増減を加え、終了時に逆操作で戻します。

```
バッテリー残量 [Wh] = ∫ (太陽電池発電量 − 総消費電力) / 3600 dt
SSR データ量 [GB]   = ∫ 正味記録レート dt
```

### クランプ付き積分（clampedIntegrate）

バッテリー残量と SSR データ量は `PolynomialResources.clampedIntegrate` により
物理的な範囲（0〜総容量）に制限されます。満充電を超えて充電されたり、
空の SSR からさらにダウンリンクしてデータ量が負になったりすることはありません。

### 離散リソースのエフェクト競合

`satellite/mode` のような離散リソースに対して、複数のアクティビティが**同時刻**に
`DiscreteEffects.set` を行うとエフェクトが競合し、`Registrar.ErrorBehavior` の設定
（本モデルでは `Log`）に従ってエラーとして記録されます。「最後の書き込みが勝つ」わけでは
ない点に注意してください。モードを設定するアクティビティ同士は重ならないように計画します。

各アクティビティは開始時のモードを記憶し、終了時にそのモードへ復帰します
（`SafeModeEntry` のみ、リカバリ完了として常に `NOMINAL` へ復帰します）。

### ミッションモデルのライフサイクル

```
PlanDev シミュレーター
  │
  ├─ 1. Mission(registrar, config) を呼び出しモデルを初期化
  ├─ 2. プラン上の各アクティビティの @ActivityType.EffectModel メソッド run(mission) を実行
  ├─ 3. 各時刻でリソース値を計算・記録
  └─ 4. SimulationResults として結果を返す
```

---

## 今後の拡張ポイント

- **シミュレーションテスト**: `merlin-framework-junit` を使ったアクティビティ単体のリソース収支検証
- **地上局可視ウィンドウ制約**: `Downlink` に可視時間のスケジュール制約を追加
- **バッテリー DOD 制約**: 放電深度 (Depth of Discharge) の上限制約
- **熱モデル**: 機器温度を追加リソースとして追跡
- **コマンドシーケンス**: アクティビティ内でサブタスクを `spawn` で並列実行

---

## 参考

- [NASA AMMOS PlanDev](https://github.com/NASA-AMMOS/plandev)
- [PlanDev ドキュメント](https://nasa-ammos.github.io/plandev-docs/)
- [公式モデリングチュートリアル (aerie-modeling-tutorial)](https://github.com/NASA-AMMOS/aerie-modeling-tutorial)
- [ミッションモデルテンプレート (aerie-mission-model-template)](https://github.com/NASA-AMMOS/aerie-mission-model-template)
