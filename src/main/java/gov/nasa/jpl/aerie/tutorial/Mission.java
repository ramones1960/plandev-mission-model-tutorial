package gov.nasa.jpl.aerie.tutorial;

import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.tutorial.models.DataModel;
import gov.nasa.jpl.aerie.tutorial.models.ModeModel;
import gov.nasa.jpl.aerie.tutorial.models.PowerModel;

/**
 * ミッションモデルのトップレベルクラス。
 *
 * <p>Aerie シミュレーションエンジンがシミュレーション開始時にインスタンス化する。
 * サブシステムモデルを組み合わせ、アクティビティが参照できる単一の状態オブジェクトを提供する。
 *
 * <p>アクティビティの {@code run(Mission mission)} メソッドはこのオブジェクトを通じて
 * 各リソースにエフェクトを加算する。
 */
public final class Mission {

    /** 電力サブシステム（バッテリー・消費電力）。 */
    public final PowerModel power;

    /** データサブシステム（SSR データ量）。 */
    public final DataModel data;

    /** 動作モード管理。 */
    public final ModeModel mode;

    /**
     * Aerie から呼び出されるコンストラクタ。
     *
     * @param registrar Aerie がリソース登録・スケジュール管理に使う依存注入オブジェクト
     * @param config    {@link Configuration} に定義されたミッション固有パラメータ
     */
    public Mission(final Registrar registrar, final Configuration config) {
        this.power = new PowerModel(
            registrar,
            config.initialBatteryEnergyWh(),
            config.batteryCapacityWh(),
            config.solarPanelMaxPowerW(),
            config.housekeepingPowerW()
        );
        this.data = new DataModel(
            registrar,
            config.initialDataVolumeGb(),
            config.dataStorageCapacityGb()
        );
        this.mode = new ModeModel(registrar);
    }
}
