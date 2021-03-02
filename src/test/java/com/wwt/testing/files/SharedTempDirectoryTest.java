package com.wwt.testing.files;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SharedTempDirectoryTest {
    private static final Logger logger = getLogger(SharedTempDirectoryTest.class);
    private static final List<String> STOOGES = List.of("Larry", "Curly", "Moe");

    @TempDir
    static Path sharedTempDir;

    @Test
    @Order(0)
    void writeContentsToSharedFile() throws IOException {
        Path destination = sharedTempDir.resolve("test.txt");
        logger.info("Writing to {}", destination);

        Files.write(destination, STOOGES);

        assertEquals(STOOGES, Files.readAllLines(destination));
    }

    @Test
    @Order(1)
    void verifyPreviouslyCreatedFileAvailable() throws IOException {
        Path destination = sharedTempDir.resolve("test.txt");

        // Note the file already exists, since we are using a shared static value.
        logger.info("Reading from {}", destination);
        assertTrue(Files.exists(destination));
        assertEquals(STOOGES, Files.readAllLines(destination));
    }
}
