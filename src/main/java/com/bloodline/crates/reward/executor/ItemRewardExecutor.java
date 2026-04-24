package com.bloodline.crates.reward.executor;

import com.bloodline.crates.model.Reward;
import com.bloodline.crates.reward.RewardContext;
import com.bloodline.crates.reward.RewardExecutor;
import com.bloodline.crates.reward.RewardType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemRewardExecutor implements RewardExecutor {
    @Override
    public void execute(RewardContext context) {
        Player player = context.getPlayer();
        Reward reward = context.getReward();
        ItemStack item = reward.getItem();
        if (item == null) {
            return;
        }

        if (!canFit(player, item)) {
            context.getPlugin().getClaimsManager().addClaim(player.getUniqueId(), reward);
            player.sendMessage(context.getPlugin().getConfigManager().getPrefix()
                + context.getPlugin().getConfigManager().getMessage("reward-claimed").replace("{reward}", reward.getDisplayNameOrFallback()));
            return;
        }

        player.getInventory().addItem(item.clone());
        player.sendMessage(context.getPlugin().getConfigManager().getPrefix()
            + context.getPlugin().getConfigManager().getMessage("reward-granted").replace("{reward}", reward.getDisplayNameOrFallback()));
    }

    @Override
    public RewardType getType() {
        return RewardType.ITEM;
    }

    @Override
    public boolean validate(Reward reward) {
        return reward.getItem() != null;
    }

    private boolean canFit(Player player, ItemStack item) {
        int remaining = item.getAmount();
        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                return true;
            }
            if (content.isSimilar(item)) {
                remaining -= Math.max(0, content.getMaxStackSize() - content.getAmount());
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
