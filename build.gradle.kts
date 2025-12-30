plugins {
    base
    kotlin("jvm") version "1.9.22" apply false
}

allprojects {
    group = "space.alnovis"
    version = "1.1.1"

    repositories {
        mavenCentral()
        mavenLocal() // Для proto-wrapper-core из Maven при разработке
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}
