/**
 * PlanDev (Aerie) ミッションモデル チュートリアル — 衛星リソース管理
 *
 * <p>このパッケージは NASA AMMOS PlanDev 向けのミッションモデルです。
 * 電力・データ量・動作モードの3系統のリソースをモデル化します。
 */
@MissionModel(model = Mission.class)
@WithConfiguration(Configuration.class)
@WithMappers(BasicValueMappers.class)
@WithActivityType(Observation.class)
@WithActivityType(Downlink.class)
@WithActivityType(SafeModeEntry.class)
@WithActivityType(SolarCharging.class)
package missionmodel;

import gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithConfiguration;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithMappers;
import missionmodel.activities.Downlink;
import missionmodel.activities.Observation;
import missionmodel.activities.SafeModeEntry;
import missionmodel.activities.SolarCharging;
