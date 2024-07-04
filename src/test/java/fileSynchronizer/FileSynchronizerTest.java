package fileSynchronizer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileSynchronizerTest {

    private final String fileSeparator = FileSystems.getDefault().getSeparator();
    private final String testingParentDirectory = System.getProperty("user.home") + fileSeparator + "filesync_test" + fileSeparator;
    private final String testingLocalDirectory = testingParentDirectory + "local";
    private final String testingRemoteDirectory = testingParentDirectory + "remote";

    void setUpTestDirectories() {
        // Create testing directories
        try {
            Files.createDirectory(Path.of(testingParentDirectory));
            Files.createDirectory(Path.of(testingLocalDirectory));
            Files.createDirectory(Path.of(testingRemoteDirectory));
        } catch (IOException ioE) {
            ioE.printStackTrace();
        }
    }
}
