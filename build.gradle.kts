plugins {
    kotlin("jvm") version "1.4.10"
}

group = "com.github.frierenzk"
version = "0.1.1".let {
    "$it${if (getGitID().isBlank()) "" else "-${getGitID()}"}"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.9")

    implementation("com.corundumstudio.socketio","netty-socketio","1.7.18")
    implementation("com.google.code.gson","gson","2.8.6")

    testImplementation(platform("org.junit:junit-bom:5.7.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
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
            attributes["Main-Class"] = "com.github.frierenzk.Muppet"
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