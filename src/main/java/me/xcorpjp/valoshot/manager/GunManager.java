package me.xcorpjp.valoshot.manager;

import me.xcorpjp.valoshot.Valoshot;
import me.xcorpjp.valoshot.model.GunCategory;
import me.xcorpjp.valoshot.model.GunData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

public class GunManager {
    private final Valoshot plugin;
    private final Map<String, GunData> guns = new HashMap<>();
    private final NamespacedKey gunKey;
    private final NamespacedKey ammoKey;

    public GunManager(Valoshot plugin) {
        this.plugin = plugin;
        this.gunKey = new NamespacedKey(plugin, "gun_id");
        this.ammoKey = new NamespacedKey(plugin, "ammo_count");
    }

    public void loadGuns() {
        guns.clear();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("guns");
        if (section == null)
            return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection g = section.getConfigurationSection(key);

            // 武器種カテゴリの読み込み
            GunCategory cat;
            try {
                cat = GunCategory.valueOf(g.getString("category", "RIFLE").toUpperCase());
            } catch (IllegalArgumentException e) {
                cat = GunCategory.RIFLE;
            }

            // カテゴリごとの特性判定
            boolean isSG = (cat == GunCategory.SG);
            boolean isPrecision = (cat == GunCategory.RIFLE || cat == GunCategory.SMG || cat == GunCategory.SIDEARM
                    || cat == GunCategory.DMR);

            double baseDmg = g.getDouble("damage", 10.0);
            double hDmg = g.getDouble("head_damage", baseDmg * 2.0);
            double bDmg = g.getDouble("body_damage", baseDmg);
            double lDmg = g.getDouble("leg_damage", baseDmg * 0.85);

            GunData data = new GunData(
                    key,
                    g.getString("display_name", key),
                    cat,
                    Material.valueOf(g.getString("material", "IRON_HOE").toUpperCase()),
                    g.getInt("custom_model_data", 0),
                    hDmg, bDmg, lDmg,
                    g.getInt("fire_rate", 10),
                    g.getInt("magazine_size", 30),
                    g.getDouble("reload_time", 2.0),
                    g.getDouble("accuracy", 0.1),
                    g.getDouble("ads_accuracy", 0.02),
                    g.getDouble("movement_spread", 0.05),
                    g.getDouble("recoil", 0.2),
                    g.getBoolean("can_ads", false),
                    g.getDouble("ads_zoom", 2.0),
                    g.getStringList("lore"),
                    // 未設定時はカテゴリ別の標準値を自動適用
                    g.getInt("pellets", isSG ? 8 : 1),
                    g.getDouble("move_spread_mult", isSG ? 0.0 : 3.0),
                    g.getDouble("ads_move_spread_mult", isSG ? 0.0 : 0.3),
                    g.getDouble("air_spread_mult", isSG ? 0.1 : 5.0),
                    g.getDouble("ads_air_spread_mult", isSG ? 0.1 : 1.2),
                    g.getDouble("recoil_factor", isPrecision ? 1.0 : 1.8),
                    g.getBoolean("full_auto", isPrecision && cat != GunCategory.SIDEARM && cat != GunCategory.DMR));
            guns.put(key, data);
        }
        plugin.getLogger().info("Loaded " + guns.size() + " guns from config.");
    }

    public ItemStack createGunItem(String id) {
        GunData data = guns.get(id);
        if (data == null)
            return null;

        ItemStack item = new ItemStack(data.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(data.getDisplayName()));
            meta.lore(data.getLore().stream()
                    .map(s -> LegacyComponentSerializer.legacyAmpersand().deserialize(s))
                    .toList());
            meta.setCustomModelData(data.getCustomModelData());
            meta.getPersistentDataContainer().set(gunKey, PersistentDataType.STRING, id);
            meta.getPersistentDataContainer().set(ammoKey, PersistentDataType.INTEGER, data.getMagazineSize());
            item.setItemMeta(meta);
        }
        return item;
    }

    public int getAmmo(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return 0;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().getOrDefault(ammoKey, PersistentDataType.INTEGER, 0);
    }

    /**
     * 指定されたItemStackに現在の残弾数を保存します
     */
    public void setAmmo(ItemStack item, int amount) {
        if (item == null || !item.hasItemMeta())
            return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        meta.getPersistentDataContainer().set(ammoKey, PersistentDataType.INTEGER, amount);
        item.setItemMeta(meta);
    }

    public GunData getGunData(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;
        ItemMeta meta = item.getItemMeta();
        String id = meta.getPersistentDataContainer().get(gunKey, PersistentDataType.STRING);
        return id != null ? guns.get(id) : null;
    }

    public Map<String, GunData> getGuns() {
        return guns;
    }
}
