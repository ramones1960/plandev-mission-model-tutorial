package gov.nasa.jpl.aerie.tutorial.models;

import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;

/**
 * 衛星の動作モード・指向モード管理。
 *
 * <p>{@link Register} は「最後の書き込み優先」で合成される離散リソース。
 * 同時刻に複数のアクティビティが書き込んだ場合は conflicted フラグが立つので、
 * モード競合の検出に使える。
 *
 * <h2>リソース一覧</h2>
 * <ul>
 *   <li>{@code /satellite/mode}            — 動作モード（{@link SatelliteMode}）</li>
 *   <li>{@code /satellite/mode_conflicted} — 同時刻のモード書き込み競合フラグ</li>
 *   <li>{@code /satellite/pointing}        — 指向モード（{@link PointingMode}）</li>
 * </ul>
 */
public final class ModeModel {

    /** 現在の衛星動作モード。 */
    public final Register<SatelliteMode> currentMode = Register.forImmutable(SatelliteMode.NOMINAL);

    /** 現在の指向モード。初期姿勢は太陽指向（充電優先の巡航姿勢）。 */
    public final Register<PointingMode> pointing = Register.forImmutable(PointingMode.SUN_POINTING);

    public ModeModel(final Registrar registrar) {
        registrar.discrete("/satellite/mode", this.currentMode,
            new EnumValueMapper<>(SatelliteMode.class));
        registrar.discrete("/satellite/mode_conflicted", this.currentMode::isConflicted,
            new BooleanValueMapper());
        registrar.discrete("/satellite/pointing", this.pointing,
            new EnumValueMapper<>(PointingMode.class));
    }

    /** 動作モードを遷移させる。 */
    public void set(final SatelliteMode newMode) {
        this.currentMode.set(newMode);
    }

    /** 指向モードを変更する。実際の姿勢変更は {@code Slew} アクティビティが行う。 */
    public void setPointing(final PointingMode newPointing) {
        this.pointing.set(newPointing);
    }

    /** 現在セーフモード中かどうかのスナップショットを返す。 */
    public boolean isSafe() {
        return this.currentMode.get() == SatelliteMode.SAFE;
    }
}
