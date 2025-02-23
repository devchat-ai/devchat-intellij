
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20"
}

group = "ai.devchat"
version = "0.3.3"

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

val pluginID: String? by project
val assistantNameZH: String? by project
val assistantNameEN: String? by project
val pluginIcon: String? by project
val pluginIconDark: String? by project
val toolWindowIcon: String? by project

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.encoding = "UTF-8"
    }
    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    patchPluginXml {
        sinceBuild.set("223")
        untilBuild.set("242.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    processResources {
        dependsOn("copyTools", "copyWorkflows")
        pluginIcon?.takeIf { it.isNotBlank() }?.let {
            from(it) {
                rename { "META-INF/pluginIcon.svg" }
            }
        }
        pluginIconDark?.takeIf { it.isNotBlank() }?.let {
            from(it) {
                rename { "META-INF/pluginIcon_dark.svg" }
            }
        }
        toolWindowIcon?.takeIf { it.isNotBlank() }?.let {
            from(it) {
                rename { "icons/toolWindowIcon.svg" }
            }
        }
        filesMatching("plugin.xml") {
            expand(
                "PLUGIN_ID" to (pluginID?.takeIf { it.isNotBlank() } ?: "ai.devchat.plugin"),
                "ASSISTANT_NAME_ZH" to (assistantNameZH?.takeIf { it.isNotBlank() } ?: "DevChat"),
                "ASSISTANT_NAME_EN" to (assistantNameEN?.takeIf { it.isNotBlank() } ?: "DevChat"),
                "default" to "\$default"
            )
        }
        filesMatching("intentionDescriptions/AskIssueIntention/description.html") {
            expand(
                "ASSISTANT_NAME_EN" to (assistantNameEN?.takeIf { it.isNotBlank() } ?: "DevChat"),
            )
        }
        filesMatching("intentionDescriptions/FixIssueIntention/description.html") {
            expand(
                "ASSISTANT_NAME_EN" to (assistantNameEN?.takeIf { it.isNotBlank() } ?: "DevChat"),
            )
        }
        filesMatching("messages/DevChatBundle.properties") {
            expand(
                "PLUGIN_ID" to (pluginID?.takeIf { it.isNotBlank() } ?: "ai.devchat.plugin"),
                "ASSISTANT_NAME_ZH" to (assistantNameZH?.takeIf { it.isNotBlank() } ?: "DevChat"),
                "ASSISTANT_NAME_EN" to (assistantNameEN?.takeIf { it.isNotBlank() } ?: "DevChat"),
            )
        }
    }

    publishPlugin {
        token.set(System.getenv("INTELLIJ_PUBLISH_TOKEN"))
        channels.set(listOf("eap", "stable"))
    }

    buildSearchableOptions {
        enabled = false
    }
}
