package gov.nasa.jpl.aerie.tutorial.fds;

/**
 * FDS 軌道イベントファイルのイベント種別。
 *
 * <p>定義の詳細は {@code docs/ICD_FDS_ORBIT_EVENTS.md} を参照。
 */
public enum OrbitEventType {
    /** Acquisition of Signal — 地上局可視ウィンドウの開始。station_id 必須。 */
    AOS,

    /** Loss of Signal — 地上局可視ウィンドウの終了。station_id 必須。 */
    LOS,

    /** 本影への突入（食の開始、太陽電池発電喪失）。 */
    UMBRA_ENTRY,

    /** 本影からの脱出（食の終了、太陽電池発電回復）。 */
    UMBRA_EXIT
}
