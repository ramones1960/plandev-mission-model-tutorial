package missionmodel;

import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import missionmodel.activities.Downlink;
import missionmodel.activities.Observation;
import missionmodel.activities.SafeModeEntry;
import missionmodel.models.SatelliteMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 観測 → ダウンリンク → セーフモードの一連のシナリオで
 * 電力・データ量・モードの収支を検証するシミュレーションテスト。
 *
 * <p>{@link MerlinExtension} によりテスト本体はシミュレーション内で実行されるため、
 * アクティビティの {@code run()} を直接呼び出してリソースの現在値を検証できる。
 */
@ExtendWith(MerlinExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class MissionScenarioTest {

    private final Mission model;

    public MissionScenarioTest(final Registrar registrar) {
        this.model = new Mission(registrar, Configuration.defaultConfiguration());
    }

    @Test
    public void observeDownlinkSafeModeScenario() {
        // 初期状態: ハウスキーピング 50 W、SSR 空、NOMINAL
        assertEquals(50.0, currentValue(model.power.totalDrawW), 1e-9);
        assertEquals(0.0, currentValue(model.data.storedVolumeGb), 1e-9);
        assertEquals(SatelliteMode.NOMINAL, model.mode.currentMode());

        // --- 観測 10 分: 0.005 GB/s × 600 s = 3 GB 蓄積 ---
        final var observation = new Observation();
        observation.duration = Duration.of(10, Duration.MINUTES);
        observation.run(model);

        assertEquals(3.0, currentValue(model.data.storedVolumeGb), 1e-9);
        // 機器停止後は消費電力がハウスキーピングに戻り、モードも復帰する
        assertEquals(50.0, currentValue(model.power.totalDrawW), 1e-9);
        assertEquals(SatelliteMode.NOMINAL, model.mode.currentMode());
        // バッテリー: (250 − 75) W × 600 s = +29.17 Wh
        assertEquals(3_229.166_667, currentValue(model.power.batteryEnergyWh), 1e-3);

        // --- ダウンリンク 2 分: 3 GB は 60 s で送信完了、残り 60 s は 0 GB でクランプ ---
        final var downlink = new Downlink();
        downlink.duration = Duration.of(2, Duration.MINUTES);
        downlink.run(model);

        assertEquals(0.0, currentValue(model.data.storedVolumeGb), 1e-9);
        assertEquals(50.0, currentValue(model.power.totalDrawW), 1e-9);
        assertEquals(SatelliteMode.NOMINAL, model.mode.currentMode());

        // --- セーフモード 1 時間: 負荷 40 W シェッドで充電が加速 ---
        final var safeMode = new SafeModeEntry();
        safeMode.safeModeDuration = Duration.of(1, Duration.HOURS);
        safeMode.run(model);

        assertEquals(50.0, currentValue(model.power.totalDrawW), 1e-9);
        assertEquals(SatelliteMode.NOMINAL, model.mode.currentMode());
        // 3200 + 29.17 (観測) + (250−80)×120/3600 (DL) + (250−10)×1h (セーフ) = 3474.83 Wh
        assertEquals(3_474.833_333, currentValue(model.power.batteryEnergyWh), 1e-3);
    }
}
