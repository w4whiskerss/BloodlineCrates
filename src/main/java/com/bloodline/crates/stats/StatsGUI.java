package com.bloodline.crates.stats;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.util.ItemBuilder;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class StatsGUI implements Listener {
    private static final int PAGE_SIZE = 7;
    private static final int[] CRATE_SLOTS = {1, 2, 3, 4, 5, 6, 7};

    private final BloodlineCrates plugin;
    private final Map<Inventory, Session> sessions = new LinkedHashMap<>();

    public StatsGUI(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, OfflinePlayer target) {
        PlayerStats stats = plugin.getStatsManager().getStats(target.getUniqueId());
        String targetName = resolveName(target, stats);
        Inventory inventory = Bukkit.createInventory(null, 27,
            ChatColor.translateAlternateColorCodes('&', "&8Stats - " + targetName));

        Session session = new Session(viewer.getUniqueId(), target.getUniqueId(), targetName, inventory);
        List<String> crateIds = new ArrayList<>(stats.getOpensPerCrate().keySet());
        crateIds.removeIf(crateId -> stats.getOpensPerCrate().getOrDefault(crateId, 0) <= 0);
        crateIds.sort(Comparator.naturalOrder());
        session.crateIds().addAll(crateIds);
        if (!crateIds.isEmpty()) {
            session.setSelectedCrateId(crateIds.get(0));
        }

        sessions.put(inventory, session);
        render(session);
        viewer.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Session session = sessions.get(event.getView().getTopInventory());
        if (session == null) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getInventory().getSize()) {
            return;
        }

        if (rawSlot == 18 || rawSlot == 26) {
            player.closeInventory();
            return;
        }
        if (rawSlot == 21 && session.page() > 0) {
            session.setPage(session.page() - 1);
            render(session);
            return;
        }
        if (rawSlot == 23 && (session.page() + 1) * PAGE_SIZE < session.crateIds().size()) {
            session.setPage(session.page() + 1);
            render(session);
            return;
        }

        for (int i = 0; i < CRATE_SLOTS.length; i++) {
            if (rawSlot != CRATE_SLOTS[i]) {
                continue;
            }
            int index = session.page() * PAGE_SIZE + i;
            if (index >= 0 && index < session.crateIds().size()) {
                session.setSelectedCrateId(session.crateIds().get(index));
                render(session);
            }
            return;
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        sessions.remove(event.getInventory());
    }

    private void render(Session session) {
        Inventory inventory = session.inventory();
        inventory.clear();
        fill(inventory);

        PlayerStats stats = plugin.getStatsManager().getStats(session.targetUUID());
        OfflinePlayer target = Bukkit.getOfflinePlayer(session.targetUUID());
        inventory.setItem(13, buildGlobalInfo(target, stats, session.targetName()));
        inventory.setItem(15, buildSelectedInfo(stats, session.selectedCrateId()));

        int start = session.page() * PAGE_SIZE;
        for (int i = 0; i < CRATE_SLOTS.length; i++) {
            int index = start + i;
            if (index >= session.crateIds().size()) {
                continue;
            }
            String crateId = session.crateIds().get(index);
            inventory.setItem(CRATE_SLOTS[i], buildCrateIcon(crateId, stats, crateId.equals(session.selectedCrateId())));
        }

        inventory.setItem(18, new ItemBuilder(XMaterial.ARROW).name("&cBack").lore("&7Close this menu").build());
        if (session.page() > 0) {
            inventory.setItem(21, new ItemBuilder(XMaterial.ARROW).name("&ePrevious Page").lore("&7View earlier crates").build());
        }
        if ((session.page() + 1) * PAGE_SIZE < session.crateIds().size()) {
            inventory.setItem(23, new ItemBuilder(XMaterial.ARROW).name("&eNext Page").lore("&7View more crates").build());
        }
        inventory.setItem(26, new ItemBuilder(XMaterial.BARRIER).name("&cClose").lore("&7Exit stats view").build());
    }

    private ItemStack buildCrateIcon(String crateId, PlayerStats stats, boolean selected) {
        Optional<Crate> crateOpt = plugin.getConfigManager().getCrate(crateId);
        String crateName = crateOpt.map(Crate::getDisplayName).orElse(crateId);
        List<String> lore = new ArrayList<>();
        lore.add("&7Opens: &f" + stats.getOpensPerCrate().getOrDefault(crateId, 0));
        lore.add("&7Rarest Win: &f" + stats.getRarestRewardPerCrate().getOrDefault(crateId, "None"));
        lore.add("&7Rarest Chance: &f" + plugin.getStatsManager().formatChance(stats.getRarestChancePerCrate().get(crateId)));
        lore.add("&7Last Opened: &f" + plugin.getStatsManager().formatRelative(stats.getLastOpenedPerCrate().getOrDefault(crateId, 0L)));
        lore.add("&7Current Pity: &f" + stats.getCurrentPityPerCrate().getOrDefault(crateId, 0));
        lore.add(selected ? "&aSelected" : "&eClick to inspect");
        return new ItemBuilder(XMaterial.CHEST)
            .name((selected ? "&a" : "&6") + (ChatColor.stripColor(crateName) == null ? crateName : ChatColor.stripColor(crateName)))
            .lore(lore)
            .build();
    }

    private ItemStack buildGlobalInfo(OfflinePlayer target, PlayerStats stats, String targetName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(target);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6" + targetName));
            List<String> lore = List.of(
                ChatColor.translateAlternateColorCodes('&', "&7Total Opens: &f" + stats.getTotalOpensAllTime()),
                ChatColor.translateAlternateColorCodes('&', "&7First Open: &f" + plugin.getStatsManager().formatDate(stats.getFirstOpenTimestamp())),
                ChatColor.translateAlternateColorCodes('&', "&7Rarest Win: &f" + plugin.getStatsManager().getOverallRarestReward(stats))
            );
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack buildSelectedInfo(PlayerStats stats, String crateId) {
        if (crateId == null || crateId.isBlank()) {
            return new ItemBuilder(XMaterial.BOOK)
                .name("&eSelected Crate")
                .lore("&7Click a crate in the top row to inspect it.")
                .build();
        }

        Optional<Crate> crateOpt = plugin.getConfigManager().getCrate(crateId);
        String crateName = crateOpt.map(Crate::getDisplayName).orElse(crateId);
        return new ItemBuilder(XMaterial.BOOK)
            .name("&eSelected: &f" + ChatColor.stripColor(crateName))
            .lore(
                "&7Opens: &f" + stats.getOpensPerCrate().getOrDefault(crateId, 0),
                "&7Rarest Win: &f" + stats.getRarestRewardPerCrate().getOrDefault(crateId, "None"),
                "&7Rarest Chance: &f" + plugin.getStatsManager().formatChance(stats.getRarestChancePerCrate().get(crateId)),
                "&7Current Pity: &f" + stats.getCurrentPityPerCrate().getOrDefault(crateId, 0),
                "&7Last Opened: &f" + plugin.getStatsManager().formatRelative(stats.getLastOpenedPerCrate().getOrDefault(crateId, 0L)),
                "&7Server Opens: &f" + plugin.getStatsManager().getServerOpens(crateId)
            )
            .build();
    }

    private void fill(Inventory inventory) {
        ItemStack filler = new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler.clone());
        }
    }

    private String resolveName(OfflinePlayer target, PlayerStats stats) {
        if (target.getName() != null && !target.getName().isBlank()) {
            return target.getName();
        }
        if (stats.getPlayerName() != null && !stats.getPlayerName().isBlank()) {
            return stats.getPlayerName();
        }
        return target.getUniqueId().toString();
    }

    private static class Session {
        private final UUID viewerUUID;
        private final UUID targetUUID;
        private final String targetName;
        private final Inventory inventory;
        private final List<String> crateIds = new ArrayList<>();
        private int page;
        private String selectedCrateId;

        private Session(UUID viewerUUID, UUID targetUUID, String targetName, Inventory inventory) {
            this.viewerUUID = viewerUUID;
            this.targetUUID = targetUUID;
            this.targetName = targetName;
            this.inventory = inventory;
        }

        public UUID viewerUUID() {
            return viewerUUID;
        }

        public UUID targetUUID() {
            return targetUUID;
        }

        public String targetName() {
            return targetName;
        }

        public Inventory inventory() {
            return inventory;
        }

        public List<String> crateIds() {
            return crateIds;
        }

        public int page() {
            return page;
        }

        public void setPage(int page) {
            this.page = Math.max(0, page);
        }

        public String selectedCrateId() {
            return selectedCrateId;
        }

        public void setSelectedCrateId(String selectedCrateId) {
            this.selectedCrateId = selectedCrateId == null ? null : selectedCrateId.toLowerCase(Locale.ROOT);
        }
    }
}
