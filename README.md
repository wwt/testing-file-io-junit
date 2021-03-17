## Testing File IO with JUnit

The complete source is available on [GitHub](https://github.com/wwt/testing-file-io-junit)

### Problem

We have been tasked with writing a Java class that reads in a text file line-by-line, applies a function to each read line,
and writes the result to a destination file.

Since we have a general idea of the interface we are trying to fulfill, we'll start by defining an empty implementation
of the class under test.
```java
package com.wwt.testing.files;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;

public class TextFileTransformer {

  public TextFileTransformer(Function<String, String> lineTransformer) {
  }

  public void transform(Path source, Path destination) throws IOException {
      // TODO!
  }
}
```

### Let's write some tests!

To start driving out our desired functionality, we'll write a failing test. First, we'll need an instance of
the class under test.

We have a few options for the line transformation function; to keep things simple, we can use 
a method reference to fulfill the required `Function<String, String>`. Making the input uppercase will be sufficient to 
verify the transformation is applied.

```java
package com.wwt.testing.files;

class TextFileTransformerTest {
  private final TextFileTransformer testObject = new TextFileTransformer(String::toUpperCase);
}
```

Our first goal is processing a single line file. We want to use a real file rather than a mock 
because we want to ensure it actually works against real input, and we do not want to spend the rest of the 
day emulating the internals of `Path` with a mocking framework.

To fill out the input file's contents, we can use `Files.write(path, stringBytes)`. Once test execution completes, this 
temporary file should be deleted. We _could_ remove the file manually in a `try/finally` block, but there must be a better way!

Let's try out JUnit5's `@TempDir` annotation instead. When you annotate a `File` or `Path` parameter with `@TempDir`, 
JUnit5 will supply a temporary directory which is recursively deleted when the test method completes. 

```java
@Test
@DisplayName("Should transform single line source to destination")
void transformsSingleLineFile(@TempDir Path tempDir) throws IOException {
    var destination = tempDir.resolve("destination.txt");
    var source = tempDir.resolve("source.txt");
    Files.write(source, "Hello World!".getBytes(StandardCharsets.UTF_8));

    testObject.transform(source, destination);

    assertEquals("HELLO WORLD!", Files.readString(destination));
}
```

The first line of our test resolves a non-existent file called "destination.txt" against the temporary
directory. This is where our results will be written.

The next lines set up our source file. Similarly, we resolve a file named "source.txt", and write "Hello World!" as its 
contents.

Now that the test is set up, we invoke the class under test by providing the source and destination files. Remember: our 
test object is configured to upper case the input. 

Finally, we read the output file to verify that the expected string "HELLO WORLD!" has been written. 

As expected, the test fails, so now we need to implement the desired functionality in `TextFileTransformer`:

```java
package com.wwt.testing.files;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.wwt.testing.files.Preconditions.checkArgument;
import static java.util.function.Predicate.not;

public class TextFileTransformer {
    private final Function<String, String> lineTransformer;

    public TextFileTransformer(Function<String, String> lineTransformer) {
        this.lineTransformer = lineTransformer;
    }

    public void transform(Path source, Path destination) throws IOException {
        try (var lines = Files.lines(source);
             var writer = Files.newBufferedWriter(destination);
             var printWriter = new PrintWriter(writer)) {

            lines
                .map(lineTransformer)
                .forEach(printWriter::println);
        }
    }
}
```
Another quick run of the test, and we should be greeted with success!

This implementation covers the basic functionality we need in this class, but we should also think about what could go 
wrong:
- What happens when a non-existent source file is provided?
- What happens when I provide a directory as a source?
- What happens when the source file is not readable?
- What happens when I provide a directory as the destination?

For the sake of this little program, I decided when possible we should fail fast when possible with
an `IllegalArgumentException`; otherwise, if something exceptional happens, let the IOException be thrown.

If you'd like to see how these tests are implemented, visit the [GitHub](https://github.com/wwt/testing-file-io-junit) repository.

### Help! I'm stuck on JUnit4!

JUnit4's @Rules should be avoided whenever possible, as JUnit5 has adopted an extension based approach to replace rules.

That said, if your team has some technical reason to stay on JUnit4, you're in luck too. JUnit4 comes bundled with
the `TemporaryFolder` rule, which can manage temporary files for you.

To use the rule, declare a public field that is a new instance of `TemporaryFolder` annotated with `@Rule`. Use that field
to create folders or files as necessary. These files will be tracked and removed after each test completes.

```java

@Deprecated // Please use JUnit5!
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
}
```
### What to do about complex data?

If you have complex test data, or want to verify your class works under a variety of realistic circumstances, I find it
useful to place well-named example files under `src/test/resources`. You can then use the Path API to enter the directory,
and resolve your test data file.

```java
  @Test
  void loadFileByPath() throws IOException {
      Path jsonFile = Paths.get("src", "test", "resources").resolve("canned-data.json");

      assertAll(
          () -> assertTrue(Files.exists(jsonFile)),
          () -> assertTrue(Files.readAllLines(jsonFile).stream().anyMatch(line -> line.contains("Bobby")))
      );
  }
```

### Other Options

#### Jimfs - An Emulated In-Memory Filesystem

If we want more granular control over the type of FileSystem or desire an in-memory system, Jimfs is a great option.

```java
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
```

Any files created in the Jimfs `FileSystem` will be removed when the fileSystem is closed, or the JVM terminates. Note
the teardown method that closes the filesystem, so we don't maintain state between our tests.


### TLDR

When you're testing file IO with JUnit, prefer using real files. Use JUnit's TemporaryDirectory support to manage your
files, so you don't have to. If you really don't want to use the filesystem, consider a solution like Jimfs. 

### Additional Resources
- [JUnit5 User Guide, re: @TempDir](https://junit.org/junit5/docs/current/user-guide/#writing-tests-built-in-extensions-TempDirectory)
- [JUnit4 @TemporaryFolder Rule](https://junit.org/junit4/javadoc/4.13/org/junit/rules/TemporaryFolder.html)
- [GitHub Source for this Article](https://github.com/wwt/testing-file-io-junit)