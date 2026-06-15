package gov.nasa.jpl.aerie.tutorial;

import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Template;

import java.nio.file.Path;

/**
 * シミュレーション開始時に設定できるミッションパラメータ。
 *
 * <p>Aerie UI/API からプランごとにオーバーライドできる。
 *
 * <p>軌道イベント（地上局可視・食）はモデル内で計算せず、外部の軌道力学系（FDS）が
 * 解析した成果物ファイルを {@link #orbitEventsFilePath} で取り込む。
 * ファイル仕様は {@code docs/ICD_FDS_ORBIT_EVENTS.md} を参照。
 */
public record Configuration(

    // --- FDS インタフェース ---
    /** FDS 軌道イベントファイル（ICD_FDS_ORBIT_EVENTS 準拠の CSV）のパス */
    Path orbitEventsFilePath,

    // --- 電力系 ---
    /** バッテリー総容量 [Wh] */
    double batteryCapacityWh,

    /** バッテリー初期残量 [Wh] */
    double initialBatteryEnergyWh,

    /** 日照中の太陽電池パドル発電量 [W] */
    double solarArrayPowerW,

    /** バスの常時消費電力（ハウスキーピング） [W] */
    double housekeepingPowerW,

    // --- データ系 ---
    /** ソリッドステートレコーダー（SSR）総容量 [GB] */
    double ssrCapacityGb,

    /** SSR 初期データ量 [GB] */
    double initialSsrVolumeGb,

    // --- 自律運用（FDIR）系 ---
    /** この SoC [%] を下回ると自動でセーフモードに入る */
    double lowPowerSocPercent,

    /** セーフモード中、この SoC [%] まで回復したら通常モードに復帰する */
    double recoverySocPercent,

    /** セーフモード時に切り離す非必須負荷 [W] */
    double safeModeLoadShedW

) {
    /** Aerie がプラン作成時に使うデフォルト構成。 */
    public static @Template Configuration defaultConfiguration() {
        return new Configuration(
            Path.of("fds/orbit_events_sample.csv"),  // リポジトリ同梱のサンプル（実運用では FDS 配付物に差し替え）
          4_000.0,   // 4 kWh バッテリー
          3_200.0,   // 初期 SoC 80%
            250.0,   // 250 W 太陽電池
             50.0,   // 50 W ハウスキーピング
            128.0,   // 128 GB SSR
              0.0,   // ミッション開始時は空
             30.0,   // SoC 30% でセーフモード移行
             60.0,   // SoC 60% で復帰
             30.0    // セーフモードで 30 W シェッド
        );
    }

    @Export.Validation("FDS orbit events file must exist (see docs/ICD_FDS_ORBIT_EVENTS.md)")
    public boolean validateOrbitEventsFile() {
        return orbitEventsFilePath.toFile().exists();
    }

    @Export.Validation("battery capacity must be positive")
    public boolean validateBatteryCapacity() {
        return batteryCapacityWh > 0.0;
    }

    @Export.Validation("initial battery energy must be within [0, capacity]")
    public boolean validateInitialBatteryEnergy() {
        return initialBatteryEnergyWh >= 0.0 && initialBatteryEnergyWh <= batteryCapacityWh;
    }

    @Export.Validation("SSR capacity must be positive and initial volume within [0, capacity]")
    public boolean validateSsr() {
        return ssrCapacityGb > 0.0
            && initialSsrVolumeGb >= 0.0
            && initialSsrVolumeGb <= ssrCapacityGb;
    }

    @Export.Validation("FDIR thresholds must satisfy 0 <= low < recovery <= 100")
    public boolean validateFdirThresholds() {
        return lowPowerSocPercent >= 0.0
            && lowPowerSocPercent < recoverySocPercent
            && recoverySocPercent <= 100.0;
    }
}
