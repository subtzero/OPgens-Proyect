package com.opgens.plugin.managers;

import com.opgens.plugin.OPGens;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maneja los precios de venta de los drops y la integracion con Vault/EssentialsX.
 * Los precios se guardan en drops.yml y solo el owner puede editarlos via /gensdrops.
 */
public class DropPriceManager {

    private final OPGens plugin;
    private Economy economy;

    // Precios custom: genTypeId -> precio de venta (sobreescribe el config.yml)
    private final Map<String, Double> customPrices = new HashMap<>();

    private File dropsFile;
    private FileConfiguration dropsConfig;

    public DropPriceManager(OPGens plugin) {
        this.plugin = plugin;
        setupEconomy();
        loadDropPrices();
    }

    // ══════════════════════════════════════════════════════════
    //  VAULT / ECONOMIA
    // ══════════════════════════════════════════════════════════

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault no encontrado! El plugin necesita Vault + EssentialsX.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            plugin.getLogger().severe("No se encontro proveedor de economia (EssentialsX?).");
            return false;
        }

        economy = rsp.getProvider();
        plugin.getLogger().info("Economia conectada: " + economy.getName());
        return true;
    }

    public boolean isEconomyReady() {
        return economy != null;
    }

    /**
     * Verifica si el jugador tiene suficiente dinero.
     */
    public boolean hasBalance(Player player, double amount) {
        if (!isEconomyReady()) return false;
        return economy.has(player, amount);
    }

    /**
     * Retira dinero del jugador.
     */
    public boolean withdrawBalance(Player player, double amount) {
        if (!isEconomyReady()) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /**
     * Deposita dinero al jugador.
     */
    public boolean depositBalance(Player player, double amount) {
        if (!isEconomyReady()) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    /**
     * Obtiene el balance del jugador.
     */
    public double getBalance(Player player) {
        if (!isEconomyReady()) return 0;
        return economy.getBalance(player);
    }

    /**
     * Formatea un numero como moneda.
     */
    public String format(double amount) {
        if (!isEconomyReady()) return "$" + String.format("%.2f", amount);
        return economy.format(amount);
    }

    // ══════════════════════════════════════════════════════════
    //  PRECIOS DE DROPS (drops.yml)
    // ══════════════════════════════════════════════════════════

    private void loadDropPrices() {
        dropsFile = new File(plugin.getDataFolder(), "drops.yml");
        if (!dropsFile.exists()) {
            try {
                dropsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear drops.yml: " + e.getMessage());
            }
        }

        dropsConfig = YamlConfiguration.loadConfiguration(dropsFile);

        // Cargar precios guardados
        if (dropsConfig.contains("prices")) {
            for (String key : dropsConfig.getConfigurationSection("prices").getKeys(false)) {
                customPrices.put(key, dropsConfig.getDouble("prices." + key));
            }
        }

        plugin.getLogger().info("Precios de drops cargados: " + customPrices.size() + " entradas custom.");
    }

    public void saveDropPrices() {
        for (Map.Entry<String, Double> entry : customPrices.entrySet()) {
            dropsConfig.set("prices." + entry.getKey(), entry.getValue());
        }
        try {
            dropsConfig.save(dropsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando drops.yml: " + e.getMessage());
        }
    }

    /**
     * Obtiene el precio de venta de un gen.
     * Si hay precio custom en drops.yml usa ese, sino usa el del config.yml.
     */
    public double getSellPrice(String genTypeId) {
        if (customPrices.containsKey(genTypeId)) {
            return customPrices.get(genTypeId);
        }
        // Fallback al config.yml
        return plugin.getConfig().getDouble("generators." + genTypeId + ".sell-price", 0);
    }

    /**
     * Setea un precio custom para el drop de un gen (solo owner via /gensdrops).
     */
    public void setSellPrice(String genTypeId, double price) {
        customPrices.put(genTypeId, price);
        saveDropPrices();
    }

    /**
     * Retorna todos los IDs que tienen precio custom.
     */
    public Set<String> getCustomPriceIds() {
        return customPrices.keySet();
    }

    /**
     * Reload completo de precios.
     */
    public void reload() {
        customPrices.clear();
        loadDropPrices();
    }
}
