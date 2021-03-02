package com.wwt.testing.files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * This is included to demonstrate @TemporaryFolder with legacy JUnit4
 * ONLY use this approach if you ABSOLUTELY cannot upgrade to JUnit5!
 */
@Deprecated
public class VintageTextFileTransformerTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final TextFileTransformer testObject = new TextFileTransformer(String::toUpperCase);

    @Test
    public void transformsSingleLineFile() throws IOException {
        Path destination = temporaryFolder.newFile("destination.txt").toPath();
        Path source = temporaryFolder.newFile("source.txt").toPath();
        Files.write(source, List.of("Hello World!"));

        testObject.transform(source, destination);

        assertEquals("HELLO WORLD!", Files.readAllLines(destination).get(0));
    }

    @Test
    public void transformManyLines() throws IOException {
        Path destination = temporaryFolder.newFile("destination.txt").toPath();
        Path source = temporaryFolder.newFile("source.txt").toPath();
        Files.write(source, List.of("Larry", "Curly", "Moe"));

        testObject.transform(source, destination);

        assertEquals(List.of("LARRY", "CURLY", "MOE"), Files.readAllLines(destination));
    }

    @Test
    public void overwritesExistingDestinationFile() throws IOException{
        Path destination = temporaryFolder.newFile("destination.txt").toPath();
        Path source = temporaryFolder.newFile("source.txt").toPath();
        Files.write(destination, List.of("Some contents"));
        Files.write(source, List.of("Larry", "Curly", "Moe"));

        testObject.transform(source, destination);

        assertEquals(List.of("LARRY", "CURLY", "MOE"), Files.readAllLines(destination));
    }

    @Test
    public void sourceMustBeRegularFile() throws IOException {
        Path destination = temporaryFolder.newFile("should-not-create.txt").toPath();

        assertThrows(IllegalArgumentException.class, () ->
            testObject.transform(temporaryFolder.getRoot().toPath(), destination)
        );
    }

    @Test
    public void destinationCannotBeDirectory() throws IOException {
        Path source = temporaryFolder.newFile("input.txt").toPath();

        assertThrows(IllegalArgumentException.class, () ->
            testObject.transform(source, temporaryFolder.getRoot().toPath())
        );
    }
}