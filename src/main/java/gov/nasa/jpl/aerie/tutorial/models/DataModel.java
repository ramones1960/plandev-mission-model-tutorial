package gov.nasa.jpl.aerie.tutorial.models;

import gov.nasa.jpl.aerie.contrib.models.Accumulator;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.tutorial.Configuration;

/**
 * オンボードデータ（C&DH）サブシステムモデル。
 *
 * <p>ソリッドステートレコーダー（SSR）に蓄積されたデータ量を追跡する。
 * 観測アクティビティで増加し、ダウンリンクアクティビティで減少する。
 *
 * <h2>リソース一覧</h2>
 * <ul>
 *   <li>{@code /data/ssr_volume_gb}     — SSR 蓄積データ量 [GB]</li>
 *   <li>{@code /data/ssr_usage_percent} — SSR 使用率 [%]（派生リソース）</li>
 *   <li>{@code /data/recording_rate_gb_per_sec} — 正味記録レート [GB/s]（正 = 記録、負 = 送信）</li>
 * </ul>
 *
 * <p>SSR の容量超過はモデル内では強制しない。Aerie の制約（Constraint）として
 * {@code /data/ssr_usage_percent <= 100} をチェックし、プラン側で回避すること。
 */
public final class DataModel {

    /** SSR に蓄積されたデータ量 [GB]。 */
    public final Accumulator ssrVolumeGb;

    /** SSR 使用率 [%]（派生リソース）。 */
    public final RealResource usagePercent;

    private final double capacityGb;

    public DataModel(final Registrar registrar, final Configuration config) {
        this.capacityGb = config.ssrCapacityGb();
        this.ssrVolumeGb = new Accumulator(config.initialSsrVolumeGb(), 0.0);
        this.usagePercent = this.ssrVolumeGb.scaledBy(100.0 / capacityGb);

        registrar.real("/data/ssr_volume_gb",     this.ssrVolumeGb);
        registrar.real("/data/ssr_usage_percent", this.usagePercent);
        registrar.real("/data/recording_rate_gb_per_sec", this.ssrVolumeGb.rate);
    }

    /**
     * 記録レートの増減を加算する。
     *
     * <p>アクティビティは開始時と終了時に逆符号で呼び出し、エフェクトを打ち消すこと。
     *
     * @param gbPerSec 正 = データ生成（観測中）、負 = データ送信（ダウンリンク中）
     */
    public void addRecordingRate(final double gbPerSec) {
        this.ssrVolumeGb.rate.add(gbPerSec);
    }

    /** 現在の SSR 蓄積量 [GB] のスナップショットを返す。 */
    public double getVolumeGb() {
        return this.ssrVolumeGb.get();
    }
}
