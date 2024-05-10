package ai.devchat.common

import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes


object PathUtils {
    val workPath: String = Paths.get(System.getProperty("user.home"), ".chat").toString()
    val workflowPath: String = Paths.get(workPath, "workflows", "scripts").toString()
    val sitePackagePath: String = Paths.get(workPath, "site-packages").toString()
    val pythonPath: String = "$sitePackagePath:$workflowPath"
    val mambaWorkPath = Paths.get(workPath, "mamba").toString()
    val mambaBinPath = Paths.get(mambaWorkPath, "micromamba").toString()

    fun copyResourceDirToPath(resourceDir: String, outputPath: String, overwrite: Boolean = false): String {
        val uri = javaClass.getResource(resourceDir)!!.toURI()
        val path = if (uri.scheme == "jar") {
            val fileSystem = try {
                FileSystems.newFileSystem(uri, emptyMap<String, Any>())
            } catch (e: FileSystemAlreadyExistsException) {
                FileSystems.getFileSystem(uri)
            }
            fileSystem.getPath("/$resourceDir")
        } else {
            Paths.get(uri)
        }
        if (overwrite) Paths.get(outputPath).toFile().deleteRecursively()

        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val relativeDir = dir.toString().substring(path.toString().length)
                val targetPath = Paths.get(outputPath, relativeDir)
                return if (!Files.exists(targetPath)) {
                    Files.createDirectory(targetPath)
                    FileVisitResult.CONTINUE
                } else {
                    if (relativeDir == "") FileVisitResult.CONTINUE else FileVisitResult.SKIP_SUBTREE
                }
            }

            @Throws(IOException::class)
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val relativePath = file.toString().substring(path.toString().length)
                val targetFilePath = Paths.get(outputPath, relativePath)
                if (!Files.exists(targetFilePath)) {
                    Files.copy(file, targetFilePath)
                }
                return FileVisitResult.CONTINUE
            }
        })

        return outputPath
    }
}
