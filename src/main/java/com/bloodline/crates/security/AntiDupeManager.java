package com.bloodline.crates.security;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.debug.DebugEventType;
import com.bloodline.crates.manager.PlayerDataManager;
import com.bloodline.crates.model.Reward;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AntiDupeManager {
    private final BloodlineCrates plugin;

    public AntiDupeManager(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    public String beginTransaction(UUID playerId, Reward reward) {
        String transactionId = UUID.randomUUID().toString();
        plugin.getPlayerDataManager().createPendingTransaction(playerId, transactionId, reward);
        plugin.getDebugManager().emit(DebugEventType.TRANSACTION_CREATE, playerId,
            () -> "transaction=" + transactionId + ", reward=" + (reward == null ? "unknown" : reward.getId()));
        return transactionId;
    }

    public void completeTransaction(UUID playerId, String transactionId) {
        plugin.getPlayerDataManager().completePendingTransaction(playerId, transactionId);
        plugin.getDebugManager().emit(DebugEventType.TRANSACTION_COMPLETE, playerId,
            () -> "transaction=" + transactionId);
    }

    public boolean isPendingTransaction(UUID playerId) {
        return plugin.getPlayerDataManager().hasPendingTransaction(playerId);
    }

    public void recoverPendingTransactions() {
        Map<UUID, List<PlayerDataManager.PendingTransaction>> allTransactions = plugin.getPlayerDataManager().getAllPendingTransactions();
        for (Map.Entry<UUID, List<PlayerDataManager.PendingTransaction>> entry : allTransactions.entrySet()) {
            UUID playerId = entry.getKey();
            for (PlayerDataManager.PendingTransaction transaction : entry.getValue()) {
                plugin.getClaimsManager().addClaim(playerId, transaction.reward());
                completeTransaction(playerId, transaction.transactionId());
            }
        }
    }
}
