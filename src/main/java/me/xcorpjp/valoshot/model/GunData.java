package me.xcorpjp.valoshot.model;

import org.bukkit.Material;
import java.util.List;

/**
 * 武器の性能データを保持するクラス
 */
public class GunData {
    private final String id;
    private final String displayName;
    private final GunCategory category;
    private final Material material;
    private final int customModelData;
    private final double headDamage;
    private final double bodyDamage;
    private final double legDamage;
    private final int fireRate;
    private final int magazineSize;
    private final double reloadTime;
    private final double accuracy;
    private final double adsAccuracy; // ADS時の射撃精度
    private final double movementSpread;
    private final double recoil;
    private final boolean canAds;
    private final double adsZoom;
    private final List<String> lore;

    // 高度な挙動設定
    private final int pellets;
    private final double moveSpreadMult;
    private final double adsMoveSpreadMult;
    private final double airSpreadMult;
    private final double adsAirSpreadMult;
    private final double recoilFactor;
    private final boolean fullAuto;

    public GunData(String id, String displayName, GunCategory category, Material material, int customModelData,
            double headDamage, double bodyDamage, double legDamage, int fireRate, int magazineSize, double reloadTime,
            double accuracy, double adsAccuracy, double movementSpread, double recoil, boolean canAds, double adsZoom,
            List<String> lore, int pellets, double moveSpreadMult, double adsMoveSpreadMult, double airSpreadMult,
            double adsAirSpreadMult, double recoilFactor, boolean fullAuto) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.material = material;
        this.customModelData = customModelData;
        this.headDamage = headDamage;
        this.bodyDamage = bodyDamage;
        this.legDamage = legDamage;
        this.fireRate = fireRate;
        this.magazineSize = magazineSize;
        this.reloadTime = reloadTime;
        this.accuracy = accuracy;
        this.adsAccuracy = adsAccuracy;
        this.movementSpread = movementSpread;
        this.recoil = recoil;
        this.canAds = canAds;
        this.adsZoom = adsZoom;
        this.lore = lore;
        this.pellets = pellets;
        this.moveSpreadMult = moveSpreadMult;
        this.adsMoveSpreadMult = adsMoveSpreadMult;
        this.airSpreadMult = airSpreadMult;
        this.adsAirSpreadMult = adsAirSpreadMult;
        this.recoilFactor = recoilFactor;
        this.fullAuto = fullAuto;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public GunCategory getCategory() {
        return category;
    }

    public Material getMaterial() {
        return material;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public double getHeadDamage() {
        return headDamage;
    }

    public double getBodyDamage() {
        return bodyDamage;
    }

    public double getLegDamage() {
        return legDamage;
    }

    public int getFireRate() {
        return fireRate;
    }

    public int getMagazineSize() {
        return magazineSize;
    }

    public double getReloadTime() {
        return reloadTime;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public double getAdsAccuracy() {
        return adsAccuracy;
    }

    public double getMovementSpread() {
        return movementSpread;
    }

    public double getRecoil() {
        return recoil;
    }

    public boolean canAds() {
        return canAds;
    }

    public double getAdsZoom() {
        return adsZoom;
    }

    public List<String> getLore() {
        return lore;
    }

    public int getPellets() {
        return pellets;
    }

    public double getMoveSpreadMult() {
        return moveSpreadMult;
    }

    public double getAdsMoveSpreadMult() {
        return adsMoveSpreadMult;
    }

    public double getAirSpreadMult() {
        return airSpreadMult;
    }

    public double getAdsAirSpreadMult() {
        return adsAirSpreadMult;
    }

    public double getRecoilFactor() {
        return recoilFactor;
    }

    public boolean isFullAuto() {
        return fullAuto;
    }
}
