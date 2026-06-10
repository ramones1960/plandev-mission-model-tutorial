package gov.nasa.jpl.aerie.tutorial;

import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.tutorial.models.DataModel;
import gov.nasa.jpl.aerie.tutorial.models.ModeModel;
import gov.nasa.jpl.aerie.tutorial.models.OrbitModel;
import gov.nasa.jpl.aerie.tutorial.models.PointingMode;
import gov.nasa.jpl.aerie.tutorial.models.PowerModel;
import gov.nasa.jpl.aerie.tutorial.models.SatelliteMode;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;

/**
 * ミッションモデルのトップレベルクラス。
 *
 * <p>Aerie シミュレーションエンジンがシミュレーション開始時にインスタンス化する。
 * サブシステムモデルを集約し、アクティビティが参照する単一の状態オブジェクトを提供する。
 * またバックグラウンドで動く自律機能（軌道サイクル・充電制御・電力 FDIR）を
 * デーモンタスクとして起動する。
 */
public final class Mission {

    /** 電力サブシステム（バッテリー・太陽電池・負荷）。 */
    public final PowerModel power;

    /** データサブシステム（SSR データ量）。 */
    public final DataModel data;

    /** 動作モード・指向モード管理。 */
    public final ModeModel mode;

    /** 軌道イベント（日照/食・地上局可視）。 */
    public final OrbitModel orbit;

    private final Configuration config;

    /**
     * Aerie から呼び出されるコンストラクタ。
     *
     * @param registrar Aerie がリソース登録に使う依存注入オブジェクト
     * @param config    {@link Configuration} に定義されたミッション固有パラメータ
     */
    public Mission(final Registrar registrar, final Configuration config) {
        this.config = config;
        this.power = new PowerModel(registrar, config);
        this.data  = new DataModel(registrar, config);
        this.mode  = new ModeModel(registrar);
        this.orbit = new OrbitModel(registrar, config, this.power);

        // 自律動作するデーモンタスク群
        spawn(this.orbit::runSunlightCycle);       // 日照⇔食サイクル
        spawn(this.orbit::runGroundStationCycle);  // 地上局可視ウィンドウ
        spawn(this.power::runChargeController);    // 満充電クランプ
        spawn(this::runBatteryFdir);               // 低電力セーフモード
    }

    /**
     * 電力 FDIR（Fault Detection, Isolation and Recovery）デーモン。
     *
     * <p>バッテリー SoC がしきい値を下回ると自動でセーフモードに入り、
     * 太陽指向に変更して非必須負荷を切り離す。SoC が回復したら通常モードに復帰する。
     * プラン上のアクティビティと独立に発動するため、過密なプランを組むと
     * 観測がセーフモードに阻まれる様子を Aerie 上で観察できる。
     */
    private void runBatteryFdir() {
        final var socLow       = this.power.socPercent.isBetween(-1.0e30, config.lowPowerSocPercent());
        final var socRecovered = this.power.socPercent.isBetween(config.recoverySocPercent(), 1.0e30);

        while (true) {
            waitUntil(socLow);

            // セーフモード移行: 太陽指向 + 非必須負荷のシェッド
            this.mode.set(SatelliteMode.SAFE);
            this.mode.setPointing(PointingMode.SUN_POINTING);
            this.power.addLoad(-config.safeModeLoadShedW());

            waitUntil(socRecovered);

            // リカバリ: 負荷を再投入して通常運用に復帰
            this.power.addLoad(+config.safeModeLoadShedW());
            this.mode.set(SatelliteMode.NOMINAL);
        }
    }
}
