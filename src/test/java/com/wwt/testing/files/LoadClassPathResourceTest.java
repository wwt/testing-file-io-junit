package com.wwt.testing.files;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadClassPathResourceTest {

    @Test
    void loadFileByPath() throws IOException {
        // You can also use the classLoader or Spring if available.
        var resolve = Paths.get("src", "test", "resources").resolve("canned-data.json");

        assertThat(resolve)
                .exists()
                .content().contains("Bobby");
    }
}
