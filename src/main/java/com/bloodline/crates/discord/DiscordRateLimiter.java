package com.bloodline.crates.discord;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DiscordRateLimiter {
    private final int maxCommandsPerMinute;
    private final Map<String, Deque<Long>> usages = new HashMap<>();

    public DiscordRateLimiter(int maxCommandsPerMinute) {
        this.maxCommandsPerMinute = Math.max(1, maxCommandsPerMinute);
    }

    public boolean isRateLimited(String discordUserId) {
        Deque<Long> deque = usages.computeIfAbsent(discordUserId, ignored -> new ArrayDeque<>());
        evictExpired(deque);
        return deque.size() >= maxCommandsPerMinute;
    }

    public long getRemainingCooldownSeconds(String discordUserId) {
        Deque<Long> deque = usages.get(discordUserId);
        if (deque == null || deque.isEmpty()) {
            return 0L;
        }
        evictExpired(deque);
        if (deque.size() < maxCommandsPerMinute) {
            return 0L;
        }
        long oldest = deque.peekFirst() == null ? 0L : deque.peekFirst();
        return Math.max(1L, (60_000L - (System.currentTimeMillis() - oldest) + 999L) / 1000L);
    }

    public void recordUsage(String discordUserId) {
        Deque<Long> deque = usages.computeIfAbsent(discordUserId, ignored -> new ArrayDeque<>());
        evictExpired(deque);
        deque.addLast(System.currentTimeMillis());
    }

    private void evictExpired(Deque<Long> deque) {
        long cutoff = System.currentTimeMillis() - 60_000L;
        Iterator<Long> iterator = deque.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() < cutoff) {
                iterator.remove();
            }
        }
    }
}
