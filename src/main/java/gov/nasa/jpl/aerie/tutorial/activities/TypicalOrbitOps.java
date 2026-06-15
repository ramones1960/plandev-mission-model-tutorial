package gov.nasa.jpl.aerie.tutorial.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.tutorial.Mission;
import gov.nasa.jpl.aerie.tutorial.models.PointingMode;

import static gov.nasa.jpl.aerie.tutorial.generated.ActivityActions.call;

/**
 * 典型的な1運用サイクルをまとめた複合アクティビティ。
 *
 * <p>LEO 衛星の標準的な運用シーケンス
 * 「観測姿勢へスルー → 観測 → 通信姿勢へスルー → 可視ウィンドウ待ち → ダウンリンク
 * → 太陽指向へ復帰」を子アクティビティの {@code call}（逐次実行）で分解する。
 *
 * <p>Aerie のタイムライン上では親子関係付きで展開されるため、
 * 運用の流れ全体と各フェーズのリソース消費を一目で確認できる。
 */
@ActivityType("TypicalOrbitOps")
public final class TypicalOrbitOps {

    /** 観測対象の識別子。 */
    @Parameter
    public String targetId = "TARGET-001";

    /** 観測継続時間。 */
    @Parameter
    public Duration observationDuration = Duration.of(8, Duration.MINUTES);

    /** ダウンリンクの最大送信時間。 */
    @Parameter
    public Duration downlinkMaxDuration = Duration.of(8, Duration.MINUTES);

    @EffectModel
    public void run(final Mission mission) {
        // 1. 観測ターゲット追尾姿勢へ
        call(mission, new Slew(PointingMode.TARGET_TRACKING));

        // 2. 科学観測（SSR にデータが溜まる）
        call(mission, new Observation(this.targetId, this.observationDuration));

        // 3. 地上局追尾姿勢へ
        call(mission, new Slew(PointingMode.GROUND_STATION_TRACKING));

        // 4. 可視ウィンドウを待ってダウンリンク（SSR からデータを掃き出す）
        call(mission, new Downlink(this.downlinkMaxDuration));

        // 5. 充電優先の太陽指向に復帰
        call(mission, new Slew(PointingMode.SUN_POINTING));
    }
}
