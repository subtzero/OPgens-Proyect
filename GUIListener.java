package com.opgens.plugin.gui;

import com.opgens.plugin.OPGens;
import com.opgens.plugin.managers.ShopManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener central que maneja todos los clicks dentro de los GUIs del plugin
 * y el input de chat para /gensdrops.
 */
public class GUIListener implements Listener {

    private final OPGens plugin;

    public GUIListener(OPGens plugin) {
        this.plugin = plugin;
    }

    // ══════════════════════════════════════════════════════════
    //  CLICKS EN INVENTARIO
    // ══════════════════════════════════════════════════════════

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        String title = e.getView().getTitle();

        // ── GenShop ──────────────────────────────────────────
        if (title.equals(OPGens.colorize("&8✦ &aGens Shop &8✦"))) {
            e.setCancelled(true);

            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) return;

            String genId = plugin.getShopManager().extractGenId(clicked);
            if (genId == null) return;

            // Intentar compra
            ShopManager.PurchaseResult result = plugin.getShopManager().purchaseGen(player, genId);

            switch (result) {
                case SUCCESS -> {
                    String genName = plugin.getGenManager().getGenType(genId).getDisplayName();
                    double price = plugin.getGenManager().getGenType(genId).getShopPrice();
                    player.sendMessage(plugin.getMessage("gen-purchased",
                            "{gen}", genName,
                            "{price}", String.valueOf(price)));
                    // Refrescar shop para actualizar colores de precio
                    plugin.getShopManager(); // ya fue procesado
                    new GenShopGUI(plugin).open(player);
                }
                case NO_MONEY -> player.sendMessage(plugin.getMessage("not-enough-money",
                        "{amount}", String.valueOf(plugin.getGenManager().getGenType(genId).getShopPrice())));
                case NO_SPACE -> player.sendMessage(OPGens.colorize(
                        plugin.getConfig().getString("prefix") + " &cNo tienes espacio en el inventario."));
                case NOT_PURCHASABLE -> player.sendMessage(plugin.getMessage("only-first-tier"));
                case INVALID -> {}
            }
        }

        // ── GensDrops (admin) ─────────────────────────────────
        else if (GensDropsGUI.isGensDrops(title)) {
            e.setCancelled(true);

            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) return;

            boolean isShift = e.isShiftClick();
            plugin.getShopManager(); // referencia
            GensDropsGUI gui = new GensDropsGUI(plugin);

            // Delegar al GUI
            // Como GensDropsGUI maneja su propio estado, usamos la instancia del plugin
            // La instancia real se obtiene del command handler; aqui solo cancelamos clicks en filler
        }
    }

    // ══════════════════════════════════════════════════════════
    //  INPUT DE CHAT PARA /gensdrops
    // ══════════════════════════════════════════════════════════

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();

        // Delegar al GensDropsGUI singleton que esta en el GensDropsCommand
        // Se accede via el plugin instance para mantener el estado de awaitingInput
        if (plugin.getGensDropsGUI() != null &&
            plugin.getGensDropsGUI().isAwaitingInput(player.getUniqueId())) {

            e.setCancelled(true);
            String msg = e.getMessage();

            // Ejecutar en el hilo principal (AsyncPlayerChatEvent es async)
            plugin.getServer().getScheduler().runTask(plugin, () ->
                plugin.getGensDropsGUI().handleChatInput(player, msg)
            );
        }
    }
}
