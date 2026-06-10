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
 * セーフモード移行アクティビティ（地上コマンドによる計画的セーフホールド）。
 *
 * <p>搭載機器の異常対処やソフトウェア更新などで、地上から明示的にセーフモードへ
 * 移行させるケースを模擬する。太陽指向に変更し非必須負荷を切り離して、
 * バッテリー保護を優先しながら指定時間ホールドする。
 *
 * <p>バッテリー SoC 低下時の「自動」セーフモード移行は、アクティビティではなく
 * Mission の FDIR デーモンが担う点に注意。
 *
 * <h2>リソースへの影響</h2>
 * <ul>
 *   <li>{@code /power/load_w}      — シェッド分だけ負荷が減少（充電が加速）</li>
 *   <li>{@code /satellite/mode}     — SAFE → NOMINAL に遷移</li>
 *   <li>{@code /satellite/pointing} — SUN_POINTING に遷移</li>
 * </ul>
 */
@ActivityType("SafeModeEntry")
public final class SafeModeEntry {

    /** セーフモードを維持する時間。経過後に NOMINAL に復帰する。 */
    @Parameter
    public Duration holdDuration = Duration.of(2, Duration.HOURS);

    /** 切り離す非必須負荷 [W]。 */
    @Parameter
    public double loadShedW = 30.0;

    @Export.Validation("hold duration must be positive")
    public boolean validateDuration() {
        return this.holdDuration.longerThan(Duration.ZERO);
    }

    @Export.Validation("load shed must be non-negative")
    public boolean validateLoadShed() {
        return this.loadShedW >= 0.0;
    }

    @EffectModel
    public void run(final Mission mission) {
        mission.mode.set(SatelliteMode.SAFE);
        mission.mode.setPointing(PointingMode.SUN_POINTING);
        mission.power.addLoad(-this.loadShedW);

        delay(this.holdDuration);

        mission.power.addLoad(+this.loadShedW);
        mission.mode.set(SatelliteMode.NOMINAL);
    }
}
