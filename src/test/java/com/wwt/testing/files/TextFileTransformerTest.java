package com.wwt.testing.files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
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

        assertThat(Files.readAllLines(destination)).first()
            .isEqualTo("HELLO WORLD!");
    }

    @Test
    @DisplayName("Should transform multiline source to destination")
    void transformManyLines(@TempDir Path tempDir) throws IOException {
        var destination = tempDir.resolve("destination.txt");
        var source = tempDir.resolve("source.txt");
        Files.write(source, List.of("Larry", "Curly", "Moe"));

        testObject.transform(source, destination);

        assertThat(Files.readAllLines(destination))
                .containsExactly("LARRY", "CURLY", "MOE");
    }

    @Test
    @DisplayName("Should overwrite existing destination")
    void shouldOverwriteExistingDestination(@TempDir Path tempDir) throws IOException {
        var destination = tempDir.resolve("destination.txt");
        var source = tempDir.resolve("source.txt");
        Files.write(source, List.of("Larry", "Curly", "Moe"));
        Files.write(destination, List.of("Hope this wasn't important"));

        testObject.transform(source, destination);

        assertThat(Files.readAllLines(destination))
                .containsExactly("LARRY", "CURLY", "MOE");
    }

    @Test
    @DisplayName("Source cannot be directory.")
    void sourceCannotBeDirectory(@TempDir Path tempDir) {
        var destination = tempDir.resolve("should-not-create.txt");

        assertThatThrownBy(() -> testObject.transform(tempDir, destination))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(destination).doesNotExist();
    }


    @Test
    @DisplayName("Source file must exist")
    void sourceMustExist(@TempDir Path tempDir) {
        var destination = tempDir.resolve("destination.txt");
        var source = tempDir.resolve("source.txt");

        assertThatThrownBy(() -> testObject.transform(source, destination))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Source must be readable")
    void sourceMustBeReadable(@TempDir Path tempDir) throws IOException {
        var destination = tempDir.resolve("destination.txt");
        var source = tempDir.resolve("source.txt");
        Files.write(source, List.of("We", "Will", "Never", "Know"));

        makeUnreadable(source);

        assertThatThrownBy(() -> testObject.transform(source, destination))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void makeUnreadable(Path source) {
        assertThat(source.toFile().setReadable(false)).isTrue();
    }

    @Test
    @DisplayName("Destination cannot be directory.")
    void destinationCannotBeDirectory(@TempDir Path tempDir) {
        var source = tempDir.resolve("input.txt");

        assertThatThrownBy(() -> testObject.transform(source, tempDir))
            .isInstanceOf(IllegalArgumentException.class);
    }
}