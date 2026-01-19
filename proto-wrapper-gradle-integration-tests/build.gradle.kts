plugins {
    kotlin("jvm") version "2.0.21"
}

group = "space.alnovis"
version = "1.6.8"

// Version of the plugin to test (must be published to mavenLocal first)
val pluginVersion = version.toString()

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()

    // Run tests sequentially to avoid conflicts
    maxParallelForks = 1

    // Pass plugin version to tests
    systemProperty("pluginVersion", pluginVersion)

    // Pass paths to tests
    systemProperty("projectRoot", rootProject.projectDir.parentFile.absolutePath)
    systemProperty("testProtosDir", "${rootProject.projectDir.parentFile.absolutePath}/test-protos/scenarios")
}
