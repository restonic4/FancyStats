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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FancyStatsList extends ObjectSelectionList<FancyStatsList.Entry> {
    public FancyStatsList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraft, width, height, top, bottom, itemHeight);
    }

    public void setReport(Map<String, Long> values) {
        clearEntries();

        Map<Category, List<Map.Entry<String, Long>>> categories = new LinkedHashMap<>();

        for (Map.Entry<String, Long> entry : values.entrySet()) {
            Stat<?> stat = getStat(entry.getKey());
            Category category = getCategory(stat);
            categories.computeIfAbsent(category, ignored -> new ArrayList<>()).add(entry);
        }

        for (Map.Entry<Category, List<Map.Entry<String, Long>>> category : categories.entrySet()) {
            addEntry(new HeaderEntry(category.getKey().title));

            category.getValue().sort(Comparator.comparing(entry -> {
                Stat<?> stat = getStat(entry.getKey());
                return stat == null ? entry.getKey() : getStatName(stat).getString();
            }));

            for (Map.Entry<String, Long> entry : category.getValue()) {
                addEntry(new StatEntry(entry.getKey(), entry.getValue()));
            }
        }
    }

    private static Category getCategory(Stat<?> stat) {
        if (stat == null) return Category.OTHER;
        if (stat.getType() == Stats.BLOCK_MINED) return Category.BLOCKS_MINED;
        if (stat.getType() == Stats.ITEM_CRAFTED) return Category.ITEMS_CRAFTED;
        if (stat.getType() == Stats.ITEM_USED) return Category.ITEMS_USED;
        if (stat.getType() == Stats.ITEM_BROKEN) return Category.ITEMS_BROKEN;
        if (stat.getType() == Stats.ITEM_PICKED_UP) return Category.ITEMS_PICKED_UP;
        if (stat.getType() == Stats.ITEM_DROPPED) return Category.ITEMS_DROPPED;
        if (stat.getType() == Stats.ENTITY_KILLED) return Category.ENTITIES_KILLED;
        if (stat.getType() == Stats.ENTITY_KILLED_BY) return Category.ENTITIES_KILLED_BY;
        if (stat.getType() == Stats.CUSTOM) return Category.CUSTOM;
        return Category.OTHER;
    }

    public static abstract class Entry extends ObjectSelectionList.Entry<Entry> {}

    public static class HeaderEntry extends Entry {
        private final Component title;

        public HeaderEntry(Component title) {
            this.title = title;
        }

        @Override
        public void render(PoseStack poseStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick) {
            Minecraft minecraft = Minecraft.getInstance();
            int titleWidth = minecraft.font.width(title);
            minecraft.font.drawShadow(poseStack, title, x + (entryWidth - titleWidth) / 2, y + 3, 0xFFFFFF);
        }

        @Override
        public Component getNarration() {
            return title;
        }
    }

    public static class StatEntry extends Entry {
        private final String statId;
        private final long value;

        public StatEntry(String statId, long value) {
            this.statId = statId;
            this.value = value;
        }

        @Override
        public void render(PoseStack poseStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick) {
            Minecraft minecraft = Minecraft.getInstance();

            Stat<?> stat = getStat(statId);
            Component name = stat == null ? Component.literal(statId) : getStatName(stat);

            minecraft.font.drawShadow(poseStack, name, x + 2, y + 3, 0xFFFFFF);

            Component valueText = Component.literal(formatStatValue(stat, value));
            int valueWidth = minecraft.font.width(valueText);

            minecraft.font.drawShadow(poseStack, valueText, x + entryWidth - valueWidth - 2, y + 3, 0xFFFFFF);
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.literal(statId).copy().append(": ").append(Long.toString(value));
        }

        private static String formatStatValue(Stat<?> stat, long value) {
            if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                return stat.format((int) value);
            }

            return Long.toString(value);
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

    private enum Category {
        CUSTOM(Component.translatable("fancystats.category.custom")),
        BLOCKS_MINED(Component.translatable("fancystats.category.blocks_mined")),
        ITEMS_CRAFTED(Component.translatable("fancystats.category.items_crafted")),
        ITEMS_USED(Component.translatable("fancystats.category.items_used")),
        ITEMS_BROKEN(Component.translatable("fancystats.category.items_broken")),
        ITEMS_PICKED_UP(Component.translatable("fancystats.category.items_picked_up")),
        ITEMS_DROPPED(Component.translatable("fancystats.category.items_dropped")),
        ENTITIES_KILLED(Component.translatable("fancystats.category.entities_killed")),
        ENTITIES_KILLED_BY(Component.translatable("fancystats.category.entities_killed_by")),
        OTHER(Component.translatable("fancystats.category.other"));

        private final Component title;

        Category(Component title) {
            this.title = title;
        }
    }
}