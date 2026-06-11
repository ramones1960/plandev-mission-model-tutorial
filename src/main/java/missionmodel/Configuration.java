package missionmodel;

import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Template;

/**
 * シミュレーション開始時に設定できるミッションパラメータ。
 *
 * <p>PlanDev UI/API からプランごとにオーバーライド可能。
 * 全パラメータにデフォルト値を与えるため {@link Template} 方式を使う。
 *
 * @param initialBatteryEnergyWh バッテリー初期残量 [Wh]
 * @param batteryCapacityWh      バッテリー総容量 [Wh]
 * @param solarPanelMaxPowerW    太陽電池パネル発電量 [W]（日照中の最大値）
 * @param housekeepingPowerW     バスの常時消費電力（ハウスキーピング） [W]
 * @param initialDataVolumeGb    ソリッドステートレコーダー（SSR）初期データ量 [GB]
 * @param dataStorageCapacityGb  SSR 総容量 [GB]
 */
public record Configuration(
    double initialBatteryEnergyWh,
    double batteryCapacityWh,
    double solarPanelMaxPowerW,
    double housekeepingPowerW,
    double initialDataVolumeGb,
    double dataStorageCapacityGb
) {
    @Template
    public static Configuration defaultConfiguration() {
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
