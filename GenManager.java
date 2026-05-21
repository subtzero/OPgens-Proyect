package com.opgens.plugin.managers;

import com.opgens.plugin.OPGens;
import com.opgens.plugin.generators.GenType;
import com.opgens.plugin.generators.PlacedGen;
import com.opgens.plugin.utils.HeadUtil;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GenManager {

    private final OPGens plugin;

    // Todos los tipos de gen definidos en config.yml
    private final Map<String, GenType> genTypes = new LinkedHashMap<>();

    // Cadena de upgrades: genTypeId -> siguiente genTypeId
    private final Map<String, String> upgradeChain = new HashMap<>();

    // Generadores colocados en el mundo: locationKey -> PlacedGen
    private final Map<String, PlacedGen> placedGens = new HashMap<>();

    // Generadores por jugador: UUID -> lista de locationKeys
    private final Map<UUID, List<String>> playerGens = new HashMap<>();

    // Archivo de persistencia
    private File dataFile;
    private FileConfiguration dataConfig;

    public GenManager(OPGens plugin) {
        this.plugin = plugin;
        loadGenTypes();
        loadData();
        startAllTasks();
    }

    // ══════════════════════════════════════════════════════════
    //  CARGA DE TIPOS DESDE config.yml
    // ══════════════════════════════════════════════════════════

    public void loadGenTypes() {
        genTypes.clear();
        upgradeChain.clear();

        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection gensSection = cfg.getConfigurationSection("generators");

        if (gensSection == null) {
            plugin.getLogger().warning("No se encontro la seccion 'generators' en config.yml");
            return;
        }

        for (String id : gensSection.getKeys(false)) {
            ConfigurationSection s = gensSection.getConfigurationSection(id);
            if (s == null) continue;

            GenType type = new GenType(
                id,
                OPGens.colorize(s.getString("display-name", id)),
                s.getInt("tier", 1),
                s.getString("block", "STONE"),
                s.getString("drop-material", "STONE"),
                s.getBoolean("drop-is-head", false),
                s.getString("head-texture", ""),
                s.getDouble("shop-price", 0),
                s.getDouble("upgrade-price", 0),
                s.getDouble("sell-price", 0),
                s.getBoolean("purchasable", false)
            );
            genTypes.put(id, type);
        }

        // Cargar cadena de upgrades
        ConfigurationSection chainSection = cfg.getConfigurationSection("upgrade-chain");
        if (chainSection != null) {
            for (String from : chainSection.getKeys(false)) {
                upgradeChain.put(from, chainSection.getString(from));
            }
        }

        plugin.getLogger().info("Cargados " + genTypes.size() + " tipos de generadores.");
    }

    // ══════════════════════════════════════════════════════════
    //  PERSISTENCIA  (gens.yml)
    // ══════════════════════════════════════════════════════════

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "gens.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear gens.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection placed = dataConfig.getConfigurationSection("placed");
        if (placed == null) return;

        for (String key : placed.getKeys(false)) {
            ConfigurationSection s = placed.getConfigurationSection(key);
            if (s == null) continue;

            String worldName = s.getString("world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Mundo '" + worldName + "' no encontrado, saltando gen " + key);
                continue;
            }

            Location loc = new Location(world,
                    s.getInt("x"), s.getInt("y"), s.getInt("z"));
            UUID ownerUUID = UUID.fromString(s.getString("owner-uuid"));
            String ownerName = s.getString("owner-name", "Unknown");
            String typeId = s.getString("type-id");

            PlacedGen pg = new PlacedGen(ownerUUID, ownerName, loc, typeId);
            placedGens.put(pg.getLocationKey(), pg);

            playerGens.computeIfAbsent(ownerUUID, k -> new ArrayList<>())
                      .add(pg.getLocationKey());
        }

        plugin.getLogger().info("Cargados " + placedGens.size() + " generadores del disco.");
    }

    public void saveAll() {
        if (dataConfig == null) return;

        // Limpiar seccion anterior
        dataConfig.set("placed", null);

        for (Map.Entry<String, PlacedGen> entry : placedGens.entrySet()) {
            PlacedGen pg = entry.getValue();
            String path = "placed." + entry.getKey().replace(",", "_").replace(".", "_");

            dataConfig.set(path + ".world", pg.getLocation().getWorld().getName());
            dataConfig.set(path + ".x", pg.getLocation().getBlockX());
            dataConfig.set(path + ".y", pg.getLocation().getBlockY());
            dataConfig.set(path + ".z", pg.getLocation().getBlockZ());
            dataConfig.set(path + ".owner-uuid", pg.getOwnerUUID().toString());
            dataConfig.set(path + ".owner-name", pg.getOwnerName());
            dataConfig.set(path + ".type-id", pg.getGenTypeId());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando gens.yml: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  COLOCAR / REMOVER GENERADORES
    // ══════════════════════════════════════════════════════════

    /**
     * Coloca un generador en el mundo para el jugador.
     * Retorna false si el jugador alcanzo su limite.
     */
    public boolean placeGen(Player player, Location loc, String genTypeId) {
        int maxGens = plugin.getConfig().getInt("max-gens-per-player", 10);

        // Check limite (bypass para ops con permiso)
        if (maxGens > 0 && !player.hasPermission("opgens.bypass")) {
            List<String> playerList = playerGens.getOrDefault(player.getUniqueId(), Collections.emptyList());
            if (playerList.size() >= maxGens) {
                return false;
            }
        }

        GenType type = genTypes.get(genTypeId);
        if (type == null) return false;

        // Colocar bloque visual
        Material blockMat = Material.matchMaterial(type.getBlockMaterial());
        if (blockMat != null) loc.getBlock().setType(blockMat);

        PlacedGen pg = new PlacedGen(player.getUniqueId(), player.getName(), loc, genTypeId);
        String key = pg.getLocationKey();

        placedGens.put(key, pg);
        playerGens.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(key);

        startTask(pg);
        saveAll();
        return true;
    }

    /**
     * Remueve un generador del mundo (rompio el bloque).
     */
    public void removeGen(Location loc) {
        String key = PlacedGen.locationToKey(loc);
        PlacedGen pg = placedGens.remove(key);
        if (pg == null) return;

        // Cancelar tarea de drop
        if (pg.getTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(pg.getTaskId());
        }

        // Quitar de la lista del jugador
        List<String> list = playerGens.get(pg.getOwnerUUID());
        if (list != null) list.remove(key);

        saveAll();
    }

    /**
     * Mejora un generador al siguiente tier.
     * Retorna el nuevo GenType o null si no hay siguiente tier.
     */
    public GenType upgradeGen(Location loc, Player player) {
        String key = PlacedGen.locationToKey(loc);
        PlacedGen pg = placedGens.get(key);
        if (pg == null) return null;

        String nextId = upgradeChain.get(pg.getGenTypeId());
        if (nextId == null) return null; // Ya es maximo tier

        GenType nextType = genTypes.get(nextId);
        if (nextType == null) return null;

        // Cobrar precio de upgrade
        DropPriceManager eco = plugin.getDropPriceManager();
        double price = genTypes.get(pg.getGenTypeId()).getUpgradePrice();
        if (!eco.hasBalance(player, price)) return null;
        eco.withdrawBalance(player, price);

        // Cancelar tarea vieja
        if (pg.getTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(pg.getTaskId());
            pg.setTaskId(-1);
        }

        // Cambiar tipo y bloque
        pg.setGenTypeId(nextId);
        Material blockMat = Material.matchMaterial(nextType.getBlockMaterial());
        if (blockMat != null) loc.getBlock().setType(blockMat);

        // Iniciar nueva tarea
        startTask(pg);
        saveAll();
        return nextType;
    }

    // ══════════════════════════════════════════════════════════
    //  TAREAS DE DROP
    // ══════════════════════════════════════════════════════════

    private void startAllTasks() {
        for (PlacedGen pg : placedGens.values()) {
            startTask(pg);
        }
    }

    private void startTask(PlacedGen pg) {
        int interval = plugin.getConfig().getInt("gen-tick-interval", 40);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                Location loc = pg.getLocation();
                if (loc.getWorld() == null) return;

                GenType type = genTypes.get(pg.getGenTypeId());
                if (type == null) return;

                // Verificar que el bloque sigue siendo el correcto
                Material expected = Material.matchMaterial(type.getBlockMaterial());
                if (expected != null && loc.getBlock().getType() != expected) {
                    // El bloque fue removido externamente
                    cancel();
                    return;
                }

                // Generar el item drop
                ItemStack drop = buildDropItem(type);
                if (drop != null) {
                    loc.getWorld().dropItemNaturally(
                        loc.clone().add(0.5, 1.0, 0.5),
                        drop
                    );
                }
            }
        };

        int id = task.runTaskTimer(plugin, interval, interval).getTaskId();
        pg.setTaskId(id);
    }

    /**
     * Construye el ItemStack del drop segun el tipo de gen.
     */
    private ItemStack buildDropItem(GenType type) {
        if (type.isDropIsHead()) {
            return HeadUtil.getCustomHead(type.getHeadTexture(), type.getDisplayName());
        }

        Material mat = Material.matchMaterial(type.getDropMaterial());
        if (mat == null) return null;

        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(type.getDisplayName());
            item.setItemMeta(meta);
        }
        return item;
    }

    // ══════════════════════════════════════════════════════════
    //  GETTERS / UTILIDADES
    // ══════════════════════════════════════════════════════════

    public GenType getGenType(String id) {
        return genTypes.get(id);
    }

    public Map<String, GenType> getAllGenTypes() {
        return Collections.unmodifiableMap(genTypes);
    }

    public List<GenType> getPurchasableGenTypes() {
        return genTypes.values().stream()
                .filter(GenType::isPurchasable)
                .collect(Collectors.toList());
    }

    public PlacedGen getPlacedGen(Location loc) {
        return placedGens.get(PlacedGen.locationToKey(loc));
    }

    public boolean isGenBlock(Location loc) {
        return placedGens.containsKey(PlacedGen.locationToKey(loc));
    }

    public String getNextTierId(String currentId) {
        return upgradeChain.get(currentId);
    }

    public int getPlayerGenCount(UUID uuid) {
        return playerGens.getOrDefault(uuid, Collections.emptyList()).size();
    }

    /**
     * Construye el item de inventario que representa a un GenType
     * (para la tienda o para dar al jugador).
     */
    public ItemStack buildGenItem(GenType type) {
        Material mat = Material.matchMaterial(type.getBlockMaterial());
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(type.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add(OPGens.colorize("&7Tier: &e" + type.getTier()));
        lore.add(OPGens.colorize("&7Drop: &f" + formatMaterial(type.getDropMaterial())));
        lore.add(OPGens.colorize("&7Venta/drop: &a$" + type.getSellPrice()));
        if (type.isPurchasable()) {
            lore.add("");
            lore.add(OPGens.colorize("&aPrecio: &e$" + type.getShopPrice()));
            lore.add(OPGens.colorize("&eClick para comprar"));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String formatMaterial(String mat) {
        return mat.replace("_", " ").toLowerCase();
    }
}
