package missionmodel.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Validation;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import missionmodel.Mission;
import missionmodel.models.SatelliteMode;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

/**
 * セーフモード移行アクティビティ。
 *
 * <p>搭載機器に異常が検知された場合や、バッテリー残量が危険域に達した場合に
 * 非必須負荷を切り離してバッテリー保護を優先する。
 * セーフモード中は太陽電池で充電しながら地上の対処を待ち、
 * 対処完了後は NOMINAL に復帰する。
 *
 * <h2>リソースへの影響</h2>
 * <ul>
 *   <li>{@code power/total_draw_w}  — 節電分の減算（放電が緩和され、充電に転じる可能性）</li>
 *   <li>{@code satellite/mode}      — SAFE → NOMINAL に遷移</li>
 * </ul>
 */
@ActivityType("SafeModeEntry")
public final class SafeModeEntry {

    /** セーフモードを維持する時間。地上対処後に NOMINAL に戻る。 */
    @Parameter
    public Duration safeModeDuration = Duration.of(2, Duration.HOURS);

    /**
     * 非必須負荷をシェッドすることで削減できる電力 [W]。
     * 正の値 = 削減量（消費電力が減り充電レートが上がる）。
     */
    @Parameter
    public double powerShedW = 40.0;

    @Validation("セーフモード維持時間は正の値でなければならない")
    @Validation.Subject("safeModeDuration")
    public boolean validateDuration() {
        return safeModeDuration.longerThan(Duration.ZERO);
    }

    @Validation("シェッド電力は負にできない")
    @Validation.Subject("powerShedW")
    public boolean validatePowerShed() {
        return powerShedW >= 0.0;
    }

    @ActivityType.EffectModel
    public void run(final Mission mission) {
        // 非必須負荷をシェッドしてバッテリー保護を優先
        mission.mode.setMode(SatelliteMode.SAFE);
        mission.power.decreaseDraw(powerShedW);

        delay(safeModeDuration);

        // リカバリ: 負荷を再投入して通常運用に復帰
        mission.power.increaseDraw(powerShedW);
        mission.mode.setMode(SatelliteMode.NOMINAL);
    }
}
