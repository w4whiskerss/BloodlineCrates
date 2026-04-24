package com.bloodline.crates.sync;

import java.util.UUID;
import java.util.function.Consumer;

public interface SyncManager {
    void publishKeyGrant(UUID playerUUID, String crateId, int amount);

    void publishRewardGrant(UUID playerUUID, String crateId, String rewardSummary);

    void subscribeToUpdates(Consumer<SyncPayload> handler);

    void shutdown();
}
