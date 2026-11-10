package mekanism.fabric.shim;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * The shim's mod list on Fabric: contains a container for each mod that uses the shim, and mod infos for all Fabric mods.
 */
public final class FabricModList extends ModList {

    public static final FabricModList INSTANCE = new FabricModList();

    private final List<FabricModContainer> containers = new CopyOnWriteArrayList<>();
    private final List<ModFileScanData> scanData = new CopyOnWriteArrayList<>();
    private List<IModInfo> mods;

    private FabricModList() {
    }

    public void addContainer(FabricModContainer container, ModFileScanData data) {
        containers.add(container);
        scanData.add(data);
    }

    @Override
    public boolean isLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Optional<FabricModContainer> getModContainerById(String modId) {
        return containers.stream().filter(container -> container.getModId().equals(modId)).findFirst();
    }

    @Override
    public List<FabricModContainer> getModContainers() {
        return containers;
    }

    @Override
    public synchronized List<IModInfo> getMods() {
        if (mods == null) {
            List<IModInfo> infos = new ArrayList<>();
            for (net.fabricmc.loader.api.ModContainer mod : FabricLoader.getInstance().getAllMods()) {
                infos.add(new FabricModInfo(mod.getMetadata()));
            }
            mods = List.copyOf(infos);
        }
        return mods;
    }

    @Override
    public List<ModFileScanData> getAllScanData() {
        return scanData;
    }

    public record FabricModInfo(ModMetadata metadata) implements IModInfo {

        @Override
        public String getModId() {
            return metadata.getId();
        }

        @Override
        public String getDisplayName() {
            return metadata.getName();
        }

        @Override
        public String getDescription() {
            return metadata.getDescription();
        }

        @Override
        public ArtifactVersion getVersion() {
            return new DefaultArtifactVersion(metadata.getVersion().getFriendlyString());
        }
    }
}
