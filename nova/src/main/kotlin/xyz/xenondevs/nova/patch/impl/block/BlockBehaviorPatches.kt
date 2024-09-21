package xyz.xenondevs.nova.patch.impl.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.block.data.BlockData
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.nmsBlockState
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelData
import xyz.xenondevs.nova.world.format.WorldDataManager
import java.util.logging.Level as LogLevel

internal object BlockBehaviorPatches : MultiTransformer(BlockStateBase::class) {
    
    override fun transform() {
        VirtualClassPath[BlockStateBase::handleNeighborChanged].delegateStatic(::handleNeighborChanged)
        VirtualClassPath[BlockStateBase::updateShape].delegateStatic(::updateShape)
        VirtualClassPath[BlockStateBase::tick].delegateStatic(::tick)
        VirtualClassPath[BlockStateBase::entityInside].delegateStatic(::entityInside)
    }
    
    @JvmStatic
    fun handleNeighborChanged(thisRef: BlockStateBase, level: Level, pos: BlockPos, sourceBlock: Block, sourcePos: BlockPos, notify: Boolean) {
        val novaPos = pos.toNovaPos(level.world)
        val novaState = WorldDataManager.getBlockState(novaPos)
        if (novaState != null) {
            try {
                novaState.block.handleNeighborChanged(novaPos, novaState, sourcePos.toNovaPos(level.world))
            } catch (e: Exception) {
                LOGGER.log(LogLevel.SEVERE, "Failed to handle neighbor change for $novaState at $novaPos", e)
            }
        } else {
            thisRef.block.neighborChanged(thisRef as BlockState, level, pos, sourceBlock, sourcePos, notify)
        }
    }
    
    @JvmStatic
    fun updateShape(thisRef: BlockStateBase, direction: Direction, neighborState: BlockState, level: LevelAccessor, pos: BlockPos, neighborPos: BlockPos): BlockState {
        if (level is ServerLevel) { // fixme: needs to support WorldGenRegion
            val novaPos = pos.toNovaPos(level.world)
            val novaState = WorldDataManager.getBlockState(novaPos)
            if (novaState != null) {
                try {
                    val newState = novaState.block.updateShape(novaPos, novaState, neighborPos.toNovaPos(level.world))
                    if (newState != novaState) {
                        WorldDataManager.setBlockState(novaPos, newState)
                        return when (val info = newState.modelProvider.info) {
                            is BackingStateConfig -> info.vanillaBlockState
                            is BlockData -> info.nmsBlockState
                            is DisplayEntityBlockModelData -> {
                                novaState.modelProvider.unload(novaPos)
                                newState.modelProvider.load(novaPos)
                                info.hitboxType.nmsBlockState
                            }
                            
                            else -> throw UnsupportedOperationException()
                        }
                    }
                    
                    return thisRef as BlockState
                } catch (e: Exception) {
                    LOGGER.log(LogLevel.SEVERE, "Failed to update shape for $novaState at $novaPos", e)
                }
            }
        }
        
        return thisRef.block.updateShape(thisRef as BlockState, direction, neighborState, level, pos, neighborPos)
    }
    
    @JvmStatic
    fun tick(thisRef: BlockStateBase, level: Level, pos: BlockPos, random: RandomSource) {
        val novaPos = pos.toNovaPos(level.world)
        val novaState = WorldDataManager.getBlockState(novaPos)
        if (novaState != null) {
            try {
                novaState.block.handleScheduledTick(novaPos, novaState)
            } catch (e: Exception) {
                LOGGER.log(LogLevel.SEVERE, "Failed to handle vanilla scheduled tick for $novaState at $pos", e)
            }
        } else {
            thisRef.block.tick(thisRef as BlockState, level as ServerLevel, pos, random)
        }
    }
    
    @JvmStatic
    fun entityInside(thisRef: BlockStateBase, level: Level, pos: BlockPos, entity: Entity) {
        val novaPos = pos.toNovaPos(level.world)
        val novaState = WorldDataManager.getBlockState(novaPos)
        if (novaState != null) {
            try {
                novaState.block.handleEntityInside(novaPos, novaState, entity.bukkitEntity)
            } catch (e: Exception) {
                LOGGER.log(LogLevel.SEVERE, "Failed to handle entity inside for $novaState at $novaPos", e)
            }
        } else {
            thisRef.block.entityInside(thisRef as BlockState, level, pos, entity)
        }
    }
    
}