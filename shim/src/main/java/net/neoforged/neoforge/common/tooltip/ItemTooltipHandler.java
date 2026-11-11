/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.tooltip;

import com.google.common.graph.ElementOrder;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.SequencedMap;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * Fabric shim version of NeoForge's tooltip appender handling. NeoForge replaces vanilla's data component tooltips in
 * {@link ItemStack#addDetailsToTooltip} with appenders sorted together with the modded ones. On Fabric vanilla keeps adding its own component tooltips
 * (so other mods' mixins into it keep working), and the modded appenders are run after the vanilla component they were sorted behind (called from
 * {@code ItemStackTooltipMixin}).
 */
@ApiStatus.Internal
public final class ItemTooltipHandler {
    /**
     * The vanilla component tooltips in the order {@link ItemStack#addDetailsToTooltip} adds them (matches NeoForge's
     * {@code VanillaDataComponentTooltips}).
     */
    private static final List<DataComponentType<?>> VANILLA_APPENDER_ORDER = List.of(
            DataComponents.TROPICAL_FISH_PATTERN,
            DataComponents.INSTRUMENT,
            DataComponents.MAP_ID,
            DataComponents.BEES,
            DataComponents.CONTAINER_LOOT,
            DataComponents.CONTAINER,
            DataComponents.BANNER_PATTERNS,
            DataComponents.POT_DECORATIONS,
            DataComponents.WRITTEN_BOOK_CONTENT,
            DataComponents.CHARGED_PROJECTILES,
            DataComponents.FIREWORKS,
            DataComponents.FIREWORK_EXPLOSION,
            DataComponents.POTION_CONTENTS,
            DataComponents.JUKEBOX_PLAYABLE,
            DataComponents.TRIM,
            DataComponents.STORED_ENCHANTMENTS,
            DataComponents.ENCHANTMENTS,
            DataComponents.DYED_COLOR,
            DataComponents.PROFILE,
            DataComponents.LORE,
            DataComponents.SULFUR_CUBE_CONTENT,
            DataComponents.ATTRIBUTE_MODIFIERS,
            DataComponents.INTANGIBLE_PROJECTILE,
            DataComponents.UNBREAKABLE,
            DataComponents.SIGN_TEXT_FRONT,
            DataComponents.SIGN_TEXT_BACK,
            DataComponents.OMINOUS_BOTTLE_AMPLIFIER,
            DataComponents.SUSPICIOUS_STEW_EFFECTS,
            DataComponents.BLOCK_STATE,
            DataComponents.ENTITY_DATA,
            DataComponents.BLOCK_ENTITY_DATA,
            DataComponents.CAN_BREAK,
            DataComponents.CAN_PLACE_ON,
            DataComponents.DAMAGE);
    private static final TooltipAppender VANILLA_PLACEHOLDER = (_, _, _, _, _, _) -> {};

    private static final List<TooltipAppender> HEAD_APPENDERS = new ArrayList<>();
    /** {@link TooltipLocation#POST_CUSTOM} appenders and component appenders sorted before all vanilla component tooltips */
    private static final List<TooltipAppender> POST_CUSTOM_APPENDERS = new ArrayList<>();
    /** Component appenders sorted after the given vanilla component (and before the next one) */
    private static final Map<DataComponentType<?>, List<TooltipAppender>> AFTER_VANILLA_APPENDERS = new IdentityHashMap<>();
    private static final List<TooltipAppender> PRE_ITEM_INFO_APPENDERS = new ArrayList<>();
    private static final List<TooltipAppender> TAIL_APPENDERS = new ArrayList<>();
    private static boolean initialized;

    private static void append(List<TooltipAppender> appenders, ItemStack stack, Item.TooltipContext context, TooltipDisplay display, @Nullable Player player,
            TooltipFlag tooltipFlag, Consumer<Component> builder) {
        for (TooltipAppender appender : appenders) {
            appender.append(stack, context, display, player, tooltipFlag, builder);
        }
    }

    public static void addDetailsToTooltipHead(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, @Nullable Player player,
            TooltipFlag tooltipFlag, Consumer<Component> builder) {
        append(HEAD_APPENDERS, stack, context, display, player, tooltipFlag, builder);
    }

    /**
     * Called after {@link Item#appendHoverText} and before vanilla's first component tooltip.
     */
    public static void addDetailsToTooltipPostCustom(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, @Nullable Player player,
            TooltipFlag tooltipFlag, Consumer<Component> builder) {
        append(POST_CUSTOM_APPENDERS, stack, context, display, player, tooltipFlag, builder);
    }

    /**
     * Called after vanilla added the tooltip of the given component. After the last vanilla component ({@link DataComponents#DAMAGE}) this also runs
     * the {@link TooltipLocation#PRE_ITEM_INFO} appenders.
     */
    public static void addDetailsAfterComponent(DataComponentType<?> vanillaType, ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            @Nullable Player player, TooltipFlag tooltipFlag, Consumer<Component> builder) {
        List<TooltipAppender> appenders = AFTER_VANILLA_APPENDERS.get(vanillaType);
        if (appenders != null) {
            append(appenders, stack, context, display, player, tooltipFlag, builder);
        }
        if (vanillaType == DataComponents.DAMAGE) {
            append(PRE_ITEM_INFO_APPENDERS, stack, context, display, player, tooltipFlag, builder);
        }
    }

    public static void addDetailsToTooltipTail(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, @Nullable Player player,
            TooltipFlag tooltipFlag, Consumer<Component> builder) {
        append(TAIL_APPENDERS, stack, context, display, player, tooltipFlag, builder);
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        Map<TooltipLocation, List<TooltipAppender>> appenders = new EnumMap<>(TooltipLocation.class);
        SequencedMap<DataComponentType<?>, TooltipAppender> componentAppenders = new LinkedHashMap<>();
        MutableGraph<DataComponentType<?>> componentGraph = GraphBuilder.directed().nodeOrder(ElementOrder.insertion()).build();
        for (DataComponentType<?> type : VANILLA_APPENDER_ORDER) {
            componentAppenders.put(type, VANILLA_PLACEHOLDER);
            componentGraph.addNode(type);
        }
        for (int i = 1; i < VANILLA_APPENDER_ORDER.size(); i++) {
            componentGraph.putEdge(VANILLA_APPENDER_ORDER.get(i - 1), VANILLA_APPENDER_ORDER.get(i));
        }
        ModLoader.postEvent(new RegisterTooltipAppendersEvent(appenders, componentAppenders, componentGraph, VANILLA_APPENDER_ORDER.getFirst(),
                VANILLA_APPENDER_ORDER.getLast()));

        HEAD_APPENDERS.addAll(appenders.getOrDefault(TooltipLocation.HEAD, List.of()));
        POST_CUSTOM_APPENDERS.addAll(appenders.getOrDefault(TooltipLocation.POST_CUSTOM, List.of()));
        List<TooltipAppender> current = POST_CUSTOM_APPENDERS;
        for (DataComponentType<?> type : sort(componentGraph, componentAppenders.sequencedKeySet())) {
            TooltipAppender appender = componentAppenders.get(type);
            if (appender == VANILLA_PLACEHOLDER) {
                current = AFTER_VANILLA_APPENDERS.computeIfAbsent(type, _ -> new ArrayList<>());
            } else if (appender != null) {
                current.add(appender);
            }
        }
        PRE_ITEM_INFO_APPENDERS.addAll(appenders.getOrDefault(TooltipLocation.PRE_ITEM_INFO, List.of()));
        TAIL_APPENDERS.addAll(appenders.getOrDefault(TooltipLocation.TAIL, List.of()));
    }

    /**
     * Topological sort that breaks ties by insertion order, same as NeoForge's {@code CommonHooks#sortGraphChecked} (FML's {@code TopologicalSort}).
     */
    private static <T> List<T> sort(MutableGraph<T> graph, Iterable<T> values) {
        Map<T, Integer> insertionOrder = new IdentityHashMap<>();
        for (T value : values) {
            insertionOrder.put(value, insertionOrder.size());
        }
        for (T node : graph.nodes()) {
            insertionOrder.putIfAbsent(node, insertionOrder.size());
        }
        Map<T, Integer> inDegree = new IdentityHashMap<>();
        PriorityQueue<T> queue = new PriorityQueue<>(Comparator.comparingInt(insertionOrder::get));
        for (T node : graph.nodes()) {
            int degree = graph.inDegree(node);
            inDegree.put(node, degree);
            if (degree == 0) {
                queue.add(node);
            }
        }
        List<T> sorted = new ArrayList<>(graph.nodes().size());
        while (!queue.isEmpty()) {
            T node = queue.remove();
            sorted.add(node);
            for (T successor : graph.successors(node)) {
                if (inDegree.merge(successor, -1, Integer::sum) == 0) {
                    queue.add(successor);
                }
            }
        }
        if (sorted.size() != graph.nodes().size()) {
            List<T> cyclic = graph.nodes().stream().filter(node -> inDegree.get(node) > 0).toList();
            throw new IllegalStateException("Cycles were detected during component tooltip appenders sorting, involving: " + cyclic);
        }
        return sorted;
    }

    @ApiStatus.Internal
    public static List<DataComponentType<?>> getVanillaAppenderOrder() {
        return VANILLA_APPENDER_ORDER;
    }

    private ItemTooltipHandler() {}
}
