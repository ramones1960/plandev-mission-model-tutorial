package missionmodel.models;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects;

import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;

/**
 * 衛星動作モードモデル。
 *
 * <p>現在の動作モードを離散リソースとして追跡する。
 * PlanDev タイムラインビューではモード遷移がステップ関数として可視化される。
 *
 * <p>注意: 複数のアクティビティが同時刻にモードを設定するとエフェクトが競合し、
 * {@code Registrar.ErrorBehavior} に従ってエラーとして記録される
 * （「最後の書き込みが勝つ」のではない）。モードを設定するアクティビティ同士は
 * 重ならないように計画するのがこのモデルの前提である。
 *
 * <h2>リソース一覧</h2>
 * <ul>
 *   <li>{@code satellite/mode} — 現在の動作モード（離散リソース）</li>
 * </ul>
 */
public final class ModeModel {

    /** 現在の衛星動作モード。 */
    public final MutableResource<Discrete<SatelliteMode>> mode;

    public ModeModel(final Registrar registrar) {
        this.mode = resource(discrete(SatelliteMode.NOMINAL));

        registrar.discrete("satellite/mode", mode, new EnumValueMapper<>(SatelliteMode.class));
    }

    /**
     * 衛星モードを遷移させる。
     *
     * @param newMode 遷移先モード
     */
    public void setMode(final SatelliteMode newMode) {
        DiscreteEffects.set(mode, newMode);
    }

    /** 現在のモードを返す（アクティビティが終了時に元のモードへ復帰するために使う）。 */
    public SatelliteMode currentMode() {
        return currentValue(mode);
    }
}
