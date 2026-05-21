package com.opgens.plugin;

import com.opgens.plugin.commands.GenShopCommand;
import com.opgens.plugin.commands.GensDropsCommand;
import com.opgens.plugin.commands.GensReloadCommand;
import com.opgens.plugin.gui.GensDropsGUI;
import com.opgens.plugin.gui.GUIListener;
import com.opgens.plugin.listeners.GenBlockListener;
import com.opgens.plugin.listeners.GenInteractListener;
import com.opgens.plugin.managers.GenManager;
import com.opgens.plugin.managers.ShopManager;
import com.opgens.plugin.managers.DropPriceManager;
import org.bukkit.plugin.java.JavaPlugin;

public class OPGens extends JavaPlugin {

    private static OPGens instance;
    private GenManager genManager;
    private ShopManager shopManager;
    private DropPriceManager dropPriceManager;
    private GensDropsGUI gensDropsGUI;

    @Override
    public void onEnable() {
        instance = this;

        // Guardar config por defecto si no existe
        saveDefaultConfig();

        // Inicializar managers
        this.dropPriceManager = new DropPriceManager(this);
        this.genManager = new GenManager(this);
        this.shopManager = new ShopManager(this);
        this.gensDropsGUI = new GensDropsGUI(this);

        // Registrar comandos
        getCommand("genshop").setExecutor(new GenShopCommand(this));
        getCommand("gensdrops").setExecutor(new GensDropsCommand(this));
        getCommand("gensreload").setExecutor(new GensReloadCommand(this));

        // Registrar listeners
        getServer().getPluginManager().registerEvents(new GenBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new GenInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        getLogger().info("==============================");
        getLogger().info("  OPGens v1.0.0 - Activado!");
        getLogger().info("==============================");
    }

    @Override
    public void onDisable() {
        // Guardar todos los generadores activos al disco
        if (genManager != null) {
            genManager.saveAll();
        }
        getLogger().info("OPGens desactivado. Datos guardados.");
    }

    public static OPGens getInstance() {
        return instance;
    }

    public GenManager getGenManager() {
        return genManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public DropPriceManager getDropPriceManager() {
        return dropPriceManager;
    }

    public GensDropsGUI getGensDropsGUI() {
        return gensDropsGUI;
    }

    /**
     * Obtiene un mensaje del config con el prefijo del plugin
     */
    public String getMessage(String key) {
        String prefix = colorize(getConfig().getString("prefix", "&8[&aOPGens&8]&r "));
        String msg = getConfig().getString("messages." + key, "&cMensaje no encontrado: " + key);
        return colorize(prefix + " " + msg);
    }

    /**
     * Obtiene un mensaje reemplazando placeholders
     */
    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    /**
     * Traduce códigos de color &
     */
    public static String colorize(String text) {
        return text == null ? "" : text.replace("&", "\u00A7");
    }
}
