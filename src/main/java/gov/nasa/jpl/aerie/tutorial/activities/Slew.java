package gov.nasa.jpl.aerie.tutorial.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.tutorial.Mission;
import gov.nasa.jpl.aerie.tutorial.models.PointingMode;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

/**
 * 姿勢変更（スルー）アクティビティ。
 *
 * <p>リアクションホイールを駆動して指向モードを切り替える。
 * 観測（ターゲット追尾）⇔ 通信（地上局追尾）⇔ 充電（太陽指向）の切り替えは
 * すべてこのアクティビティを挟む必要があり、運用計画上のオーバーヘッドになる。
 *
 * <h2>リソースへの影響</h2>
 * <ul>
 *   <li>{@code /power/load_w} — スルー中のみホイール駆動電力を消費</li>
 *   <li>{@code /satellite/pointing} — 完了時に目標指向モードへ遷移</li>
 * </ul>
 */
@ActivityType("Slew")
public final class Slew {

    /** 変更先の指向モード。 */
    @Parameter
    public PointingMode targetPointing = PointingMode.NADIR_POINTING;

    /** 姿勢変更に要する時間。 */
    @Parameter
    public Duration slewDuration = Duration.of(3, Duration.MINUTES);

    /** スルー中のリアクションホイール駆動電力 [W]。 */
    @Parameter
    public double wheelPowerW = 15.0;

    public Slew() {}

    public Slew(final PointingMode targetPointing) {
        this.targetPointing = targetPointing;
    }

    @Export.Validation("slew duration must be positive")
    public boolean validateDuration() {
        return this.slewDuration.longerThan(Duration.ZERO);
    }

    @Export.Validation("wheel power must be non-negative")
    public boolean validateWheelPower() {
        return this.wheelPowerW >= 0.0;
    }

    @EffectModel
    public void run(final Mission mission) {
        mission.power.addLoad(+this.wheelPowerW);
        delay(this.slewDuration);
        mission.power.addLoad(-this.wheelPowerW);

        mission.mode.setPointing(this.targetPointing);
    }
}
