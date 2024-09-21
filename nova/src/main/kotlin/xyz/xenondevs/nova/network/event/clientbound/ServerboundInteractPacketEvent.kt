package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ServerboundInteractPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundInteractPacketEvent(
    player: Player,
    packet: ServerboundInteractPacket
) : PlayerPacketEvent<ServerboundInteractPacket>(player, packet) {
    
    var entityId = packet.entityId
        set(value) {
            field = value
            changed = true
        }
    
    var action = packet.action
        set(value) {
            field = value
            changed = true
        }
    
    var isUsingSecondaryAction = packet.isUsingSecondaryAction
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundInteractPacket =
        ServerboundInteractPacket(entityId, isUsingSecondaryAction, action)
    
}