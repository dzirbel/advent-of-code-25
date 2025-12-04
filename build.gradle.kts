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

            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "day2a") {
                mainClass.set("com.dzirbel.day2.Day2aKt")
            }

            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "day2b") {
                mainClass.set("com.dzirbel.day2.Day2bKt")
            }

            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "day3a") {
                mainClass.set("com.dzirbel.day3.Day3aKt")
            }

            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "day3b") {
                mainClass.set("com.dzirbel.day3.Day3bKt")
            }

            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "day4a") {
                mainClass.set("com.dzirbel.day4.Day4aKt")
            }

            executable(KotlinCompilation.MAIN_COMPILATION_NAME, "day4b") {
                mainClass.set("com.dzirbel.day4.Day4bKt")
            }
        }
    }
}

tasks.register("runAll") {
    group = "application"
    description = "Run all executable binaries."

    dependsOn(tasks.named { taskName -> taskName.startsWith("run") && taskName != name })
}
