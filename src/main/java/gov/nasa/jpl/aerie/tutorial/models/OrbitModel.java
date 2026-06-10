package gov.nasa.jpl.aerie.tutorial.models;

import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.contrib.models.counters.Counter;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.tutorial.Configuration;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

/**
 * 軌道イベントモデル。
 *
 * <p>LEO 衛星の日照⇔食サイクルと地上局可視ウィンドウを、周期的なデーモンタスクとして
 * 模擬する（実ミッションでは SPICE などの軌道計算で外部から与える部分の簡易版）。
 *
 * <p>デーモンは Mission のコンストラクタから {@code spawn(...)} で起動する。
 *
 * <h2>リソース一覧</h2>
 * <ul>
 *   <li>{@code /orbit/in_sunlight}   — 日照中かどうか</li>
 *   <li>{@code /orbit/orbit_number}  — 周回番号（食明けでインクリメント）</li>
 *   <li>{@code /ground/station_visible} — 地上局可視ウィンドウ内かどうか</li>
 * </ul>
 */
public final class OrbitModel {

    /** 日照中なら true。シミュレーションは日照状態から始まる。 */
    public final Register<Boolean> inSunlight = Register.forImmutable(true);

    /** 地上局から可視なら true。ダウンリンクはこのウィンドウ内でのみ有効。 */
    public final Register<Boolean> stationVisible = Register.forImmutable(false);

    /** 周回番号。食から日照に戻るたびにインクリメントする。 */
    public final Counter<Integer> orbitNumber = Counter.ofInteger(0);

    private final Duration sunlitDuration;
    private final Duration eclipseDuration;
    private final Duration contactDuration;
    private final Duration contactGap;
    private final Duration firstContactOffset;
    private final double solarArrayPowerW;
    private final PowerModel power;

    public OrbitModel(final Registrar registrar, final Configuration config, final PowerModel power) {
        this.power = power;
        this.solarArrayPowerW = config.solarArrayPowerW();

        final var orbitPeriod = minutes(config.orbitPeriodMinutes());
        this.eclipseDuration = minutes(config.eclipseDurationMinutes());
        this.sunlitDuration = orbitPeriod.minus(this.eclipseDuration);

        this.contactDuration = minutes(config.contactDurationMinutes());
        this.contactGap = minutes(config.contactIntervalMinutes()).minus(this.contactDuration);
        this.firstContactOffset = minutes(config.firstContactOffsetMinutes());

        registrar.discrete("/orbit/in_sunlight", this.inSunlight, new BooleanValueMapper());
        registrar.discrete("/orbit/orbit_number", this.orbitNumber, new IntegerValueMapper());
        registrar.discrete("/ground/station_visible", this.stationVisible, new BooleanValueMapper());
    }

    private static Duration minutes(final double quantity) {
        return Duration.roundNearest(quantity, Duration.MINUTE);
    }

    /**
     * 日照⇔食サイクルデーモン。
     *
     * <p>食に入ると太陽電池発電を喪失し、日照に戻ると回復する。
     * 電力収支は {@link PowerModel#addSolarInput} を通じて反映される。
     */
    public void runSunlightCycle() {
        while (true) {
            delay(this.sunlitDuration);

            // 食に突入: 発電喪失
            this.inSunlight.set(false);
            this.power.addSolarInput(-this.solarArrayPowerW);

            delay(this.eclipseDuration);

            // 食から脱出: 発電回復・周回番号を更新
            this.inSunlight.set(true);
            this.power.addSolarInput(+this.solarArrayPowerW);
            this.orbitNumber.add(1);
        }
    }

    /**
     * 地上局可視ウィンドウデーモン。
     *
     * <p>一定間隔で可視ウィンドウを開閉する簡易モデル。
     * 実ミッションでは局ごとのパス予報（AOS/LOS）で置き換える。
     */
    public void runGroundStationCycle() {
        delay(this.firstContactOffset);
        while (true) {
            this.stationVisible.set(true);
            delay(this.contactDuration);
            this.stationVisible.set(false);
            delay(this.contactGap);
        }
    }
}
