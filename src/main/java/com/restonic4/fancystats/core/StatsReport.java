package com.restonic4.fancystats.core;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class StatsReport {
    private final long fromMillis;
    private final long toMillis;
    private final Map<String, Long> values;

    public StatsReport(long fromMillis, long toMillis, Map<String, Long> values) {
        this.fromMillis = fromMillis;
        this.toMillis = toMillis;
        this.values = Collections.unmodifiableMap(new TreeMap<>(values));
    }

    public long fromMillis() {
        return fromMillis;
    }

    public long toMillis() {
        return toMillis;
    }

    public Map<String, Long> values() {
        return values;
    }

    public long get(String statId) {
        return values.getOrDefault(statId, 0L);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatsReport that = (StatsReport) o;
        return fromMillis == that.fromMillis &&
                toMillis == that.toMillis &&
                Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromMillis, toMillis, values);
    }

    @Override
    public @NotNull String toString() {
        return "StatsReport{" + "fromMillis=" + fromMillis + ", toMillis=" + toMillis + ", values=" + values + '}';
    }
}