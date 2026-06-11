package gov.nasa.jpl.aerie.tutorial.models;

import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.contrib.models.counters.Counter;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.StringValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.tutorial.fds.OrbitEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

/**
 * 軌道イベントモデル。
 *
 * <p>軌道力学系（FDS）が解析・配付した軌道イベント（地上局可視の AOS/LOS、
 * 食の本影出入り）をシミュレーションタイムラインに再生し、離散リソースとして公開する。
 * <b>可視や食をミッションモデル内で計算することはせず</b>、外部 FDS 成果物
 * （{@code docs/ICD_FDS_ORBIT_EVENTS.md} 準拠の CSV）を唯一の情報源とする。
 *
 * <p>公開リソースはアクティビティの条件待ち（{@code waitUntil}）と
 * Aerie の制約（Constraint）の両方から参照できる。
 *
 * <h2>リソース一覧</h2>
 * <ul>
 *   <li>{@code /orbit/in_sunlight}          — 日照中かどうか（UMBRA_ENTRY/EXIT 由来）</li>
 *   <li>{@code /orbit/orbit_number}         — 周回番号（UMBRA_EXIT ごとにインクリメント）</li>
 *   <li>{@code /ground/station_visible}     — 地上局可視ウィンドウ内かどうか（AOS/LOS 由来）</li>
 *   <li>{@code /ground/visible_station_id}  — 可視中の地上局識別子（不可視時は空文字列）</li>
 * </ul>
 */
public final class OrbitModel {

    /** 日照中なら true。初期値はプラン開始時刻以前の食イベントから導出する。 */
    public final Register<Boolean> inSunlight;

    /** 地上局から可視なら true。ダウンリンクはこのウィンドウ内でのみ有効。 */
    public final Register<Boolean> stationVisible;

    /** 可視中の地上局識別子。不可視のときは空文字列。 */
    public final Register<String> visibleStationId;

    /** 周回番号。食から日照に戻るたびにインクリメントする。 */
    public final Counter<Integer> orbitNumber = Counter.ofInteger(0);

    /** プラン開始以降に再生するイベント（プラン開始からのオフセット付き）。 */
    private final List<TimedEvent> futureEvents;

    /** プラン開始時点で食の最中なら true（発電の初期補正に使う）。 */
    private final boolean initiallyInUmbra;

    private final PowerModel power;
    private final double solarArrayPowerW;

    private record TimedEvent(Duration offset, OrbitEvent event) {}

    /**
     * @param registrar        リソース登録用
     * @param planStart        プラン開始時刻（UTC）。イベント時刻をシミュレーション相対時刻に変換する基準
     * @param events           FDS 軌道イベント（{@code OrbitEventsLoader} で検証済み・時刻昇順）
     * @param solarArrayPowerW 日照中の太陽電池発電量 [W]
     * @param power            食の出入りで発電量を増減させる対象
     */
    public OrbitModel(final Registrar registrar,
                      final Instant planStart,
                      final List<OrbitEvent> events,
                      final double solarArrayPowerW,
                      final PowerModel power) {
        this.power = power;
        this.solarArrayPowerW = solarArrayPowerW;

        // プラン開始以前のイベントを畳み込んで初期状態を求め、以降のイベントを再生対象にする
        var sunlit = true;          // 食イベントの記載がなければ日照と仮定（ICD 5.2）
        var visibleStation = "";    // 可視ウィンドウ外から開始
        this.futureEvents = new ArrayList<>();

        for (final var event : events) {
            if (event.time().isAfter(planStart)) {
                this.futureEvents.add(new TimedEvent(offsetFrom(planStart, event.time()), event));
                continue;
            }
            switch (event.type()) {
                case UMBRA_ENTRY -> sunlit = false;
                case UMBRA_EXIT  -> sunlit = true;
                case AOS         -> visibleStation = event.stationId();
                case LOS         -> visibleStation = "";
            }
        }

        this.initiallyInUmbra = !sunlit;
        this.inSunlight       = Register.forImmutable(sunlit);
        this.stationVisible   = Register.forImmutable(!visibleStation.isEmpty());
        this.visibleStationId = Register.forImmutable(visibleStation);

        registrar.discrete("/orbit/in_sunlight", this.inSunlight, new BooleanValueMapper());
        registrar.discrete("/orbit/orbit_number", this.orbitNumber, new IntegerValueMapper());
        registrar.discrete("/ground/station_visible", this.stationVisible, new BooleanValueMapper());
        registrar.discrete("/ground/visible_station_id", this.visibleStationId, new StringValueMapper());
    }

    private static Duration offsetFrom(final Instant planStart, final Instant time) {
        final var elapsed = java.time.Duration.between(planStart, time);
        return Duration.of(elapsed.getSeconds(), Duration.SECONDS)
            .plus(Duration.of(elapsed.getNano() / 1_000, Duration.MICROSECONDS));
    }

    /**
     * FDS 軌道イベント再生デーモン。
     *
     * <p>Mission のコンストラクタから {@code spawn(orbit::replayOrbitEvents)} で起動する。
     * プラン開始時点で食の最中なら、まず発電の初期値（日照前提）を補正する。
     */
    public void replayOrbitEvents() {
        if (this.initiallyInUmbra) {
            this.power.addSolarInput(-this.solarArrayPowerW);
        }

        var elapsed = Duration.ZERO;
        for (final var timed : this.futureEvents) {
            delay(timed.offset().minus(elapsed));
            elapsed = timed.offset();
            apply(timed.event());
        }
    }

    private void apply(final OrbitEvent event) {
        switch (event.type()) {
            case UMBRA_ENTRY -> {
                this.inSunlight.set(false);
                this.power.addSolarInput(-this.solarArrayPowerW);
            }
            case UMBRA_EXIT -> {
                this.inSunlight.set(true);
                this.power.addSolarInput(+this.solarArrayPowerW);
                this.orbitNumber.add(1);
            }
            case AOS -> {
                this.stationVisible.set(true);
                this.visibleStationId.set(event.stationId());
            }
            case LOS -> {
                this.stationVisible.set(false);
                this.visibleStationId.set("");
            }
        }
    }
}
