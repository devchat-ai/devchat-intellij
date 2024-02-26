plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.1"
    id("org.jetbrains.changelog") version "2.2.0"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
}

group = "ai.devchat"
version = "0.1.7"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.alibaba:fastjson:2.0.47")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.1")
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("io.ktor:ktor-server-core:2.3.8") {exclude("org.slf4j")}
    implementation("io.ktor:ktor-server-netty:2.3.8") {exclude("org.slf4j")}
    implementation("io.ktor:ktor-features:1.6.8") {exclude("org.slf4j")}
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8") {exclude("org.slf4j")}
    implementation("io.ktor:ktor-server-content-negotiation:2.3.8") {exclude("org.slf4j")}
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.2.5")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks.register<Copy>("copyTools") {
    from(layout.projectDirectory.dir("tools")) { exclude(".git/**", ".gitignore") }
    into(layout.buildDirectory.dir("tmp/copyTools/tools"))
}

tasks.register<Copy>("copyWorkflows") {
    from(layout.projectDirectory.dir("workflows")) { exclude(".git/**", ".gitignore") }
    into(layout.buildDirectory.dir("tmp/copyWorkflows/workflows"))
}

tasks.register<Exec>("buildGUI") {
    commandLine("yarn", "idea")
    workingDir(layout.projectDirectory.dir("gui"))
}

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.dir("tmp/copyTools"))
            srcDir(layout.buildDirectory.dir("tmp/copyWorkflows"))
        }
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
    }

    patchPluginXml {
        sinceBuild.set("223")
        untilBuild.set("233.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    processResources {
        dependsOn("copyTools", "copyWorkflows")
    }

    publishPlugin {
        token.set(System.getenv("INTELLIJ_PUBLISH_TOKEN"))
        channels.set(listOf("stable", "eap"))
    }
}
kotlin {
    jvmToolchain(17)
}
