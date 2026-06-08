package gov.nasa.jpl.aerie.tutorial.models;

import gov.nasa.jpl.aerie.merlin.framework.MutableResource;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;

import static gov.nasa.jpl.aerie.merlin.framework.Resources.emit;
import static gov.nasa.jpl.aerie.merlin.framework.Resources.resource;

/**
 * オンボードデータ量モデル。
 *
 * <p>ソリッドステートレコーダー（SSR）に蓄積されたデータ量を追跡する。
 * 観測アクティビティで増加し、ダウンリンクアクティビティで減少する。
 *
 * <h3>リソース一覧</h3>
 * <ul>
 *   <li>{@code data/stored_volume_gb} — SSR 蓄積データ量 [GB]（線形リソース）</li>
 * </ul>
 */
public final class DataModel {

    /** SSR に蓄積されたデータ量 [GB]。 */
    public final MutableResource<RealDynamics> storedDataVolumeGb;

    private final double maxCapacityGb;

    public DataModel(final Registrar registrar,
                     final double initialVolumeGb,
                     final double maxCapacityGb) {
        this.maxCapacityGb = maxCapacityGb;
        this.storedDataVolumeGb = resource(RealDynamics.linear(initialVolumeGb, 0.0));

        registrar.real("data/stored_volume_gb", storedDataVolumeGb);
    }

    /**
     * データレートの増減を加算する。
     *
     * @param deltaGbPerSec 正 = データ生成（観測中）、負 = データ送信（ダウンリンク中）
     */
    public void emitDataRateDelta(final double deltaGbPerSec) {
        emit(storedDataVolumeGb, RealDynamics.linear(0.0, deltaGbPerSec));
    }

    /** SSR の使用率 [0.0〜1.0] を返す（スナップショット）。 */
    public double usageRatio() {
        return storedDataVolumeGb.getDynamics().extract() / maxCapacityGb;
    }
}
