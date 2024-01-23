package ai.devchat.common

import java.util.*

object OSInfo {
    val OS_NAME: String = System.getProperty("os.name").lowercase(Locale.getDefault())
    val OS_ARCH: String = System.getProperty("os.arch")

    val isWindows: Boolean = OS_NAME.contains("win")
    val platform: String  = when {
        OS_NAME.contains("win") ->
            if (OS_ARCH.contains("64")) "win-64"
            else throw RuntimeException("Unsupported architecture: $OS_ARCH")

        OS_NAME.contains("darwin") || OS_NAME.contains("mac") -> when {
            OS_ARCH.contains("arm") -> "osx-arm64"
            OS_ARCH.contains("64") -> "osx-64"
            else -> throw RuntimeException("Unsupported architecture: $OS_ARCH")
        }

        OS_NAME.contains("linux") -> when {
            OS_ARCH.contains("x64") -> "linux-64"
            OS_ARCH.contains("amd64") -> "linux-64"
            OS_ARCH.contains("ppc64le") -> "linux-ppc64le"
            OS_ARCH.contains("aarch64") -> "linux-aarch64"
            else -> throw RuntimeException("Unsupported architecture: $OS_ARCH")
        }

        else -> throw RuntimeException("Unsupported operating system: $OS_NAME")
    }
}