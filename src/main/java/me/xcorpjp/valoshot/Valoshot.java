package me.xcorpjp.valoshot;

import me.xcorpjp.valoshot.commands.ValoshotCommand;
import me.xcorpjp.valoshot.listeners.GunListener;
import me.xcorpjp.valoshot.manager.GunManager;
import me.xcorpjp.valoshot.model.GunData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Valoshot extends JavaPlugin {

    private static Valoshot instance;
    private GunManager gunManager;
    private GunListener gunListener;
    private final Map<UUID, Long> lastHeadshotTime = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.gunManager = new GunManager(this);
        this.gunManager.loadGuns();

        this.gunListener = new GunListener(this);
        ValoshotCommand cmd = new ValoshotCommand(this);
        getCommand("valoshot").setExecutor(cmd);
        getCommand("valoshot").setTabCompleter(cmd);
        getServer().getPluginManager().registerEvents(this.gunListener, this);

        startAmmoDisplayTask();

        getLogger().info("Valoshot has been enabled.");
    }

    private void startAmmoDisplayTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    GunData data = gunManager.getGunData(item);

                    if (data != null) {
                        if (gunListener.isReloading(player.getUniqueId()))
                            continue;

                        int ammo = gunManager.getAmmo(item);
                        String adsStatus = gunListener.isAds(player.getUniqueId()) ? " §b[ADS]" : "";
                        String ammoColor = (ammo <= 0) ? "§c" : "§e";
                        String emptyText = (ammo <= 0) ? " §7[EMPTY]" : "";

                        String coloredName = data.getDisplayName().replace("&", "§");
                        String headshotTag = "";
                        if (System.currentTimeMillis()
                                - lastHeadshotTime.getOrDefault(player.getUniqueId(), 0L) < 1500) {
                            headshotTag = " §6§l[HEADSHOT!]";
                        }

                        if (data.getCategory() == me.xcorpjp.valoshot.model.GunCategory.KNIFE) {
                            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                                    "§r" + coloredName + headshotTag + " §7| §fMelee Active"));
                        } else {
                            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                                    "§r" + coloredName + adsStatus + headshotTag + " §7| §fAmmo: " + ammoColor + ammo
                                            + " / "
                                            + data.getMagazineSize() + emptyText));
                        }
                    } else {
                        // 武器を持っていない場合はアクションバーをリセット
                        player.sendActionBar(Component.empty());
                    }
                }
            }
        }.runTaskTimer(this, 0L, 2L);
    }

    @Override
    public void onDisable() {
        getLogger().info("Valoshot has been disabled.");
    }

    public static Valoshot getInstance() {
        return instance;
    }

    public GunManager getGunManager() {
        return gunManager;
    }

    public void markHeadshot(UUID uuid) {
        lastHeadshotTime.put(uuid, System.currentTimeMillis());
    }
}
