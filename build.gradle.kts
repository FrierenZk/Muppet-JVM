plugins {
    kotlin("jvm") version "1.4.20"
}

group = "com.github.frierenzk"
version = "0.2.4".let {
    "$it${if (getGitID().isBlank()) "unknown" else "-${getGitID()}"}"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.4.2")

    implementation("com.corundumstudio.socketio","netty-socketio","1.7.18")
    implementation("com.google.code.gson","gson","2.8.6")

    testImplementation(platform("org.junit:junit-bom:5.7.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.socket", "socket.io-client", "1.0.0")
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
        manifest {
            attributes["Main-Class"] = "com.github.frierenzk.MuppetKt"
        }
        configurations["compileClasspath"].forEach { file: File ->
            from(zipTree(file.absoluteFile))
        }
    }
}

fun getGitID():String {
    val p = Runtime.getRuntime().exec("git rev-parse --short HEAD")
    return p?.inputStream?.bufferedReader()?.readLine() ?: ""
}