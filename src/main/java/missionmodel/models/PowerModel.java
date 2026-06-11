package missionmodel.models;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources;
import missionmodel.Configuration;

import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.asPolynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.constant;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.scale;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.subtract;

/**
 * 電力サブシステムモデル。
 *
 * <p>総消費電力と太陽電池発電量を離散リソースとして保持し、
 * その差（充放電レート）を時間積分してバッテリー残量を導出する。
 * バッテリー残量は {@code clampedIntegrate} により 0〜総容量の範囲に制限される。
 *
 * <h2>リソース一覧</h2>
 * <ul>
 *   <li>{@code power/battery_energy_wh} — バッテリー残量 [Wh]（線形・0〜容量にクランプ）</li>
 *   <li>{@code power/total_draw_w}      — 瞬時総消費電力 [W]（離散）</li>
 *   <li>{@code power/solar_power_w}     — 太陽電池発電量 [W]（離散）</li>
 * </ul>
 */
public final class PowerModel {

    /** 瞬時総消費電力 [W]。アクティビティが負荷の増減を加える。 */
    public final MutableResource<Discrete<Double>> totalDrawW;

    /** 太陽電池発電量 [W]。充電強化アクティビティで増加する。 */
    public final MutableResource<Discrete<Double>> solarPowerW;

    /** バッテリー残量 [Wh]。(発電 − 消費) の時間積分として導出される。 */
    public final Resource<Polynomial> batteryEnergyWh;

    public PowerModel(final Registrar registrar, final Configuration config) {
        this.totalDrawW = resource(discrete(config.housekeepingPowerW()));
        this.solarPowerW = resource(discrete(config.solarPanelMaxPowerW()));

        // 充放電レート [Wh/s] = (太陽電池出力 [W] − 総消費電力 [W]) / 3600
        final var chargeRateWhPerSec =
            scale(subtract(asPolynomial(solarPowerW), asPolynomial(totalDrawW)), 1.0 / 3_600.0);

        // バッテリー残量はレートの積分。物理的に 0〜総容量を超えないようクランプする。
        this.batteryEnergyWh = PolynomialResources.clampedIntegrate(
            chargeRateWhPerSec,
            constant(0.0),
            constant(config.batteryCapacityWh()),
            config.initialBatteryEnergyWh()
        ).integral();

        registrar.discrete("power/total_draw_w", totalDrawW, new DoubleValueMapper());
        registrar.discrete("power/solar_power_w", solarPowerW, new DoubleValueMapper());
        registrar.real("power/battery_energy_wh", PolynomialResources.assumeLinear(batteryEnergyWh));
    }

    /** 機器の起動などによる消費電力の増加 [W] を反映する。 */
    public void increaseDraw(final double watts) {
        DiscreteEffects.increase(totalDrawW, watts);
    }

    /** 機器の停止・負荷シェッドなどによる消費電力の減少 [W] を反映する。 */
    public void decreaseDraw(final double watts) {
        DiscreteEffects.decrease(totalDrawW, watts);
    }

    /** 姿勢制御などによる太陽電池発電量の増加 [W] を反映する。 */
    public void increaseSolarPower(final double watts) {
        DiscreteEffects.increase(solarPowerW, watts);
    }

    /** 太陽電池発電量の減少 [W] を反映する。 */
    public void decreaseSolarPower(final double watts) {
        DiscreteEffects.decrease(solarPowerW, watts);
    }
}
