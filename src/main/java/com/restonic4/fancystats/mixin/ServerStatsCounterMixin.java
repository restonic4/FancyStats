package com.restonic4.fancystats.mixin;

import com.restonic4.fancystats.core.ExtraStats;
import com.restonic4.fancystats.core.ExtraStatsAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.ServerStatsCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.nio.file.Path;

@Mixin(ServerStatsCounter.class)
public class ServerStatsCounterMixin implements ExtraStatsAccess {
    @Shadow @Final private File file;

    @Unique private ExtraStats fancystats$extraStats;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void fancystats$init(MinecraftServer server, File file, CallbackInfo ci) {
        Path extraFile = file.toPath().resolveSibling(
                file.getName().endsWith(".json")
                        ? file.getName().substring(0, file.getName().length() - 5) + "_extra.jsonl"
                        : file.getName() + "_extra.jsonl"
        );

        this.fancystats$extraStats = ExtraStats.load(extraFile);
    }

    @Inject(method = "save", at = @At("TAIL"))
    private void fancystats$save(CallbackInfo ci) {
        if (this.fancystats$extraStats != null) {
            this.fancystats$extraStats.save();
        }
    }

    @Override
    public ExtraStats fancystats$extraStats() {
        return this.fancystats$extraStats;
    }
}
