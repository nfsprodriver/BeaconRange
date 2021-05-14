package events

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.meta.ItemMeta

class RightClick : Listener {
    @EventHandler
    fun onRightClick(event: PlayerInteractEvent) {
        val player: Player = event.player
        if (event.action.name.contains("RIGHT_CLICK")) {
            val inv = player.inventory
            val itemName: String? = inv.itemInMainHand.itemMeta?.displayName
            if (itemName != null) {
                if(itemName.contains("Super Star")) {
                    val meta: ItemMeta = inv.itemInMainHand.itemMeta
                    if (itemName.contains("enabled")) {
                        meta.setDisplayName("Super Star (disabled)")
                    } else {
                        meta.setDisplayName("Super Star (enabled)")
                    }
                    inv.itemInMainHand.itemMeta = meta
                }
            }
        }
    }
}