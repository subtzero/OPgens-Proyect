package com.opgens.plugin.gui;

import com.opgens.plugin.OPGens;
import com.opgens.plugin.generators.GenType;
import com.opgens.plugin.managers.ShopManager;
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
 * GUI de /genshop — muestra todos los generadores comprables (solo tier 1).
 * Los demas tiers se desbloquean mejorando con Shift+Click derecho.
 */
public class GenShopGUI {

    private static final String TITLE = OPGens.colorize("&8✦ &aGens Shop &8✦");
    private static final int SIZE = 54; // 6 filas

    private final OPGens plugin;

    public GenShopGUI(OPGens plugin) {
        this.plugin = plugin;
    }

    /**
     * Abre el inventario de la tienda para el jugador.
     */
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);

        // Relleno decorativo
        ItemStack filler = makeFiller();

        // Fila superior e inferior con relleno
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        // Columnas laterales
        for (int i = 1; i < 5; i++) {
            inv.setItem(i * 9, filler);
            inv.setItem(i * 9 + 8, filler);
        }

        // Cargar gens comprables
        List<GenType> purchasable = plugin.getGenManager().getPurchasableGenTypes();
        int[] slots = {10, 11, 12, 13, 14, 15, 16,
                       19, 20, 21, 22, 23, 24, 25,
                       28, 29, 30, 31, 32, 33, 34,
                       37, 38, 39, 40, 41, 42, 43};

        for (int i = 0; i < purchasable.size() && i < slots.length; i++) {
            GenType type = purchasable.get(i);
            inv.setItem(slots[i], buildShopDisplayItem(type, player));
        }

        // Info de balance del jugador en slot 49 (centro fila inferior)
        inv.setItem(49, buildBalanceItem(player));

        player.openInventory(inv);
    }

    /**
     * Construye el item de display para la tienda con toda la info.
     */
    private ItemStack buildShopDisplayItem(GenType type, Player player) {
        ItemStack base = plugin.getShopManager().buildShopItem(type);
        ItemMeta meta = base.getItemMeta();
        if (meta == null) return base;

        double balance = plugin.getDropPriceManager().getBalance(player);
        boolean canAfford = balance >= type.getShopPrice();

        List<String> lore = new ArrayList<>();
        lore.add(OPGens.colorize("&8━━━━━━━━━━━━━━━━━━━━"));
        lore.add(OPGens.colorize("&7Tier: &e" + type.getTier()));
        lore.add(OPGens.colorize("&7Bloque: &f" + formatMat(type.getBlockMaterial())));
        lore.add(OPGens.colorize("&7Drop: &f" + formatMat(type.getDropMaterial())));
        lore.add(OPGens.colorize("&7Venta por drop: &a$" + type.getSellPrice()));
        lore.add(OPGens.colorize("&8━━━━━━━━━━━━━━━━━━━━"));
        lore.add(OPGens.colorize("&7Precio: &e$" + type.getShopPrice()));

        if (canAfford) {
            lore.add(OPGens.colorize("&a▶ Click para comprar"));
        } else {
            lore.add(OPGens.colorize("&c✗ No tienes suficiente dinero"));
            lore.add(OPGens.colorize("&7Necesitas: &c$" +
                    String.format("%.2f", type.getShopPrice() - balance) + " &7mas"));
        }

        // Mantener la linea codificada con el ID al final
        List<String> oldLore = meta.getLore();
        if (oldLore != null) {
            for (String line : oldLore) {
                if (line.contains("OPGEN:")) {
                    lore.add(line);
                    break;
                }
            }
        }

        meta.setLore(lore);
        base.setItemMeta(meta);
        return base;
    }

    /**
     * Item de balance del jugador (decorativo, slot inferior centro).
     */
    private ItemStack buildBalanceItem(Player player) {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        double balance = plugin.getDropPriceManager().getBalance(player);
        meta.setDisplayName(OPGens.colorize("&6Tu balance"));
        List<String> lore = new ArrayList<>();
        lore.add(OPGens.colorize("&7Balance actual: &a$" + String.format("%.2f", balance)));
        lore.add(OPGens.colorize("&8Solo informativo"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Item de relleno gris para decorar el inventario.
     */
    private ItemStack makeFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatMat(String mat) {
        return mat.replace("_", " ").toLowerCase();
    }

    /**
     * Verifica si un inventario es el GenShop.
     */
    public static boolean isGenShop(Inventory inv) {
        return inv != null && inv.getSize() == SIZE &&
               TITLE.equals(inv.getViewers().isEmpty() ? null :
               inv.getViewers().get(0).getOpenInventory().getTitle());
    }
}
