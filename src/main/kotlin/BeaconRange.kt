import events.RightClick
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Beacon
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.inventory.ShapedRecipe

import org.bukkit.inventory.ItemStack
import java.util.function.Predicate


class BeaconRange : JavaPlugin(){

    override fun onEnable() {
        saveDefaultConfig()
        server.pluginManager.registerEvents(RightClick(), this)
        server.scheduler.scheduleSyncRepeatingTask(this, {
            beaconTask()
        }, 0L, 100L)
        if (config.getBoolean("useSuperStar")) {
            addSSRecipe()
        }
    }

    override fun onDisable() {
        remSSRecipe()
    }

    private fun generateConfig() {

    }

    private fun addSSRecipe() {
        val superStar = ItemStack(Material.NETHER_STAR)
        val ssMeta = superStar.itemMeta
        ssMeta.setDisplayName("Super Star (enabled)")
        superStar.setItemMeta(ssMeta)
        superStar.lore = listOf("No effects")
        val superStarRec = ShapedRecipe(NamespacedKey(this, "superStar"), superStar)
        superStarRec.shape("SSS", "SSS", "SSS")
        superStarRec.setIngredient('S', Material.NETHER_STAR)
        server.addRecipe(superStarRec)
    }

    private fun remSSRecipe() {
        server.removeRecipe(NamespacedKey(this, "superStar"))
    }

    private fun beaconTask() {
        val range: Double = config.getDouble("range")
        val duration: Int = config.getInt("duration")
        val combine: Boolean = config.getBoolean("combine")
        val useDefault: Boolean = config.getBoolean("useDefault")
        val defaultPrim: String? = config.getString("defaultPrim")
        val defaultSec: String? = config.getString("defaultSec")
        val useSuperStar: Boolean = config.getBoolean("useSuperStar")
        server.worlds.forEach { world ->
            val loadedChunks = world.loadedChunks
            val beacons: MutableList<Beacon> = mutableListOf()
            loadedChunks.forEach { chunk->
                val chunkBlocks: Array<out BlockState> = chunk.tileEntities
                chunkBlocks.forEach { block ->
                    if (block is Beacon) {
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
                val beaconEffects: MutableMap<PotionEffectType, Int> = mutableMapOf()
                beacons.forEach { beacon ->
                    val playerLoc: Location = player.location
                    val beaconLoc: Location = beacon.location
                    if (isActive(beacon) && playerLoc.distance(beaconLoc) < range) {
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
                        if (useSuperStar) {
                            addSuperStarEffects(player)
                        }
                    }
                }
                beaconEffects.forEach { (beaconEffectType, beaconEffectAmp) ->
                    val inv: Inventory = player.inventory
                    val stars: List<ItemStack> = inv.contents.filter { itemStack -> itemStack?.itemMeta?.displayName?.contains("Super Star") == true }
                    if (stars.isEmpty() || stars.first().itemMeta.displayName.contains("enabled")) {
                        val effect = PotionEffect(beaconEffectType, duration, minOf(beaconEffectAmp, config.getInt("maxAmp").minus(1)))
                        player.addPotionEffect(effect)
                    }
                }
                if (useSuperStar) {
                    setSuperStar(player, beaconEffects)
                }
            }
            world.loadedChunks.forEach { chunk ->
                val villagers: List<Villager> = chunk.entities.filterIsInstance<Villager>()
                villagers.forEach { villager ->
                    val beaconEffects: MutableMap<PotionEffectType, Int> = mutableMapOf()
                    beacons.forEach { beacon ->
                        val villagerLoc: Location = villager.location
                        val beaconLoc: Location = beacon.location
                        if (isActive(beacon) && villagerLoc.distance(beaconLoc) < range) {
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
                        val effect = PotionEffect(beaconEffectType, duration, minOf(beaconEffectAmp, config.getInt("maxAmp").minus(1)))
                        if ((effect.type == PotionEffectType.REGENERATION || effect.type == PotionEffectType.DAMAGE_RESISTANCE)) {
                            villager.addPotionEffect(effect)
                        }
                    }
                }
            }
        }
    }

    private fun setSuperStar (player: Player, beaconEffects: MutableMap<PotionEffectType, Int>) {
        val inv = player.inventory
        if (inv.itemInOffHand.itemMeta?.displayName?.contains("Super Star") == true) {
            if (beaconEffects.keys.count() > 0) {
                val lore = mutableListOf<String>()
                beaconEffects.forEach { beaconEffect ->
                    lore.add(beaconEffect.key.name+": "+beaconEffect.value.toString())
                }
                inv.itemInOffHand.lore = lore
            }
        }
    }

    private fun isActive(beacon: Beacon): Boolean {
        val aboveBeacon: Block = beacon.location.add(0.0, 1.0, 0.0).block
        return aboveBeacon.isEmpty || aboveBeacon.isLiquid || aboveBeacon.type.isTransparent
    }

    private fun addSuperStarEffects(player: Player) {
        val inv = player.inventory
        val itemName: String? = inv.itemInOffHand.itemMeta?.displayName
        if (itemName != null && itemName.contains("Super Star") && itemName.contains("enabled")) {
            inv.itemInOffHand.lore?.forEach { starEffect ->
                val split: List<String> = starEffect.split(": ")
                if (split.count() == 2) {
                    val effectType: PotionEffectType = PotionEffectType.getByName(split[0])!!
                    val duration: Int = config.getInt("duration")
                    val amp: Int = split[1].toInt()
                    val potionEffect = PotionEffect(effectType, duration, amp)
                    player.addPotionEffect(potionEffect)
                }
            }
        }
    }
}