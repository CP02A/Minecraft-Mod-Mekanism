package mekanism.fabric.shim;

import com.mojang.serialization.Codec;
import io.netty.buffer.Unpooled;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.attachment.AttachmentInternals;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.shim.AttachmentBackend;
import org.jspecify.annotations.Nullable;

/**
 * Stores NeoForge style data attachments with Fabric's data attachment API.
 * <p>
 * Each NeoForge attachment type is backed by a Fabric attachment type of the same id that holds a {@link Box}. As NeoForge's serializers and sync
 * handlers have access to the holder, but Fabric's codecs don't, loaded and synced data is kept in its serialized form until it is first accessed
 * through the holder.
 */
public class FabricAttachmentBackend implements AttachmentBackend {

    private static final Map<AttachmentType<?>, net.fabricmc.fabric.api.attachment.v1.AttachmentType<Box<?>>> TYPES = new ConcurrentHashMap<>();

    /**
     * Data of an attachment. Exactly one of the value, the serialized (persisted) form, or the synced form is present.
     */
    private static final class Box<T> {

        @Nullable
        T value;
        @Nullable
        CompoundTag serialized;
        byte @Nullable [] synced;

        static <T> Box<T> of(T value) {
            Box<T> box = new Box<>();
            box.value = value;
            return box;
        }
    }

    /**
     * Creates the Fabric attachment types for all registered NeoForge style attachment types.
     */
    public static void registerTypes() {
        for (AttachmentType<?> type : NeoForgeRegistries.ATTACHMENT_TYPES) {
            fabricType(type);
        }
    }

