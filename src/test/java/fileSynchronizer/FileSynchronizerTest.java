package fileSynchronizer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static org.junit.jupiter.api.Assertions.*;

public final class FileSynchronizerTest {

    private final Path testingParentDirectory = Path.of(System.getProperty("user.home")).resolve("Desktop").resolve("filesync_test");
    private final Path testingLocalDirectory = testingParentDirectory.resolve("local");
    private final Path testingRemoteDirectory = testingParentDirectory.resolve("remote");

    private InputStream userInput = new ByteArrayInputStream(new byte[]{});

    void createFiles(Path parentDir, Path...relativePaths) {
        for (Path relative : relativePaths) {
            createFile(parentDir.resolve(relative));
        }
    }

    void createFile(Path absoluePath) {
        try {
            Files.createDirectories(absoluePath.getParent());
            Files.createFile(absoluePath);
        } catch (IOException ioE) {
            ioE.printStackTrace();
        }
    }

    void createDirectories(Path parentDir, Path...relativePaths) {
        for (Path relative : relativePaths) {
            createDirectories(parentDir.resolve(relative));
        }
    }

    void createDirectories(Path absoluePath) {
        try {
            Files.createDirectories(absoluePath.getParent());
        } catch (IOException ioE) {
            ioE.printStackTrace();
        }
    }

