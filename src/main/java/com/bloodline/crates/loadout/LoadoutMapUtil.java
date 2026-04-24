package com.bloodline.crates.loadout;

import com.bloodline.crates.animation.AnimationConfig;
import com.bloodline.crates.animation.AnimationType;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.CrateKey;
import com.bloodline.crates.model.CrateType;
import com.bloodline.crates.model.Reward;
import com.bloodline.crates.reward.RewardType;
import org.bukkit.configuration.MemorySection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LoadoutMapUtil {
    private LoadoutMapUtil() {
    }

    public static Map<String, Object> toMap(LoadoutData data) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("pluginVersion", data.pluginVersion());
        root.put("createdAt", data.createdAt());
        root.put("crates", data.crates().stream().map(LoadoutMapUtil::crateToMap).toList());
        root.put("keys", data.keys().stream().map(LoadoutMapUtil::keyToMap).toList());
        return root;
    }

    @SuppressWarnings("unchecked")
    public static LoadoutData fromMap(Map<String, Object> root) throws IOException {
        String pluginVersion = stringValue(root.get("pluginVersion"));
        long createdAt = longValue(root.get("createdAt"));

        List<Crate> crates = new ArrayList<>();
        Object cratesValue = root.get("crates");
        if (cratesValue instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> rawMap) {
                    crates.add(crateFromMap((Map<String, Object>) normalizeMap(rawMap)));
                }
            }
        }

        List<CrateKey> keys = new ArrayList<>();
        Object keysValue = root.get("keys");
        if (keysValue instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> rawMap) {
                    keys.add(keyFromMap((Map<String, Object>) normalizeMap(rawMap)));
                }
            }
        }

        return new LoadoutData(pluginVersion, createdAt, crates, keys);
    }

    private static Map<String, Object> crateToMap(Crate crate) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", crate.getId());
        map.put("displayName", crate.getDisplayName());
        map.put("description", crate.getDescription());
        map.put("type", crate.getType().name());
        map.put("keyId", crate.getKeyId());
        map.put("keyDisplayItem", encodeItem(crate.getKeyDisplayItem()));
        map.put("guiRows", crate.getGuiRows());
        map.put("rewardSlots", new ArrayList<>(crate.getRewardSlots() == null ? List.of() : crate.getRewardSlots()));
        map.put("pityEnabled", crate.isPityEnabled());
        map.put("pityThreshold", crate.getPityThreshold());
        map.put("jackpotRewardId", crate.getJackpotRewardId());
        map.put("keyMode", crate.getKeyMode().name());
        map.put("virtualOnly", crate.isVirtualOnly());
        map.put("globalLimitEnabled", crate.isGlobalLimitEnabled());
        map.put("globalLimitMax", crate.getGlobalLimitMax());
        map.put("globalLimitMessage", crate.getGlobalLimitMessage());
        map.put("globalLimitOnReached", crate.getGlobalLimitOnReached());
        map.put("perPlayerLimitEnabled", crate.isPerPlayerLimitEnabled());
        map.put("perPlayerLimitMax", crate.getPerPlayerLimitMax());
        map.put("perPlayerLimitMessage", crate.getPerPlayerLimitMessage());
        map.put("perPlayerResetInterval", crate.getPerPlayerResetInterval());
        map.put("animation", animationToMap(crate.getAnimationConfig()));
        map.put("rewards", crate.getRewards().stream().map(LoadoutMapUtil::rewardToMap).toList());
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Crate crateFromMap(Map<String, Object> map) throws IOException {
        List<Reward> rewards = new ArrayList<>();
        Object rewardsValue = map.get("rewards");
        if (rewardsValue instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> rawMap) {
                    rewards.add(rewardFromMap((Map<String, Object>) normalizeMap(rawMap)));
                }
            }
        }

        Crate crate = new Crate(
            stringValue(map.get("id")),
            stringValue(map.get("displayName")),
            enumValue(CrateType.class, stringValue(map.get("type")), CrateType.RANDOM),
            stringValue(map.get("keyId")),
            rewards,
            animationFromMap(normalizeMap(map.get("animation"))),
            booleanValue(map.get("pityEnabled")),
            intValue(map.get("pityThreshold")),
            stringValue(map.get("jackpotRewardId")),
            stringValue(map.get("description")),
            decodeItem(stringValue(map.get("keyDisplayItem"))),
            intValue(map.get("guiRows"), 6),
            integerList(map.get("rewardSlots"))
        );
        crate.setKeyMode(enumValue(com.bloodline.crates.model.KeyMode.class, stringValue(map.get("keyMode")), com.bloodline.crates.model.KeyMode.PHYSICAL));
        crate.setVirtualOnly(booleanValue(map.get("virtualOnly")));
        crate.setGlobalLimitEnabled(booleanValue(map.get("globalLimitEnabled")));
        crate.setGlobalLimitMax(intValue(map.get("globalLimitMax")));
        crate.setGlobalLimitMessage(stringValue(map.get("globalLimitMessage")));
        crate.setGlobalLimitOnReached(stringValue(map.get("globalLimitOnReached"), "DISABLE"));
        crate.setPerPlayerLimitEnabled(booleanValue(map.get("perPlayerLimitEnabled")));
        crate.setPerPlayerLimitMax(intValue(map.get("perPlayerLimitMax")));
        crate.setPerPlayerLimitMessage(stringValue(map.get("perPlayerLimitMessage")));
        crate.setPerPlayerResetInterval(stringValue(map.get("perPlayerResetInterval"), "NEVER"));
        return crate;
    }

    private static Map<String, Object> rewardToMap(Reward reward) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", reward.getId());
        map.put("rewardType", reward.getRewardType().name());
        map.put("item", encodeItem(reward.getItem()));
        map.put("displayItem", encodeItem(reward.getStoredDisplayItem()));
        map.put("chance", reward.getChance());
        map.put("requiredPermission", reward.getRequiredPermission());
        map.put("economyRewardEnabled", reward.isEconomyRewardEnabled());
        map.put("economyRewardAmount", reward.getEconomyAmount());
        map.put("command", reward.getCommand());
        map.put("broadcast", reward.isBroadcast());
        map.put("broadcastMessage", reward.getBroadcastMessage());
        map.put("crateId", reward.getCrateId());
        map.put("hidden", reward.isHidden());
        map.put("subRewards", reward.getSubRewards() == null ? List.of() : reward.getSubRewards().stream().map(LoadoutMapUtil::rewardToMap).toList());
        map.put("displayName", reward.getDisplayName());
        map.put("description", reward.getDescription());
        return map;
    }

    private static Reward rewardFromMap(Map<String, Object> map) throws IOException {
        Reward reward = new Reward(
            stringValue(map.get("id")),
            enumValue(RewardType.class, stringValue(map.get("rewardType")), RewardType.ITEM),
            decodeItem(stringValue(map.get("item"))),
            doubleValue(map.get("chance")),
            stringValue(map.get("requiredPermission")),
            booleanValue(map.get("economyRewardEnabled")),
            doubleValue(map.get("economyRewardAmount")),
            stringValue(map.get("command")),
            stringValue(map.get("displayName")),
            stringValue(map.get("description"))
        );
        reward.setDisplayItem(decodeItem(stringValue(map.get("displayItem"))));
        reward.setBroadcast(booleanValue(map.get("broadcast")));
        reward.setBroadcastMessage(stringValue(map.get("broadcastMessage")));
        reward.setCrateId(stringValue(map.get("crateId")));
        reward.setHidden(booleanValue(map.get("hidden")));
        Object subRewardsValue = map.get("subRewards");
        if (subRewardsValue instanceof List<?> list) {
            List<Reward> subRewards = new ArrayList<>();
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> rawMap) {
                    subRewards.add(rewardFromMap((Map<String, Object>) normalizeMap(rawMap)));
                }
            }
            reward.setSubRewards(subRewards);
        }
        return reward;
    }

    private static Map<String, Object> keyToMap(CrateKey crateKey) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("crateId", crateKey.getCrateId());
        map.put("displayItem", encodeItem(crateKey.getDisplayItem()));
        return map;
    }

    private static CrateKey keyFromMap(Map<String, Object> map) throws IOException {
        return new CrateKey(
            stringValue(map.get("crateId")),
            decodeItem(stringValue(map.get("displayItem")))
        );
    }

    private static Map<String, Object> animationToMap(AnimationConfig config) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (config == null) {
            return map;
        }
        map.put("type", config.getType().name());
        map.put("durationTicks", config.getDurationTicks());
        map.put("particleType", config.getParticleType());
        map.put("particleCount", config.getParticleCount());
        map.put("openSound", config.getOpenSound());
        map.put("revealSound", config.getRevealSound());
        map.put("skipPermission", config.getSkipPermission());
        return map;
    }

    private static AnimationConfig animationFromMap(Map<String, Object> map) {
        return new AnimationConfig(
            enumValue(AnimationType.class, stringValue(map.get("type")), AnimationType.INSTANT),
            intValue(map.get("durationTicks"), 60),
            stringValue(map.get("particleType"), "FLAME"),
            intValue(map.get("particleCount"), 30),
            stringValue(map.get("openSound"), "BLOCK_CHEST_OPEN"),
            stringValue(map.get("revealSound"), "ENTITY_PLAYER_LEVELUP"),
            stringValue(map.get("skipPermission"), "bloodcrates.skip")
        );
    }

    private static String encodeItem(ItemStack item) {
        if (item == null) {
            return null;
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
                dataOutput.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode item stack.", exception);
        }
    }

    private static ItemStack decodeItem(String encoded) throws IOException {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        byte[] bytes = Base64.getDecoder().decode(encoded);
        try (BukkitObjectInputStream inputStream = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object object = inputStream.readObject();
            return object instanceof ItemStack itemStack ? itemStack : null;
        } catch (ClassNotFoundException exception) {
            throw new IOException("Failed to decode item stack.", exception);
        }
    }

    private static Map<String, Object> normalizeMap(Object value) {
        if (value instanceof MemorySection section) {
            return section.getValues(true);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                normalized.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return normalized;
        }
        return new LinkedHashMap<>();
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static List<Integer> integerList(Object value) {
        List<Integer> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object entry : list) {
                result.add(intValue(entry));
            }
        }
        return result;
    }

    private static String stringValue(Object value) {
        return stringValue(value, "");
    }

    private static String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private static boolean booleanValue(Object value) {
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private static int intValue(Object value) {
        return intValue(value, 0);
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? 0L : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? 0.0D : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }
}
