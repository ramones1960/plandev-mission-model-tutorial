package gov.nasa.jpl.aerie.tutorial.fds;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * FDS 軌道イベントファイル（CSV）のローダー。
 *
 * <p>{@code docs/ICD_FDS_ORBIT_EVENTS.md} に定義されたフォーマットを読み込み、
 * ICD の整合性規則（時系列順・AOS/LOS の対応・可視ウィンドウの非重複・
 * 食イベントの交互性）を検証する。違反があればシミュレーション開始前に
 * 行番号付きの例外で fail-fast する。
 */
public final class OrbitEventsLoader {

    private OrbitEventsLoader() {}

    /**
     * 軌道イベントファイルを読み込み、検証済みのイベント列を返す。
     *
     * @param path FDS が配付した軌道イベントファイル（CSV）のパス
     * @return 時刻昇順のイベントリスト
     * @throws IllegalArgumentException ICD のフォーマット・整合性規則に違反した場合
     * @throws UncheckedIOException     ファイルが読めない場合
     */
    public static List<OrbitEvent> load(final Path path) {
        final List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (final IOException ex) {
            throw new UncheckedIOException("cannot read FDS orbit events file: " + path, ex);
        }

        final var events = new ArrayList<OrbitEvent>();
        for (int i = 0; i < lines.size(); i++) {
            final var lineNo = i + 1;
            final var line = lines.get(i).strip();

            // 空行・コメント行・ヘッダ行は読み飛ばす
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("event_time_utc")) continue;

            final var fields = line.split(",", -1);
            if (fields.length < 2) {
                throw error(path, lineNo, "expected 'event_time_utc,event_type[,station_id]' but got: " + line);
            }

            final Instant time;
            try {
                time = Instant.parse(fields[0].strip());
            } catch (final DateTimeParseException ex) {
                throw error(path, lineNo, "event_time_utc must be ISO 8601 UTC (e.g. 2026-01-01T00:00:00Z): " + fields[0]);
            }

            final OrbitEventType type;
            try {
                type = OrbitEventType.valueOf(fields[1].strip());
            } catch (final IllegalArgumentException ex) {
                throw error(path, lineNo, "unknown event_type: " + fields[1]);
            }

            final var stationId = (fields.length >= 3) ? fields[2].strip() : "";
            if ((type == OrbitEventType.AOS || type == OrbitEventType.LOS) && stationId.isEmpty()) {
                throw error(path, lineNo, type + " requires a station_id");
            }

            events.add(new OrbitEvent(time, type, stationId));
        }

        validate(path, events);
        return events;
    }

    /** ICD 5章の整合性規則を検証する。 */
    private static void validate(final Path path, final List<OrbitEvent> events) {
        Instant previousTime = null;
        String visibleStation = null;   // 開いている可視ウィンドウの局（null = 不可視）
        Boolean inUmbra = null;         // 食イベントを一度でも見たら non-null

        for (final var event : events) {
            if (previousTime != null && event.time().isBefore(previousTime)) {
                throw new IllegalArgumentException(
                    path + ": events must be in chronological order, but " + event + " precedes " + previousTime);
            }
            previousTime = event.time();

            switch (event.type()) {
                case AOS -> {
                    if (visibleStation != null) {
                        throw new IllegalArgumentException(
                            path + ": overlapping contact windows — AOS " + event
                            + " while window for station " + visibleStation + " is still open");
                    }
                    visibleStation = event.stationId();
                }
                case LOS -> {
                    if (visibleStation == null || !visibleStation.equals(event.stationId())) {
                        throw new IllegalArgumentException(
                            path + ": LOS " + event + " does not match an open AOS window"
                            + " (open station: " + visibleStation + ")");
                    }
                    visibleStation = null;
                }
                case UMBRA_ENTRY -> {
                    if (Boolean.TRUE.equals(inUmbra)) {
                        throw new IllegalArgumentException(
                            path + ": UMBRA_ENTRY " + event + " while already in umbra");
                    }
                    inUmbra = true;
                }
                case UMBRA_EXIT -> {
                    if (Boolean.FALSE.equals(inUmbra)) {
                        throw new IllegalArgumentException(
                            path + ": UMBRA_EXIT " + event + " while not in umbra");
                    }
                    inUmbra = false;
                }
            }
        }
    }

    private static IllegalArgumentException error(final Path path, final int lineNo, final String message) {
        return new IllegalArgumentException(path + " (line " + lineNo + "): " + message);
    }
}
