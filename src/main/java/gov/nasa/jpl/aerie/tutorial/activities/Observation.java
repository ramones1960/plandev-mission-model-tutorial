package gov.nasa.jpl.aerie.tutorial.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.tutorial.Mission;
import gov.nasa.jpl.aerie.tutorial.models.SatelliteMode;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

/**
 * 科学観測アクティビティ。
 *
 * <p>観測機器を起動してサイエンスデータを取得する。
 * 実行中は機器分の追加電力を消費し、生成データが SSR に蓄積される。
 *
 * <h3>リソースへの影響</h3>
 * <ul>
 *   <li>{@code power/battery_energy_wh} — 機器消費分だけ放電が加速</li>
 *   <li>{@code power/total_draw_w}       — 機器消費電力の加算</li>
 *   <li>{@code data/stored_volume_gb}    — データ生成レートで増加</li>
 *   <li>{@code satellite/mode}           — SCIENCE → NOMINAL に遷移</li>
 * </ul>
 */
@ActivityType("Observation")
public final class Observation {

    /** 観測継続時間。 */
    @Parameter
    public Duration duration = Duration.of(5, Duration.MINUTES);

    /** ハウスキーピング以外の機器追加消費電力 [W]。 */
    @Parameter
    public double instrumentPowerDrawW = 25.0;

    /** サイエンスデータ生成レート [GB/s]。 */
    @Parameter
    public double dataProductionRateGbPerSec = 0.005; // 約 18 GB/時間

    /** 観測対象の識別子（プランニングビューでの区別用）。 */
    @Parameter
    public String targetId = "TARGET_A";

    public void run(final Mission mission) {
        // モードを SCIENCE に遷移
        mission.mode.setMode(SatelliteMode.SCIENCE);

        // 追加電力消費とデータ生成を開始（加算エフェクト）
        mission.power.emitPowerDelta(+instrumentPowerDrawW);
        mission.data.emitDataRateDelta(+dataProductionRateGbPerSec);

        delay(duration);

        // 加算したエフェクトを逆符号で打ち消す（終了処理）
        mission.power.emitPowerDelta(-instrumentPowerDrawW);
        mission.data.emitDataRateDelta(-dataProductionRateGbPerSec);

        // 通常モードに復帰
        mission.mode.setMode(SatelliteMode.NOMINAL);
    }
}
