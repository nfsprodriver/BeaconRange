package events

import BeaconRange
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Beacon
import org.bukkit.block.BlockState
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.logging.Logger

class ChunkLoad(private val range: Double,
                private val duration: Int,
                private val combine: Boolean,
                private val logger: Logger) : Listener {

    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        effectHandler(event)
    }

    @EventHandler
    fun onWorldLoad(event: ChunkLoadEvent) {
        effectHandler(event)
    }

    private fun effectHandler(event: ChunkLoadEvent) {
        val world: World = event.world
        val loadedChunks = world.loadedChunks
        val beacons: MutableList<Beacon> = mutableListOf<Beacon>()
        loadedChunks.forEach { chunk->
            val chunkBlocks: Array<out BlockState> = chunk.tileEntities
            chunkBlocks.forEach { block ->
                if (block is Beacon) {
                    block.effectRange = range
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
                        if (combine) {
                            beaconEffects[effect1.type] = beaconEffects.getOrDefault(effect1.type, -1) + (effect1.amplifier + 1)
                        } else {
                            beaconEffects[effect1.type] = effect1.amplifier
                        }
                    }
                    val effect2: PotionEffect? = beacon.secondaryEffect
                    if (effect2 != null) {
                        if (combine) {
                            beaconEffects[effect2.type] = beaconEffects.getOrDefault(effect2.type, -1) + (effect2.amplifier + 1)
                        } else {
                            beaconEffects[effect2.type] = effect2.amplifier
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