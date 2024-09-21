package xyz.xenondevs.nova.util.bossbar

import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import net.minecraft.network.protocol.game.ClientboundBossEventPacket.AddOperation
import net.minecraft.network.protocol.game.ClientboundBossEventPacket.UpdateNameOperation
import net.minecraft.network.protocol.game.ClientboundBossEventPacket.UpdateProgressOperation
import net.minecraft.network.protocol.game.ClientboundBossEventPacket.UpdatePropertiesOperation
import net.minecraft.network.protocol.game.ClientboundBossEventPacket.UpdateStyleOperation
import net.minecraft.world.BossEvent
import xyz.xenondevs.nova.network.AddOperation
import xyz.xenondevs.nova.util.component.adventure.toAdventureComponent
import xyz.xenondevs.nova.util.component.adventure.toNMSComponent
import java.util.*

class BossBar(
    val id: UUID,
    name: Component = Component.text(""),
    progress: Float = 0.0f,
    color: BossEvent.BossBarColor = BossEvent.BossBarColor.WHITE,
    overlay: BossEvent.BossBarOverlay = BossEvent.BossBarOverlay.PROGRESS,
    darkenScreen: Boolean = false,
    playMusic: Boolean = false,
    createWorldFog: Boolean = false
) {
    
    var name: Component = name
        set(value) {
            field = value
            
            _addPacket = null
            _updateNamePacket = null
        }
    var progress: Float = progress
        set(value) {
            field = value
            
            _addPacket = null
            _updateProgressPacket = null
        }
    var color: BossEvent.BossBarColor = color
        set(value) {
            field = value
            
            _addPacket = null
            _updateStylePacket = null
        }
    var overlay: BossEvent.BossBarOverlay = overlay
        set(value) {
            field = value
            
            _addPacket = null
            _updateStylePacket = null
        }
    var darkenScreen: Boolean = darkenScreen
        set(value) {
            field = value
            
            _addPacket = null
            _updatePropertiesPacket = null
        }
    var playMusic: Boolean = playMusic
        set(value) {
            field = value
            
            _addPacket = null
            _updatePropertiesPacket = null
        }
    var createWorldFog: Boolean = createWorldFog
        set(value) {
            field = value
            
            _addPacket = null
            _updatePropertiesPacket = null
        }
    
    private var _addPacket: ClientboundBossEventPacket? = null
    val addPacket: ClientboundBossEventPacket
        get() {
            if (_addPacket == null) {
                _addPacket = ClientboundBossEventPacket(id, AddOperation(name, progress, color, overlay, darkenScreen, playMusic, createWorldFog))
            }
            return _addPacket!!
        }
    
    private var _updateNamePacket: ClientboundBossEventPacket? = null
    val updateNamePacket: ClientboundBossEventPacket
        get() {
            if (_updateNamePacket == null) {
                _updateNamePacket = ClientboundBossEventPacket(id, UpdateNameOperation(name.toNMSComponent()))
            }
            return _updateNamePacket!!
        }
    
    private var _updateProgressPacket: ClientboundBossEventPacket? = null
    val updateProgressPacket: ClientboundBossEventPacket
        get() {
            if (_updateProgressPacket == null) {
                _updateProgressPacket = ClientboundBossEventPacket(id, UpdateProgressOperation(progress))
            }
            return _updateProgressPacket!!
        }
    
    private var _updateStylePacket: ClientboundBossEventPacket? = null
    val updateStylePacket: ClientboundBossEventPacket
        get() {
            if (_updateStylePacket == null) {
                _updateStylePacket = ClientboundBossEventPacket(id, UpdateStyleOperation(color, overlay))
            }
            return _updateStylePacket!!
        }
    
    private var _updatePropertiesPacket: ClientboundBossEventPacket? = null
    val updatePropertiesPacket: ClientboundBossEventPacket
        get() {
            if (_updatePropertiesPacket == null) {
                _updatePropertiesPacket = ClientboundBossEventPacket(id, UpdatePropertiesOperation(darkenScreen, playMusic, createWorldFog))
            }
            return _updatePropertiesPacket!!
        }
    
    val removePacket = ClientboundBossEventPacket.createRemovePacket(id)
    
    companion object {
        
        fun of(id: UUID, operation: AddOperation) = BossBar(
            id,
            operation.name.toAdventureComponent(),
            operation.progress,
            operation.color,
            operation.overlay,
            operation.darkenScreen,
            operation.playMusic,
            operation.createWorldFog
        )
        
    }
    
}