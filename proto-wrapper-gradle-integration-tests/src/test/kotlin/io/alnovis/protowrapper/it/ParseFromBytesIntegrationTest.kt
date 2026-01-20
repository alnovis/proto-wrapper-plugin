package io.alnovis.protowrapper.it

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Integration tests for static parseFromBytes() method generation.
 *
 * These tests create real Gradle projects and verify that:
 * 1. The parseFromBytes method is generated on interfaces
 * 2. The generated code compiles successfully
 * 3. The method works correctly at runtime
 */
@DisplayName("parseFromBytes() Integration Tests")
class ParseFromBytesIntegrationTest {

    @TempDir
    lateinit var testProjectDir: Path

    private val pluginVersion: String = System.getProperty("pluginVersion", "1.6.9")
    private val gradleExecutable: String = findGradleExecutable()

    companion object {
        @JvmStatic
        fun isProtocAvailable(): Boolean {
            return try {
                val process = ProcessBuilder("protoc", "--version")
                    .redirectErrorStream(true)
                    .start()
                process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0
            } catch (e: Exception) {
                false
            }
        }

        private fun findGradleExecutable(): String {
            val candidates = listOf(
                "/usr/bin/gradle",
                "/usr/local/bin/gradle",
                "${System.getProperty("user.home")}/.sdkman/candidates/gradle/current/bin/gradle",
                "gradle"
            )
            for (candidate in candidates) {
                val file = File(candidate)
                if (file.exists() && file.canExecute()) {
                    return candidate
                }
            }
            return "gradle"
        }
    }

    @BeforeEach
    fun setUp() {
        // Create settings.gradle.kts
        testProjectDir.resolve("settings.gradle.kts").toFile().writeText("""
            pluginManagement {
                repositories {
                    mavenLocal()
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

            dependencyResolutionManagement {
                repositories {
                    mavenLocal()
                    mavenCentral()
                }
            }

            rootProject.name = "test-project"
        """.trimIndent())
    }

    private fun createProtoFiles() {
        val v1Dir = testProjectDir.resolve("proto/v1").toFile()
        v1Dir.mkdirs()

        // Create simple Money.proto with correct naming for protoc
        File(v1Dir, "common.proto").writeText("""
            syntax = "proto2";
            package com.example.proto.v1;
            option java_package = "com.example.proto.v1";
            option java_outer_classname = "Common";

            message Money {
                required int64 amount = 1;
                required string currency = 2;
            }
        """.trimIndent())
    }

    private fun createBuildFile() {
        testProjectDir.resolve("build.gradle.kts").toFile().writeText("""
            plugins {
                java
                id("com.google.protobuf") version "0.9.4"
                id("io.alnovis.proto-wrapper") version "$pluginVersion"
            }

            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(17))
                }
            }

            dependencies {
                implementation("com.google.protobuf:protobuf-java:3.25.1")
                testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
                testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
                testImplementation("org.assertj:assertj-core:3.24.2")
            }

            sourceSets {
                main {
                    proto {
                        srcDir("proto")
                    }
                }
            }

            protobuf {
                protoc {
                    artifact = "com.google.protobuf:protoc:3.25.1"
                }
            }

            protoWrapper {
                basePackage.set("com.example.model")
                protoRoot.set(file("proto"))
                protoPackagePattern.set("com.example.proto.{version}")
                generateBuilders.set(true)
                versions {
                    create("v1") {
                        protoDir.set("v1")
                    }
                }
            }

            tasks.test {
                useJUnitPlatform()
            }

            tasks.named("compileJava") {
                dependsOn("generateProtoWrappers")
            }
        """.trimIndent())
    }

    private fun createTestFile() {
        val testDir = testProjectDir.resolve("src/test/java/com/example").toFile()
        testDir.mkdirs()

        File(testDir, "ParseFromBytesTest.java").writeText("""
            package com.example;

            import com.example.model.api.Money;
            import com.example.model.api.VersionContext;
            import com.google.protobuf.InvalidProtocolBufferException;
            import org.junit.jupiter.api.Test;
            import static org.assertj.core.api.Assertions.assertThat;

            class ParseFromBytesTest {

                @Test
                void parseFromBytesMethodExists() throws InvalidProtocolBufferException {
                    VersionContext ctx = VersionContext.forVersionId("v1");

                    // Create and serialize
                    Money original = Money.newBuilder(ctx)
                            .setAmount(1000)
                            .setCurrency("USD")
                            .build();
                    byte[] bytes = original.toBytes();

                    // Parse using static method on interface
                    Money parsed = Money.parseFromBytes(ctx, bytes);

                    assertThat(parsed).isNotNull();
                    assertThat(parsed.getAmount()).isEqualTo(1000);
                    assertThat(parsed.getCurrency()).isEqualTo("USD");
                }

                @Test
                void parseFromBytesIsEquivalentToContextMethod() throws InvalidProtocolBufferException {
                    VersionContext ctx = VersionContext.forVersionId("v1");

                    Money original = Money.newBuilder(ctx)
                            .setAmount(500)
                            .setCurrency("EUR")
                            .build();
                    byte[] bytes = original.toBytes();

                    Money viaStatic = Money.parseFromBytes(ctx, bytes);
                    Money viaContext = ctx.parseMoneyFromBytes(bytes);

                    assertThat(viaStatic.getAmount()).isEqualTo(viaContext.getAmount());
                    assertThat(viaStatic.getCurrency()).isEqualTo(viaContext.getCurrency());
                }
            }
        """.trimIndent())
    }

    private fun runGradle(vararg args: String): GradleResult {
        val command = listOf(gradleExecutable, "--no-daemon", "--stacktrace") + args
        val process = ProcessBuilder(command)
            .directory(testProjectDir.toFile())
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val completed = process.waitFor(5, TimeUnit.MINUTES)

        return if (completed) {
            GradleResult(process.exitValue(), output)
        } else {
            process.destroyForcibly()
            GradleResult(-1, "$output\n[TIMEOUT]")
        }
    }

    data class GradleResult(val exitCode: Int, val output: String) {
        val isSuccess: Boolean get() = exitCode == 0
    }

    // ==================== Tests ====================

    @Test
    @EnabledIf("isProtocAvailable")
    @DisplayName("parseFromBytes method is generated and compiles")
    fun parseFromBytesMethodIsGeneratedAndCompiles() {
        createProtoFiles()
        createBuildFile()

        val result = runGradle("generateProtoWrappers", "compileJava")

        assertThat(result.isSuccess)
            .withFailMessage { "Build failed:\n${result.output}" }
            .isTrue()

        // Verify the generated interface contains parseFromBytes
        val generatedInterface = testProjectDir
            .resolve("build/generated/sources/proto-wrapper/main/java/com/example/model/api/Money.java")
            .toFile()

        assertThat(generatedInterface).exists()
        val content = generatedInterface.readText()
        assertThat(content).contains("static Money parseFromBytes(VersionContext ctx, byte[] bytes)")
        assertThat(content).contains("InvalidProtocolBufferException")
        assertThat(content).contains("return ctx.parseMoneyFromBytes(bytes)")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    @DisplayName("parseFromBytes works correctly at runtime")
    fun parseFromBytesWorksCorrectlyAtRuntime() {
        createProtoFiles()
        createBuildFile()
        createTestFile()

        val result = runGradle("test")

        assertThat(result.isSuccess)
            .withFailMessage { "Tests failed:\n${result.output}" }
            .isTrue()
    }
}
