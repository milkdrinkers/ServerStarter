import java.time.Instant

plugins {
    `java-library`

    kotlin("jvm") version "1.9.24"
    id("com.github.johnrengelman.shadow") version "8.1.1"

    eclipse
    idea
}

group = "atm.bloodworkxgaming"
version = "2.5.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    jvmToolchain(8)
}

repositories {
    mavenCentral()

    maven("https://dvs1.progwml6.com/files/maven/")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")
    annotationProcessor("org.jetbrains:annotations:24.1.0")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.yaml:snakeyaml:2.2")
    implementation("commons-io:commons-io:2.16.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.fusesource.jansi:jansi:2.4.1")
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(8)
        options.compilerArgs.addAll(arrayListOf("-Xlint:all", "-Xlint:-processing", "-Xdiags:verbose"))
        dependsOn("generateResources")
    }

    compileKotlin {
        kotlinOptions {
            javaParameters = true
        }
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    shadowJar {
        archiveBaseName.set(project.name)
        archiveClassifier.set("")
    }

    jar {
        manifest {
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(" ") { it.name };
            attributes["Main-Class"] = "atm.bloodworkxgaming.serverstarter.ServerStarterKt"
        }
    }
}

tasks.register<Copy>("generateResources") {
    group = "build"

    from(
        file("src/main/resources/startserver.bat"),
        file("src/main/resources/startserver.sh")
    )
    filter { it.replace("@@serverstarter-libVersion@@", versionArg) }
    delete(layout.buildDirectory.dir("generatedResources"))
    into(file(layout.buildDirectory.dir("generatedResources")))
}

// Apply custom version arg
val versionArg = if (hasProperty("customVersion"))
    (properties["customVersion"] as String).uppercase() // Uppercase version string
else
    "${project.version}-SNAPSHOT-${Instant.now().epochSecond}" // Append snapshot to version

// Strip prefixed "v" from version tag
project.version = if (versionArg.first().equals('v', true))
    versionArg.substring(1)
else
    versionArg.uppercase()
