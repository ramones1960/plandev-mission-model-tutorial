package gov.nasa.jpl.aerie.tutorial;

import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Template;

/**
 * シミュレーション開始時に設定できるミッションパラメータ。
 *
 * <p>Aerie UI/API からプランごとにオーバーライドできる。
 * 軌道・地上局・電力・データ・自律運用（FDIR）の5グループに分かれる。
 */
public record Configuration(

    // --- 軌道系 ---
    /** 軌道周期 [min]（LEO 高度約 550 km で約 95 分） */
    double orbitPeriodMinutes,

    /** 1周回あたりの食（地球の影）の継続時間 [min] */
    double eclipseDurationMinutes,

    // --- 地上局系 ---
    /** 地上局可視ウィンドウの発生間隔 [min]（パス開始から次のパス開始まで） */
    double contactIntervalMinutes,

    /** 1回の可視ウィンドウの継続時間 [min] */
    double contactDurationMinutes,

    /** シミュレーション開始から最初の可視ウィンドウまでのオフセット [min] */
    double firstContactOffsetMinutes,

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
             95.0,   // 軌道周期 95 分
             35.0,   // 食 35 分／周回
            360.0,   // 地上局パスは約 6 時間ごと
              8.0,   // 可視ウィンドウ 8 分
            100.0,   // 最初のパスは開始 100 分後
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

    @Export.Validation("orbit period must be positive")
    public boolean validateOrbitPeriod() {
        return orbitPeriodMinutes > 0.0;
    }

    @Export.Validation("eclipse duration must be non-negative and shorter than the orbit period")
    public boolean validateEclipseDuration() {
        return eclipseDurationMinutes >= 0.0 && eclipseDurationMinutes < orbitPeriodMinutes;
    }

    @Export.Validation("contact duration must be positive and shorter than the contact interval")
    public boolean validateContactWindow() {
        return contactDurationMinutes > 0.0 && contactDurationMinutes < contactIntervalMinutes;
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
