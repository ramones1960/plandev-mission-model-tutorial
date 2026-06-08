package gov.nasa.jpl.aerie.tutorial.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.tutorial.Mission;
import gov.nasa.jpl.aerie.tutorial.models.SatelliteMode;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

/**
 * 地上局ダウンリンクアクティビティ。
 *
 * <p>地上局との可視時間内に SSR のデータを送信する。
 * RF 送信機の電力を消費しながら、SSR データ量を減少させる。
 * 実際の運用では地上局可視ウィンドウの制約と組み合わせる。
 *
 * <h3>リソースへの影響</h3>
 * <ul>
 *   <li>{@code power/battery_energy_wh} — RF 送信機分だけ放電が加速</li>
 *   <li>{@code power/total_draw_w}       — 送信機消費電力の加算</li>
 *   <li>{@code data/stored_volume_gb}    — ダウンリンクレートで減少</li>
 *   <li>{@code satellite/mode}           — DOWNLINK → NOMINAL に遷移</li>
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

    public void run(final Mission mission) {
        mission.mode.setMode(SatelliteMode.DOWNLINK);

        // 送信機起動: 電力消費増・データ量減
        mission.power.emitPowerDelta(+transmitterPowerDrawW);
        mission.data.emitDataRateDelta(-downlinkRateGbPerSec);

        delay(duration);

        // 送信機停止: エフェクトを打ち消す
        mission.power.emitPowerDelta(-transmitterPowerDrawW);
        mission.data.emitDataRateDelta(+downlinkRateGbPerSec);

        mission.mode.setMode(SatelliteMode.NOMINAL);
    }
}
