package xyz.xenondevs.nova.update

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scheduler.BukkitTask
import org.spongepowered.configurate.ConfigurationNode
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.initialize.Dispatcher
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runAsyncTaskTimer
import xyz.xenondevs.nova.util.unregisterEvents

private val NOVA_DISTRIBUTORS = listOf(
    // GitHub is intentionally omitted because in our current setup releases are created before the jar is uploaded
    ProjectDistributor.hangar("xenondevs/Nova"),
    ProjectDistributor.modrinth("nova-framework")
)

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dispatcher = Dispatcher.ASYNC
)
internal object UpdateReminder : Listener {
    
    private var task: BukkitTask? = null
    private val needsUpdate = HashMap<Addon?, String>()
    private val alreadyNotified = ArrayList<Addon?>()
    
    @InitFun
    private fun init() {
        val cfg = MAIN_CONFIG.node("update_reminder")
        cfg.addUpdateHandler(::reload)
        reload(cfg.get())
    }
    
    private fun reload(cfg: ConfigurationNode) {
        val enabled = cfg.node("enabled").boolean
        val interval = cfg.node("interval").long
        
        if (task == null && enabled) {
            // Enable reminder
            registerEvents()
            task = runAsyncTaskTimer(0, interval, ::checkForUpdates)
        } else if (task != null && !enabled) {
            // Disable reminder
            unregisterEvents()
            task?.cancel()
            task = null
        }
    }
    
    private fun checkForUpdates() {
        checkVersion(null)
        AddonManager.addons.values
            .filter { it.projectDistributors.isNotEmpty() }
            .forEach(UpdateReminder::checkVersion)
        
        if (needsUpdate.isNotEmpty()) {
            needsUpdate.asSequence()
                .filter { it.key !in alreadyNotified }
                .forEach { (addon, resourcePage) ->
                    val name = addon?.description?.name ?: "Nova"
                    LOGGER.warning("You're running an outdated version of $name. Please download the latest version at $resourcePage")
                    alreadyNotified += addon
                }
        }
    }
    
    private fun checkVersion(addon: Addon?) {
        if (addon in needsUpdate)
            return
        
        val distributors: List<ProjectDistributor>
        val currentVersion: Version
        
        if (addon != null) {
            distributors = addon.projectDistributors
            currentVersion = Version(addon.description.version)
        } else {
            distributors = NOVA_DISTRIBUTORS
            currentVersion = NOVA.version
        }
        
        for (distributor in distributors) {
            try {
                val newVersion = distributor.getLatestVersion(currentVersion.isFullRelease)
                if (newVersion > currentVersion) {
                    needsUpdate[addon] = distributor.projectUrl
                    break
                }
            } catch (ignored: Throwable) {
            }
        }
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.hasPermission("nova.misc.updateReminder") && needsUpdate.isNotEmpty()) {
            needsUpdate.forEach { (addon, resourcePage) ->
                val name = addon?.description?.name ?: "Nova"
                val msg = Component.translatable(
                    "nova.outdated_version",
                    NamedTextColor.RED,
                    Component.text(name, NamedTextColor.AQUA),
                    Component.text(resourcePage).clickEvent(ClickEvent.openUrl(resourcePage))
                )
                player.sendMessage(msg)
            }
        }
    }
    
}