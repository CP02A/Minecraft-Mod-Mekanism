package mekanism.fabric.gametest;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTask;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.lua.ObjectArguments;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import dan200.computercraft.shared.computer.core.ServerComputer;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * Everything {@link ComputerCraftTests} does with CC: Tweaked's classes. Kept out of the test class, whose methods are reflected on in every run that
 * loads the game tests (including the client game tests, which run without CC: Tweaked), so no CC: Tweaked types may appear in its signatures.
 */
final class ComputerCraftAccess {

    /**
     * Runs main thread tasks immediately, as the game tests call the peripherals from the server thread.
     */
    private static final ILuaContext MAIN_THREAD_CONTEXT = new ILuaContext() {
        @Override
        public long issueMainThreadTask(LuaTask task) throws LuaException {
            throw new LuaException("Only executeMainThreadTask is supported");
        }

        @Override
        public MethodResult executeMainThreadTask(LuaTask task) throws LuaException {
            return MethodResult.of(task.execute());
        }
    };

    private ComputerCraftAccess() {
    }

    /**
     * {@return the peripheral CC: Tweaked finds for the given side of a block, through its Fabric lookup}
     */
    @Nullable
    static Object find(ServerLevel level, BlockPos pos, Direction side) {
        return PeripheralLookup.get().find(level, pos, side);
    }

    static String type(Object peripheral) {
        return ((IPeripheral) peripheral).getType();
    }

    @Nullable
    static Object target(Object peripheral) {
        return ((IPeripheral) peripheral).getTarget();
    }

    static boolean isEqual(Object peripheral, Object other) {
        return ((IPeripheral) peripheral).equals((IPeripheral) other);
    }

    static List<String> methods(Object peripheral) {
        return Arrays.asList(((IDynamicPeripheral) peripheral).getMethodNames());
    }

    /**
     * Calls a method of a peripheral like a computer would.
     *
     * @return the method's results
     *
     * @throws IllegalStateException with the Lua error's message if the method fails
     */
    static Object[] call(Object peripheral, String method, Object... arguments) {
        IDynamicPeripheral dynamic = (IDynamicPeripheral) peripheral;
        int index = Arrays.asList(dynamic.getMethodNames()).indexOf(method);
        if (index == -1) {
            throw new IllegalArgumentException("Peripheral " + dynamic.getType() + " has no method " + method);
        }
        try {
            return dynamic.callMethod(null, MAIN_THREAD_CONTEXT, index, new ObjectArguments(arguments)).getResult();
        } catch (LuaException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Places a computer, labels it (the label selects the test's autorun script, {@code data/computercraft/lua/rom/autorun/mekanism_fabric_tests.lua})
     * and turns it on.
     */
    static void startComputer(ServerLevel level, BlockPos pos, String label) {
        Block computer = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("computercraft", "computer_advanced"));
        level.setBlockAndUpdate(pos, computer.defaultBlockState());
        AbstractComputerBlockEntity blockEntity = (AbstractComputerBlockEntity) level.getBlockEntity(pos);
        blockEntity.setLabel(label);
        blockEntity.createServerComputer().turnOn();
    }

    /**
     * {@return the label of a running computer, which the test script sets to its result}
     */
    @Nullable
    static String label(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AbstractComputerBlockEntity blockEntity) {
            ServerComputer computer = blockEntity.getServerComputer();
            return computer == null ? null : computer.getLabel();
        }
        return null;
    }
}
