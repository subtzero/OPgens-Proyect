package com.opgens.plugin.utils;

import com.opgens.plugin.OPGens;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Utilidad para crear cabezas de jugador con texturas custom (Base64).
 * Compatible con Minecraft 1.21.x usando PlayerProfile API.
 */
public class HeadUtil {

    /**
     * Crea un ItemStack de cabeza con textura base64.
     *
     * @param base64Texture Textura en formato base64 (valor de textura de Mojang)
     * @param displayName   Nombre visible del item
     * @return ItemStack de PLAYER_HEAD con la textura aplicada
     */
    public static ItemStack getCustomHead(String base64Texture, String displayName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        // Aplicar textura via PlayerProfile (API moderna 1.19+)
        try {
            String textureUrl = extractUrlFromBase64(base64Texture);
            if (textureUrl != null) {
                PlayerProfile profile = OPGens.getInstance()
                        .getServer()
                        .createPlayerProfile(UUID.randomUUID(), "OPGens");

                PlayerTextures textures = profile.getTextures();
                textures.setSkin(new URL(textureUrl));
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
            }
        } catch (MalformedURLException e) {
            OPGens.getInstance().getLogger().warning(
                "URL de textura invalida para cabeza: " + e.getMessage()
            );
        }

        // Nombre y lore
        meta.setDisplayName(displayName);
        List<String> lore = new ArrayList<>();
        lore.add(OPGens.colorize("&7Item especial del generador"));
        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    /**
     * Decodifica el base64 de la textura de Mojang y extrae la URL de la skin.
     * El base64 contiene JSON del tipo:
     * {"textures":{"SKIN":{"url":"https://textures.minecraft.net/texture/..."}}}
     */
    private static String extractUrlFromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            String decoded = new String(Base64.getDecoder().decode(base64));
            // Buscar la URL dentro del JSON decodificado
            int urlStart = decoded.indexOf("\"url\":\"");
            if (urlStart == -1) return null;
            urlStart += 7; // largo de "url":"
            int urlEnd = decoded.indexOf("\"", urlStart);
            if (urlEnd == -1) return null;
            return decoded.substring(urlStart, urlEnd);
        } catch (Exception e) {
            return null;
        }
    }
}
