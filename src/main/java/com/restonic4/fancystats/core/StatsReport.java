package com.restonic4.fancystats.core;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public record StatsReport(long fromMillis, long toMillis, Map<String, Long> values) {
    public StatsReport(long fromMillis, long toMillis, Map<String, Long> values) {
        this.fromMillis = fromMillis;
        this.toMillis = toMillis;
        this.values = Collections.unmodifiableMap(new TreeMap<>(values));
    }

    public long get(String statId) {
        return values.getOrDefault(statId, 0L);
    }

    @Override
    public @NotNull String toString() {
        return "StatsReport{" + "fromMillis=" + fromMillis + ", toMillis=" + toMillis + ", values=" + values + '}';
    }
}