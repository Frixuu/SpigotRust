package xyz.lukasz.pluginbridge;

import static jdk.incubator.foreign.CLinker.*;

import jdk.incubator.foreign.*;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.*;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.lukasz.pluginbridge.util.Streams;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class PluginBridge extends JavaPlugin {

    private static PluginBridge instance;

    private Set<LibraryLookup> nativeLibraries = new HashSet<>();

    @Override
    public void onEnable() {

        instance = this;
        saveDefaultConfig();

        final var cLinker = CLinker.getInstance();
        final var pluginDir = new File(
            getDataFolder().getParentFile().getParentFile(), "plugins-native");

        if (!pluginDir.exists()) {
            pluginDir.mkdir();
        }

        try {
            nativeLibraries = Streams
                .of(Files.newDirectoryStream(pluginDir.toPath(), "*.{dll,so,dylib}"))
                .map(path -> LibraryLookup.ofPath(path.toAbsolutePath()))
                .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            final var upcallHandle = MethodHandles.lookup()
                .findStatic(PluginBridge.class,
                    "someJavaMethod",
                    MethodType.methodType(void.class));
            MemorySegment javaMethodFunc = cLinker.upcallStub(
                upcallHandle,
                FunctionDescriptor.ofVoid()
            );

            getLogger().info("Enabling " + nativeLibraries.size() + " native plugins");
            nativeLibraries.forEach(library -> {

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
            });

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