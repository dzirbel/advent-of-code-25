import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

plugins {
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)

    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        binaries {
            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "day1") {
                mainClass.set("com.dzirbel.day1.Day1Kt")
            }
        }
    }
}

tasks.register("runAll") {
    group = "application"
    description = "Run all executable binaries."

    dependsOn(tasks.named { taskName -> taskName.startsWith("run") && taskName != name })
}
