package com.restonic4.fancystats.core;

import com.google.gson.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StatsKeyRegistry {
    public static final int FILE_VERSION = 1;
    public static final String FILE_NAME = "fancystats_keys.json";

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final char[] ALPHABET = buildAlphabet();

    private static final Map<Path, StatsKeyRegistry> INSTANCES = new ConcurrentHashMap<>();

    private final Path file;
    private final Map<String, String> statToKey = new HashMap<>();
    private final Map<String, String> keyToStat = new HashMap<>();
    private long nextIndex = 0L;

    private StatsKeyRegistry(Path file) {
        this.file = file;
        load();
    }

    public static StatsKeyRegistry get(Path statsFolder) {
        Path normalized = statsFolder.toAbsolutePath().normalize();
        return INSTANCES.computeIfAbsent(normalized, p -> new StatsKeyRegistry(p.resolve(FILE_NAME)));
    }

    public synchronized String encode(String statId) {
        String existing = statToKey.get(statId);
        if (existing != null) return existing;

        String key;
        do {key = indexToKey(nextIndex++);
        } while (keyToStat.containsKey(key));

        statToKey.put(statId, key);
        keyToStat.put(key, statId);
        save();

        return key;
    }

    public synchronized String decode(String key) {
        String statId = keyToStat.get(key);
        return statId != null ? statId : key;
    }

    private static String indexToKey(long index) {
        long n = index + 1;
        StringBuilder sb = new StringBuilder();

        while (n > 0) {
            n--;
            int rem = (int) (n % 62);
            sb.append(ALPHABET[rem]);
            n /= 62;
        }

        return sb.reverse().toString();
    }

    private static char[] buildAlphabet() {
        StringBuilder sb = new StringBuilder();
        for (char c = 'a'; c <= 'z'; c++) sb.append(c);
        for (char c = 'A'; c <= 'Z'; c++) sb.append(c);
        for (char c = '0'; c <= '9'; c++) sb.append(c);
        return sb.toString().toCharArray();
    }

    private void load() {
        if (!Files.exists(file)) return;

        try {
            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) return;

            JsonObject root = JsonParser.parseString(content).getAsJsonObject();

            int version = root.has("v") ? root.get("v").getAsInt() : 0;
            if (version != FILE_VERSION) return;

            if (root.has("n")) {
                nextIndex = Math.max(nextIndex, root.get("n").getAsLong());
            }

            JsonObject keys = root.getAsJsonObject("k");
            if (keys == null) return;

            for (Map.Entry<String, JsonElement> entry : keys.entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) continue;

                String statId = entry.getKey();
                String key = entry.getValue().getAsString();

                statToKey.put(statId, key);
                keyToStat.put(key, statId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load stats key registry: " + file, e);
        }
    }

    private void save() {
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);

            JsonObject root = new JsonObject();
            root.addProperty("v", FILE_VERSION);
            root.addProperty("n", nextIndex);

            JsonObject keys = new JsonObject();
            for (Map.Entry<String, String> entry : statToKey.entrySet()) {
                keys.addProperty(entry.getKey(), entry.getValue());
            }
            root.add("k", keys);

            String json = GSON.toJson(root);

            Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
            Files.write(
                    tmp, json.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
            );
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save stats key registry: " + file, e);
        }
    }
}