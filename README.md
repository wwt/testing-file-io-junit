### Problem

When writing JUnit tests, you want your test cases to be as descriptive and concise as possible. File I/O complicates
matters because one must set up any necessary state, invoke functionality of the class under test, and clean up
afterwards.

### Some Possible Solutions

- JUnit5:
  The [`@TempDir`](https://junit.org/junit5/docs/current/user-guide/#writing-tests-built-in-extensions-TempDirectory)
  annotation
- JUnit4: The [`@TemporaryFolder`](https://junit.org/junit4/javadoc/4.13/org/junit/rules/TemporaryFolder.html) rule
- [Jimfs](https://github.com/google/jimfs): An in-memory file system
- Files Under `src/test/resources`  
- `@Mock`/`@Spy` of `File`/`Path`: Try to avoid this approach. Files require extensive set up to emulate, only
  attempt if you are trying to test a very specific edge case.

### Let's write some code!

The class we'll be testing today is a `TextFileTranformer`, its responsibility is to read each line of an input file,
apply a transformation, and store all the transformed lines in a new file.

We'll start off by creating an instance of the class, and we'll configure the test instance to upper-case each line of input.

```java
import ...

class TextFileTransformerTest {
    private final TextFileTransformer testObject = new TextFileTransformer(String::toUpperCase);
}
```

Now comes the tricky part. We want to programmatically create a file with specified contents, operate on that file, and
verify the output. We will also want to clean up any files created during the test run when the test completes -- be it
on success, or after an error/failure, so we don't litter the disk with generated files.

Instead of managing test files manually, let's try out JUnit5's `@TempDir` annotation. The `@TempDir` annotation will
supply a temporary directory (as a `Path` or `File`) that is recursively deleted at the end of the test. This allows us
to safely create files without having to worry about cleaning them up.

```java
@Test
@DisplayName("Should transform multiline source to destination")
void transformManyLines(@TempDir Path tempDir)throws IOException{
    Path destination = tempDir.resolve("destination.txt");
    Path source = tempDir.resolve("source.txt");
    Files.write(source, List.of("Larry","Curly","Moe"));
    
    testObject.transform(source, destination);
    
    assertEquals(List.of("LARRY","CURLY","MOE"), Files.readAllLines(destination));
}
```

In this test case, we needed a source `Path` to read from, and a destination `Path` to write to. We annotated a
parameter named `tempDir` with `@TempDir`, causing the Junit Engine to inject a temporary directory that will be removed
after the test finishes.

From that directory, we can resolve our destination and source paths. Write a few lines of content to the source file
with `Files.write(...)`, invoke the class under test, and verify the expected contents have been written to the target
file with` Files.readAllLines(...)`.

Phew, now that there is a failing test, we have something we need to implement in `TextFileTransformer`.

```java
package com.wwt.testing.files;

import ...

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

The last few things to wrap up are to verify:

- What happens when I provide a directory as a source?
- What happens when I provide a directory as the destination?

For the sake of this little program, I decided that either of those situations should fail fast with
an `IllegalArgumentException`.

```java
@Test
@DisplayName("Destination cannot be directory.")
void destinationCannotBeDirectory(@TempDir Path tempDir){
    Path source = tempDir.resolve("input.txt");

    assertThrows(IllegalArgumentException.class, ()->
        testObject.transform(source,tempDir)
    );
}
```

For both situations, the test is straightforward, just send in a directory rather than a path to the file, and verify
the thrown exception with JUnit's `assertThrows(...)`.

### Help! I'm stuck on JUnit4!

JUnit4's @Rules should be avoided whenever possible, as JUnit5 has adopted an extension based approach to replace rules.

That said, if your team has some technical reason to stay on JUnit4, you're in luck too. JUnit4 comes bundled with
the `TemporaryFolder` rule, which can manage temporary files for you

To use the rule, create a public field that is an instance of `TemporaryFolder`, annotated with `@Rule`. Use that field
to create folders or files as necessary. These files will be tracked and removed after each test completes.

```java

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
}
```

### Other Options

#### Jimfs - An Emulated In-Memory Filesystem

If we want more granular control over the type of Filesystem or desire an in-memory system, Jimfs is a great option.

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
the teardown method that closes the filesystem, so we don't maintain static state between our tests.

#### Test Data Under src/test/resources

If you have complex test data, or want to verify your class works under a variety of realistic circumstances, I find it
useful to place well-named example files under `src/test/resources`. You can then use the Path API to enter the directory,
and resolve your test data file.

```java
  @Test
  public void loadFileByPath() throws IOException {
      Path jsonFile = Paths.get("src", "test", "resources").resolve("canned-data.json");

      assertAll(
          () -> assertTrue(Files.exists(jsonFile)),
          () -> assertTrue(Files.readAllLines(jsonFile).stream().anyMatch(line -> line.contains("Bobby")))
      );
  }
```

### Additional Resources
- [JUnit5 User Guide, re: @TempDir](https://junit.org/junit5/docs/current/user-guide/#writing-tests-built-in-extensions-TempDirectory)
- [JUnit4 @TemporaryFolder Rule](https://junit.org/junit4/javadoc/4.13/org/junit/rules/TemporaryFolder.html)
- [GitHub Source for this Article](https://github.com/wwt/testing-file-io-junit)