package com.opgens.plugin.generators;

/**
 * Representa la definicion/tipo de un generador leido desde config.yml
 */
public class GenType {

    private final String id;
    private final String displayName;
    private final int tier;
    private final String blockMaterial;   // bloque visual que se coloca
    private final String dropMaterial;    // item que suelta
    private final boolean dropIsHead;     // si suelta una cabeza custom
    private final String headTexture;     // base64 de la textura si es cabeza
    private final double shopPrice;       // precio de compra en /genshop
    private final double upgradePrice;    // precio para mejorar al siguiente tier
    private final double sellPrice;       // precio de venta del drop
    private final boolean purchasable;    // si aparece en /genshop para comprar

    public GenType(String id, String displayName, int tier,
                   String blockMaterial, String dropMaterial,
                   boolean dropIsHead, String headTexture,
                   double shopPrice, double upgradePrice,
                   double sellPrice, boolean purchasable) {
        this.id = id;
        this.displayName = displayName;
        this.tier = tier;
        this.blockMaterial = blockMaterial;
        this.dropMaterial = dropMaterial;
        this.dropIsHead = dropIsHead;
        this.headTexture = headTexture;
        this.shopPrice = shopPrice;
        this.upgradePrice = upgradePrice;
        this.sellPrice = sellPrice;
        this.purchasable = purchasable;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getTier() { return tier; }
    public String getBlockMaterial() { return blockMaterial; }
    public String getDropMaterial() { return dropMaterial; }
    public boolean isDropIsHead() { return dropIsHead; }
    public String getHeadTexture() { return headTexture; }
    public double getShopPrice() { return shopPrice; }
    public double getUpgradePrice() { return upgradePrice; }
    public double getSellPrice() { return sellPrice; }
    public boolean isPurchasable() { return purchasable; }

    @Override
    public String toString() {
        return "GenType{id='" + id + "', tier=" + tier + ", block=" + blockMaterial + "}";
    }
}
