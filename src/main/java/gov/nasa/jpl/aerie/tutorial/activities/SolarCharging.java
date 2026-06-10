package gov.nasa.jpl.aerie.tutorial.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.tutorial.Mission;
import gov.nasa.jpl.aerie.tutorial.models.PointingMode;
import gov.nasa.jpl.aerie.tutorial.models.SatelliteMode;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

/**
 * 太陽電池充電強化アクティビティ。
 *
 * <p>太陽電池パドルが最適角度になるよう姿勢を固定し、充電レートを通常より高める。
 * 観測・通信向けの姿勢と競合するため、プラン上でのトレードオフ検討対象になる。
 *
 * <p>注意: 簡易モデルのため、食の最中に実行してもボーナス発電が加算される。
 * 日照中にスケジュールするのはプランナーの責務であり、
 * {@code /orbit/in_sunlight} を使った制約でチェックできる。
 *
 * <h2>リソースへの影響</h2>
 * <ul>
 *   <li>{@code /power/solar_input_w} — 追加発電分が加算（バッテリー充電が加速）</li>
 *   <li>{@code /satellite/mode}      — CHARGING → NOMINAL に遷移</li>
 *   <li>{@code /satellite/pointing}  — SUN_POINTING に遷移</li>
 * </ul>
 */
@ActivityType("SolarCharging")
public final class SolarCharging {

    /** 充電強化を維持する時間。 */
    @Parameter
    public Duration duration = Duration.of(30, Duration.MINUTES);

    /** 通常姿勢に比べて追加で得られる発電量 [W]。 */
    @Parameter
    public double additionalPowerW = 50.0;

    public SolarCharging() {}

    public SolarCharging(final Duration duration) {
        this.duration = duration;
    }

    @Export.Validation("charging duration must be positive")
    public boolean validateDuration() {
        return this.duration.longerThan(Duration.ZERO);
    }

    @Export.Validation("additional power must be non-negative")
    public boolean validateAdditionalPower() {
        return this.additionalPowerW >= 0.0;
    }

    @EffectModel
    public void run(final Mission mission) {
        mission.mode.set(SatelliteMode.CHARGING);
        mission.mode.setPointing(PointingMode.SUN_POINTING);

        mission.power.addSolarInput(+this.additionalPowerW);
        delay(this.duration);
        mission.power.addSolarInput(-this.additionalPowerW);

        mission.mode.set(SatelliteMode.NOMINAL);
    }
}
