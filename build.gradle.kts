plugins {
    kotlin("jvm") version "1.5.21"
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
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.5.1")

    implementation("com.corundumstudio.socketio", "netty-socketio", "1.7.19")
    implementation("com.google.code.gson", "gson", "2.8.7")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
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
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
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

fun getGitID(): String {
    val p = Runtime.getRuntime().exec("git rev-parse --short HEAD")
    val id = p?.inputStream?.bufferedReader()?.readLine()
    if (id is String && id.isNotBlank()) return id.trim()
    else return "unknown"
}