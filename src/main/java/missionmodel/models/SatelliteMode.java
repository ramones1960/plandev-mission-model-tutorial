package missionmodel.models;

/**
 * 衛星の動作モード。
 *
 * <p>PlanDev タイムライン上に離散リソースとして可視化される。
 */
public enum SatelliteMode {
    /** 最小電力・保護状態。科学観測・ダウンリンク不可。 */
    SAFE,

    /** 通常ハウスキーピング。観測・ダウンリンク待機中。 */
    NOMINAL,

    /** 科学観測実行中。電力消費大・データ生成中。 */
    SCIENCE,

    /** 地上局へのダウンリンク実行中。電力消費大・データ減少中。 */
    DOWNLINK,

    /** バッテリー充電強化モード（太陽電池パネルを最適角度に固定）。 */
    CHARGING
}
