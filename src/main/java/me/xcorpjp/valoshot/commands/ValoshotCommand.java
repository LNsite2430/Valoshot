package me.xcorpjp.valoshot.commands;

import me.xcorpjp.valoshot.Valoshot;
import me.xcorpjp.valoshot.model.GunData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /valoshot コマンドの実行およびタブ補完を管理するクラス
 */
public class ValoshotCommand implements CommandExecutor, TabCompleter {
    private final Valoshot plugin;

    public ValoshotCommand(Valoshot plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§e[Valoshot] §f使用法: /valoshot <give|list|reload>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("valoshot.admin")) {
                player.sendMessage("§e[Valoshot] §cこのコマンドを実行する権限がありません (valoshot.admin)。");
                return true;
            }
            // config.yml をリロードして武器データを再読み込み
            plugin.reloadConfig();
            plugin.getGunManager().loadGuns();
            player.sendMessage("§e[Valoshot] §a設定ファイルをリロードしました！");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            player.sendMessage("§e[Valoshot] §f登録されている武器一覧:");
            for (String gunId : plugin.getGunManager().getGuns().keySet()) {
                GunData data = plugin.getGunManager().getGuns().get(gunId);
                player.sendMessage(" §7- §f" + gunId + " §7(§e" + data.getDisplayName() + "§7)");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("give") && args.length > 1) {
            String gunId = args[1];
            ItemStack item = plugin.getGunManager().createGunItem(gunId);
            if (item != null) {
                player.getInventory().addItem(item);
                player.sendMessage("§e[Valoshot] §f" + gunId + " を付与しました。");
            } else {
                player.sendMessage("§e[Valoshot] §c指定された武器が見つかりません。");
            }
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("give");
            completions.add("list");
            if (sender.hasPermission("valoshot.admin")) {
                completions.add("reload");
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return plugin.getGunManager().getGuns().keySet().stream().collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
