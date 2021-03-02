package com.wwt.testing.files;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import static com.wwt.testing.files.Preconditions.checkArgument;
import static java.util.function.Predicate.not;

public class TextFileTransformer {
    private final Function<String, String> lineTransformer;

    public TextFileTransformer(Function<String, String> lineTransformer) {
        this.lineTransformer = lineTransformer;
    }

    public void transform(Path source, Path destination) throws IOException {
        checkArgument(source, Files::isRegularFile, "Source must be file.");
        checkArgument(destination, not(Files::isDirectory), "Destination cannot be directory.");

        try (var lines = Files.lines(source);
             var writer = Files.newBufferedWriter(destination);
             var printWriter = new PrintWriter(writer)) {

            lines
                .map(lineTransformer)
                .forEach(printWriter::println);
        }
    }
}
