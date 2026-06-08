package gov.nasa.jpl.aerie.tutorial.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.tutorial.Mission;
import gov.nasa.jpl.aerie.tutorial.models.SatelliteMode;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

/**
 * 太陽電池充電強化アクティビティ。
 *
 * <p>衛星の姿勢を太陽電池パネルが最適な角度になるよう制御し、
 * 充電レートを通常より高める。
 * 観測対象に向けた姿勢制御と競合するためトレードオフが必要。
 *
 * <h3>リソースへの影響</h3>
 * <ul>
 *   <li>{@code power/battery_energy_wh} — 追加発電分だけ充電が加速</li>
 *   <li>{@code power/total_draw_w}       — 充電強化で見掛け上の消費が減少</li>
 *   <li>{@code satellite/mode}           — CHARGING → NOMINAL に遷移</li>
 * </ul>
 */
@ActivityType("SolarCharging")
public final class SolarCharging {

    /** 充電強化を維持する時間。 */
    @Parameter
    public Duration duration = Duration.of(30, Duration.MINUTES);

    /**
     * 通常姿勢に比べて追加で得られる発電量 [W]。
     * 正の値 = 追加発電（バッテリーへの充電が加速する）。
     */
    @Parameter
    public double additionalPowerW = 50.0;

    public void run(final Mission mission) {
        mission.mode.setMode(SatelliteMode.CHARGING);

        // 追加発電: 消費電力の「削減」として表現（バッテリーに多く流れる）
        mission.power.emitPowerDelta(-additionalPowerW);

        delay(duration);

        // 通常姿勢に戻す
        mission.power.emitPowerDelta(+additionalPowerW);
        mission.mode.setMode(SatelliteMode.NOMINAL);
    }
}
