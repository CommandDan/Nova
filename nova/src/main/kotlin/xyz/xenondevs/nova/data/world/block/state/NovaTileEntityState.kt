package xyz.xenondevs.nova.data.world.block.state

import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.buffer.ByteBuffer
import xyz.xenondevs.nova.data.serialization.persistentdata.get
import xyz.xenondevs.nova.data.serialization.persistentdata.getLegacy
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.LegacyCompound
import xyz.xenondevs.nova.integration.utp.UTPIntegration
import xyz.xenondevs.nova.material.TileEntityNovaMaterial
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.TileEntity.Companion.LEGACY_TILE_ENTITY_KEY
import xyz.xenondevs.nova.tileentity.TileEntity.Companion.TILE_ENTITY_KEY
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import java.util.*
import xyz.xenondevs.nova.api.block.NovaTileEntityState as INovaTileEntityState

class NovaTileEntityState : NovaBlockState, INovaTileEntityState {
    
    override val material: TileEntityNovaMaterial
    
    @Volatile
    lateinit var uuid: UUID
    
    @Volatile
    lateinit var ownerUUID: UUID
    
    @Volatile
    lateinit var data: Compound
    
    @Volatile
    internal var legacyData: LegacyCompound? = null
    
    @Volatile
    private var _tileEntity: TileEntity? = null
    override var tileEntity: TileEntity
        get() = _tileEntity ?: throw IllegalStateException("TileEntity is not initialized")
        internal set(value) {
            _tileEntity = value
        }
    
    constructor(pos: BlockPos, material: TileEntityNovaMaterial) : super(pos, material) {
        this.material = material
    }
    
    constructor(material: TileEntityNovaMaterial, ctx: BlockPlaceContext) : super(material, ctx) {
        this.material = material
        this.uuid = UUID.randomUUID()
        this.ownerUUID = ctx.ownerUUID
        this.data = Compound()
        
        val item = ctx.item
        val itemMeta = item.itemMeta!!
        val dataContainer = itemMeta.persistentDataContainer
        
        val legacyGlobalData = dataContainer.getLegacy<LegacyCompound>(LEGACY_TILE_ENTITY_KEY)
        if (legacyGlobalData != null) {
            legacyData = LegacyCompound()
            legacyData!!["global"] = legacyGlobalData
        } else {
            val globalData = ctx.item.itemMeta?.persistentDataContainer?.get<Compound>(TILE_ENTITY_KEY)
            if (globalData != null) data["global"] = globalData
        }
    }
    
    override fun handleInitialized(placed: Boolean) {
        _tileEntity = material.tileEntityConstructor(this)
        tileEntity.handleInitialized(placed)
        
        TileEntityManager.registerTileEntity(this)
        if (placed) UTPIntegration.handleTileEntityPlace(tileEntity)
        
        super.handleInitialized(placed)
    }
    
    override fun handleRemoved(broken: Boolean) {
        super.handleRemoved(broken)
        
        if (_tileEntity != null) {
            tileEntity.saveData()
            tileEntity.handleRemoved(!broken)
            TileEntityManager.unregisterTileEntity(this)
            if (broken) UTPIntegration.handleTileEntityBreak(tileEntity)
            _tileEntity = null
        }
    }
    
    override fun read(buf: ByteBuffer) {
        super.read(buf)
        uuid = buf.readUUID()
        ownerUUID = buf.readUUID()
        data = CBF.read(buf)!!
    }
    
    override fun write(buf: ByteBuffer) {
        super.write(buf)
        buf.writeUUID(uuid)
        buf.writeUUID(ownerUUID)
        
        if (_tileEntity != null)
            tileEntity.saveData()
        
        CBF.write(data, buf)
    }
    
    override fun toString(): String {
        return "NovaTileEntityState(pos=$pos, id=$id, uuid=$uuid, ownerUUID=$ownerUUID, data=$data)"
    }
    
}