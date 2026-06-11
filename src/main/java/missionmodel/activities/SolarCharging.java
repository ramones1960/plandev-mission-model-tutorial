package missionmodel.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Validation;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import missionmodel.Mission;
import missionmodel.models.SatelliteMode;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

/**
 * 太陽電池充電強化アクティビティ。
 *
 * <p>衛星の姿勢を太陽電池パネルが最適な角度になるよう制御し、
 * 充電レートを通常より高める。
 * 観測対象に向けた姿勢制御と競合するためトレードオフが必要。
 *
 * <h2>リソースへの影響</h2>
 * <ul>
 *   <li>{@code power/solar_power_w}  — 追加発電分の加算（バッテリーへの充電が加速）</li>
 *   <li>{@code satellite/mode}       — CHARGING → 開始時のモードに復帰</li>
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

    @Validation("充電強化維持時間は正の値でなければならない")
    @Validation.Subject("duration")
    public boolean validateDuration() {
        return duration.longerThan(Duration.ZERO);
    }

    @Validation("追加発電量は負にできない")
    @Validation.Subject("additionalPowerW")
    public boolean validateAdditionalPower() {
        return additionalPowerW >= 0.0;
    }

    @ActivityType.EffectModel
    public void run(final Mission mission) {
        final SatelliteMode previousMode = mission.mode.currentMode();
        mission.mode.setMode(SatelliteMode.CHARGING);

        // 最適姿勢に遷移して追加発電を得る
        mission.power.increaseSolarPower(additionalPowerW);

        delay(duration);

        // 通常姿勢に戻す
        mission.power.decreaseSolarPower(additionalPowerW);
        mission.mode.setMode(previousMode);
    }
}
