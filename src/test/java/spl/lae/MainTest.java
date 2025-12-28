package spl.lae;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @TempDir
    Path tempDir;

    private static final String VALID_INPUT_JSON = """
    {
      "operator": "+",
      "operands": [
        [
          [1, 2],
          [3, 4]
        ],
        [
          [10, 20],
          [30, 40]
        ]
      ]
    }
    """;

    @Test
    void main_invalidArgsCount_throws() {
        assertThrows(IllegalArgumentException.class, () -> Main.main(new String[]{}));
        assertThrows(IllegalArgumentException.class, () -> Main.main(new String[]{"1"}));
        assertThrows(IllegalArgumentException.class, () -> Main.main(new String[]{"1", "a"}));
        assertThrows(IllegalArgumentException.class, () -> Main.main(new String[]{"1", "a", "b", "c"}));
    }

    @Test
    void main_invalidThreadCount_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                Main.main(new String[]{"abc", "in.json", "out.json"})
        );
        assertThrows(IllegalArgumentException.class, () ->
                Main.main(new String[]{"0", "in.json", "out.json"})
        );
        assertThrows(IllegalArgumentException.class, () ->
                Main.main(new String[]{"-2", "in.json", "out.json"})
        );
    }

    @Test
    void main_validComputation_writesOutputFile() throws Exception {
        Path input = tempDir.resolve("input.json");
        Path output = tempDir.resolve("output.json");

        Files.writeString(input, VALID_INPUT_JSON);

        assertDoesNotThrow(() -> Main.main(new String[]{
                "2",
                input.toString(),
                output.toString()
        }));

        assertTrue(Files.exists(output));
        String out = Files.readString(output);
        assertFalse(out.isBlank());
    }

    @Test
    void main_invalidJson_writesErrorMessage() throws Exception {
        Path input = tempDir.resolve("bad.json");
        Path output = tempDir.resolve("out.json");

        Files.writeString(input, "{ not valid json }");

        assertDoesNotThrow(() -> Main.main(new String[]{
                "2",
                input.toString(),
                output.toString()
        }));

        assertTrue(Files.exists(output));
        String out = Files.readString(output);
        assertFalse(out.isBlank());
    }
}