    void appendFile(Path filepath, String textToAppend) {
        try {
            Files.write(filepath, textToAppend.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException ioE) {
            ioE.printStackTrace();
        }
    }

    void delay(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void passUserInput(String ... inputs) {
        StringBuilder allInputs = new StringBuilder();
        for (String token : inputs) {
            allInputs.append(token).append('\n');
        }

        userInput = new ByteArrayInputStream(allInputs.toString().getBytes());
    }

    String getFileContents(Path filepath) {
        try {
            StringBuilder allLines = new StringBuilder();

            for (String line : Files.readAllLines(filepath)) {
                allLines.append(line);
            }

            return allLines.toString();
        } catch (IOException ioE) {
            ioE.printStackTrace();
        }

        return "";
    }

    boolean allFilesExist(Path parentDir, Path...paths) {
        for (Path p : paths) {
            if (!parentDir.resolve(p).toFile().exists()) return false;
        }
        return true;
    }

    FileSynchronizer testingFileSynchronizer() {
        return new FileSynchronizer(testingLocalDirectory.toString(), testingRemoteDirectory.toString(), "local", "remote", userInput, true, true);
    }

    @BeforeEach
    void setUpTestDirectories() {
        try {
            Files.createDirectory(testingParentDirectory);
            Files.createDirectory(testingLocalDirectory);
            Files.createDirectory(testingRemoteDirectory);
        } catch (IOException ioE) {
            ioE.printStackTrace();
        }
    }

    @AfterEach
    void tearDownTestDirectories() {
        try {
            Files.walkFileTree(testingParentDirectory, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    Files.delete(path);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
                    System.out.println("TEST ERROR: Could not visit file '" + path + "'");
                    System.exit(1);
                    return FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
                    Files.delete(path);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ioE) {
            ioE.printStackTrace();
        }
    }

    @Test
    void noUserFilesInEitherDirectory() {
        FileSynchronizer synchronizer = testingFileSynchronizer();
        synchronizer.synchronizeFileTrees();
        delay(10);

        File localDir = testingLocalDirectory.toFile();
        String[] localDirFilenames = localDir.list();
        boolean localHasUserFiles = false;
        for (String filename : localDirFilenames) {
            if (!filename.equals(".sync_trash") && !filename.equals(".sync_log") && !filename.equals(".sync_exclude")) {
                localHasUserFiles = true;
                break;
            }
        }

        File remoteDir = testingRemoteDirectory.toFile();
        String[] remoteDirFilenames = remoteDir.list();
        boolean remoteHasUserFiles = false;
        for (String filename : remoteDirFilenames) {
            if (!filename.equals(".sync_trash") && !filename.equals(".sync_log") && !filename.equals(".sync_exclude")) {
                remoteHasUserFiles = true;
                break;
            }
        }

        assertFalse(localHasUserFiles);
        assertFalse(remoteHasUserFiles);
    }

    @Test
    void newLocalFilesShouldBeCopiedToRemote() {
        FileSynchronizer synchronizer = testingFileSynchronizer();
        synchronizer.synchronizeFileTrees();
        delay(10);

        Path newLocalFile1 = Path.of("newLocalFile1.txt");
        Path newLocalFile2 = Path.of("newLocalFile2.txt");

        createFiles(testingLocalDirectory, newLocalFile1, newLocalFile2);

        synchronizer.synchronizeFileTrees();

        assertTrue(allFilesExist(testingRemoteDirectory, newLocalFile1, newLocalFile2));
    }

    @Test
    void newRemoteFilesShouldBeCopiedToLocal() {
        FileSynchronizer synchronizer = testingFileSynchronizer();
        synchronizer.synchronizeFileTrees();
        delay(10);

        Path newLocalFile1 = Path.of("newRemoteFile1.txt");
        Path newLocalFile2 = Path.of("newRemoteFile2.txt");

        createFiles(testingRemoteDirectory, newLocalFile1, newLocalFile2);

        synchronizer.synchronizeFileTrees();

        assertTrue(allFilesExist(testingLocalDirectory, newLocalFile1, newLocalFile2));
    }

    @Test
    void newEmptyLocalDirectoriesShouldNotBeCopiedToRemote() {
        FileSynchronizer synchronizer = testingFileSynchronizer();
        synchronizer.synchronizeFileTrees();
        delay(10);

        Path newLocalDir1 = Path.of("newLocalDir1.txt");
        Path newLocalDir2 = Path.of("newLocalDir2.txt");

        createDirectories(testingLocalDirectory, newLocalDir1, newLocalDir2);

        synchronizer.synchronizeFileTrees();

        assertFalse(allFilesExist(testingRemoteDirectory, newLocalDir1, newLocalDir2));
    }

    @Test
    void newEmptyRemoteDirectoriesShouldNotBeCopiedToRemote() {
        FileSynchronizer synchronizer = testingFileSynchronizer();
        synchronizer.synchronizeFileTrees();
        delay(10);

        Path newRemoteDir1 = Path.of("newRemoteDir1.txt");
        Path newRemoteDir2 = Path.of("newRemoteDir2.txt");

        createDirectories(testingRemoteDirectory, newRemoteDir1, newRemoteDir2);

        synchronizer.synchronizeFileTrees();

        assertFalse(allFilesExist(testingLocalDirectory, newRemoteDir1, newRemoteDir2));
    }

    @Test
    void newNonEmptyLocalDirectoryShouldBeCopiedToRemote() {
        FileSynchronizer synchronizer = testingFileSynchronizer();
        synchronizer.synchronizeFileTrees();
        delay(10);

        Path newLocalDir1 = Path.of("newLocalDir1.txt");
        Path newLocalFile1 = newLocalDir1.resolve("newLocalFile1.txt");

        createDirectories(testingLocalDirectory, newLocalDir1);
        createFiles(testingLocalDirectory, newLocalFile1);

        synchronizer.synchronizeFileTrees();

        assertTrue(allFilesExist(testingRemoteDirectory, newLocalDir1, newLocalFile1));
    }

    @Test
    void newNonEmptyRemoteDirectoryShouldBeCopiedToRemote() {
        FileSynchronizer synchronizer = testingFileSynchronizer();
        synchronizer.synchronizeFileTrees();
        delay(10);

        Path newRemoteDir1 = Path.of("newRemoteDir1.txt");
        Path newRemoteFile1 = newRemoteDir1.resolve("newRemoteFile1.txt");

        createDirectories(testingRemoteDirectory, newRemoteDir1);
        createFiles(testingRemoteDirectory, newRemoteFile1);

        synchronizer.synchronizeFileTrees();

        assertTrue(allFilesExist(testingLocalDirectory, newRemoteDir1, newRemoteFile1));
    }

    @Test
    void modifiedLocalFilesShouldBeCopiedToRemote() {
        Path localFile1 = Path.of("modifiedLocalFile");
        Path localFile2 = Path.of("modifiedLocalFile2");
        createFiles(testingLocalDirectory, localFile1, localFile2);

        FileSynchronizer synchronizer = testingFileSynchronizer();
        synchronizer.synchronizeFileTrees();
        delay(10);

        appendFile(testingLocalDirectory.resolve(localFile1), "AppendTest");
        appendFile(testingLocalDirectory.resolve(localFile2), "AppendTest2");

        FileSynchronizer secondSync = testingFileSynchronizer();
        secondSync.synchronizeFileTrees();

        assertEquals("AppendTest", getFileContents(testingRemoteDirectory.resolve(localFile1)));
        assertEquals("AppendTest2", getFileContents(testingRemoteDirectory.resolve(localFile2)));
    }

    @Test
    void modifiedRemoteFilesShouldBeCopiedToLocal() {
        Path remoteFile1 = Path.of("modifiedRemoteFile");
        Path remoteFile2 = Path.of("modifiedRemoteFile2");
        createFiles(testingLocalDirectory, remoteFile1, remoteFile2);

        FileSynchronizer synchronizer = testingFileSynchronizer();
        synchronizer.synchronizeFileTrees();
        delay(10);

        appendFile(testingLocalDirectory.resolve(remoteFile1), "AppendTest");
        appendFile(testingLocalDirectory.resolve(remoteFile2), "AppendTest2");

        FileSynchronizer secondSync = testingFileSynchronizer();
        secondSync.synchronizeFileTrees();

        assertEquals("AppendTest", getFileContents(testingRemoteDirectory.resolve(remoteFile1)));
        assertEquals("AppendTest2", getFileContents(testingRemoteDirectory.resolve(remoteFile2)));
    }

    @Test
    void fileWithSpaceInNameSyncSuccessfully() {
        FileSynchronizer synchronizer = testingFileSynchronizer();
        synchronizer.synchronizeFileTrees();
        delay(10);

        Path newLocalFile1 = Path.of("new Local File1.txt");
        Path newLocalFile2 = Path.of("new Local File2.txt");

        createFiles(testingLocalDirectory, newLocalFile1, newLocalFile2);

        synchronizer.synchronizeFileTrees();

        assertTrue(allFilesExist(testingRemoteDirectory, newLocalFile1, newLocalFile2));
    }

    @Test
    void syncConflictShouldCopyLocalFileOverRemote() {

        Path conflict = Path.of("conflictFile");
        createFiles(testingLocalDirectory, conflict);
        createFiles(testingRemoteDirectory, conflict);

        passUserInput("1"); // Choose local changes

        FileSynchronizer synchronizer = testingFileSynchronizer();
        synchronizer.synchronizeFileTrees();
        delay(10);

        appendFile(testingLocalDirectory.resolve(conflict), "Local");
        appendFile(testingRemoteDirectory.resolve(conflict), "Remote");

        passUserInput("1"); // Choose local changes

        FileSynchronizer secondSync = testingFileSynchronizer();
        secondSync.synchronizeFileTrees();

        assertEquals("Local", getFileContents(testingLocalDirectory.resolve(conflict)));
        assertEquals("Local", getFileContents(testingRemoteDirectory.resolve(conflict)));
    }

    @Test
    void syncConflictShouldCopyRemoteFileOverLocal() {

        Path conflict = Path.of("conflictFile");
        createFiles(testingLocalDirectory, conflict);
        createFiles(testingRemoteDirectory, conflict);

        passUserInput("2"); // Choose local changes

        FileSynchronizer synchronizer = testingFileSynchronizer();
        synchronizer.synchronizeFileTrees();
        delay(10);

        appendFile(testingLocalDirectory.resolve(conflict), "Local");
        appendFile(testingRemoteDirectory.resolve(conflict), "Remote");

        passUserInput("2"); // Choose local changes

        FileSynchronizer secondSync = testingFileSynchronizer();
        secondSync.synchronizeFileTrees();

        assertEquals("Remote", getFileContents(testingLocalDirectory.resolve(conflict)));
        assertEquals("Remote", getFileContents(testingRemoteDirectory.resolve(conflict)));
    }


    /*
    1. [X] No files changed since last sync
    2. [X] Only regular files changed since last sync l/r
    4. [X] Only directories changed since last sync l/r
    5. [X] Files and directories changed since last sync (from only one root) l/r
    6. [ ] Directories have never been synced before
    	    (if .sync_log or sync log entry doesn't exist, give option to either set last sync time OR
    	    perform the sync with last_sync_time=0, which would update all the files to each of their
    	    latest versions, and would make the two directories identical) (l/r)
    7. [ ] File modified since last sync in both directories, resulting in a conflict
    9. [X] New file created l/r
    10. [X] New directory created l/r
    11. [ ] 1000 files l/r
    12. [ ] File is deleted since last sync l/r
    13. [X] Files with spaces in the name l/r
    14. [ ] Excluded files are modified l/r
    15. [ ] Test .sync_exclude is written correctly l/r
    16. [ ] Test .sync_log is written correctly l/r
    17. [ ] Test sync trash structures files correctly l/r
    18. [ ] Syncing with multiple directories l/r
    */
}
