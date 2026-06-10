package gov.nasa.jpl.aerie.tutorial.models;

import gov.nasa.jpl.aerie.contrib.models.Accumulator;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.tutorial.Configuration;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;

/**
 * 電力サブシステム（EPS）モデル。
 *
 * <p>バッテリー残量を線形リソース（{@link Accumulator}）として追跡する。
 * すべての電力変化は {@link #addLoad} / {@link #addSolarInput} を通じて行うこと。
 * 両メソッドは瞬時電力リソースとバッテリー充放電レートを同時に更新し、整合を保つ。
 *
 * <h2>リソース一覧</h2>
 * <ul>
 *   <li>{@code /power/battery_energy_wh}  — バッテリー残量 [Wh]</li>
 *   <li>{@code /power/battery_soc_percent} — 充電率 [%]（派生リソース）</li>
 *   <li>{@code /power/battery_rate_w}      — バッテリー充放電レート [W]（正 = 充電）</li>
 *   <li>{@code /power/load_w}              — 瞬時消費電力（負荷合計） [W]</li>
 *   <li>{@code /power/solar_input_w}       — 瞬時発電量 [W]</li>
 *   <li>{@code /power/net_w}               — 電力収支（発電 − 負荷） [W]</li>
 * </ul>
 */
public final class PowerModel {

    /** 条件式で「無限大」として使う番兵値。 */
    private static final double HUGE = 1.0e30;

    /** 満充電クランプ解除のヒステリシス幅 [Wh]。 */
    private static final double FULL_EPSILON_WH = 0.01;

    /** 1 時間の秒数。W ⇔ Wh/s の換算に使う。 */
    private static final double SECONDS_PER_HOUR = 3_600.0;

    /** バッテリー残量 [Wh]。負荷・発電の変化に応じてレートが変わる。 */
    public final Accumulator batteryEnergyWh;

    /** 瞬時消費電力（全負荷の合計） [W]。Accumulator の volume を瞬時値として使う。 */
    public final Accumulator loadPowerW;

    /** 瞬時発電量 [W]。日照中は太陽電池出力、食中は 0。 */
    public final Accumulator solarInputW;

    /** バッテリー充電率 [%]（派生リソース）。FDIR の条件監視にも使う。 */
    public final RealResource socPercent;

    private final double capacityWh;

    public PowerModel(final Registrar registrar, final Configuration config) {
        this.capacityWh = config.batteryCapacityWh();

        // シミュレーション開始時は日照中・ハウスキーピング負荷のみと仮定する
        final double initialNetW = config.solarArrayPowerW() - config.housekeepingPowerW();

        this.batteryEnergyWh = new Accumulator(
            config.initialBatteryEnergyWh(),
            initialNetW / SECONDS_PER_HOUR);
        this.loadPowerW  = new Accumulator(config.housekeepingPowerW(), 0.0);
        this.solarInputW = new Accumulator(config.solarArrayPowerW(), 0.0);

        this.socPercent = this.batteryEnergyWh.scaledBy(100.0 / capacityWh);

        registrar.real("/power/battery_energy_wh",  this.batteryEnergyWh);
        registrar.real("/power/battery_soc_percent", this.socPercent);
        registrar.real("/power/battery_rate_w",     this.batteryEnergyWh.rate.scaledBy(SECONDS_PER_HOUR));
        registrar.real("/power/load_w",             this.loadPowerW);
        registrar.real("/power/solar_input_w",      this.solarInputW);
        registrar.real("/power/net_w",              this.solarInputW.minus(this.loadPowerW));
    }

    /**
     * 負荷の増減を加算する。
     *
     * <p>アクティビティは開始時に正の値、終了時に同じ大きさの負の値を渡して
     * エフェクトを打ち消すこと（加算合成）。
     *
     * @param watts 正 = 負荷追加（放電加速）、負 = 負荷削減
     */
    public void addLoad(final double watts) {
        this.loadPowerW.add(watts);
        this.batteryEnergyWh.rate.add(-watts / SECONDS_PER_HOUR);
    }

    /**
     * 発電量の増減を加算する。
     *
     * <p>軌道モデルが日照⇔食の遷移で太陽電池出力を、
     * 充電強化アクティビティがボーナス発電をそれぞれ加算する。
     *
     * @param watts 正 = 発電追加（充電加速）、負 = 発電喪失（食への突入など）
     */
    public void addSolarInput(final double watts) {
        this.solarInputW.add(watts);
        this.batteryEnergyWh.rate.add(watts / SECONDS_PER_HOUR);
    }

    /** 現在の充電率 [%] のスナップショットを返す。 */
    public double getSocPercent() {
        return this.batteryEnergyWh.get() * 100.0 / capacityWh;
    }

    /**
     * 充電制御デーモン（バッテリーチャージレギュレーター）。
     *
     * <p>バッテリーが満充電に達したら余剰発電分のレートをトリムして容量超過を防ぐ。
     * 負荷増加や食への突入で放電に転じたらトリムを解除して通常の収支計算に戻す。
     * Mission のコンストラクタから {@code spawn(power::runChargeController)} で起動する。
     */
    public void runChargeController() {
        // 「満充電バンドから外れた」を表す条件:
        // 放電で割り込んだ場合（下側）と、トリム後の負荷減少で再び余剰が出た場合（上側）の両方を拾う
        final Condition leftFullBand = Condition.or(
            this.batteryEnergyWh.isBetween(-HUGE, capacityWh - FULL_EPSILON_WH),
            this.batteryEnergyWh.isBetween(capacityWh + FULL_EPSILON_WH, HUGE));

        while (true) {
            // 満充電到達を待つ
            waitUntil(this.batteryEnergyWh.isBetween(capacityWh, HUGE));

            // 容量を超えた分があれば切り捨てて満充電値にスナップする
            final double overshoot = this.batteryEnergyWh.get() - capacityWh;
            if (overshoot > 0.0) this.batteryEnergyWh.subtract(overshoot);

            // 余剰発電分をトリムし、バッテリーレートを 0 に保持する
            final double trimW = this.solarInputW.get() - this.loadPowerW.get();
            if (trimW > 0.0) {
                this.batteryEnergyWh.rate.add(-trimW / SECONDS_PER_HOUR);
                // 電力収支が変化して満充電バンドから外れるまで保持
                waitUntil(leftFullBand);
                this.batteryEnergyWh.rate.add(+trimW / SECONDS_PER_HOUR);
            } else {
                // すでに放電中: 満充電バンドから外れるまで待ってから次のサイクルへ
                waitUntil(leftFullBand);
            }
        }
    }
}
