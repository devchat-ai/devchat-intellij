import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.10"
}

group = "ai.devchat"
version = "0.3.2"

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.12"

dependencies {
    implementation("com.alibaba:fastjson:2.0.51")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-features:1.6.8")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

configurations.all {
    exclude("org.slf4j", "slf4j-api")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.2.5")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks.register<Copy>("copyTools") {
    from(layout.projectDirectory.dir("tools")) {
        include(
            "gpt-token/**",
            "micromamba-*/**",
            "python-3.11.6-embed-amd64/**",
            "site-packages/**",
            "sonar-rspec/**",
            "code-editor/**",
            "replace.sh"
        )
    }
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
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("223")
        untilBuild.set("241.*")
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
        channels.set(listOf("eap", "stable"))
    }

    buildSearchableOptions {
        enabled = false
    }
}
