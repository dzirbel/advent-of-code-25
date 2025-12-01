plugins {
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    jvmToolchain(21)
}

tasks.register<JavaExec>("day1") {
    mainClass = "com.dzirbel.day1.Day1Kt"
    classpath = sourceSets["jvmMain"].runtimeClasspath
}
