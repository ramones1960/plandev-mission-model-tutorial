/**
 * Aerie ミッションモデル チュートリアル — 小型 LEO 衛星の運用計画
 *
 * <p>電力・データ・動作モード・指向モード・軌道イベント（日照/食・地上局可視）を
 * モデル化し、観測 → 姿勢変更 → ダウンリンク → 充電という衛星運用の流れを
 * Aerie 上で検討できるようにするミッションモデル。
 */
@MissionModel(model = Mission.class)
@WithConfiguration(Configuration.class)
@WithMappers(BasicValueMappers.class)
@WithActivityType(Slew.class)
@WithActivityType(Observation.class)
@WithActivityType(Downlink.class)
@WithActivityType(SolarCharging.class)
@WithActivityType(SafeModeEntry.class)
@WithActivityType(TypicalOrbitOps.class)
package gov.nasa.jpl.aerie.tutorial;

import gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithConfiguration;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithMappers;
import gov.nasa.jpl.aerie.tutorial.activities.Downlink;
import gov.nasa.jpl.aerie.tutorial.activities.Observation;
import gov.nasa.jpl.aerie.tutorial.activities.SafeModeEntry;
import gov.nasa.jpl.aerie.tutorial.activities.Slew;
import gov.nasa.jpl.aerie.tutorial.activities.SolarCharging;
import gov.nasa.jpl.aerie.tutorial.activities.TypicalOrbitOps;
