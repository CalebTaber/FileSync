package fileSynchronizer;

import org.junit.jupiter.api.BeforeEach;
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

    //@BeforeEach
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

    @Test
    void copyOneFileFromLocalToRemote() {

    }
    
    /*
    1. No files changed since last sync
    2. Only regular files changed since last sync l/r
    4. Only directories changed since last sync l/r
    5. Files and directories changed since last sync (from only one root) l/r
    6. Directories have never been synced before 
    	(if .sync_log or sync log entry doesn't exist, give option to either set last sync time OR
    	 perform the sync with last_sync_time=0, which would update all the files to each of their
    	 latest versions, and would make the two directories identical) (l/r)
    7. File modified since last sync in both directories, resulting in a conflict
    8. Multiple nested directories of files modified
    9. New file created l/r
    10. New directory created l/r
    11. New file 'new' is created locally. New directory 'new' is created remotely. File copying fails l/r
    12. 1000 files l/r
    13. File is deleted since last sync l/r
    14. Files with spaces in the name l/r
    15. Excluded files are modified l/r
    16. Test .sync_exclude is written correctly l/r
    17. Test .sync_log is written correctly l/r
    18. Test sync trash structures files correctly l/r
    19. Test if 'y' option is selected, that the sync trash dir is deleted entirely l/r
    20. Test if 'n' option is selected, that the sync trash dir is not deleted l/r
    21. Test if 'n' option is selected and then another sync is started that the sync trash dir is emptied before starting the sync
    */
}
