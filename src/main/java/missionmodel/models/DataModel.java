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

/**
 * オンボードデータ量モデル。
 *
 * <p>正味の記録レート（観測による生成 − ダウンリンクによる送信）を離散リソースとして保持し、
 * その時間積分としてソリッドステートレコーダー（SSR）の蓄積データ量を導出する。
 * 蓄積データ量は {@code clampedIntegrate} により 0〜SSR 総容量の範囲に制限される。
 *
 * <h2>リソース一覧</h2>
 * <ul>
 *   <li>{@code data/stored_volume_gb}          — SSR 蓄積データ量 [GB]（線形・0〜容量にクランプ）</li>
 *   <li>{@code data/recording_rate_gb_per_sec} — 正味記録レート [GB/s]（離散）</li>
 * </ul>
 */
public final class DataModel {

    /** 正味記録レート [GB/s]。正 = 蓄積（観測中）、負 = 送信超過（ダウンリンク中）。 */
    public final MutableResource<Discrete<Double>> recordingRateGbPerSec;

    /** SSR に蓄積されたデータ量 [GB]。記録レートの時間積分として導出される。 */
    public final Resource<Polynomial> storedVolumeGb;

    public DataModel(final Registrar registrar, final Configuration config) {
        this.recordingRateGbPerSec = resource(discrete(0.0));

        // SSR は空（0 GB）以下にも総容量以上にもならない
        this.storedVolumeGb = PolynomialResources.clampedIntegrate(
            asPolynomial(recordingRateGbPerSec),
            constant(0.0),
            constant(config.dataStorageCapacityGb()),
            config.initialDataVolumeGb()
        ).integral();

        registrar.discrete("data/recording_rate_gb_per_sec", recordingRateGbPerSec, new DoubleValueMapper());
        registrar.real("data/stored_volume_gb", PolynomialResources.assumeLinear(storedVolumeGb));
    }

    /** データ生成の開始（観測機器の起動など）によるレート増加 [GB/s] を反映する。 */
    public void increaseRecordingRate(final double gbPerSec) {
        DiscreteEffects.increase(recordingRateGbPerSec, gbPerSec);
    }

    /** データ送信の開始（ダウンリンクなど）やデータ生成の停止によるレート減少 [GB/s] を反映する。 */
    public void decreaseRecordingRate(final double gbPerSec) {
        DiscreteEffects.decrease(recordingRateGbPerSec, gbPerSec);
    }
}
