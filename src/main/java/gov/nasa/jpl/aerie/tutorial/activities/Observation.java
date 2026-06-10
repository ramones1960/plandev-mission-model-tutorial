package gov.nasa.jpl.aerie.tutorial.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
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
 * <p>セーフモード中に開始された場合、観測は実行されずに即終了する
 * （実衛星でコマンドがオンボードフォールトプロテクションに拒否される動作の簡易版）。
 *
 * <h2>リソースへの影響</h2>
 * <ul>
 *   <li>{@code /power/load_w}      — 観測機器の電力を消費</li>
 *   <li>{@code /data/ssr_volume_gb} — データ生成レートで増加</li>
 *   <li>{@code /satellite/mode}     — SCIENCE → NOMINAL に遷移</li>
 * </ul>
 */
@ActivityType("Observation")
public final class Observation {

    /** 観測継続時間。 */
    @Parameter
    public Duration duration = Duration.of(8, Duration.MINUTES);

    /** 観測機器の追加消費電力 [W]。 */
    @Parameter
    public double instrumentPowerW = 40.0;

    /** サイエンスデータ生成レート [GB/s]（0.01 GB/s = 36 GB/h）。 */
    @Parameter
    public double dataRateGbPerSec = 0.01;

    /** 観測対象の識別子（プランニングビューでの区別用）。 */
    @Parameter
    public String targetId = "TARGET-001";

    public Observation() {}

    public Observation(final String targetId, final Duration duration) {
        this.targetId = targetId;
        this.duration = duration;
    }

    @Export.Validation("observation duration must be positive")
    public boolean validateDuration() {
        return this.duration.longerThan(Duration.ZERO);
    }

    @Export.Validation("instrument power and data rate must be non-negative")
    public boolean validateRates() {
        return this.instrumentPowerW >= 0.0 && this.dataRateGbPerSec >= 0.0;
    }

    @EffectModel
    public void run(final Mission mission) {
        // セーフモード中は観測コマンドを拒否する
        if (mission.mode.isSafe()) return;

        mission.mode.set(SatelliteMode.SCIENCE);

        mission.power.addLoad(+this.instrumentPowerW);
        mission.data.addRecordingRate(+this.dataRateGbPerSec);

        delay(this.duration);

        mission.data.addRecordingRate(-this.dataRateGbPerSec);
        mission.power.addLoad(-this.instrumentPowerW);

        mission.mode.set(SatelliteMode.NOMINAL);
    }
}
