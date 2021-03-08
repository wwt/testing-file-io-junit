package com.wwt.testing.files;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoadClassPathResource {

    @Test
    public void loadFileByPath() throws IOException {
        // You can also use the classLoader or Spring if available.
        Path resolve = Paths.get("src", "test", "resources").resolve("canned-data.json");

        assertAll(
            () -> assertTrue(Files.exists(resolve)),
            () -> assertTrue(Files.readAllLines(resolve).stream().anyMatch(line -> line.contains("Bobby")))
        );
    }
}
