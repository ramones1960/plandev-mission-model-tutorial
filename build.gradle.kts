import java.net.URI

plugins {
    id("java")
}

group   = "gov.nasa.jpl.aerie.tutorial"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
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

dependencies {
    implementation("gov.nasa.jpl.aerie:merlin-framework:$aerieVersion")
    annotationProcessor("gov.nasa.jpl.aerie:merlin-framework-processor:$aerieVersion")

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
