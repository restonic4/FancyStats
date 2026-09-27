package com.restonic4.fancystats.other;

import com.mojang.brigadier.CommandDispatcher;
import com.restonic4.fancystats.core.ExtraStatsAccess;
import com.restonic4.fancystats.core.StatsReport;
import com.restonic4.fancystats.core.StatsReportNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class FancyStatsCommands {
    private static final long HOUR_MILLIS = 60L * 60L * 1000L;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fancystats").then(
                Commands.literal("report").then(
                        Commands.literal("last_hour").executes(context ->
                                executeLastHour(context.getSource())
                        )
                )
        ));
    }

    private static int executeLastHour(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        if (!(player.getStats() instanceof ExtraStatsAccess access)) {
            source.sendFailure(Component.literal("Extra statistics are not available."));
            return 0;
        }

        long toMillis = System.currentTimeMillis();
        long fromMillis = toMillis - HOUR_MILLIS;

        StatsReport report = access.fancystats$extraStats().createReport(fromMillis, toMillis);
        StatsReportNetworking.sendReport(player, report);

        source.sendSuccess(Component.literal("Generated statistics report for the last hour."), false);

        return 1;
    }
}
