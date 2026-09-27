package com.restonic4.fancystats.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.restonic4.fancystats.client.FancyStatsList;
import com.restonic4.fancystats.client.FancyStatsScreenAccess;
import com.restonic4.fancystats.core.StatsReport;
import com.restonic4.fancystats.core.StatsReportNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Mixin(StatsScreen.class)
public abstract class StatsScreenMixin extends Screen implements FancyStatsScreenAccess {
    @Unique private static final long FANCYSTATS_HOUR = 60L * 60L * 1000L;
    @Unique private static final DateTimeFormatter FANCYSTATS_DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

    @Unique private boolean fancystats$loading;

    @Unique private Button fancystats$fancyTab;
    @Unique private EditBox fancystats$startField;
    @Unique private EditBox fancystats$endField;
    @Unique private Button fancystats$applyButton;
    @Unique private FancyStatsList fancystats$list;
    @Unique private Component fancystats$error;

    protected StatsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "initButtons", at = @At("TAIL"))
    private void fancystats$addFancyTab(CallbackInfo ci) {
        int buttonWidth = 80;
        int buttonHeight = 20;
        int x = this.width / 2 + 80;
        int y = this.height - 52;

        int index = 0;
        for (var child : this.children()) {
            if (child instanceof Button button && button.getY() == y) {
                button.setX(this.width / 2 - 160 + (index * buttonWidth));
                index++;
                if (index >= 3) break;
            }
        }

        this.fancystats$fancyTab = Button.builder(
                Component.translatable("fancystats.tab"),
                button -> fancystats$openFancy()
        ).bounds(x, y, buttonWidth, buttonHeight).build();

        this.addRenderableWidget(this.fancystats$fancyTab);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void fancystats$afterInit(CallbackInfo ci) {
        if (!fancystats$isFancyActive()) return;

        fancystats$openFancy();
    }

    @Inject(method = "setActiveList", at = @At("HEAD"))
    private void fancystats$onSetActiveList(ObjectSelectionList<?> newList, CallbackInfo ci) {
        if (this.fancystats$list != null && newList != this.fancystats$list) {
            fancystats$closeFancy();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void fancystats$renderFancyOverlay(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!fancystats$isFancyActive()) return;

        int center = this.width / 2;

        this.font.drawShadow(poseStack, Component.translatable("fancystats.start"), center - 160, 43, 0xFFFFFF);
        this.font.drawShadow(poseStack, Component.translatable("fancystats.end"), center + 10, 43, 0xFFFFFF);

        if (this.fancystats$loading) {
            this.font.drawShadow(poseStack, Component.translatable("fancystats.loading"), center - 35, 108, 0xFFFFFF);
        }

        if (this.fancystats$error != null) {
            this.font.drawShadow(poseStack, this.fancystats$error, center - 150, 108, 0xFF5555);
        }
    }

    @Unique
    private boolean fancystats$isFancyActive() {
        StatsScreen screen = (StatsScreen) (Object) this;
        return this.fancystats$list != null && screen.getActiveList() == this.fancystats$list;
    }

    @Unique
    private void fancystats$openFancy() {
        if (fancystats$isFancyActive()) return;

        this.fancystats$error = null;
        this.fancystats$loading = false;

        fancystats$createFancyWidgets();

        ((StatsScreen) (Object) this).setActiveList(this.fancystats$list);
    }

    @Unique
    private void fancystats$closeFancy() {
        this.fancystats$loading = false;

        if (this.fancystats$startField != null) {
            this.removeWidget(this.fancystats$startField);
            this.fancystats$startField = null;
        }

        if (this.fancystats$endField != null) {
            this.removeWidget(this.fancystats$endField);
            this.fancystats$endField = null;
        }

        if (this.fancystats$applyButton != null) {
            this.removeWidget(this.fancystats$applyButton);
            this.fancystats$applyButton = null;
        }

        this.fancystats$list = null;
        this.fancystats$error = null;
    }

    @Unique
    private void fancystats$createFancyWidgets() {
        if (this.fancystats$startField != null) this.removeWidget(this.fancystats$startField);
        if (this.fancystats$endField != null) this.removeWidget(this.fancystats$endField);
        if (this.fancystats$applyButton != null) this.removeWidget(this.fancystats$applyButton);

        int center = this.width / 2;
        long now = System.currentTimeMillis();
        long oneHourAgo = now - FANCYSTATS_HOUR;

        this.fancystats$startField = new EditBox(this.font, center - 160, 57, 150, 20, Component.translatable("fancystats.start"));
        this.fancystats$endField = new EditBox(this.font, center + 10, 57, 150, 20, Component.translatable("fancystats.end"));

        this.fancystats$startField.setValue(formatDate(oneHourAgo));
        this.fancystats$endField.setValue(formatDate(now));

        this.addRenderableWidget(this.fancystats$startField);
        this.addRenderableWidget(this.fancystats$endField);

        this.fancystats$applyButton = Button.builder(
                        Component.translatable("fancystats.apply"),
                        button -> fancystats$requestReport()
                )
                .bounds(center - 40, 83, 80, 20)
                .build();

        this.addRenderableWidget(this.fancystats$applyButton);

        this.fancystats$list = new FancyStatsList(Minecraft.getInstance(), this.width, this.height, 110, this.height - 65, 20);
    }

    @Unique
    private void fancystats$requestReport() {
        this.fancystats$error = null;

        String start = this.fancystats$startField.getValue();
        String end = this.fancystats$endField.getValue();

        long fromMillis;
        long toMillis;

        try {
            fromMillis = parseDate(start);
            toMillis = parseDate(end);
        } catch (DateTimeParseException exception) {
            this.fancystats$error = Component.translatable("fancystats.invalid_date");
            return;
        }

        if (fromMillis >= toMillis) {
            this.fancystats$error = Component.translatable("fancystats.invalid_range");
            return;
        }

        if (!StatsReportNetworking.canRequestReport()) {
            this.fancystats$error = Component.translatable("fancystats.network_unavailable");
            return;
        }

        this.fancystats$loading = true;
        this.fancystats$applyButton.active = false;
        this.fancystats$list.clearEntries();

        StatsReportNetworking.requestReport(fromMillis, toMillis);
    }

    @Unique
    private static String formatDate(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(FANCYSTATS_DATE_FORMAT);
    }

    @Unique
    private static long parseDate(String value) {
        return LocalDateTime.parse(value, FANCYSTATS_DATE_FORMAT)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    @Override
    public void fancystats$receiveReport(StatsReport report) {
        if (!fancystats$isFancyActive()) return;

        this.fancystats$loading = false;
        this.fancystats$error = null;

        if (this.fancystats$applyButton != null) {
            this.fancystats$applyButton.active = true;
        }

        if (this.fancystats$list != null) {
            this.fancystats$list.setReport(report.values());
        }
    }
}