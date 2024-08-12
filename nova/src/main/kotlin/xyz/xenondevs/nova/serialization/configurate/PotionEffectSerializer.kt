package xyz.xenondevs.nova.serialization.configurate

import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import xyz.xenondevs.nova.util.data.get
import java.lang.reflect.Type

internal object PotionEffectSerializer : TypeSerializer<PotionEffect?> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): PotionEffect? {
        if (node.raw() == null)
            return null
        
        return PotionEffect(
            node.node("type").get<PotionEffectType>() ?: throw NoSuchElementException("Missing value 'type'"),
            node.node("duration").get<Int>() ?: throw NoSuchElementException("Missing value 'duration'"),
            node.node("amplifier").get<Int>() ?: throw NoSuchElementException("Missing value 'amplifier'"),
            node.node("ambient").getBoolean(true),
            node.node("particles").getBoolean(true),
            node.node("icon").getBoolean(true)
        )
    }
    
    override fun serialize(type: Type, obj: PotionEffect?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }
        
        node.node("type").set(obj.type)
        node.node("duration").raw(obj.duration)
        node.node("amplifier").raw(obj.amplifier)
        node.node("ambient").raw(obj.isAmbient)
        node.node("particles").raw(obj.hasParticles())
        node.node("icon").raw(obj.hasIcon())
    }
    
}

internal object PotionEffectTypeSerializer : TypeSerializer<PotionEffectType?> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): PotionEffectType? {
        if (node.raw() == null)
            return null
        
        return Registry.POTION_EFFECT_TYPE.get(node.get<NamespacedKey>()!!)
            ?: throw IllegalArgumentException("No such potion type: ${node.raw()}")
    }
    
    override fun serialize(type: Type, obj: PotionEffectType?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }
        
        node.set(obj.key)
    }
    
}