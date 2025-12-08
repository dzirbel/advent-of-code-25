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

    val mainRegex = Regex("""fun\s+main\s*\(""")
    val mainClassFiles: List<File> = sourceSets.flatMap { sourceSet ->
        files(sourceSet.kotlin.srcDirs).flatMap { srcDir ->
            files(srcDir)
                .asFileTree
                .matching { include("**/*.kt") }
                .filter { file ->
                    file.useLines { lines ->
                        lines.any { line -> line.contains(mainRegex) }
                    }
                }
                .map { file -> file.relativeTo(srcDir) }
        }
    }

    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        binaries {
            for (file in mainClassFiles) {
                executable(KotlinCompilation.MAIN_COMPILATION_NAME, file.nameWithoutExtension.lowercase()) {
                    mainClass.set("${file.path.removeSuffix(".${file.extension}")}Kt")
                }
            }
        }
    }
}

val runTasks = tasks.named { taskName -> taskName.startsWith("run") }

// execute days one-by-one, in order
runTasks.sortedBy { it.name }.zipWithNext { previous, next -> next.mustRunAfter(previous) }

tasks.register("runAll") {
    group = "application"
    description = "Run all executable binaries."

    dependsOn(runTasks.named { taskName -> taskName != name })
}
