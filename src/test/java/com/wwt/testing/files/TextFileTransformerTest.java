package com.wwt.testing.files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test file IO with JUnit5")
class TextFileTransformerTest {
    private final TextFileTransformer testObject = new TextFileTransformer(String::toUpperCase);

    @Test
    @DisplayName("Should transform single line source to destination")
    void transformsSingleLineFile(@TempDir Path tempDir) throws IOException {
        var destination = tempDir.resolve("destination.txt");
        var source = tempDir.resolve("source.txt");
        Files.write(source, List.of("Hello World!"));

        testObject.transform(source, destination);

        assertEquals("HELLO WORLD!", Files.readAllLines(destination).get(0));
    }

    @Test
    @DisplayName("Should transform multiline source to destination")
    void transformManyLines(@TempDir Path tempDir) throws IOException {
        Path destination = tempDir.resolve("destination.txt");
        Path source = tempDir.resolve("source.txt");
        Files.write(source, List.of("Larry", "Curly", "Moe"));

        testObject.transform(source, destination);

        assertEquals(List.of("LARRY", "CURLY", "MOE"), Files.readAllLines(destination));
    }

    @Test
    @DisplayName("Should overwrite existing destination")
    void shouldOverwriteExistingDestination(@TempDir Path tempDir) throws IOException {
        Path destination = tempDir.resolve("destination.txt");
        Path source = tempDir.resolve("source.txt");
        Files.write(source, List.of("Larry", "Curly", "Moe"));
        Files.write(destination, List.of("Hope this wasn't important"));

        testObject.transform(source, destination);

        assertEquals(List.of("LARRY", "CURLY", "MOE"), Files.readAllLines(destination));
    }

    @Test
    @DisplayName("Source cannot be directory.")
    void sourceCannotBeDirectory(@TempDir Path tempDir) {
        Path destination = tempDir.resolve("should-not-create.txt");

        assertThrows(IllegalArgumentException.class, () ->
            testObject.transform(tempDir, destination)
        );
        assertFalse(Files.exists(destination));
    }


    @Test
    @DisplayName("Source file must exist")
    void sourceMustExist(@TempDir Path tempDir) {
        Path destination = tempDir.resolve("destination.txt");
        Path source = tempDir.resolve("source.txt");

        assertThrows(IllegalArgumentException.class, () ->
            testObject.transform(source, destination)
        );
    }

    @Test
    @DisplayName("Source must be readable")
    void sourceMustBeReadable(@TempDir Path tempDir) throws IOException {
        Path destination = tempDir.resolve("destination.txt");
        Path source = tempDir.resolve("source.txt");
        Files.write(source, List.of("We", "Will", "Never", "Know"));

        assertAll(
            () -> assertTrue(source.toFile().setReadable(false)),
            () -> assertThrows(IllegalArgumentException.class, () ->
                testObject.transform(source, destination)
            )
        );
    }

    @Test
    @DisplayName("Destination cannot be directory.")
    void destinationCannotBeDirectory(@TempDir Path tempDir) {
        Path source = tempDir.resolve("input.txt");

        assertThrows(IllegalArgumentException.class, () ->
            testObject.transform(source, tempDir)
        );
    }
}