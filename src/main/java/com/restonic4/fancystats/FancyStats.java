package com.restonic4.fancystats;

import com.restonic4.fancystats.core.StatsReportNetworking;
import com.restonic4.fancystats.other.FancyStatsCommands;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;

// TODO:
//  - Key system to save disk space on the jsons. Make some json on the stats folder containing the keys and its compressed values, then use the compressed values on the extra stats jsons, for example, instead of using "minecraft.custom:minecraft.total_world_time" we generate something like "abc"
//  - Add compatibility with other stats mods.
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
