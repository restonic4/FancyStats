package com.restonic4.fancystats;

import com.restonic4.fancystats.core.StatsReport;
import com.restonic4.fancystats.core.StatsReportNetworking;
import net.fabricmc.api.ClientModInitializer;

public class FancyStatsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        StatsReportNetworking.registerClient();
    }

    public static void handleReport(StatsReport report) {
        System.out.println("========== FancyStats Report ==========");
        System.out.println("From: " + report.fromMillis());
        System.out.println("To: " + report.toMillis());
        System.out.println("Stats:");
        report.values().forEach((stat, value) -> System.out.println("  " + stat + " = " + value));
        System.out.println("=======================================");
    }
}
