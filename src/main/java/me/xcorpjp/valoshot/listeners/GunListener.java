package me.xcorpjp.valoshot.listeners;

import me.xcorpjp.valoshot.Valoshot;

import me.xcorpjp.valoshot.model.GunCategory;
import me.xcorpjp.valoshot.model.GunData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GunListener implements Listener {
    private final Valoshot plugin;
    private final Map<UUID, Long> lastShot = new HashMap<>();
    private final Map<UUID, Integer> sprayCount = new HashMap<>();
    private final Set<UUID> adsPlayers = new HashSet<>();
    private final Set<UUID> reloadingPlayers = new HashSet<>();
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> reloadTasks = new HashMap<>();

    // リコイルリカバリー管理
    private final Map<UUID, Float> pitchToRecover = new HashMap<>();
    private final Map<UUID, Float> yawToRecover = new HashMap<>();
    private final Map<UUID, Float> lastAssignedYaw = new HashMap<>();
    private final Map<UUID, Float> lastAssignedPitch = new HashMap<>();

    // 連射速度およびフルオート管理
    private final Map<UUID, Long> shootingUntil = new HashMap<>();

    // ストッピング検知用
    private final Map<UUID, Location> lastLocation = new HashMap<>();
    private final Map<UUID, Double> lastMoveDist = new HashMap<>();

    public GunListener(Valoshot plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    private void startUpdateTask() {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    updateMovementStatus(player, uuid);
                    handleFullAutoTick(player, uuid);
                    handleRecoilRecoveryTick(player, uuid);

                    // ポーション効果を毎ティック更新して、ズーム表示を維持
                    ItemStack item = player.getInventory().getItemInMainHand();
                    applyMovementStatus(player, plugin.getGunManager().getGunData(item));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void updateMovementStatus(Player player, UUID uuid) {
        Location current = player.getLocation();
        Location last = lastLocation.get(uuid);
        if (last != null && last.getWorld().equals(current.getWorld())) {
            // 水平移動のみを計測
            double dist = Math
                    .sqrt(Math.pow(current.getX() - last.getX(), 2) + Math.pow(current.getZ() - last.getZ(), 2));
            lastMoveDist.put(uuid, dist);
        } else {
            lastMoveDist.put(uuid, 0.0);
        }
        lastLocation.put(uuid, current.clone());
    }

    private void handleFullAutoTick(Player player, UUID uuid) {
        long until = shootingUntil.getOrDefault(uuid, 0L);
        long now = System.currentTimeMillis();
        if (now > until)
            return;

        if (reloadingPlayers.contains(uuid))
            return;

        ItemStack item = player.getInventory().getItemInMainHand();
        GunData data = plugin.getGunManager().getGunData(item);
        if (data == null || !data.isFullAuto()) {
            shootingUntil.put(uuid, 0L);
            return;
        }

        shootProcedure(player, item, data, uuid, now);
    }

    private void handleRecoilRecoveryTick(Player player, UUID uuid) {
        if (!pitchToRecover.containsKey(uuid))
            return;

        long now = System.currentTimeMillis();
        if (now < shootingUntil.getOrDefault(uuid, 0L) || now - lastShot.getOrDefault(uuid, 0L) < 250)
            return;

        Location currentLoc = player.getLocation();
        float currentYaw = currentLoc.getYaw();
        float currentPitch = currentLoc.getPitch();

        // プレイヤーの自発的なエイム操作を検知した場合は制御を離す
        if (lastAssignedYaw.containsKey(uuid)) {
            float dY = Math.abs(currentYaw - lastAssignedYaw.get(uuid));
            float dP = Math.abs(currentPitch - lastAssignedPitch.get(uuid));

            if (dY > 0.02f || dP > 0.02f) {
                clearRecoil(uuid);
                return;
            }
        }

        float pRem = pitchToRecover.getOrDefault(uuid, 0f);
        float yRem = yawToRecover.getOrDefault(uuid, 0f);

        if (Math.abs(pRem) < 0.05f && Math.abs(yRem) < 0.05f) {
            clearRecoil(uuid);
            return;
        }

        // 指数減衰によるスムーズなリカバリーを実現
        float pStep = pRem * 0.08f;
        float yStep = yRem * 0.08f;

        if (Math.abs(pStep) < 0.01f)
            pStep = (pRem > 0 ? 0.01f : -0.01f);
        if (Math.abs(yStep) < 0.01f)
            yStep = (yRem > 0 ? 0.01f : -0.01f);

        float nP = currentPitch + pStep;
        float nY = currentYaw - yStep;

        player.setRotation(nY, nP);

        lastAssignedYaw.put(uuid, nY);
        lastAssignedPitch.put(uuid, nP);
        pitchToRecover.put(uuid, pRem - pStep);
        yawToRecover.put(uuid, yRem - yStep);
    }

    private void clearRecoil(UUID uuid) {
        pitchToRecover.remove(uuid);
        yawToRecover.remove(uuid);
        lastAssignedYaw.remove(uuid);
        lastAssignedPitch.remove(uuid);
    }

    private final Map<UUID, Long> lastInteract = new HashMap<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND)
            return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        long now = System.currentTimeMillis();
        if (now - lastInteract.getOrDefault(uuid, 0L) < 100)
            return;
        lastInteract.put(uuid, now);

        ItemStack item = player.getInventory().getItemInMainHand();
        GunData data = plugin.getGunManager().getGunData(item);
        if (data == null)
            return;

        if (!player.isSneaking()) {
            event.setCancelled(true);
        }

        Action action = event.getAction();

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            if (data.getCategory() == GunCategory.KNIFE) {
                event.setCancelled(true);
                return;
            }

            if (reloadingPlayers.contains(uuid))
                return;

            if (data.canAds()) {
                if (adsPlayers.contains(uuid)) {
                    adsPlayers.remove(uuid);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.2f);
                } else {
                    adsPlayers.add(uuid);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.8f);
                }
                applyMovementStatus(player, data);
            }
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (data.getCategory() == GunCategory.KNIFE) {
                knifeProcedure(player, data, uuid, now, false);
                return;
            }

            shootProcedure(player, item, data, uuid, now);
            if (data.isFullAuto()) {
                shootingUntil.put(uuid, now + 110);
            }
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        handleSecondaryInteract(event.getPlayer(), event);
    }

    @EventHandler
    public void onAtEntityInteract(PlayerInteractAtEntityEvent event) {
        handleSecondaryInteract(event.getPlayer(), event);
    }

    private void handleSecondaryInteract(Player player, org.bukkit.event.player.PlayerEvent event) {
        if (event instanceof org.bukkit.event.Cancellable cancellable) {
            ItemStack item = player.getInventory().getItemInMainHand();
            GunData data = plugin.getGunManager().getGunData(item);
            if (data != null) {
                // 武器所持時はエンティティへの操作（ライド・取引等）をキャンセルして武器アクションを優先
                cancellable.setCancelled(true);

                long now = System.currentTimeMillis();
                UUID uuid = player.getUniqueId();

                if (data.getCategory() == GunCategory.KNIFE) {
                    knifeProcedure(player, data, uuid, now, false);
                } else {
                    shootProcedure(player, item, data, uuid, now);
                    if (data.isFullAuto()) {
                        shootingUntil.put(uuid, now + 110);
                    }
                }
            }
        }
    }

    private void knifeProcedure(Player player, GunData data, UUID uuid, long now, boolean isStrong) {
        long cooldownMs = 600L;
        if (now - lastShot.getOrDefault(uuid, 0L) < cooldownMs) {
            return;
        }

        lastShot.put(uuid, now);
        player.swingMainHand();
        performMeleeTrace(player, data, isStrong);
    }

    private void performMeleeTrace(Player shooter, GunData data, boolean isStrong) {
        Location start = shooter.getEyeLocation();
        Vector direction = start.getDirection();
        double range = isStrong ? 3.2 : 2.8;

        // 当たり判定の太さを調整して命中しやすく改善
        RayTraceResult result = shooter.getWorld().rayTrace(start, direction, range,
                org.bukkit.FluidCollisionMode.NEVER, true, 0.4, entity -> entity != shooter);

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            target.setNoDamageTicks(0);
            double damage = isStrong ? data.getHeadDamage() : data.getBodyDamage();

            // 背後からの攻撃（バックスタブ）判定
            Vector shooterLook = shooter.getLocation().getDirection().setY(0).normalize();
            Vector targetLook = target.getLocation().getDirection().setY(0).normalize();
            if (shooterLook.dot(targetLook) > 0.6) {
                damage *= 2.0;
                shooter.sendActionBar(LegacyComponentSerializer.legacySection().deserialize("§6§lBACKSTAB!"));
                shooter.getWorld().playSound(target.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            }

            target.damage(damage, shooter);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
            target.getWorld().spawnParticle(org.bukkit.Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
        } else {
            shooter.getWorld().playSound(shooter.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        }
    }

    private void shootProcedure(Player player, ItemStack item, GunData data, UUID uuid, long now) {
        // 連打によるレート制限無視の防止
        long fireRateMs = data.getFireRate() * 50L;
        if (now - lastShot.getOrDefault(uuid, 0L) < fireRateMs) {
            return;
        }

        int currentAmmo = plugin.getGunManager().getAmmo(item);
        if (currentAmmo <= 0) {
            shootingUntil.put(uuid, 0L);
            if (!reloadingPlayers.contains(uuid)) {
                player.sendActionBar(
                        LegacyComponentSerializer.legacySection().deserialize("§cOut of Ammo! Reloading..."));
                reload(player, data, item);
            }
            return;
        }

        if (now - lastShot.getOrDefault(uuid, 0L) > fireRateMs + 250) {
            sprayCount.put(uuid, 0);
        }
        int count = sprayCount.getOrDefault(uuid, 0) + 1;
        sprayCount.put(uuid, count);

        shoot(player, data, uuid);
        applyImpactRecoil(player, data, count);

        plugin.getGunManager().setAmmo(item, currentAmmo - 1);
        lastShot.put(uuid, now);

        // セミオート武器の長押し連打をバニラのクールダウン機能で防止
        if (!data.isFullAuto()) {
            player.setCooldown(item.getType(), Math.max(data.getFireRate(), 5));
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player shooter) {
            ItemStack item = shooter.getInventory().getItemInMainHand();
            if (plugin.getGunManager().getGunData(item) != null) {
                // 銃によるダメージ時のノックバックを大幅に軽減し、トラッキングを容易にする
                org.bukkit.entity.Entity target = event.getEntity();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (target.isValid()) {
                        // ノックバックを半分（0.5倍）に抑制
                        target.setVelocity(target.getVelocity().multiply(0.5));
                    }
                });
            }
        }
    }

    private void applyImpactRecoil(Player player, GunData data, int count) {
        if (data.getRecoil() <= 0)
            return;
        Location loc = player.getLocation();
        UUID uuid = player.getUniqueId();

        float factor = (float) data.getRecoilFactor();
        float vKick = (float) (data.getRecoil() * 4.8 * factor * (1.0 + Math.min(count, 12) * 0.1));
        boolean isRiffleSmg = data.getRecoilFactor() <= 1.0;

        float swayMultiplier = isRiffleSmg ? ((count > 5) ? 1.2f : 0.35f) : ((count > 3) ? 4.8f : 2.5f);
        float hSway = (float) ((Math.random() - 0.5) * data.getRecoil() * factor * swayMultiplier
                * (count * 0.5 + 1.0));

        float nY = loc.getYaw() + hSway;
        float nP = Math.max(-90f, loc.getPitch() - vKick);

        player.setRotation(nY, nP);
        lastAssignedYaw.put(uuid, nY);
        lastAssignedPitch.put(uuid, nP);
        pitchToRecover.put(uuid, pitchToRecover.getOrDefault(uuid, 0f) + vKick);
        yawToRecover.put(uuid, yawToRecover.getOrDefault(uuid, 0f) + hSway);
    }

    private void shoot(Player player, GunData data, UUID uuid) {
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection();

        boolean isADS = adsPlayers.contains(uuid);
        double spread = isADS ? data.getAdsAccuracy() : data.getAccuracy();

        double moveDist = lastMoveDist.getOrDefault(uuid, 0.0);
        // 滑り時間を考慮した移動判定
        boolean isMoving = moveDist > 0.08;

        if (isMoving) {
            spread += data.getMovementSpread() * (isADS ? data.getAdsMoveSpreadMult() : data.getMoveSpreadMult());
        } else if (!(data.getId().equals("operator") && !isADS)) {
            // 静止（ストッピング）成功時は精度を最大化（オペレーターの腰撃ち以外）
            spread = 0.0;
        }

        if (!((org.bukkit.entity.Entity) player).isOnGround()) {
            spread += data.getMovementSpread() * (isADS ? data.getAdsAirSpreadMult() : data.getAirSpreadMult());
        }

        if (spread > 0) {
            direction.add(new Vector((Math.random() - 0.5) * spread, (Math.random() - 0.5) * spread,
                    (Math.random() - 0.5) * spread));
        }

        int pellets = data.getPellets();
        for (int i = 0; i < pellets; i++) {
            Vector d = direction.clone();
            if (pellets > 1) {
                // ショショットガンの散弾集弾
                d.add(new Vector((Math.random() - 0.5) * 0.1, (Math.random() - 0.5) * 0.1,
                        (Math.random() - 0.5) * 0.1));
            }
            performRayTrace(player, start, d, data);
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 2.0f);
    }

    private void performRayTrace(Player shooter, Location start, Vector direction, GunData data) {
        double range = 100.0;
        RayTraceResult result = shooter.getWorld().rayTrace(start, direction, range,
                org.bukkit.FluidCollisionMode.NEVER, true, 0.1, entity -> entity != shooter);
        Location end;
        if (result != null) {
            end = result.getHitPosition().toLocation(shooter.getWorld());
            if (result.getHitEntity() instanceof LivingEntity target) {
                target.setNoDamageTicks(0);

                // ヒット位置の高さによる部位ダメージの振り分け
                double hitY = result.getHitPosition().getY();
                double entityY = target.getLocation().getY();
                double height = target.getHeight();
                double relY = (hitY - entityY) / height;

                double finalDmg;
                if (relY > 0.8) {
                    finalDmg = data.getHeadDamage();
                    plugin.markHeadshot(shooter.getUniqueId());
                } else if (relY > 0.4) {
                    finalDmg = data.getBodyDamage();
                } else {
                    finalDmg = data.getLegDamage();
                }

                target.damage(finalDmg, shooter);
            }
            end.getWorld().spawnParticle(Particle.CRIT, end, 2, 0, 0, 0, 0.05);
        } else {
            end = start.clone().add(direction.multiply(range));
        }
        drawSimpleTracer(start, end);
    }

    private void drawSimpleTracer(Location start, Location end) {
        double distance = start.distance(end);
        Vector dir = end.toVector().subtract(start.toVector()).normalize();

        // 視界を遮らないよう、出現位置を前方へ調整してトレイルを発生
        for (double d = 1.5; d < distance; d += 0.3) {
            Location p = start.clone().add(dir.clone().multiply(d));
            start.getWorld().spawnParticle(Particle.CRIT, p, 1, 0, 0, 0, 0.0);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        GunData data = plugin.getGunManager().getGunData(item);
        if (data == null)
            return;

        event.setCancelled(true);
        if (!reloadingPlayers.contains(player.getUniqueId())) {
            reload(player, data, item);
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        handleActionHandChange(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            // インベントリ操作が行われた次のティックに持ち物を確認
            plugin.getServer().getScheduler().runTask(plugin, () -> handleActionHandChange(player));
        }
    }

    private void handleActionHandChange(Player player) {
        UUID uuid = player.getUniqueId();
        if (adsPlayers.contains(uuid) || reloadingPlayers.contains(uuid)) {
            ItemStack item = player.getInventory().getItemInMainHand();
            GunData data = plugin.getGunManager().getGunData(item);

            // 手に持っているものが変わった、あるいはADSできないものになったなら解除
            if (data == null) {
                clearAds(player);
                reloadingPlayers.remove(uuid);
                if (reloadTasks.containsKey(uuid)) {
                    reloadTasks.get(uuid).cancel();
                    reloadTasks.remove(uuid);
                }
                player.removePotionEffect(PotionEffectType.SLOW);
                clearRecoil(uuid);
            }
        }
    }

    @EventHandler
    public void onSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (reloadTasks.containsKey(uuid)) {
            reloadTasks.get(uuid).cancel();
            reloadTasks.remove(uuid);
        }

        clearAds(player);
        clearRecoil(uuid);
        reloadingPlayers.remove(uuid);
        sprayCount.remove(uuid);
        shootingUntil.put(uuid, 0L);
        player.removePotionEffect(PotionEffectType.SLOW);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (reloadTasks.containsKey(uuid)) {
            reloadTasks.get(uuid).cancel();
            reloadTasks.remove(uuid);
        }
        adsPlayers.remove(uuid);
        reloadingPlayers.remove(uuid);
        shootingUntil.remove(uuid);
        clearRecoil(uuid);
        lastLocation.remove(uuid);
        lastMoveDist.remove(uuid);
    }

    @EventHandler
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        if (reloadTasks.containsKey(uuid)) {
            reloadTasks.get(uuid).cancel();
            reloadTasks.remove(uuid);
        }
        clearAds(event.getEntity());
        clearRecoil(uuid);
    }

    public boolean isAds(UUID uuid) {
        return adsPlayers.contains(uuid);
    }

    public boolean isReloading(UUID uuid) {
        return reloadingPlayers.contains(uuid);
    }

    private void applyMovementStatus(Player player, GunData data) {
        UUID uuid = player.getUniqueId();

        int targetAmpli = -1;
        if (reloadingPlayers.contains(uuid)) {
            targetAmpli = 2;
        } else if (adsPlayers.contains(uuid) && data != null) {
            double configZoom = data.getAdsZoom();
            if (configZoom > 0) {
                targetAmpli = Math.min((int) Math.floor(configZoom * 0.5), 9);
            }
        }

        PotionEffect current = player.getPotionEffect(PotionEffectType.SLOW);

        if (targetAmpli < 0) {
            if (current != null && current.getDuration() > 2000) {
                player.removePotionEffect(PotionEffectType.SLOW);
            }
            return;
        }

        if (current == null || current.getAmplifier() != targetAmpli || current.getDuration() < 1000) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100000, targetAmpli, false, false, false));
        }
    }

    private void clearAds(Player player) {
        adsPlayers.remove(player.getUniqueId());
    }

    private void reload(Player player, GunData data, ItemStack originalItem) {
        UUID uuid = player.getUniqueId();

        if (reloadTasks.containsKey(uuid)) {
            return;
        }

        reloadingPlayers.add(uuid);
        shootingUntil.put(uuid, 0L);
        clearAds(player);
        clearRecoil(uuid);
        applyMovementStatus(player, data);

        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize("§7Reloading..."));
        player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 0.5f, 1.5f);

        String gunId = data.getId();

        org.bukkit.scheduler.BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                reloadTasks.remove(uuid);
                if (!player.isOnline()) {
                    reloadingPlayers.remove(uuid);
                    return;
                }

                ItemStack targetItem = null;
                for (ItemStack invItem : player.getInventory().getContents()) {
                    if (invItem == null)
                        continue;
                    GunData invData = plugin.getGunManager().getGunData(invItem);
                    if (invData != null && invData.getId().equals(gunId)) {
                        targetItem = invItem;
                        break;
                    }
                }

                if (targetItem != null) {
                    plugin.getGunManager().setAmmo(targetItem, data.getMagazineSize());
                    player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize("§aReloaded!"));
                    player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 0.5f, 1.5f);
                }

                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        reloadingPlayers.remove(uuid);
                        clearAds(player);
                        applyMovementStatus(player,
                                plugin.getGunManager().getGunData(player.getInventory().getItemInMainHand()));
                    }
                }.runTaskLater(plugin, 10L);
            }
        }.runTaskLater(plugin, (long) (data.getReloadTime() * 20));

        reloadTasks.put(uuid, task);
    }
}
