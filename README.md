# SpigotRust

An experiment combining Spigot Java plugins and Rust.  
Please note that this project requires exactly Java 16.
Tested with OpenJDK 16.0.1, x64 Windows, Rust 1.51, MSVC toolchain.

## Building & running

1. Build the Java plugin with ```mvn package```. Throw the resulting ```.jar``` into your server's ```/plugins``` directory.
2. Build the Rust library with ```cargo build --release```. Put the ```plugin_native``` .dll/.so in the ```/plugins/PluginBridge``` folder.
3. Start your server with foreign access features enabled.
> Example command: ```java -D"foreign.restricted"="permit" --add-modules=jdk.incubator.foreign -jar paper.jar -nogui```   
4. You should be able to see a ```[PluginBridge] Hello from Rust, world!``` log message in your terminal.
