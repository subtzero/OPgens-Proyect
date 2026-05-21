package com.opgens.plugin.gui;

import com.opgens.plugin.OPGens;
import com.opgens.plugin.generators.GenType;
import com.opgens.plugin.managers.DropPriceManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI de /gensdrops — solo visible para el owner (opgens.admin).
 * Permite ver y editar el precio de venta de cada tipo de gen.
 * Al hacer click en un gen, se pide via chat el nuevo precio.
 */
public class GensDropsGUI {

    private static final String TITLE = OPGens.colorize("&8[&cADMIN&8] &cGens Drops");
    private static final int SIZE = 54;

    private final OPGens plugin;

    // Jugadores esperando input de precio via chat: UUID -> genTypeId
    private final java.util.HashMap<java.util.UUID, String> awaitingInput = new java.util.HashMap<>();

    public GensDropsGUI(OPGens plugin) {
        this.plugin = plugin;
    }

    /**
     * Abre el panel de drops para el admin.
     */
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);

        ItemStack filler = makeFiller(Material.RED_STAINED_GLASS_PANE);

        // Bordes
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);
        for (int i = 1; i < 5; i++) {
            inv.setItem(i * 9, filler);
            inv.setItem(i * 9 + 8, filler);
        }

        // Slots disponibles para los gens
        int[] slots = {10, 11, 12, 13, 14, 15, 16,
                       19, 20, 21, 22, 23, 24, 25,
                       28, 29, 30, 31, 32, 33, 34,
                       37, 38, 39, 40, 41, 42, 43};

        Map<String, GenType> allTypes = plugin.getGenManager().getAllGenTypes();
        int i = 0;
        for (GenType type : allTypes.values()) {
            if (i >= slots.length) break;
            inv.setItem(slots[i], buildDropEditItem(type));
            i++;
        }

        // Boton de info en slot 49
        inv.setItem(49, buildInfoItem());

        player.openInventory(inv);
    }

    /**
     * Construye el item de cada gen para el panel admin.
     */
    private ItemStack buildDropEditItem(GenType type) {
        Material mat = Material.matchMaterial(type.getBlockMaterial());
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        DropPriceManager dpm = plugin.getDropPriceManager();
        double currentPrice = dpm.getSellPrice(type.getId());
        double configPrice = plugin.getConfig().getDouble(
                "generators." + type.getId() + ".sell-price", 0);
        boolean isCustom = currentPrice != configPrice;

        meta.setDisplayName(type.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add(OPGens.colorize("&8━━━━━━━━━━━━━━━━━━━━"));
        lore.add(OPGens.colorize("&7ID: &f" + type.getId()));
        lore.add(OPGens.colorize("&7Tier: &e" + type.getTier()));
        lore.add(OPGens.colorize("&7Drop: &f" + type.getDropMaterial().replace("_", " ").toLowerCase()));
        lore.add(OPGens.colorize("&8━━━━━━━━━━━━━━━━━━━━"));
        lore.add(OPGens.colorize("&7Precio config.yml: &7$" + configPrice));

        if (isCustom) {
            lore.add(OPGens.colorize("&7Precio actual: &a$" + currentPrice + " &e(custom)"));
        } else {
            lore.add(OPGens.colorize("&7Precio actual: &a$" + currentPrice));
        }

        lore.add(OPGens.colorize("&8━━━━━━━━━━━━━━━━━━━━"));
        lore.add(OPGens.colorize("&eClick &7para cambiar el precio"));
        lore.add(OPGens.colorize("&cShift+Click &7para resetear al default"));

        // ID codificado para identificar al hacer click
        lore.add(OPGens.colorize("&0&rOPGEN:" + type.getId()));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(OPGens.colorize("&eInformacion"));
        List<String> lore = new ArrayList<>();
        lore.add(OPGens.colorize("&7Click en un gen para"));
        lore.add(OPGens.colorize("&7cambiar su precio de venta."));
        lore.add(OPGens.colorize("&7Los cambios se guardan"));
        lore.add(OPGens.colorize("&7en &fdrop.yml&7 automaticamente."));
        lore.add(OPGens.colorize("&8Solo visible para admins."));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeFiller(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    // ══════════════════════════════════════════════════════════
    //  MANEJO DE CLICKS Y INPUT DE PRECIO
    // ══════════════════════════════════════════════════════════

    /**
     * Maneja el click en un item del panel admin.
     * Retorna true si el click fue procesado.
     */
    public boolean handleClick(Player player, ItemStack clicked, boolean isShiftClick) {
        if (clicked == null || !clicked.hasItemMeta()) return false;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;

        String genId = extractGenId(clicked);
        if (genId == null) return false;

        if (isShiftClick) {
            // Resetear al precio del config.yml
            double configPrice = plugin.getConfig().getDouble(
                    "generators." + genId + ".sell-price", 0);
            plugin.getDropPriceManager().setSellPrice(genId, configPrice);
            player.sendMessage(plugin.getMessage("drop-price-set",
                    "{item}", genId, "{price}", String.valueOf(configPrice)));
            // Refrescar GUI
            open(player);
        } else {
            // Pedir nuevo precio via chat
            awaitingInput.put(player.getUniqueId(), genId);
            player.closeInventory();
            player.sendMessage(OPGens.colorize(
                "&8[&cAdmin&8] &7Escribe el nuevo precio de venta para &e" + genId +
                "&7 en el chat. &c(escribe 'cancelar' para cancelar)"));
        }
        return true;
    }

    /**
     * Procesa el input de precio desde el chat.
     * Retorna true si el mensaje fue consumido (jugador estaba en modo input).
     */
    public boolean handleChatInput(Player player, String message) {
        java.util.UUID uuid = player.getUniqueId();
        if (!awaitingInput.containsKey(uuid)) return false;

        String genId = awaitingInput.remove(uuid);

        if (message.equalsIgnoreCase("cancelar")) {
            player.sendMessage(OPGens.colorize("&7Cambio de precio cancelado."));
            return true;
        }

        try {
            double newPrice = Double.parseDouble(message.replace(",", "."));
            if (newPrice < 0) {
                player.sendMessage(OPGens.colorize("&cEl precio no puede ser negativo."));
                return true;
            }
            plugin.getDropPriceManager().setSellPrice(genId, newPrice);
            player.sendMessage(plugin.getMessage("drop-price-set",
                    "{item}", genId,
                    "{price}", String.format("%.2f", newPrice)));
        } catch (NumberFormatException e) {
            player.sendMessage(OPGens.colorize("&cNúmero inválido. Usa solo numeros. Ej: 150.5"));
        }

        return true;
    }

    public boolean isAwaitingInput(java.util.UUID uuid) {
        return awaitingInput.containsKey(uuid);
    }

    private String extractGenId(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        for (String line : meta.getLore()) {
            String stripped = line.replace("\u00A7", "&");
            if (stripped.startsWith("&0&rOPGEN:")) {
                return stripped.substring(10);
            }
        }
        return null;
    }

    /**
     * Verifica si un inventario es el GensDrops.
     */
    public static boolean isGensDrops(String title) {
        return TITLE.equals(title);
    }
}
