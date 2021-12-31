plugins {
    kotlin("jvm") version "1.6.10"
}

val ver = "0.2.6"

group = "com.github.frierenzk"
version = "$ver-${getGitID()}"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.0")

    implementation("com.corundumstudio.socketio", "netty-socketio", "1.7.19")
    implementation("com.google.code.gson", "gson", "2.8.9")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("io.socket", "socket.io-client", "1.0.1")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    jar {
        duplicatesStrategy = DuplicatesStrategy.WARN
        manifest {
            attributes["Main-Class"] = "com.github.frierenzk.MuppetKt"
            attributes["Implementation-Version"] = ver
        }
        configurations["runtimeClasspath"].forEach { file: File ->
            from(zipTree(file.absoluteFile))
        }
    }
}

fun getGitID(): String =
    Runtime.getRuntime().exec("git rev-parse --short HEAD")?.inputStream?.bufferedReader()?.readLine().let {
        if (it is String && it.toLongOrNull(radix = 16) is Long) it.trim()
        else "unknown"
    }