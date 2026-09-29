package com.restonic4.fancystats;

import com.restonic4.fancystats.core.StatsReportNetworking;
import net.fabricmc.api.ModInitializer;

public class FancyStats implements ModInitializer {
    public static final String MOD_ID = "fancystats";

    @Override
    public void onInitialize() {
        StatsReportNetworking.registerServer();
    }
}
