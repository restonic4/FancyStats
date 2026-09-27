package com.restonic4.fancystats.core;

import com.restonic4.fancystats.FancyStats;
import com.restonic4.fancystats.FancyStatsClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public class StatsReportNetworking {
    public static final ResourceLocation REQUEST_REPORT = FancyStats.id("request_report");
    public static final ResourceLocation REPORT = FancyStats.id("report");

    private static final int MAX_STATS = 4096;

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(
                REQUEST_REPORT,
                (server, player, handler, buf, responseSender) -> {
                    long fromMillis = buf.readLong();
                    long toMillis = buf.readLong();
                    server.execute(() -> handleReportRequest(player, fromMillis, toMillis));
                }
        );
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                REPORT,
                (client, handler, buf, responseSender) -> {
                    StatsReport report = readReport(buf);
                    client.execute(() -> FancyStatsClient.handleReport(client, report));
                }
        );
    }

    private static void handleReportRequest(ServerPlayer player, long fromMillis, long toMillis) {
        if (fromMillis >= toMillis) return;

        long maxRange = 1000L * 60L * 60L * 24L * 365L * 10L;
        if (toMillis - fromMillis > maxRange) return;

        if (!(player.getStats() instanceof ExtraStatsAccess access)) return;

        StatsReport report = access.fancystats$extraStats().createReport(fromMillis, toMillis);
        sendReport(player, report);
    }

    private static void writeReport(FriendlyByteBuf buf, StatsReport report) {
        buf.writeLong(report.fromMillis());
        buf.writeLong(report.toMillis());

        if (report.values().size() > MAX_STATS) throw new IllegalStateException("Stats report contains too many stats");
        buf.writeVarInt(report.values().size());

        for (var entry : report.values().entrySet()) {
            buf.writeUtf(entry.getKey(), 256);
            buf.writeLong(entry.getValue());
        }
    }

    private static StatsReport readReport(FriendlyByteBuf buf) {
        long fromMillis = buf.readLong();
        long toMillis = buf.readLong();

        int count = buf.readVarInt();

        if (count < 0 || count > MAX_STATS) throw new IllegalArgumentException("Invalid stats report size: " + count);

        Map<String, Long> values = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            String statId = buf.readUtf(256);

            long value = buf.readLong();
            values.put(statId, value);
        }

        return new StatsReport(fromMillis, toMillis, values);
    }

    public static void sendReport(ServerPlayer player, StatsReport report) {
        if (!ServerPlayNetworking.canSend(player, REPORT)) return;

        FriendlyByteBuf buf = PacketByteBufs.create();
        writeReport(buf, report);

        ServerPlayNetworking.send(player, REPORT, buf);
    }

    public static boolean canRequestReport() {
        return ClientPlayNetworking.canSend(REQUEST_REPORT);
    }

    public static void requestReport(long fromMillis, long toMillis) {
        FriendlyByteBuf buf = PacketByteBufs.create();

        buf.writeLong(fromMillis);
        buf.writeLong(toMillis);

        ClientPlayNetworking.send(REQUEST_REPORT, buf);
    }
}
