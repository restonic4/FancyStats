package com.restonic4.fancystats.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FancyStatsList extends ObjectSelectionList<FancyStatsList.Entry> {
    public FancyStatsList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraft, width, height, top, bottom, itemHeight);
    }

    public void setReport(Map<String, Long> values) {
        clearEntries();

        List<Map.Entry<String, Long>> entries = new ArrayList<>(values.entrySet());
        entries.sort(Map.Entry.comparingByKey());

        for (Map.Entry<String, Long> entry : entries) {
            addEntry(new Entry(entry.getKey(), entry.getValue()));
        }
    }

    public static class Entry extends ObjectSelectionList.Entry<Entry> {
        private final String statId;
        private final long value;

        public Entry(String statId, long value) {
            this.statId = statId;
            this.value = value;
        }

        @Override
        public void render(
                PoseStack poseStack,
                int index,
                int y, int x,
                int entryWidth, int entryHeight,
                int mouseX, int mouseY,
                boolean hovered,
                float partialTick
        ) {
            Minecraft minecraft = Minecraft.getInstance();

            Stat<?> stat = getStat(statId);
            Component name = stat == null ? Component.literal(statId) : getStatName(stat);

            minecraft.font.drawShadow(poseStack, name, x + 2, y + 3, 0xFFFFFF);

            Component valueText = Component.literal(Long.toString(value));
            int valueWidth = minecraft.font.width(valueText);

            minecraft.font.drawShadow(poseStack, valueText, x + entryWidth - valueWidth - 2, y + 3, 0xFFFFFF);
        }

        @Override
        public Component getNarration() {
            return Component.literal(statId)
                    .copy()
                    .append(
                            ": "
                    )
                    .append(
                            Long.toString(value)
                    );
        }
    }

    private static Stat<?> getStat(String statId) {
        int separator = statId.indexOf(':');
        if (separator < 0) return null;

        ResourceLocation typeId = fromStatPart(statId.substring(0, separator));
        ResourceLocation valueId = fromStatPart(statId.substring(separator + 1));
        if (typeId == null || valueId == null) return null;

        StatType<?> rawType = BuiltInRegistries.STAT_TYPE.get(typeId);
        if (rawType == null) return null;

        Object value = rawType.getRegistry().get(valueId);
        if (value == null) return null;

        StatType<Object> type = (StatType<Object>) rawType;
        return type.get(value);
    }

    private static ResourceLocation fromStatPart(String value) {
        int separator = value.indexOf('.');
        if (separator < 0) return null;

        return new ResourceLocation(value.substring(0, separator), value.substring(separator + 1));
    }

    private static Component getStatName(Stat<?> stat) {
        if (stat.getType() == Stats.CUSTOM) {
            Stat<ResourceLocation> customStat = (Stat<ResourceLocation>) stat;
            return Component.translatable(StatsScreen.getTranslationKey(customStat));
        }

        return getValueName(stat.getValue());
    }

    private static Component getValueName(Object value) {
        if (value instanceof Block block) {
            return Component.translatable(block.getDescriptionId());
        }

        if (value instanceof Item item) {
            return Component.translatable(item.getDescriptionId());
        }

        if (value instanceof EntityType<?> entityType) {
            return Component.translatable(entityType.getDescriptionId());
        }

        return Component.literal(value.toString());
    }
}