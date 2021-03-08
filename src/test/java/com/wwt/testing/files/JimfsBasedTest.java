package com.wwt.testing.files;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JimfsBasedTest {
    private Path rootPath;
    private FileSystem fileSystem;

    private final TextFileTransformer testObject = new TextFileTransformer(String::toUpperCase);

    @BeforeEach
    void setup() throws IOException {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        rootPath = fileSystem.getPath("/test");
        Files.createDirectory(rootPath);
    }

    @AfterEach
    void teardown() throws IOException {
        fileSystem.close();
    }

    @Test
    void testWithFakeFilesystem() throws IOException {
        Path source = rootPath.resolve("source.txt");
        Path destination = rootPath.resolve("destination.txt");
        Files.write(source, List.of("larry", "curly", "moe"));

        testObject.transform(source, destination);

        assertEquals(List.of("LARRY", "CURLY", "MOE"), Files.readAllLines(destination));
    }
}
