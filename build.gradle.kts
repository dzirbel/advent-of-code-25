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

private val runRegex = """^runJvmDay(\d+)([a|b])$""".toRegex()

private val runTasks = tasks.named { taskName -> runRegex.matches(taskName) }

runTasks.configureEach {
    outputs.upToDateWhen { false }
}

private data class Day(val num: Int, val half: Char) : Comparable<Day> {
    override fun compareTo(other: Day) = num.compareTo(other.num).takeIf { it != 0 } ?: half.compareTo(other.half)
}

// execute days one-by-one, in order
runTasks
    .sortedBy { task ->
        val (day, half) = checkNotNull(runRegex.matchEntire(task.name)).destructured
        Day(num = day.toInt(), half = half.first())
    }
    .zipWithNext { previous, next -> next.mustRunAfter(previous) }

tasks.register("runAll") {
    group = "application"
    description = "Run all executable binaries."

    dependsOn(runTasks.named { taskName -> taskName != name })
}
