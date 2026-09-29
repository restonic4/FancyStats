package com.restonic4.fancystats.mixin;

import com.restonic4.fancystats.core.ExtraStatsAccess;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StatsCounter.class)
public class StatsCounterMixin {
    @Inject(method = "increment", at = @At("TAIL"))
    private void fancystats$onIncrement(Player player, Stat<?> stat, int amount, CallbackInfo ci) {
        if (amount == 0) return;
        if (!((Object) this instanceof ExtraStatsAccess)) return;
        ExtraStatsAccess access = (ExtraStatsAccess) this;
        access.fancystats$extraStats().record(stat, amount, System.currentTimeMillis());
    }
}
