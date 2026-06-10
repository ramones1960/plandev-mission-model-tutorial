package gov.nasa.jpl.aerie.tutorial.models;

/**
 * 衛星の指向（ポインティング）モード。
 *
 * <p>観測・通信・発電はそれぞれ異なる姿勢を要求するため、
 * 姿勢変更（{@code Slew} アクティビティ）が運用計画上の重要な制約になる。
 */
public enum PointingMode {
    /** 太陽電池パドルを太陽に正対。充電効率最大。 */
    SUN_POINTING,

    /** 機体を地心方向（直下）に指向。デフォルトの巡航姿勢。 */
    NADIR_POINTING,

    /** 観測ターゲットを追尾。科学観測時に必要。 */
    TARGET_TRACKING,

    /** 地上局アンテナ方向を追尾。ダウンリンク時に必要。 */
    GROUND_STATION_TRACKING
}
