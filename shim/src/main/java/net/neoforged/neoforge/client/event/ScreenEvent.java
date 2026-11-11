/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.client.event;

import java.util.Objects;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * Fired on different events/actions when a {@link Screen} is active and visible.
 * <p>
 * Shim: only {@link Opening} and {@link Closing} are implemented (fired by the Fabric backend from {@code Minecraft#setScreen}).
 */
public abstract class ScreenEvent extends Event {
    private final Screen screen;

    @ApiStatus.Internal
    protected ScreenEvent(Screen screen) {
        this.screen = Objects.requireNonNull(screen);
    }

    /**
     * {@return the screen that caused this event}
     */
    public Screen getScreen() {
        return screen;
    }

    /**
     * Fired before any {@link Screen} is opened, to allow changing it or preventing it from being opened.
     * All screen layers on the screen are closed before this event is fired.
     *
     * <p>This event is {@linkplain ICancellableEvent cancellable}.
     * If this event is cancelled, then the {@code Screen} shall be prevented from opening and any previous screen
     * will remain open. However, cancelling this event will not prevent the closing of screen layers which happened before
     * this event fired.</p>
     */
    public static class Opening extends ScreenEvent implements ICancellableEvent {
        @Nullable
        private final Screen currentScreen;
        @Nullable
        private Screen newScreen;

        @ApiStatus.Internal
        public Opening(@Nullable Screen currentScreen, Screen screen) {
            super(screen);
            this.currentScreen = currentScreen;
            this.newScreen = screen;
        }

        /**
         * Gets the currently open screen at the time of the event being fired.
         * <p>
         * May be null if no screen was open.
         */
        @Nullable
        public Screen getCurrentScreen() {
            return currentScreen;
        }

        /**
         * @return The screen that will be opened if the event is not cancelled. May be null.
         */
        @Nullable
        public Screen getNewScreen() {
            return newScreen;
        }

        /**
         * Sets the new screen to be opened if the event is not cancelled. May be null.
         */
        public void setNewScreen(@Nullable Screen newScreen) {
            this.newScreen = newScreen;
        }
    }

    /**
     * Fired before a {@link Screen} is closed.
     *
     * <p>This event is not {@linkplain ICancellableEvent cancellable}.</p>
     */
    public static class Closing extends ScreenEvent {
        @ApiStatus.Internal
        public Closing(Screen screen) {
            super(screen);
        }
    }
}
