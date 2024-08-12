package xyz.xenondevs.nova.hook.impl.oraxen

import io.th0rgal.oraxen.config.Settings
import io.th0rgal.oraxen.pack.upload.hosts.Polymath
import org.spongepowered.configurate.ConfigurationNode
import xyz.xenondevs.nova.resources.upload.UploadService
import xyz.xenondevs.nova.integration.Hook
import java.io.File

@Hook(plugins = ["Oraxen"])
internal object OraxenUploadService : UploadService {
    
    override val names = listOf("oraxen")
    
    override suspend fun upload(file: File): String {
        val polymath = Polymath(Settings.POLYMATH_SERVER.toString())
        if(!polymath.uploadPack(file)) throw IllegalStateException("Failed to upload pack to polymath!")
        return polymath.minecraftPackURL
    }
    
    override fun loadConfig(cfg: ConfigurationNode) = Unit
    
    override fun disable() = Unit
    
}