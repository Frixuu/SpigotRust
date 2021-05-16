package xyz.lukasz.pluginbridge.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

public final class Streams {

    public static Stream<Path> of(DirectoryStream<Path> ds) throws IOException {
        final var elements = new ArrayList<Path>();
        for (var p : ds) {
            elements.add(p);
        }
        ds.close();
        return elements.stream();
    }
}
