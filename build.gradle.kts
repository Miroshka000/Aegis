plugins {
    id("java-library")
    id("org.allaymc.gradle.plugin").version("0.1.2")
}

group = "miroshka.aegis"
version = "1.0.0"
description = "Aegis Regions For Allay"

allay {
    api = "0.18.0"
    apiOnly = false
    
    plugin {
        entrance = "miroshka.aegis.Aegis"
        apiVersion = ">=0.18.0"
        authors += "Miroshka"
    }

    dependencies {
        compileOnly(group = "org.projectlombok", name = "lombok", version = "1.18.34")
        annotationProcessor(group = "org.projectlombok", name = "lombok", version = "1.18.34")
    }
}
