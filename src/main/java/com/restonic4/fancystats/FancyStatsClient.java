package com.restonic4.fancystats;

import com.restonic4.fancystats.client.FancyStatsScreenAccess;
import com.restonic4.fancystats.core.StatsReport;
import com.restonic4.fancystats.core.StatsReportNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public class FancyStatsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        StatsReportNetworking.registerClient();
    }

    public static void handleReport(Minecraft minecraft, StatsReport report) {
        if (minecraft.screen instanceof FancyStatsScreenAccess access) {
            access.fancystats$receiveReport(report);
        }
    }
}
