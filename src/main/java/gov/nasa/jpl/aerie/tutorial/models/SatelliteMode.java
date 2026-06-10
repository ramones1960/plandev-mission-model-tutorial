package gov.nasa.jpl.aerie.tutorial.models;

/**
 * 衛星の動作モード。
 *
 * <p>Aerie タイムライン上に離散リソースとして可視化される。
 * モード遷移は {@code Register} の「最後の書き込み優先 (last-write-wins)」で合成される。
 */
public enum SatelliteMode {
    /** 最小電力・保護状態。科学観測・ダウンリンク不可。FDIR により自動遷移することもある。 */
    SAFE,

    /** 通常ハウスキーピング。観測・ダウンリンク待機中。 */
    NOMINAL,

    /** 科学観測実行中。電力消費大・データ生成中。 */
    SCIENCE,

    /** 地上局へのダウンリンク実行中。電力消費大・データ減少中。 */
    DOWNLINK,

    /** バッテリー充電強化モード（太陽電池パドルを最適角度に固定）。 */
    CHARGING
}
