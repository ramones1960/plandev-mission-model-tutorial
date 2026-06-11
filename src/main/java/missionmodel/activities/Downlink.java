package missionmodel.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Validation;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import missionmodel.Mission;
import missionmodel.models.SatelliteMode;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

/**
 * 地上局ダウンリンクアクティビティ。
 *
 * <p>地上局との可視時間内に SSR のデータを送信する。
 * RF 送信機の電力を消費しながら、SSR データ量を減少させる。
 * 実際の運用では地上局可視ウィンドウの制約と組み合わせる。
 *
 * <h2>リソースへの影響</h2>
 * <ul>
 *   <li>{@code power/total_draw_w}              — 送信機消費電力の加算（結果としてバッテリー放電が加速）</li>
 *   <li>{@code data/recording_rate_gb_per_sec}  — ダウンリンクレート分の減算（SSR データ量が減少）</li>
 *   <li>{@code satellite/mode}                  — DOWNLINK → 開始時のモードに復帰</li>
 * </ul>
 */
@ActivityType("Downlink")
public final class Downlink {

    /** 地上局との接触継続時間（可視ウィンドウに制約される）。 */
    @Parameter
    public Duration duration = Duration.of(10, Duration.MINUTES);

    /** RF 送信機の追加消費電力 [W]。 */
    @Parameter
    public double transmitterPowerDrawW = 30.0;

    /** ダウンリンクデータレート [GB/s]（X バンドの典型値）。 */
    @Parameter
    public double downlinkRateGbPerSec = 0.05; // 約 180 GB/時間

    /** 使用する地上局の識別子。 */
    @Parameter
    public String groundStationId = "GS_USSC";

    @Validation("接触継続時間は正の値でなければならない")
    @Validation.Subject("duration")
    public boolean validateDuration() {
        return duration.longerThan(Duration.ZERO);
    }

    @Validation("送信機消費電力とダウンリンクレートは負にできない")
    @Validation.Subject({"transmitterPowerDrawW", "downlinkRateGbPerSec"})
    public boolean validateRates() {
        return transmitterPowerDrawW >= 0.0 && downlinkRateGbPerSec >= 0.0;
    }

    @ActivityType.EffectModel
    public void run(final Mission mission) {
        final SatelliteMode previousMode = mission.mode.currentMode();
        mission.mode.setMode(SatelliteMode.DOWNLINK);

        // 送信機起動: 電力消費増・SSR データ量減
        mission.power.increaseDraw(transmitterPowerDrawW);
        mission.data.decreaseRecordingRate(downlinkRateGbPerSec);

        delay(duration);

        // 送信機停止: 開始時のエフェクトを戻す
        mission.data.increaseRecordingRate(downlinkRateGbPerSec);
        mission.power.decreaseDraw(transmitterPowerDrawW);

        mission.mode.setMode(previousMode);
    }
}
