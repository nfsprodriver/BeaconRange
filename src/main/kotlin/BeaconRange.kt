import org.bukkit.Location
import org.bukkit.block.Beacon
import org.bukkit.block.BlockState
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class BeaconRange : JavaPlugin(){

    override fun onEnable() {
        saveDefaultConfig()
        server.scheduler.scheduleSyncRepeatingTask(this, {
            beaconTask()
        }, 0L, 100L)
    }

    override fun onDisable() {
        /*server.worlds.forEach { world ->
            world.loadedChunks.forEach { chunk ->
                chunk.tileEntities.forEach { block ->
                    if (block is Beacon) {
                        block.resetEffectRange()
                    }
                }
            }
        }*/
    }

    private fun beaconTask() {
        val range: Double = config.getDouble("range")
        val duration: Int = config.getInt("duration")
        val combine: Boolean = config.getBoolean("combine")
        val useDefault: Boolean = config.getBoolean("useDefault")
        val defaultPrim: String? = config.getString("defaultPrim")
        val defaultSec: String? = config.getString("defaultSec")
        server.worlds.forEach { world ->
            val loadedChunks = world.loadedChunks
            val beacons: MutableList<Beacon> = mutableListOf<Beacon>()
            loadedChunks.forEach { chunk->
                val chunkBlocks: Array<out BlockState> = chunk.tileEntities
                chunkBlocks.forEach { block ->
                    if (block is Beacon) {
                        //block.effectRange = range
                        if (useDefault && block.tier >= 4 &&block.primaryEffect == null && block.secondaryEffect == null) {
                            if (defaultPrim != null) {
                                block.setPrimaryEffect(PotionEffectType.getByName(defaultPrim))
                            }
                            if (defaultSec != null) {
                                block.setSecondaryEffect(PotionEffectType.getByName(defaultSec))
                            }
                        }
                        beacons.add(block)
                    }
                }
            }
            world.players.forEach { player ->
                val beaconEffects: MutableMap<PotionEffectType, Int> = mutableMapOf<PotionEffectType, Int>()
                beacons.forEach { beacon ->
                    val playerLoc: Location = player.location
                    val beaconLoc: Location = beacon.location
                    if (playerLoc.distance(beaconLoc) < range) {
                        val effect1: PotionEffect? = beacon.primaryEffect
                        if (effect1 != null) {
                            val effect1Type: PotionEffectType = effect1.type
                            if (combine) {
                                beaconEffects[effect1Type] = beaconEffects.getOrDefault(effect1Type, -1) + (effect1.amplifier + 1)
                            } else {
                                beaconEffects[effect1Type] = effect1.amplifier
                            }
                        }
                        val effect2: PotionEffect? = beacon.secondaryEffect
                        if (effect2 != null) {
                            val effect2Type: PotionEffectType = effect2.type
                            if (combine) {
                                beaconEffects[effect2Type] = beaconEffects.getOrDefault(effect2Type, -1) + (effect2.amplifier + 1)
                            } else {
                                beaconEffects[effect2Type] = effect2.amplifier
                            }
                        }
                    }
                }
                beaconEffects.forEach { (beaconEffectType, beaconEffectAmp) ->
                    val effect: PotionEffect = PotionEffect(beaconEffectType, duration, beaconEffectAmp)
                    player.addPotionEffect(effect)
                }
            }
        }
    }
}