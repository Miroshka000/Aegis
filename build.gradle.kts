plugins {
    id("java-library")
    id("org.allaymc.gradle.plugin").version("0.2.1")
}

group = "miroshka.aegis"
version = "1.0.0"
description = "Aegis Regions For Allay"

allay {
    api = "0.18.0"

    apiOnly = true

    server = null
    
    generatePluginDescriptor = true

    plugin {
        entrance = "miroshka.aegis.Aegis"
        apiVersion = ">=0.18.0"
        authors += "Miroshka"
    }
}

dependencies {
    compileOnly(group = "org.projectlombok", name = "lombok", version = "1.18.34")
    annotationProcessor(group = "org.projectlombok", name = "lombok", version = "1.18.34")
}