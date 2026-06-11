package missionmodel.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Validation;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import missionmodel.Mission;
import missionmodel.models.SatelliteMode;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

/**
 * 科学観測アクティビティ。
 *
 * <p>観測機器を起動してサイエンスデータを取得する。
 * 実行中は機器分の追加電力を消費し、生成データが SSR に蓄積される。
 *
 * <h2>リソースへの影響</h2>
 * <ul>
 *   <li>{@code power/total_draw_w}              — 機器消費電力の加算（結果としてバッテリー放電が加速）</li>
 *   <li>{@code data/recording_rate_gb_per_sec}  — データ生成レートの加算</li>
 *   <li>{@code satellite/mode}                  — SCIENCE → 開始時のモードに復帰</li>
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

    @Validation("観測継続時間は正の値でなければならない")
    @Validation.Subject("duration")
    public boolean validateDuration() {
        return duration.longerThan(Duration.ZERO);
    }

    @Validation("機器消費電力とデータ生成レートは負にできない")
    @Validation.Subject({"instrumentPowerDrawW", "dataProductionRateGbPerSec"})
    public boolean validateRates() {
        return instrumentPowerDrawW >= 0.0 && dataProductionRateGbPerSec >= 0.0;
    }

    @ActivityType.EffectModel
    public void run(final Mission mission) {
        final SatelliteMode previousMode = mission.mode.currentMode();
        mission.mode.setMode(SatelliteMode.SCIENCE);

        // 機器を起動: 電力消費とデータ生成を開始
        mission.power.increaseDraw(instrumentPowerDrawW);
        mission.data.increaseRecordingRate(dataProductionRateGbPerSec);

        delay(duration);

        // 機器を停止: 開始時のエフェクトを戻す
        mission.data.decreaseRecordingRate(dataProductionRateGbPerSec);
        mission.power.decreaseDraw(instrumentPowerDrawW);

        mission.mode.setMode(previousMode);
    }
}
