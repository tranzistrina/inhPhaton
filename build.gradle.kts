plugins {
    java
}

group = "ru.khozain"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper 26.2 API (последний stable — 26.2.build.121-stable)
    compileOnly("io.papermc.paper:paper-api:26.2.build.121-stable")

    // JUnit для smoke-тестов конфигурации
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("inhPhaton")
    archiveVersion.set("")
    archiveClassifier.set("")
    manifest {
        attributes(
            mapOf(
                "Built-By" to "inh",
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        )
    }
}

tasks.processResources {
    filesMatching("*.yml") {
        expand("version" to project.version, "name" to project.name)
    }
}