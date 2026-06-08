# Plandev Mission Model Tutorial

NASA AMMOS [Aerie](https://github.com/NASA-AMMOS/aerie) 向け衛星運用計画ミッションモデルのチュートリアルリポジトリです。  
**リソース管理**（電力・データ量・動作モード）に焦点を当てた小型 LEO 衛星を模したモデルを実装しています。

---

## リソース構成

| リソース名 | 型 | 単位 | 説明 |
|---|---|---|---|
| `power/battery_energy_wh` | 線形（Real） | Wh | バッテリー残量 |
| `power/total_draw_w` | 線形（Real） | W | 瞬時消費電力 |
| `data/stored_volume_gb` | 線形（Real） | GB | SSR 蓄積データ量 |
| `satellite/mode` | 離散（Discrete） | — | 動作モード（列挙型） |

## アクティビティ構成

| アクティビティ | 主なパラメータ | 影響リソース |
|---|---|---|
| `Observation` | `duration`, `instrumentPowerDrawW`, `dataProductionRateGbPerSec` | 電力↑ / データ↑ / モード→SCIENCE |
| `Downlink` | `duration`, `transmitterPowerDrawW`, `downlinkRateGbPerSec` | 電力↑ / データ↓ / モード→DOWNLINK |
| `SafeModeEntry` | `safeModeDuration`, `powerShedW` | 電力↓ / モード→SAFE |
| `SolarCharging` | `duration`, `additionalPowerW` | 電力↑（充電） / モード→CHARGING |

## ディレクトリ構成

```
src/main/java/gov/nasa/jpl/aerie/tutorial/
├── package-info.java          # @MissionModel アノテーション（Aerie への登録）
├── Mission.java               # トップレベルモデル（サブシステムを集約）
├── Configuration.java         # シミュレーション設定パラメータ
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

- Java 17+
- Gradle 8+
- GitHub アカウント（Aerie の GitHub Packages を取得するため）

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

### 3. Aerie へのアップロード

```bash
# JAR を生成して Aerie にアップロード
./gradlew jar
# 生成された build/libs/*.jar を Aerie UI または API でアップロード
```

---

## Aerie の基本概念

### 線形リソースの合成（加算モデル）

Aerie の線形リソースはエフェクトを**加算**で合成します。  
アクティビティが `emitPowerDelta(+25.0)` を呼ぶと、現在の充放電レートに `-25W` の放電が追加されます。  
アクティビティ終了時に `emitPowerDelta(-25.0)` を呼ぶことでキャンセルします。

```
バッテリーレート = ベースライン（太陽電池 - HK） + Σ(アクティビティからの追加デルタ)
```

### 離散リソースの合成（上書きモデル）

動作モードは**上書き (override)** 合成です。同時刻に複数のアクティビティがモードを変更した場合、最後に実行されたものが有効になります。

### ミッションモデルのライフサイクル

```
Aerie シミュレーター
  │
  ├─ 1. Mission(registrar, config) を呼び出しモデルを初期化
  ├─ 2. アクティビティを時系列順に run(mission) で実行
  ├─ 3. 各時刻でリソース値を計算・記録
  └─ 4. SimulationResults として結果を返す
```

---

## 今後の拡張ポイント

- **地上局可視ウィンドウ制約**: `Downlink` に可視時間のスケジュール制約を追加
- **バッテリー DOD 制約**: 放電深度 (Depth of Discharge) の上限制約
- **熱モデル**: 機器温度を追加リソースとして追跡
- **コマンドシーケンス**: アクティビティ内でサブタスクを `spawn` で並列実行

---

## 参考

- [NASA AMMOS Aerie](https://github.com/NASA-AMMOS/aerie)
- [Aerie ドキュメント](https://nasa-ammos.github.io/aerie-docs/)
- [Merlin Framework](https://github.com/NASA-AMMOS/aerie/tree/develop/merlin-framework)
