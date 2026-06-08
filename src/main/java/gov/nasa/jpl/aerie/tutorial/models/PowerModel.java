package gov.nasa.jpl.aerie.tutorial.models;

import gov.nasa.jpl.aerie.merlin.framework.MutableResource;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;

import static gov.nasa.jpl.aerie.merlin.framework.Resources.emit;
import static gov.nasa.jpl.aerie.merlin.framework.Resources.resource;

/**
 * 電力サブシステムモデル。
 *
 * <p>バッテリー残量を線形リソースとして追跡する。
 * シミュレーション時間の経過とともに現在の充放電レートで自動的に積算される。
 *
 * <h3>リソース一覧</h3>
 * <ul>
 *   <li>{@code power/battery_energy_wh} — バッテリー残量 [Wh]（線形リソース）</li>
 *   <li>{@code power/total_draw_w}      — 瞬時消費電力 [W]（線形リソース）</li>
 * </ul>
 */
public final class PowerModel {

    /** バッテリー残量 [Wh]。アクティビティによる充放電レートの加算で変動する。 */
    public final MutableResource<RealDynamics> batteryEnergyWh;

    /** 瞬時消費電力 [W]。可視化・制約チェック用。 */
    public final MutableResource<RealDynamics> totalDrawW;

    private final double capacityWh;

    public PowerModel(final Registrar registrar,
                      final double initialEnergyWh,
                      final double capacityWh,
                      final double solarPanelMaxPowerW,
                      final double housekeepingPowerW) {
        this.capacityWh = capacityWh;

        // 日照中は太陽電池でハウスキーピング分を賄い、余剰があれば充電。
        // 初期レート: (太陽電池出力 - ハウスキーピング) を Wh/s に換算
        final double baselineRateWhs = (solarPanelMaxPowerW - housekeepingPowerW) / 3_600.0;

        this.batteryEnergyWh = resource(RealDynamics.linear(initialEnergyWh, baselineRateWhs));
        this.totalDrawW       = resource(RealDynamics.linear(housekeepingPowerW, 0.0));

        registrar.real("power/battery_energy_wh", batteryEnergyWh);
        registrar.real("power/total_draw_w",      totalDrawW);
    }

    /**
     * 追加の電力消費/回収を加算する。
     *
     * @param deltaWatts 正 = 追加消費（放電加速）、負 = 消費削減（節電）
     */
    public void emitPowerDelta(final double deltaWatts) {
        // RealDynamics は加算合成: 初期値 0 でレートだけを加算する
        emit(batteryEnergyWh, RealDynamics.linear(0.0, -deltaWatts / 3_600.0));
        emit(totalDrawW,       RealDynamics.linear(0.0,  deltaWatts));
    }

    /** バッテリー残量を総容量で割った充電率 [0.0〜1.0] を返す（スナップショット）。 */
    public double stateOfCharge() {
        return batteryEnergyWh.getDynamics().extract() / capacityWh;
    }
}