    private static <T> net.fabricmc.fabric.api.attachment.v1.AttachmentType<Box<?>> fabricType(AttachmentType<T> type) {
        return TYPES.computeIfAbsent(type, t -> {
            Identifier id = Objects.requireNonNull(NeoForgeRegistries.ATTACHMENT_TYPES.getKey(t), "Unregistered attachment type");
            return AttachmentRegistry.<Box<?>>create(id, builder -> {
                IAttachmentSerializer<T> serializer = AttachmentInternals.serializer(type);
                if (serializer != null) {
                    builder.persistent(persistenceCodec(type, serializer));
                }
                AttachmentSyncHandler<T> syncHandler = AttachmentInternals.syncHandler(type);
                if (syncHandler != null) {
                    builder.syncWith(syncCodec(syncHandler), (target, player) -> {
                        Box<?> box = target.getAttached(TYPES.get(type));
                        return box != null && box.value != null && syncHandler.sendToPlayer((IAttachmentHolder) target, player);
                    });
                }
            });
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> Codec<Box<?>> persistenceCodec(AttachmentType<T> type, IAttachmentSerializer<T> serializer) {
        return CompoundTag.CODEC.xmap(tag -> {
            Box<T> box = new Box<>();
            box.serialized = tag;
            return box;
        }, box -> {
            Box<T> typed = (Box<T>) box;
            if (typed.serialized != null) {
                return typed.serialized;
            } else if (typed.value == null) {
                return new CompoundTag();
            }
            ProblemReporter.Collector reporter = new ProblemReporter.Collector();
            TagValueOutput output = TagValueOutput.createWithContext(reporter, fallbackProvider());
            if (!serializer.write(typed.value, output)) {
                return new CompoundTag();
            }
            return output.buildResult();
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> StreamCodec<RegistryFriendlyByteBuf, Box<?>> syncCodec(AttachmentSyncHandler<T> syncHandler) {
        return new StreamCodec<>() {
            @Override
            public Box<?> decode(RegistryFriendlyByteBuf buf) {
                Box<T> box = new Box<>();
                box.synced = ByteBufCodecs.BYTE_ARRAY.decode(buf);
                return box;
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, Box<?> box) {
                RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(Unpooled.buffer(), buf.registryAccess());
                syncHandler.write(data, Objects.requireNonNull(((Box<T>) box).value), true);
                byte[] bytes = new byte[data.readableBytes()];
                data.readBytes(bytes);
                ByteBufCodecs.BYTE_ARRAY.encode(buf, bytes);
            }
        };
    }

    private static HolderLookup.Provider fallbackProvider() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? RegistryAccess.EMPTY : server.registryAccess();
    }

    private static HolderLookup.Provider provider(IAttachmentHolder holder) {
        return switch (holder) {
            case Entity entity -> entity.registryAccess();
            case Level level -> level.registryAccess();
            case BlockEntity blockEntity when blockEntity.getLevel() != null -> blockEntity.getLevel().registryAccess();
            case LevelChunk chunk -> chunk.getLevel().registryAccess();
            default -> fallbackProvider();
        };
    }

    private static AttachmentTarget target(IAttachmentHolder holder) {
        return (AttachmentTarget) holder;
    }

    /**
     * {@return the attachment's value, resolving the serialized or synced form if necessary, or {@code null} if there is no data}
     */
    @Nullable
    @SuppressWarnings("unchecked")
    private static <T> T resolve(IAttachmentHolder holder, AttachmentType<T> type, @Nullable Box<?> raw) {
        if (raw == null) {
            return null;
        }
        Box<T> box = (Box<T>) raw;
        if (box.value == null) {
            if (box.serialized != null) {
                CompoundTag tag = box.serialized;
                box.serialized = null;
                IAttachmentSerializer<T> serializer = AttachmentInternals.serializer(type);
                if (serializer != null && !tag.isEmpty()) {
                    ProblemReporter.Collector reporter = new ProblemReporter.Collector();
                    box.value = serializer.read(holder, TagValueInput.create(reporter, provider(holder), tag));
                }
            } else if (box.synced != null) {
                byte[] bytes = box.synced;
                box.synced = null;
                AttachmentSyncHandler<T> syncHandler = AttachmentInternals.syncHandler(type);
                if (syncHandler != null) {
                    RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), (RegistryAccess) provider(holder));
                    box.value = syncHandler.read(holder, buf, null);
                }
            }
        }
        return box.value;
    }

    @Override
    public boolean hasAttachments(IAttachmentHolder holder) {
        AttachmentTarget target = target(holder);
        for (Map.Entry<AttachmentType<?>, net.fabricmc.fabric.api.attachment.v1.AttachmentType<Box<?>>> entry : TYPES.entrySet()) {
            if (target.hasAttached(entry.getValue()) && hasData(holder, entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasData(IAttachmentHolder holder, AttachmentType<?> type) {
        return getExistingData(holder, type) != null;
    }

    @Override
    public <T> T getData(IAttachmentHolder holder, AttachmentType<T> type) {
        T existing = getExistingData(holder, type);
        if (existing == null) {
            existing = AttachmentInternals.createDefault(type, holder);
            target(holder).setAttached(fabricType(type), Box.of(existing));
        }
        return existing;
    }

    @Override
    public <T> @Nullable T getExistingData(IAttachmentHolder holder, AttachmentType<T> type) {
        return resolve(holder, type, target(holder).getAttached(fabricType(type)));
    }

    @Override
    public <T> @Nullable T setData(IAttachmentHolder holder, AttachmentType<T> type, T data) {
        T previous = getExistingData(holder, type);
        target(holder).setAttached(fabricType(type), Box.of(data));
        return previous;
    }

    @Override
    public <T> @Nullable T removeData(IAttachmentHolder holder, AttachmentType<T> type) {
        T previous = getExistingData(holder, type);
        target(holder).removeAttached(fabricType(type));
        return previous;
    }

    @Override
    public void syncData(IAttachmentHolder holder, AttachmentType<?> type) {
        resync(holder, type);
    }

    private static <T> void resync(IAttachmentHolder holder, AttachmentType<T> type) {
        T value = getExisting(holder, type);
        if (value != null && AttachmentInternals.syncHandler(type) != null) {
            //Fabric syncs attachments whenever they are set
            target(holder).setAttached(fabricType(type), Box.of(value));
        }
    }

    @Nullable
    private static <T> T getExisting(IAttachmentHolder holder, AttachmentType<T> type) {
        return resolve(holder, type, target(holder).getAttached(fabricType(type)));
    }

    /**
     * Copies the attachments of a player that respawns, like NeoForge does: all serializable attachments if the player didn't die (returned from the
     * end), and those that are marked to be copied on death otherwise.
     */
    public static void copyOnRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        for (AttachmentType<?> type : TYPES.keySet()) {
            copy(type, oldPlayer, newPlayer, alive);
        }
    }

    private static <T> void copy(AttachmentType<T> type, ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        if (AttachmentInternals.serializer(type) == null || (!alive && !AttachmentInternals.copyOnDeath(type))) {
            if (!alive) {
                //Fabric may have transferred it already
                newPlayer.removeAttached(fabricType(type));
            }
            return;
        }
        T value = getExisting(oldPlayer, type);
        if (value != null) {
            T copy = AttachmentInternals.copyHandler(type).copy(value, newPlayer, newPlayer.registryAccess());
            if (copy == null) {
                newPlayer.removeAttached(fabricType(type));
            } else {
                newPlayer.setAttached(fabricType(type), Box.of(copy));
            }
        }
    }
}
