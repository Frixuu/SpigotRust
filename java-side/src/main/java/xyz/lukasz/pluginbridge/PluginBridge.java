package xyz.lukasz.pluginbridge;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.LibraryLookup;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.lukasz.pluginbridge.util.Streams;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.invoke.MethodType.methodType;
import static java.nio.charset.StandardCharsets.UTF_8;
import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_POINTER;

public class PluginBridge extends JavaPlugin {

    private static PluginBridge instance;

    @Override
    public void onEnable() {

        instance = this;
        final var logger = getLogger();

        // Find or create a directory where the native libs will be loaded from
        final var serverRoot = getDataFolder().getParentFile().getParentFile();
        final var pluginDir = new File(serverRoot, "plugins-native");
        if (!pluginDir.exists()) {
            pluginDir.mkdir();
        }

        // Find all libraries that we can load
        Map<Path, LibraryLookup> nativeLibraries;
        try {
            nativeLibraries = Streams
                .of(Files.newDirectoryStream(
                    pluginDir.toPath(),
                    "*.{dll,so,dylib}"))
                .map(Path::toAbsolutePath)
                .collect(Collectors.toUnmodifiableMap(
                    Function.identity(),
                    LibraryLookup::ofPath));
        } catch (Exception e) {
            logger.severe("Could not load native libraries: " + e.getMessage());
            return;
        }

        final var linker = CLinker.getInstance();

        try {

            // We can pass Java function pointers to native code
            final var lookup =  MethodHandles.lookup();
            final var javaMethodPtr = linker.upcallStub(
                lookup.findStatic(PluginBridge.class, "someJavaMethod", methodType(void.class)),
                FunctionDescriptor.ofVoid()
            );

            logger.info("There are " + nativeLibraries.size() + " native plugins");
            nativeLibraries.forEach((path, library) -> {

                // The native lib must have a function we want to call
                final var symbol = library.lookup("create_greeting");
                if (symbol.isEmpty()) {
                    logger.warning("Symbol not found in " + path.getFileName());
                    return;
                }

                // Create a handle to that function
                final var handle = linker.downcallHandle(
                    symbol.get(),
                    methodType(
                        int.class,
                        MemoryAddress.class,
                        MemoryAddress.class,
                        MemoryAddress.class,
                        int.class
                    ),
                    FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER, C_INT)
                );

                // Closing a MemorySegment frees allocated memory associated with this resource
                try (final var input = CLinker.toCString("world", UTF_8);
                     final var outputBuffer = MemorySegment.allocateNative(200)) {

                    // Call a native function
                    final int bytesWritten = (int) handle.invokeExact(
                        input.address(),
                        javaMethodPtr.address(),
                        outputBuffer.address(),
                        200);

                    final var greeting = CLinker.toJavaString(outputBuffer, UTF_8);
                    logger.info(greeting);
                    logger.info("(BTW, the response was " + bytesWritten + " bytes long.)");
                } catch (Throwable t) {
                    logger.severe(t.toString());
                    t.printStackTrace();
                }
            });

        } catch (Throwable t) {
            logger.severe(t.getMessage());
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