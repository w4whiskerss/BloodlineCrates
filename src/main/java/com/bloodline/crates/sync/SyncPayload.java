package com.bloodline.crates.sync;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SyncPayload {
    private String serverId;
    private String eventType;
    private String payload;
    private long createdAt;

    public String toJson() {
        return "{\"serverId\":\"" + escape(serverId) + "\","
            + "\"eventType\":\"" + escape(eventType) + "\","
            + "\"payload\":\"" + escape(payload) + "\","
            + "\"createdAt\":" + createdAt + "}";
    }

    public static SyncPayload fromJson(String json) {
        return new SyncPayload(
            extract(json, "serverId"),
            extract(json, "eventType"),
            extract(json, "payload"),
            parseLong(extractRaw(json, "createdAt"))
        );
    }

    private static String extract(String json, String key) {
        String raw = extractRaw(json, key);
        if (raw == null) {
            return "";
        }
        return raw.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String extractRaw(String json, String key) {
        String quotedPrefix = "\"" + key + "\":\"";
        int quotedStart = json.indexOf(quotedPrefix);
        if (quotedStart >= 0) {
            int valueStart = quotedStart + quotedPrefix.length();
            StringBuilder builder = new StringBuilder();
            boolean escaped = false;
            for (int i = valueStart; i < json.length(); i++) {
                char current = json.charAt(i);
                if (escaped) {
                    builder.append(current);
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                    builder.append(current);
                } else if (current == '"') {
                    return builder.toString();
                } else {
                    builder.append(current);
                }
            }
        }

        String rawPrefix = "\"" + key + "\":";
        int rawStart = json.indexOf(rawPrefix);
        if (rawStart < 0) {
            return null;
        }
        int valueStart = rawStart + rawPrefix.length();
        int valueEnd = json.indexOf(',', valueStart);
        if (valueEnd < 0) {
            valueEnd = json.indexOf('}', valueStart);
        }
        if (valueEnd < 0) {
            return null;
        }
        return json.substring(valueStart, valueEnd).trim();
    }

    private static long parseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String escape(String input) {
        return input == null ? "" : input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
