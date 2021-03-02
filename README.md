### Problem

When writing JUnit tests, you want your test cases to be as descriptive and concise as possible. File I/O complicates
matters because one must set up any necessary state, invoke functionality of the class under test, and clean up
afterwards.

### Options

- JUnit4: The `@TemporaryFolder` rule
- JUnit5: The `@TempDir` annotation
- Mocking `File`/`Path`: Avoid this whenever possible

### Demo Time!

To test the different approaches, we created a class called `TextFileTranformer`, its responsibility is to stream each
line of an input file, apply a transformation, and write the transformed line to an output file.

Let's start off with a JUnit5 test. As with all tests, we need an instance of our class under test. So we'll go ahead
and new one up as a field. 

The class under test happens to use a `Function<String, String>` that it will apply to each
line of the input file. For the sake of this test we'll simply uppercase the input.

```java
import ...

@DisplayName("Test file IO with JUnit5")
class TextFileTransformerTest {
    private final TextFileTransformer testObject = new TextFileTransformer(String::toUpperCase);
}
```

Now that we have a instance to test, lets start using it.

```java
@Test
@DisplayName("Should transform multiline source to destination")
void transformManyLines(@TempDir Path tempDir) throws IOException {
    Path destination = tempDir.resolve("destination.txt");
    Path source = tempDir.resolve("source.txt");
    Files.write(source, List.of("Larry", "Curly", "Moe"));

    testObject.transform(source, destination);

    assertEquals(List.of("LARRY", "CURLY", "MOE"), Files.readAllLines(destination));
}
```

### Additional Resources