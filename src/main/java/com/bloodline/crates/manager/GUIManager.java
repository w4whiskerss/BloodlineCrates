package com.bloodline.crates.manager;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.gui.ConfirmGUI;
import com.bloodline.crates.gui.PreviewGUI;
import com.bloodline.crates.gui.SelectableGUI;
import com.bloodline.crates.gui.VirtualOpenConfirmGUI;
import com.bloodline.crates.gui.editor.*;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.Reward;
import com.bloodline.crates.util.InputHandler;
import com.bloodline.crates.virtual.VirtualInventoryGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIManager {
    private final BloodlineCrates plugin;
    private final Map<Inventory, GUIHandler> activeGUIs = new HashMap<>();
    private final Map<UUID, GUIType> playerGUITypes = new HashMap<>();
    private final InputHandler inputHandler;

    public GUIManager(BloodlineCrates plugin) {
        this.plugin = plugin;
        this.inputHandler = new InputHandler(plugin);
    }

    public InputHandler getInputHandler() {
        return inputHandler;
    }

    public void openPreview(Player player, Crate crate) {
        openRegistered(player, new PreviewGUI(plugin, crate), GUIType.PREVIEW);
    }

    public void openSelectable(Player player, Crate crate, org.bukkit.Location location) {
        openRegistered(player, new SelectableGUI(plugin, crate, location), GUIType.SELECTABLE);
    }

    public void openSelectable(Player player, Crate crate, org.bukkit.Location location, boolean consumeVirtualCrate) {
        openRegistered(player, new SelectableGUI(plugin, crate, location, consumeVirtualCrate), GUIType.SELECTABLE);
    }

    public void openConfirm(Player player, Crate crate, Reward reward, org.bukkit.Location location) {
        openRegistered(player, new ConfirmGUI(plugin, crate, reward, location), GUIType.CONFIRM);
    }

    public void openConfirm(Player player, Crate crate, Reward reward, org.bukkit.Location location, boolean consumeVirtualCrate) {
        openRegistered(player, new ConfirmGUI(plugin, crate, reward, location, consumeVirtualCrate), GUIType.CONFIRM);
    }

    public void openVirtualOpenConfirm(Player player, Crate crate) {
        openRegistered(player, new VirtualOpenConfirmGUI(plugin, crate), GUIType.VIRTUAL_OPEN_CONFIRM);
    }

    public void openVirtualInventory(Player viewer, Player owner, boolean readOnly) {
        openRegistered(viewer, new VirtualInventoryGUI(plugin, viewer, owner, readOnly), GUIType.VIRTUAL_INVENTORY);
    }

    public void openEditorMain(Player player) {
        openRegistered(player, new EditorMainGUI(plugin), GUIType.EDITOR_MAIN);
    }

    public void openCrateManager(Player player) {
        openRegistered(player, new CrateManagerGUI(plugin), GUIType.CRATE_MANAGER);
    }

    public void openKeyBrowser(Player player) {
        openRegistered(player, new KeyBrowserGUI(plugin), GUIType.KEY_BROWSER);
    }

    public void openCrateEdit(Player player, Crate crate) {
        openRegistered(player, new CrateEditGUI(plugin, crate), GUIType.CRATE_EDIT);
    }

    public void openCrateAdjustments(Player player, Crate crate) {
        openRegistered(player, new CrateAdjustmentsGUI(plugin, crate), GUIType.CRATE_ADJUSTMENTS);
    }

    public void openCrateLayoutEditor(Player player, Crate crate) {
        openRegistered(player, new CrateLayoutEditorGUI(plugin, crate), GUIType.CRATE_LAYOUT_EDITOR);
    }

    public void openRewardEdit(Player player, Crate crate) {
        openRegistered(player, new RewardEditGUI(plugin, crate), GUIType.REWARD_EDIT);
    }

    public void openRewardTypeSelector(Player player, Crate crate, ItemStack item) {
        openRegistered(player, new RewardTypeSelectGUI(plugin, crate, item), GUIType.REWARD_TYPE_SELECT);
    }

    public void openRewardSettings(Player player, Crate crate, Reward reward, int index) {
        openRegistered(player, new RewardSettingsGUI(plugin, crate, reward, index), GUIType.REWARD_SETTINGS);
    }

    public void openKeyEdit(Player player, Crate crate) {
        openKeyEdit(player, crate, KeyEditReturn.KEY_BROWSER);
    }

    public void openKeyEdit(Player player, Crate crate, KeyEditReturn returnTarget) {
        openRegistered(player, new KeyEditGUI(plugin, crate, returnTarget), GUIType.KEY_EDIT);
    }

    public void openConfirmDelete(Player player, Crate crate, GUIHandler returnGUI) {
        openRegistered(player, new ConfirmDeleteGUI(plugin, crate, returnGUI), GUIType.CONFIRM_DELETE);
    }

    public void openConfirmDelete(Player player, Crate crate, Reward reward, int rewardIndex, GUIHandler returnGUI) {
        openRegistered(player, new ConfirmDeleteGUI(plugin, crate, reward, rewardIndex, returnGUI), GUIType.CONFIRM_DELETE);
    }

    public void reopenExistingGUI(Player player, GUIHandler handler, GUIType type) {
        activeGUIs.put(handler.getInventory(), handler);
        playerGUITypes.put(player.getUniqueId(), type);
        player.openInventory(handler.getInventory());
    }

    private void openRegistered(Player player, GUIHandler gui, GUIType type) {
        activeGUIs.put(gui.getInventory(), gui);
        playerGUITypes.put(player.getUniqueId(), type);
        player.openInventory(gui.getInventory());
    }

    public void handleClick(InventoryClickEvent event) {
        if (inputHandler.handleAnvilClick(event)) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        GUIHandler handler = activeGUIs.get(topInventory);
        if (handler == null) {
            return;
        }

        if (event.getRawSlot() < topInventory.getSize()) {
            handler.onClick(event);
            return;
        }

        InventoryAction action = event.getAction();
        if (event.isShiftClick()
            || action == InventoryAction.MOVE_TO_OTHER_INVENTORY
            || action == InventoryAction.COLLECT_TO_CURSOR
            || action == InventoryAction.HOTBAR_MOVE_AND_READD
            || action == InventoryAction.HOTBAR_SWAP) {
            event.setCancelled(true);
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        GUIHandler handler = activeGUIs.get(topInventory);
        if (handler != null && event.getRawSlots().stream().anyMatch(slot -> slot < topInventory.getSize())) {
            event.setCancelled(true);
        }
    }

    public void handleOpen(InventoryOpenEvent event) {
        GUIHandler handler = activeGUIs.get(event.getInventory());
        if (handler != null) {
            handler.onOpen(event);
        }
    }

    public void handleClose(InventoryCloseEvent event) {
        inputHandler.handleAnvilClose(event);
        GUIHandler handler = activeGUIs.get(event.getInventory());
        if (handler != null) {
            handler.onClose(event);
            activeGUIs.remove(event.getInventory());
            playerGUITypes.remove(event.getPlayer().getUniqueId());
        }
    }

    public boolean isInGUI(UUID playerId) {
        return playerGUITypes.containsKey(playerId) || inputHandler.hasPendingAnvilInput(playerId);
    }

    public void handleAnvilPrepare(org.bukkit.event.inventory.PrepareAnvilEvent event) {
        inputHandler.handleAnvilPrepare(event);
    }

    public GUIType getGUIType(UUID playerId) {
        return playerGUITypes.get(playerId);
    }

    public enum GUIType {
        PREVIEW,
        SELECTABLE,
        CONFIRM,
        EDITOR_MAIN,
        CRATE_MANAGER,
        KEY_BROWSER,
        CRATE_EDIT,
        CRATE_ADJUSTMENTS,
        CRATE_LAYOUT_EDITOR,
        REWARD_EDIT,
        REWARD_TYPE_SELECT,
        REWARD_SETTINGS,
        KEY_EDIT,
        CONFIRM_DELETE,
        VIRTUAL_INVENTORY,
        VIRTUAL_OPEN_CONFIRM
    }

    public enum KeyEditReturn {
        KEY_BROWSER,
        CRATE_EDIT
    }

    public interface GUIHandler {
        void onClick(InventoryClickEvent event);

        void onOpen(InventoryOpenEvent event);

        void onClose(InventoryCloseEvent event);

        Inventory getInventory();
    }
}
