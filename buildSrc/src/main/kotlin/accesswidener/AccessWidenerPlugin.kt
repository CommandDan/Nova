package accesswidener

import net.fabricmc.accesswidener.AccessWidener
import net.fabricmc.accesswidener.AccessWidenerClassVisitor
import net.fabricmc.accesswidener.AccessWidenerReader
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.register
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.notExists
import kotlin.io.path.readBytes

private const val GROUP_ID = "xyz.xenondevs.nova"
private const val ARTIFACT_ID = "paper-server"

class AccessWidenerPlugin : Plugin<Project> {
    
    override fun apply(target: Project) {
        val repoDir = target.layout.projectDirectory.dir(".gradle/caches/nova-access-widener/").asFile.toPath()
        
        target.repositories {
            maven(repoDir) {
                content { includeModule(GROUP_ID, ARTIFACT_ID) }
            }
        }
        
        val accessWidenedServer = target.configurations.create("accessWidenedServer") {
            defaultDependencies {
                val coordinates = widenAndInstall(target, repoDir)
                add(target.dependencies.create(coordinates))
            }
        }
        
        val compileOnly = target.configurations.getByName("compileOnly")
        compileOnly.extendsFrom(accessWidenedServer)
        compileOnly.exclude("io.papermc.paper", "paper-server")
    }
    
    private fun widenAndInstall(project: Project, repo: Path): String {
        val devBundle = readDevBundle(project)
        val (_, artifactId, version) = devBundle.mappedServerCoordinates.split(':')
        
        val mojangMappedServer = project.configurations.getByName("mojangMappedServer")
        val serverArtifacts = mojangMappedServer.incoming.artifacts.artifactFiles
        val binJar = serverArtifacts.first { it.name == "$artifactId-$version.jar" }.toPath()
        val sourcesJar = binJar.parent.resolve(binJar.nameWithoutExtension + "-sources.jar")
        
        val accessWidener = project.layout.projectDirectory.asFile.toPath().resolve("src/main/resources/nova.accesswidener")
        
        val hash = hashServer(binJar, accessWidener)
        
        val coordinates = "$GROUP_ID:$ARTIFACT_ID:$version-$hash"
        if (resolveCoordinates(repo, coordinates).notExists()) {
            install(repo, coordinates, devBundle.dependencies, applyAccessWideners(accessWidener, binJar), sourcesJar)
            println("access-widened server installed")
        } else {
            println("access-widened server was cached")
        }
        
        return coordinates
    }
    
    private fun readDevBundle(project: Project): DevBundleInfo {
        val paperDevBundle = project.configurations.getByName("paperweightDevelopmentBundle")
            .incoming.artifacts.artifactFiles.first()
        ZipInputStream(paperDevBundle.inputStream()).use { zin ->
            generateSequence { zin.nextEntry }
                .forEach { entry ->
                    if (entry.name == "config.json") {
                        val reader = BufferedReader(InputStreamReader(zin))
                        return DevBundleInfo.fromJson(reader)
                    }
                }
        }
        
        throw IllegalStateException("config.json not found in paperweight development bundle")
    }
    
    private fun hashServer(jar: Path, accessWideners: Path): String {
        val md = MessageDigest.getInstance("MD5")
        md.update(jar.readBytes())
        md.update(accessWideners.readBytes())
        return HexFormat.of().formatHex(md.digest())
    }
    
    private fun applyAccessWideners(accessWidener: Path, jar: Path): ByteArray {
        val widener = readAccessWidenerFile(accessWidener)
        
        val out = ByteArrayOutputStream()
        ZipInputStream(jar.inputStream()).use { zin ->
            val zout = ZipOutputStream(out)
            generateSequence { zin.nextEntry }
                .forEach { entry ->
                    zout.putNextEntry(entry)
                    if (entry.name.endsWith(".class")) {
                        val classReader = ClassReader(zin)
                        val classWriter = ClassWriter(0)
                        val widenerVisitor = AccessWidenerClassVisitor.createClassVisitor(Opcodes.ASM9, classWriter, widener)
                        classReader.accept(widenerVisitor, 0)
                        zout.write(classWriter.toByteArray())
                    } else {
                        zin.copyTo(zout)
                    }
                    zout.closeEntry()
                }
            zout.close()
        }
        
        return out.toByteArray()
    }
    
    private fun readAccessWidenerFile(file: Path): AccessWidener {
        val widener = AccessWidener()
        file.bufferedReader().use { reader -> AccessWidenerReader(widener).read(reader) }
        return widener
    }
    
}