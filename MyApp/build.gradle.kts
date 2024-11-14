plugins {
    kotlin("jvm") version "2.0.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("com.kohlschutter.junixsocket:junixsocket-common:2.10.1")
    implementation("com.kohlschutter.junixsocket:junixsocket-core:2.10.1")
    testImplementation("io.mockk:mockk:1.13.13")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(
        "-XX:+EnableDynamicAgentLoading",
        "-Djdk.instrumeent.traceUsage"
    )
}

