package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.audit.AuditEntry;
import com.bloodline.crates.model.Crate;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GiveVirtualKeySubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public GiveVirtualKeySubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "givevirtualkey";
    }

    @Override
    public String getDescription() {
        return "Give virtual crate keys to a player";
    }

    @Override
    public String getUsage() {
        return "<crate_id> <player> [amount]";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.admin.givevirtualkey";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("gvk");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc givevirtualkey <crate_id> <player> [amount]");
            return;
        }

        String crateId = args[0];
        Optional<Crate> crateOpt = plugin.getConfigManager().getCrate(crateId);
        if (crateOpt.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cCrate not found: " + crateId);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cPlayer not found: " + args[1]);
            return;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cAmount must be at least 1");
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cInvalid amount: " + args[2]);
                return;
            }
        }

        Crate crate = crateOpt.get();
        plugin.getKeyManager().giveVirtualKey(target, crate.getId(), amount);
        plugin.getAuditLogger().log(new AuditEntry(
            null,
            sender.getName(),
            AuditEntry.AuditEventType.KEY_GIVEN,
            target.getName(),
            crate.getId() + " virtual x" + amount,
            System.currentTimeMillis()
        ));

        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aGave " + amount + "x virtual " + crate.getDisplayName() + " §6Key §ato " + target.getName());
        target.sendMessage(plugin.getConfigManager().getPrefix() + "§aYou received " + amount + "x virtual " + crate.getDisplayName() + " §6Key§a!");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getConfigManager().getAllCrates().stream()
                .map(Crate::getId)
                .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
