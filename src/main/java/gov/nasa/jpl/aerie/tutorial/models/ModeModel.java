package gov.nasa.jpl.aerie.tutorial.models;

import gov.nasa.jpl.aerie.merlin.framework.MutableResource;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import static gov.nasa.jpl.aerie.merlin.framework.Resources.emit;
import static gov.nasa.jpl.aerie.merlin.framework.Resources.resource;

/**
 * 衛星動作モードモデル。
 *
 * <p>現在の動作モードを離散リソースとして追跡する。
 * Aerie タイムラインビューではモード遷移がステップ関数として可視化される。
 *
 * <h3>リソース一覧</h3>
 * <ul>
 *   <li>{@code satellite/mode} — 現在の動作モード（離散リソース）</li>
 * </ul>
 */
public final class ModeModel {

    /** 現在の衛星動作モード。 */
    public final MutableResource<SatelliteMode> currentMode;

    public ModeModel(final Registrar registrar) {
        this.currentMode = resource(SatelliteMode.NOMINAL);

        // 離散リソースとして登録。シリアライザはモード名を文字列に変換する。
        registrar.discrete(
            "satellite/mode",
            currentMode,
            mode -> SerializedValue.of(mode.name())
        );
    }

    /**
     * 衛星モードを遷移させる。
     *
     * <p>Aerie の離散リソース合成は「最後の書き込み優先 (override)」のため、
     * 同一タイムステップで複数のアクティビティが遷移を要求した場合は
     * 実行順序が後のものが有効になる。
     *
     * @param newMode 遷移先モード
     */
    public void setMode(final SatelliteMode newMode) {
        emit(currentMode, newMode);
    }
}
