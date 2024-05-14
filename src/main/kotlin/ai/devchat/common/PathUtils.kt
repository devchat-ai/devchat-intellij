package ai.devchat.common

import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes


object PathUtils {
    val workPath: String = Paths.get(System.getProperty("user.home"), ".chat").toString()
    val workflowPath: String = Paths.get(workPath, "scripts").toString()
    val sitePackagePath: String = Paths.get(workPath, "site-packages").toString()
    val pythonPath: String = "$sitePackagePath:$workflowPath"
    val mambaWorkPath = Paths.get(workPath, "mamba").toString()
    val mambaBinPath = Paths.get(mambaWorkPath, "micromamba").toString()

    fun copyResourceDirToPath(resourceDir: String, outputDir: String): String {
        val uri = javaClass.getResource(resourceDir)!!.toURI()
        val sourcePath = if (uri.scheme == "jar") {
            val fileSystem = try {
                FileSystems.newFileSystem(uri, emptyMap<String, Any>())
            } catch (e: FileSystemAlreadyExistsException) {
                FileSystems.getFileSystem(uri)
            }
            fileSystem.getPath("/$resourceDir")
        } else {
            Paths.get(uri)
        }
        val targetPath = Paths.get(outputDir)

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
}
