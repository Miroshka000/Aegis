plugins {
    id("java-library")
    id("org.allaymc.gradle.plugin").version("0.2.1")
    id("com.gradleup.shadow").version("9.2.2")
}

group = "miroshka.aegis"
version = "1.0.3"
description = "Aegis Regions For Allay"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven("https://storehouse.okaeri.eu/repository/maven-public/")
}

allay {
    api = "0.27.0"

    apiOnly = true

    server = null
    
    generatePluginDescriptor = true

    plugin {
        entrance = "miroshka.aegis.Aegis"
        apiVersion = ">=0.27.0"
        authors += "Miroshka"
    }
}

dependencies {
    compileOnly(group = "org.projectlombok", name = "lombok", version = "1.18.34")
    annotationProcessor(group = "org.projectlombok", name = "lombok", version = "1.18.34")
    implementation("eu.okaeri:okaeri-configs-yaml-snakeyaml:5.0.13")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveFileName.set("Aegis.jar")
    mergeServiceFiles()
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
