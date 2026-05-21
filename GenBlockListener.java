package com.opgens.plugin.listeners;

import com.opgens.plugin.OPGens;
import com.opgens.plugin.generators.GenType;
import com.opgens.plugin.generators.PlacedGen;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Detecta cuando un jugador coloca o rompe un generador.
 * - Colocar: si el item en mano es un gen valido, registra el gen
 * - Romper:  si el bloque es un gen, lo cancela y devuelve el item
 */
public class GenBlockListener implements Listener {

    private final OPGens plugin;

    public GenBlockListener(OPGens plugin) {
        this.plugin = plugin;
    }

    // ══════════════════════════════════════════════════════════
    //  COLOCAR BLOQUE
    // ══════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        ItemStack itemInHand = e.getItemInHand();

        // Verificar si el item es un gen valido
        String genId = plugin.getShopManager().extractGenId(itemInHand);
        if (genId == null) return;

        GenType type = plugin.getGenManager().getGenType(genId);
        if (type == null) return;

        Location loc = e.getBlockPlaced().getLocation();

        // Intentar colocar el gen
        boolean placed = plugin.getGenManager().placeGen(player, loc, genId);

        if (!placed) {
            // Limite alcanzado — cancelar colocacion
            e.setCancelled(true);
            int max = plugin.getConfig().getInt("max-gens-per-player", 10);
            player.sendMessage(plugin.getMessage("gen-limit",
                    "{max}", String.valueOf(max)));
            return;
        }

        // Consumir el item de la mano (ya fue colocado)
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (itemInHand.getAmount() > 1) {
                itemInHand.setAmount(itemInHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }

        player.sendMessage(plugin.getMessage("gen-placed"));
    }

    // ══════════════════════════════════════════════════════════
    //  ROMPER BLOQUE
    // ══════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Location loc = e.getBlock().getLocation();

        // Verificar si es un gen colocado
        if (!plugin.getGenManager().isGenBlock(loc)) return;

        PlacedGen pg = plugin.getGenManager().getPlacedGen(loc);
        if (pg == null) return;

        // Solo el dueño o un admin puede romperlo
        boolean isOwner = pg.getOwnerUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("opgens.admin");

        if (!isOwner && !isAdmin) {
            e.setCancelled(true);
            player.sendMessage(OPGens.colorize(
                plugin.getConfig().getString("prefix", "&8[&aOPGens&8]") +
                " &cEste generador no es tuyo."));
            return;
        }

        // Cancelar el drop vanilla del bloque
        e.setDropItems(false);

        // Remover el gen del manager (cancela la tarea de drop)
        String genTypeId = pg.getGenTypeId();
        plugin.getGenManager().removeGen(loc);

        // Devolver el item de gen al jugador
        if (player.getGameMode() != GameMode.CREATIVE) {
            GenType type = plugin.getGenManager().getGenType(genTypeId);
            if (type != null) {
                ItemStack genItem = plugin.getShopManager().buildShopItem(type);
                player.getInventory().addItem(genItem);
            }
        }

        player.sendMessage(plugin.getMessage("gen-removed"));
    }

    // ══════════════════════════════════════════════════════════
    //  PROTECCION CONTRA EXPLOSIONES
    // ══════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent e) {
        // Remover de la lista de bloques a destruir cualquier gen
        e.blockList().removeIf(block ->
            plugin.getGenManager().isGenBlock(block.getLocation())
        );
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(block ->
            plugin.getGenManager().isGenBlock(block.getLocation())
        );
    }
}
