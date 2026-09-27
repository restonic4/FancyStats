package com.restonic4.fancystats.core;

import com.google.gson.*;
import net.minecraft.stats.Stat;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class ExtraStats {
    public static final int FILE_VERSION = 1;
    public static final long BUCKET_SIZE_MILLIS = 60000L;

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final Path file;

    private final NavigableMap<Long, Map<String, Long>> history = new TreeMap<>();
    private final NavigableMap<Long, Map<String, Long>> pending = new TreeMap<>();

    private long nextSequence = 1L;

    private ExtraStats(Path file) {
        this.file = file;
    }

    public static ExtraStats load(Path file) {
        ExtraStats stats = new ExtraStats(file);
        if (!Files.exists(file)) return stats;

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try { stats.readRecord(JsonParser.parseString(line));} catch (Exception ignored) {}
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load extra stats: " + file, e);
        }

        return stats;
    }

    public void record(Stat<?> stat, long amount, long timestampMillis) {
        if (amount == 0L) return;

        String statId = stat.getName();
        record(statId, amount, timestampMillis);
    }

    public void record(String statId, long amount, long timestampMillis) {
        if (amount == 0L || statId == null || statId.isEmpty()) return;

        long bucket = bucketStart(timestampMillis);

        merge(history, bucket, statId, amount);
        merge(pending, bucket, statId, amount);
    }

    public long sum(String statId, long fromMillis, long toMillis) {
        if (statId == null || statId.isEmpty() || toMillis < fromMillis) return 0L;

        long fromBucket = bucketStart(fromMillis);
        long toBucket = bucketStart(toMillis);

        long result = 0L;

        for (Map.Entry<Long, Map<String, Long>> entry : history.subMap(fromBucket, true, toBucket, true).entrySet()) {
            result += entry.getValue().getOrDefault(statId, 0L);
        }

        return result;
    }

    public long sum(Stat<?> stat, long fromMillis, long toMillis) {
        return sum(stat.getName(), fromMillis, toMillis);
    }

    public void save() {
        if (pending.isEmpty()) return;

        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);

            JsonObject root = new JsonObject();
            root.addProperty("v", FILE_VERSION);
            root.addProperty("s", nextSequence++);

            JsonObject buckets = new JsonObject();
            for (Map.Entry<Long, Map<String, Long>> entry : pending.entrySet()) {
                JsonObject values = new JsonObject();

                for (Map.Entry<String, Long> value : entry.getValue().entrySet()) {
                    values.addProperty(value.getKey(), value.getValue());
                }

                buckets.add(Long.toString(entry.getKey()), values);
            }

            root.add("b", buckets);

            String line = GSON.toJson(root) + System.lineSeparator();

            Files.writeString(
                    file,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );

            pending.clear();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save extra stats: " + file, e);
        }
    }

    public boolean hasPendingChanges() {
        return !pending.isEmpty();
    }

    public Path getFile() {
        return file;
    }

    private void readRecord(JsonElement element) {
        if (!element.isJsonObject()) return;

        JsonObject root = element.getAsJsonObject();

        int version = root.has("v") ? root.get("v").getAsInt() : 0;
        if (version != FILE_VERSION) return;

        if (root.has("s")) {
            nextSequence = Math.max(nextSequence, root.get("s").getAsLong() + 1L);
        }

        JsonObject buckets = root.getAsJsonObject("b");
        if (buckets == null) return;

        for (Map.Entry<String, JsonElement> bucketEntry : buckets.entrySet()) {
            long bucket;

            try {
                bucket = Long.parseLong(bucketEntry.getKey());
            } catch (NumberFormatException ignored) {
                continue;
            }

            if (!bucketEntry.getValue().isJsonObject()) continue;

            JsonObject values = bucketEntry.getValue().getAsJsonObject();

            for (Map.Entry<String, JsonElement> valueEntry : values.entrySet()) {
                JsonElement value = valueEntry.getValue();
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) continue;

                merge(history, bucket, valueEntry.getKey(), value.getAsLong());
            }
        }
    }

    private static void merge(
            NavigableMap<Long, Map<String, Long>> target,
            long bucket, String statId, long amount
    ) {
        target.computeIfAbsent(bucket, ignored -> new HashMap<>()).merge(statId, amount, Long::sum);
    }

    private static long bucketStart(long timestampMillis) {
        return Math.floorDiv(timestampMillis, BUCKET_SIZE_MILLIS) * BUCKET_SIZE_MILLIS;
    }

    public StatsReport createReport(long fromMillis, long toMillis) {
        if (fromMillis >= toMillis) throw new IllegalArgumentException("fromMillis must be smaller than toMillis");

        long fromBucket = bucketStart(fromMillis);
        long toBucket = Math.floorDiv(toMillis - 1L, BUCKET_SIZE_MILLIS) * BUCKET_SIZE_MILLIS;

        Map<String, Long> totals = new HashMap<>();
        for (Map.Entry<Long, Map<String, Long>> entry : history.subMap(fromBucket, true, toBucket, true).entrySet()) {
            for (Map.Entry<String, Long> stat : entry.getValue().entrySet()) {
                totals.merge(stat.getKey(), stat.getValue(), Long::sum);
            }
        }

        return new StatsReport(fromMillis, toMillis, totals);
    }
}
