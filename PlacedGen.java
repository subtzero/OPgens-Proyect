package com.opgens.plugin.generators;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Representa un generador fisicamente colocado en el mundo por un jugador.
 */
public class PlacedGen {

    private final UUID ownerUUID;
    private final String ownerName;
    private Location location;
    private String genTypeId;        // ID del GenType actual
    private int taskId = -1;         // ID de la tarea BukkitRunnable activa

    public PlacedGen(UUID ownerUUID, String ownerName, Location location, String genTypeId) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.location = location;
        this.genTypeId = genTypeId;
    }

    // ── Getters ──────────────────────────────────────────────

    public UUID getOwnerUUID() { return ownerUUID; }

    public String getOwnerName() { return ownerName; }

    public Location getLocation() { return location; }

    public String getGenTypeId() { return genTypeId; }

    public int getTaskId() { return taskId; }

    // ── Setters ──────────────────────────────────────────────

    public void setLocation(Location location) { this.location = location; }

    public void setGenTypeId(String genTypeId) { this.genTypeId = genTypeId; }

    public void setTaskId(int taskId) { this.taskId = taskId; }

    // ── Utilidades ───────────────────────────────────────────

    /**
     * Clave unica basada en la ubicacion del bloque para usar en Maps.
     * Formato: mundo,x,y,z
     */
    public String getLocationKey() {
        return locationToKey(location);
    }

    public static String locationToKey(Location loc) {
        return loc.getWorld().getName() + "," +
               loc.getBlockX() + "," +
               loc.getBlockY() + "," +
               loc.getBlockZ();
    }

    @Override
    public String toString() {
        return "PlacedGen{owner=" + ownerName +
               ", type=" + genTypeId +
               ", loc=" + getLocationKey() + "}";
    }
}
