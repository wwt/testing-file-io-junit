package com.wwt.testing.files;

import java.util.function.Predicate;

class Preconditions {

    static <T> void checkArgument(T argument, Predicate<T> check, String message) {
        if (!check.test(argument)) {
            throw new IllegalArgumentException(message);
        }
    }

    private Preconditions() {
    }
}
