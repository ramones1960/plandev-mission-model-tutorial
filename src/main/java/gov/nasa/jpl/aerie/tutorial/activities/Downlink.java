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
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;

/**
 * 地上局ダウンリンクアクティビティ。
 *
 * <p>地上局可視ウィンドウ内に SSR のデータを送信する。
 * デフォルトでは {@code /ground/station_visible} が true になるまで待機してから
 * 送信を開始する（{@code waitUntil} による条件待ちの実例）。
 *
 * <p>送信時間は「最大送信時間」と「SSR を空にするのに必要な時間」の短い方で打ち切られ、
 * SSR が負の値になることはない。
 *
 * <h2>リソースへの影響</h2>
 * <ul>
 *   <li>{@code /power/load_w}       — RF 送信機の電力を消費</li>
 *   <li>{@code /data/ssr_volume_gb}  — ダウンリンクレートで減少</li>
 *   <li>{@code /satellite/mode}      — DOWNLINK → NOMINAL に遷移</li>
 *   <li>{@code /satellite/pointing}  — GROUND_STATION_TRACKING に遷移</li>
 * </ul>
 */
@ActivityType("Downlink")
public final class Downlink {

    /** 最大送信時間。可視ウィンドウの長さに合わせて設定する。 */
    @Parameter
    public Duration maxTransmitDuration = Duration.of(8, Duration.MINUTES);

    /** RF 送信機の追加消費電力 [W]。 */
    @Parameter
    public double transmitterPowerW = 60.0;

    /** ダウンリンクレート [GB/s]（0.0125 GB/s = 100 Mbps）。 */
    @Parameter
    public double downlinkRateGbPerSec = 0.0125;

    /** true なら地上局可視ウィンドウの開始まで待機してから送信する。 */
    @Parameter
    public boolean waitForStationVisibility = true;

    /** 使用する地上局の識別子（プランニングビューでの区別用）。 */
    @Parameter
    public String groundStationId = "GS-1";

    public Downlink() {}

    public Downlink(final Duration maxTransmitDuration) {
        this.maxTransmitDuration = maxTransmitDuration;
    }

    @Export.Validation("max transmit duration must be positive")
    public boolean validateDuration() {
        return this.maxTransmitDuration.longerThan(Duration.ZERO);
    }

    @Export.Validation("transmitter power and downlink rate must be positive")
    public boolean validateRates() {
        return this.transmitterPowerW >= 0.0 && this.downlinkRateGbPerSec > 0.0;
    }

    @EffectModel
    public void run(final Mission mission) {
        // セーフモード中はダウンリンクコマンドを拒否する
        if (mission.mode.isSafe()) return;

        if (this.waitForStationVisibility) {
            waitUntil(mission.orbit.stationVisible.is(true));
        }

        mission.mode.set(SatelliteMode.DOWNLINK);
        mission.mode.setPointing(PointingMode.GROUND_STATION_TRACKING);

        // SSR を空にするのに必要な時間と最大送信時間の短い方だけ送信する
        final double storedGb = mission.data.getVolumeGb();
        final double maxSeconds = this.maxTransmitDuration.ratioOver(Duration.SECONDS);
        final double transmitSeconds = Math.min(maxSeconds, storedGb / this.downlinkRateGbPerSec);

        if (transmitSeconds > 0.0) {
            mission.power.addLoad(+this.transmitterPowerW);
            mission.data.addRecordingRate(-this.downlinkRateGbPerSec);

            delay(Duration.roundNearest(transmitSeconds, Duration.SECONDS));

            mission.data.addRecordingRate(+this.downlinkRateGbPerSec);
            mission.power.addLoad(-this.transmitterPowerW);
        }

        mission.mode.set(SatelliteMode.NOMINAL);
    }
}
