package com.bloodlinecrates.command;

import com.bloodlinecrates.BloodlineCratesPlugin;
import com.bloodlinecrates.model.CooldownScope;
import com.bloodlinecrates.model.CrateDefinition;
import com.bloodlinecrates.model.CrateKey;
import com.bloodlinecrates.model.CrateType;
import com.bloodlinecrates.model.EffectSettings;
import com.bloodlinecrates.model.AnimationType;
import com.bloodlinecrates.model.RewardTier;
import com.bloodlinecrates.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class BloodlineCommand implements CommandExecutor, TabCompleter {
    private final BloodlineCratesPlugin plugin;

    public BloodlineCommand(BloodlineCratesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Text.color("&e/bloodcrates editor|open|preview|givekey|givecrate|reload|keys|test"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "editor" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Text.color(plugin.messages().getString("player-only")));
                    return true;
                }
                plugin.guiManager().openEditorMain(player);
            }
            case "open" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Text.color(plugin.messages().getString("player-only")));
                    return true;
                }
                if (args.length < 2) {
                    return true;
                }
                plugin.crateManager().get(args[1]).ifPresent(crate -> plugin.crateManager().open(player, crate, false, 1));
            }
            case "preview" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Text.color(plugin.messages().getString("player-only")));
                    return true;
                }
                if (args.length < 2) {
                    return true;
                }
                plugin.crateManager().get(args[1]).ifPresent(crate -> plugin.guiManager().openPreview(player, crate));
            }
            case "keys" -> {
                if (!(sender instanceof Player player)) {
                    return true;
                }
                plugin.keyManager().all().forEach(key -> player.sendMessage(Text.color("&7" + key.id() + ": &f" + plugin.keyManager().getVirtual(player.getUniqueId(), key.id()))));
            }
            case "test" -> {
                if (!(sender instanceof Player player) || args.length < 2) {
                    return true;
                }
                plugin.crateManager().get(args[1]).ifPresent(crate -> plugin.crateManager().open(player, crate, true, 1));
            }
            case "givekey" -> {
                if (args.length < 3) {
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    return true;
                }
                int amount = args.length > 3 ? Integer.parseInt(args[3]) : 1;
                plugin.keyManager().giveVirtual(target.getUniqueId(), args[1], amount);
                target.sendMessage(Text.color("&aYou received " + amount + " virtual key(s)."));
            }
            case "givecrate" -> {
                if (args.length < 3) {
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    return true;
                }
                plugin.crateManager().get(args[1]).ifPresent(crate -> {
                    if (crate.keyId() != null && !crate.keyId().isBlank()) {
                        ItemStack item = plugin.keyManager().createPhysicalKey(crate.keyId(), 1);
                        target.getInventory().addItem(item);
                    }
                });
            }
            case "reload" -> plugin.reloadPlugin();
            case "create" -> {
                if (args.length < 3) {
                    return true;
                }
                if (args[1].equalsIgnoreCase("crate")) {
                    createCrate(sender, args[2]);
                } else if (args[1].equalsIgnoreCase("key")) {
                    createKey(sender, args[2]);
                }
            }
        }
        return true;
    }

    public void createCrate(CommandSender sender, String id) {
        CrateDefinition crate = new CrateDefinition(id);
        crate.setDisplayName(id);
        crate.setType(CrateType.RNG);
        crate.setCooldownScope(CooldownScope.PER_CRATE);
        crate.setCooldownMillis(0L);
        crate.setEffectSettings(new EffectSettings(AnimationType.SPIN, 2, 40, true, true, true, "bloodlinecrates.skipanimation"));
        crate.setPityGuaranteedTier(RewardTier.RARE);
        plugin.crateManager().register(crate);
        plugin.auditLogManager().log(sender, "Created crate " + id);
    }

    public void createKey(CommandSender sender, String id) {
        CrateKey key = new CrateKey(id);
        key.setDisplayName(id);
        key.setPhysicalEnabled(true);
        key.setVirtualEnabled(true);
        key.setItem(new ItemStack(Material.TRIPWIRE_HOOK));
        plugin.keyManager().register(key);
        plugin.auditLogManager().log(sender, "Created key " + id);
    }

    public void deleteCrate(CommandSender sender, String id) {
        plugin.crateManager().delete(id);
        plugin.auditLogManager().log(sender, "Deleted crate " + id);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.addAll(List.of("editor", "open", "preview", "keys", "givekey", "givecrate", "reload", "create", "test"));
        } else if (args.length == 2 && List.of("open", "preview", "givekey", "givecrate", "test").contains(args[0].toLowerCase())) {
            plugin.crateManager().all().forEach(crate -> suggestions.add(crate.id()));
        }
        return suggestions;
    }
}
