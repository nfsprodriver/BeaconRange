import events.ChunkLoad
import org.bukkit.block.Beacon
import org.bukkit.plugin.java.JavaPlugin

class BeaconRange : JavaPlugin(){

    override fun onEnable() {
        saveDefaultConfig()
        val range: Double = config.getDouble("range")
        val duration: Int = config.getInt("duration")
        val combine: Boolean = config.getBoolean("combine")
        server.pluginManager.registerEvents(ChunkLoad(range, duration, combine, logger), this)
    }

    override fun onDisable() {
        server.worlds.forEach { world ->
            world.loadedChunks.forEach { chunk ->
                chunk.tileEntities.forEach { block ->
                    if (block is Beacon) {
                        block.resetEffectRange()
                    }
                }
            }
        }
    }
}