package missionmodel;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import missionmodel.models.DataModel;
import missionmodel.models.ModeModel;
import missionmodel.models.PowerModel;

/**
 * ミッションモデルのトップレベルクラス。
 *
 * <p>PlanDev シミュレーションエンジンがシミュレーション開始時にインスタンス化する。
 * サブシステムモデルを組み合わせ、アクティビティが参照できる単一の状態オブジェクトを提供する。
 *
 * <p>アクティビティの {@code run(Mission mission)} メソッドはこのオブジェクトを通じて
 * 各サブシステムのリソースにエフェクトを与える。
 */
public final class Mission {

    /** streamline ライブラリ用レジストラ。エフェクト競合などのモデルエラーはログとして記録する。 */
    public final Registrar errorRegistrar;

    /** 電力サブシステム（バッテリー・消費電力・太陽電池）。 */
    public final PowerModel power;

    /** データサブシステム（SSR データ量）。 */
    public final DataModel data;

    /** 動作モード管理。 */
    public final ModeModel mode;

    /**
     * PlanDev から呼び出されるコンストラクタ。
     *
     * @param registrar PlanDev がリソース登録に使う依存注入オブジェクト
     * @param config    {@link Configuration} に定義されたミッション固有パラメータ
     */
    public Mission(final gov.nasa.jpl.aerie.merlin.framework.Registrar registrar, final Configuration config) {
        this.errorRegistrar = new Registrar(registrar, Registrar.ErrorBehavior.Log);
        this.power = new PowerModel(errorRegistrar, config);
        this.data = new DataModel(errorRegistrar, config);
        this.mode = new ModeModel(errorRegistrar);
    }
}
