package xyz.lukasz.pluginbridge;

import static jdk.incubator.foreign.CLinker.*;
import jdk.incubator.foreign.*;

import java.io.File;
import java.lang.invoke.*;

import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public class PluginBridge extends JavaPlugin {

    private static PluginBridge instance;

    @Override
    public void onEnable() {

        instance = this;
        saveDefaultConfig();

        final var nativePath = Path.of(
            new File(getDataFolder(), "plugin_native.dll")
                .getAbsolutePath());

        final var library = LibraryLookup.ofPath(nativePath);
        final var cLinker = CLinker.getInstance();

        try {
            final var upcallHandle = MethodHandles.lookup()
                .findStatic(PluginBridge.class,
                    "someJavaMethod",
                    MethodType.methodType(void.class));
            MemorySegment javaMethodFunc = cLinker.upcallStub(
                upcallHandle,
                FunctionDescriptor.ofVoid()
            );

            final var handle = cLinker.downcallHandle(
                library.lookup("create_greeting").get(),
                MethodType.methodType(int.class,
                    MemoryAddress.class,
                    MemoryAddress.class,
                    MemoryAddress.class,
                    int.class),
                FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER, C_INT)
            );

            final var input = CLinker.toCString("world").address();
            final var outputBuffer = MemorySegment.allocateNative(200);

            try {
                final int bytesWritten = (int) handle.invokeExact(
                    input,
                    javaMethodFunc.address(),
                    outputBuffer.address(),
                    200);
                final var greeting = CLinker.toJavaString(outputBuffer);
                getLogger().info(greeting);
            } catch (Throwable t) {
                getLogger().severe(t.toString());
                t.printStackTrace();
            }

            CLinker.freeMemoryRestricted(input);
            CLinker.freeMemoryRestricted(outputBuffer.address());
        } catch (Throwable t) {
            getLogger().severe(t.getMessage());
            t.printStackTrace();
        }
    }

    public static PluginBridge getInstance() {
        return instance;
    }

    public static void someJavaMethod() {
        getInstance().getLogger().info("Hello from Java!");
    }

    @Override
    public void onDisable() {

    }
}