package accesswidener

import java.nio.file.Path
import javax.xml.XMLConstants
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import kotlin.io.path.writeBytes
import kotlin.io.use
import kotlin.text.replace
import kotlin.text.split

fun install(
    repo: Path,
    coordinates: String,
    dependencyCoordinates: List<String>,
    jar: ByteArray,
    sources: Path
) {
    val repoJar = resolveCoordinates(repo, coordinates)
    repoJar.parent.createDirectories()
    repoJar.deleteIfExists()
    repoJar.writeBytes(jar)
    
    if (sources.exists()) {
        val repoSources = resolveCoordinates(repo, coordinates, classifier = "sources")
        repoSources.deleteIfExists()
        sources.copyTo(repoSources)
    }
    
    val pom = resolveCoordinates(repo, coordinates, packaging = "pom")
    pom.outputStream().use { out ->
        val writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out)
        
        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("project")
        writer.writeNamespace("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI)
        writer.writeNamespace("", "http://maven.apache.org/POM/4.0.0")
        writer.writeAttribute(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "schemaLocation", "http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd")
        
        writer.writeElement("modelVersion", "4.0.0")
        writer.writeCoordinates(coordinates)
        
        writer.writeStartElement("dependencies")
        for (dep in dependencyCoordinates) {
            writer.writeStartElement("dependency")
            writer.writeCoordinates(dep)
            writer.writeEndElement()
        }
        writer.writeEndElement()
        
        writer.writeEndElement()
        writer.writeEndDocument()
    }
}

fun resolveCoordinates(repo: Path, coordinates: String, classifier: String = "", packaging: String = "jar"): Path {
    val (group, artifact, version) = coordinates.split(':')
    val dir = repo.resolve("${group.replace('.', '/')}/$artifact/$version")
    val fileName = if (classifier.isBlank()) "$artifact-$version.$packaging" else "$artifact-$version-$classifier.$packaging"
    return dir.resolve(fileName)
}

private fun XMLStreamWriter.writeElement(name: String, value: String) {
    writeStartElement(name)
    writeCharacters(value)
    writeEndElement()
}

private fun XMLStreamWriter.writeCoordinates(coordinates: String) {
    val (group, artifact, version) = coordinates.split(':')
    writeElement("groupId", group)
    writeElement("artifactId", artifact)
    writeElement("version", version)
}