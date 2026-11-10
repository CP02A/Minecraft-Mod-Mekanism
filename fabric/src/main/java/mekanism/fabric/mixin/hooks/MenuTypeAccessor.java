package mekanism.fabric.mixin.hooks;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MenuType.class)
public interface MenuTypeAccessor<T extends AbstractContainerMenu> {

    @Accessor("constructor")
    MenuType.MenuSupplier<T> mekanism$getConstructor();
}
