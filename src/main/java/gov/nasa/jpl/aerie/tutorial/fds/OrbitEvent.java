package gov.nasa.jpl.aerie.tutorial.fds;

import java.time.Instant;

/**
 * FDS 軌道イベントファイルの1イベント。
 *
 * <p>外部の軌道力学系（FDS）が解析・配付した軌道イベント
 * （地上局可視の AOS/LOS、食の本影出入り）を表す。
 * フォーマットの正は {@code docs/ICD_FDS_ORBIT_EVENTS.md}。
 *
 * @param time      イベント発生時刻（UTC）
 * @param type      イベント種別
 * @param stationId 地上局識別子（AOS/LOS のみ。それ以外は空文字列）
 */
public record OrbitEvent(Instant time, OrbitEventType type, String stationId) {}
