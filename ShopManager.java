package com.opgens.plugin.managers;

import com.opgens.plugin.OPGens;
import com.opgens.plugin.generators.GenType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Maneja la logica de compra de generadores desde /genshop.
 */
public class ShopManager {

    private final OPGens plugin;

    public ShopManager(OPGens plugin) {
        this.plugin = plugin;
    }

    /**
     * Intenta comprar un generador para el jugador.
     *
     * @param player    Jugador que compra
     * @param genTypeId ID del gen a comprar
     * @return Resultado de la compra como enum
     */
    public PurchaseResult purchaseGen(Player player, String genTypeId) {
        GenType type = plugin.getGenManager().getGenType(genTypeId);
        if (type == null) return PurchaseResult.INVALID;

        // Solo se pueden comprar gens purchasable (tier 1)
        if (!type.isPurchasable()) return PurchaseResult.NOT_PURCHASABLE;

        double price = type.getShopPrice();
        DropPriceManager eco = plugin.getDropPriceManager();

        // Verificar dinero
        if (!eco.hasBalance(player, price)) return PurchaseResult.NO_MONEY;

        // Verificar espacio en inventario
        if (player.getInventory().firstEmpty() == -1) return PurchaseResult.NO_SPACE;

        // Cobrar
        eco.withdrawBalance(player, price);

        // Dar item al jugador
        ItemStack genItem = buildShopItem(type);
        player.getInventory().addItem(genItem);

        return PurchaseResult.SUCCESS;
    }

    /**
     * Construye el ItemStack del gen para dar al jugador al comprar.
     * Incluye NBT-like data en el lore para identificar el tipo al colocar.
     */
    public ItemStack buildShopItem(GenType type) {
        ItemStack item = plugin.getGenManager().buildGenItem(type);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Agregar linea oculta con el ID para identificarlo al colocar
        // Usamos PersistentDataContainer via lore oculto con color invisible
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();

        // Linea con ID codificado (invisible con color negro en fondo oscuro)
        lore.add(OPGens.colorize("&0&r" + encodeGenId(type.getId())));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Extrae el genTypeId de un ItemStack (busca la linea codificada en el lore).
     * Retorna null si no es un item de gen valido.
     */
    public String extractGenId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;

        List<String> lore = meta.getLore();
        if (lore == null) return null;

        for (String line : lore) {
            // Buscar la linea con el prefijo codificado
            String stripped = line.replace("\u00A7", "&");
            if (stripped.startsWith("&0&r")) {
                return decodeGenId(stripped.substring(4));
            }
        }
        return null;
    }

    /**
     * Verifica si un ItemStack es un item de gen valido.
     */
    public boolean isGenItem(ItemStack item) {
        return extractGenId(item) != null;
    }

    // ── Codificacion simple del ID en el lore ──────────────

    private String encodeGenId(String id) {
        // Prefijo unico para no confundir con otro lore
        return "OPGEN:" + id;
    }

    private String decodeGenId(String encoded) {
        if (encoded.startsWith("OPGEN:")) {
            return encoded.substring(6);
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════
    //  ENUM DE RESULTADOS
    // ══════════════════════════════════════════════════════════

    public enum PurchaseResult {
        SUCCESS,
        NO_MONEY,
        NO_SPACE,
        NOT_PURCHASABLE,
        INVALID
    }
}
