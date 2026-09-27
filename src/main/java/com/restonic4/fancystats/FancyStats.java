package com.restonic4.fancystats;

import com.restonic4.fancystats.core.StatsReportNetworking;
import com.restonic4.fancystats.other.FancyStatsCommands;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;

public class FancyStats implements ModInitializer {
    public static final String MOD_ID = "fancystats";

    @Override
    public void onInitialize() {
        StatsReportNetworking.registerServer();
        FancyStatsCommands.register();
    }

    public static ResourceLocation id(String id) {
        return new ResourceLocation(MOD_ID, id);
    }
}
