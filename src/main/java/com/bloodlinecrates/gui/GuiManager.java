package com.bloodlinecrates.gui;

import com.bloodlinecrates.BloodlineCratesPlugin;
import com.bloodlinecrates.model.CrateDefinition;
import com.bloodlinecrates.model.CrateReward;
import com.bloodlinecrates.model.CrateType;
import com.bloodlinecrates.model.RewardTier;
import com.bloodlinecrates.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public final class GuiManager {
    private final BloodlineCratesPlugin plugin;
    private final Map<Player, SelectionContext> selectionContexts = new HashMap<>();
    private final Map<Player, EditorContext> editorContexts = new HashMap<>();

    public GuiManager(BloodlineCratesPlugin plugin) {
        this.plugin = plugin;
    }

    public void openEditorMain(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 27, Text.color("&4BloodlineCrates Editor"));
        inventory.setItem(11, icon(Material.CHEST, "&cCrates", "&7Manage crate definitions"));
        inventory.setItem(13, icon(Material.TRIPWIRE_HOOK, "&bKeys", "&7Manage key definitions"));
        inventory.setItem(15, icon(Material.EMERALD, "&aSave All", "&7Write cached changes"));
        fill(inventory);
        player.openInventory(inventory);
        editorContexts.put(player, new EditorContext(EditorView.MAIN, null));
    }

    public void openCrateBrowser(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 54, Text.color("&4Crate Editor"));
        int slot = 0;
        for (CrateDefinition crate : plugin.crateManager().all()) {
            inventory.setItem(slot++, icon(Material.CHEST, "&c" + crate.displayName(), "&7Type: " + crate.type(), "&7Shift-right click to delete"));
        }
        inventory.setItem(45, icon(Material.ANVIL, "&aCreate Crate", "&7Creates a new crate from held item name"));
        inventory.setItem(49, icon(Material.BARRIER, "&cBack", "&7Return to main editor"));
        fill(inventory);
        player.openInventory(inventory);
        editorContexts.put(player, new EditorContext(EditorView.CRATE_BROWSER, null));
    }

    public void openKeyBrowser(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 54, Text.color("&bKey Editor"));
        int slot = 0;
        for (com.bloodlinecrates.model.CrateKey key : plugin.keyManager().all()) {
            inventory.setItem(slot++, icon(Material.TRIPWIRE_HOOK, "&b" + key.displayName(), "&7ID: " + key.id(), "&7Shift-right click to delete"));
        }
        inventory.setItem(45, icon(Material.ANVIL, "&aCreate Key", "&7Creates a new key from held item name"));
        inventory.setItem(49, icon(Material.BARRIER, "&cBack", "&7Return to main editor"));
        fill(inventory);
        player.openInventory(inventory);
        editorContexts.put(player, new EditorContext(EditorView.KEY_BROWSER, null));
    }

    public void openCrateEditor(Player player, CrateDefinition crate) {
        Inventory inventory = Bukkit.createInventory(player, 45, Text.color("&4Edit " + crate.id()));
        inventory.setItem(10, icon(Material.COMPASS, "&eType: " + crate.type(), "&7Click to toggle between RNG and SELECTABLE"));
        inventory.setItem(11, icon(Material.CLOCK, "&eCooldown", "&7Current: " + (crate.cooldownMillis() / 1000L) + "s", "&7Left +5s, Right -5s"));
        inventory.setItem(12, icon(Material.ENDER_EYE, "&eBind Location", "&7Adds current block location"));
        inventory.setItem(13, icon(Material.BLAZE_POWDER, "&eAnimation: " + crate.effectSettings().animationType(), "&7Cycle animation type"));
        inventory.setItem(14, icon(Material.NETHER_STAR, "&ePity", "&7Threshold: " + crate.pityThreshold(), "&7Click to toggle"));
        inventory.setItem(15, icon(Material.CHEST_MINECART, "&eRewards", "&7Manage crate rewards"));
        inventory.setItem(31, icon(Material.BARRIER, "&cBack", "&7Return to crate browser"));
        fill(inventory);
        player.openInventory(inventory);
        editorContexts.put(player, new EditorContext(EditorView.CRATE_DETAIL, crate.id()));
    }

    public void openRewardEditor(Player player, CrateDefinition crate) {
        Inventory inventory = Bukkit.createInventory(player, 54, Text.color("&6Rewards " + crate.id()));
        int slot = 0;
        for (CrateReward reward : plugin.crateManager().sortedRewards(crate)) {
            inventory.setItem(slot++, rewardIcon(reward, crate.type() == CrateType.RNG && crate.previewChances()));
        }
        inventory.setItem(45, icon(Material.ANVIL, "&aAdd Reward From Hand", "&7Copies the item in your hand"));
        inventory.setItem(49, icon(Material.BARRIER, "&cBack", "&7Return to crate editor"));
        fill(inventory);
        player.openInventory(inventory);
        editorContexts.put(player, new EditorContext(EditorView.REWARD_BROWSER, crate.id()));
    }

    public void openSelectableCrate(Player player, CrateDefinition crate, boolean testMode) {
        Inventory inventory = Bukkit.createInventory(player, 54, Text.color("&6Select Reward"));
        int slot = 0;
        for (CrateReward reward : plugin.crateManager().sortedRewards(crate)) {
            if (!reward.permission().isBlank() && !player.hasPermission(reward.permission())) {
                continue;
            }
            inventory.setItem(slot++, rewardIcon(reward, false));
        }
        inventory.setItem(49, icon(Material.BARRIER, "&cClose", "&7Do not claim anything"));
        fill(inventory);
        player.openInventory(inventory);
        selectionContexts.put(player, new SelectionContext(crate.id(), testMode));
    }

    public void openPreview(Player player, CrateDefinition crate) {
        Inventory inventory = Bukkit.createInventory(player, 54, Text.color("&ePreview " + crate.id()));
        int slot = 0;
        for (CrateReward reward : plugin.crateManager().sortedRewards(crate)) {
            inventory.setItem(slot++, rewardIcon(reward, crate.type() == CrateType.RNG && crate.previewChances()));
        }
        fill(inventory);
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        SelectionContext selectionContext = selectionContexts.get(player);
        if (selectionContext != null) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) {
                return;
            }
            if (event.getSlot() == 49) {
                player.closeInventory();
                return;
            }
            CrateDefinition crate = plugin.crateManager().get(selectionContext.crateId()).orElse(null);
            if (crate == null) {
                return;
            }
            CrateReward reward = plugin.crateManager().sortedRewards(crate).stream()
                    .filter(candidate -> candidate.item() != null && candidate.item().isSimilar(event.getCurrentItem()))
                    .findFirst()
                    .orElse(null);
            if (reward != null) {
                plugin.crateManager().finalizeSelectableReward(player, crate, reward, selectionContext.testMode(), 1);
                player.closeInventory();
            }
            return;
        }

        EditorContext editorContext = editorContexts.get(player);
        if (editorContext == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getCurrentItem() == null) {
            return;
        }

        switch (editorContext.view()) {
            case MAIN -> handleMain(player, event.getSlot());
            case CRATE_BROWSER -> handleCrateBrowser(player, event);
            case KEY_BROWSER -> handleKeyBrowser(player, event);
            case CRATE_DETAIL -> handleCrateDetail(player, editorContext.targetId(), event.getSlot());
            case REWARD_BROWSER -> handleRewardBrowser(player, editorContext.targetId(), event);
        }
    }

    public void handleClose(InventoryCloseEvent event) {
        HumanEntity entity = event.getPlayer();
        selectionContexts.remove(entity);
    }

    private void handleMain(Player player, int slot) {
        if (slot == 11) {
            openCrateBrowser(player);
        } else if (slot == 13) {
            openKeyBrowser(player);
        } else if (slot == 15) {
            plugin.saveAllData();
            player.sendMessage(Text.color(plugin.messages().getString("editor-saved")));
        }
    }

    private void handleCrateBrowser(Player player, InventoryClickEvent event) {
        if (event.getSlot() == 45) {
            plugin.commandHandler().createCrate(player, "crate_" + System.currentTimeMillis());
            openCrateBrowser(player);
            return;
        }
        if (event.getSlot() == 49) {
            openEditorMain(player);
            return;
        }
        CrateDefinition crate = plugin.crateManager().all().stream().skip(event.getSlot()).findFirst().orElse(null);
        if (crate == null) {
            return;
        }
        if (event.getClick() == ClickType.SHIFT_RIGHT) {
            plugin.commandHandler().deleteCrate(player, crate.id());
            openCrateBrowser(player);
            return;
        }
        openCrateEditor(player, crate);
    }

    private void handleKeyBrowser(Player player, InventoryClickEvent event) {
        if (event.getSlot() == 45) {
            plugin.commandHandler().createKey(player, "key_" + System.currentTimeMillis());
            openKeyBrowser(player);
            return;
        }
        if (event.getSlot() == 49) {
            openEditorMain(player);
        }
    }

    private void handleCrateDetail(Player player, String crateId, int slot) {
        CrateDefinition crate = plugin.crateManager().get(crateId).orElse(null);
        if (crate == null) {
            return;
        }
        if (slot == 10) {
            crate.setType(crate.type() == CrateType.RNG ? CrateType.SELECTABLE : CrateType.RNG);
        } else if (slot == 11) {
            crate.setCooldownMillis(crate.cooldownMillis() + 5000L);
        } else if (slot == 12) {
            crate.locations().add(com.bloodlinecrates.model.CrateLocation.fromLocation(player.getLocation().getBlock().getLocation().add(0.5D, 0.0D, 0.5D)));
        } else if (slot == 13) {
            com.bloodlinecrates.model.AnimationType[] values = com.bloodlinecrates.model.AnimationType.values();
            int next = (crate.effectSettings().animationType().ordinal() + 1) % values.length;
            crate.setEffectSettings(new com.bloodlinecrates.model.EffectSettings(values[next], crate.effectSettings().speedTicks(), crate.effectSettings().durationTicks(), crate.effectSettings().particles(), crate.effectSettings().sounds(), crate.effectSettings().synced(), crate.effectSettings().skipPermission()));
        } else if (slot == 14) {
            crate.setPityEnabled(!crate.pityEnabled());
        } else if (slot == 15) {
            openRewardEditor(player, crate);
            return;
        } else if (slot == 31) {
            openCrateBrowser(player);
            return;
        }
        openCrateEditor(player, crate);
    }

    private void handleRewardBrowser(Player player, String crateId, InventoryClickEvent event) {
        CrateDefinition crate = plugin.crateManager().get(crateId).orElse(null);
        if (crate == null) {
            return;
        }
        if (event.getSlot() == 45) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType() != Material.AIR) {
                String rewardId = "reward_" + (crate.rewards().size() + 1);
                CrateReward reward = new CrateReward(rewardId);
                reward.setItem(held.clone());
                reward.setTier(RewardTier.COMMON);
                reward.setChance(100.0D);
                crate.rewards().put(rewardId, reward);
            }
            openRewardEditor(player, crate);
            return;
        }
        if (event.getSlot() == 49) {
            openCrateEditor(player, crate);
            return;
        }
        CrateReward reward = plugin.crateManager().sortedRewards(crate).stream().skip(event.getSlot()).findFirst().orElse(null);
        if (reward == null) {
            return;
        }
        if (event.getClick() == ClickType.SHIFT_RIGHT) {
            crate.rewards().remove(reward.id());
        } else if (event.getClick() == ClickType.RIGHT) {
            reward.setChance(Math.max(1.0D, reward.chance() - 5.0D));
        } else {
            reward.setChance(reward.chance() + 5.0D);
        }
        openRewardEditor(player, crate);
    }

    private ItemStack rewardIcon(CrateReward reward, boolean showChance) {
        ItemStack item = reward.item() == null ? new ItemStack(Material.CHEST) : reward.item().clone();
        ItemMeta meta = item.getItemMeta();
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("&7Tier: " + reward.tier());
        if (showChance) {
            lore.add("&7Chance: " + reward.chance() + "%");
        }
        if (!reward.permission().isBlank()) {
            lore.add("&7Permission: " + reward.permission());
        }
        lore.add("&8Left/right click to adjust");
        lore.add("&8Shift-right click to delete");
        meta.lore(Text.color(lore).stream().map(Text::component).toList());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack icon(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.component(name));
        meta.lore(java.util.Arrays.stream(lore).map(Text::color).map(Text::component).toList());
        item.setItemMeta(meta);
        return item;
    }

    private void fill(Inventory inventory) {
        if (!plugin.getConfig().getBoolean("gui.fill_empty_slots", true)) {
            return;
        }
        ItemStack filler = icon(Material.matchMaterial(plugin.getConfig().getString("gui.filler_material", "BLACK_STAINED_GLASS_PANE")), " ");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private record SelectionContext(String crateId, boolean testMode) {
    }

    private record EditorContext(EditorView view, String targetId) {
    }

    private enum EditorView {
        MAIN,
        CRATE_BROWSER,
        KEY_BROWSER,
        CRATE_DETAIL,
        REWARD_BROWSER
    }
}
