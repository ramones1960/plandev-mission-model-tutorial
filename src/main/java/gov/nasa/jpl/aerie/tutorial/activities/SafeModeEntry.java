package gov.nasa.jpl.aerie.tutorial.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.tutorial.Mission;
import gov.nasa.jpl.aerie.tutorial.models.SatelliteMode;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

/**
 * セーフモード移行アクティビティ。
 *
 * <p>搭載機器に異常が検知された場合や、バッテリー残量が危険域に達した場合に
 * 非必須負荷を切り離してバッテリー保護を優先する。
 * セーフモード中は太陽電池で充電しながら地上の対処を待つ。
 *
 * <h3>リソースへの影響</h3>
 * <ul>
 *   <li>{@code power/battery_energy_wh} — 節電分だけ放電が緩和（充電に転じる可能性）</li>
 *   <li>{@code power/total_draw_w}       — 節電分が減算</li>
 *   <li>{@code satellite/mode}           — SAFE → NOMINAL に遷移</li>
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

    public void run(final Mission mission) {
        // 非必須負荷をシェッド（消費電力削減 = 負のデルタ）
        mission.mode.setMode(SatelliteMode.SAFE);
        mission.power.emitPowerDelta(-powerShedW);

        delay(safeModeDuration);

        // リカバリ: 負荷を再投入して通常運用に復帰
        mission.power.emitPowerDelta(+powerShedW);
        mission.mode.setMode(SatelliteMode.NOMINAL);
    }
}
