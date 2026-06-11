import java.net.URI

plugins {
    id("java")
}

group   = "missionmodel"
version = "0.1.0"

java {
    toolchain {
        // Aerie 2.8.0 以降のミッションモデルは Java 21 が必要
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages-AMMOS-Aerie"
        url  = URI("https://maven.pkg.github.com/NASA-AMMOS/aerie")
        credentials {
            username = project.findProperty("gpr.user") as String?
                ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String?
                ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

val aerieVersion = "2.14.0"

// JNISpice (contrib の推移的依存) は Maven で再配布されておらず、
// 本モデルは SPICE 機能を使わないため全 configuration から除外する
configurations.all {
    exclude(group = "JNISpice", module = "JNISpice")
}

dependencies {
    annotationProcessor("gov.nasa.jpl.aerie:merlin-framework-processor:$aerieVersion")

    implementation("gov.nasa.jpl.aerie:merlin-framework:$aerieVersion")
    implementation("gov.nasa.jpl.aerie:merlin-sdk:$aerieVersion")
    implementation("gov.nasa.jpl.aerie:merlin-driver:$aerieVersion")
    implementation("gov.nasa.jpl.aerie:parsing-utilities:$aerieVersion")
    implementation("gov.nasa.jpl.aerie:contrib:$aerieVersion")

    testImplementation("gov.nasa.jpl.aerie:merlin-framework-junit:$aerieVersion")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

// PlanDev にアップロードする JAR は依存ライブラリ（contrib など）を同梱した fat jar にする。
// 公式 aerie-mission-model-template と同じ方式。
tasks.named<Jar>("jar") {
    from(configurations.runtimeClasspath.get().filter { it.exists() }.map {
        if (it.isDirectory) it else zipTree(it)
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
