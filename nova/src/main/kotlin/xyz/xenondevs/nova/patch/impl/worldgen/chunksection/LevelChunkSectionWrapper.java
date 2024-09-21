package xyz.xenondevs.nova.patch.impl.worldgen.chunksection;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.nova.patch.Patcher;
import xyz.xenondevs.nova.patch.adapter.LcsWrapperAdapter;
import xyz.xenondevs.nova.util.reflection.ReflectionUtils;
import xyz.xenondevs.nova.world.BlockPos;
import xyz.xenondevs.nova.world.block.migrator.BlockMigrator;
import xyz.xenondevs.nova.world.format.WorldDataManager;
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlockState;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/**
 * Wrapper for {@link LevelChunkSection}s to allow placing {@link WrapperBlockState}s.
 * <p>
 * <h2>! UPDATE {@link Patcher Patcher.injectedClasses} WHEN MOVING THIS CLASS !</h2>
 * <h2> Also check out {@link LcsWrapperAdapter} when refactoring </h2>
 */
@ApiStatus.Internal
public class LevelChunkSectionWrapper extends LevelChunkSection {
    
    // Pufferfish
    private static final MethodHandle GET_FLUID_STATE_COUNT;
    private static final MethodHandle SET_FLUID_STATE_COUNT;
    
    static {
        try {
            Field fluidStateCount = ReflectionUtils.getFieldOrNull(LevelChunkSection.class, "fluidStateCount");
            if (fluidStateCount != null) {
                var lookup = MethodHandles.privateLookupIn(LevelChunkSection.class, MethodHandles.lookup());
                GET_FLUID_STATE_COUNT = lookup.unreflectGetter(fluidStateCount);
                SET_FLUID_STATE_COUNT = lookup.unreflectSetter(fluidStateCount);
            } else {
                GET_FLUID_STATE_COUNT = null;
                SET_FLUID_STATE_COUNT = null;
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    private final Level level;
    private final ChunkPos chunkPos;
    private final int bottomBlockY;
    private final LevelChunkSection delegate;
    private boolean migrationActive = false;
    
    public LevelChunkSectionWrapper(Level level, ChunkPos chunkPos, int bottomBlockY, LevelChunkSection delegate) throws Throwable {
        super(delegate.states, delegate.biomes);
        this.level = level;
        this.chunkPos = chunkPos;
        this.bottomBlockY = bottomBlockY;
        this.delegate = delegate instanceof LevelChunkSectionWrapper w ? w.delegate : delegate;
        recalcBlockCounts();
        this.tickingBlocks = delegate.tickingBlocks;
    }
    
    @Override
    public @NotNull BlockState setBlockState(int relX, int relY, int relZ, @NotNull BlockState state) {
        return setBlockState(relX, relY, relZ, state, true);
    }
    
    @Override
    public @NotNull BlockState setBlockState(int relX, int relY, int relZ, @NotNull BlockState state, boolean sync) {
        var pos = getBlockPos(relX, relY, relZ);
        
        if (state instanceof WrapperBlockState wrappedState) {
            WorldDataManager.INSTANCE.setBlockState(pos, wrappedState.getNovaState());
            return Blocks.AIR.defaultBlockState();
        }
        
        BlockState migrated = state;
        if (migrationActive) {
            migrated = BlockMigrator.migrateBlockState(pos, state);
        }
        
        var previous = delegate.setBlockState(relX, relY, relZ, migrated, sync);
        
        if (migrationActive) {
            BlockMigrator.handleBlockStatePlaced(pos, previous, state);
        }
        
        copyBlockCounts();
        return previous;
    }
    
    private BlockPos getBlockPos(int relX, int relY, int relZ) {
        return new BlockPos(
            level.getWorld(),
            relX + chunkPos.getMinBlockX(),
            relY + bottomBlockY,
            relZ + chunkPos.getMinBlockZ()
        );
    }
    
    @Override
    public void recalcBlockCounts() {
        if (delegate == null) return;
        delegate.recalcBlockCounts();
        copyBlockCounts();
    }
    
    @Override
    public void read(@NotNull FriendlyByteBuf buf) {
        delegate.read(buf);
        copyBlockCounts();
    }
    
    public int getBottomBlockY() {
        return bottomBlockY;
    }
    
    public boolean isMigrationActive() {
        return migrationActive;
    }
    
    public void setMigrationActive(boolean migrationActive) {
        this.migrationActive = migrationActive;
    }
    
    private void copyBlockCounts() {
        try {
            nonEmptyBlockCount = delegate.nonEmptyBlockCount;
            tickingBlockCount = delegate.tickingBlockCount;
            tickingFluidCount = delegate.tickingFluidCount;
            specialCollidingBlocks = delegate.specialCollidingBlocks;
            if (GET_FLUID_STATE_COUNT != null && SET_FLUID_STATE_COUNT != null) {
                SET_FLUID_STATE_COUNT.invoke(this, GET_FLUID_STATE_COUNT.invoke(delegate));
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    
}
