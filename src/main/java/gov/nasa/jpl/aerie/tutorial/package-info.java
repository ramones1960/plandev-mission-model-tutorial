/**
 * Aerie ミッションモデル チュートリアル — 衛星リソース管理
 *
 * このパッケージは NASA AMMOS Aerie 向けのミッションモデルです。
 * 電力・データ量・動作モードの3リソースをモデル化します。
 */
@MissionModel(model = Mission.class)
@MissionModel.WithConfiguration(Configuration.class)
@MissionModel.WithMappers(BasicValueMappers.class)
@MissionModel.WithActivityTypes({
    Observation.class,
    Downlink.class,
    SafeModeEntry.class,
    SolarCharging.class
})
package gov.nasa.jpl.aerie.tutorial;

import gov.nasa.jpl.aerie.merlin.framework.BasicValueMappers;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel;
import gov.nasa.jpl.aerie.tutorial.activities.Downlink;
import gov.nasa.jpl.aerie.tutorial.activities.Observation;
import gov.nasa.jpl.aerie.tutorial.activities.SafeModeEntry;
import gov.nasa.jpl.aerie.tutorial.activities.SolarCharging;
