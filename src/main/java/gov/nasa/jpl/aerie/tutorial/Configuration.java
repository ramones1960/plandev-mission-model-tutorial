package gov.nasa.jpl.aerie.tutorial;

import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.WithDefaults;

/**
 * シミュレーション開始時に設定できるミッションパラメータ。
 *
 * Aerie UI/API からプランごとにオーバーライド可能。
 */
public record Configuration(

    // --- 電力系 ---
    /** バッテリー初期残量 [Wh] */
    @Parameter double initialBatteryEnergyWh,

    /** バッテリー総容量 [Wh] */
    @Parameter double batteryCapacityWh,

    /** 太陽電池パネル発電量 [W] （日照中の最大値） */
    @Parameter double solarPanelMaxPowerW,

    /** バスの常時消費電力（ハウスキーピング） [W] */
    @Parameter double housekeepingPowerW,

    // --- データ系 ---
    /** ソリッドステートレコーダー初期データ量 [GB] */
    @Parameter double initialDataVolumeGb,

    /** SSR 総容量 [GB] */
    @Parameter double dataStorageCapacityGb

) {
    /** Aerie が使うデフォルト値。 @WithDefaults が必要。 */
    public static @WithDefaults Configuration defaults() {
        return new Configuration(
            3_200.0,   // 80% of 4 kWh — LEO 小型衛星の典型値
            4_000.0,   // 4 kWh バッテリー
              250.0,   // 250 W 太陽電池
               50.0,   // 50 W ハウスキーピング
                0.0,   // ミッション開始時は空
              128.0    // 128 GB SSR
        );
    }
}
