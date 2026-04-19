package com.bloodlinecrates.model;

import java.util.Map;

public record TimedKeyRule(int intervalMinutes, int amount, Map<String, Double> permissionMultiplier, boolean dailyLogin) {
}
