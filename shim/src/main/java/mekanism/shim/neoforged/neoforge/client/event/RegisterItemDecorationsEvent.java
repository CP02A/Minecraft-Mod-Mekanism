/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package mekanism.shim.neoforged.neoforge.client.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.Event;
import mekanism.shim.neoforged.fml.LogicalSide;
import mekanism.shim.neoforged.fml.event.IModBusEvent;
import mekanism.shim.neoforged.neoforge.client.IItemDecorator;
import org.jetbrains.annotations.ApiStatus;

/**
 * Allows users to register custom {@linkplain IItemDecorator IItemDecorator} to Items.
 *
 * <p>This event is not {@linkplain ICancellableEvent cancelable}, and does not {@linkplain HasResult have a result}.
 *
 * <p>This event is fired on the mod-specific event bus, only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
 */
public class RegisterItemDecorationsEvent extends Event implements IModBusEvent {
    private final Map<Item, List<IItemDecorator>> decorators;

    @ApiStatus.Internal
    public RegisterItemDecorationsEvent(Map<Item, List<IItemDecorator>> decorators) {
        this.decorators = decorators;
    }

    /**
     * Register an ItemDecorator to an Item
     */
    public void register(ItemLike itemLike, IItemDecorator decorator) {
        List<IItemDecorator> itemDecoratorList = decorators.computeIfAbsent(itemLike.asItem(), item -> new ArrayList<>());
        itemDecoratorList.add(decorator);
    }
}
