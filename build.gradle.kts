import java.time.Instant

plugins {
    `java-library`

    kotlin("jvm") version "1.9.24"
    id("com.github.johnrengelman.shadow") version "8.1.1"

    eclipse
    idea
}

group = "atm.bloodworkxgaming"
version = "2.4.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()

    maven("https://dvs1.progwml6.com/files/maven/")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")
    annotationProcessor("org.jetbrains:annotations:24.1.0")

    testImplementation("junit:junit:4.13.2")
    implementation("org.yaml:snakeyaml:1.29")
    implementation("commons-io:commons-io:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.2")
    implementation("com.google.code.gson:gson:2.8.8")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.fusesource.jansi:jansi:2.3.4")
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(8)
        options.compilerArgs.addAll(arrayListOf("-Xlint:all", "-Xlint:-processing", "-Xdiags:verbose"))
    }

    javadoc {
        isFailOnError = false
        val options = options as StandardJavadocDocletOptions
        options.encoding = Charsets.UTF_8.name()
        options.overview = "src/main/javadoc/overview.html"
        options.tags("apiNote:a:API Note:", "implNote:a:Implementation Note:", "implSpec:a:Implementation Requirements:")
        options.use()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    shadowJar {
        archiveBaseName.set(project.name)
        archiveClassifier.set("")

        // Shadow classes
//        fun reloc(originPkg: String, targetPkg: String) = relocate(originPkg, "${mainPackage}.lib.${targetPkg}")

//        reloc("space.arim.morepaperlib", "morepaperlib")
//        reloc("com.github.milkdrinkers.Crate", "crate")
//        reloc("com.github.milkdrinkers.colorparser", "colorparser")
//        reloc("dev.jorel.commandapi", "commandapi")
//        reloc("dev.triumphteam.gui", "gui")
//        reloc("com.zaxxer.hikari", "hikaricp")

        minimize()
    }
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