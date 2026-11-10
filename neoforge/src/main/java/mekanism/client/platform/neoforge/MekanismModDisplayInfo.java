package mekanism.client.platform.neoforge;

import java.util.function.Supplier;
import mekanism.common.base.holiday.HolidayManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.modlist.DefaultModDisplayInfo;
import net.neoforged.neoforge.client.gui.modlist.ImageResource;
import org.jspecify.annotations.Nullable;

public class MekanismModDisplayInfo extends DefaultModDisplayInfo {

    private final Supplier<Component> translatableDisplayName;

    public MekanismModDisplayInfo(ModContainer container, Supplier<Component> translatableDisplayName) {
        super(container);
        this.translatableDisplayName = translatableDisplayName;
    }

    @Override
    public Component displayName() {
        //Allow translating the display name so that it can display upside down
        return translatableDisplayName.get();
    }

    @Override
    @Nullable
    public ImageResource icon() {
        Identifier icon = HolidayManager.getCustomModIconToday();
        if (icon != null) {
            return ImageResource.packAsset(icon);
        }
        return super.icon();
    }
}