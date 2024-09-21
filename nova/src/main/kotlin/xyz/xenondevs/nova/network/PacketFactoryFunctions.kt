package xyz.xenondevs.nova.network

import io.netty.buffer.Unpooled
import net.kyori.adventure.text.Component
import net.minecraft.core.Holder
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.BossEvent
import xyz.xenondevs.nova.util.RegistryFriendlyByteBuf
import xyz.xenondevs.nova.util.component.adventure.toNMSComponent

fun ClientboundSetPassengersPacket(vehicle: Int, passengers: IntArray): ClientboundSetPassengersPacket {
    val buffer = FriendlyByteBuf(Unpooled.buffer())
    buffer.writeVarInt(vehicle)
    buffer.writeVarIntArray(passengers)
    return ClientboundSetPassengersPacket.STREAM_CODEC.decode(buffer)
}

internal fun AddOperation(
    name: Component,
    progress: Float, 
    color: BossEvent.BossBarColor, 
    overlay: BossEvent.BossBarOverlay,
    darkenScreen: Boolean,
    playMusic: Boolean,
    createWorldFog: Boolean
): ClientboundBossEventPacket.AddOperation {
    val buf = RegistryFriendlyByteBuf()
    ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, name.toNMSComponent())
    buf.writeFloat(progress)
    buf.writeEnum(color)
    buf.writeEnum(overlay)
    var result = 0
    if (darkenScreen)
        result = result or 1
    if (playMusic)
        result = result or 2
    if (createWorldFog)
        result = result or 4
    buf.writeByte(result)
    return ClientboundBossEventPacket.AddOperation(buf)
}

fun ClientboundSoundEntityPacket(sound: Holder<SoundEvent>, source: SoundSource, entityId: Int, volume: Float, pitch: Float, seed: Long): ClientboundSoundEntityPacket {
    val buf = RegistryFriendlyByteBuf()
    SoundEvent.STREAM_CODEC.encode(buf, sound)
    buf.writeEnum(source)
    buf.writeVarInt(entityId)
    buf.writeFloat(volume)
    buf.writeFloat(pitch)
    buf.writeLong(seed)
    
    return ClientboundSoundEntityPacket.STREAM_CODEC.decode(buf)
}

fun ServerboundPlaceRecipePacket(containerId: Int, recipe: ResourceLocation, shiftDown: Boolean): ServerboundPlaceRecipePacket {
    val buf = FriendlyByteBuf(Unpooled.buffer())
    buf.writeByte(containerId)
    buf.writeResourceLocation(recipe)
    buf.writeBoolean(shiftDown)
    return ServerboundPlaceRecipePacket.STREAM_CODEC.decode(buf)
}