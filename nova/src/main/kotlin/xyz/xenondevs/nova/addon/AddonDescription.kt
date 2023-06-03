package xyz.xenondevs.nova.addon

import org.bukkit.configuration.file.YamlConfiguration
import org.eclipse.aether.graph.Dependency
import xyz.xenondevs.nova.loader.library.NovaLibraryLoader
import xyz.xenondevs.nova.util.data.Version
import java.io.Reader

data class AddonDescription internal constructor(
    val id: String,
    val name: String,
    val version: String,
    val main: String,
    val novaVersion: Version,
    val authors: List<String>,
    val depend: Set<String>,
    val softdepend: Set<String>,
    val repositories: List<String>,
    val libraries: List<Dependency>
) {
    
    internal companion object {
        
        fun deserialize(reader: Reader): AddonDescription {
            val cfg = YamlConfiguration.loadConfiguration(reader)
            
            val id = cfg.getString("id")
            val name = cfg.getString("name")
            val version = cfg.getString("version")
            val main = cfg.getString("main")
            val novaVersion = cfg.getString("novaVersion")
            
            val authors = if ("author" in cfg)
                listOf(cfg.getString("author")!!)
            else if ("authors" in cfg)
                cfg.getStringList("authors")
            else null
            
            require(id != null) { "Missing required field 'id'" }
            require(name != null) { "Missing required field 'name'" }
            require(version != null) { "Missing required field 'version'" }
            require(main != null) { "Missing required field 'main'" }
            require(novaVersion != null) { "Missing required field 'novaVersion'" }
            require(authors != null) { "Missing required field 'author' or 'authors'" }
            require(authors.isNotEmpty()) { "List of authors cannot be empty" }
            
            return AddonDescription(
                id,
                name,
                version,
                main,
                Version(novaVersion),
                authors,
                cfg.getStringList("depend").toHashSet(),
                cfg.getStringList("softdepend").toHashSet(),
                cfg.getStringList("repositories"),
                NovaLibraryLoader.readRequestedLibraries(cfg)
            )
        }
        
    }
    
}