### What we are Building Today

I want to create a simple Java class that reads an input file, applies a transformation, and writes the result to an 
output file.

The class requires a transformation function that will be invoked on each line to create the output file. Something like
this in Java:
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

We want to make sure our implementation works, and provide examples of how the class should be used. Seems like good
case for writing some tests!

### Test Time

First we'll create `TextFileTransformerTest`, and configure an instance of the class under test. To verify the 
transformation is applied, we can just upper-case the input.

```java
package com.wwt.testing.files;

class TextFileTransformerTest {
  private final TextFileTransformer testObject = new TextFileTransformer(String::toUpperCase);
}
```

Now that we have a test instance to use, let's try to process a single line file. For this test, I want to write 
a string out to an input file, provide it to the class under test, and then delete the file when the test completes. 
We _could_ remove the file ourselves in a `try/finally` block, but there must be a cleaner way!

Let's try out JUnit5's _experimental_ `@TempDir` annotation. When you annotate a `File` or `Path` parameter with `@TempDir`, 
JUnit5 will supply a temporary directory that is recursively deleted when the test method completes. 

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

The first line of our test resolves a non-existent file called `destination.txt` against the temporary
directory. This is where our results will be written.

The next lines set up our source file. Similarly, we resolve a file named `source.txt`, and write "Hello World!" as its 
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

If you'd like to see how these tests are implemented, visit the [GitHub](https://github.com/wwt/testing-file-io-junit/blob/main/src/test/java/com/wwt/testing/files/TextFileTransformerTest.java) repository.

### Help! I'm stuck on JUnit4!

JUnit4's `@Rules` should be avoided whenever possible, as JUnit5 has adopted an extension based approach to replace rules.

That said, if your team has some technical reason to stay on JUnit4, you are in luck too. JUnit4 comes bundled with
the `TemporaryFolder` rule, which can also clean up temporary files for you.

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

### TL;DR

When you're testing file IO with JUnit, prefer using real files. Use JUnit's TemporaryDirectory support to manage your
files, so you don't have to. If you really don't want to use the filesystem, consider a solution like Jimfs. 

### Additional Resources
- [JUnit5 User Guide, re: @TempDir](https://junit.org/junit5/docs/current/user-guide/#writing-tests-built-in-extensions-TempDirectory)
- [JUnit4 @TemporaryFolder Rule](https://junit.org/junit4/javadoc/4.13/org/junit/rules/TemporaryFolder.html)
- [GitHub Source for this Article](https://github.com/wwt/testing-file-io-junit)
- [Jimfs - In Memory Filesystem](https://github.com/google/jimfs)