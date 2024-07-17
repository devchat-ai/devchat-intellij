package ai.devchat.common

import ai.devchat.plugin.currentProject
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes


object PathUtils {
    val workspace: String? = currentProject?.basePath
    val workPath: String = Paths.get(System.getProperty("user.home"), ".chat").toString()
    val workflowPath: String = Paths.get(workPath, "scripts").toString()
    val sitePackagePath: String = Paths.get(workPath, "site-packages").toString()
    val pythonPath: String = "$sitePackagePath:$workflowPath"
    val mambaWorkPath = Paths.get(workPath, "mamba").toString()
    val mambaBinPath = Paths.get(mambaWorkPath, "micromamba").toString()
    val toolsPath: String = Paths.get(workPath, "tools").toString()
    val localServicePath: String = Paths.get(sitePackagePath, "devchat", "_service", "main.py").toString()
    val codeEditorBinary: String = "${when {
        OSInfo.OS_ARCH.contains("aarch") || OSInfo.OS_ARCH.contains("arm") -> "aarch64"
        else -> "x86_64"
    }}-${when {
        OSInfo.OS_NAME.contains("win") -> "pc-windows-msvc"
        OSInfo.OS_NAME.contains("darwin") || OSInfo.OS_NAME.contains("mac") -> "apple-darwin"
        OSInfo.OS_NAME.contains("linux") -> "unknown-linux-musl"
        else -> throw RuntimeException("Unsupported OS: ${OSInfo.OS_NAME}")
    }}-code_editor" + if (OSInfo.isWindows) ".exe" else ""

    fun copyResourceDirToPath(resourcePath: String, outputPath: String, overwrite: Boolean = false): String {
        val uri = javaClass.getResource(resourcePath)?.toURI() ?: throw IllegalArgumentException(
            "Resource not found: $resourcePath"
        )
        val sourcePath = if (uri.scheme == "jar") {
            val fileSystem = try {
                FileSystems.newFileSystem(uri, emptyMap<String, Any>())
            } catch (e: FileSystemAlreadyExistsException) {
                FileSystems.getFileSystem(uri)
            }
            fileSystem.getPath("/$resourcePath")
        } else {
            Paths.get(uri)
        }

        val targetPath = Paths.get(outputPath)
        if (!Files.exists(targetPath.parent)) Files.createDirectories(targetPath.parent)
        if (overwrite && Files.exists(targetPath)) targetPath.toFile().deleteRecursively()

        // Handle single file copying
        if (Files.isRegularFile(sourcePath)) {
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
            targetPath.toFile().setExecutable(true)
            return targetPath.toString()
        }

        Files.walkFileTree(sourcePath, object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val target = targetPath.resolve(sourcePath.relativize(dir).toString())
                if (!Files.exists(target)) Files.createDirectory(target)
                Files.setLastModifiedTime(target, attrs.lastModifiedTime())
                return FileVisitResult.CONTINUE
            }

            @Throws(IOException::class)
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val target = targetPath.resolve(sourcePath.relativize(file).toString())
                if (!Files.exists(target) || attrs.lastModifiedTime() > Files.getLastModifiedTime(target)) {
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING)
                    Files.setLastModifiedTime(target, attrs.lastModifiedTime())
                }
                return FileVisitResult.CONTINUE
            }
        })

        return targetPath.toString()
    }

    fun createTempFile(content: String, prefix: String = "devchat-tmp-", suffix: String = ""): String? {
        return try {
            val tempFile = File.createTempFile(prefix, suffix)
            tempFile.writeText(content)
            tempFile.absolutePath
        } catch (e: IOException) {
            Log.error("Failed to create a temporary file: $e")
            return null
        }
    }
}
